package com.hokuten.wikipicstrivia;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;


public class AdapterCreateMode extends FragmentPagerAdapter
{
    public enum CreateModePageType
    {
        NAME,
        QUESTION_COUNT,
        TIMER,
        MISSES,
        BROWSABLE,
        HASLINKS,
        CATEGORIES,
        NUM_PAGES  // always last
    }

    private final static String[] mPageTypes = {"Name", "Num Questions", "Timer", "Num Misses", "Browsable", "Wiki Links", "Categories"};
    private ModelGameMode   mGameMode;

    public AdapterCreateMode(FragmentManager fragmentManager)
    {
        super(fragmentManager);
        mGameMode = new ModelGameMode();
    }

    public AdapterCreateMode(FragmentManager fragmentManager, ModelGameMode gameMode)
    {
        super(fragmentManager);
        mGameMode = gameMode;
    }

    @Override
    public int getCount()
    {
        return CreateModePageType.NUM_PAGES.ordinal();
    }

    @Override
    public Fragment getItem(int position)
    {
        if (position >= CreateModePageType.NUM_PAGES.ordinal())
            return null;

        CreateModePageType page = CreateModePageType.values()[position];
        switch (page)
        {
            case NAME:           return FragmentCreateName.newInstance(mGameMode);
            case QUESTION_COUNT: return FragmentCreateNumQuestions.newInstance(mGameMode);
            case TIMER:          return FragmentCreateTimer.newInstance(mGameMode);
            case MISSES:         return FragmentCreateMisses.newInstance(mGameMode);
            case BROWSABLE:      return FragmentCreateBrowsable.newInstance(mGameMode);
            case HASLINKS:       return FragmentCreateHasLinks.newInstance(mGameMode);
            case CATEGORIES:     return FragmentCreateCategory.newInstance(mGameMode);
            default:             return null;
        }
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        if (position >= CreateModePageType.NUM_PAGES.ordinal())
            return null;

        return mPageTypes[position];
    }

    @Override
    public int getItemPosition(Object object)
    {
        FragmentCreateMode.Updater f = (FragmentCreateMode.Updater) object;
        if (f != null)
            f.update(mGameMode);

        return super.getItemPosition(object);
    }
}