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
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            IntentFilter f =
                new IntentFilter(
                "com.echo.ACCESSIBILITY");
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(receiver, f,
                    RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(receiver, f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doScreenshot() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                takeScreenshot(1,
                    getMainExecutor(),
                    new TakeScreenshotCallback(){
                    public void onSuccess(
                        ScreenshotResult res) {
                        try {
                            android.graphics
                                .Bitmap bm =
                                android.graphics
                                .Bitmap
                                .wrapHardwareBuffer(
                                res
                                .getHardwareBuffer(),
                                res
                                .getColorSpace());
                            android.provider
                                .MediaStore
                                .Images.Media
                                .insertImage(
                                getContentResolver(),
                                bm,
                                "Echo_"
                                + System
                                .currentTimeMillis(),
                                "Echo Screenshot");
                            res
                                .getHardwareBuffer()
                                .close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    public void onFailure(
                        int code) {
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
            performGlobalAction(
                GLOBAL_ACTION_TAKE_SCREENSHOT);
        }
    }

    public void clickSendButton() {
        try {
            AccessibilityNodeInfo root =
                getRootInActiveWindow();
            if (root == null) return;

            // Try WhatsApp send button IDs
            String[] ids = {
                "com.whatsapp:id/send",
                "com.whatsapp:id/send_btn",
                "com.whatsapp:id/"
                + "conversation_send_button",
                "com.whatsapp:id/"
                + "mic_to_send_button"
            };
            for (String id : ids) {
                try {
                    List<AccessibilityNodeInfo>
                        nodes = root
                        .findAccessibilityNodeInfosByViewId(
                        id);
                    if (nodes != null
                        && !nodes.isEmpty()) {
                        AccessibilityNodeInfo
                            btn = nodes.get(0);
                        if (btn.isEnabled()) {
                            btn.performAction(
                                AccessibilityNodeInfo
                                .ACTION_CLICK);
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Fallback by text
            clickSendButton2(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void clickSendButton2(
        AccessibilityNodeInfo root) {
        try {
            String[] texts = {
                "Send", "send", "SEND"};
            for (String text : texts) {
                List<AccessibilityNodeInfo>
                    nodes = root
                    .findAccessibilityNodeInfosByText(
                    text);
                if (nodes != null) {
                    for (AccessibilityNodeInfo
                        n : nodes) {
                        if (n.isClickable()
                            && n.isEnabled()) {
                            n.performAction(
                                AccessibilityNodeInfo
                                .ACTION_CLICK);
                            return;
                        }
                        AccessibilityNodeInfo
                            p = n.getParent();
                        if (p != null
                            && p.isClickable()
                            && p.isEnabled()) {
                            p.performAction(
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

    // Click by text and return success
    public boolean clickByTextReturns(
        String text) {
        try {
            AccessibilityNodeInfo root =
                getRootInActiveWindow();
            if (root == null) return false;

            List<AccessibilityNodeInfo>
                nodes = root
                .findAccessibilityNodeInfosByText(
                text);
            if (nodes != null) {
                for (AccessibilityNodeInfo n
                    : nodes) {
                    if (n.isClickable()
                        && n.isEnabled()) {
                        n.performAction(
                            AccessibilityNodeInfo
                            .ACTION_CLICK);
                        return true;
                    }
                    // Try parent node
                    AccessibilityNodeInfo p =
                        n.getParent();
                    if (p != null
                        && p.isClickable()
                        && p.isEnabled()) {
                        p.performAction(
                            AccessibilityNodeInfo
                            .ACTION_CLICK);
                        return true;
                    }
                    // Try grandparent
                    if (p != null) {
                        AccessibilityNodeInfo
                            gp = p.getParent();
                        if (gp != null
                            && gp.isClickable()
                            && gp.isEnabled()) {
                            gp.performAction(
                                AccessibilityNodeInfo
                                .ACTION_CLICK);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Click by text void version
    public void clickByText(String text) {
        clickByTextReturns(text);
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
