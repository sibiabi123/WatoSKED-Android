package com.sibiabi.watosked.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.sibiabi.watosked.db.DatabaseHelper;
import com.sibiabi.watosked.model.ScheduledMessage;
import com.sibiabi.watosked.service.WhatsAppAccessibilityService;
import com.sibiabi.watosked.util.AlarmSchedulerHelper;

import java.net.URLEncoder;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AlarmSchedulerHelper.ACTION_SEND.equals(intent.getAction())) return;

        long   scheduleId  = intent.getLongExtra("schedule_id", -1);
        String recipient   = intent.getStringExtra("recipient");
        String message     = intent.getStringExtra("message");
        String waPackage   = intent.getStringExtra("whatsapp_pkg");
        String repeatType  = intent.getStringExtra("repeat_type");

        if (recipient == null || message == null) {
            Log.e(TAG, "Missing recipient or message in alarm intent");
            return;
        }

        if (waPackage == null) waPackage = "com.whatsapp";

        Log.i(TAG, "Alarm fired for schedule id=" + scheduleId + " recipient=" + recipient);

        // Acquire WakeLock to keep CPU running and turn screen on
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE,
                    "WatoSKED::AlarmWakeLock"
            );
            wl.acquire(30_000L); // 30 seconds max
        }

        // Pass data to Accessibility Service
        WhatsAppAccessibilityService.isScheduledSendActive = true;
        WhatsAppAccessibilityService.currentScheduleId     = scheduleId;
        WhatsAppAccessibilityService.pendingRecipient      = recipient;
        WhatsAppAccessibilityService.pendingMessage        = message;
        WhatsAppAccessibilityService.pendingWaPackage      = waPackage;

        // Launch WhatsApp with pre-filled message via deep link
        try {
            String cleanPhone = recipient.replaceAll("[^0-9+]", "");
            String url        = "https://api.whatsapp.com/send?phone=" + cleanPhone
                              + "&text=" + URLEncoder.encode(message, "UTF-8");
            Intent waIntent = new Intent(Intent.ACTION_VIEW);
            waIntent.setData(Uri.parse(url));
            waIntent.setPackage(waPackage);
            waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(waIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch WhatsApp", e);
            // Try WhatsApp without specifying package (fallback)
            try {
                String cleanPhone = recipient.replaceAll("[^0-9+]", "");
                String url = "https://api.whatsapp.com/send?phone=" + cleanPhone
                           + "&text=" + URLEncoder.encode(message, "UTF-8");
                Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (Exception ex) {
                Log.e(TAG, "Fallback WhatsApp launch also failed", ex);
                DatabaseHelper db = new DatabaseHelper(context);
                db.updateStatus(scheduleId, ScheduledMessage.STATUS_FAILED);
            }
        }

        // Handle repeating schedule — reschedule next occurrence
        if (scheduleId != -1 && repeatType != null && !repeatType.equals(ScheduledMessage.REPEAT_NONE)) {
            DatabaseHelper db = new DatabaseHelper(context);
            ScheduledMessage scheduled = db.getScheduleById(scheduleId);
            if (scheduled != null) {
                AlarmSchedulerHelper.rescheduleIfRepeating(context, scheduled);
                db.updateSchedule(scheduled);
            }
        }

        // Release WakeLock after 25 seconds (accessibility service needs time to act)
        final PowerManager.WakeLock finalWl = wl;
        if (finalWl != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (finalWl.isHeld()) finalWl.release();
            }, 25_000L);
        }
    }
}