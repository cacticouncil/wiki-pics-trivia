package com.hokuten.wikipicstrivia;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;


public class ModelHelp implements Parcelable
{
    public enum Help
    {
        PAD_DO_NOT_USE,         // 0

        LEGEND,
        CUSTOM_MODES,
        CREATE_MODE_SWIPE,
        QUESTIONS_DOWNLOAD,
        BROWSABLE,
        HASLINKS,
        MISSES,
        QOTD_SUGGEST,
        DISCLAIMER
    }

    public String message;
    public Point  highlightPoint;
    public int    highlightRadius;

    public ModelHelp()
    {
        message = "";
        highlightPoint = new Point();
        highlightRadius = 0;
    }

    //-----------------------------------------
    // Parcelable Interface
    //-----------------------------------------

    public static final Creator<ModelHelp> CREATOR = new Creator<ModelHelp>()
    {
        public ModelHelp createFromParcel(Parcel in)
        {
            return new ModelHelp(in);
        }

        public ModelHelp[] newArray(int size)
        {
            return new ModelHelp[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        // write and read order must match!

        parcel.writeString(message);
        parcel.writeInt(highlightPoint.x);
        parcel.writeInt(highlightPoint.y);
        parcel.writeInt(highlightRadius);
    }

    private ModelHelp(Parcel in)
    {
        // write and read order must match!

        message = in.readString();
        highlightPoint = new Point();
        highlightPoint.x = in.readInt();
        highlightPoint.y = in.readInt();
        highlightRadius = in.readInt();
    }

    public static String getHelpMessage(Help h)
    {
        String msg = "";

        switch (h)
        {
            case CUSTOM_MODES:
                msg = Application2.instance().getResString(R.string.help_custom_modes1);
                break;
            case QUESTIONS_DOWNLOAD:
                msg = Application2.instance().getResString(R.string.help_questions_download);
                break;
            case BROWSABLE:
                msg = Application2.instance().getResString(R.string.help_browsable1);
                break;
            case HASLINKS:
                msg = Application2.instance().getResString(R.string.help_links);
                break;
            case MISSES:
                msg = Application2.instance().getResString(R.string.help_misses);
                break;
            case CREATE_MODE_SWIPE:
                msg = Application2.instance().getResString(R.string.help_createmode);
                break;
            default: break;
        }

        return msg;
    }
}
