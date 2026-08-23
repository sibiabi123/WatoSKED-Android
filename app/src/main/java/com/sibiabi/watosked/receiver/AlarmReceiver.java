package com.sibiabi.watosked.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.sibiabi.watosked.service.WhatsAppAccessibilityService;

import java.net.URLEncoder;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        long scheduleId = intent.getLongExtra("schedule_id", -1);
        String recipient = intent.getStringExtra("recipient");
        String message = intent.getStringExtra("message");

        Log.i(TAG, "Scheduled alarm triggered! ID: " + scheduleId + " -> " + recipient);

        if (recipient == null || message == null) {
            return;
        }

        // Keep CPU awake
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "WatoSKED:WakeLock");
            wakeLock.acquire(15000); // 15 seconds max
        }

        try {
            // Clean phone number (remove +, spaces, dashes)
            String cleanPhone = recipient.replaceAll("[^0-9]", "");

            // Flag accessibility service to auto-click Send
            WhatsAppAccessibilityService.isScheduledSendActive = true;
            WhatsAppAccessibilityService.currentScheduleId = scheduleId;

            // Build WhatsApp Intent
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + URLEncoder.encode(message, "UTF-8");
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse(url));
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            context.startActivity(whatsappIntent);
            Log.d(TAG, "WhatsApp Intent launched for automated send.");

        } catch (Exception e) {
            Log.e(TAG, "Failed to launch WhatsApp Intent", e);
            WhatsAppAccessibilityService.isScheduledSendActive = false;
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }
}
