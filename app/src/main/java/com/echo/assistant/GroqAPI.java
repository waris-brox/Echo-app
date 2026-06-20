package com.echo.assistant;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

    public static void handle(Context ctx, String json) {
        handle(ctx, json, (success, msg) -> {
            if (!success) {
                Toast.makeText(ctx, msg,
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void handle(Context ctx,
        String json, Result result) {
        try {
            JSONObject a = new JSONObject(json);
            String type = a.getString("type");
            String target = a.optString("target", "");
            JSONObject p = a.optJSONObject("params");
            if (p == null) p = new JSONObject();

            switch (type) {
                case "call":
                    makeCall(ctx, target, result);
                    break;
                case "whatsapp_msg":
                    sendWhatsApp(ctx, target,
                        p.optString("text",""), result);
                    break;
                case "whatsapp_call":
                    waCall(ctx, target, false, result);
                    break;
                case "whatsapp_video":
                    waCall(ctx, target, true, result);
                    break;
                case "open_app":
                    openApp(ctx, target, result);
                    break;
                case "toggle_wifi":
                    toggleWifi(ctx,
                        p.optString("state","on"), result);
                    break;
                case "toggle_mobile_data":
                    toggleMobileData(ctx,
                        p.optString("state","on"), result);
                    break;
                case "toggle_hotspot":
                    openHotspot(ctx, result);
                    break;
                case "toggle_flashlight":
                    toggleFlash(ctx,
                        p.optString("state","on"), result);
                    break;
                case "set_brightness":
                    setBrightness(ctx,
                        p.optInt("value",128), result);
                    break;
                case "take_screenshot":
                    takeScreenshot(ctx, result);
                    break;
                case "set_screen_timeout":
                    setScreenTimeout(ctx,
                        p.optInt("seconds",30), result);
                    break;
                case "set_volume":
                    setVolume(ctx,
                        p.optInt("level",5), result);
                    break;
                case "set_alarm":
                    setAlarm(ctx,
                        p.optString("time","07:00"),
                        p.optString("label","Echo Alarm"),
                        result);
                    break;
                case "set_reminder":
                    setReminder(ctx,
                        p.optString("text",""),
                        p.optInt("minutes",30), result);
                    break;
                case "save_note":
                    saveNote(ctx,
                        p.optString("text",""), result);
                    break;
                default:
                    result.onResult(false,
                        "Unknown action: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.onResult(false,
                "Action error: " + e.getMessage());
        }
    }

    // ── CONTACTS ────────────────────────────────────────

    static String findNumber(Context c, String name) {
        if (name == null || name.isEmpty()) return "";
        // Already a number
        if (name.matches("[+\\d\\s\\-()]+")) return name;
        try {
            Cursor cursor = c.getContentResolver().query(
                ContactsContract.CommonDataKinds
                    .Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds
                        .Phone.NUMBER,
                    ContactsContract.CommonDataKinds
                        .Phone.DISPLAY_NAME},
                ContactsContract.CommonDataKinds
                    .Phone.DISPLAY_NAME
                    + " LIKE ?",
                new String[]{"%" + name + "%"},
                null);
            if (cursor != null && cursor.moveToFirst()) {
                String num = cursor.getString(0);
                cursor.close();
                return num.replaceAll("[^0-9+]", "");
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    // ── CALLS ───────────────────────────────────────────

    static void makeCall(Context c, String target,
        Result r) {
        try {
            String number = findNumber(c, target);
            if (number.isEmpty()) {
                r.onResult(false,
                    "Could not find number for "
                    + target);
                return;
            }
            Intent i = new Intent(Intent.ACTION_CALL);
            i.setData(Uri.parse("tel:" + number));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true, "Calling " + target);
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false, "Call failed: "
                + e.getMessage());
        }
    }

    // ── WHATSAPP ────────────────────────────────────────

    static void sendWhatsApp(Context c, String target,
        String text, Result r) {
        try {
            String number = findNumber(c, target);
            // Remove non-numeric except +
            number = number.replaceAll("[^0-9+]", "");
            // Add country code if missing
            if (!number.startsWith("+")
                && !number.startsWith("91")) {
                number = "91" + number;
            }

            // Method 1: Direct WhatsApp intent
            try {
                Intent i = new Intent(
                    Intent.ACTION_VIEW);
                i.setData(Uri.parse(
                    "https://api.whatsapp.com/send?"
                    + "phone=" + number
                    + "&text="
                    + Uri.encode(text)));
                i.setPackage("com.whatsapp");
                i.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);

                // After WhatsApp opens click send
                // via Accessibility
                new Handler(Looper.getMainLooper())
                    .postDelayed(() -> {
                    try {
                        Intent acc = new Intent(
                            "com.echo.ACCESSIBILITY");
                        acc.setPackage(
                            c.getPackageName());
                        acc.putExtra("action",
                            "click_send");
                        c.sendBroadcast(acc);
                    } catch (Exception ignored) {}
                }, 3000);

                r.onResult(true,
                    "WhatsApp opened for " + target);
            } catch (Exception e) {
                // Fallback to web
                Intent fallback = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                    "https://wa.me/" + number
                    + "?text="
                    + Uri.encode(text)));
                fallback.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(fallback);
                r.onResult(true,
                    "WhatsApp opened in browser");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "WhatsApp error: " + e.getMessage());
        }
    }

    static void waCall(Context c, String target,
        boolean video, Result r) {
        try {
            String number = findNumber(c, target);
            number = number.replaceAll("[^0-9+]", "");
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(
                "https://wa.me/" + number));
            i.setPackage("com.whatsapp");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true,
                "WhatsApp call started");
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "WhatsApp call failed");
        }
    }

    // ── OPEN APP ────────────────────────────────────────

    static void openApp(Context c, String name,
        Result r) {
        try {
            String pkg = mapApp(name.toLowerCase()
                .trim().replace(" ", ""));

            // Try direct package first
            Intent i = c.getPackageManager()
                .getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(true, "Opened " + name);
                return;
            }

            // Search all installed apps by label
            Intent search = new Intent(
                Intent.ACTION_MAIN);
            search.addCategory(
                Intent.CATEGORY_LAUNCHER);
            java.util.List<android.content.pm
                .ResolveInfo> apps = c
                .getPackageManager()
                .queryIntentActivities(search, 0);

            String lname = name.toLowerCase();
            for (android.content.pm.ResolveInfo app
                : apps) {
                String label = app.loadLabel(
                    c.getPackageManager())
                    .toString().toLowerCase();
                if (label.contains(lname)
                    || lname.contains(label)) {
                    Intent launch = c
                        .getPackageManager()
                        .getLaunchIntentForPackage(
                        app.activityInfo
                        .packageName);
                    if (launch != null) {
                        launch.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK);
                        c.startActivity(launch);
                        r.onResult(true,
                            "Opened " + name);
                        return;
                    }
                }
            }

            // Nothing found open play store
            Intent store = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                "market://search?q=" + name));
            store.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(store);
            r.onResult(false,
                name + " not found. "
                + "Opened Play Store.");

        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Could not open " + name);
        }
    }

    static String mapApp(String n) {
        switch (n) {
            case "instagram":
                return "com.instagram.android";
            case "whatsapp":
                return "com.whatsapp";
            case "youtube":
                return "com.google.android.youtube";
            case "camera":
                return "com.android.camera2";
            case "settings":
                return "com.android.settings";
            case "calculator":
            case "calc":
                return "com.android.calculator2";
            case "chrome":
                return "com.android.chrome";
            case "maps":
            case "googlemaps":
                return
                "com.google.android.apps.maps";
            case "spotify":
                return "com.spotify.music";
            case "netflix":
                return "com.netflix.mediaclient";
            case "telegram":
                return "org.telegram.messenger";
            case "snapchat":
                return "com.snapchat.android";
            case "facebook":
                return "com.facebook.katana";
            case "twitter":
            case "x":
                return "com.twitter.android";
            case "gallery":
            case "photos":
                return
                "com.google.android.apps.photos";
            case "contacts":
                return "com.android.contacts";
            case "phone":
            case "dialer":
                return "com.android.dialer";
            case "messages":
            case "sms":
                return "com.android.mms";
            case "clock":
            case "alarm":
                return "com.android.deskclock";
            case "calendar":
                return "com.android.calendar";
            case "files":
            case "filemanager":
                return "com.android.documentsui";
            case "playstore":
            case "store":
                return "com.android.vending";
            case "gmail":
                return "com.google.android.gm";
            case "drive":
                return
                "com.google.android.apps.docs";
            case "meet":
                return
                "com.google.android.apps.tachyon";
            case "zoom":
                return "us.zoom.videomeetings";
            default:
                return n;
        }
    }

    // ── WIFI ────────────────────────────────────────────

    static void toggleWifi(Context c, String state,
        Result r) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                // Android 10+ cannot toggle wifi
                // programmatically — open panel
                Intent panel = new Intent(
                    Settings.Panel.ACTION_WIFI);
                panel.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(panel);
                r.onResult(true,
                    "WiFi panel opened Boss."
                    + " Toggle it there.");
            } else {
                WifiManager wm = (WifiManager)
                    c.getApplicationContext()
                    .getSystemService(
                    Context.WIFI_SERVICE);
                if (wm != null) {
                    wm.setWifiEnabled(
                        state.equals("on"));
                    r.onResult(true,
                        "WiFi turned " + state);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Intent i = new Intent(
                Settings.ACTION_WIFI_SETTINGS);
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true,
                "Opened WiFi settings Boss");
        }
    }

    // ── MOBILE DATA ─────────────────────────────────────

    static void toggleMobileData(Context c,
        String state, Result r) {
        try {
            // Android restricts this — open settings
            Intent i = new Intent(
                Settings.ACTION_DATA_ROAMING_SETTINGS);
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true,
                "Mobile data settings opened Boss");
        } catch (Exception e) {
            try {
                Intent i = new Intent(
                    Settings
                    .ACTION_WIRELESS_SETTINGS);
                i.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(true,
                    "Network settings opened");
            } catch (Exception ex) {
                r.onResult(false,
                    "Could not open data settings");
            }
        }
    }

    // ── HOTSPOT ─────────────────────────────────────────

    static void openHotspot(Context c, Result r) {
        try {
            Intent i = new Intent();
            i.setClassName("com.android.settings",
                "com.android.settings"
                + ".TetherSettings");
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true,
                "Hotspot settings opened Boss");
        } catch (Exception e) {
            try {
                Intent i = new Intent(
                    Settings
                    .ACTION_WIRELESS_SETTINGS);
                i.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(true,
                    "Network settings opened");
            } catch (Exception ex) {
                r.onResult(false,
                    "Could not open hotspot");
            }
        }
    }

    // ── FLASHLIGHT ──────────────────────────────────────

    static void toggleFlash(Context c, String state,
        Result r) {
        try {
            CameraManager cm = (CameraManager)
                c.getSystemService(
                Context.CAMERA_SERVICE);
            String[] ids = cm.getCameraIdList();
            if (ids.length > 0) {
                cm.setTorchMode(ids[0],
                    state.equals("on"));
                r.onResult(true,
                    "Flashlight " + state);
            } else {
                r.onResult(false,
                    "No camera found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Flashlight error: "
                + e.getMessage());
        }
    }

    // ── BRIGHTNESS ──────────────────────────────────────

    static void setBrightness(Context c, int value,
        Result r) {
        try {
            if (Settings.System.canWrite(c)) {
                Settings.System.putInt(
                    c.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    Math.min(255,
                    Math.max(0, value)));
                r.onResult(true,
                    "Brightness set to "
                    + (value*100/255) + "%");
            } else {
                // Request write settings permission
                Intent i = new Intent(
                    Settings
                    .ACTION_MANAGE_WRITE_SETTINGS);
                i.setData(Uri.parse(
                    "package:"
                    + c.getPackageName()));
                i.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(false,
                    "Boss please allow "
                    + "write settings permission "
                    + "then try again");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Brightness error: "
                + e.getMessage());
        }
    }

    // ── SCREENSHOT ──────────────────────────────────────

    static void takeScreenshot(Context c, Result r) {
        try {
            if (EchoAccessibility.instance != null) {
                EchoAccessibility.instance
                    .doScreenshot();
                r.onResult(true,
                    "Screenshot taken Boss");
            } else {
                // Ask user to enable accessibility
                Intent i = new Intent(
                    Settings
                    .ACTION_ACCESSIBILITY_SETTINGS);
                i.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(false,
                    "Boss please enable Echo "
                    + "in Accessibility Settings "
                    + "for screenshots to work");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Screenshot failed: "
                + e.getMessage());
        }
    }

    // ── SCREEN TIMEOUT ──────────────────────────────────

    static void setScreenTimeout(Context c,
        int seconds, Result r) {
        try {
            if (Settings.System.canWrite(c)) {
                Settings.System.putInt(
                    c.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    seconds * 1000);
                r.onResult(true,
                    "Screen timeout set to "
                    + seconds + " seconds");
            } else {
                Intent i = new Intent(
                    Settings
                    .ACTION_MANAGE_WRITE_SETTINGS);
                i.setData(Uri.parse(
                    "package:"
                    + c.getPackageName()));
                i.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(false,
                    "Boss please allow "
                    + "write settings first");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Timeout error: "
                + e.getMessage());
        }
    }

    // ── VOLUME ──────────────────────────────────────────

    static void setVolume(Context c, int level,
        Result r) {
        try {
            AudioManager am = (AudioManager)
                c.getSystemService(
                Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(
                    AudioManager.STREAM_MUSIC);
                int vol = (int)(
                    (level / 10.0) * max);
                am.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    Math.min(vol, max),
                    AudioManager.FLAG_SHOW_UI);
                r.onResult(true,
                    "Volume set to "
                    + level + "/10");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Volume error: "
                + e.getMessage());
        }
    }

    // ── ALARM ───────────────────────────────────────────

    static void setAlarm(Context c, String time,
        String label, Result r) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(
                parts[0].trim());
            int min = parts.length > 1
                ? Integer.parseInt(
                parts[1].trim()) : 0;
            Intent i = new Intent(
                AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR, hour);
            i.putExtra(
                AlarmClock.EXTRA_MINUTES, min);
            i.putExtra(
                AlarmClock.EXTRA_MESSAGE, label);
            i.putExtra(
                AlarmClock.EXTRA_SKIP_UI, true);
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true,
                "Alarm set for " + time);
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Alarm error: "
                + e.getMessage());
        }
    }

    // ── REMINDER ────────────────────────────────────────

    static void setReminder(Context c, String text,
        int minutes, Result r) {
        try {
            // Save as note with time
            String ts = new SimpleDateFormat(
                "dd/MM HH:mm",
                Locale.getDefault())
                .format(new Date());
            saveNote(c, "REMINDER [" + ts + " +"
                + minutes + "min]: " + text,
                (s, m) -> {});

            // Also set alarm if possible
            long triggerTime = System
                .currentTimeMillis()
                + (minutes * 60 * 1000L);
            Intent alarm = new Intent(
                AlarmClock.ACTION_SET_TIMER);
            alarm.putExtra(
                AlarmClock.EXTRA_LENGTH,
                minutes * 60);
            alarm.putExtra(
                AlarmClock.EXTRA_MESSAGE, text);
            alarm.putExtra(
                AlarmClock.EXTRA_SKIP_UI, true);
            alarm.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(alarm);

            r.onResult(true,
                "Reminder set for "
                + minutes + " minutes");
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Reminder error: "
                + e.getMessage());
        }
    }

    // ── NOTES ───────────────────────────────────────────

    static void saveNote(Context c, String text,
        Result r) {
        try {
            SharedPreferences p =
                c.getSharedPreferences(
                "echo_notes", 0);
            String ts = new SimpleDateFormat(
                "dd/MM HH:mm",
                Locale.getDefault())
                .format(new Date());
            String existing =
                p.getString("notes", "");
            p.edit().putString("notes",
                existing + "\n["
                + ts + "] " + text)
                .apply();
            r.onResult(true,
                "Note saved Boss");
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Note error: "
                + e.getMessage());
        }
    }
}
