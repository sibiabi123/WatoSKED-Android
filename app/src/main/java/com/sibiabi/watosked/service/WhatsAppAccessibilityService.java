package com.sibiabi.watosked.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.sibiabi.watosked.db.DatabaseHelper;

import java.net.URLEncoder;
import java.util.List;

public class WhatsAppAccessibilityService extends AccessibilityService {

    private static final String TAG = "WatoAccessibility";
    public static boolean isScheduledSendActive = false;
    public static long currentScheduleId = -1;
    public static String pendingRecipient = null;
    public static String pendingMessage = null;

    private boolean isUnlockingInProgress = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isScheduledSendActive) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;
        String pkg = packageName.toString();

        Log.d(TAG, "Accessibility Event from package: " + pkg);

        // 1. Check if device is on Lockscreen (SystemUI / Keyguard)
        if (pkg.contains("systemui") || pkg.contains("keyguard") || pkg.contains("lockscreen")) {
            handleLockScreenUnlock(rootNode);
            return;
        }

        // 2. Check if we are inside WhatsApp or WhatsApp Business
        if (pkg.equals("com.whatsapp") || pkg.equals("com.whatsapp.w4b")) {
            isUnlockingInProgress = false;

            // Small delay to ensure chat window elements are completely loaded
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                boolean clicked = findAndClickSendButton(rootNode);
                if (clicked) {
                    Log.d(TAG, "✅ WhatsApp Send button clicked successfully!");
                    isScheduledSendActive = false;
                    pendingRecipient = null;
                    pendingMessage = null;

                    // Update database status to SENT
                    if (currentScheduleId != -1) {
                        DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                        db.updateStatus(currentScheduleId, "SENT");
                        currentScheduleId = -1;
                    }
                }
            }, 450);
        }
    }

    private void handleLockScreenUnlock(AccessibilityNodeInfo rootNode) {
        if (isUnlockingInProgress) return;
        isUnlockingInProgress = true;

        Log.d(TAG, "Device is locked. Attempting lockscreen bypass...");

        SharedPreferences prefs = getSharedPreferences("watosked_prefs", Context.MODE_PRIVATE);
        String savedPin = prefs.getString("screen_pin", "");

        // Step 1: Swipe up to reveal PIN pad if swipe screen is present
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path swipePath = new Path();
            swipePath.moveTo(500, 1500);
            swipePath.lineTo(500, 300);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(swipePath, 50, 250));
            dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d(TAG, "Swipe-up gesture completed.");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> enterPinIfAvailable(rootNode, savedPin), 300);
                }
            }, null);
        } else {
            enterPinIfAvailable(rootNode, savedPin);
        }
    }

    private void enterPinIfAvailable(AccessibilityNodeInfo rootNode, String pin) {
        if (TextUtils.isEmpty(pin)) {
            Log.d(TAG, "No PIN configured. Launching WhatsApp directly after keyguard dismiss.");
            launchWhatsAppDirectly();
            return;
        }

        Log.d(TAG, "Auto-entering user configured screen PIN...");

        // Try entering PIN by pressing digit buttons (e.g. key0, key1... or text 0, 1, 2)
        for (char digit : pin.toCharArray()) {
            String digitStr = String.valueOf(digit);
            boolean pressed = clickDigitNode(rootNode, digitStr);
            if (!pressed) {
                // Try setting text into password field
                List<AccessibilityNodeInfo> editTexts = rootNode.findAccessibilityNodeInfosByViewId("com.android.systemui:id/password_entry");
                if (editTexts != null && !editTexts.isEmpty()) {
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pin);
                    editTexts.get(0).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    break;
                }
            }
        }

        // Try clicking Enter/OK button on lockscreen
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            clickKeyguardEnter(rootNode);
            launchWhatsAppDirectly();
        }, 500);
    }

    private boolean clickDigitNode(AccessibilityNodeInfo root, String digit) {
        if (root == null) return false;

        // Find by text
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(digit);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }

        // Find by common view ID in SystemUI (key0, key1, etc.)
        List<AccessibilityNodeInfo> idNodes = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/key" + digit);
        if (idNodes != null && !idNodes.isEmpty()) {
            idNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }

        return false;
    }

    private void clickKeyguardEnter(AccessibilityNodeInfo root) {
        if (root == null) return;
        List<AccessibilityNodeInfo> enterNodes = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/key_enter");
        if (enterNodes != null && !enterNodes.isEmpty()) {
            enterNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private void launchWhatsAppDirectly() {
        if (pendingRecipient == null || pendingMessage == null) return;

        try {
            String cleanPhone = pendingRecipient.replaceAll("[^0-9]", "");
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + URLEncoder.encode(pendingMessage, "UTF-8");
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse(url));
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(whatsappIntent);
            Log.d(TAG, "WhatsApp Intent re-launched after lockscreen bypass.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch WhatsApp after unlock", e);
        }
    }

    private boolean findAndClickSendButton(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // Method 1: Find by view ID
        List<AccessibilityNodeInfo> sendNodes = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send");
        if (sendNodes != null && !sendNodes.isEmpty()) {
            for (AccessibilityNodeInfo btn : sendNodes) {
                if (btn.isClickable() && btn.isEnabled()) {
                    btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }

        // Method 2: Find by Content Description ("Send")
        List<AccessibilityNodeInfo> descNodes = node.findAccessibilityNodeInfosByText("Send");
        if (descNodes != null && !descNodes.isEmpty()) {
            for (AccessibilityNodeInfo btn : descNodes) {
                if (btn.isClickable() && btn.isEnabled()) {
                    btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }

        // Method 3: Recursive traversal for Send button
        return recursiveFindSend(node);
    }

    private boolean recursiveFindSend(AccessibilityNodeInfo node) {
        if (node == null) return false;

        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.toString().equalsIgnoreCase("Send")) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (recursiveFindSend(child)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility Service Interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "WhatsApp Accessibility Service Connected & Ready!");
    }
}
