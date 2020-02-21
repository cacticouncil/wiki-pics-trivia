package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;


public class AdapterGameModes extends BaseExpandableListAdapter
{
    private Context                               mContext;
    private HashMap<String, List<ModelGameMode>>  mGameModes;

    public AdapterGameModes(Context context)
    {
        mContext   = context;
        mGameModes = new HashMap<String, List<ModelGameMode>>();

        for (int i = 0; i < ModelGameMode.CategoryNames.length; i++)
        {
            mGameModes.put(ModelGameMode.CategoryNames[i], ManagerDB.instance().getGameModes(ModelGameMode.Categories.values()[i]));
        }
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }

    @Override
    public int getGroupCount()
    {
        return mGameModes.size();
    }

    @Override
    public int getChildrenCount(int groupPosition)
    {
        return mGameModes.get(ModelGameMode.CategoryNames[groupPosition]).size();
    }

    @Override
    public Object getGroup(int groupPosition)
    {
        return mGameModes.get(ModelGameMode.CategoryNames[groupPosition]);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        if (getChildrenCount(groupPosition) > 0)
            return mGameModes.get(ModelGameMode.CategoryNames[groupPosition]).get(childPosition);
        else
            return null;
    }

    @Override
    public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        if (getChildrenCount(groupPosition) > 0)
            return mGameModes.get(ModelGameMode.CategoryNames[groupPosition]).get(childPosition).id;
        else
            return -1;
    }

    @Override
    public boolean hasStableIds()
    {
        return false;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = new ViewGameMode(mContext);
        }

        ModelGameMode gameMode = (ModelGameMode)getChild(groupPosition, childPosition);
        if (gameMode != null)
        {
            ((ViewGameMode)convertView).setGameMode(gameMode);
            ((ViewGameMode)convertView).enableArrow(true);
        }

        return convertView;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.view_gamemodeheader, null);
        }

        TextView txtHeader = (TextView) convertView.findViewById(R.id.txtHeader);
        txtHeader.setText(ModelGameMode.CategoryNames[groupPosition]);

        return convertView;
    }

    public void removeItem(int position)
    {
        mGameModes.get(ModelGameMode.CategoryNames[ModelGameMode.Categories.CUSTOM.ordinal()]).remove(position);
    }
}
