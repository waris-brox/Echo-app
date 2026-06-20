package com.echo.assistant;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class EchoAccessibility extends AccessibilityService {

    public static EchoAccessibility instance;
    private BroadcastReceiver receiver;

    public void onServiceConnected() {
        instance = this;

        AccessibilityServiceInfo info =
            new AccessibilityServiceInfo();
        info.eventTypes =
            AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType =
            AccessibilityServiceInfo
            .FEEDBACK_GENERIC;
        info.flags =
            AccessibilityServiceInfo
            .FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        receiver = new BroadcastReceiver() {
            public void onReceive(Context ctx,
                Intent intent) {
                try {
                    String action = intent
                        .getStringExtra("action");
                    if (action == null) return;
                    switch (action) {
                        case "screenshot":
                            doScreenshot(); break;
                        case "click_send":
                            clickSendButton(); break;
                        case "back":
                            performGlobalAction(
                            GLOBAL_ACTION_BACK);
                            break;
                        case "home":
                            performGlobalAction(
                            GLOBAL_ACTION_HOME);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        IntentFilter filter = new IntentFilter(
            "com.echo.ACCESSIBILITY");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter,
                RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    public void doScreenshot() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                takeScreenshot(
                    TAKE_SCREENSHOT_SOFT_KEY,
                    getMainExecutor(),
                    new TakeScreenshotCallback() {
                    public void onSuccess(
                        ScreenshotResult result) {
                        try {
                            android.graphics.Bitmap bm =
                                android.graphics.Bitmap
                                .wrapHardwareBuffer(
                                result.getHardwareBuffer(),
                                result.getColorSpace());
                            android.provider.MediaStore
                                .Images.Media.insertImage(
                                getContentResolver(),
                                bm,
                                "Echo_"
                                + System
                                .currentTimeMillis(),
                                "Echo Screenshot");
                            result.getHardwareBuffer()
                                .close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    public void onFailure(int i) {
                        performGlobalAction(
                            GLOBAL_ACTION_TAKE_SCREENSHOT);
                    }
                });
            } else {
                performGlobalAction(
                    GLOBAL_ACTION_TAKE_SCREENSHOT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void clickSendButton() {
        try {
            AccessibilityNodeInfo root =
                getRootInActiveWindow();
            if (root == null) return;

            // Try WhatsApp send button IDs
            String[] sendIds = {
                "com.whatsapp:id/send",
                "com.whatsapp:id/send_btn",
                "com.whatsapp:id/conversation_send_button"
            };

            for (String id : sendIds) {
                List<AccessibilityNodeInfo> nodes =
                    root.findAccessibilityNodeInfosByViewId(
                    id);
                if (nodes != null
                    && !nodes.isEmpty()) {
                    nodes.get(0).performAction(
                        AccessibilityNodeInfo
                        .ACTION_CLICK);
                    return;
                }
            }

            // Try by content description
            String[] descs = {
                "Send", "send", "SEND"};
            for (String desc : descs) {
                List<AccessibilityNodeInfo> nodes =
                    root.findAccessibilityNodeInfosByText(
                    desc);
                if (nodes != null
                    && !nodes.isEmpty()) {
                    for (AccessibilityNodeInfo n
                        : nodes) {
                        if (n.isClickable()) {
                            n.performAction(
                                AccessibilityNodeInfo
                                .ACTION_CLICK);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onAccessibilityEvent(
        AccessibilityEvent e) {}

    public void onInterrupt() {}

    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            if (receiver != null)
                unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
