package com.sibiabi.watosked.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sibiabi.watosked.db.DatabaseHelper;
import com.sibiabi.watosked.model.ScheduledMessage;
import com.sibiabi.watosked.util.AlarmSchedulerHelper;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
            !"android.intent.action.QUICKBOOT_POWERON".equals(action)) return;

        Log.i(TAG, "Device booted — restoring pending alarms...");

        DatabaseHelper db = new DatabaseHelper(context);
        List<ScheduledMessage> pending = db.getPendingSchedules();

        int count = 0;
        long now = System.currentTimeMillis();
        for (ScheduledMessage msg : pending) {
            if (msg.getTimestamp() > now) {
                AlarmSchedulerHelper.scheduleAlarm(context, msg);
                count++;
            } else {
                // Overdue — mark as failed (was not sent while device was off)
                db.updateStatus(msg.getId(), ScheduledMessage.STATUS_FAILED);
                Log.w(TAG, "Overdue message id=" + msg.getId() + " marked FAILED");
            }
        }

        Log.i(TAG, "Restored " + count + " pending alarm(s)");
    }
}