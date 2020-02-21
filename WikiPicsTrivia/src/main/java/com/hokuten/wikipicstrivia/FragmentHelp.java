package com.hokuten.wikipicstrivia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class FragmentHelp extends Fragment implements View.OnClickListener
{
    private ViewHelp mHelp;

    public FragmentHelp()
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
        View view = null; //inflater.inflate(R.layout.fragment_help, container, false);

        // handle arguments passed in
        ModelHelp help = null;
        if (getArguments() != null)
            if (getArguments().getParcelable("help") != null)
                help = getArguments().getParcelable("help");

        mHelp = (ViewHelp)view.findViewById(R.id.vHelp);
        mHelp.setOnClickListener(this);
        mHelp.set(help);

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
        // close this screen
        ((ActivityMain)getActivity()).back();
    }
}
