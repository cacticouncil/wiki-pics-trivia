package com.hokuten.wikipicstrivia;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FragmentPlayPage extends Fragment implements View.OnClickListener
{
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(2);

    private ModelGameRound  mGameRound;
    private ModelQuestion   mQuestion;
    private TextView        mTxtIndex;
    private ImageView       mImgTimer;
    private TextView        mTxtTimer;
    private ImageView       mImgMisses;
    private TextView        mTxtMisses;
    private ImageView       mImgBrowsable;
    private ImageView       mImgImage;
    private ImageView       mImgLink;
    private ImageView       mImgLicenseInfo;
    private TextView        mTxtQuestion;
    private Button[]        mBtnAnswers;
    private Future<?>       mUpdateThread;
    private ImageView       mImgQuestionResult;
    private Animation       mAnimation;
    private LinearLayout    mBackground;
    //private LinearLayout    mThemeBar;
    //private TextView        mTxtTheme;

    public static final FragmentPlayPage newInstance(ModelQuestion q)
    {
        FragmentPlayPage p = new FragmentPlayPage();
        Bundle args = new Bundle();
        args.putParcelable("question", q);
        p.setArguments(args);
        return p;
    }

    public FragmentPlayPage()
    {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // question passed in as argument
        if (getArguments() != null)
            if (getArguments().getParcelable("question") != null)
                mQuestion = (ModelQuestion)getArguments().getParcelable("question");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_playpage, container, false);

        // init
        ModelSettings settings = ManagerDB.instance().getSettings();
        mBtnAnswers = new Button[4];
        mAnimation  = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fade_scale_repeat);
        mGameRound  = ManagerGame.instance().getGameRoundInfo();

        // views
        mTxtIndex          = (TextView)view.findViewById(R.id.tvQuestionIndex);
        mImgTimer          = (ImageView)view.findViewById(R.id.ivTimeLeft);
        mTxtTimer          = (TextView)view.findViewById(R.id.tvTimeLeft);
        mImgMisses         = (ImageView)view.findViewById(R.id.ivMisses);
        mTxtMisses         = (TextView)view.findViewById(R.id.tvMisses);
        mImgBrowsable      = (ImageView)view.findViewById(R.id.ivBrowsable);
        mImgImage          = (ImageView)view.findViewById(R.id.ivQuestionImage);
        mImgLink           = (ImageView)view.findViewById(R.id.ivQuestionLink);
        mImgLicenseInfo    = (ImageView)view.findViewById(R.id.ivImageLicenseInfo);
        mTxtQuestion       = (TextView)view.findViewById(R.id.tvQuestion);
        mBtnAnswers[0]     = (Button)view.findViewById(R.id.btnAnswer0);
        mBtnAnswers[1]     = (Button)view.findViewById(R.id.btnAnswer1);
        mBtnAnswers[2]     = (Button)view.findViewById(R.id.btnAnswer2);
        mBtnAnswers[3]     = (Button)view.findViewById(R.id.btnAnswer3);
        mImgQuestionResult = (ImageView) view.findViewById(R.id.ivQuestionResult);
        mBackground        = (LinearLayout)view.findViewById(R.id.llBackground);
        //mThemeBar          = (LinearLayout)view.findViewById(R.id.llOptionBar);
        //mTxtTheme          = (TextView)view.findViewById(R.id.tvTheme);
        mImgLicenseInfo.setOnClickListener(this);

        // show help (only on the first question)
        if (mQuestion != null && mQuestion.index == 0)
        {
            if (settings.isFirstTimeHelp(ModelHelp.Help.BROWSABLE) && mGameRound.mode.isBrowsable)
            {
                ((ActivityMain)getActivity()).showHelp(ModelHelp.Help.BROWSABLE, mImgBrowsable);

                // do not show this help again
                settings.setDoNotShowHelpAgain(ModelHelp.Help.BROWSABLE);
                ManagerDB.instance().updateSettings(settings);
            }
            else if (settings.isFirstTimeHelp(ModelHelp.Help.HASLINKS) && mGameRound.mode.hasLinks)
            {
                ((ActivityMain)getActivity()).showHelp(ModelHelp.Help.HASLINKS, mImgLink);

                // do not show this help again
                settings.setDoNotShowHelpAgain(ModelHelp.Help.HASLINKS);
                ManagerDB.instance().updateSettings(settings);
            }
            else if (settings.isFirstTimeHelp(ModelHelp.Help.MISSES) && mGameRound.mode.misses > 0)
            {
                ((ActivityMain)getActivity()).showHelp(ModelHelp.Help.MISSES, mImgMisses);

                // do not show this help again
                settings.setDoNotShowHelpAgain(ModelHelp.Help.MISSES);
                ManagerDB.instance().updateSettings(settings);
            }
        }

        // gamemode specific initialization
        if (mGameRound.mode != null)
        {
            if (mGameRound.mode.questions > 0)
                mTxtIndex.setText((mQuestion.index+1) + "/" + Integer.toString(mGameRound.mode.questions));
            else
                mTxtIndex.setText((mQuestion.index+1) + "/~");

            if (mGameRound.mode.timer <= 0)
            {
                mImgTimer.setVisibility(view.INVISIBLE);
                mTxtTimer.setVisibility(View.INVISIBLE);
            }

            if (mGameRound.mode.misses > 0)
            {
                mTxtMisses.setText(Integer.toString(mGameRound.mode.misses - mGameRound.missed));
            }
            else
            {
                mImgMisses.setVisibility(View.INVISIBLE);
                mTxtMisses.setVisibility(View.INVISIBLE);
            }

            if (!mGameRound.mode.isBrowsable)
            {
                mImgBrowsable.setVisibility(View.INVISIBLE);
            }

            if (mGameRound.mode.hasLinks)
            {
                mImgLink.setOnClickListener(this);
            }
            else
            {
                mImgLink.setVisibility(View.INVISIBLE);
            }
        }

        // question specific initialization
        if (mQuestion != null && !mQuestion.mid.isEmpty())
        {
            // set gradients
            GradientDrawable bgGradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] { mQuestion.color, 0xff1f1f1f, 0xff000000 });
            bgGradient.setCornerRadius(20f);
            bgGradient.setStroke(2, Color.GRAY);
            //GradientDrawable thmGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] { mQuestion.color, 0xFF000000, 0xFF000000 });
            //thmGradient.setCornerRadius(10f);
            if (Build.VERSION.SDK_INT < 16)
            {
                mBackground.setBackgroundDrawable(bgGradient);
                //mThemeBar.setBackgroundDrawable(thmGradient);
            }
            else
            {
                mBackground.setBackground(bgGradient);
                //mThemeBar.setBackground(thmGradient);
            }

            // set theme text
            //mTxtTheme.setText(mQuestion.theme);

            // question text
            mTxtQuestion.setText(mQuestion.question);

            // generate a PRNG seed, so question answers don't shift when page reloads
            if (mQuestion.seed <= 0) mQuestion.seed = System.nanoTime();

            // assign answer to random button
            int btnIdx = Application2.randIntSeed(mQuestion.seed, 0,3);
            mBtnAnswers[btnIdx].setText(mQuestion.answer);
            // assign fake answers to the rest
            int btnIdxFake = Application2.rotateIndex(btnIdx, 0,3, true);
            mBtnAnswers[btnIdxFake].setText(mQuestion.fakes[0]);
            btnIdxFake = Application2.rotateIndex(btnIdxFake, 0,3, true);
            mBtnAnswers[btnIdxFake].setText(mQuestion.fakes[1]);
            btnIdxFake = Application2.rotateIndex(btnIdxFake, 0,3, true);
            mBtnAnswers[btnIdxFake].setText(mQuestion.fakes[2]);

            mQuestion.idxCorrect = btnIdx;

            // if already answered,
            if (mQuestion.isAnswered())
            {
                // show result
                showAnswerResult(settings);

                // disable buttons (possible due to paging back n forth)
                for (int i=0; i<mBtnAnswers.length; i++)
                {
                    mBtnAnswers[i].setTextIsSelectable(false);
                    mBtnAnswers[i].setEnabled(false);
                    mBtnAnswers[i].setActivated(false);
                    mBtnAnswers[i].setClickable(false);
                }
            }
            else
            {
                // enable listeners
                for (int i=0; i<mBtnAnswers.length; i++)
                    mBtnAnswers[i].setOnClickListener(this);
            }
        }

        return view;
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // cancel update thread
        if (mUpdateThread != null)
        {
            mUpdateThread.cancel(false);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // resuming, but the game round has already ended!
        if (!mGameRound.active)
        {
            ((ActivityMain)getActivity()).show(R.layout.fragment_roundover);
            return;
        }

        // since there is no question, the round should end immediately!
        if (mQuestion == null || mQuestion.mid.isEmpty())
        {
            ManagerGame.instance().endRound();
            return;
        }

        // load question image dynamically
        mScheduler.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Bitmap b = ManagerMedia.instance().getTopicImage(mQuestion.mid);
                        if (b != null) mImgImage.setImageBitmap(b);
                    }
                });
            }
        }, 0, TimeUnit.SECONDS);

        // update any animations
        mUpdateThread = mScheduler.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mTxtTimer.setText(ManagerGame.instance().getRemainingTime());

                        if (mGameRound.mode.misses > 0)
                            mTxtMisses.setText(Integer.toString(mGameRound.mode.misses - mGameRound.missed));
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onClick(View view)
    {
        if (view instanceof Button)
        {
            Button b      = (Button)view;
            String answer = b.getText().toString();

            // ignore click event
            if (mQuestion.isAnswered()) return;  // if question already answered
            if (answer.isEmpty()) return;        // if answer is empty

            ModelSettings settings = ManagerDB.instance().getSettings();

            // which button did user select?
            for (int i=0; i<mBtnAnswers.length; i++)
            {
                if (mBtnAnswers[i] == b)
                {
                    mQuestion.idxSelected = i;
                    break;
                }
            }

            // correct
            if (answer.compareToIgnoreCase(mQuestion.answer) == 0)
            {
                mQuestion.state = ModelQuestion.State.CORRECT;
                mGameRound.correct++;

                ManagerMedia.instance().playSound(R.raw.sfx_correct);
                showAnswerResult(settings);
            }
            // incorrect
            else
            {
                mQuestion.state = ModelQuestion.State.INCORRECT;
                mGameRound.missed++;

                ManagerMedia.instance().playSound(R.raw.sfx_incorrect);
                showAnswerResult(settings);
            }

            // release buttons
            for (int i=0; i<mBtnAnswers.length; i++)
            {
                mBtnAnswers[i].setOnClickListener(null);
                mBtnAnswers[i].setTextIsSelectable(false);
                mBtnAnswers[i].setEnabled(false);
                mBtnAnswers[i].setActivated(false);
                mBtnAnswers[i].setClickable(false);
            }

            // end round if player is awesome :)
            if (ManagerGame.instance().isRoundComplete())
            {
                ManagerGame.instance().endRound();
                return;
            }

            // go to next page
            if (!isLastQuestion())
            {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        //mImgQuestionResult.setVisibility(View.GONE);
                        ((FragmentPlayScreen)getParentFragment()).nextPage();
                    }
                }, 500);
            }
        }
        else if (view == mImgLink)
        {
            if (mGameRound.mode != null && mGameRound.mode.hasLinks)
            {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse(String.format(Application2.instance().getResString(R.string.url_wikipage), mQuestion.wid)));
                browseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                getActivity().startActivity(browseIntent);
            }
        }
        else if (view == mImgLicenseInfo)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            builder.setTitle(Application2.instance().getResString(R.string.image_info));

            View licenseView = inflater.inflate(R.layout.view_license, null);
            TextView author =  (TextView) licenseView.findViewById(R.id.imageAuthor);
            TextView license =  (TextView) licenseView.findViewById(R.id.imageLicense);

            if (mQuestion.author != null)
            {
                author.setText(mQuestion.author);
            }

            if (mQuestion.license != null)
            {
                String licenseLinkStr = Application2.getLicenseLink(mQuestion.license);
                if (licenseLinkStr != null)
                {
                    license.setText(Html.fromHtml("<a href=\"" + licenseLinkStr + "\">" + mQuestion.license + "</a>"));
                    license.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    license.setText(mQuestion.license);
                }
            }

            builder.setView(licenseView);
            builder.setPositiveButton(Application2.instance().getResString(R.string.common_ok), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.show();
        }
    }

    private boolean isLastQuestion()
    {
        return ((mGameRound.mode.questions > 0) && (mQuestion.index == mGameRound.mode.questions-1));
    }

    private void showAnswerResult(ModelSettings settings)
    {
        if (mQuestion.isCorrect())
        {
            mImgQuestionResult.setImageResource(R.drawable.correct);
            mImgQuestionResult.setVisibility(View.VISIBLE);
            mImgQuestionResult.startAnimation(mAnimation);

            if (Build.VERSION.SDK_INT < 16)
                mBtnAnswers[mQuestion.idxSelected].setBackgroundDrawable(getResources().getDrawable(R.drawable.answer_button_correct));
            else
                mBtnAnswers[mQuestion.idxSelected].setBackground(getResources().getDrawable(R.drawable.answer_button_correct));
        }
        else
        {
            mImgQuestionResult.setImageResource(R.drawable.wrong);
            mImgQuestionResult.setVisibility(View.VISIBLE);
            mImgQuestionResult.startAnimation(mAnimation);

            if (Build.VERSION.SDK_INT < 16)
                mBtnAnswers[mQuestion.idxSelected].setBackgroundDrawable(getResources().getDrawable(R.drawable.answer_button_wrong));
            else
                mBtnAnswers[mQuestion.idxSelected].setBackground(getResources().getDrawable(R.drawable.answer_button_wrong));

            if (settings.showAnswer)
            {
                if (Build.VERSION.SDK_INT < 16)
                    mBtnAnswers[mQuestion.idxCorrect].setBackgroundDrawable(getResources().getDrawable(R.drawable.answer_button_correct));
                else
                    mBtnAnswers[mQuestion.idxCorrect].setBackground(getResources().getDrawable(R.drawable.answer_button_correct));
            }
        }
    }
}
