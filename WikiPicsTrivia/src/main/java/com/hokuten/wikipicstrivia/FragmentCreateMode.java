package com.hokuten.wikipicstrivia;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class FragmentCreateMode extends Fragment implements ViewPager2.OnPageChangeListener
{
    public interface Updater
    {
        public void update(ModelGameMode gameMode);
    }

    private ModelGameMode     mGameMode;
    private ViewPager2        mPager;
    private AdapterCreateMode mAdapter;
    private PagerTabStrip     mPagerTab;

    public FragmentCreateMode()
    {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null)
            mGameMode = args.getParcelable("gamemode");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_createmode, container, false);

        if (mGameMode != null)
            mAdapter = new AdapterCreateMode(getChildFragmentManager(), mGameMode);
        else
            mAdapter = new AdapterCreateMode(getChildFragmentManager());

        mPager = (ViewPager2) view.findViewById(R.id.pagerCreateMode);
        mPager.storeAdapter(mAdapter);
        mPager.setOnPageChangeListener(this);
        //mPager.setPageTransformer(true, new PageTransformerZoomOut());

        mPagerTab = (PagerTabStrip) view.findViewById(R.id.pagerTabStrip);
        mPagerTab.setTabIndicatorColor(Color.GRAY);

        // show help
        ModelSettings settings = ManagerDB.instance().getSettings();
        if (settings.isFirstTimeHelp(ModelHelp.Help.CREATE_MODE_SWIPE))
        {
            ((ActivityMain)getActivity()).showHelp(ModelHelp.Help.CREATE_MODE_SWIPE, mPagerTab);

            // do not show this help again
            settings.setDoNotShowHelpAgain(ModelHelp.Help.CREATE_MODE_SWIPE);
            ManagerDB.instance().updateSettings(settings);
        }

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

    public void nextPage()
    {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1);
    }

    public void previousPage()
    {
        mPager.setCurrentItem(mPager.getCurrentItem() - 1);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {

    }

    @Override
    public void onPageSelected(int position)
    {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {

    }
}
