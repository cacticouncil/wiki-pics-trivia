package com.hokuten.wikipicstrivia;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class ModelAlphaCursor implements Serializable
{
    private static final String FILENAME = "alphacursor";

    private short[][] mAlphaCursors;

    public static ModelAlphaCursor open()
    {
        ModelAlphaCursor obj;

        if (!getFile().exists())
            obj = new ModelAlphaCursor();
        else
            obj = load();

        return obj;
    }

    public static void delete()
    {
       getFile().delete();
    }

    public int getCursor(int category, int alpha)
    {
        return (int)mAlphaCursors[category][alpha];
    }

    public void setCursor(int category, int alpha, int cursor)
    {
        mAlphaCursors[category][alpha] = (short)cursor;
    }

    public void close()
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(getFile());
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(this);
            out.close();
            fos.close();
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ModelAlphaCursor-close", "Error", ex);
        }
    }

    //-----------------------------------------
    // Private Helpers
    //-----------------------------------------

    private ModelAlphaCursor()
    {
        // should not be called outside this class!

        int numCategories = ManagerDB.instance().getNumCategories();

        mAlphaCursors = new short[numCategories][26];
    }

    private static ModelAlphaCursor load()
    {
        ModelAlphaCursor obj = null;

        try
        {
            FileInputStream fis = new FileInputStream(getFile());
            ObjectInputStream in = new ObjectInputStream(fis);
            obj = (ModelAlphaCursor)in.readObject();
            in.close();
            fis.close();
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ModelAlphaCursor-load", "Error", ex);
        }

        return obj;
    }

    private static File getFile()
    {
        return new File(Application2.instance().getFilesDir(), FILENAME);
    }
}
