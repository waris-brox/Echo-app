package com.echo.assistant;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class EchoAccessibility extends AccessibilityService {

    public static EchoAccessibility instance;
    private BroadcastReceiver receiver;

    @Override
    public void onServiceConnected() {
        instance = this;

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            | AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getStringExtra("action");
                if (action == null) return;
                switch (action) {
                    case "screenshot":
                        takeEchoScreenshot(); break;
                    case "whatsapp_send":
                        sendWhatsAppMessage(
                            intent.getStringExtra("text")); break;
                    case "back":
                        performGlobalAction(GLOBAL_ACTION_BACK); break;
                    case "home":
                        performGlobalAction(GLOBAL_ACTION_HOME); break;
                    case "recents":
                        performGlobalAction(GLOBAL_ACTION_RECENTS); break;
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.echo.ACCESSIBILITY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    // Renamed from takeScreenshot() to avoid clashing with the
    // system's own AccessibilityService.takeScreenshot() method.
    public void takeEchoScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                getMainExecutor(),
                new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult result) {
                        android.util.Log.d("ECHO", "Screenshot captured successfully");
                    }
                    @Override
                    public void onFailure(int errorCode) {
                        android.util.Log.e("ECHO", "Screenshot failed, code: " + errorCode);
                    }
                });
        } else {
            android.util.Log.e("ECHO", "Screenshot API requires Android 11+ (this device is older)");
        }
    }

    // Send WhatsApp message by clicking send button
    public void sendWhatsAppMessage(String text) {
        if (text == null) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        List<AccessibilityNodeInfo> sendBtns =
            root.findAccessibilityNodeInfosByViewId(
                "com.whatsapp:id/send");
        if (sendBtns != null && !sendBtns.isEmpty()) {
            sendBtns.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            android.util.Log.e("ECHO", "WhatsApp send button not found - UI may have changed or chat not open");
        }
    }

    // Click any button by text
    public static boolean clickByText(String text) {
        if (instance == null) return false;
        AccessibilityNodeInfo root = instance.getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes =
            root.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }
        return false;
    }

    // Type text into focused field
    public static boolean typeText(String text) {
        if (instance == null) return false;
        AccessibilityNodeInfo root = instance.getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo focused = root.findFocus(
            AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null) {
            Bundle args = new Bundle();
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text);
            focused.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            return true;
        }
        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (receiver != null) unregisterReceiver(receiver);
    }
}
