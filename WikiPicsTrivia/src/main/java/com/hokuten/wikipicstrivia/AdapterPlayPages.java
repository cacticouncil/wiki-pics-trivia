package com.hokuten.wikipicstrivia;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;


public class AdapterPlayPages extends FragmentStatePagerAdapter
{
    public AdapterPlayPages(FragmentManager fragmentManager)
    {
        super(fragmentManager);
    }

    @Override
    public int getCount()
    {
        ModelGameRound info = ManagerGame.instance().getGameRoundInfo();

        if (info.mode.questions > 0)
            return info.mode.questions;
        else
            return Integer.MAX_VALUE;
    }

    @Override
    public Fragment getItem(int position)
    {
        ModelQuestion q = null;
        FragmentPlayPage p = null;
        int count = ManagerGame.instance().getQuestionCount();

        // load/store new question
        if (position == count)
        {
            // index requested is equal to the size of our question collection.
            // that means we need to add 1 new question to the collection.

            q = ManagerGame.instance().newQuestion();
            p = FragmentPlayPage.newInstance(q);

            // hack to set very first page visited because onPageSelected not called
            if (position == 0) q.visit();
        }
        else if (position >= 0 && position < count)
        {
            // index requested is less than the size of our question collection.
            // that means we just return an existing question in collection.

            q = ManagerGame.instance().getQuestion(position);
            p = FragmentPlayPage.newInstance(q);
        }
        else
        {
            // this means the pager wants a page index beyond the size of our questions collection.
            // this can happen if endRound occurred already (which involves dropping some questions)

            // this should never happen!!
            if (AppConfig.DEBUG)
                if (ManagerGame.instance().getGameRoundInfo().active)
                    Log.e("AdapterPlayPages-getItem", "ERROR in paging; position '" +
                            position + "' out of bounds of questions array!  This should never happen!!!");

            // if this happens, just use a question that has already been loaded
            q = ManagerGame.instance().getQuestion(0);
            p = FragmentPlayPage.newInstance(q);
        }

        if (AppConfig.DEBUG)
            if (q != null)
                Log.i("", String.format("Q(%03d): %-10s %-6d '%s'", q.index+1, q.mid, q.aid, q.answer));

        return p;
    }
}