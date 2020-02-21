package com.hokuten.wikipicstrivia;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

//import android.app.DialogFragment;


public class FragmentQuestions extends Fragment implements View.OnClickListener, ManagerFreebase.Listener
{
    private static final int UPDATE_ON_INCREMENT = 10;
    private static enum UpdateState { IDLE, IN_PROGRESS, COMPLETE };

    private TextView    mTxtLocale;
    private TextView    mTxtStatsTotal;
    private TextView    mTxtStatsAnswered;
    //private TextView    mTxtStatsUnanswered;
    private TextView    mTxtStatsTopics;
    private TextView    mTxtStatsCategories;
    private Button      mBtnUpdateQuestions;
    private Button      mBtnResetAnswers;
    private Button      mBtnDeleteAll;
    private Button      mBtnDone;
    private ProgressBar mPrgUpdateQuestions;
    private UpdateState mQuestionUpdateState;
    //private int         mProgressStatus;
    private int         mProgressMaxValue;

    public FragmentQuestions()
    {
        super();

        mQuestionUpdateState = UpdateState.IDLE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_questions, container, false);

        // views
        mTxtLocale           = (TextView) view.findViewById(R.id.tvQuestionsLocale);
        mTxtStatsTotal       = (TextView) view.findViewById(R.id.tvStatsTotal);
        mTxtStatsAnswered    = (TextView) view.findViewById(R.id.tvStatsAnswered);
        //mTxtStatsUnanswered  = (TextView) view.findViewById(R.id.tvStatsUnanswered);
        mTxtStatsTopics      = (TextView) view.findViewById(R.id.tvStatsTopics);
        mTxtStatsCategories  = (TextView) view.findViewById(R.id.tvStatsCategories);
        mBtnUpdateQuestions  = (Button) view.findViewById(R.id.btnUpdateQuestions);
        mPrgUpdateQuestions  = (ProgressBar) view.findViewById(R.id.prgUpdateQuestions);
        mBtnResetAnswers     = (Button) view.findViewById(R.id.btnResetAnswers);
        mBtnDeleteAll        = (Button) view.findViewById(R.id.btnDeleteAll);
        mBtnDone             = (Button) view.findViewById(R.id.btnQuestionsDone);

        // listeners
        mBtnDone.setOnClickListener(this);
        mBtnResetAnswers.setOnClickListener(this);
        mBtnDeleteAll.setOnClickListener(this);
        mBtnUpdateQuestions.setOnClickListener(this);
        mPrgUpdateQuestions.setMax(mProgressMaxValue);
        ManagerFreebase.instance().addListener(this);

        ModelGameRound gameRoundInfo = ManagerGame.instance().getGameRoundInfo();
        // disable reset answers and delete all when in play
        if (gameRoundInfo != null && gameRoundInfo.active)
        {
            mBtnResetAnswers.setEnabled(false);
            mBtnDeleteAll.setEnabled(false);
        }

        // query db
        ModelSettings settings = ManagerDB.instance().getSettings();
        ModelQuestionBank bank = ManagerDB.instance().getQuestionBankStats();

        // question bank stats display
        mTxtStatsTotal.setText(Integer.toString(bank.total));
        mTxtStatsAnswered.setText(Integer.toString(bank.answered));
        //mTxtStatsUnanswered.setText(Integer.toString(bank.unanswered));
        mTxtStatsTopics.setText(Integer.toString(bank.topics));
        mTxtStatsCategories.setText(Integer.toString(bank.categories));

        // calculated max number of topics returned from update
        mProgressMaxValue = (bank.categories * settings.increment);
        mQuestionUpdateState = UpdateState.IDLE;

        // not supported yet
        //mTxtLocale.setText(getActivity().getResources().getConfiguration().locale.getCountry());

        ((ActivityMain)getActivity()).enableMenuItem(R.id.menu_questions, false);

        // show help
        if (settings.isFirstTimeHelp(ModelHelp.Help.QUESTIONS_DOWNLOAD))
        {
            ((ActivityMain)getActivity()).showHelp(ModelHelp.Help.QUESTIONS_DOWNLOAD, mBtnUpdateQuestions);

            // do not show this help again
            settings.setDoNotShowHelpAgain(ModelHelp.Help.QUESTIONS_DOWNLOAD);
            ManagerDB.instance().updateSettings(settings);
        }

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        ((ActivityMain)getActivity()).enableMenuItem(R.id.menu_questions, true);

        ManagerFreebase.instance().removeListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mPrgUpdateQuestions.setMax(mProgressMaxValue);

        switch(mQuestionUpdateState)
        {
            case IDLE:
            {
                mBtnUpdateQuestions.setText(Application2.instance().getResString(R.string.menu_questions_update));
                break;
            }
            case COMPLETE:
            {
                mBtnUpdateQuestions.setText(Application2.instance().getResString(R.string.menu_questions_complete));
                break;
            }
            case IN_PROGRESS:
            {
                mBtnUpdateQuestions.setText(Application2.instance().getResString(R.string.menu_questions_stop));
                break;
            }
            default:
            {
                break;
            }
        }
    }

    @Override
    public void onClick(View view)
    {
        if (view == mBtnDone)
        {
            ManagerMedia.instance().playSound(R.raw.sfx_button);

            ((ActivityMain)getActivity()).back();
        }
        else if (view == mBtnResetAnswers)
        {
            AlertDialog ad = new AlertDialog.Builder(getActivity()).create();
            ad.setIcon(android.R.drawable.ic_dialog_alert);
            ad.setTitle(Application2.instance().getResString(R.string.common_areyousure));
            ad.setMessage(Application2.instance().getResString(R.string.dialog_reset_questions));
            ad.setButton(
                DialogInterface.BUTTON_POSITIVE, Application2.instance().getResString(R.string.common_ok),
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();

                        ManagerDB.instance().resetAnswers();

                        Application2.toast(R.string.toast_reset_answers);

                        ModelQuestionBank stats = ManagerDB.instance().getQuestionBankStats();
                        mTxtStatsTotal.setText(Integer.toString(stats.total));
                        mTxtStatsAnswered.setText(Integer.toString(stats.answered));
                        //mTxtStatsUnanswered.setText(Integer.toString(stats.unanswered));
                        mTxtStatsTopics.setText(Integer.toString(stats.topics));
                        mTxtStatsCategories.setText(Integer.toString(stats.categories));
                    }
                }
            );
            ad.setButton(
                DialogInterface.BUTTON_NEGATIVE, Application2.instance().getResString(R.string.common_cancel),
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                }
            );
            ad.show();
        }
        else if (view == mBtnDeleteAll)
        {
            AlertDialog ad = new AlertDialog.Builder(getActivity()).create();
            ad.setIcon(android.R.drawable.ic_dialog_alert);
            ad.setTitle(Application2.instance().getResString(R.string.common_areyousure));
            ad.setMessage(Application2.instance().getResString(R.string.dialog_delete_questions));
            ad.setButton(
                    DialogInterface.BUTTON_POSITIVE, Application2.instance().getResString(R.string.common_ok),
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();

                            ManagerDB.instance().deleteAllQuestions();
                            ManagerMedia.instance().deleteAllTopicImages();

                            Application2.toast(R.string.toast_delete_all);

                            ModelQuestionBank stats = ManagerDB.instance().getQuestionBankStats();
                            mTxtStatsTotal.setText(Integer.toString(stats.total));
                            mTxtStatsAnswered.setText(Integer.toString(stats.answered));
                            //mTxtStatsUnanswered.setText(Integer.toString(stats.unanswered));
                            mTxtStatsTopics.setText(Integer.toString(stats.topics));
                            mTxtStatsCategories.setText(Integer.toString(stats.categories));
                        }
                    }
            );
            ad.setButton(
                    DialogInterface.BUTTON_NEGATIVE, Application2.instance().getResString(R.string.common_cancel),
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    }
            );
            ad.show();
        }
        else if (view == mBtnUpdateQuestions)
        {
            switch(mQuestionUpdateState)
            {
                case IDLE:
                {
                    FragmentActivity fa = getActivity();
                    if(fa != null)
                    {
                        // Test the connection, onUpdateTestConnectionComplete actually runs the QuestionTask
                        if(ManagerFreebase.instance().isOnline(fa))
                        {
                            ManagerFreebase.instance().hasActiveInternetConnection(fa);
                        }
                        else
                        {
                            // display dialog to say there's no connection?
                            AlertDialog ad = new AlertDialog.Builder(fa).create();
                            ad.setIcon(android.R.drawable.ic_dialog_alert);
                            ad.setTitle(Application2.instance().getResString(R.string.common_areyousure));
                            ad.setCancelable(false);
                            ad.setMessage(Application2.instance().getResString(R.string.dialog_no_connection));
                            ad.setButton(
                                DialogInterface.BUTTON_POSITIVE, Application2.instance().getResString(R.string.common_ok), new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        dialog.dismiss();
                                    }
                                }
                            );
                            ad.show();
                        }
                    }
                }
                case COMPLETE:
                {
                    // do nothing
                    break;
                }
                case IN_PROGRESS:
                {
                    ManagerFreebase.instance().stopQuestionRetrieval();
                    break;
                }
                default:
                {
                    break;
                }
            }
        }
    }

    @Override
    public void onUpdateQuestionsInitiated()
    {
        setNewQuestionState(R.string.menu_questions_stop, 0, UpdateState.IDLE);
    }

    @Override
    public void onUpdateQuestionsInProgress(int progress)
    {
        setNewQuestionState(
                R.string.menu_questions_stop,
                progress,
                UpdateState.IN_PROGRESS);

        if(progress % UPDATE_ON_INCREMENT == 0)
        {
            updateStatistics();
        }
    }

    @Override
    public void onUpdateQuestionsCancelled()
    {
        setNewQuestionState(
                R.string.menu_questions_update,
                mPrgUpdateQuestions.getProgress(),
                UpdateState.IDLE);
    }

    @Override
    public void onUpdateQuestionsComplete()
    {
        setNewQuestionState(
                R.string.menu_questions_complete,
                mProgressMaxValue,
                UpdateState.COMPLETE);

        mBtnUpdateQuestions.setEnabled(false);
    }

    @Override
    public void onUpdateTestConnectionComplete(final Boolean active)
    {
        // If we have an active connection check if it's wifi or not
        // and give the user prompt to continue or not
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(active)
                {
                    final FragmentActivity fa = getActivity();
                    if(fa != null)
                    {
                        if(ManagerFreebase.instance().isOnWiFi(fa))
                        {
                            ManagerFreebase.instance().createQuestionBank();
                        }
                        else
                        {
                            // display dialog to say there's no connection?
                            AlertDialog ad = new AlertDialog.Builder(fa).create();
                            ad.setIcon(android.R.drawable.ic_dialog_alert);
                            ad.setTitle(Application2.instance().getResString(R.string.common_areyousure));
                            ad.setCancelable(false);
                            ad.setMessage(Application2.instance().getResString(R.string.dialog_question_bank));
                            ad.setButton(
                                    DialogInterface.BUTTON_POSITIVE, Application2.instance().getResString(R.string.common_confirm), new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            ManagerFreebase.instance().createQuestionBank();
                                        }
                                    }
                            );
                            ad.setButton(
                                    DialogInterface.BUTTON_NEGATIVE, Application2.instance().getResString(R.string.common_cancel), new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            dialog.dismiss();
                                        }
                                    }
                            );
                            ad.show();
                        }
                    }
                }
            }
        });
    }

    private void setNewQuestionState(final int res, final int progress, final UpdateState newState)
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mBtnUpdateQuestions.setText(Application2.instance().getResString(res));
                mPrgUpdateQuestions.setProgress(progress);
                mQuestionUpdateState = newState;
            }
        });
        }

    private void updateStatistics()
    {
        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ModelQuestionBank stats = ManagerDB.instance().getQuestionBankStats();
                mTxtStatsTotal.setText(Integer.toString(stats.total));
                mTxtStatsAnswered.setText(Integer.toString(stats.answered));
                //mTxtStatsUnanswered.setText(Integer.toString(stats.unanswered));
                mTxtStatsTopics.setText(Integer.toString(stats.topics));
                mTxtStatsCategories.setText(Integer.toString(stats.categories));
            }
        });

    }
}
