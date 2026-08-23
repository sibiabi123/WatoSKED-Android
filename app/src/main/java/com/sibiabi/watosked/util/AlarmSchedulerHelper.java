package com.sibiabi.watosked.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.sibiabi.watosked.model.ScheduledMessage;
import com.sibiabi.watosked.receiver.AlarmReceiver;

public class AlarmSchedulerHelper {

    private static final String TAG = "AlarmSchedulerHelper";

    public static void scheduleAlarm(Context context, ScheduledMessage message) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.sibiabi.watosked.ACTION_SEND_MESSAGE");
        intent.putExtra("schedule_id", message.getId());
        intent.putExtra("recipient", message.getRecipient());
        intent.putExtra("message", message.getMessage());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) message.getId(),
                intent,
                flags
        );

        long triggerAtMillis = message.getTimestamp();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }

        Log.i(TAG, "Exact alarm scheduled for message ID: " + message.getId() + " at " + triggerAtMillis);
    }
}
