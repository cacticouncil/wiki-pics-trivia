package com.hokuten.wikipicstrivia;

import android.os.Parcel;
import android.os.Parcelable;


public class ModelQuestion implements Parcelable
{
    public enum State
    {
        UNKNOWN,
        UNANSWERED,
        INCORRECT,
        CORRECT
    }

    public static final int NUM_FAKE_ANSWERS = 3;

    public int      index;
    public State    state;
    public String   question;
    public String   answer;
    public int      aid;
    public String   mid;
    public String   wid;
    public long     seed;
    public int      idxSelected;
    public int      idxCorrect;
    public String[] fakes;
    public int      color;
    public String   theme;
    public String   author;
    public String   license;

    public ModelQuestion()
    {
        index       = -1;
        state       = State.UNKNOWN;
        question    = "";
        answer      = "";
        aid         = -1;
        mid         = "";
        wid         = "";
        seed        = -1;
        idxSelected = -1;
        idxCorrect  = -1;
        fakes       = new String[NUM_FAKE_ANSWERS];
        color       = 0;
        theme       = "";
        author      = "";
        license     = "";
    }

    public ModelQuestion(ModelQuestion copy)
    {
        index       = copy.index;
        state       = copy.state;
        question    = copy.question;
        answer      = copy.answer;
        aid         = copy.aid;
        mid         = copy.mid;
        wid         = copy.wid;
        seed        = copy.seed;
        idxSelected = copy.idxSelected;
        idxCorrect  = copy.idxCorrect;
        fakes       = new String[copy.fakes.length];
        color       = copy.color;
        theme       = copy.theme;
        author      = copy.author;
        license     = copy.license;

        System.arraycopy(copy.fakes,0,fakes,0,copy.fakes.length);
    }

    public boolean isCorrect()
    {
        return (state == State.CORRECT);
    }

    public boolean isAnswered()
    {
        return (state == State.CORRECT || state == State.INCORRECT);
    }

    public boolean isVisited()
    {
        return (state != State.UNKNOWN);
    }

    public void visit()
    {
        if (state == State.UNKNOWN)
            state = State.UNANSWERED;
    }

    //-----------------------------------------
    // Parcelable Interface
    //-----------------------------------------

    public static final Parcelable.Creator<ModelQuestion> CREATOR = new Parcelable.Creator<ModelQuestion>()
    {
        public ModelQuestion createFromParcel(Parcel in)
        {
            return new ModelQuestion(in);
        }

        public ModelQuestion[] newArray(int size)
        {
            return new ModelQuestion[size];
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

        parcel.writeInt(index);
        parcel.writeInt(state.ordinal());
        parcel.writeString(question);
        parcel.writeString(answer);
        parcel.writeInt(aid);
        parcel.writeString(mid);
        parcel.writeString(wid);
        parcel.writeLong(seed);
        parcel.writeInt(idxSelected);
        parcel.writeInt(idxCorrect);
        parcel.writeArray(fakes);
        parcel.writeInt(color);
        parcel.writeString(theme);
        parcel.writeString(author);
        parcel.writeString(license);
    }

    private ModelQuestion(Parcel in)
    {
        // write and read order must match!

        index       = in.readInt();
        state       = State.values()[in.readInt()];
        question    = in.readString();
        answer      = in.readString();
        aid         = in.readInt();
        mid         = in.readString();
        wid         = in.readString();
        seed        = in.readLong();
        idxSelected = in.readInt();
        idxCorrect  = in.readInt();
        fakes       = (String[])in.readArray(String.class.getClassLoader());
        color       = in.readInt();
        theme       = in.readString();
        author      = in.readString();
        license     = in.readString();
    }
}
