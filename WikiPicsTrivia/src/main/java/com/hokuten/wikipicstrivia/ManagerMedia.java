package com.hokuten.wikipicstrivia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.util.Log;
import android.util.SparseArray;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;


public class ManagerMedia
{
    private static ManagerMedia mInstance;

    //private LinkedBlockingQueue<Pair<String, String>> imageQueue;
    //private Thread                                    imageLoaderThread;
    private SparseArray<Bitmap>                       mCachedDrawables;
    private MediaPlayer                               mBGMPlayer;
    private int                                       mBGM;

    private static final int NO_MUSIC = 0;

    private static final String WIKIMEDIA_URL = "http://en.wikipedia.org/w/api.php?";
    private static final String FORMAT        = "format";
    private static final String ACTION        = "action";
    private static final String GENERATOR     = "generator";
    private static final String PROP          = "prop";
    private static final String RV_PROP       = "rvprop";
    private static final String IMG_PROP      = "iiprop";
    private static final String PAGEIDS       = "pageids";
    private static final String TITLES        = "titles";

    private static final String FORMAT_JSON         = "json";
    private static final String ACTION_QUERY        = "query";
    private static final String GENERATOR_IMAGES    = "images";
    private static final String GENERATOR_IMG_LIMIT = "gimlimit";
    private static final String PROP_IMGINFO        = "imageinfo";
    private static final String PROP_REVISION       = "revisions";
    private static final String PROP_PAGEIMAGES     = "pageimages";
    private static final String RV_PROP_CONTENT     = "content";
    private static final String IMG_PROP_URL        = "url";
    private static final String IMG_PROP_MEDIATYPE  = "mediatype";
    private static final String IMG_INFO_URLWIDTH   = "iiurlwidth";
    private static final String IMG_INFO_URLHEIGHT  = "iiurlheight";
    private static final String PAGE_IMG_URLHEIGHT  = "pithumbsize";
    private static final String PAGE_IMG_LIMIT      = "pilimit";
    private static final String TITLES_FILE         = "File:";
    private static final String LIMIT_MAX           = "max";

    private static final String WM_JSON_QUERY     = "query";
    private static final String WM_JSON_PAGES     = "pages";
    private static final String WM_JSON_THUMBNAIL = "thumbnail";
    private static final String WM_JSON_SOURCE    = "source";
    private static final String WM_JSON_IMGINFO   = "imageinfo";
    private static final String WM_JSON_THUMBURL  = "thumburl";
    private static final String WM_JSON_REVISIONS = "revisions";
    private static final String WM_JSON_CONTENT   = "*";

    private static final String WM_CONTENT_IMAGE_TAG        = "image = ";
    private static final String WM_CONTENT_IMAGE_CAPION_TAG = "\n|";

    private static final String IMG_WIDTH  = "256";
    private static final String IMG_HEIGHT = "256";

    private static final String ERROR = "error";

//    private String standardQuery = WIKIMEDIA_URL + FORMAT + "=" + FORMAT_JSON + "&" +
//            ACTION + "=" + ACTION_QUERY + "&" +
//            GENERATOR + "=" + GENERATOR_IMAGES + "&" +
//            PROP + "=" + PROP_IMGINFO + "&" +
//            IMG_PROP + "=" + IMG_PROP_URL + "|" + IMG_PROP_MEDIATYPE + "&" +
//            IMG_INFO_URLWIDTH + "=" + IMG_WIDTH + "&" +
//            IMG_INFO_URLHEIGHT + "=" + IMG_HEIGHT + "&" +
//            PAGEIDS + "=" + "%s";
//
//    private String contentQuery = WIKIMEDIA_URL + FORMAT + "=" + FORMAT_JSON + "&" +
//            ACTION + "=" + ACTION_QUERY + "&" +
//            PROP + "=" + PROP_REVISION + "&" +
//            RV_PROP + "=" + RV_PROP_CONTENT + "&" +
//            PAGEIDS + "=" + "%s";
//
//    private String imageQuery = WIKIMEDIA_URL + FORMAT + "=" + FORMAT_JSON + "&" +
//            ACTION + "=" + ACTION_QUERY + "&" +
//            TITLES + "=" + TITLES_FILE + "%s" + "&" +
//            PROP + "=" + PROP_IMGINFO + "&" +
//            IMG_PROP + "=" + IMG_PROP_URL + "&" +
//            IMG_INFO_URLWIDTH + "=" + IMG_WIDTH + "&" +
//            IMG_INFO_URLHEIGHT + "=" + IMG_HEIGHT + "&";

    private String pageImagesQuery = WIKIMEDIA_URL + FORMAT + "=" + FORMAT_JSON + "&" +
            ACTION + "=" + ACTION_QUERY + "&" +
            PROP + "=" + PROP_PAGEIMAGES + "&" +
            PAGE_IMG_URLHEIGHT + "=" + IMG_HEIGHT + "&" +
            PAGEIDS + "=" + "%s" + "&" +
            PAGE_IMG_LIMIT + "=" + LIMIT_MAX;

    private ManagerMedia()
    {
        //imageQueue = new LinkedBlockingQueue<Pair<String, String>>();
        //imageLoaderThread = new Thread();
        mCachedDrawables = new SparseArray<Bitmap>();
        mBGMPlayer = new MediaPlayer();
        mBGM = NO_MUSIC;
    }

    public static ManagerMedia instance()
    {
        if (mInstance == null) mInstance = new ManagerMedia();
        return mInstance;
    }

    public void pause()
    {
        if (mBGM != NO_MUSIC)
            if (mBGMPlayer.isPlaying())
                mBGMPlayer.pause();
    }

    public void resume()
    {
        if (!ManagerDB.instance().getSettings().music) return;

        if (mBGM != NO_MUSIC)
            mBGMPlayer.start();
        else
        {
            ModelGameRound gameRound = ManagerGame.instance().getGameRoundInfo();
            if (gameRound != null && gameRound.active)
                playBGM(R.raw.bgm_game);
            else
                playBGM(R.raw.bgm_menu);
        }
    }

    public void setCachedDrawable(int resID, Bitmap b)
    {
        mCachedDrawables.put(resID, b);
    }

    public Bitmap getCachedDrawable(int resID)
    {
        return mCachedDrawables.get(resID);
    }

    public void playSound(int resID)
    {
        playSound(resID, 1.0f);
    }

    public void playSound(int resID, float volume)
    {
        if (!ManagerDB.instance().getSettings().sfx) return;

        MediaPlayer m = MediaPlayer.create(Application2.instance().getApplicationContext(), resID);
        m.setVolume(volume, volume);
        m.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer)
            {
                mediaPlayer.start();
            }
        });
        m.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer)
            {
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        });
    }

    public void playBGM(int resID)
    {
        playBGM(resID, 1.0f);
    }

    public void playBGM(int resID, float volume)
    {
        if (!ManagerDB.instance().getSettings().music) return;

        // bgm already playing
        if (mBGMPlayer.isPlaying())
        {
            // request to play song already playing; exit
            if (resID == mBGM) return;
            // stop playing old bgm
            else mBGMPlayer.stop();
        }

        // release old bgm
        mBGMPlayer.reset();
        mBGMPlayer.release();

        // load new bgm
        mBGM = resID;
        mBGMPlayer = MediaPlayer.create(Application2.instance().getApplicationContext(), resID);
        mBGMPlayer.setVolume(volume, volume);
        mBGMPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer)
            {
                mediaPlayer.seekTo(0);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        });
    }

    public void stopBGM()
    {
        mBGM = NO_MUSIC;

        if (mBGMPlayer != null)
        {
            if (mBGMPlayer.isPlaying())
                mBGMPlayer.stop();

            mBGMPlayer.reset();
            mBGMPlayer.release();
            mBGMPlayer = new MediaPlayer();
        }
    }

    public String getImagesDir()
    {
        return Application2.combinePaths(
               Application2.instance().getFilesDir().toString(),
               Application2.instance().getResString(R.string.app_image_dir));
    }

    public boolean downloadImage(String url, String mid)
    {
        boolean success = false;
        Bitmap bitmap;
        InputStream is = null;

        if(url == null || url.isEmpty())
            return false;

        try
        {
            URL fullURL = new URL(url);

            URLConnection openConnection = fullURL.openConnection();
            openConnection.setDoInput(true);
            openConnection.connect();

            is = openConnection.getInputStream();

            bitmap = BitmapFactory.decodeStream(is);
            if(bitmap != null)
            {
                createTopicImage(mid, bitmap);
                success = true;
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerMedia-downloadImage", "Url: " + url);
            if (AppConfig.DEBUG) Log.e("ManagerMedia-downloadImage", "Error!", ex);
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (Exception e)
                {
                    // do nothing?
                }
            }
        }

        return success;
    }

    public Bitmap getTopicImage(String mid)
    {
        return getTopicImage(mid, 0);
    }

    public Bitmap getTopicImage(String mid, int maxdim)
    {
        Bitmap image = null;
        try
        {
            File filepath = getTopicImageFile(mid);
            FileInputStream fis = new FileInputStream(filepath);
            try
            {
                if (maxdim > 0)
                {
                    // decode bitmap just with inJustDecodeBounds=true to check the dimensions without loading into memory
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(fis, null, options);
                    fis = new FileInputStream(filepath); // reload stream

                    // calculate inSampleSize for sub-sampling
                    options.inSampleSize = calculateInSampleSize(options, maxdim, maxdim);
                    //if (AppConfig.DEBUG)
                    //    if (options.inSampleSize > 1)
                    //        Log.v("ManagerMedia-getTopicImage", mid + " image decoded with inSampleSize of " + Integer.toString(options.inSampleSize));

                    // decode bitmap with inSampleSize set
                    options.inJustDecodeBounds = false;
                    image = BitmapFactory.decodeStream(fis, null, options);
                }
                else
                {
                    image = BitmapFactory.decodeStream(fis);
                }
            }
            finally { fis.close(); }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerMedia-getTopicImage", "Failed to retrieve image '" + mid + "'");
        }
        return image;
    }

    public File getTopicImageFile(String mid)
    {
        File filePath = null;
        try
        {
            String dirPath = topicImageDir(mid);
            if (dirPath == null) return null;

            filePath = new File(getImagesDir(),
                                Application2.combinePaths(dirPath, midSansPrefix(mid) + Application2.instance().getResString(R.string.app_image_extension)));
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerMedia-getTopicImageFile", "Failed to retrieve image '" + mid + "'");
        }
        return filePath;
    }

    public void createTopicImage(String mid, Bitmap bmp)
    {
        try
        {
            String dirPath = topicImageDir(mid);
            if (dirPath == null) return;

            File dir = new File(getImagesDir(), dirPath);
            dir.mkdirs();
            File filePath = new File(dir, midSansPrefix(mid) + Application2.instance().getResString(R.string.app_image_extension));
            FileOutputStream fos = new FileOutputStream(filePath);

            // here we apply any necessary alterations to images
            bmp = cleanupImage(mid, bmp);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);

            try     { fos.write(bos.toByteArray()); }
            finally { fos.close(); }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerMedia-createTopicImage", "Failed to save image file for '" + mid + "'");
        }
    }

    public void packageImages()
    {
        if (!Application2.isExternalStorageWritable())
        {
            Application2.toast(R.string.toast_error_writable_storage);
        }

        File srcFile = new File(Application2.instance().getExternalFilesDir(null), Application2.instance().getResString(R.string.app_image_zip));
        if (srcFile.exists())
            srcFile.delete();

        if (Application2.zipFolder(getImagesDir(), srcFile.toString()))
        {
            Application2.toast(R.string.toast_success_pack_images);
        }
        else
        {
            Application2.toast(R.string.toast_error_pack_images);
        }
    }

    public void unpackImages()
    {
        InputStream zipFile = Application2.instance().getResources().openRawResource(R.raw.images);
        String destFolder = Application2.instance().getFilesDir().toString();

        if (!Application2.unpackZip(zipFile, destFolder))
        {
            // TODO: Handle this more gracefully in final release

            if (AppConfig.DEBUG)
                Log.e("ManagerMedia-unpackageImages", "Failed to unpack images.");
        }
    }

    public void deleteAllTopicImages()
    {
        File imgDir = new File(getImagesDir());
        if (imgDir.exists())
            Application2.deleteFileTree(imgDir);
        // note that images directory no longer exists at this point
        // should it be recreated?
    }

    //public void createTopicImage(String mid, int res)
    //{
    //    Bitmap bmp = BitmapFactory.decodeResource(Application2.instance().getResources(), res);
    //    createTopicImage(mid, bmp);
    //}

    //---------------------------------------------------------------
    // Helpers
    //---------------------------------------------------------------

    private String midSansPrefix(String mid)
    {
        String[] tokens = mid.split("/");
        return ((tokens != null && tokens.length > 0) ? tokens[tokens.length-1] : "");
    }

    private String topicImageDir(String mid)
    {
        // This method computes the local directory of where a topic image is stored
        // In order to prevent too many files in a single directory of internal storage,
        // this method uses the mid itself and breaks it up into a path.

        if (mid == null || mid.isEmpty() || !mid.startsWith("/m/") || mid.length() < 4)
        {
            // TODO: handle this more gracefully in final release build
            if (AppConfig.DEBUG) Log.e("ManagerDB-topicImagePath", "Invalid mid " + ((mid != null) ? mid : "null"));
            return null;  // this is null to catch broken mids
        }

        StringBuilder dirpath = new StringBuilder();

        char[] chars = midSansPrefix(mid).toCharArray();
        for (int i=0; i<chars.length; i++)
        {
            dirpath.append(chars[i]);
            if (i % 2 == 1) dirpath.append("/");
        }
        if (dirpath.charAt(dirpath.length()-1) != '/') dirpath.append('/');

        return dirpath.toString();
    }

    private Bitmap cleanupImage(String mid, Bitmap original)
    {
        int w = original.getWidth();
        int h = original.getHeight();
        int l = w*h;
        int a,r,g,b;
        int pixCountDark = 0;
        int pixCountVisible = 0;
        int pixCountColor = 0;

        // for each pixel in original bitmap
        int[] pixelsOriginal = new int[l];
        original.getPixels(pixelsOriginal, 0, w, 0, 0, w, h);
        for (int i=0; i<l; i++)
        {
            a = (pixelsOriginal[i] >> 24) & 0xff;
            r = (pixelsOriginal[i] >> 16) & 0xff;
            g = (pixelsOriginal[i] >> 8)  & 0xff;
            b = (pixelsOriginal[i])       & 0xff;

            // visible pixel
            if (a > 0x80)
            {
                pixCountVisible++;

                // very dark pixel
                if (r < 0x30 && g < 0x30 && b < 0x30) pixCountDark++;
                // non-gray-scale pixel
                else if (r != g || r != b || g != b)  pixCountColor++;
            }
        }

        float color = (float)pixCountColor/(float)pixCountVisible;
        float dark  = (float)pixCountDark/(float)pixCountVisible;

        // for images that have alpha, are gray-scale, with more black than white
        if ((pixCountVisible < l) && (color < .1) && (dark > 0.65f))
        {
            // for each pixel in inverted bitmap
            int[] pixelsInverted = new int[l];
            Bitmap inverted = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            inverted.getPixels(pixelsInverted, 0, w, 0, 0, w, h);
            for (int i = 0; i < l; i++)
            {
                a =         (pixelsOriginal[i] >> 24) & 0xff;
                r = 0xff - ((pixelsOriginal[i] >> 16) & 0xff);
                g = 0xff - ((pixelsOriginal[i] >> 8)  & 0xff);
                b = 0xff - ((pixelsOriginal[i])       & 0xff);
                pixelsInverted[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }

            // set
            inverted.setPixels(pixelsInverted, 0, w, 0, 0, w, h);
            if (AppConfig.DEBUG)
                Log.w("ManagerDB-invertDarkBWImage",
                      String.format("Inverted image for '%s': dark(%.2f) color(%.2f)", mid, dark, color));

            return inverted;
        }
        else
        {
            return original;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth)
                inSampleSize *= 2;
        }

        return inSampleSize;
    }

//    public void queueImageForDownload(String baseURL, String parameters)
//    {
//        // Put url to process in the image queue
//        try
//        {
//            synchronized(imageQueue)
//            {
//                imageQueue.put(new Pair(baseURL, parameters));
//                imageQueue.notifyAll();
//            }
//        }
//        catch (Exception ex)
//        {
//            if (AppConfig.DEBUG) Log.e("ManagerMedia-getBitmap", "Error!", ex);
//        }
//
//        // Start thread if it's not started yet
//        if(imageLoaderThread.getState() == Thread.State.NEW)
//        {
//            imageLoaderThread.start();
//        }
//    }

//    private class ImageQueueManager implements Runnable
//    {
//        public ImageQueueManager()
//        {
//        }
//
//        public void run()
//        {
//            try
//            {
//                while(true)
//                {
//                    // Thread waits until there are images in the
//                    // queue to be retrieved
//                    if(imageQueue.size() == 0)
//                    {
//                        synchronized(imageQueue)
//                        {
//                            imageQueue.wait();
//                        }
//                    }
//
//                    // When we have images to be loaded in the future check if it's in cache?
//                    if(imageQueue.size() != 0)
//                    {
//                        Pair<String, String> image = null;
//                        synchronized(imageQueue)
//                        {
//                            image = imageQueue.poll();
//                        }
//
//                        if(image != null)
//                        {
//                            Bitmap bmp = getBitmap(image.first + image.second);
//
//                            if(bmp != null)
//                            {
//                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                                byte[] byteArray = stream.toByteArray();
//
//                                ManagerMedia.instance().createTopicImage(image.second, byteArray);
//                            }
//                        }
//
//                    }
//
//                    if(Thread.interrupted())
//                        break;
//                }
//            }
//            catch (Exception ex)
//            {
//                if (AppConfig.DEBUG) Log.e("ManagerMedia-ImageQueueManager-run", "Error.", ex);
//            }
//        }
//    }


    // Public access to the different types of queries to use for getting the correct wiki image
    public void getWikipediaImageUrls(ArrayList<ManagerFreebase.TopicInfo> topics)
    {
        pageImagesQueryMultiple(topics);
    }


    // Page Images query. Currently beta extension of media wiki. Uses pageId to find the most
    // appropriate image. Parsing the thumbnail tag and returning the contents as the image url.
    private void pageImagesQueryMultiple(ArrayList<ManagerFreebase.TopicInfo> topics)
    {
        String pageIdParamString = "";
        JSONObject response;

        if(topics.size() == 0)
            return;

        int i = 0;
        for(ManagerFreebase.TopicInfo topic: topics)
        {
            if(i > 0)
            {
                pageIdParamString += "|";
            }
            pageIdParamString += topic.wikiId;

            i++;
        }

        try
        {
            pageIdParamString = URLEncoder.encode(pageIdParamString, "UTF-8");
        }
        catch(Exception e)
        {
            if (AppConfig.DEBUG) Log.e("ManagerMedia-pageImagesQueryMultiple", "Encode failed");
            return;
        }

        String url = String.format(pageImagesQuery, pageIdParamString);

        response = generateQuery(url);

        JSONObject query;
        JSONObject pages;
        JSONObject page;
        JSONObject thumbnail;

        if(response == null)
            return;
        try
        {
            query = response.getJSONObject(WM_JSON_QUERY);
            pages = query.getJSONObject(WM_JSON_PAGES);

            for(ManagerFreebase.TopicInfo topic: topics)
            {
                // Second is the WikiId, first is the mid
                page = pages.getJSONObject(topic.wikiId);
                if (page.has(WM_JSON_THUMBNAIL))
                {
                    thumbnail = page.getJSONObject(WM_JSON_THUMBNAIL);
                    topic.imageUrl = thumbnail.getString(WM_JSON_SOURCE);
                } else
                {
                    if (AppConfig.DEBUG)
                        Log.d("ManagerMedia-pageImagesQueryMultiple", "No Thumbnail for PageId "
                                + topic.wikiId + " mid " + topic.mid);
                }
            }

        }
        catch(JSONException e)
        {
            if (AppConfig.DEBUG) Log.e("ManagerMedia-pageImagesQueryMultiple", "Error parsing data "
                    + e.toString());
        }

        return;
    }

    // Create query to retrieve images associated with the specified page id
    private JSONObject generateQuery(String url)
    {
        InputStream is = null;
        JSONObject json = null;
        String response = null;

        if (AppConfig.DEBUG) Log.v("ManagerMedia-generateQuery", "Query URL " + url);

        // Making the HTTP request
        try
        {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "wikipics-trivia");
            HttpGet httpGet = new HttpGet(url);

            if(httpGet != null)
            {
                HttpResponse httpResponse = httpClient.execute(httpGet);
                if(httpResponse != null)
                {
                    HttpEntity httpEntity = httpResponse.getEntity();
                    is = httpEntity.getContent();
                }
            }

        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        catch (ClientProtocolException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            if(is != null)
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = in.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
                is.close();
                response = sb.toString();
            }
        }
        catch (Exception e)
        {
            if (AppConfig.DEBUG) Log.e("ManagerMedia-generateQuery", "Error converting result " + e.toString());
        }

        try {
            if(response != null)
            {
                json = new JSONObject(response);
                if (!(json.optString(ERROR)).isEmpty())
                {
                    if (AppConfig.DEBUG) Log.w("ManagerMedia-generateQuery", "Errors returned");
                    json = null;
                } else if (json.optString(ACTION_QUERY).isEmpty())
                {
                    if (AppConfig.DEBUG)Log.w("ManagerMedia-generateQuery", "Empty result set");
                    json = null;
                }
            }

        }
        catch (JSONException e)
        {
            if (AppConfig.DEBUG) Log.e("ManagerMedia-generateQuery", "Error parsing data " + e.toString());
        }

        // return JSON String
        return json;
    }

}
