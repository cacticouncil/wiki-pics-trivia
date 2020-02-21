package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.HttpsURLConnection;


public class ManagerFreebase
{
    public interface Listener
    {
        public void onUpdateQuestionsInitiated();
        public void onUpdateQuestionsInProgress(int progress);
        public void onUpdateQuestionsCancelled();
        public void onUpdateQuestionsComplete();
        public void onUpdateTestConnectionComplete(Boolean aBoolean);
    }

    private static final String API_URL       = "https://www.googleapis.com/freebase/v1/";
    private static final String IMAGE_API_URL = "https://usercontent.googleapis.com/freebase/v1/image";
    private static final String TOPIC_API_URL = "https://usercontent.googleapis.com/freebase/v1/topic";

    private static final String KEY                       = "key";
    private static final String API_TYPE_SEARCH           = "search";
    private static final String API_KEY                   = "AIzaSyB9hO10nP9xo-M9KUAdEpbDkRap1zJ-KGQ";
    private static final String IMAGE_API_MAX_SIZE_PARAMS = "?maxwidth=256&maxheight=256";

    private static final String ERROR      = "error";
    private static final String CODE       = "code";
    private static final String RESULT     = "result";
    private static final String QUERY      = "query";
    private static final String FILTER     = "filter";
    private static final String FILTER_ANY = "any";
    private static final String FILTER_ALL = "all";
    private static final String OUTPUT     = "output";
    private static final String LIMIT      = "limit";
    private static final String CURSOR     = "cursor";
    private static final String L_PAREN    = "(";
    private static final String R_PAREN    = ")";
    private static final String PREFIXED   = "prefixed";

    private static final String FREEBASE_NAME                 = "name";
    private static final String FREEBASE_MID                  = "mid";
    private static final String FREEBASE_TYPE                 = "type:";
    private static final String FREEBASE_OBJECT_TYPE          = "/type/object/type";
    private static final String FREEBASE_OBJECT_KEY           = "/type/object/key";
    private static final String FREEEBASE_COMMON_IMAGE        = "/common/topic/image";
    private static final String FREEBASE_KEY                  = "key:";
    private static final String FREEBASE_WIKI_EN              = "/wikipedia/en_id/";
    private static final String FREEBASE_NAME_ATTRIBUTE_VALUE = "name";
    private static final String FREEBASE_PROPERTY             = "property";
    private static final String FREEBASE_TEXT                 = "text";
    private static final String FREEBASE_VALUES               = "values";

    private static final String FREEBASE_ATTRIBUTION = "/common/image/rights_holder_text_attribution";
    private static final String FREEBASE_LICENSE = "/common/licensed_object/license";

    private static final String RESULT_CODE_OK      = "200 OK";
    private static final int    RESULT_CODE_UNKNOWN = 403;
    private static final int    RESULT_CODE_BAD     = 400;

    private static final int numFakeTopics        = 200;
    private static final int categoryCursorLimit  = 200;
    private static final int lettersPerCategory   = 5;
    private static final int maxAttemptsPerLetter = 10;

    private static final float categoryQuestionThreshold = 0.70f;

    private static ManagerFreebase mInstance;

    private int                  mProgressStatus;
    private QuestionTask         mQuestionTask;
    private FakeAnswerTask       mFakeAnswerTask;
    private int                  mNameAttributeId;
    private List<Listener>       mListeners;
    private int                  mTopicsPerCategory;
    private int                  mCursor;
    private ArrayList<String>    mBlacklistTerms;
    private ArrayList<TopicInfo> mTopicImagesToDownload;

    private ManagerFreebase()
    {
        mNameAttributeId = ManagerDB.instance().getNamePropertyId();
        mListeners = new CopyOnWriteArrayList<Listener>();
        mTopicsPerCategory = ManagerDB.instance().getSettings().increment;
        mCursor = 0;
        mBlacklistTerms = ManagerDB.instance().getBlackListTerms();
        mTopicImagesToDownload = new ArrayList<TopicInfo>();
    }

    public static ManagerFreebase instance()
    {
        if (mInstance == null) mInstance = new ManagerFreebase();
        return mInstance;
    }

    public void addListener(Listener listener)
    {
        mListeners.add(listener);
    }

    public boolean removeListener(Listener listener)
    {
        return mListeners.remove(listener);
    }

    private void notifyUpdateInitiated()
    {
        for (Listener l : mListeners)
        {
            l.onUpdateQuestionsInitiated();
        }
    }

    private void notifyUpdateInProgress(int progress)
    {
        for (Listener l : mListeners)
        {
            l.onUpdateQuestionsInProgress(progress);
        }
    }

    private void notifyUpdateCancelled()
    {
        for (Listener l : mListeners)
        {
            l.onUpdateQuestionsCancelled();
        }
    }

    private void notifyUpdateComplete()
    {
        for (Listener l : mListeners)
        {
            l.onUpdateQuestionsComplete();
        }
    }

    private void notifyTestConnectionComplete(boolean aBoolean)
    {
        for(Listener l: mListeners)
        {
            l.onUpdateTestConnectionComplete(aBoolean);
        }
    }

    public void createQuestionBank()
    {
        ModelSettings mSettings = ManagerDB.instance().getSettings();
        mTopicsPerCategory = mSettings.increment;
        mCursor = mSettings.increment * mSettings.updates;
        mQuestionTask = new QuestionTask();
        mQuestionTask.execute();
    }

    public void stopQuestionRetrieval()
    {
        mQuestionTask.cancel(true);
    }

    public void generateFakeAnswers()
    {
        mFakeAnswerTask = new FakeAnswerTask();
        mFakeAnswerTask.execute();
    }

    public void stopGenerateFakeAnswers()
    {
        mFakeAnswerTask.cancel(true);
    }

    public void hasActiveInternetConnection(Context context)
    {
        if (isOnline(context))
        {
            new TestConnectonTask().execute();
        }
        else
        {
            if (AppConfig.DEBUG) Log.i("ManagerFreebase-hasActiveInternetConnection","No network available!");
        }
    }

    public boolean isOnline(Context ct)
    {
        ConnectivityManager cm = (ConnectivityManager)ct.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting())
        {
            return true;
        }
        return false;
    }

    public boolean isOnWiFi(Context ct)
    {
        ConnectivityManager cm = (ConnectivityManager)ct.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting())
        {
            return (netInfo.getType() == ConnectivityManager.TYPE_WIFI);
        }
        return false;
    }

    private class TestConnectonTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        protected void onPostExecute(Boolean aBoolean)
        {
            super.onPostExecute(aBoolean);
            notifyTestConnectionComplete(aBoolean);
        }

        protected Boolean doInBackground(Void... params)
        {
            boolean connected = false;
            HttpURLConnection urlc = null;
            try
            {
                URL url = new URL("http://www.google.com");

                urlc = (HttpURLConnection) url.openConnection();
                urlc.setRequestProperty("User-Agent", "Test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(10000);
                urlc.connect();

                connected = (urlc.getResponseCode() == 200);

            }
            catch (IOException e)
            {
                if (AppConfig.DEBUG) Log.i("ManagerFreebase-testConnection","Can't connect to google");
            }
            finally
            {
                if(urlc != null)
                {
                    try
                    {
                        urlc.disconnect();
                    }
                    catch (Exception e)
                    {

                    }
                }
            }

            return connected;
        }
    }

    private class QuestionTask extends AsyncTask<Void, Integer, Long>
    {
        protected void onPreExecute()
        {
            notifyUpdateInitiated();
            mProgressStatus = 0;
//            ManagerReport.instance().resetReport();
        }

        protected Long doInBackground(Void... params)
        {
            notifyUpdateInProgress(0);

//            standardAlgorithm();
            priorityAlgorithm();
            return Long.MIN_VALUE;
        }

        protected void onProgressUpdate(Integer... progress)
        {
            if(!isCancelled())
            {
                notifyUpdateInProgress(progress[0]);
                mProgressStatus++;
            }
        }

        protected void onPostExecute(Long result)
        {
            // Increment the update field on question update complete
            ModelSettings settings = ManagerDB.instance().getSettings();
            settings.updates++;
            ManagerDB.instance().updateSettings(settings);
            notifyUpdateComplete();
//            ManagerReport.instance().outputReport();
//            ManagerReport.instance().resetReport();
        }

        protected void onCancelled()
        {
            notifyUpdateCancelled();
//            ManagerReport.instance().resetReport();
            if (AppConfig.DEBUG) Log.d("ManagerFreebase-onCancelled", "Thread Id: " + Thread.currentThread().getId());
        }

        // Generates all Questions based on the category and the specified attributes of the category
        // The objects are the JSON response from the initial query
        private int generateQuestions(ModelCategory category, List<ModelCategoryProperty> attributes, JSONObject objects, int limit)
        {
            int validTopics = 0;

            if(objects != null)
            {
                try
                {
                    JSONArray topics = objects.getJSONArray(RESULT);
//                    ManagerReport.instance().addCategory(category.name, topics.length(), attributes);
                    for (int i = 0; i < topics.length(); i++)
                    {
                        if (isCancelled() || validTopics >= limit)
                        {
                            return validTopics;
                        }

                        JSONObject topic = topics.getJSONObject(i);

                        // Get topic name and mid
                        String name = topic.getString(FREEBASE_NAME);
                        String mid = topic.getString(FREEBASE_MID);

//                        ManagerReport.instance().addTopic(category.name, mid);

                        if (mBlacklistTerms.contains(name.toLowerCase()))
                        {
//                            ManagerReport.instance().setTopicStatus(category.name,
//                                    mid, ModelCategoryStatistic.Status.FAILED_BLACKLIST);

                            if (AppConfig.DEBUG)
                                Log.w("ManagerFreebase-generateQuestions", "Blacklisted topic: " + name);

                            publishProgress(mProgressStatus);
                            continue;
                        }

                        // Get objects within the output tag
                        JSONObject output = topic.getJSONObject(OUTPUT);

                        // Get WikiId
                        String wikiId = getWikiAttribute(output);

                        if (wikiId == null)
                        {
//                            ManagerReport.instance().setTopicStatus(category.name,
//                                    mid, ModelCategoryStatistic.Status.FAILED_WIKI);
                            if (AppConfig.DEBUG)
                                Log.d("ManagerFreebase-generateQuestions", "Mid " + mid + " WikiId " + wikiId);

                            publishProgress(mProgressStatus);
                        }
                        else
                        {
                            // Check if topic is already in DB
                            long topicRowId = ManagerDB.instance().getTopicId(mid);

                            // Topic already exists
                            if (topicRowId > 0)
                            {
                                // Create answer for attributes
                                boolean questionCreated = createAttributeQuestions(category, attributes, output, topicRowId, mid);

                                if (questionCreated)
                                    validTopics++;
                            }
                            else
                            {
                                String imageMid = getTopicImageMid(output);
                                ImageAttributionInfo attributionInfo = null;

                                if (imageMid != null) {
                                    attributionInfo = getTopicImageAttribution(imageMid);
                                }

                                if (attributionInfo == null) {
                                    topicRowId = ManagerDB.instance().createTopic(mid, wikiId, name, null, null);
                                } else {
                                    topicRowId = ManagerDB.instance().createTopic(mid, wikiId, name, attributionInfo.author, attributionInfo.license);
                                }
                                // Create topicToCategory in DB
                                if (topicRowId > 0)
                                {
                                    String imageFreebaseUrl = getTopicImageFreebaseUrl(output);
                                    if (imageFreebaseUrl != null)
                                        mTopicImagesToDownload.add(new TopicInfo(mid, wikiId, imageFreebaseUrl));
                                    else
                                        mTopicImagesToDownload.add(new TopicInfo(mid, wikiId, ""));

                                    // Create answer for name question
                                    if (ManagerDB.instance().doesCategoryHasNameProperty(category.id))
                                        ManagerDB.instance().createAnswer(name, mNameAttributeId, ModelCategoryProperty.Type.STRING, topicRowId, category.id);

                                    // Create answer for attributes
                                    createAttributeQuestions(category, attributes, output, topicRowId, mid);

                                    validTopics++;
                                }
                            }
                        }

                    }
                }
                catch (JSONException e)
                {
                    if (AppConfig.DEBUG) Log.e("ManagerFreebase-generateQuestions", "Error parsing data " + e.toString());
                }
            }

            return limit;
        }

        // Gets the wiki id associated with the output section of the current topic
        // Returns the id if parsed correctly
        String getWikiAttribute(JSONObject output)
        {
            String wikiId = null;
            JSONObject wikiWrapper = null;

            // Get Wikipedia Id from output tag
            try
            {
                wikiWrapper = output.getJSONObject(FREEBASE_KEY + FREEBASE_WIKI_EN);
            }
            catch (JSONException e)
            {
                if (AppConfig.DEBUG)
                    Log.e("ManagerFreebase-getWikiAttribute", "Error parsing Wiki Wrapper " + e.toString());
            }

            if(wikiWrapper != null && wikiWrapper.length() > 0)
            {
                JSONArray wikiArrray = wikiWrapper.optJSONArray(FREEBASE_OBJECT_KEY);
                if (wikiArrray.length() > 0)
                {
                    try
                    {
                        wikiId = (wikiArrray.getString(0)).substring(FREEBASE_WIKI_EN.length());
                    }
                    catch(JSONException e)
                    {
                        if (AppConfig.DEBUG)
                            Log.e("ManagerFreebase-getWikiAttribute", "Error parsing Wiki Array " + e.toString());
                    }
                }
            }

            return wikiId;
        }

        // Gets the image mid associated with the output section of the current topic
        // Attempts to download the image via ManagerMedia
        // Returns whether the image downloaded correctly
        String getTopicImageFreebaseUrl(JSONObject output)
        {
            String imageUrl = null;
            JSONObject imageWrapper = null;

            try
            {
                imageWrapper = output.getJSONObject(FREEEBASE_COMMON_IMAGE);
            }
            catch(JSONException e)
            {
                if (AppConfig.DEBUG) Log.e("ManagerFreebase-getTopicImage", "Error parsing Image Wrapper " + e.toString());
            }

            if(imageWrapper != null && imageWrapper.length() > 0)
            {
                JSONArray images = null;

                try
                {
                    images = imageWrapper.getJSONArray(FREEEBASE_COMMON_IMAGE);
                }
                catch (JSONException e)
                {
                    if (AppConfig.DEBUG) Log.e("ManagerFreebase-getTopicImage", "Error parsing Image Array " + e.toString());
                }

                // Check if there are any associated images with the topic, if not skip the entry
                if (images != null &&images.length() > 0)
                {
                    try
                    {
                        String imageMid = images.getJSONObject(0).getString(FREEBASE_MID);
                        if (imageMid != null)
                        {
                            imageUrl = IMAGE_API_URL + imageMid + IMAGE_API_MAX_SIZE_PARAMS + "&key=" + API_KEY;
                        }
                    }
                    catch(JSONException e)
                    {
                        if (AppConfig.DEBUG) Log.e("ManagerFreebase-getTopicImage", "Error parsing Image Mid " + e.toString());
                    }
                }
            }

            return imageUrl;
        }

        // Gets the image mid associated with the output section of the current topic
        // Attempts to download the image via ManagerMedia
        // Returns whether the image downloaded correctly
        String getTopicImageMid(JSONObject output)
        {
            String imageMid = null;
            JSONObject imageWrapper = null;

            try
            {
                imageWrapper = output.getJSONObject(FREEEBASE_COMMON_IMAGE);
            }
            catch(JSONException e)
            {
                if (AppConfig.DEBUG) Log.e("ManagerFreebase-getTopicImageMid", "Error parsing Image Wrapper " + e.toString());
            }

            if(imageWrapper != null && imageWrapper.length() > 0)
            {
                JSONArray images = null;

                try
                {
                    images = imageWrapper.getJSONArray(FREEEBASE_COMMON_IMAGE);
                }
                catch (JSONException e)
                {
                    if (AppConfig.DEBUG) Log.e("ManagerFreebase-getTopicImageMid", "Error parsing Image Array " + e.toString());
                }

                // Check if there are any associated images with the topic, if not skip the entry
                if (images != null &&images.length() > 0)
                {
                    try
                    {
                        imageMid = images.getJSONObject(0).getString(FREEBASE_MID);
                    }
                    catch(JSONException e)
                    {
                        if (AppConfig.DEBUG) Log.e("ManagerFreebase-getTopicImageMid", "Error parsing Image Mid " + e.toString());
                    }
                }
            }

            return imageMid;
        }

        ImageAttributionInfo getTopicImageAttribution(String mid) {
            ImageAttributionInfo attributionInfo = new ImageAttributionInfo();
            JSONObject topicInfo = getTopicInformation(mid);

            try {
                JSONObject propertyArray = topicInfo.getJSONObject(FREEBASE_PROPERTY);

                // Attribution info
                JSONObject attribution = propertyArray.getJSONObject(FREEBASE_ATTRIBUTION);
                JSONArray attributionValuesArray = attribution.getJSONArray(FREEBASE_VALUES);
                JSONObject attributionValues = attributionValuesArray.getJSONObject(0);
                String author = attributionValues.getString(FREEBASE_TEXT);
                if (author != null )
                {
                    attributionInfo.author = author;
                }

                // License info
                JSONObject licenseInfo = propertyArray.getJSONObject(FREEBASE_LICENSE);
                JSONArray licenseValuesArray = licenseInfo.getJSONArray(FREEBASE_VALUES);
                JSONObject licenseValues = licenseValuesArray.getJSONObject(0);
                String license = licenseValues.getString(FREEBASE_TEXT);
                if (license != null) {
                    attributionInfo.license = license;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return  attributionInfo;
        }

        // Creates all answers specified by the attributes from the output of the associated topic
        boolean createAttributeQuestions(ModelCategory category, List<ModelCategoryProperty> attributes, JSONObject output, long topicRowId, String mid)
        {
            boolean questionCreated = false;
            // Loop through all attributes and get first entry of types
            // with array. Attributes of a type will always include an array of objects
            // within the attribute with the same name.
            for (ModelCategoryProperty attribute : attributes)
            {
                String fullAttributeStr;
                if (!attribute.name.startsWith("/"))
                    fullAttributeStr = category.name + "/" + attribute.name;
                else
                    fullAttributeStr = attribute.name;

                JSONObject outerJsonAttribute = null;

                try
                {
                    outerJsonAttribute = output.getJSONObject(fullAttributeStr);
                }
                catch (JSONException e)
                {
                    if (AppConfig.DEBUG) Log.e("ManagerFreebase-createAttributeQuestions", "Error parsing outer json attribute " + e.toString());
                }

                // Check if the field is a reverse field (an attribute directing to another)
                // Adjust the string to remove the redirect and use the real attribute
                int index = fullAttributeStr.indexOf('.');
                String adjustedAttString = (index > 0) ? fullAttributeStr.substring(index + 1, fullAttributeStr.length()): fullAttributeStr;

                if (outerJsonAttribute != null && outerJsonAttribute.has(adjustedAttString))
                {
                    // Just get the first element in the Array for the answers for now
                    JSONArray innerJsonAttributeArray = null;
                    JSONObject firstValue = null;

                    try
                    {
                        innerJsonAttributeArray = outerJsonAttribute.getJSONArray(adjustedAttString);
                        firstValue = innerJsonAttributeArray.optJSONObject(0);
                    }
                    catch(JSONException e)
                    {
                        if (AppConfig.DEBUG) Log.e("ManagerFreebase-createAttributeQuestions", "Error parsing inner json attribute " + e.toString());
                    }

                    String attributeValue = "";

                    // If the first value is a JSONObject get the name portion of the object otherwise
                    // use the string.
                    try
                    {
                        if (!(firstValue == null))
                            attributeValue = firstValue.getString(FREEBASE_NAME_ATTRIBUTE_VALUE);
                        else
                            attributeValue = innerJsonAttributeArray.getString(0);

//                        ManagerReport.instance().setTopicAttributeValid(category.name, mid, attribute);

                        long answerId = ManagerDB.instance().createAnswer(attributeValue, attribute.id, attribute.type, topicRowId, category.id);

                        if (answerId > 0 && !questionCreated)
                            questionCreated = true;
                    }
                    catch(JSONException e)
                    {
                        if (AppConfig.DEBUG) Log.e("ManagerFreebase-createAttributeQuestions", "Error adding attribute answer" + e.toString());
                    }
                }
            }

            return questionCreated;
        }

        // Standard question fetch algorithm. Goes through all categories and generates questions based on
        // the associated attributes for the category.
        private void standardAlgorithm()
        {
            // Grab all categories and loop through all properties per category, appending them to the
            // output string. Query is run per category(64 times).
            for (ModelCategory category: ManagerDB.instance().getCategories())
            {
                if (AppConfig.DEBUG)
                    Log.d("ManagerFreebase-standardAlgorithm", "Category: " + category.name);

                if(isCancelled())
                {
                    break;
                }

                String output = "";
                ArrayList<ModelCategoryProperty> attributes = ManagerDB.instance().getCategoryProperties(category.id);

                for (ModelCategoryProperty categoryProperty: attributes)
                {
                    String attributeStr;
                    if (!categoryProperty.name.startsWith("/"))
                    {
                        attributeStr = category.name + "/" + categoryProperty.name + " ";
                    }
                    else
                    {
                        attributeStr = categoryProperty.name + " ";
                    }

                    if (categoryProperty.filter != ModelCategoryProperty.Filter.NONE)
                        attributeStr = " (" + attributeStr + L_PAREN + FILTER + " " + FREEBASE_TYPE + ModelCategoryProperty.Filters[categoryProperty.filter.ordinal()] + R_PAREN + R_PAREN + " ";

                    output += attributeStr;
                }

                generateQuestions(category, attributes, generateQuery("", category.name, mCursor, mTopicsPerCategory, output, API_TYPE_SEARCH, false), mTopicsPerCategory);

                downloadImages(category.name);
            }
        }

        private void priorityAlgorithm()
        {
            ArrayList<ModelCategory> categories        =  ManagerDB.instance().getCategories();
            ArrayList<ModelCategory> categoriesTracker = new ArrayList<ModelCategory>();
            int maxTopics                              = categories.size() * mTopicsPerCategory;
            int[] distribution                         = new int[categories.size()];

            // Distribute all topics to download according to usage
            distributeTopics(categories, categoriesTracker, maxTopics, distribution);

            // Grab all categories and loop through all properties per category, appending them to the
            // output string. Query is run per category(64 times).
            for (ModelCategory category: ManagerDB.instance().getCategories())
            {
                if (AppConfig.DEBUG)
                    Log.d("ManagerFreebase-priorityAlgorithm", "Category: " + category.name);

                if (isCancelled())
                    break;

                String output = "";
                ArrayList<ModelCategoryProperty> attributes = ManagerDB.instance().getCategoryProperties(category.id);

                // Grab all attributes from a category to build output string
                for (ModelCategoryProperty categoryProperty: attributes)
                {
                    String attributeStr;
                    if (!categoryProperty.name.startsWith("/"))
                        attributeStr = category.name + "/" + categoryProperty.name + " ";
                    else
                        attributeStr = categoryProperty.name + " ";

                    if (categoryProperty.filter != ModelCategoryProperty.Filter.NONE)
                        attributeStr = " (" + attributeStr + L_PAREN + FILTER + " " + FREEBASE_TYPE + ModelCategoryProperty.Filters[categoryProperty.filter.ordinal()] + R_PAREN + R_PAREN + " ";

                    output += attributeStr;
                }

                int categoryCursor = ManagerDB.instance().getCategoryQueryCursor(category.id);
                int categoryDistribution = distribution[category.id - 1];

                // Update cursor for the category
                ManagerDB.instance().updateCategoryCursor(category.id, categoryCursor + categoryDistribution);

                // Clear any old topics for image download
                mTopicImagesToDownload.clear();

                // If the cursor is above 200, use the alternative algorithm
                if (categoryCursor >= categoryCursorLimit)
                {
                    if (AppConfig.DEBUG)
                        Log.d("ManagerFreebase-priorityAlgorithm", "Using alternative query");

                    alternativeQueryAlgorithm(categoryDistribution, category, output, attributes);
                }
                else if (categoryCursor + categoryDistribution > categoryCursorLimit)
                {
                    if (AppConfig.DEBUG)
                        Log.d("ManagerFreebase-priorityAlgorithm", "Using both query types");

                    int topicsLeft = categoryCursorLimit - categoryCursor;
                    standardQueryAlgorithm(topicsLeft, category, categoryCursor, output, attributes);
                    alternativeQueryAlgorithm(categoryDistribution - topicsLeft, category, output, attributes);
                }
                else
                {
                    if (AppConfig.DEBUG)
                        Log.d("ManagerFreebase-priorityAlgorithm", "Using standard query");

                    standardQueryAlgorithm(categoryDistribution, category, categoryCursor, output, attributes);
                }
            }
        }

        private void distributeTopics(ArrayList<ModelCategory> categories, ArrayList<ModelCategory> categoriesTracker, int maxTopics, int[] distribution)
        {
            long currentTime = System.currentTimeMillis();

            // initialize all categories to get 1 topic
            for (int i = 0; i < categories.size(); i ++)
            {
                distribution[i] = 1;
                maxTopics--;
            }

            boolean allComplete = false;
            while (maxTopics > 0)
            {
                categoriesTracker.clear();
                categoriesTracker.addAll(categories);
                Iterator<ModelCategory> i = categoriesTracker.iterator();

                while (!categoriesTracker.isEmpty())
                {
                    if (maxTopics == 0)
                        break;

                    while(i.hasNext())
                    {
                        ModelCategory category = i.next();

                        // Category isn't marked as complete
                        int propertyCount   = ManagerDB.instance().getCategoryPropertiesCount(category.id);
                        int unusedQuestions = ManagerDB.instance().getCategoryQuestionCount(category.id, 0);
                        int usedQuestions   = ManagerDB.instance().getCategoryQuestionCount(category.id, 1);
                        int totalQuestions  = unusedQuestions + usedQuestions;
                        int newQuestions    = (distribution[category.id - 1] * propertyCount);
                        int newUnusedQuestions = unusedQuestions + newQuestions;
                        int newTotalQuestions  = totalQuestions + newQuestions;
                        float ratio         = 0;

                        if (totalQuestions > 0)
                            ratio = (float)newUnusedQuestions / (float)newTotalQuestions;

                        // Category needs more questions
                        if (ratio < categoryQuestionThreshold || allComplete)
                        {
                            distribution[category.id - 1]++;

                            if (maxTopics > 0)
                                maxTopics--;
                            else
                                break;
                        }
                        else
                        {
                            i.remove();
                        }
                    }

                    i = categoriesTracker.iterator();
                }

                allComplete = true;
            }

            // Log total time taken
            long totalTime = System.currentTimeMillis() - currentTime;
            if (AppConfig.DEBUG)
                Log.d("distributeTopics", "Seconds: " + totalTime/1000);

            // Log distribution
            for (int i = 0; i < categories.size(); i ++)
            {
                int propertyCount      = ManagerDB.instance().getCategoryPropertiesCount(i+1);
                int unusedQuestions    = ManagerDB.instance().getCategoryQuestionCount(i+1, 0);
                int usedQuestions      = ManagerDB.instance().getCategoryQuestionCount(i+1, 1);
                int totalQuestions     = unusedQuestions + usedQuestions;
                int newQuestions       = (distribution[i] * propertyCount);
                int newUnusedQuestions = unusedQuestions + newQuestions;
                int newTotalQuestions  = totalQuestions + newQuestions;
                int categoryId        = i + 1;

                if (AppConfig.DEBUG)
                    Log.d("category id: " + categoryId , " topics: " + distribution[i] + " oldRatio: " + unusedQuestions + "/" + totalQuestions  + " newRatio: " + newUnusedQuestions + "/" + newTotalQuestions);
            }
        }

        private void alternativeQueryAlgorithm(int categoryDistribution, ModelCategory category, String output, ArrayList<ModelCategoryProperty> attributes)
        {
            ModelAlphaCursor alphaCursor               = ModelAlphaCursor.open();
            int[] alphaCursors                         = new int[lettersPerCategory];
            int letterLimit                            = (categoryDistribution / lettersPerCategory) > 0 ? (categoryDistribution / lettersPerCategory) : 1;

            // Loop through the letterPerCategory and select appropriate letter to query
            for (int i = 0; i < alphaCursors.length; i++)
            {
                // Get a random letter
                int letter       = -1;
                int letterCursor = categoryCursorLimit;
                int counter      = 0;

                // Find a letter with available topics to query
                do
                {
                    if (counter > maxAttemptsPerLetter)
                    {
                        letter = -1;
                        break;
                    }
                    letter =  (int)(Math.random() * 26f);
                    letterCursor = alphaCursor.getCursor(category.id - 1, letter);
                    counter++;
                }
                while (letterLimit > (categoryCursorLimit - letterCursor));

                alphaCursors[i] = letter;
            }

            // Loop through all letters and generate query based on the letter limit
            for (int i = 0; i < alphaCursors.length; i++)
            {
                if (alphaCursors[i] != -1)
                {
                    int queryCursor = alphaCursor.getCursor(category.id - 1, alphaCursors[i]);
                    int bufferLimit = queryCursor + (letterLimit * 2);
                    int limit = bufferLimit > categoryCursorLimit ? letterLimit : letterLimit * 2;

                    char c = 'a';
                    c += alphaCursors[i];
                    String letter = String.valueOf(c);
                    // Run query
                    JSONObject topics = generateQuery(letter, category.name, queryCursor, limit, output, API_TYPE_SEARCH, true);

                    if (topics != null)
                    {
                        // Set the new cursor of the category
                        alphaCursor.setCursor(category.id - 1, alphaCursors[i], queryCursor + generateQuestions(category, attributes, topics, letterLimit));

                        // Download images
                        downloadImages(category.name);
                    }
                }
            }

            alphaCursor.close();
        }

        private void standardQueryAlgorithm(int categoryDistribution, ModelCategory category, int categoryCursor, String output, ArrayList<ModelCategoryProperty> attributes)
        {
            int bufferLimit = categoryCursor + (categoryDistribution * 2);
            int limit = bufferLimit > categoryCursorLimit ? categoryDistribution : categoryDistribution * 2;

            JSONObject topics = generateQuery("", category.name, categoryCursor, limit, output, API_TYPE_SEARCH, false);

            // Got the results download as normal otherwise we reached the limit, move to alphabet algorithm
            if (topics != null)
            {
                generateQuestions(category, attributes, topics, categoryDistribution);
                downloadImages(category.name);
            }
            else
            {
                alternativeQueryAlgorithm(categoryDistribution, category, output, attributes);
            }
        }

        private void downloadImages(String categoryName)
        {
            if (mTopicImagesToDownload.size() > 0)
            {
                ManagerMedia.instance().getWikipediaImageUrls(mTopicImagesToDownload);

                for (TopicInfo topic : mTopicImagesToDownload)
                {
                    handleImageDownload(categoryName, topic);
                    publishProgress(mProgressStatus);
                }
            }
        }

        // Attempts to download form wikipedia, if not fallsback on freebase. If neither works
        // the topic is removed from the DB.
        private void handleImageDownload(String categoryName, TopicInfo topic)
        {
            if (!ManagerMedia.instance().downloadImage(topic.imageUrl, topic.mid))
            {
                if(topic.freebaseUrl != null)
                {
                    if (!ManagerMedia.instance().downloadImage(topic.freebaseUrl, topic.mid))
                    {
//                        ManagerReport.instance().setTopicStatus(categoryName,
//                                topic.mid, ModelCategoryStatistic.Status.FAILED_IMAGE);
                        ManagerDB.instance().removeTopic(topic.mid);

                        if (AppConfig.DEBUG)
                            Log.d("ManagerFreebase-standardAlgorithm",
                                    "Couldn't download url:" + topic.imageUrl +
                                            " for mid:" + topic.mid
                            );
                    }
                    else
                    {
//                        ManagerReport.instance().setTopicStatus(categoryName,
//                                topic.mid, ModelCategoryStatistic.Status.SUCCESS);

                        if (AppConfig.DEBUG)
                            Log.d("ManagerFreebase-standardAlgorithm",
                                    "Downloaded from freebase!");
                    }
                }
                else
                {
//                    ManagerReport.instance().setTopicStatus(categoryName,
//                            topic.mid, ModelCategoryStatistic.Status.FAILED_IMAGE);
                    ManagerDB.instance().removeTopic(topic.mid);

                    if (AppConfig.DEBUG)
                        Log.d("ManagerFreebase-standardAlgorithm",
                                "No wiki, no freebase image.");
                }
            }
        }

        // Create a new query based on the filter, limit, output and search type properties
        private JSONObject generateQuery(String query, String filter, int cursor,  int limit, String output, String searchType, boolean prefix)
        {
            InputStream is = null;
            JSONObject json = null;
            String response = null;

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(QUERY, query));
            // Fix this later to just take the filter, could have multiple types with different algorithms
            params.add(new BasicNameValuePair(FILTER, L_PAREN + FILTER_ANY + " " + FREEBASE_TYPE + filter + R_PAREN));
            params.add(new BasicNameValuePair(OUTPUT, L_PAREN + FREEBASE_KEY + FREEBASE_WIKI_EN + " " + FREEEBASE_COMMON_IMAGE + " " + output + R_PAREN));
            params.add(new BasicNameValuePair(LIMIT, String.valueOf(limit)));
            params.add(new BasicNameValuePair(CURSOR, String.valueOf(cursor)));
            params.add(new BasicNameValuePair(KEY, API_KEY));
            if (prefix)
                params.add(new BasicNameValuePair(PREFIXED, "true"));

            String url = API_URL + searchType + "?" + URLEncodedUtils.format(params, "utf-8");

            if (AppConfig.DEBUG) Log.v("ManagerFreebase-generateQuery", "Query URL " + url);

            // Making the HTTP request
            try
            {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);

                if (httpGet != null)
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
                if (AppConfig.DEBUG) Log.e("ManagerFreebase-generateQuery", "Error converting result " + e.toString());
            }

            try
            {
                if(response != null)
                {
                    json = new JSONObject(response);
                    if (!(json.optString(ERROR)).isEmpty())
                    {
                        if (AppConfig.DEBUG) Log.w("ManagerFreebase-generateQuery", "Errors returned");
                        json = null;
                    }
                    else if (json.optString(RESULT).isEmpty())
                    {
                        if (AppConfig.DEBUG)Log.w("ManagerFreebase-generateQuery", "Empty result set");
                        json = null;
                    }
                }

            }
            catch (JSONException e)
            {
                if (AppConfig.DEBUG) Log.e("ManagerFreebase-generateQuery", "Error parsing data " + e.toString());
            }

            // return JSON String
            return json;
        }

        private JSONObject getTopicInformation(String mid) {
            InputStream is = null;
            JSONObject json = null;
            String response = null;
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            // Fix this later to just take the filter, could have multiple types with different algorithms
            params.add(new BasicNameValuePair(KEY, API_KEY));

            String urlString = TOPIC_API_URL + mid + "?" + URLEncodedUtils.format(params, "utf-8");
            URL url;
            if (AppConfig.DEBUG) Log.v("ManagerFreebase-getTopicInformation", "Query URL " + urlString);

            // Making the HTTP request
            try
            {
                url = new URL(urlString);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                is = connection.getInputStream();
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
                if (AppConfig.DEBUG) Log.e("ManagerFreebase-getTopicInformation", "Error converting result " + e.toString());
            }

            try
            {
                if(response != null)
                {
                    json = new JSONObject(response);
                }

            }
            catch (JSONException e)
            {
                if (AppConfig.DEBUG) Log.e("ManagerFreebase-getTopicInformation", "Error parsing data " + e.toString());
            }

            // return JSON String
            return json;
        }
    }

    public static HttpClient createHttpClient()
    {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);

        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

        return new DefaultHttpClient(conMgr, params);
    }

    public class TopicInfo
    {
        public String mid;
        public String wikiId;
        public String freebaseUrl;
        public String imageUrl;

        TopicInfo(String mid, String wikiId, String freebaseUrl)
        {
            this.mid = mid;
            this.wikiId = wikiId;
            this.freebaseUrl = freebaseUrl;
        }

        TopicInfo(String mid, String wikiId, String freebaseUrl, String imageUrl)
        {
            this.mid = mid;
            this.wikiId = wikiId;
            this.freebaseUrl = freebaseUrl;
            this.imageUrl = imageUrl;
        }
    }

    public class ImageAttributionInfo {
        public String author = null;
        public String license = null;

        public ImageAttributionInfo() {

        }

        public ImageAttributionInfo(String author, String license) {
            this.author = author;
            this.license = license;
        }
    }

    private class FakeAnswerTask extends AsyncTask<Void, Integer, Long>
    {
        @Override
        protected void onPreExecute()
        {
            notifyUpdateInitiated();
        }

        protected Long doInBackground(Void... params)
        {
            standardAlgorithm();

            return Long.MIN_VALUE;
        }

        protected void onPostExecute(Long result)
        {
            notifyUpdateComplete();
        }

        // Generates all Questions based on the category and the specified attributes of the category
        // The objects are the JSON response from the initial query
        private void generateQuestions(ModelCategory category, List<ModelCategoryProperty> attributes, JSONObject objects)
        {
            if(objects != null)
            {
                try
                {
                    JSONArray topics = objects.getJSONArray(RESULT);

                    for (int i = 0; i < topics.length(); i++)
                    {
                        if (isCancelled())
                        {
                            break;
                        }

                        JSONObject topic = topics.getJSONObject(i);

                        // Get topic name and mid
                        String name = topic.getString(FREEBASE_NAME);

                        if (mBlacklistTerms.contains(name.toLowerCase()))
                        {
                            continue;
                        }

                        // Create answer for name question
                        if (ManagerDB.instance().doesCategoryHasNameProperty(category.id))
                            ManagerDB.instance().createFakeAnswer(name, mNameAttributeId, category.id, ModelCategoryProperty.Type.STRING);

                        // Create answer for attributes
                        createAttributeQuestions(category, attributes, topic.getJSONObject(OUTPUT));
                    }
                }
                catch (JSONException e)
                {
                    if (AppConfig.DEBUG) Log.e("FakeAnswerTask-generateQuestions", "Error parsing data " + e.toString());
                }
            }
        }

        // Creates all answers specified by the attributes from the output of the associated topic
        void createAttributeQuestions(ModelCategory category, List<ModelCategoryProperty> attributes, JSONObject output)
        {
            // Loop through all attributes and get first entry of types
            // with array. Attributes of a type will always include an array of objects
            // within the attribute with the same name.
            for (ModelCategoryProperty attribute : attributes)
            {
                String fullAttributeStr;
                if (!attribute.name.startsWith("/"))
                {
                    fullAttributeStr = category.name + "/" + attribute.name;
                }
                else
                {
                    fullAttributeStr = attribute.name;
                }

                JSONObject outerJsonAttribute = null;

                try
                {
                    outerJsonAttribute = output.getJSONObject(fullAttributeStr);
                }
                catch (JSONException e)
                {
                    if (AppConfig.DEBUG) Log.e("FakeAnswerTask-createAttributeQuestions", "Error parsing outer json attribute " + e.toString());
                }

                // Check if the field is a reverse field (an attribute directing to another)
                // Adjust the string to remove the redirect and use the real attribute
                int index = fullAttributeStr.indexOf('.');
                String adjustedAttString = (index > 0) ? fullAttributeStr.substring(index + 1, fullAttributeStr.length()): fullAttributeStr;

                if (outerJsonAttribute != null && outerJsonAttribute.has(adjustedAttString))
                {
                    // Just get the first element in the Array for the answers for now
                    JSONArray innerJsonAttributeArray = null;
                    JSONObject firstValue = null;

                    try
                    {
                        innerJsonAttributeArray = outerJsonAttribute.getJSONArray(adjustedAttString);
                        firstValue = innerJsonAttributeArray.optJSONObject(0);
                    }
                    catch(JSONException e)
                    {
                        if (AppConfig.DEBUG) Log.e("FakeAnswerTask-createAttributeQuestions", "Error parsing inner json attribute " + e.toString());
                    }

                    String attributeValue = "";

                    // If the first value is a JSONObject get the name portion of the object otherwise
                    // use the string.
                    try
                    {
                        if (!(firstValue == null))
                            attributeValue = firstValue.getString(FREEBASE_NAME_ATTRIBUTE_VALUE);
                        else
                            attributeValue = innerJsonAttributeArray.getString(0);

                        ManagerDB.instance().createFakeAnswer(attributeValue, attribute.id, category.id, attribute.type);
                    }
                    catch(JSONException e)
                    {
                        if (AppConfig.DEBUG) Log.e("FakeAnswerTask-createAttributeQuestions", "Error adding attribute answer" + e.toString());
                    }
                }
            }
        }

        // Standard question fetch algorithm. Goes through all categories and generates questions based on
        // the associated attributes for the category.
        private void standardAlgorithm()
        {
            // Grab all categories and loop through all properties per category, appending them to the
            // output string. Query is run per category(64 times).
            for (ModelCategory category: ManagerDB.instance().getCategories())
            {
                if(isCancelled())
                {
                    break;
                }

                String output = "";
                ArrayList<ModelCategoryProperty> attributes = ManagerDB.instance().getCategoryProperties(category.id);

                for (ModelCategoryProperty categoryProperty: attributes)
                {
                    String attributeStr;
                    if (!categoryProperty.name.startsWith("/"))
                    {
                        attributeStr = category.name + "/" + categoryProperty.name + " ";
                    }
                    else
                    {
                        attributeStr = categoryProperty.name + " ";
                    }

                    if (categoryProperty.filter != ModelCategoryProperty.Filter.NONE)
                        attributeStr = " (" + attributeStr + L_PAREN + FILTER + " " + FREEBASE_TYPE + ModelCategoryProperty.Filters[categoryProperty.filter.ordinal()] + R_PAREN + R_PAREN + " ";

                    output += attributeStr;
                }

                generateQuestions(category, attributes, generateQuery(category.name, numFakeTopics, output, API_TYPE_SEARCH));
            }
        }

        // Create a new query based on the filter, limit, output and search type properties
        private JSONObject generateQuery(String filter, int limit, String output, String searchType)
        {
            InputStream is = null;
            JSONObject json = null;
            String response = null;

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(QUERY, ""));
            // Fix this later to just take the filter, could have multiple types with different algorithms
            params.add(new BasicNameValuePair(FILTER, L_PAREN + FILTER_ANY + " " + FREEBASE_TYPE + filter + R_PAREN));
            params.add(new BasicNameValuePair(OUTPUT, L_PAREN + FREEBASE_KEY + FREEBASE_WIKI_EN + " " + FREEEBASE_COMMON_IMAGE + " " + output + R_PAREN));
            params.add(new BasicNameValuePair(LIMIT, String.valueOf(limit)));
            params.add(new BasicNameValuePair(KEY, API_KEY));
            String url = API_URL + searchType + "?" + URLEncodedUtils.format(params, "utf-8");

            if (AppConfig.DEBUG) Log.v("FakeAnswerTask-generateQuery", "Query URL " + url);

            // Making the HTTP request
            try
            {
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);

                if (httpGet != null)
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
                if (AppConfig.DEBUG) Log.e("FakeAnswerTask-generateQuery", "Error converting result " + e.toString());
            }

            try
            {
                if(response != null)
                {
                    json = new JSONObject(response);
                    if (!(json.optString(ERROR)).isEmpty())
                    {
                        if (AppConfig.DEBUG) Log.w("FakeAnswerTask-generateQuery", "Errors returned");
                        json = null;
                    }
                    else if (json.optString(RESULT).isEmpty())
                    {
                        if (AppConfig.DEBUG)Log.w("FakeAnswerTask-generateQuery", "Empty result set");
                        json = null;
                    }
                }

            }
            catch (JSONException e)
            {
                if (AppConfig.DEBUG) Log.e("FakeAnswerTask-generateQuery", "Error parsing data " + e.toString());
            }

            // return JSON String
            return json;
        }
    }
}
