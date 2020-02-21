package com.hokuten.wikipicstrivia;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;


public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Start the alarm for QOTD
        AlarmManager mAlarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent mAlarmIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, IntentReceiver.class), 0);
        ModelSettings settings = ManagerDB.instance().getSettings();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, settings.qotd_hour);
        calendar.set(Calendar.MINUTE, settings.qotd_minute);

        long time = calendar.getTimeInMillis();
        if (time < System.currentTimeMillis())
            time += AlarmManager.INTERVAL_DAY;

        if (mAlarmMgr != null)
            mAlarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY, mAlarmIntent);

    }
}
