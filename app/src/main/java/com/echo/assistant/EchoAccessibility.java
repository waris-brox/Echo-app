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
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        receiver = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {
                try {
                    String action = intent.getStringExtra("action");
                    if (action == null) return;
                    switch (action) {
                        case "screenshot":
                            doScreenshot();
                            break;
                        case "click_send":
                            AccessibilityNodeInfo dbgRoot1 = getRootInActiveWindow();
                            if (dbgRoot1 != null) {
                                android.util.Log.d("ECHO_DEBUG", "=== DUMP: SEND BUTTON SCREEN ===");
                                dumpClickableNodes(dbgRoot1, 0);
                            }
                            clickSendButton();
                            break;
                        case "whatsapp_call_click":
                            AccessibilityNodeInfo dbgRoot2 = getRootInActiveWindow();
                            if (dbgRoot2 != null) {
                                android.util.Log.d("ECHO_DEBUG", "=== DUMP: VOICE CALL SCREEN ===");
                                dumpClickableNodes(dbgRoot2, 0);
                            }
                            clickWhatsAppCall(false);
                            break;
                        case "whatsapp_video_click":
                            AccessibilityNodeInfo dbgRoot3 = getRootInActiveWindow();
                            if (dbgRoot3 != null) {
                                android.util.Log.d("ECHO_DEBUG", "=== DUMP: VIDEO CALL SCREEN ===");
                                dumpClickableNodes(dbgRoot3, 0);
                            }
                            clickWhatsAppCall(true);
                            break;
                        case "back":
                            performGlobalAction(GLOBAL_ACTION_BACK);
                            break;
                        case "home":
                            performGlobalAction(GLOBAL_ACTION_HOME);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            IntentFilter f = new IntentFilter("com.echo.ACCESSIBILITY");
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(receiver, f, RECEIVER_NOT_EXPORTED);
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
                takeScreenshot(1, getMainExecutor(), new TakeScreenshotCallback() {
                    public void onSuccess(ScreenshotResult res) {
                        try {
                            android.graphics.Bitmap bm = android.graphics.Bitmap
                                .wrapHardwareBuffer(res.getHardwareBuffer(), res.getColorSpace());
                            android.provider.MediaStore.Images.Media.insertImage(
                                getContentResolver(), bm,
                                "Echo_" + System.currentTimeMillis(), "Echo Screenshot");
                            res.getHardwareBuffer().close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    public void onFailure(int code) {
                        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
                    }
                });
            } else {
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        }
    }

    public void clickSendButton() {
    try {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        String[] sendIds = {
                "com.whatsapp:id/send",
                "com.whatsapp:id/send_btn",
                "com.whatsapp:id/conversation_send_button",
                "com.whatsapp:id/mic_to_send_button"
        };

        for (String id : sendIds) {
            try {
                List<AccessibilityNodeInfo> nodes =
                        root.findAccessibilityNodeInfosByViewId(id);

                if (nodes != null) {
                    for (AccessibilityNodeInfo node : nodes) {

                        AccessibilityNodeInfo target = node;

                        while (target != null) {

                            if (target.isClickable()) {
                                target.performAction(
                                        AccessibilityNodeInfo.ACTION_CLICK
                                );
                                return;
                            }

                            target = target.getParent();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (clickByDescription(root, "Send")) return;
        if (clickByDescription(root, "send")) return;

        List<AccessibilityNodeInfo> sendTexts =
                root.findAccessibilityNodeInfosByText("Send");

        if (sendTexts != null) {
            for (AccessibilityNodeInfo node : sendTexts) {

                AccessibilityNodeInfo parent = node;

                while (parent != null) {

                    if (parent.isClickable()) {
                        parent.performAction(
                                AccessibilityNodeInfo.ACTION_CLICK
                        );
                        return;
                    }

                    parent = parent.getParent();
                }
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

    boolean clickByDescription(AccessibilityNodeInfo node, String desc) {
        if (node == null) return false;
        try {
            CharSequence cd = node.getContentDescription();
            if (cd != null && cd.toString().equalsIgnoreCase(desc) && node.isClickable() && node.isEnabled()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null && clickByDescription(child, desc)) return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    void clickBottomRightClickable(AccessibilityNodeInfo root) {
        try {
            android.graphics.Rect screenBounds = new android.graphics.Rect();
            root.getBoundsInScreen(screenBounds);
            int screenHeight = screenBounds.height();

            findAndClickInBottomArea(root, screenHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean findAndClickInBottomArea(AccessibilityNodeInfo node, int screenHeight) {
        if (node == null) return false;
        try {
            if (node.isClickable() && node.isEnabled()) {
                android.graphics.Rect bounds = new android.graphics.Rect();
                node.getBoundsInScreen(bounds);
                if (bounds.top > screenHeight * 0.80
                    && bounds.left > screenHeight * 0.5) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null && findAndClickInBottomArea(child, screenHeight)) return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    void dumpClickableNodes(AccessibilityNodeInfo node, int depth) {
        if (node == null) return;
        try {
            if (node.isClickable()) {
                android.graphics.Rect b = new android.graphics.Rect();
                node.getBoundsInScreen(b);
                String id = node.getViewIdResourceName();
                CharSequence desc = node.getContentDescription();
                CharSequence text = node.getText();
                android.util.Log.d("ECHO_DEBUG",
                    "CLICKABLE id=" + id
                    + " desc=" + desc
                    + " text=" + text
                    + " bounds=" + b.toString());
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                dumpClickableNodes(node.getChild(i), depth + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clickWhatsAppCall(boolean video) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;

            String[] ids = video
                ? new String[]{"com.whatsapp:id/menuitem_video_call"}
                : new String[]{"com.whatsapp:id/menuitem_voice_call"};

            for (String id : ids) {
                try {
                    List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
                    if (nodes != null && !nodes.isEmpty()) {
                        AccessibilityNodeInfo btn = nodes.get(0);
                        if (btn.isEnabled()) {
                            btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String desc = video ? "Video call" : "Voice call";
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(desc);
            if (nodes != null) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (n.isClickable() && n.isEnabled()) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean clickByTextReturns(String text) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;

            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
            if (nodes != null) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (n.isClickable() && n.isEnabled()) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                    AccessibilityNodeInfo p = n.getParent();
                    if (p != null && p.isClickable() && p.isEnabled()) {
                        p.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                    if (p != null) {
                        AccessibilityNodeInfo gp = p.getParent();
                        if (gp != null && gp.isClickable() && gp.isEnabled()) {
                            gp.performAction(AccessibilityNodeInfo.ACTION_CLICK);
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

    public void clickByText(String text) {
        clickByTextReturns(text);
    }

    public void onAccessibilityEvent(AccessibilityEvent e) {}

    public void onInterrupt() {}

    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            if (receiver != null) unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
