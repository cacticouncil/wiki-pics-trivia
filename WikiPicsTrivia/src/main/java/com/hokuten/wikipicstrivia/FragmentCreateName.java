package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;


public class FragmentCreateName extends Fragment implements View.OnKeyListener, TextWatcher, FragmentCreateMode.Updater
{
    private EditText      mEdtTxtName;
    private ModelGameMode mGameMode;

    public static final FragmentCreateName newInstance(ModelGameMode g)
    {
        FragmentCreateName p = new FragmentCreateName();
        Bundle args = new Bundle();
        args.putParcelable("gamemode", g);
        p.setArguments(args);
        return p;
    }

    public FragmentCreateName()
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
        View view = inflater.inflate(R.layout.fragment_createname, container, false);

        // views
        mEdtTxtName = (EditText)view.findViewById(R.id.eTxtName);

        if (!mGameMode.name.isEmpty())
        {
            mEdtTxtName.setText(mGameMode.name);
        }

        // listeners
        mEdtTxtName.addTextChangedListener(this);
        mEdtTxtName.setOnKeyListener(this);

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
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {

    }

    public void afterTextChanged(Editable editable)
    {
        // Make sure whatever is typed is set, they don't have to hit enter
        mGameMode.name = editable.toString();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
        // If the event is a key-down event on the "enter" button
        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                (keyCode == KeyEvent.KEYCODE_ENTER)) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mEdtTxtName.getWindowToken(), 0);
                }
            }, 300);
            return true;
        }
        return false;
    }

    @Override
    public void update(ModelGameMode gameMode)
    {

    }
}
