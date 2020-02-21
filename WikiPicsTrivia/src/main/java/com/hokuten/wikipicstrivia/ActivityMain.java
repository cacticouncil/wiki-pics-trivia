package com.hokuten.wikipicstrivia;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ExpandableListView;

import com.facebook.widget.FacebookDialog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ActivityMain extends FragmentActivity implements android.support.v4.app.FragmentManager.OnBackStackChangedListener
{
    private Menu         mMenu;
    private PauseHandler mPauseHandler;
    private boolean      mInitialized;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().addOnBackStackChangedListener(this);
        mPauseHandler = new PauseHandler();

        if (savedInstanceState == null)
        {
            setContentView(R.layout.activity_main);

            // this needs to be loaded onto stack first
            show(R.layout.fragment_titlescreen);

            // if no DB file, we need to do all our DB initialization first before anything else
            if (!ManagerDB.doesDBExist())
            {
                // show loading screen
                Bundle bundle = new Bundle();
                bundle.putString("message", Application2.instance().getResString(R.string.load_database));
                show(R.layout.fragment_loadingscreen, bundle);

                // DB initialization
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.schedule(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // LOADING DB FOR VERY FIRST TIME
                        ManagerDB.instance();

                        // unpacking default question images
                        ManagerMedia.instance().unpackImages();

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                // when done, continue with normal initialization
                                initialize();

                                // hide loading screen
                                Message msg = new Message();
                                msg.what = PauseHandler.Message_Types.DB_INITIALIZED.ordinal();
                                mPauseHandler.sendMessage(msg);
                            }
                        });
                    }
                }, 0, TimeUnit.SECONDS);
            }
            // otherwise continue on to normal initialization
            else initialize();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mPauseHandler.pause();

        if (mInitialized)
            ManagerMedia.instance().pause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mPauseHandler.resume(this);

        if (mInitialized)
            ManagerMedia.instance().resume();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.menu_settings)
        {
            show(R.layout.fragment_settings);
            return true;
        }
        else if (id == R.id.menu_questions)
        {
            show(R.layout.fragment_questions);
            return true;
        }
        else if (id == R.id.menu_endround)
        {
            ManagerGame.instance().endRound();
            return true;
        }
        else if (id == R.id.menu_about)
        {
            show(R.layout.fragment_about);
            return true;
        }
//        else if (id == R.id.menu_flag_question)
//        {
//            popupFlagQuestion();
//            return true;
//        }
        else if (id == R.id.menu_package_images)
        {
            if (AppConfig.DEBUG) ManagerMedia.instance().packageImages();
            return true;
        }
        else if (id == R.id.menu_create_fake_answers)
        {
            if (AppConfig.DEBUG) ManagerFreebase.instance().generateFakeAnswers();
            return true;
        }
        else if (id == R.id.menu_gmo)
        {
            popupLegend();
            return true;
        }
        else if (id == R.id.menu_categories)
        {
            popupCategories();
            return true;
        }
        else if (id == R.id.menu_feedback)
        {
            popupSendFeedback();
            return true;
        }
        else if (id == R.id.menu_disclaimer)
        {
            popupDisclaimer();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            // ignore user pressing back button during loading screens
            Fragment f = top();
            if (f != null && f instanceof FragmentLoadingScreen)
                return true;

            if (back())
                return true;

            handleFirstTimeExit();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment f = top();

        if (f instanceof FragmentRoundOver)
        {
            ((FragmentRoundOver)f).mUiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback()
            {
                @Override
                public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data)
                {
                    Log.e("Activity", String.format("Error: %s", error.toString()));
                }

                @Override
                public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data)
                {
                    Log.i("Activity", "Success!");
                }
            });
        }
    }

    public Fragment show(int id)
    {
        return show(id, null);
    }

    public Fragment show(int id, Bundle args)
    {
        Fragment f = createFragment(id);
        if (args != null) f.setArguments(args);

        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        trans.setTransition(FragmentTransaction.TRANSIT_NONE);
        trans.replace(R.id.container, f);
        trans.addToBackStack(Integer.toString(id));
        trans.commit();

        return f;
    }

    public void showHelp(ModelHelp.Help help, View highlight)
    {
        // quick out
        if (highlight == null) return;

        // view must be on screen to have dimensions
        final View v = highlight;
        final ModelHelp.Help h = help;
        if (highlight.getViewTreeObserver() != null)
        {
            highlight.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
            {
                @Override
                public void onGlobalLayout()
                {
                    // remove this listener
                    if (Build.VERSION.SDK_INT < 16)
                        v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    else
                        v.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // get dimensions of view
                    int[] l = new int[2];
                    v.getLocationOnScreen(l);
                    Rect r = new Rect(l[0], l[1], l[0] + v.getWidth(), l[1] + v.getHeight());
                    //Point pos = new Point(l[0] + (v.getWidth() >> 1), l[1] - (v.getHeight() >> 1));
                    //Point loc = getViewScreenPos(v);
                    //Rect r = new Rect(loc.x, loc.y, loc.x + v.getWidth(), loc.y + v.getHeight());
                    //Point pos = new Point(loc.x + (v.getWidth() >> 1), loc.y - (v.getHeight() >> 1));
                    //int radius = (int)((float)r.width() * .55f);
                    //pos.y += getActionBarHeight();

                    // pos
                    int[] location = new int[2];
                    v.getLocationInWindow(location);
                    int x = location[0] + v.getWidth() / 2;
                    int y = location[1] + v.getHeight() / 2;
                    Point pos = new Point(x,y);

                    // radius
                    int radius = (int)(v.getWidth() / 1.5);
                    Point dimen = Application2.instance().getScreenDimensions();
                    radius = (int)Math.max(radius, (float)Math.min(dimen.x, dimen.y) * .2f); // no smaller than 20% of screen
                    radius = (int)Math.min(radius, (float)Math.min(dimen.x, dimen.y) * .5f); // no bigger than 50% of screen

                    // popup help
                    showHelp(h, pos, radius);
                }
            });
        }
    }

    public void showHelp(ModelHelp.Help help, Point pos, int radius)
    {
        ModelHelp model = new ModelHelp();
        model.message = ModelHelp.getHelpMessage(help);
        model.highlightPoint = pos;
        model.highlightRadius = radius;

        View view = getLayoutInflater().inflate(R.layout.window_help, null);
        HelpWindow window = new HelpWindow(view, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, true);
        window.show(model, getWindow().getDecorView().findViewById(android.R.id.content));
    }

    public boolean back()
    {
        Fragment f = top();

        // pop back stack
        if (f != null)
        {
            if (f instanceof FragmentPlayScreen)
            {
                ManagerGame.instance().endRound();
            }
            else if (f instanceof FragmentRoundOver)
            {
                getSupportFragmentManager().popBackStack(Integer.toString(R.layout.fragment_gamemodes), 0);

                // BIG FUCKING HACK
                // menu bgm needs to play:
                //  when titlescreen starts first time (after db initialization)
                //  when roundover goes back to titlescreen
                // putting this in FragmentTitlescreen causes issues because either the DB
                // isn't created yet or music begins during loading screen
                ManagerMedia.instance().playBGM(R.raw.bgm_menu);
            }
            else
            {
                getSupportFragmentManager().popBackStack();
            }
        }

        return (f != null && !(f instanceof FragmentTitleScreen));
    }

    public Fragment top()
    {
        // apparently this searches backstack starting from top of stack
        // http://stackoverflow.com/a/22881272/1002098
        return getSupportFragmentManager().findFragmentById(R.id.container);
    }

    public void enableMenuItem(int id, boolean b)
    {
        MenuItem i = mMenu.findItem(id);
        if (i != null) i.setEnabled(b);
    }

    public void showMenuItem(int id, boolean b)
    {
        MenuItem i = mMenu.findItem(id);
        if (i != null) i.setVisible(b);
    }

    //---------------------------------------------------------------
    // Helper Methods
    //---------------------------------------------------------------

    private void initialize()
    {
        Application2.instance().setMainActivity(this);

        // load all singletons
        ManagerDB.instance();
        ManagerFreebase.instance();
        ManagerMedia.instance();
        ManagerGame.instance();

        boolean qotd = false;
        Intent intent = getIntent();
        if (intent != null)
        {
            String action = intent.getAction();
            if (action != null)
            {
                // handle QOTD intent
                if (action.equals("com.hokuten.wikipicstrivia.QOTD_START"))
                {
                    qotd = true;
                    handleQotdIntent();
                }
            }
        }

        if (!qotd)
        {
            ManagerMedia.instance().playBGM(R.raw.bgm_menu);
        }

        //Runtime rt = Runtime.getRuntime();
        //long maxMemory = rt.maxMemory();
        //Log.i("onCreate", "maxMemory:" + Long.toString(maxMemory));
        //ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        //int memoryClass = am.getMemoryClass();
        //Log.i("onCreate", "memoryClass:" + Integer.toString(memoryClass));

        mInitialized = true;
    }

    private static Fragment createFragment(int id)
    {
        switch (id)
        {
            case (R.layout.fragment_titlescreen):   return new FragmentTitleScreen();
            case (R.layout.fragment_gamemodes):     return new FragmentGameModes();
            case (R.layout.fragment_playscreen):    return new FragmentPlayScreen();
            case (R.layout.fragment_roundover):     return new FragmentRoundOver();
            case (R.layout.fragment_settings):      return new FragmentSettings();
            case (R.layout.fragment_questions):     return new FragmentQuestions();
            case (R.layout.fragment_createmode):    return new FragmentCreateMode();
            case (R.layout.fragment_about):         return new FragmentAbout();
            case (R.layout.fragment_qotd):          return new FragmentQotd();
            case (R.layout.fragment_loadingscreen): return new FragmentLoadingScreen();
            default:                                return null;
        }
    }

    private void handleQotdIntent()
    {
        ModelGameMode qotdGameMode = new ModelGameMode();
        qotdGameMode.name      = Application2.instance().getResString(R.string.qotd_title);
        qotdGameMode.timer     = 0;
        qotdGameMode.questions = 1;
        qotdGameMode.hasLinks  = true;

        ModelSettings settings = ManagerDB.instance().getSettings();
        String message = ManagerGame.instance().canUserProceedWithGameMode(settings, qotdGameMode);

        // user will proceed with game mode
        if (message == null)
        {
            ManagerMedia.instance().playSound(R.raw.sfx_correct);

            // Start the new round
            ManagerGame.instance().startRound(qotdGameMode);

            // Show the qotd
            show(R.layout.fragment_qotd);
        }
        // user needs to download more questions
        else
        {
            AlertDialog ad = new AlertDialog.Builder(this).create();
            ad.setCancelable(false);
            ad.setIcon(android.R.drawable.ic_dialog_alert);
            ad.setTitle(Application2.instance().getResString(R.string.dialog_need_questions_title));
            ad.setMessage(message);
            ad.setButton(
                    DialogInterface.BUTTON_POSITIVE, Application2.instance().getResString(R.string.common_ok),
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                            show(R.layout.fragment_questions);
                        }
                    }
            );
            ad.show();
        }
    }

//    private void popupFlagQuestion()
//    {
//        Fragment f = top();
//
//        if (f instanceof FragmentPlayScreen)
//        {
//            int idx = ((FragmentPlayScreen)f).getCurrentPage();
//            ModelQuestion q = ManagerGame.instance().getQuestion(idx);
//            ModelGameRound r = ManagerGame.instance().getGameRoundInfo();
//
//            String body = String.format(
//                    "\n\n" +
//                    "Qidx    : %d\n" +
//                    "Question: %s\n" +
//                    "Answer  : %s\n" +
//                    "Fakes   : '%s', '%s', '%s'\n" +
//                    "mid     : %s\n" +
//                    "wid     : %s\n\n" +
//                    "GameMode: %s\n" +
//                    "Correct : %d\n" +
//                    "Missed  : %d",
//                    q.index,
//                    q.question,
//                    q.answer,
//                    q.fakes[0], q.fakes[1], q.fakes[2],
//                    q.mid,
//                    q.wid,
//                    r.mode.name,
//                    r.correct,
//                    r.missed);
//
//            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
//            sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            sharingIntent.setType("message/rfc822");
//            sharingIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"hokuten-studios-testers@googlegroups.com"});
//            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "WikiPicsTrivia Flagged Question");
//            sharingIntent.putExtra(Intent.EXTRA_TEXT, body);
//
//            File srcFile = ManagerMedia.instance().getTopicImageFile(q.mid);
//            Uri contentUri = FileProvider.getUriForFile(this, "com.hokuten.wikipicstrivia.fileprovider", srcFile);
//
//            if (contentUri != null)
//            {
//                sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
//                sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                startActivity(Intent.createChooser(sharingIntent, "Send email via..."));
//            }
//            else
//            {
//                Application2.toast("Something went wrong! Can't flag question.");
//            }
//        }
//    }

    public void popupLegend()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Application2.instance().getResString(R.string.dialog_game_options_title));
        builder.setView(getLayoutInflater().inflate(R.layout.view_legend, null));
        builder.setPositiveButton(Application2.instance().getResString(R.string.common_ok), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        //WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        //lp.copyFrom(dialog.getWindow().getAttributes());
        //lp.width  = WindowManager.LayoutParams.WRAP_CONTENT;
        //lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        //dialog.getWindow().setAttributes(lp);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    private void popupSendFeedback()
    {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sharingIntent.setType("message/rfc822");
        sharingIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"feedback@hokutenstudios.com"});
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "(WikiPicsTrivia) ");

        sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(sharingIntent, "Send email via..."));
    }

    private void popupCategories()
    {
        View categoryList = getLayoutInflater().inflate(R.layout.view_categorylist, null);

        ExpandableListView categories = (ExpandableListView)categoryList.findViewById(R.id.elvCategories);
        categories.setAdapter(new AdapterCategories(this, true, null));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Application2.instance().getResString(R.string.dialog_categories_title));
        builder.setView(categoryList);
        builder.setPositiveButton(Application2.instance().getResString(R.string.common_ok), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.dismiss();
            }
        });


        AlertDialog dialog = builder.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    public void popupDisclaimer()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Application2.instance().getResString(R.string.dialog_disclaimer_title));
        builder.setMessage(Application2.instance().readTextFile(R.raw.eula));
        builder.setPositiveButton(Application2.instance().getResString(R.string.common_ok), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                ModelSettings settings = ManagerDB.instance().getSettings();
                // do not show this help again
                settings.setDoNotShowHelpAgain(ModelHelp.Help.DISCLAIMER);
                ManagerDB.instance().updateSettings(settings);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(Application2.instance().getResString(R.string.common_cancel), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dialog.dismiss();
                finish();
            }
        });


        AlertDialog dialog = builder.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    private void handleFirstTimeExit()
    {
        ModelSettings settings = ManagerDB.instance().getSettings();

        // Only show dialog if it's not set and it's the first time the user exits
        if (!settings.qotd && settings.isFirstTimeHelp(ModelHelp.Help.QOTD_SUGGEST))
        {
            // Save settings to not show again
            settings.setDoNotShowHelpAgain(ModelHelp.Help.QOTD_SUGGEST);
            ManagerDB.instance().updateSettings(settings);

            // Display dialog
            AlertDialog ad = new AlertDialog.Builder(this).create();
            ad.setCancelable(false);
            ad.setTitle(Application2.instance().getResString(R.string.dialog_qotd_title));
            ad.setMessage(Application2.instance().getResString(R.string.dialog_qotd_text));
            ad.setButton(DialogInterface.BUTTON_POSITIVE, Application2.instance().getResString(R.string.common_yes), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    // Push title screen back into stack
                    show(R.layout.fragment_titlescreen);
                    // Show the settings for the user
                    show(R.layout.fragment_settings);
                }
            });
            ad.setButton(DialogInterface.BUTTON_NEGATIVE, Application2.instance().getResString(R.string.common_no), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    //Stop the activity
                    finish();
                }
            });
            ad.show();
        }
    }

    @Override
    public void onBackStackChanged()
    {
        getActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 1);
    }

    @Override
    public boolean onNavigateUp()
    {
        back();
        return true;
    }

    public PauseHandler getPauseHandler()
    {
        return mPauseHandler;
    }
}
