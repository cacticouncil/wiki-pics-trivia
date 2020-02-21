package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class ViewGameMode extends LinearLayout
{
    private Context             mContext;
    private ModelGameMode       mGameMode;
    private TextView            mName;
    //private ViewGameModeOption  mGMOCategories;
    private ViewGameModeOption  mGMOQuestions;
    private ViewGameModeOption  mGMOTimer;
    private ViewGameModeOption  mGMOMisses;
    private ViewGameModeOption  mGMOBrowsable;
    //private ViewGameModeOption  mGMOHints;
    private ViewGameModeOption  mGMOLinks;
    private ImageView           mIVArrow;

    public ViewGameMode(Context context)
    {
        super(context);
        initialize(context);
    }

    public ViewGameMode(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public ViewGameMode(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public void initialize(Context context)
    {
        // ensure hardware acceleration
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mContext = context;

        inflate(context, R.layout.view_gamemode, this);

        mName          = (TextView)findViewById(R.id.tvGameModeName);
        mGMOQuestions  = (ViewGameModeOption)findViewById(R.id.vGMOQuestions);
        mGMOTimer      = (ViewGameModeOption)findViewById(R.id.vGMOTimer);
        mGMOMisses     = (ViewGameModeOption)findViewById(R.id.vGMOMisses);
        mGMOBrowsable  = (ViewGameModeOption)findViewById(R.id.vGMOBrowsable);
        mGMOLinks      = (ViewGameModeOption)findViewById(R.id.vGMOLinks);
        mIVArrow       = (ImageView)findViewById(R.id.ivGameModeArrow);
    }

    public void setGameMode(ModelGameMode model)
    {
        mGameMode = model;

        // Set the name
        mName.setText(mGameMode.name);

        // Zero out numbers
        mGMOQuestions.setText("");
        mGMOTimer.setText("");
        mGMOMisses.setText("");

        // Enable all icons
        mGMOQuestions.setEnabled(true);
        mGMOTimer.setEnabled(true);
        mGMOMisses.setEnabled(true);
        mGMOBrowsable.setEnabled(true);
        mGMOLinks.setEnabled(true);

        //mGMOCategories.setText(Character.toString(model.categories.toString().charAt(0)));

        if (model.questions > 0)
            mGMOQuestions.setText(Integer.toString(model.questions));
        else
            mGMOQuestions.setEnabled(false);

        if (model.timer > 0)
            mGMOTimer.setText(Integer.toString(model.timer));
        else
            mGMOTimer.setEnabled(false);

        if (model.misses > 0)
            mGMOMisses.setText(Integer.toString(model.misses));
        else
            mGMOMisses.setEnabled(false);

        if (!model.isBrowsable)
            mGMOBrowsable.setEnabled(false);

        if (!model.hasLinks)
            mGMOLinks.setEnabled(false);
    }

    public ModelGameMode getGameMode()
    {
        return mGameMode;
    }

    public void enableArrow(boolean b)
    {
        if (b) mIVArrow.setVisibility(VISIBLE);
        else   mIVArrow.setVisibility(INVISIBLE);
    }
}
