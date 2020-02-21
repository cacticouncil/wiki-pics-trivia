package com.hokuten.wikipicstrivia;


import android.os.Parcel;
import android.os.Parcelable;


public class ModelGameMode implements Parcelable
{
    public enum Categories
    {
        ALL,
        CATEGORY,
        CUSTOM,
        // STANDARD, // no longer exists
    }

    public static final String[] CategoryNames = {Application2.instance().getResString(R.string.gamemode_default_label),
                                                  Application2.instance().getResString(R.string.gamemode_category_label),
                                                  Application2.instance().getResString(R.string.gamemode_custom_label)};

    public int        id;
    public String     name;
    public Categories categories;
    public int        questions;
    public int        timer;
    public int        misses;
    public boolean    isBrowsable;
    public boolean    hasHints;
    public boolean    hasLinks;

    public ModelGameMode()
    {
        id          = -1;
        name        = "";
        categories  = Categories.ALL;
        questions   = 10;
        timer       = 5;
        misses      = 0;
        isBrowsable = true;
        hasHints    = false;
        hasLinks    = false;
    }

    //-----------------------------------------
    // Parcelable Interface
    //-----------------------------------------

    public static final Parcelable.Creator<ModelGameMode> CREATOR = new Parcelable.Creator<ModelGameMode>()
    {
        public ModelGameMode createFromParcel(Parcel in)
        {
            return new ModelGameMode(in);
        }

        public ModelGameMode[] newArray(int size)
        {
            return new ModelGameMode[size];
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

        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeInt(categories.ordinal());
        parcel.writeInt(questions);
        parcel.writeInt(timer);
        parcel.writeInt(misses);
        parcel.writeInt(isBrowsable ? 1 : 0);
        parcel.writeInt(hasHints ? 1 : 0);
        parcel.writeInt(hasLinks ? 1 : 0);
    }

    private ModelGameMode(Parcel in)
    {
        // write and read order must match!

        id          = in.readInt();
        name        = in.readString();
        categories  = Categories.values()[in.readInt()];
        questions   = in.readInt();
        timer       = in.readInt();
        misses      = in.readInt();
        isBrowsable = (in.readInt()==0) ? false : true;
        hasHints    = (in.readInt()==0) ? false : true;
        hasLinks    = (in.readInt()==0) ? false : true;
    }
}
