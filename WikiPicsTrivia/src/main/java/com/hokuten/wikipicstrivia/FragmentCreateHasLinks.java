package com.hokuten.wikipicstrivia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;


public class FragmentCreateHasLinks extends Fragment implements View.OnClickListener, FragmentCreateMode.Updater
{
    private ToggleButton  mTglBtnHasLinks;
    private ModelGameMode mGameMode;

    public static final FragmentCreateHasLinks newInstance(ModelGameMode g)
    {
        FragmentCreateHasLinks p = new FragmentCreateHasLinks();
        Bundle args = new Bundle();
        args.putParcelable("gamemode", g);
        p.setArguments(args);
        return p;
    }

    public FragmentCreateHasLinks()
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
        View view = inflater.inflate(R.layout.fragment_createhaslinks, container, false);

        // views
        mTglBtnHasLinks = (ToggleButton)view.findViewById(R.id.tglBtnHasLinks);
        mTglBtnHasLinks.setChecked(mGameMode.hasLinks);

        // listeners
        mTglBtnHasLinks.setOnClickListener(this);

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

        if (view == mTglBtnHasLinks)
        {
            mGameMode.hasLinks = ((ToggleButton)view).isChecked();
        }
    }

    @Override
    public void update(ModelGameMode gameMode)
    {

    }
}
