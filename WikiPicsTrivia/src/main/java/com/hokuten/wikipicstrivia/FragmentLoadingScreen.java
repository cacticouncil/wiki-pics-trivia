package com.hokuten.wikipicstrivia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class FragmentLoadingScreen extends Fragment implements View.OnClickListener
{
    private String   mMessage;
    private TextView mTxtMessage;

    public FragmentLoadingScreen()
    {
        super();

        mMessage = "";
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // handle arguments passed in
        if (getArguments() != null)
            if (getArguments().getString("message") != null)
                mMessage = getArguments().getString("message");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_loadingscreen, container, false);

        mTxtMessage = (TextView)view.findViewById(R.id.txtMessage);

        mTxtMessage.setText(mMessage);

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
        // do nothing for now
    }
}
