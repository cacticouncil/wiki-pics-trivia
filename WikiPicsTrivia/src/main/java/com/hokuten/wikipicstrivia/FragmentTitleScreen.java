package com.hokuten.wikipicstrivia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class FragmentTitleScreen extends Fragment implements View.OnClickListener
{
    private Button mBtnPlay;

    public FragmentTitleScreen()
    {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_titlescreen, container, false);

        mBtnPlay = (Button)view.findViewById(R.id.btnPlay);
        mBtnPlay.setOnClickListener(this);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
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
        ManagerMedia.instance().playSound(R.raw.sfx_button);

        if (view == mBtnPlay)
        {
            ((ActivityMain)getActivity()).show(R.layout.fragment_gamemodes);
        }
    }
}
