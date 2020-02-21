package com.hokuten.wikipicstrivia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FragmentQotd extends Fragment implements View.OnClickListener //, TextWatcher
{
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(2);

    private ModelGameRound mGameRound;
    private ModelQuestion  mQuestion;
    private ImageView      mImgImage;
    private ImageView      mImgLink;
    private TextView       mTxtQuestion;
    private Button[]       mBtnAnswers;
    private ImageView      mImgQuestionResult;
    private Animation      mAnimation;
    private LinearLayout   mBackground;

    public FragmentQotd()
    {
        super();
        mQuestion = ManagerGame.instance().newQuestion();
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
        View view = inflater.inflate(R.layout.fragment_qotd, container, false);

        ModelSettings settings = ManagerDB.instance().getSettings();
        mBtnAnswers = new Button[4];

        mImgImage = (ImageView)view.findViewById(R.id.ivQuestionImage);
        mImgLink = (ImageView)view.findViewById(R.id.ivQuestionLink);
        mTxtQuestion       = (TextView)view.findViewById(R.id.tvQuestion);
        mBtnAnswers[0]     = (Button)view.findViewById(R.id.btnAnswer0);
        mBtnAnswers[1]     = (Button)view.findViewById(R.id.btnAnswer1);
        mBtnAnswers[2]     = (Button)view.findViewById(R.id.btnAnswer2);
        mBtnAnswers[3]     = (Button)view.findViewById(R.id.btnAnswer3);
        mImgQuestionResult = (ImageView) view.findViewById(R.id.ivQuestionResult);
        mAnimation         = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fade_scale_repeat);
        mBackground        = (LinearLayout)view.findViewById(R.id.llBackground);

        // get current round information
        mGameRound = ManagerGame.instance().getGameRoundInfo();

        // gamemode specific initialization
        if (mGameRound.mode != null)
        {
            if (mGameRound.mode.hasLinks)
                mImgLink.setOnClickListener(this);
            else
                mImgLink.setVisibility(View.INVISIBLE);
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
                    if (i != mQuestion.idxSelected)
                        mBtnAnswers[i].setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
                }
            }
            else
            {
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
    }

    @Override
    public void onResume()
    {
        super.onResume();

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
