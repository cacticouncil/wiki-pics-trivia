package com.hokuten.wikipicstrivia;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class FragmentPlayScreen extends Fragment implements ViewPager.OnPageChangeListener
{
    private static final int mPageMargin = -100;
    private static final int mOffScreenLimit = 2;

    private ViewPager2       mPager;
    private AdapterPlayPages mAdapter;

    public FragmentPlayScreen()
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
        View view = inflater.inflate(R.layout.fragment_playscreen, container, false);

        // we want the background to have a repeating tileMode
        Application2.fixRepeatingBackground(view);

        mAdapter = new AdapterPlayPages(getChildFragmentManager());
        mPager = (ViewPager2) view.findViewById(R.id.pagerPlayScreen);
        mPager.storeAdapter(mAdapter);
        mPager.setOnPageChangeListener(this);
        mPager.setPageTransformer(true, new PageTransformerZoomOut());
        //mPager.setCurrentItem(0);
        //mPager.removeAllViews();

        // handle non-browsable modes
        mPager.setPagingEnabled(ManagerGame.instance().getGameRoundInfo().mode.isBrowsable);
        mPager.setPageMargin(mPageMargin);
        mPager.setOffscreenPageLimit(mOffScreenLimit);

        ((ActivityMain)getActivity()).showMenuItem(R.id.menu_endround, true);
        //((ActivityMain)getActivity()).showMenuItem(R.id.menu_flag_question, true);

        ManagerMedia.instance().playBGM(R.raw.bgm_game);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        ((ActivityMain)getActivity()).showMenuItem(R.id.menu_endround, false);
        //((ActivityMain)getActivity()).showMenuItem(R.id.menu_flag_question, false);
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
    public void onDestroy()
    {
        super.onDestroy();
    }

    public void nextPage()
    {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1);
    }

    public void previousPage()
    {
        mPager.setCurrentItem(mPager.getCurrentItem() - 1);
    }

    public int getCurrentPage()
    {
        return mPager.getCurrentItem();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {
        //Log.i("", String.format("onPageScrolled %d %f %d", position, positionOffset, positionOffsetPixels));
    }

    @Override
    public void onPageSelected(int position)
    {
        // for some bizarre reason this is not called on the very first page,
        // so that visit is set in the adapter

        ModelQuestion q = ManagerGame.instance().getQuestion(position);
        if (q != null) q.visit();
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {
        //Log.i("", String.format("onPageScrollStateChanged %d", state));
    }
}
