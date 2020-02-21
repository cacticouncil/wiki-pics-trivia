package com.hokuten.wikipicstrivia;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;


public class FragmentSettings extends Fragment implements View.OnClickListener, TimePickerDialog.OnTimeSetListener
{
    private final int     mNoon       = 12;
    private final int     mMidnight   = 0;
    private final String  mPM         = "PM";
    private final String  mAM         = "AM";
    private final String  mDateFormat = "%d:%02d %s";

    private TextView      mTxtLocale;
    private TextView      mTxtTime;
    private CheckBox      mCbxMusic;
    private CheckBox      mCbxSfx;
    private CheckBox      mCbxDNR;
    private CheckBox      mCbxLearning;
    private CheckBox      mCbxShowAnswer;
    private CheckBox      mCbxQotd;
    private Button        mBtnResetHelp;
    private Button        mBtnDone;
    private LinearLayout  mBtnSetTime;
    private AlarmManager  mAlarmMgr;
    private PendingIntent mAlarmIntent;


    public FragmentSettings()
    {
        super();
        mAlarmMgr = (AlarmManager)Application2.instance().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(Application2.instance(), IntentReceiver.class);
        intent.setAction("com.hokuten.wikipicstrivia.QOTD_ALARM");
        mAlarmIntent = PendingIntent.getBroadcast(Application2.instance(), 0, intent, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // views
        mTxtLocale     = (TextView)view.findViewById(R.id.tvSettingsLocale);
        mTxtTime       = (TextView)view.findViewById(R.id.tvSettingsTime);
        mCbxMusic      = (CheckBox)view.findViewById(R.id.cbSettingsMusic);
        mCbxSfx        = (CheckBox)view.findViewById(R.id.cbSettingsSound);
        mCbxDNR        = (CheckBox)view.findViewById(R.id.cbSettingsDNR);
        mCbxLearning   = (CheckBox)view.findViewById(R.id.cbSettingsLearning);
        mCbxShowAnswer = (CheckBox)view.findViewById(R.id.cbSettingsShowAnswer);
        mCbxQotd       = (CheckBox)view.findViewById(R.id.cbSettingsQotd);
        mBtnResetHelp  = (Button)view.findViewById(R.id.btnResetHelp);
        mBtnDone       = (Button)view.findViewById(R.id.btnSettingsDone);
        mBtnSetTime    = (LinearLayout)view.findViewById(R.id.btnSetTime);

        // listeners
        mCbxMusic.setOnClickListener(this);
        mCbxSfx.setOnClickListener(this);
        mCbxDNR.setOnClickListener(this);
        mCbxLearning.setOnClickListener(this);
        mCbxShowAnswer.setOnClickListener(this);
        mCbxQotd.setOnClickListener(this);
        mBtnSetTime.setOnClickListener(this);
        mBtnResetHelp.setOnClickListener(this);
        mBtnDone.setOnClickListener(this);

        initializeValues();

        ((ActivityMain)getActivity()).enableMenuItem(R.id.menu_settings, false);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        ((ActivityMain)getActivity()).enableMenuItem(R.id.menu_settings, true);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        initializeValues();
        super.onResume();
    }

    @Override
    public void onClick(View view)
    {
        ModelSettings settings = ManagerDB.instance().getSettings();

        if (view == mBtnDone)
        {
            ManagerMedia.instance().playSound(R.raw.sfx_button);

            ((ActivityMain)getActivity()).back();
        }
        else if (view == mCbxMusic)
        {
            settings.music = mCbxMusic.isChecked();
            ManagerDB.instance().updateSettings(settings);

            if (!settings.music)
                ManagerMedia.instance().pause();
            else
                ManagerMedia.instance().resume();
        }
        else if (view == mCbxSfx)
        {
            settings.sfx = mCbxSfx.isChecked();
            ManagerDB.instance().updateSettings(settings);
        }
        else if (view == mCbxDNR)
        {
            settings.doNotRepeat = mCbxDNR.isChecked();
            ManagerDB.instance().updateSettings(settings);
        }
        else if (view == mCbxLearning)
        {
            settings.learning = mCbxLearning.isChecked();
            ManagerDB.instance().updateSettings(settings);
        }
        else if (view == mCbxShowAnswer)
        {
            settings.showAnswer = mCbxShowAnswer.isChecked();
            ManagerDB.instance().updateSettings(settings);
        }
        else if (view == mBtnResetHelp)
        {
            settings.resetHelp();
            ManagerDB.instance().updateSettings(settings);

            Application2.toast(R.string.toast_reset_help);
        }
        else if (view == mCbxQotd)
        {
            settings.qotd = mCbxQotd.isChecked();
            ManagerDB.instance().updateSettings(settings);

            // Enable/Disable setting the time for QOTD
            mBtnSetTime.setEnabled(settings.qotd);
            for (int i = 0; i < mBtnSetTime.getChildCount(); i++)
            {
                mBtnSetTime.getChildAt(i).setEnabled(settings.qotd);
            }

            // Cancel the alarm if disabled
            if (!settings.qotd)
            {
                if (mAlarmMgr != null)
                {
                    mAlarmMgr.cancel(mAlarmIntent);
                }
                else
                {
                    if (AppConfig.DEBUG) Log.e("FrgmentSettings-onClick", "AlarmManager is null");
                }
            }

            // Update broadcast receiver depending on QOTD state
            ComponentName receiver = new ComponentName(Application2.instance(), BootReceiver.class);
            PackageManager pm = Application2.instance().getPackageManager();

            if (pm != null)
            {
                if (settings.qotd)
                {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
                else
                {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
            }
            else
            {
                if (AppConfig.DEBUG) Log.e("FrgmentSettings-onClick", "PackageManger is null");
            }
        }
        else if (view == mBtnSetTime)
        {
            if (mBtnSetTime.isEnabled())
            {
                TimePickerDialog timePickerDialog = new TimePickerDialog((ActivityMain)getActivity(), this, settings.qotd_hour, settings.qotd_minute, false);
                timePickerDialog.show();
            }
        }
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute)
    {
        // Update text and db settings
        ModelSettings settings = ManagerDB.instance().getSettings();

        settings.qotd_hour = hourOfDay;
        settings.qotd_minute = minute;

        mTxtTime.setText(getFormattedDate(hourOfDay, minute));

        ManagerDB.instance().updateSettings(settings);

        // Create calendar for specified time and set the intent in the alarm
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);

        long time = calendar.getTimeInMillis();
        if (time < System.currentTimeMillis())
            time += AlarmManager.INTERVAL_DAY;

        mAlarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY, mAlarmIntent);
    }

    public String getFormattedDate(int hour, int minute)
    {
        int hourFormatted = mNoon;
        String amPm = mAM;

        if (hour > mMidnight && hour < mNoon)
            hourFormatted = hour;
        else if (hour >= mNoon)
        {
            amPm = mPM;
            if (hour > mNoon)
                hourFormatted = hour - mNoon;
        }

        return String.format(mDateFormat, hourFormatted, minute, amPm);
    }

    private void initializeValues()
    {
        // current settings
        ModelSettings settings = ManagerDB.instance().getSettings();
        mCbxMusic.setChecked(settings.music);
        mCbxSfx.setChecked(settings.sfx);
        mCbxDNR.setChecked(settings.doNotRepeat);
        mCbxLearning.setChecked(settings.learning);
        mCbxShowAnswer.setChecked(settings.showAnswer);
        mCbxQotd.setChecked(settings.qotd);

        // cannot change DNR during gameplay
        ModelGameRound round = ManagerGame.instance().getGameRoundInfo();
        if (round != null)
            if (round.active)
                mCbxDNR.setEnabled(false);

        mTxtLocale.setText(getActivity().getResources().getConfiguration().locale.getLanguage());

        // Enable or disable layout and children depending on check
        mBtnSetTime.setEnabled(settings.qotd);
        for (int i = 0; i < mBtnSetTime.getChildCount();  i++)
        {
            mBtnSetTime.getChildAt(i).setEnabled(mCbxQotd.isChecked());
        }

        // Enable the alarm receiver if on
        if (mCbxQotd.isChecked())
        {
            ComponentName receiver = new ComponentName(Application2.instance(), BootReceiver.class);
            PackageManager pm = Application2.instance().getPackageManager();

            if (pm != null)
            {
                pm.setComponentEnabledSetting(receiver,
                                              PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                              PackageManager.DONT_KILL_APP);
            }
            else
            {
                if (AppConfig.DEBUG) Log.e("FrgmentSettings-onClick", "PackageManger is null");
            }
        }

        // Set the stored time
        mTxtTime.setText(getFormattedDate(settings.qotd_hour, settings.qotd_minute));
    }
}
