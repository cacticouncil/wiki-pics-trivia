package com.hokuten.wikipicstrivia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;


public class FragmentCreateTimer extends Fragment implements View.OnClickListener, NumberPicker.OnValueChangeListener, FragmentCreateMode.Updater
{
    private static final int mSecondsPerMinute = 60;

    private NumberPicker  mPckrTimer;
    private ModelGameMode mGameMode;
    private TextView      mTxtTimePerQuestion;

    public static final FragmentCreateTimer newInstance(ModelGameMode g)
    {
        FragmentCreateTimer p = new FragmentCreateTimer();
        Bundle args = new Bundle();
        args.putParcelable("gamemode", g);
        p.setArguments(args);
        return p;
    }

    public FragmentCreateTimer()
    {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            if (getArguments().getParcelable("gamemode") != null)
                mGameMode = (ModelGameMode)getArguments().getParcelable("gamemode");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_createtimer, container, false);

        // views
        mPckrTimer = (NumberPicker)view.findViewById(R.id.pckrTimer);
        mPckrTimer.setMinValue(0);
        mPckrTimer.setMaxValue(99);
        mPckrTimer.setValue(mGameMode.timer);

        mTxtTimePerQuestion = (TextView)view.findViewById(R.id.tvTimePerQuestion);

        if (mGameMode.questions > 0 && mGameMode.timer > 0)
            mTxtTimePerQuestion.setText(String.format(Application2.instance().getResString(R.string.createmode_timer_helper),
                                                      String.format("%.2f", (float)(mGameMode.timer * mSecondsPerMinute) / mGameMode.questions),
                                                      Integer.toString(mGameMode.questions)));
        else
            mTxtTimePerQuestion.setText("");

        // listeners
        mPckrTimer.setOnClickListener(this);
        mPckrTimer.setOnValueChangedListener(this);

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
    }

    @Override
    public void onClick(View view)
    {
//        ModelSettings settings = ManagerDB.instance().getSettings();
//
//        if (settings.sfx)
//            ManagerMedia.instance().playSound(R.raw.sfx_button);
//
//        if (view == mPckrTimer)
//        {
//
//        }
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal)
    {
        mGameMode.timer = newVal;

        if (mGameMode.questions > 0 && newVal > 0)
            mTxtTimePerQuestion.setText(String.format(Application2.instance().getResString(R.string.createmode_timer_helper),
                                                      String.format("%.2f", (float)(newVal * mSecondsPerMinute) / mGameMode.questions),
                                                      Integer.toString(mGameMode.questions)));
        else
            mTxtTimePerQuestion.setText("");
    }

    @Override
    public void update(ModelGameMode gameMode)
    {
        if (mTxtTimePerQuestion != null)
        {
            if (mGameMode.questions > 0 && mGameMode.timer > 0)
                mTxtTimePerQuestion.setText(String.format(Application2.instance().getResString(R.string.createmode_timer_helper),
                                                          String.format("%.2f", (float)(mGameMode.timer * mSecondsPerMinute) / mGameMode.questions),
                                                          Integer.toString(mGameMode.questions)));
            else
                mTxtTimePerQuestion.setText("");
        }
    }
}
