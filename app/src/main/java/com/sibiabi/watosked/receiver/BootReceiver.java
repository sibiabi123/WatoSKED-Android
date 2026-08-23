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
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            Log.i(TAG, "Device rebooted. Restoring all pending scheduled WhatsApp messages...");

            DatabaseHelper db = new DatabaseHelper(context);
            List<ScheduledMessage> pending = db.getPendingSchedules();
            long now = System.currentTimeMillis();

            for (ScheduledMessage msg : pending) {
                if (msg.getTimestamp() > now) {
                    AlarmSchedulerHelper.scheduleAlarm(context, msg);
                    Log.d(TAG, "Restored schedule ID: " + msg.getId() + " at " + msg.getTimestamp());
                } else {
                    db.updateStatus(msg.getId(), "MISSED");
                }
            }
        }
    }
}
