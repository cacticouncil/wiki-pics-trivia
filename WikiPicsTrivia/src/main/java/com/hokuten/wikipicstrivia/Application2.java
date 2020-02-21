package com.hokuten.wikipicstrivia;

import android.app.Application;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Application2 extends Application
{
    private static final String CCBYSA = "https://creativecommons.org/licenses/by-sa/1.0/";
    private static final String CCBY2 = "https://creativecommons.org/licenses/by/2.0/legalcode";
    private static final String CCBY2_5 = "https://creativecommons.org/licenses/by/2.5/legalcode";
    private static final String CCBY3 = "https://creativecommons.org/licenses/by/3.0/legalcode";
    private static final String GNUFDL = "http://www.gnu.org/licenses/fdl.html";

    private static Application2 mInstance;

    private ActivityMain mActivity;
    private Random       mRandom;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mInstance = (Application2)getApplicationContext();
        mRandom = new Random(System.nanoTime());
    }

    public static Application2 instance()
    {
        return mInstance;
    }

    public int randInt(int min, int max)
    {
        return mRandom.nextInt((max - min) + 1) + min;
    }

    public String getResString(int res)
    {
        String text;
        try { text = getResources().getString(res); }
        catch (Exception ex) { text = ""; }
        return text;
    }

    public float getResDimen(int res)
    {
        float dimen;
        try { dimen = getResources().getDimension(res); }
        catch (Exception ex) { dimen = 0; }
        return dimen;
    }

    public String readTextFile(int resID)
    {
        InputStream input = getResources().openRawResource(resID);
        Scanner s = new Scanner(input).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public ActivityMain getMainActivity()
    {
        return mActivity;
    }

    public void setMainActivity(ActivityMain activity)
    {
        mActivity = activity;
    }

    public Point getScreenDimensions()
    {
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    //---------------------------------------------------------------
    // Static Helpers
    //---------------------------------------------------------------

    public static void toast(int resId)
    {
        //int length = (shortLength) ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(mInstance, mInstance.getResString(resId), Toast.LENGTH_SHORT).show();
    }

    public static void toast(String str)
    {
        //int length = (shortLength) ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(mInstance, str, Toast.LENGTH_SHORT).show();
    }

    public static int randIntSeed(long seed, int min, int max)
    {
        Random rand = new Random(seed);
        return rand.nextInt((max - min) + 1) + min;
    }

    public static int rotateIndex(int idx, int min, int max, boolean right)
    {
        if (right)
        {
            idx++;
            if (idx > max) idx = min;
        }
        else
        {
            idx--;
            if (idx < min) idx = max;
        }
        return idx;
    }

    public static String combinePaths(String path1, String path2)
    {
        String path;
        if      (path1.endsWith("/") && path2.startsWith("/")) path = path1 + path2.substring(1);
        else if (path1.endsWith("/") || path2.startsWith("/")) path = path1 + path2;
        else                                                   path = path1 + "/" + path2;
        return path;
    }

    public static void deleteFileTree(File file)
    {
        if (file.isDirectory())
            for (String child : file.list())
                deleteFileTree(new File(file, child));
        //Log.i("", file.getAbsolutePath());
        file.delete();  // delete child file or empty directory
        // note this will delete the directory passed in as well
    }

    public static void logFileTree(File[] files)
    {
        if (AppConfig.DEBUG)
        {
            for (File file : files)
            {
                Log.i("", file.getAbsolutePath());
                if (file.isDirectory())
                    logFileTree(file.listFiles());
            }
        }
    }

    public static void copy(File src, File dst) throws IOException
    {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
        {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
        {
            return true;
        }
        return false;
    }

    public static float interpolate(float a, float b, float proportion)
    {
        return (a + ((b - a) * proportion));
    }

    public static int interpolateColor(int a, int b, float proportion)
    {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++)
        {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }

    public static boolean unpackZip(InputStream srcFile, String destFolder)
    {
        ZipInputStream zin = new ZipInputStream(srcFile);
        boolean completed = false;

        byte buffer[] = new byte[4096];
        int bytesRead;
        ZipEntry entry;

        try
        {
            while ((entry = zin.getNextEntry()) != null)
            {
                File file = new File(destFolder, entry.getName());
                file.mkdirs();

                if(!entry.isDirectory())
                {
                    if (file.exists())
                        file.delete();
                    FileOutputStream fos = new FileOutputStream(file);

                    while ((bytesRead = zin.read(buffer)) != -1)
                    {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.close();
                }
            }
            zin.close();

            completed = true;
        }
        catch (FileNotFoundException e)
        {
            if (AppConfig.DEBUG)
                Log.e("Application2-unpackZip", "Failed to create new file.");
        }
        catch (IOException e)
        {
            if (AppConfig.DEBUG)
                Log.e("Application2-unpackZip", "Failed to flush, read entry or close zip.");
        }

        return completed;
    }

    // Creates a new zip file at the folder with the specified name
    public static boolean zipFolder(String srcFolder, String destZipFile)
    {
        boolean completed = false;
        ZipOutputStream zip;
        FileOutputStream fileWriter;

        try
        {
            fileWriter = new FileOutputStream(destZipFile);
            zip = new ZipOutputStream(fileWriter);

            addFolderToZip("", srcFolder, zip);
            zip.flush();
            zip.close();

            completed = true;
        }
        catch (Exception e)
        {
            if (AppConfig.DEBUG)
                Log.e("Application2-zipFolder", "Failed to zip folder: '" + srcFolder + "'", e);
        }

        return completed;
    }

    // Adds the file to the zip output
    public static void addFileToZip(String path, String srcFile, ZipOutputStream zip)
    {
        File folder = new File(srcFile);
        if (folder.isDirectory())
        {
            addFolderToZip(path, srcFile, zip);
        }
        else
        {
            byte[] buf = new byte[1024];
            int len;
            try
            {
                FileInputStream in = new FileInputStream(srcFile);
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                while ((len = in.read(buf)) > 0)
                {
                    zip.write(buf, 0, len);
                }
            }
            catch (FileNotFoundException e)
            {
                if (AppConfig.DEBUG)
                    Log.e("Application2-addFileToZip", "Failed to create input file.");
            }
            catch (IOException e)
            {
                if (AppConfig.DEBUG)
                    Log.e("Application2-addFileToZip", "Failed to put next entry in zip.");
            }
        }
    }

    // Adds the folder to the zip output
    public static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip)
    {
        File folder = new File(srcFolder);
        for (String fileName : folder.list())
        {
            if (path.equals(""))
            {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            }
            else
            {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
            }
        }
    }

    public static void fixRepeatingBackground(View view)
    {
        Drawable bg = view.getBackground();
        if (bg != null)
        {
            if (bg instanceof BitmapDrawable)
            {
                BitmapDrawable bmp = (BitmapDrawable) bg;
                bmp.mutate(); // make sure that we aren't sharing state anymore
                bmp.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            }
        }
    }

    public static String getLicenseLink(String license)
    {
        if (license.contains("2.0")) {
            return CCBY2;
        } else if (license.contains("2.5")) {
            return CCBY2_5;
        } else if (license.contains("3.0")) {
            return CCBY3;
        } else if (license.toLowerCase().contains("gnu free")) {
            return GNUFDL;
        } else if ((license.toLowerCase().contains("creative commons") && license.contains("sa")) ||
                license.contains("SA-1.0")) {
            return CCBYSA;
        }

        return null;
    }
}
