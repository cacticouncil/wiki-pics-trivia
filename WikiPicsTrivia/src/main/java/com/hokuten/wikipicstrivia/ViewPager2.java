package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;


/**
 * This custom ViewPager addresses the following error:
 *
 * 07-16 12:26:02.995  22543-22543/com.hokuten.wikipics E/AndroidRuntime﹕ FATAL EXCEPTION: main
 *     Process: com.hokuten.wikipics, PID: 22543
 *     java.lang.IllegalArgumentException: No view found for id (x) for fragment FragmentPlayPage
 *         at android.support.v4.app.FragmentManagerImpl.moveToState(FragmentManager.java:930)
 *         at android.support.v4.app.FragmentManagerImpl.moveToState(FragmentManager.java:1115)
 *         at android.support.v4.app.BackStackRecord.run(BackStackRecord.java:682)
 *         at android.support.v4.app.FragmentManagerImpl.execPendingActions(FragmentManager.java:1478)
 *         at android.support.v4.app.FragmentManagerImpl.executePendingTransactions(FragmentManager.java:478)
 *         at android.support.v4.app.FragmentStatePagerAdapter.finishUpdate(FragmentStatePagerAdapter.java:163)
 *         at android.support.v4.view.ViewPager.populate(ViewPager.java:1068)
 *         at android.support.v4.view.ViewPager.populate(ViewPager.java:914)
 *         at android.support.v4.view.ViewPager$3.run(ViewPager.java:244)
 *
 * A solution (that seems to work) was suggested here (making a custom ViewPager):
 * http://stackoverflow.com/a/19900206/1002098
 */
public class ViewPager2 extends ViewPager
{
    private PagerAdapter mPagerAdapter;
    private boolean      mPagingEnabled;

    public ViewPager2(Context context)
    {
        super(context);

        mPagingEnabled = true;
    }

    public ViewPager2(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mPagingEnabled = true;
    }

    @Override
    public void setAdapter(PagerAdapter adapter)
    {
        // do nothing
    }

    public void storeAdapter(PagerAdapter adapter)
    {
        mPagerAdapter = adapter;
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (mPagerAdapter != null)
        {
            super.setAdapter(mPagerAdapter);
        }
    }

    public void setPagingEnabled(boolean b)
    {
        mPagingEnabled = b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!mPagingEnabled) return false;
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        if (!mPagingEnabled) return false;
        return super.onInterceptTouchEvent(event);
    }
}
