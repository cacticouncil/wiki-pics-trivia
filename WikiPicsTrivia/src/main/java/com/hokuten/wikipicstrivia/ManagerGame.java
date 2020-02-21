package com.hokuten.wikipicstrivia;

import android.os.Message;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ManagerGame
{
    private static ManagerGame mInstance;

    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);

    private ModelGameRound            mGameRound;
    private ArrayList<ModelQuestion>  mQuestions;
    private StopWatch                 mStopWatch;
    private Future<?>                 mTimerThread;

    private ManagerGame()
    {
        mStopWatch         = new StopWatch();
        mQuestions         = new ArrayList<ModelQuestion>();
    }

    public static ManagerGame instance()
    {
        if (mInstance == null) mInstance = new ManagerGame();
        return mInstance;
    }

    public String canUserProceedWithGameMode(ModelSettings settings, ModelGameMode mode)
    {
        ModelQuestionBank bank = ManagerDB.instance().getQuestionBankStats();
        String message = null;

        // check for enough remaining questions before starting
        if (settings.updates <= 0)
        {
            message = Application2.instance().getResString(R.string.dialog_need_questions_start);
        }
        else if (bank.total <= 0)
        {
            message = Application2.instance().getResString(R.string.dialog_need_questions_empty);
        }
        else if (settings.doNotRepeat)
        {
            if (mode.categories == ModelGameMode.Categories.ALL)
            {
                if (mode.questions > 0 && (bank.total-bank.answered) < mode.questions)
                {
                    message = Application2.instance().getResString(R.string.dialog_need_questions_unanswered);
                }
                // if infinite, then enough to start round
            }
            else
            {
                int qcount = ManagerDB.instance().getCustomModeQuestionCount(mode);
                if ((mode.questions > 0 && qcount < mode.questions) || qcount == 0)
                {
                    message = Application2.instance().getResString(R.string.dialog_need_questions_custom);
                }
                // if infinite, then enough to start round
            }
        }
        else
        {
            // if we have questions and dnr is not set, this shouldn't be an issue
        }

        return message;
    }

    public void startRound(ModelGameMode mode)
    {
        mGameRound = new ModelGameRound();
        mGameRound.mode        = mode;
        mGameRound.active      = true;
        mGameRound.doNotRepeat = ManagerDB.instance().getSettings().doNotRepeat;
        mGameRound.correct     = 0;
        mGameRound.missed      = 0;

        mQuestions.clear();
        mStopWatch.reset();

        // start timer if necessary
        if (mGameRound.mode.timer > 0)
        {
            mStopWatch.start();

            if(mTimerThread != null)
                mTimerThread.cancel(false);
            mTimerThread = mScheduler.schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    endRound();
                }
            }, mGameRound.mode.timer, TimeUnit.MINUTES);
        }
    }

    public synchronized void endRound()
    {
        // error checking - return if startRound called or if endRound already called
        if (mGameRound == null || mGameRound.active == false)
            return;

        mGameRound.active = false;

        // list of incorrect or unvisited questions that may be marked unused
        final ArrayList<ModelQuestion> unusedQuestions = new ArrayList<ModelQuestion>();

        mStopWatch.stop();
        if(mTimerThread != null)
            mTimerThread.cancel(false);

        // for static number question modes, we must load all the remaining questions
        if (mGameRound.mode.questions > 0 && mQuestions.size() < mGameRound.mode.questions)
        {
            ArrayList<ModelQuestion> qs = ManagerDB.instance().getQuestionsRemaining(
                    mGameRound.doNotRepeat, mGameRound.mode, mGameRound.mode.questions - mQuestions.size());
            mQuestions.addAll(qs);
        }

        // for infinite question game modes, some questions may need to be removed from stats
        if (mGameRound.mode.questions == 0)
        {
            // last page in list is probably unvisited, because pager loads 1 extra
            if (!mQuestions.isEmpty() && !mQuestions.get(mQuestions.size() - 1).isVisited())
            {
                unusedQuestions.add(mQuestions.get(mQuestions.size() - 1));
                mQuestions.remove(mQuestions.size() - 1);
            }

            // second to last in list is probably unanswered, because answering triggers next page
            if (!mQuestions.isEmpty() && !mQuestions.get(mQuestions.size() - 1).isAnswered())
            {
                unusedQuestions.add(mQuestions.get(mQuestions.size() - 1));
                mQuestions.remove(mQuestions.size() - 1);
            }
        }

        // mark unused questions as unused
        Executors.newSingleThreadScheduledExecutor().schedule(new Runnable()
        {
            @Override
            public void run()
            {
                // if learning enabled, then also mark all incorrect questions as unused
                ModelSettings settings = ManagerDB.instance().getSettings();
                if (settings.learning)
                {
                    for (ModelQuestion q : mQuestions)
                        if (!q.isCorrect())
                            unusedQuestions.add(q);
                }

                ManagerDB.instance().markQuestionsUnused(unusedQuestions);
            }
        }, 0, TimeUnit.SECONDS);

        // switch to round over screen now!
        // note: this is happening on the UI thread
        Application2.instance().getMainActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Message msg = new Message();
                msg.what = PauseHandler.Message_Types.ROUND_OVER.ordinal();

                Application2.instance().getMainActivity().getPauseHandler().sendMessage(msg);
            }
        });
    }

    public ModelGameRound getGameRoundInfo()
    {
        return mGameRound;
    }

    public String getRemainingTime()
    {
        return mStopWatch.getRemainingTime(mGameRound.mode.timer);
    }

    public ModelQuestion newQuestion()
    {
        ModelQuestion q = ManagerDB.instance().getQuestion(mGameRound.doNotRepeat, mGameRound.mode);
        q.index = mQuestions.size();
        mQuestions.add(q);
        return q;
    }

    public ModelQuestion getQuestion(int index)
    {
        if (index >=0 && index < mQuestions.size()) return mQuestions.get(index);
        else return null;
    }

    public ArrayList<ModelQuestion> getQuestions()
    {
        return mQuestions;
    }

    public int getQuestionCount()
    {
        return mQuestions.size();
    }

    public boolean isRoundComplete()
    {
        // did player exhaust all misses?
        if (mGameRound.mode.misses > 0)
        {
            if (mGameRound.missed == mGameRound.mode.misses)
            {
                return true;
            }
        }

        // did player answer all questions?
        if (mGameRound.mode.questions > 0)
        {
            if ((mGameRound.correct + mGameRound.missed) == mGameRound.mode.questions)
            {
                return true;
            }
        }

        return false;
    }
}
