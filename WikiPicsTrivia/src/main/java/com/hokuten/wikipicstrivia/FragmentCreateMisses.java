package com.hokuten.wikipicstrivia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;


public class FragmentCreateMisses extends Fragment implements View.OnClickListener, NumberPicker.OnValueChangeListener, FragmentCreateMode.Updater
{
    private NumberPicker mPckrMisses;
    private ModelGameMode mGameMode;

    public static final FragmentCreateMisses newInstance(ModelGameMode g)
    {
        FragmentCreateMisses p = new FragmentCreateMisses();
        Bundle args = new Bundle();
        args.putParcelable("gamemode", g);
        p.setArguments(args);
        return p;
    }

    public FragmentCreateMisses()
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
        View view = inflater.inflate(R.layout.fragment_createmisses, container, false);

        // views
        mPckrMisses = (NumberPicker)view.findViewById(R.id.pckrMisses);
        mPckrMisses.setMinValue(0);
        mPckrMisses.setMaxValue(9);
        mPckrMisses.setValue(mGameMode.misses);

        // listeners
        mPckrMisses.setOnClickListener(this);
        mPckrMisses.setOnValueChangedListener(this);

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
//        if (view == mPckrMisses)
//        {
//
//        }
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal)
    {
        mGameMode.misses = newVal;
    }

    @Override
    public void update(ModelGameMode gameMode)
    {

    }
}
