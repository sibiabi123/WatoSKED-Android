package com.sibiabi.watosked.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.sibiabi.watosked.model.ScheduledMessage;
import com.sibiabi.watosked.receiver.AlarmReceiver;

import java.util.Calendar;

public class AlarmSchedulerHelper {

    private static final String TAG = "AlarmScheduler";
    public static final  String ACTION_SEND = "com.sibiabi.watosked.ACTION_SEND_MESSAGE";

    public static void scheduleAlarm(Context ctx, ScheduledMessage msg) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(ctx, msg);
        long triggerAt  = msg.getTimestamp();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }

        Log.i(TAG, "Alarm scheduled: id=" + msg.getId() + " at=" + triggerAt);
    }

    public static void cancelAlarm(Context ctx, long scheduleId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, AlarmReceiver.class).setAction(ACTION_SEND);
        int flags = PendingIntent.FLAG_NO_CREATE |
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        PendingIntent pi = PendingIntent.getBroadcast(ctx, (int) scheduleId, intent, flags);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
            Log.i(TAG, "Alarm cancelled for id=" + scheduleId);
        }
    }

    /**
     * Compute next fire time for a repeating schedule and reschedule it.
     * Should be called right after a message is sent.
     */
    public static void rescheduleIfRepeating(Context ctx, ScheduledMessage msg) {
        if (msg == null || !msg.isRepeating()) return;

        long nextTimestamp = computeNextTimestamp(msg);
        if (nextTimestamp <= 0) return;

        msg.setTimestamp(nextTimestamp);
        msg.setStatus(ScheduledMessage.STATUS_PENDING);
        scheduleAlarm(ctx, msg);
        Log.i(TAG, "Repeating alarm rescheduled for id=" + msg.getId() + " next=" + nextTimestamp);
    }

    private static long computeNextTimestamp(ScheduledMessage msg) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(msg.getTimestamp());

        switch (msg.getRepeatType()) {
            case ScheduledMessage.REPEAT_DAILY:
                cal.add(Calendar.DAY_OF_MONTH, 1);
                return cal.getTimeInMillis();

            case ScheduledMessage.REPEAT_WEEKLY:
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                return cal.getTimeInMillis();

            case ScheduledMessage.REPEAT_CUSTOM:
                // Find next valid day from repeatDays (MON,TUE,WED,THU,FRI,SAT,SUN)
                String[] days = msg.getRepeatDays() != null ? msg.getRepeatDays().split(",") : new String[0];
                if (days.length == 0) return -1;

                Calendar next = Calendar.getInstance();
                next.setTimeInMillis(msg.getTimestamp());
                next.add(Calendar.DAY_OF_MONTH, 1); // start from tomorrow

                for (int attempt = 0; attempt < 7; attempt++) {
                    String dayName = getDayName(next.get(Calendar.DAY_OF_WEEK));
                    for (String d : days) {
                        if (d.trim().equalsIgnoreCase(dayName)) {
                            // Set same hour/minute
                            Calendar original = Calendar.getInstance();
                            original.setTimeInMillis(msg.getTimestamp());
                            next.set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY));
                            next.set(Calendar.MINUTE, original.get(Calendar.MINUTE));
                            next.set(Calendar.SECOND, 0);
                            next.set(Calendar.MILLISECOND, 0);
                            return next.getTimeInMillis();
                        }
                    }
                    next.add(Calendar.DAY_OF_MONTH, 1);
                }
                return -1;

            default:
                return -1;
        }
    }

    private static String getDayName(int calendarDay) {
        switch (calendarDay) {
            case Calendar.MONDAY:    return "MON";
            case Calendar.TUESDAY:   return "TUE";
            case Calendar.WEDNESDAY: return "WED";
            case Calendar.THURSDAY:  return "THU";
            case Calendar.FRIDAY:    return "FRI";
            case Calendar.SATURDAY:  return "SAT";
            case Calendar.SUNDAY:    return "SUN";
            default:                 return "";
        }
    }

    private static PendingIntent buildPendingIntent(Context ctx, ScheduledMessage msg) {
        Intent intent = new Intent(ctx, AlarmReceiver.class);
        intent.setAction(ACTION_SEND);
        intent.putExtra("schedule_id",  msg.getId());
        intent.putExtra("recipient",    msg.getRecipient());
        intent.putExtra("message",      msg.getMessage());
        intent.putExtra("whatsapp_pkg", msg.getWhatsAppPackage());
        intent.putExtra("repeat_type",  msg.getRepeatType());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

        return PendingIntent.getBroadcast(ctx, (int) msg.getId(), intent, flags);
    }
}