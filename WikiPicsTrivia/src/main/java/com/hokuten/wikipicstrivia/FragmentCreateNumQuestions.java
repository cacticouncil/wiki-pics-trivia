package com.hokuten.wikipicstrivia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;


public class FragmentCreateNumQuestions extends Fragment implements View.OnClickListener, NumberPicker.OnValueChangeListener,  FragmentCreateMode.Updater
{
    private NumberPicker mPckrNumQuestions;
    private ModelGameMode mGameMode;

    public static final FragmentCreateNumQuestions newInstance(ModelGameMode g)
    {
        FragmentCreateNumQuestions p = new FragmentCreateNumQuestions();
        Bundle args = new Bundle();
        args.putParcelable("gamemode", g);
        p.setArguments(args);
        return p;
    }

    public FragmentCreateNumQuestions()
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
        View view = inflater.inflate(R.layout.fragment_createnumquestions, container, false);

        // views
        mPckrNumQuestions = (NumberPicker)view.findViewById(R.id.pckrNumQuestions);
        mPckrNumQuestions.setMinValue(0);
        mPckrNumQuestions.setMaxValue(99);
        mPckrNumQuestions.setValue(mGameMode.questions);

        // listeners
        mPckrNumQuestions.setOnClickListener(this);
        mPckrNumQuestions.setOnValueChangedListener(this);

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
//        if (view == mPckrNumQuestions)
//        {
//
//        }
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal)
    {
        mGameMode.questions = newVal;
    }

    @Override
    public void update(ModelGameMode gameMode)
    {

    }
}
