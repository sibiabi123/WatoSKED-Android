package com.sibiabi.watosked.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.sibiabi.watosked.db.DatabaseHelper;
import com.sibiabi.watosked.model.ScheduledMessage;

import java.util.List;

public class WhatsAppAccessibilityService extends AccessibilityService {

    private static final String TAG         = "WatoAccessibility";
    private static final int    MAX_RETRIES = 3;
    private static final long   RETRY_DELAY = 1500L;

    // ---- Static flags (set by AlarmReceiver) ----
    public static volatile boolean isScheduledSendActive = false;
    public static volatile long    currentScheduleId     = -1;
    public static volatile String  pendingRecipient      = null;
    public static volatile String  pendingMessage        = null;
    public static volatile String  pendingWaPackage      = "com.whatsapp";

    private boolean isUnlockingInProgress = false;
    private int     sendRetryCount        = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "WatoSKED Accessibility Service connected and ready");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isScheduledSendActive) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        CharSequence pkgCS = event.getPackageName();
        if (pkgCS == null) return;
        String pkg = pkgCS.toString();

        // --- Lockscreen detected ---
        if (pkg.contains("systemui") || pkg.contains("keyguard") || pkg.contains("lockscreen")) {
            handleLockScreenUnlock(root);
            return;
        }

        // --- WhatsApp or WhatsApp Business opened ---
        if ("com.whatsapp".equals(pkg) || "com.whatsapp.w4b".equals(pkg)) {
            isUnlockingInProgress = false;
            // Delayed attempt to click send (let chat fully load)
            handler.postDelayed(() -> attemptSend(getRootInActiveWindow()), 600);
        }
    }

    // ---- Send with retry ----
    private void attemptSend(AccessibilityNodeInfo root) {
        if (!isScheduledSendActive || root == null) return;

        boolean clicked = findAndClickSendButton(root);
        if (clicked) {
            Log.i(TAG, "Send button clicked! Attempt #" + (sendRetryCount + 1));
            onMessageSent();
        } else {
            sendRetryCount++;
            if (sendRetryCount < MAX_RETRIES) {
                Log.w(TAG, "Send not yet available, retry #" + sendRetryCount + " in " + RETRY_DELAY + "ms");
                handler.postDelayed(() -> attemptSend(getRootInActiveWindow()), RETRY_DELAY);
            } else {
                Log.e(TAG, "Send failed after " + MAX_RETRIES + " attempts");
                onMessageFailed();
            }
        }
    }

    private void onMessageSent() {
        isScheduledSendActive = false;
        sendRetryCount        = 0;
        if (currentScheduleId != -1) {
            DatabaseHelper db = new DatabaseHelper(getApplicationContext());
            db.updateStatus(currentScheduleId, ScheduledMessage.STATUS_SENT);
            currentScheduleId = -1;
        }
        pendingRecipient = null;
        pendingMessage   = null;
    }

    private void onMessageFailed() {
        isScheduledSendActive = false;
        sendRetryCount        = 0;
        if (currentScheduleId != -1) {
            DatabaseHelper db = new DatabaseHelper(getApplicationContext());
            db.updateStatus(currentScheduleId, ScheduledMessage.STATUS_FAILED);
            currentScheduleId = -1;
        }
        pendingRecipient = null;
        pendingMessage   = null;
    }

    // ---- Lockscreen bypass ----
    private void handleLockScreenUnlock(AccessibilityNodeInfo root) {
        if (isUnlockingInProgress) return;
        isUnlockingInProgress = true;
        Log.d(TAG, "Lockscreen detected — attempting bypass...");

        SharedPreferences prefs = getSharedPreferences("watosked_prefs", Context.MODE_PRIVATE);
        String pin = prefs.getString("screen_pin", "");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Swipe up to reveal PIN pad
            Path path = new Path();
            path.moveTo(540, 1600);
            path.lineTo(540, 400);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, 300))
                    .build();
            dispatchGesture(gesture, new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription g) {
                    handler.postDelayed(() -> enterPin(getRootInActiveWindow(), pin), 400);
                }
            }, null);
        } else {
            enterPin(root, pin);
        }
    }

    private void enterPin(AccessibilityNodeInfo root, String pin) {
        if (root == null) { isUnlockingInProgress = false; return; }

        if (TextUtils.isEmpty(pin)) {
            // No PIN — just try to launch WhatsApp after swipe
            handler.postDelayed(this::launchWhatsApp, 500);
            isUnlockingInProgress = false;
            return;
        }

        // Try direct text set into password field first
        List<AccessibilityNodeInfo> passFields = root.findAccessibilityNodeInfosByViewId(
                "com.android.systemui:id/password_entry");
        if (passFields != null && !passFields.isEmpty()) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pin);
            passFields.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            // Press enter after 300ms
            handler.postDelayed(() -> {
                AccessibilityNodeInfo r2 = getRootInActiveWindow();
                if (r2 != null) clickKeyguardEnter(r2);
                handler.postDelayed(this::launchWhatsApp, 600);
            }, 300);
        } else {
            // Tap individual digit buttons
            for (char digit : pin.toCharArray()) {
                clickDigit(root, String.valueOf(digit));
            }
            handler.postDelayed(() -> {
                AccessibilityNodeInfo r2 = getRootInActiveWindow();
                if (r2 != null) clickKeyguardEnter(r2);
                handler.postDelayed(this::launchWhatsApp, 600);
            }, 500);
        }
        isUnlockingInProgress = false;
    }

    private void clickDigit(AccessibilityNodeInfo root, String digit) {
        // Try by text
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(digit);
        if (nodes != null) {
            for (AccessibilityNodeInfo n : nodes) {
                if (n.isClickable()) { n.performAction(AccessibilityNodeInfo.ACTION_CLICK); return; }
            }
        }
        // Try by ViewId (AOSP pattern)
        List<AccessibilityNodeInfo> idNodes = root.findAccessibilityNodeInfosByViewId(
                "com.android.systemui:id/key" + digit);
        if (idNodes != null && !idNodes.isEmpty()) {
            idNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private void clickKeyguardEnter(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> enter = root.findAccessibilityNodeInfosByViewId(
                "com.android.systemui:id/key_enter");
        if (enter != null && !enter.isEmpty()) {
            enter.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private void launchWhatsApp() {
        if (pendingRecipient == null || pendingMessage == null) return;
        try {
            String phone = pendingRecipient.replaceAll("[^0-9+]", "");
            String url   = "https://api.whatsapp.com/send?phone=" + phone
                         + "&text=" + java.net.URLEncoder.encode(pendingMessage, "UTF-8");
            String pkg   = pendingWaPackage != null ? pendingWaPackage : "com.whatsapp";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(android.net.Uri.parse(url));
            i.setPackage(pkg);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            Log.d(TAG, "WhatsApp re-launched after lockscreen bypass");
        } catch (Exception e) {
            Log.e(TAG, "Error launching WhatsApp after unlock", e);
        }
    }

    // ---- Find and click the WhatsApp Send button ----
    private boolean findAndClickSendButton(AccessibilityNodeInfo root) {
        if (root == null) return false;

        // Method 1: By ViewId (most reliable)
        String[] sendIds = {
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp:id/conversation_send_button",
            "com.whatsapp.w4b:id/conversation_send_button"
        };
        for (String resId : sendIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(resId);
            if (nodes != null) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (n.isClickable() && n.isEnabled()) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                }
            }
        }

        // Method 2: By content description
        String[] descs = {"Send", "Send message", "Send voice message"};
        for (String desc : descs) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(desc);
            if (nodes != null) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (n.isClickable() && n.isEnabled()) {
                        CharSequence cd = n.getContentDescription();
                        if (cd != null && cd.toString().toLowerCase().contains("send")) {
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            return true;
                        }
                    }
                }
            }
        }

        // Method 3: Recursive traversal
        return recursiveFind(root);
    }

    private boolean recursiveFind(AccessibilityNodeInfo node) {
        if (node == null) return false;
        CharSequence desc = node.getContentDescription();
        if (node.isClickable() && node.isEnabled() && desc != null &&
            desc.toString().toLowerCase().contains("send")) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (recursiveFind(node.getChild(i))) return true;
        }
        return false;
    }

    @Override public void onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted");
    }
}