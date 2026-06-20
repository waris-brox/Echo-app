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

public class EchoAccessibility
    extends AccessibilityService {

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
            public void onReceive(
                Context ctx, Intent intent) {
                try {
                    String action =
                        intent.getStringExtra(
                        "action");
                    if (action == null) return;
                    switch (action) {
                        case "screenshot":
                            doScreenshot();
                            break;
                        case "click_send":
                            clickSendButton();
                            break;
                        case "back":
                            performGlobalAction(
                            GLOBAL_ACTION_BACK);
                            break;
                        case "home":
                            performGlobalAction(
                            GLOBAL_ACTION_HOME);
                            break;
                        case "recents":
                            performGlobalAction(
                            GLOBAL_ACTION_RECENTS);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            IntentFilter filter =
                new IntentFilter(
                "com.echo.ACCESSIBILITY");
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(receiver,
                    filter,
                    RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(
                    receiver, filter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doScreenshot() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                // Must use integer value 1
                // TAKE_SCREENSHOT_SOFT_KEY = 1
                takeScreenshot(
                    1,
                    getMainExecutor(),
                    new TakeScreenshotCallback() {
                    public void onSuccess(
                        ScreenshotResult result) {
                        try {
                            android.graphics
                                .Bitmap bm =
                                android.graphics
                                .Bitmap
                                .wrapHardwareBuffer(
                                result
                                .getHardwareBuffer(),
                                result
                                .getColorSpace());
                            android.provider
                                .MediaStore
                                .Images.Media
                                .insertImage(
                                getContentResolver(),
                                bm,
                                "Echo_Screenshot_"
                                + System
                                .currentTimeMillis(),
                                "Echo Screenshot");
                            result
                                .getHardwareBuffer()
                                .close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    public void onFailure(
                        int errorCode) {
                        try {
                            performGlobalAction(
                            GLOBAL_ACTION_TAKE_SCREENSHOT);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                performGlobalAction(
                    GLOBAL_ACTION_TAKE_SCREENSHOT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                performGlobalAction(
                    GLOBAL_ACTION_TAKE_SCREENSHOT);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    void clickSendButton() {
        try {
            AccessibilityNodeInfo root =
                getRootInActiveWindow();
            if (root == null) return;

            String[] sendIds = {
                "com.whatsapp:id/send",
                "com.whatsapp:id/send_btn",
                "com.whatsapp:id/"
                + "conversation_send_button"
            };

            for (String id : sendIds) {
                try {
                    List<AccessibilityNodeInfo>
                        nodes = root
                        .findAccessibilityNodeInfosByViewId(
                        id);
                    if (nodes != null
                        && !nodes.isEmpty()) {
                        nodes.get(0)
                            .performAction(
                            AccessibilityNodeInfo
                            .ACTION_CLICK);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String[] texts = {
                "Send", "send", "SEND"};
            for (String text : texts) {
                try {
                    List<AccessibilityNodeInfo>
                        nodes = root
                        .findAccessibilityNodeInfosByText(
                        text);
                    if (nodes != null) {
                        for (AccessibilityNodeInfo
                            n : nodes) {
                            if (n.isClickable()) {
                                n.performAction(
                                AccessibilityNodeInfo
                                .ACTION_CLICK);
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
