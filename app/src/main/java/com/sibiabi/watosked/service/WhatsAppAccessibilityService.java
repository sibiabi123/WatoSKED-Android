package com.sibiabi.watosked.service;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.sibiabi.watosked.db.DatabaseHelper;

import java.util.List;

public class WhatsAppAccessibilityService extends AccessibilityService {

    private static final String TAG = "WatoAccessibility";
    public static boolean isScheduledSendActive = false;
    public static long currentScheduleId = -1;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isScheduledSendActive) {
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        // WhatsApp package validation
        CharSequence packageName = event.getPackageName();
        if (packageName == null || (!packageName.equals("com.whatsapp") && !packageName.equals("com.whatsapp.w4b"))) {
            return;
        }

        Log.d(TAG, "Active scheduled send detected. Searching for WhatsApp Send button...");

        // Delay slightly (350ms) to ensure WhatsApp chat UI has completely loaded
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean clicked = findAndClickSendButton(rootNode);
            if (clicked) {
                Log.d(TAG, "WhatsApp Send button clicked successfully!");
                isScheduledSendActive = false;

                // Update database
                if (currentScheduleId != -1) {
                    DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                    db.updateStatus(currentScheduleId, "SENT");
                    currentScheduleId = -1;
                }
            }
        }, 400);
    }

    private boolean findAndClickSendButton(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // Method 1: Find by resource ID
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

        // Method 3: Recursive traversal for ImageButton with contentDescription "Send"
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
