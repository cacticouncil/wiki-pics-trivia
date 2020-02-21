package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class ViewQuestion extends LinearLayout
{
    private Context             mContext;
    private TextView            mTxtNumber;
    private ImageView           mImgImage;
    private TextView            mTxtQuestion;
    private TextView            mTxtAnswer;
    private ModelQuestion       mQuestionModel;
    private ImageButton         mBtnWikipedia;
    private ImageButton         mBtnShare;
    private ImageView           mGuessImage;

    public ViewQuestion(Context context)
    {
        super(context);
        initialize(context);
    }

    public ViewQuestion(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public ViewQuestion(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public void initialize(Context context)
    {
        // ensure hardware acceleration for this view
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mContext = context;

        inflate(context, R.layout.view_question, this);

        mTxtNumber    = (TextView)findViewById(R.id.tvQuestionNumber);
        mImgImage     = (ImageView)findViewById(R.id.ivQuestionImage);
        mGuessImage   = (ImageView)findViewById(R.id.ivQuestionCorrectness);
        mTxtQuestion  = (TextView)findViewById(R.id.tvQuestionQuestion);
        mTxtAnswer    = (TextView)findViewById(R.id.tvQuestionAnswer);
        mBtnShare     = (ImageButton)findViewById(R.id.btnShare);
        mBtnWikipedia = (ImageButton)findViewById(R.id.btnWikipedia);
    }

    public void setQuestion(ModelQuestion model)
    {
        mQuestionModel = model;

        if(model != null)
        {
            mTxtNumber.setText(Integer.toString(model.index+1));

            if(model.question != null)
                mTxtQuestion.setText(model.question);

            if(model.answer != null)
                mTxtAnswer.setText(model.answer);

            // set question image, but sub-sample since a thumbnail is displayed
            mImgImage.setImageBitmap(ManagerMedia.instance().getTopicImage(model.mid, FragmentRoundOver.QUESTION_IMG_SIZE));

            mBtnShare.setVisibility(View.INVISIBLE);
            mBtnWikipedia.setVisibility(View.INVISIBLE);

            if(model.isCorrect())
            {
                mGuessImage.setImageResource(R.drawable.ic_correct);
            }
            else
            {
                mGuessImage.setImageResource(R.drawable.ic_wrong);
            }
        }

        setBackgroundColor(Color.TRANSPARENT);
    }

    public ModelQuestion getQuestion()
    {
        return mQuestionModel;
    }

    public ImageButton getShareButton()
    {
        return mBtnShare;
    }

    public ImageButton getWikipediaButton()
    {
        return mBtnWikipedia;
    }

}
