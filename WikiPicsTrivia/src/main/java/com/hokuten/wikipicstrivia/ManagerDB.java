package com.hokuten.wikipicstrivia;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;


public class ManagerDB extends SQLiteOpenHelper
{
    private static ManagerDB mInstance;

    private static final int    DB_VERSION     = 2;
    private static final String DB_NAME        = "wikipicstrivia.db";
    private static final String WITH_DELIMITER = "(?<=%1$s)";
    public static final  String TOPIC_TOKEN    = "$X$";

    private static final String Q_QUESTION = SQLiteQueryBuilder.buildQueryString(
            false,
            "answers a join properties p on p.id=a.property join topics t on t.id=a.topic join categories c on c.id=a.category join themes th on th.id=c.theme",
            new String[] { "a.id as aid", "a.text as answer", "t.mid", "t.wid", "t.name", "t.author", "t.license", "p.id as pid", "p.question", "p.type", "p.unit", "c.id as cid", "th.color as color", "th.name as theme" },
            "a.used in (?,?)",
            null, null,
            "RANDOM()",
            "1");

    private static final String Q_QUESTION_CUSTOM = SQLiteQueryBuilder.buildQueryString(
            false,
            "answers a join properties p on p.id=a.property join topics t on t.id=a.topic join categories c on c.id=a.category join modes_to_categories mc on mc.category=c.id join themes th on th.id=c.theme",
            new String[] { "a.id as aid", "a.text as answer", "t.mid", "t.wid", "t.name", "t.author", "t.license", "p.id as pid", "p.question", "p.type", "p.unit", "c.id as cid", "th.color as color", "th.name as theme" },
            "a.used in (?,?) and mc.mode=?",
            null, null,
            "RANDOM()",
            "1");

    private static final String Q_QUESTIONS_REMAINING = SQLiteQueryBuilder.buildQueryString(
            false,
            "answers a join properties p on p.id=a.property join topics t on t.id=a.topic  join categories c on c.id=a.category join themes th on th.id=c.theme",
            new String[] { "a.id as aid", "a.topic as tid", "a.text as answer", "t.mid", "t.wid", "t.name", "t.author", "t.license", "p.id as pid", "p.question", "p.type", "p.unit", "th.color as color", "th.name as theme" },
            "a.used in (?,?)",
            null, null,
            "RANDOM()",
            null);

    private static final String Q_QUESTIONS_REMAINING_CUSTOM = SQLiteQueryBuilder.buildQueryString(
            false,
            "answers a join properties p on p.id=a.property join topics t on t.id=a.topic join categories c on c.id=a.category join modes_to_categories mc on mc.category=c.id join themes th on th.id=c.theme",
            new String[] { "a.id as aid", "a.topic as tid", "a.text as answer", "t.mid", "t.wid", "t.name", "t.author", "t.license", "p.id as pid", "p.question", "p.type", "p.unit", "th.color as color", "th.name as theme" },
            "a.used in (?,?) and mc.mode=?",
            null, null,
            "RANDOM()",
            null);

    private static final String Q_ANSWER_FAKE = SQLiteQueryBuilder.buildQueryString(
            true,
            "fake_answers fa",
            new String[] { "fa.text as answer" },
            "fa.text<>? and fa.property=? and fa.category=?",
            null, null,
            "RANDOM()",
            Integer.toString(ModelQuestion.NUM_FAKE_ANSWERS));

    private static final SimpleDateFormat   DATE_FORMAT_YEAR = new SimpleDateFormat("yyyy");
    private static final SimpleDateFormat[] DATE_FORMATS     = {
            new SimpleDateFormat("yyyy"),
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yyyy/MM/dd"),
            new SimpleDateFormat("M/dd/yyyy"),
            new SimpleDateFormat("dd.M.yyyy"),
            new SimpleDateFormat("M/dd/yyyy hh:mm:ss a"),
            new SimpleDateFormat("dd.M.yyyy hh:mm:ss a"),
            new SimpleDateFormat("dd.MMM.yyyy"),
            new SimpleDateFormat("dd-MMM-yyyy") };

    private SQLiteDatabase   mDB;
    private SQLiteStatement  mStmtMarkQUsed;
    private SQLiteStatement  mStmtMarkQUnused;
    private SQLiteStatement  mStmtResetAllUsed;
    private SQLiteStatement  mStmtCountQTotal;
    private SQLiteStatement  mStmtCountQAnswered;
    private SQLiteStatement  mStmtCountQCustomMode;
    private SQLiteStatement  mStmtCountQCategory;
    private SQLiteStatement  mStmtCountTopics;
    private SQLiteStatement  mStmtCountCategories;
    private SQLiteStatement  mStmtCountProperties;
    private SQLiteStatement  mStmtGetNamePropertyId;
    private SQLiteStatement  mStmtGetTopicId;
    private SQLiteStatement  mStmtCountNameProp;
    private SQLiteStatement  mStmtGetCategoryCursor;
    private int              mNamePropertyId;
    private TreeSet<Integer> mFakeIntAnswers;

    private ManagerDB()
    {
        super(Application2.instance(), DB_NAME, null, DB_VERSION);

        mDB = getWritableDatabase(); // will be null until db exists

        // if null, initialization occurs later
        if (mDB != null) initialize();
    }

    public static ManagerDB instance()
    {
        if (mInstance == null) mInstance = new ManagerDB();
        return mInstance;
    }

    public static boolean doesDBExist()
    {
        return Application2.instance().getDatabasePath(DB_NAME).exists();
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // this event is only triggered upon database creation, not on loading existing db.
        // if you need to run this method, then wipe your app db or uninstall app on device.
        mDB = db;

        // read and load schema
        String sql = Application2.instance().readTextFile(R.raw.schema);
        String[] statements = sql.split(String.format(WITH_DELIMITER, ";"));
        try
        {
            // guard against whitespace after last statement
            int count = statements.length;
            if (!statements[statements.length-1].endsWith(";"))
                count -= 1;

            // execute each statement
            for (int i = 0; i < count; i++)
                if (statements[i] != null)
                    mDB.execSQL(statements[i].trim());
        }
        catch (Exception ex)
        {
            // TODO: handle failure of db creation more gracefully
            if (AppConfig.DEBUG) Log.e("ManagerDB", "Error loading initial database schema! Must notify and kill app!", ex);
        }

        // read and load schema
        sql = Application2.instance().readTextFile(R.raw.data);
        statements = sql.split(String.format(WITH_DELIMITER, ";"));
        try
        {
            // guard against whitespace after last statement
            int count = statements.length;
            if (!statements[statements.length-1].endsWith(";"))
                count -= 1;

            // execute each statement
            for (int i = 0; i < count; i++)
                if (statements[i] != null)
                    mDB.execSQL(statements[i].trim());
        }
        catch (Exception ex)
        {
            // TODO: handle failure of db creation more gracefully
            if (AppConfig.DEBUG) Log.e("ManagerDB", "Error loading data database schema! Must notify and kill app!", ex);
        }

        initialize();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // we do not need to worry about this until we ship the first version :)

        // when the database schema changes or version changes, this event will be called.
        // the best way to upgrade a db is to write an update script from the immediate previous
        // version and name it by version number, then when an update occurs it loads each sql
        // script in order until we get to the current version.

        // if user updates their old app, it is likely their old database doesn't sync up.
        // so we have to load all the upgrade scripts from their database version to the version
        // of the new app.  so if they are on a mid release (version 2), and the new app is
        // (version 11), then we run scripts (5 and 11), etc.

        // schema.sql    (version 1)  <------- first release ships with original schema
        // upgrade2.sql  (version 2)  <------- second release includes this as well
        // upgrade5.sql  (version 5)  <------- third release includes this as well
        // upgrade11.sql (version 11) <------- fourth release includes this as well
        // etc.

        // this means that after the first ship, you cannot change the original schema file,
        // but rather must add drop and add calls to the upgrade scripts.

        mDB = db;
        // read and load schema
        if (newVersion > 1) {
            upgradeVersion2();
        }
    }

    public void upgradeVersion2() {
        String sql = Application2.instance().readTextFile(R.raw.upgrade2);
        String[] statements = sql.split(String.format(WITH_DELIMITER, ";"));
        try {
            // guard against whitespace after last statement
            int count = statements.length;
            if (!statements[statements.length - 1].endsWith(";"))
                count -= 1;

            // execute each statement
            for (int i = 0; i < count; i++)
                if (statements[i] != null)
                    mDB.execSQL(statements[i].trim());
        } catch (Exception ex) {
            // TODO: handle failure of db creation more gracefully
            if (AppConfig.DEBUG)
                Log.e("ManagerDB", "Error loading initial database schema! Must notify and kill app!", ex);
        }
    }

    public void initialize()
    {
        // make db readable so it can be copied off device for debugging purposes
        if (AppConfig.DEBUG) new File(mDB.getPath()).setReadable(true, false);

        // pre-compiled statements
        // NOTE: SQLiteStatements are simple scalar queries with 1x1 result
        mStmtMarkQUsed         = mDB.compileStatement("update answers set used=1 where id=?;");
        mStmtMarkQUnused       = mDB.compileStatement("update answers set used=0 where id=?;");
        mStmtResetAllUsed      = mDB.compileStatement("update answers set used=0;");
        mStmtCountQTotal       = mDB.compileStatement("select count(id) from answers;");
        mStmtCountQAnswered    = mDB.compileStatement("select count(id) from answers where used=1;");
        mStmtCountQCustomMode  = mDB.compileStatement("select count(a.id) from answers a join modes_to_categories mc on mc.category=a.category where a.used=0 and mc.mode=?;");
        mStmtCountQCategory    = mDB.compileStatement("select count(a.id) from answers a where a.category=? and used=?");
        mStmtCountTopics       = mDB.compileStatement("select count(mid) from topics;");
        mStmtCountCategories   = mDB.compileStatement("select count(id) from categories;");
        mStmtCountProperties   = mDB.compileStatement("select count(id) from categories_to_properties where category=?;");
        mStmtGetNamePropertyId = mDB.compileStatement("select id from properties where name='name';");
        mStmtGetTopicId        = mDB.compileStatement("select id from topics where mid=?;");
        mStmtCountNameProp     = mDB.compileStatement("select count(category) from categories_to_properties where category=? and property=?;");
        mStmtGetCategoryCursor = mDB.compileStatement("select cursor from category_stats where id=?;");

        mNamePropertyId = (int)mStmtGetNamePropertyId.simpleQueryForLong();

        mFakeIntAnswers = new TreeSet<Integer>();
    }

    public int getNamePropertyId()
    {
        return mNamePropertyId;
    }

    //---------------------------------------------------------------
    // Reading From Database
    //---------------------------------------------------------------

    public ModelSettings getSettings()
    {
        Cursor c = mDB.query("settings", null, null, null, null, null, null);
        ModelSettings model = new ModelSettings();

        try
        {
            if (c != null && c.moveToFirst())
            {
                model.locale       = c.getString(c.getColumnIndex("lang"));
                model.sfx          = c.getInt(c.getColumnIndex("sfx")) != 0;
                model.doNotRepeat  = c.getInt(c.getColumnIndex("dnr")) != 0;
                model.learning     = c.getInt(c.getColumnIndex("learning")) != 0;
                model.showAnswer   = c.getInt(c.getColumnIndex("showAnswer")) != 0;
                model.updates      = c.getInt(c.getColumnIndex("updates"));
                model.increment    = c.getInt(c.getColumnIndex("increment"));
                model.help         = c.getInt(c.getColumnIndex("help"));
                model.qotd         = c.getInt(c.getColumnIndex("qotd")) != 0;
                model.qotd_hour    = c.getInt(c.getColumnIndex("qotd_hour"));
                model.qotd_minute  = c.getInt(c.getColumnIndex("qotd_minute"));
                model.music        = c.getInt(c.getColumnIndex("music")) != 0;

            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getSettings", "Error", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return model;
    }

    public ModelQuestionBank getQuestionBankStats()
    {
        ModelQuestionBank stats = new ModelQuestionBank();

        stats.total       = (int)mStmtCountQTotal.simpleQueryForLong();
        stats.answered    = (int)mStmtCountQAnswered.simpleQueryForLong();
        //stats.unanswered  = stats.total - stats.answered;
        stats.topics      = (int)mStmtCountTopics.simpleQueryForLong();
        stats.categories  = (int)mStmtCountCategories.simpleQueryForLong();

        return stats;
    }

    public int getNumCategories()
    {
        return (int)mStmtCountCategories.simpleQueryForLong();
    }

    public ArrayList<ModelGameMode> getGameModes(ModelGameMode.Categories category)
    {
        Cursor c = mDB.query("modes", null, "categories = " + category.ordinal(), null, null, null, null);
        ArrayList<ModelGameMode> list = new ArrayList<ModelGameMode>();

        try
        {
            if (c != null && c.moveToFirst())
            {
                ModelGameMode.Categories[] gmc = ModelGameMode.Categories.values();

                do
                {
                    ModelGameMode model = new ModelGameMode();
                    model.id           = c.getInt(c.getColumnIndex("id"));
                    model.name         = c.getString(c.getColumnIndex("name"));
                    model.categories   = gmc[c.getInt(c.getColumnIndex("categories"))];
                    model.questions    = c.getInt(c.getColumnIndex("questions"));
                    model.timer        = c.getInt(c.getColumnIndex("timer"));
                    model.misses       = c.getInt(c.getColumnIndex("misses"));
                    model.isBrowsable  = c.getInt(c.getColumnIndex("browsable")) != 0;
                    model.hasHints     = c.getInt(c.getColumnIndex("hints")) != 0;
                    model.hasLinks     = c.getInt(c.getColumnIndex("links")) != 0;
                    list.add(model);
                }
                while (c.moveToNext());
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getGameModes", "Error", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return list;
    }

    public long getTopicId(String mid)
    {
        long result = -1;

        mStmtGetTopicId.bindString(1, mid);

        try
        {
            result = mStmtGetTopicId.simpleQueryForLong();
        }
        catch (SQLiteDoneException e)
        {
            // do nothing, returns exception when no row is present
        }
        finally { mStmtGetTopicId.clearBindings(); }

        return result;
    }

    public ArrayList<ModelCategory> getCategories()
    {
        Cursor c = mDB.query("categories", new String[] { "id, name" }, null, null, null, null, null);
        ArrayList<ModelCategory> list = new ArrayList<ModelCategory>();

        try
        {
            if (c != null && c.moveToFirst())
            {
                do
                {
                    ModelCategory model = new ModelCategory();
                    model.name = c.getString(c.getColumnIndex("name"));
                    model.id   = c.getInt(c.getColumnIndex("id"));
                    list.add(model);
                }
                while (c.moveToNext());
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getCategories", "Error", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return list;
    }

    // Returns only non-name properties
    public ArrayList<ModelCategoryProperty> getCategoryProperties(int categoryId)
    {
        Cursor c = mDB.query("properties, categories_to_properties",
                            new String[] { "properties.id", "properties.name, properties.type, properties.unit, properties.filter" },
                            "categories_to_properties.category = " + Integer.toString(categoryId) + " AND " +
                            "properties.id = categories_to_properties.property" + " AND " +
                            "properties.name != 'name'", null, null, null, null);

        ArrayList<ModelCategoryProperty> list = new ArrayList<ModelCategoryProperty>();

        try
        {
            if (c != null && c.moveToFirst())
            {
                ModelCategoryProperty.Type[] types    = ModelCategoryProperty.Type.values();
                ModelCategoryProperty.Unit[] units    = ModelCategoryProperty.Unit.values();
                ModelCategoryProperty.Filter[] filter = ModelCategoryProperty.Filter.values();

                do
                {
                    ModelCategoryProperty model = new ModelCategoryProperty();
                    model.name   = c.getString(c.getColumnIndex("name"));
                    model.id     = c.getInt(c.getColumnIndex("id"));
                    model.type   = types[c.getInt(c.getColumnIndex("type"))];
                    model.unit   = units[c.getInt(c.getColumnIndex("unit"))];
                    model.filter = filter[c.getInt(c.getColumnIndex("filter"))];
                    list.add(model);
                }
                while (c.moveToNext());
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getCategoryProperties", "Error", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return list;
    }

    public ArrayList<String> getBlackListTerms()
    {
        Cursor c = mDB.query("blacklist", new String[] { "term" }, null, null, null, null, null);
        return drainCursorToList(c);
    }

    public int getCustomModeQuestionCount(ModelGameMode mode)
    {
        int result = 0;

        mStmtCountQCustomMode.bindLong(1, mode.id);

        try
        {
            result = (int)mStmtCountQCustomMode.simpleQueryForLong();
        }
        catch (SQLiteDoneException e)
        {
            // do nothing, returns exception when no row is present
        }
        finally { mStmtCountQCustomMode.clearBindings(); }

        return result;
    }

    public ModelQuestion getQuestion(boolean dnr, ModelGameMode mode)
    {
        ModelQuestion q = new ModelQuestion();
        Cursor c = null;

        try
        {
            // query arguments
            CopyOnWriteArrayList<String> args = new CopyOnWriteArrayList<String>();
            // dnr settings
            args.add("0");
            args.add((dnr) ? "0" : "1");
            // category settings
            if (mode.categories == ModelGameMode.Categories.ALL)
                c = mDB.rawQuery(Q_QUESTION, args.toArray(new String[2]));
            else
            {
                args.add(Integer.toString(mode.id));
                c = mDB.rawQuery(Q_QUESTION_CUSTOM, args.toArray(new String[3]));
            }

            // query results
            //int ridx = (int)(Math.random() * (double)c.getCount());
            if (c != null && c.moveToFirst()) //c.moveToPosition(ridx))
            {
                q.question = c.getString(c.getColumnIndex("question"));
                q.answer   = c.getString(c.getColumnIndex("answer"));
                q.aid      = c.getInt(c.getColumnIndex("aid"));
                q.mid      = c.getString(c.getColumnIndex("mid"));
                q.wid      = c.getString(c.getColumnIndex("wid"));
                q.color    = c.getInt(c.getColumnIndex("color"));
                q.theme    = c.getString(c.getColumnIndex("theme"));
                q.author   = c.getString(c.getColumnIndex("author"));
                q.license  = c.getString(c.getColumnIndex("license"));
                int pid    = c.getInt(c.getColumnIndex("pid"));
                int cid    = c.getInt(c.getColumnIndex("cid"));
                ModelCategoryProperty.Type type = ModelCategoryProperty.Type.values()[c.getInt(c.getColumnIndex("type"))];
                ModelCategoryProperty.Unit unit = ModelCategoryProperty.Unit.values()[c.getInt(c.getColumnIndex("unit"))];

                // update question text with name of topic
                if (pid != mNamePropertyId)
                    q.question = q.question.replace(TOPIC_TOKEN, c.getString(c.getColumnIndex("name")));

                // mark as used for now
                mStmtMarkQUsed.bindLong(1, (long)q.aid);
                mStmtMarkQUsed.executeUpdateDelete();
                mStmtMarkQUsed.clearBindings();

                // create fake answers
                boolean pullFromDB = false;
                switch (type)
                {
                    case DATE:
                    {
                        try
                        {
                            Calendar calendar = Calendar.getInstance();
                            int val = Integer.parseInt(q.answer);                       // real answer
                            int old = calendar.get(Calendar.YEAR) - val;                // how long ago?
                            int dev = Math.max((int)((float)old * .75f), 5);            // deviation
                            int max = Math.min(val + dev, calendar.get(Calendar.YEAR)); // range max
                            int min;
                            if (val >= 0) min = Math.max(val - dev, 1);                 // range min for pos dates
                            else          min = val - dev;                              // range min for neg dates
                            int tmp = val;

                            //if (AppConfig.DEBUG)
                            //    Log.i("", String.format("date:\tmin(%d) val(%d) max(%d) old(%d) dev(%d)", min, val, max, old, dev));

                            HashSet<Integer> answers = new HashSet<Integer>();
                            answers.add(val);

                            // assign fake answers
                            for (int i=0; i<ModelQuestion.NUM_FAKE_ANSWERS; i++)
                            {
                                // make sure there are no duplicate answers
                                while (tmp == val || answers.contains(tmp))
                                    tmp = Application2.instance().randInt(min, max);
                                answers.add(tmp);
                                q.fakes[i] = Integer.toString(tmp);
                            }
                        }
                        catch (Exception ex)
                        {
                            pullFromDB = true;
                            if (AppConfig.DEBUG)
                                Log.e("ManagerDB-getQuestion", "Error generating fake answer DATE", ex);
                        }
                    }
                    break;

                    case INT:
                    {
                        try
                        {
                            int val = Integer.parseInt(q.answer);            // answer
                            int abs = Math.abs(val);                         // answer abs
                            int sig = (val>0) ? 1 : -1;                      // answer sign
                            int dev = Math.max((int)(((float)abs)*.23f), 1); // deviation
                            int mul;

                            // compute fake answer list
                            mFakeIntAnswers.clear();
                            mFakeIntAnswers.add(val);    // add real answer to prevent duplicate fakes
                            for (int i=1; i<=4; i++)     // add multiples of the deviation
                            {
                                mul = (dev*i);
                                mFakeIntAnswers.add(Math.max(abs - mul, 1) * sig);
                                mFakeIntAnswers.add(Math.max(abs + mul, 1) * sig);
                            }
                            mFakeIntAnswers.remove(val); // remove real answer

                            //if (AppConfig.DEBUG)
                            //{
                            //    StringBuilder msg = new StringBuilder("Fakes:\t");
                            //    for (int i : mFakeIntAnswers) msg.append(Integer.toString(i) + ", ");
                            //    Log.i("", msg.toString());
                            //}

                            // pick a random fake answer from the list
                            Iterator<Integer> iterator;
                            int iterations;
                            int fake = 0;
                            iterator   = mFakeIntAnswers.iterator();
                            iterations = Application2.instance().randInt(1, mFakeIntAnswers.size());
                            for (int j=0; j<iterations; j++) fake = iterator.next();

                            // collect some fake answers
                            for (int i=0; i<ModelQuestion.NUM_FAKE_ANSWERS; i++)
                            {
                                // assign to question (formatted)
                                q.fakes[i] = NumberFormat.getInstance().format(fake);

                                // update units
                                if (unit != ModelCategoryProperty.Unit.NONE)
                                    q.fakes[i] += " " + ModelCategoryProperty.Units[unit.ordinal()];

                                // wrap list of fake answers
                                if (!iterator.hasNext())
                                    iterator = mFakeIntAnswers.iterator();

                                // next fake answer please
                                fake = iterator.next();
                            }
                        }
                        catch (Exception ex)
                        {
                            pullFromDB = true;
                            if (AppConfig.DEBUG)
                                Log.e("ManagerDB-getQuestion", "Error generating fake answer INT", ex);
                        }
                    }
                    break;

                    case FLOAT:
                    case STRING:
                    default:
                    {
                        pullFromDB = true;
                    }
                    break;
                }

                // if no heuristic to create fake answers, pull from our local db
                if (pullFromDB)
                {
                    String[] args2 = new String[] { q.answer, Integer.toString(pid), Integer.toString(cid) };
                    c = mDB.rawQuery(Q_ANSWER_FAKE, args2);
                    if (c != null && c.moveToFirst())
                    {
                        int i = 0;
                        do
                        {
                            q.fakes[i] = c.getString(0);
                            i++;
                        }
                        while (c.moveToNext() && i < ModelQuestion.NUM_FAKE_ANSWERS);
                    }
                }

                // format numerical answers
                if (type == ModelCategoryProperty.Type.INT)
                {
                    // commas
                    q.answer = NumberFormat.getInstance().format(Integer.parseInt(q.answer));
                    // units
                    if (unit != ModelCategoryProperty.Unit.NONE)
                        q.answer += " " + ModelCategoryProperty.Units[unit.ordinal()];
                }
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getQuestion", "Error", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return q;
    }

    public ArrayList<ModelQuestion> getQuestionsRemaining(boolean dnr, ModelGameMode mode, int remaining)
    {
        // should only be called when:
        // (mode.questions > 0) and player has already loaded (mode.questions - remaining) questions

        ArrayList<ModelQuestion> questions = new ArrayList<ModelQuestion>();
        Cursor c = null;

        try
        {
            // query arguments
            CopyOnWriteArrayList<String> args = new CopyOnWriteArrayList<String>();
            // dnr settings
            args.add("0");
            args.add((dnr) ? "0" : "1");
            // category settings
            if (mode.categories == ModelGameMode.Categories.ALL)
                c = mDB.rawQuery(Q_QUESTIONS_REMAINING, args.toArray(new String[2]));
            else
            {
                args.add(Integer.toString(mode.id));
                c = mDB.rawQuery(Q_QUESTIONS_REMAINING_CUSTOM, args.toArray(new String[3]));
            }
            // number of questions to fetch
            //args.add(Integer.toString(remaining));

            // query results
            if (c != null && c.moveToFirst())
            {
                int index = mode.questions - remaining;
                for (int i=0; i<remaining; i++, index++)
                {
                    ModelQuestion q = new ModelQuestion();

                    q.index    = index;
                    q.state    = ModelQuestion.State.UNANSWERED;
                    q.question = c.getString(c.getColumnIndex("question"));
                    q.answer   = c.getString(c.getColumnIndex("answer"));
                    q.aid      = c.getInt(c.getColumnIndex("aid"));
                    q.mid      = c.getString(c.getColumnIndex("mid"));
                    q.wid      = c.getString(c.getColumnIndex("wid"));
                    q.color    = c.getInt(c.getColumnIndex("color"));
                    q.theme    = c.getString(c.getColumnIndex("theme"));
                    int pid    = c.getInt(c.getColumnIndex("pid"));
                    q.author   = c.getString(c.getColumnIndex("author"));
                    q.license  = c.getString(c.getColumnIndex("license"));
                    ModelCategoryProperty.Type type = ModelCategoryProperty.Type.values()[c.getInt(c.getColumnIndex("type"))];
                    ModelCategoryProperty.Unit unit = ModelCategoryProperty.Unit.values()[c.getInt(c.getColumnIndex("unit"))];

                    // update question text with name of topic
                    if (pid != mNamePropertyId)
                        q.question = q.question.replace(TOPIC_TOKEN, c.getString(c.getColumnIndex("name")));

                    // format numerical answers
                    if (type == ModelCategoryProperty.Type.INT)
                    {
                        // commas
                        q.answer = NumberFormat.getInstance().format(Integer.parseInt(q.answer));
                        // units
                        if (unit != ModelCategoryProperty.Unit.NONE)
                            q.answer += " " + ModelCategoryProperty.Units[unit.ordinal()];
                    }

                    questions.add(q);
                    c.moveToNext();
                }
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getQuestionsRemaining", "Error", ex);
        }
        finally
        {
            if (c  != null && !c.isClosed())  c.close();
        }

        return questions;
    }

    public ArrayList<ModelTheme> getThemes()
    {
        Cursor c = mDB.query("themes", new String[] { "name, id, color" }, null, null, null, null, null);
        ArrayList<ModelTheme> list = new ArrayList<ModelTheme>();

        try
        {
            if (c != null && c.moveToFirst())
            {
                do
                {
                    ModelTheme model = new ModelTheme();
                    model.id    = c.getInt(c.getColumnIndex("id"));
                    model.name  = c.getString(c.getColumnIndex("name"));
                    model.color = c.getInt(c.getColumnIndex("color"));
                    list.add(model);
                }
                while (c.moveToNext());
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getThemes", "Error", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return list;
    }

    public ArrayList<ModelCategory> getCategoryPerTheme(int themeId)
    {
        Cursor c = mDB.query("categories", new String[] { "displayname, name, id" }, "theme = " + themeId, null, null, null, null);
        ArrayList<ModelCategory> list = new ArrayList<ModelCategory>();

        try
        {
            if (c != null && c.moveToFirst())
            {
                do
                {
                    ModelCategory model = new ModelCategory();
                    model.id          = c.getInt(c.getColumnIndex("id"));
                    model.displayName = c.getString(c.getColumnIndex("displayname"));
                    model.name        = c.getString(c.getColumnIndex("name"));
                    list.add(model);
                }
                while (c.moveToNext());
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getCategoryPerTheme", "Error", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return list;
    }

    public ArrayList<ModelCategory> getCategoriesPerMode(int modeId)
    {
        Cursor c = mDB.query("modes_to_categories", new String[] { "category" }, "mode = " + modeId, null, null, null, null);
        ArrayList<ModelCategory> list = new ArrayList<ModelCategory>();

        try
        {
            if (c != null && c.moveToFirst())
            {
                do
                {
                    ModelCategory model = new ModelCategory();
                    model.id   = c.getInt(c.getColumnIndex("category"));
                    list.add(model);
                }
                while (c.moveToNext());
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB-getCategoryPerTheme", "Error", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return list;
    }

    public int getCategoryQueryCursor(int category)
    {
        int numTopics = 0;

        mStmtGetCategoryCursor.bindLong(1, category);

        try
        {
            numTopics = (int)mStmtGetCategoryCursor.simpleQueryForLong();
        }
        catch (SQLiteDoneException e)
        {
            // do nothing, returns exception when no row is present
        }
        finally
        {
            mStmtGetCategoryCursor.clearBindings();
        }

        return numTopics;
    }

    public int getCategoryQuestionCount(int category, int used)
    {
        int count = 0;

        mStmtCountQCategory.bindLong(1, category);
        mStmtCountQCategory.bindLong(2, used);

        try
        {
            count = (int)mStmtCountQCategory.simpleQueryForLong();
        }
        catch (SQLiteDoneException e)
        {
            // do nothing, returns exception when no row is present
        }
        finally
        {
            mStmtCountQCategory.clearBindings();
        }

        return count;
    }

    public int getCategoryPropertiesCount(int category)
    {
        int count = 0;

        mStmtCountProperties.bindLong(1, category);

        try
        {
            count = (int)mStmtCountProperties.simpleQueryForLong();
        }
        catch (SQLiteDoneException e)
        {
            // do nothing, returns exception when no row is present
        }
        finally
        {
            mStmtCountProperties.clearBindings();
        }

        return count;
    }



    public boolean doesCategoryHasNameProperty(int category)
    {
        boolean result = false;

        mStmtCountNameProp.bindLong(1, category);
        mStmtCountNameProp.bindLong(2, mNamePropertyId);

        try
        {
            result = (mStmtCountNameProp.simpleQueryForLong() > 0);
        }
        catch (SQLiteDoneException e)
        {
            // do nothing, returns exception when no row is present
        }
        finally { mStmtCountNameProp.clearBindings(); }

        return result;
    }

    //---------------------------------------------------------------
    // Writing To Database
    //---------------------------------------------------------------

    public void updateSettings(ModelSettings model)
    {
        mDB.delete("settings", null, null);

        ContentValues values = new ContentValues();
        values.put("lang",        model.locale);
        values.put("sfx",         model.sfx ? 1 : 0);
        values.put("dnr",         model.doNotRepeat ? 1 : 0);
        values.put("learning",    model.learning ? 1 : 0);
        values.put("showAnswer",  model.showAnswer ? 1 : 0);
        values.put("updates",     model.updates);
        values.put("increment",   model.increment);
        values.put("help",        model.help);
        values.put("qotd",        model.qotd ? 1 : 0);
        values.put("qotd_hour",   model.qotd_hour);
        values.put("qotd_minute", model.qotd_minute);
        values.put("music",       model.music ? 1 : 0);

        mDB.insert("settings", null, values);
    }

    public int updateGameMode(ModelGameMode gameMode)
    {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        if (gameMode.name.isEmpty())
            values.put("name", Application2.instance().getResString(R.string.createmode_name_default));
        else
            values.put("name", gameMode.name);

        values.put("categories", gameMode.categories.ordinal());
        values.put("questions",  gameMode.questions);
        values.put("timer",      gameMode.timer);
        values.put("misses",     gameMode.misses);
        values.put("browsable",  gameMode.isBrowsable);
        values.put("hints",      gameMode.hasHints);
        values.put("links",      gameMode.hasLinks);

        // Insert the new row, returning the primary key value of the new row
        return mDB.update("modes", values, "id = " + gameMode.id, null);
    }

    public int updateCategoryCursor(int category, int cursor)
    {
        ContentValues values = new ContentValues();
        values.put("cursor", cursor);

        return mDB.update("category_stats", values, "id = " + category, null);
    }

    public long createTopic(String mid, String wid, String name, String author, String license)
    {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put("mid", mid);
        values.put("wid", wid);
        values.put("name", name);
        values.put("author", author);
        values.put("license", license);

        long rowId = getTopicId(mid);
        if (rowId != -1)
            return rowId;
        else
            return mDB.insertWithOnConflict("topics", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public long createGameMode(ModelGameMode gameMode)
    {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        if (gameMode.name.isEmpty())
            values.put("name", Application2.instance().getResString(R.string.createmode_name_default));
        else
            values.put("name", gameMode.name);

        values.put("categories", gameMode.categories.ordinal());
        values.put("questions",  gameMode.questions);
        values.put("timer",      gameMode.timer);
        values.put("misses",     gameMode.misses);
        values.put("browsable",  gameMode.isBrowsable);
        values.put("hints",      gameMode.hasHints);
        values.put("links",      gameMode.hasLinks);

        // Insert the new row, returning the primary key value of the new row
        return mDB.insert("modes", null, values);
    }

    public long createModeToCategory(long modeId, int categoryId)
    {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put("mode", modeId);
        values.put("category", categoryId);

        // Insert the new row, returning the primary key value of the new row
        return mDB.insert("modes_to_categories", null, values);
    }

    public long createFakeAnswer(String text, int property, int category, ModelCategoryProperty.Type type)
    {
        // If it's not a string type do nothing
        if (type !=  ModelCategoryProperty.Type.STRING)
            return -1;

        // If the text contains a semi-colon don't add to db
        if (text.contains(";"))
            return -1;

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put("text", text);
        values.put("property", property);
        values.put("category", category);
        // Insert the new row, returning the primary key value of the new row
        return mDB.insertWithOnConflict("fake_answers", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public long createAnswer(String text, int property, ModelCategoryProperty.Type type, long topic, int category)
    {
        String formattedText;

        switch(type)
        {
            case STRING:
            {
                formattedText = text;
                break;
            }
            case DATE:
            {
                formattedText = ManagerDB.strDateToYear(text);
                break;
            }
            case INT:
            {
                formattedText = ManagerDB.strDoubleToInt(text);
                break;
            }
            case FLOAT:
            {
                formattedText = ManagerDB.strDoubleToDouble(text);
                break;
            }
            default:
            {
                formattedText = text;
                break;
            }
        }

        if (formattedText == null)
            return -1;

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put("text", formattedText);
        values.put("used", 0);
        values.put("property", property);
        values.put("topic", topic);
        values.put("category", category);

        // Insert the new row, returning the primary key value of the new row
        return mDB.insertWithOnConflict("answers", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void markQuestionsUnused(ArrayList<ModelQuestion> questions)
    {
        for (ModelQuestion q : questions)
        {
            mStmtMarkQUnused.bindLong(1, (long)q.aid);
            mStmtMarkQUnused.executeUpdateDelete();
            mStmtMarkQUnused.clearBindings();
        }
    }

    public void removeTopic(String mid)
    {
        long topicId = getTopicId(mid);
        if (topicId > 0)
        {
            // Remove the main topic
            mDB.delete("topics", "id = " + topicId, null);
            // Remove the answers related to the topic
            mDB.delete("answers", "topic = " + topicId, null);
        }
    }

    public void removeModeToCatgory(long mode, int category)
    {
        // Remove the mode to category relation
        mDB.delete("modes_to_categories", "category = " + category + " AND mode = " + mode, null);
    }

    public void removeGameMode(int id)
    {
        // Remove the mode
        mDB.delete("modes", "id = " + id, null);
        // Remove the mode to category relation
        mDB.delete("modes_to_categories", "mode = " + id, null);
    }

    public void resetAnswers()
    {
        mStmtResetAllUsed.executeUpdateDelete();
    }

    public void deleteAllQuestions()
    {
        mDB.delete("topics", null, null);
        mDB.delete("answers", null, null);

        ContentValues values = new ContentValues();
        values.put("cursor", 0);
        mDB.update("category_stats", values, null, null);

        ModelAlphaCursor.delete();

        // reset the # of updates in settings
        ModelSettings settings =  getSettings();
        settings.updates = 0;
        updateSettings(settings);
        // do we need to 'vacuum' the db?
    }

    //---------------------------------------------------------------
    // Helpers
    //---------------------------------------------------------------

    private ArrayList<String> drainCursorToList(Cursor c)
    {
        ArrayList<String> list = new ArrayList<String>();

        try
        {
            if (c != null && c.moveToFirst())
            {
                do { list.add(c.getString(0)); }
                while (c.moveToNext());
            }
        }
        catch (Exception ex)
        {
            if (AppConfig.DEBUG) Log.e("ManagerDB", "Error drainCursorToList()", ex);
        }
        finally
        {
            if (c != null && !c.isClosed()) c.close();
        }

        return list;
    }

    private static String strDoubleToInt(String s)
    {
        String result = null;
        try
        {
            Double d = Double.parseDouble(s);
            result = Integer.toString(d.intValue());
        }
        catch(NumberFormatException nfe)
        {
            // do nothing
        }
        return result;
    }

    private static String strDoubleToDouble(String s)
    {
        String result = null;
        try
        {
            Double d = Double.parseDouble(s);
            result = new BigDecimal(d).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
        }
        catch(NumberFormatException nfe)
        {
            // do nothing
        }
        return result;
    }

    private static String strDateToYear(String s)
    {
        String result = null;
        Date date = null;

        if (s == null) return null;

        for (SimpleDateFormat format : DATE_FORMATS)
        {
            try
            {
                format.setLenient(false);
                date = format.parse(s);
                result = DATE_FORMAT_YEAR.format(date);

                // Strip prepended 0's
                result = result.replaceFirst("^0+(?!$)", "");

            }
            catch (ParseException e)
            {
                // do nothing, test other formats
            }

            if (date != null) break;
        }

        return result;
    }
 }
