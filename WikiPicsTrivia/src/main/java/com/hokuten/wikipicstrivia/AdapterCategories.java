package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class AdapterCategories extends BaseExpandableListAdapter
{
    private Context                          mContext;
    private ArrayList<ModelTheme>            mThemes;
    private SparseArray<List<ModelCategory>> mCategories;
    private RadioGroup                       mSelectGroup;
    private boolean                          mViewOnly;

    public AdapterCategories(Context context, RadioGroup rSelectGroup)
    {
        mContext = context;
        mSelectGroup = rSelectGroup;
        mViewOnly = false;
        mThemes = ManagerDB.instance().getThemes();
        mCategories = new SparseArray<List<ModelCategory>>();

        for (ModelTheme theme : mThemes)
        {
            mCategories.put(theme.id, ManagerDB.instance().getCategoryPerTheme(theme.id));
        }
    }

    public AdapterCategories(Context context, int modeId, RadioGroup rSelectGroup)
    {
        mContext = context;
        mSelectGroup = rSelectGroup;
        mViewOnly = false;
        mThemes = ManagerDB.instance().getThemes();
        mCategories = new SparseArray<List<ModelCategory>>();

        for (ModelTheme theme: mThemes)
        {
            ArrayList<ModelCategory> checkedCategories = ManagerDB.instance().getCategoriesPerMode(modeId);
            mCategories.put(theme.id, ManagerDB.instance().getCategoryPerTheme(theme.id));

            boolean themeChecked = false;
            for (ModelCategory checkedCategory: checkedCategories)
            {
                for (ModelCategory category: mCategories.get(theme.id))
                {
                    if (checkedCategory.id == category.id)
                    {
                        themeChecked = true;
                        category.checked = true;
                    }
                }
            }
            theme.checked = themeChecked;
        }
        notifyDataSetChanged();
    }

    public AdapterCategories(Context context, boolean viewOnly, RadioGroup rSelectGroup)
    {
        mContext = context;
        mSelectGroup = rSelectGroup;
        mViewOnly = viewOnly;
        mThemes = ManagerDB.instance().getThemes();
        mCategories = new SparseArray<List<ModelCategory>>();

        for (ModelTheme theme : mThemes)
        {
            mCategories.put(theme.id, ManagerDB.instance().getCategoryPerTheme(theme.id));
        }
    }

    public SparseArray<List<ModelCategory>> getCategories()
    {
        return mCategories;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }

    @Override
    public int getGroupCount()
    {
        return mThemes.size();
    }

    @Override
    public int getChildrenCount(int groupPosition)
    {
        return mCategories.get(mThemes.get(groupPosition).id).size();
    }

    @Override
    public Object getGroup(int groupPosition)
    {
        return mThemes.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        return mCategories.get(mThemes.get(groupPosition).id).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition)
    {
        return mThemes.get(groupPosition).id;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return mCategories.get(mThemes.get(groupPosition).id).get(childPosition).id;
    }

    @Override
    public boolean hasStableIds()
    {
        return false;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
    {
        final ModelTheme theme = ((ModelTheme)getGroup(groupPosition));
        final ExpandableListView eLv = (ExpandableListView)parent;
        //int themeColorNoAlpha = theme.color & 0x00ffffff;
        String header = theme.name;

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.view_theme, null);
        }

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { 0xff101010, theme.color });
        gradient.setCornerRadius(0f);

        TextView txtHeader = (TextView) convertView.findViewById(R.id.txtTheme);
        txtHeader.setText(header);
        txtHeader.setBackgroundDrawable(gradient);
        //txtHeader.setBackgroundColor(0xff0000ff);

        CheckBox checkBox = (CheckBox)convertView.findViewById(R.id.chkThemeSelected);

        if (!mViewOnly)
        {
            checkBox.setChecked(theme.checked);
            checkBox.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // If a theme is toggled, toggle all categories part of that theme
                    theme.checked = !theme.checked;

                    for (ModelCategory category : mCategories.get(theme.id))
                    {
                        category.checked = theme.checked;
                    }

                    if (theme.checked)
                    {
                        if (allSelected() && mSelectGroup != null)
                            mSelectGroup.check(R.id.rBtnAll);
                    }
                    else
                    {
                        if (mSelectGroup != null)
                            mSelectGroup.check(R.id.rBtnCustom);
                    }

                    notifyDataSetChanged();
                }
            });

            // set click event for expanding, on whole view
            convertView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // Manually toggle expanding/collapsing
                    if (theme.expanded)
                        eLv.collapseGroup(groupPosition);
                    else
                        eLv.expandGroup(groupPosition, true);

                    theme.expanded = !theme.expanded;
                }
            });
        }
        else
        {
            checkBox.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    @Override
    public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        final ModelCategory category = ((ModelCategory)getChild(groupPosition, childPosition));
        String header = category.displayName;

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.view_category, null);
        }

        TextView txtHeader = (TextView) convertView.findViewById(R.id.txtCategory);
        txtHeader.setText(header);

        CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.chkCatSelected);

        if (!mViewOnly)
        {
            checkBox.setChecked(category.checked);
            checkBox.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    handleCategoryClick(category, groupPosition);
                }
            });

            // set click event on whole view
            convertView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    handleCategoryClick(category, groupPosition);
                }
            });
        }
        else
        {
            checkBox.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private void handleCategoryClick(ModelCategory category, int groupPosition)
    {
        // Toggle the checked category
        ModelTheme theme = ((ModelTheme)getGroup(groupPosition));
        int count = getChildrenCount(groupPosition);
        boolean checkedList = false;
        category.checked = !category.checked;

        // Check if any categories in a theme are checked. If they are, check the theme.
        for (int i = 0; i < count; i++)
        {
            if (((ModelCategory)getChild(groupPosition, i)).checked)
            {
                checkedList = true;
                break;
            }
        }

        theme.checked = checkedList;

        if (category.checked)
        {
            if (allSelected() && mSelectGroup != null)
                mSelectGroup.check(R.id.rBtnAll);
        }
        else
        {
            if (mSelectGroup != null)
                mSelectGroup.check(R.id.rBtnCustom);
        }

        notifyDataSetChanged();
    }

    public void selectAll(boolean checked)
    {
        for (ModelTheme theme: mThemes)
        {
            theme.checked = checked;
            for (ModelCategory category: mCategories.get(theme.id))
            {
                category.checked = checked;
            }
        }

        notifyDataSetChanged();
    }

    public boolean modeValid()
    {
        for (ModelTheme theme: mThemes)
        {
            if (theme.checked)
                return true;

            for (ModelCategory category: mCategories.get(theme.id))
            {
                if (category.checked)
                    return true;
            }
        }

        return false;
    }

    public boolean allSelected()
    {
        for (ModelTheme theme: mThemes)
        {
            if (!theme.checked)
                return false;

            for (ModelCategory category: mCategories.get(theme.id))
            {
                if (!category.checked)
                    return false;
            }
        }

        return true;
    }
}
