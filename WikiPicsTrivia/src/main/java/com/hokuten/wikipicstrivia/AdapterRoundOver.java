package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;


public class AdapterRoundOver extends BaseAdapter
{
    private Context                  mContext;
    private ArrayList<ModelQuestion> mQuestions;
    private int                      mSelectedPosition;

    public AdapterRoundOver(Context context, ArrayList<ModelQuestion> questions)
    {
        mContext = context;
        mQuestions = questions;
        mSelectedPosition = -1;
    }

    public int getCount()
    {
        return mQuestions.size();
    }

    @Override
    public ModelQuestion getItem(int position)
    {
        return mQuestions.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return mSelectedPosition;
    }

    public int getSelectedPosition()
    {
        return mSelectedPosition;
    }

    public void setSelectedPosition(int position)
    {
        mSelectedPosition = position;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = new ViewQuestion(mContext);
            ((ViewQuestion) convertView).setQuestion(mQuestions.get(position));
        }
        else
        {
            // Using old View, reset it
            ((ViewQuestion) convertView).setQuestion(mQuestions.get(position));
        }

        // Check if the position is the one selected and show the share button and change
        // the background of the selected item.
        if(mSelectedPosition != -1 && position == mSelectedPosition)
        {
            convertView.setBackgroundColor(Color.GRAY);
            ((ViewQuestion) convertView).getShareButton().setVisibility(View.VISIBLE);
            ((ViewQuestion) convertView).getWikipediaButton().setVisibility(View.VISIBLE);
        }
        else
        {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            ((ViewQuestion) convertView).getShareButton().setVisibility(View.INVISIBLE);
            ((ViewQuestion) convertView).getWikipediaButton().setVisibility(View.INVISIBLE);
        }

        return convertView;
    }
}
