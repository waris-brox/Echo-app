package com.echo.assistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
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
import java.util.List;
import java.util.Locale;

public class DeviceActions {

    public interface Result {
        void onResult(boolean success,
            String message);
    }

    public static void handle(Context ctx,
        String json) {
        handle(ctx, json, (s, m) -> {
            if (!s && m != null
                && !m.isEmpty())
                Toast.makeText(ctx, m,
                Toast.LENGTH_SHORT).show();
        });
    }

    public static void handle(Context ctx,
        String json, Result result) {
        try {
            JSONObject a =
                new JSONObject(json);
            String type = a.getString("type");
            String target =
                a.optString("target", "");
            JSONObject p =
                a.optJSONObject("params");
            if (p == null) p =
                new JSONObject();
            switch (type) {
                case "call":
                    makeCall(ctx, target,
                        result); break;
                case "whatsapp_msg":
                    waMsg(ctx, target,
                        p.optString("text",""),
                        result); break;
                case "whatsapp_call":
                    waCall(ctx, target,
                        false, result); break;
                case "whatsapp_video":
                    waCall(ctx, target,
                        true, result); break;
                case "open_app":
                    openApp(ctx, target,
                        result); break;
                case "youtube_search":
                    ytSearch(ctx,
                        p.optString("query",""),
                        result); break;
                case "toggle_wifi":
                    wifi(ctx,
                        p.optString(
                        "state","on"),
                        result); break;
                case "toggle_flashlight":
                    flash(ctx,
                        p.optString(
                        "state","on"),
                        result); break;
                case "set_brightness":
                    brightness(ctx,
                        p.optInt("value",128),
                        result); break;
                case "take_screenshot":
                    screenshot(ctx,
                        result); break;
                case "set_alarm":
                    alarm(ctx,
                        p.optString(
                        "time","07:00"),
                        p.optString(
                        "label","Echo Alarm"),
                        result); break;
                case "set_reminder":
                    reminder(ctx,
                        p.optString("text",""),
                        p.optInt("minutes",30),
                        result); break;
                case "save_note":
                    note(ctx,
                        p.optString("text",""),
                        result); break;
                case "set_volume":
                    volume(ctx,
                        p.optInt("level",5),
                        result); break;
                case "toggle_mobile_data":
                    mobileData(ctx,
                        result); break;
                case "toggle_hotspot":
                    hotspot(ctx,
                        result); break;
                case "set_screen_timeout":
                    timeout(ctx,
                        p.optInt("seconds",30),
                        result); break;
                default:
                    result.onResult(false,
                        "Unknown: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.onResult(false,
                "Error: " + e.getMessage());
        }
    }

    // Find phone number from contacts
    static String findNum(Context c,
        String name) {
        if (name == null || name.isEmpty())
            return "";
        if (name.matches("[+\\d\\s\\-()]+"))
            return name;
        try {
            Cursor cur = c.getContentResolver()
                .query(
                ContactsContract
                .CommonDataKinds
                .Phone.CONTENT_URI,
                new String[]{ContactsContract
                    .CommonDataKinds
                    .Phone.NUMBER},
                ContactsContract
                .CommonDataKinds
                .Phone.DISPLAY_NAME
                + " LIKE ?",
                new String[]{
                    "%" + name + "%"},
                null);
            if (cur != null
                && cur.moveToFirst()) {
                String num =
                    cur.getString(0);
                cur.close();
                return num.replaceAll(
                    "[^0-9+]", "");
            }
            if (cur != null) cur.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    static void makeCall(Context c,
        String target, Result r) {
        try {
            String num = findNum(c, target);
            Intent i = new Intent(
                Intent.ACTION_CALL);
            i.setData(Uri.parse("tel:" + num));
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true,
                "Calling " + target);
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Call failed: "
                + e.getMessage());
        }
    }

    static void waMsg(Context c,
        String target, String text,
        Result r) {
        try {
            String num = findNum(c, target);
            num = num.replaceAll("[^0-9+]","");
            if (!num.startsWith("+")
                && !num.startsWith("91"))
                num = "91" + num;

            Uri uri = Uri.parse(
                "https://api.whatsapp.com"
                + "/send?phone=" + num
                + "&text="
                + Uri.encode(text));
            Intent i = new Intent(
                Intent.ACTION_VIEW, uri);
            i.setPackage("com.whatsapp");
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);

            // Click send after WhatsApp opens
            new Handler(Looper.getMainLooper())
                .postDelayed(() -> {
                try {
                    if (EchoAccessibility
                        .instance != null) {
                        EchoAccessibility
                            .instance
                            .clickSendButton();
                        r.onResult(true,
                            "Message sent"
                            + " to "
                            + target);
                    } else {
                        r.onResult(true,
                            "WhatsApp opened."
                            + " Enable Echo"
                            + " Accessibility"
                            + " for auto send.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 4000);

        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "WhatsApp error: "
                + e.getMessage());
        }
    }

    static void waCall(Context c,
        String target, boolean video,
        Result r) {
        try {
            String num = findNum(c, target);
            num = num.replaceAll("[^0-9+]","");
            if (!num.startsWith("+")
                && !num.startsWith("91"))
                num = "91" + num;

            Uri uri = Uri.parse(
                "https://api.whatsapp.com"
                + "/send?phone=" + num);
            Intent i = new Intent(
                Intent.ACTION_VIEW, uri);
            i.setPackage("com.whatsapp");
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);

            String btnText = video
                ? "Video call"
                : "Voice call";
            new Handler(Looper.getMainLooper())
                .postDelayed(() -> {
                try {
                    if (EchoAccessibility
                        .instance != null) {
                        EchoAccessibility
                            .instance
                            .clickByText(
                            btnText);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 3500);

            r.onResult(true,
                "WhatsApp "
                + (video ? "video " : "")
                + "call to " + target);
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "WA call failed");
        }
    }

    static void openApp(Context c,
        String name, Result r) {
        try {
            String lname = name.toLowerCase()
                .trim().replace(" ","");

            // Check mapped packages first
            String pkg = mapApp(lname);
            Intent launch = c
                .getPackageManager()
                .getLaunchIntentForPackage(pkg);
            if (launch != null) {
                launch.addFlags(
                    Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(launch);
                r.onResult(true,
                    "Opened " + name);
                return;
            }

            // Search all installed apps
            Intent searchIntent = new Intent(
                Intent.ACTION_MAIN);
            searchIntent.addCategory(
                Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = c
                .getPackageManager()
                .queryIntentActivities(
                searchIntent, 0);

            // Try exact match first
            for (ResolveInfo app : apps) {
                String label = app.loadLabel(
                    c.getPackageManager())
                    .toString()
                    .toLowerCase().trim()
                    .replace(" ","");
                if (label.equals(lname)) {
                    Intent l = c
                        .getPackageManager()
                        .getLaunchIntentForPackage(
                        app.activityInfo
                        .packageName);
                    if (l != null) {
                        l.addFlags(Intent
                            .FLAG_ACTIVITY_NEW_TASK);
                        c.startActivity(l);
                        r.onResult(true,
                            "Opened " + name);
                        return;
                    }
                }
            }

            // Try partial match
            for (ResolveInfo app : apps) {
                String label = app.loadLabel(
                    c.getPackageManager())
                    .toString()
                    .toLowerCase().trim();
                String nameClean =
                    name.toLowerCase().trim();
                if (label.contains(nameClean)
                    || nameClean
                    .contains(label)) {
                    Intent l = c
                        .getPackageManager()
                        .getLaunchIntentForPackage(
                        app.activityInfo
                        .packageName);
                    if (l != null) {
                        l.addFlags(Intent
                            .FLAG_ACTIVITY_NEW_TASK);
                        c.startActivity(l);
                        r.onResult(true,
                            "Opened " + name);
                        return;
                    }
                }
            }

            // Open Play Store as fallback
            try {
                Intent store = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                    "market://search?q="
                    + name));
                store.addFlags(Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(store);
                r.onResult(false,
                    name + " not found."
                    + " Opened Play Store.");
            } catch (Exception ex) {
                r.onResult(false,
                    name
                    + " not installed Boss.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Cannot open " + name);
        }
    }

    static String mapApp(String n) {
        switch (n) {
            case "instagram":
                return
                "com.instagram.android";
            case "whatsapp":
                return "com.whatsapp";
            case "youtube":
                return
                "com.google.android.youtube";
            case "camera":
                return
                "com.android.camera2";
            case "settings":
                return
                "com.android.settings";
            case "calculator":
            case "calc":
                return
                "com.android.calculator2";
            case "chrome":
                return
                "com.android.chrome";
            case "maps":
            case "googlemaps":
                return
                "com.google.android"
                + ".apps.maps";
            case "spotify":
                return "com.spotify.music";
            case "netflix":
                return
                "com.netflix.mediaclient";
            case "telegram":
                return
                "org.telegram.messenger";
            case "snapchat":
                return
                "com.snapchat.android";
            case "facebook":
                return
                "com.facebook.katana";
            case "twitter":
            case "x":
                return
                "com.twitter.android";
            case "gallery":
            case "photos":
                return
                "com.google.android"
                + ".apps.photos";
            case "contacts":
                return
                "com.android.contacts";
            case "phone":
            case "dialer":
                return "com.android.dialer";
            case "messages":
            case "sms":
                return "com.android.mms";
            case "clock":
            case "alarm":
                return
                "com.android.deskclock";
            case "calendar":
                return
                "com.android.calendar";
            case "files":
                return
                "com.android.documentsui";
            case "playstore":
            case "store":
                return
                "com.android.vending";
            case "gmail":
                return
                "com.google.android.gm";
            case "drive":
                return
                "com.google.android"
                + ".apps.docs";
            case "meet":
                return
                "com.google.android"
                + ".apps.tachyon";
            case "zoom":
                return
                "us.zoom.videomeetings";
            case "linkedin":
                return
                "com.linkedin.android";
            case "twitter/x":
                return
                "com.twitter.android";
            default:
                return n;
        }
    }

    static void ytSearch(Context c,
        String query, Result r) {
        try {
            if (query != null
                && !query.isEmpty()) {
                // Try YouTube app search
                try {
                    Intent i = new Intent(
                        Intent.ACTION_SEARCH);
                    i.setPackage(
                        "com.google.android"
                        + ".youtube");
                    i.putExtra("query", query);
                    i.addFlags(Intent
                        .FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(i);
                    r.onResult(true,
                        "Searching YouTube"
                        + " for " + query);
                    return;
                } catch (Exception ex) {
                    // YouTube app not found
                    // use browser
                    Intent browser =
                        new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                        "https://www.youtube"
                        + ".com/results"
                        + "?search_query="
                        + Uri.encode(query)));
                    browser.addFlags(Intent
                        .FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(browser);
                    r.onResult(true,
                        "YouTube search done");
                }
            } else {
                // Just open YouTube
                Intent i = c
                    .getPackageManager()
                    .getLaunchIntentForPackage(
                    "com.google.android"
                    + ".youtube");
                if (i != null) {
                    i.addFlags(Intent
                        .FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(i);
                    r.onResult(true,
                        "YouTube opened Boss");
                } else {
                    Intent b = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                        "https://youtube.com"));
                    b.addFlags(Intent
                        .FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(b);
                    r.onResult(true,
                        "YouTube in browser");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "YouTube error");
        }
    }

    static void wifi(Context c,
        String state, Result r) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                Intent i = new Intent(
                    Settings.Panel.ACTION_WIFI);
                i.addFlags(Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(true,
                    "WiFi panel opened Boss");
            } else {
                WifiManager wm =
                    (WifiManager) c
                    .getApplicationContext()
                    .getSystemService(
                    Context.WIFI_SERVICE);
                if (wm != null)
                    wm.setWifiEnabled(
                    state.equals("on"));
                r.onResult(true,
                    "WiFi " + state);
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false, "WiFi error");
        }
    }

    static void flash(Context c,
        String state, Result r) {
        try {
            CameraManager cm =
                (CameraManager) c
                .getSystemService(
                Context.CAMERA_SERVICE);
            String[] ids =
                cm.getCameraIdList();
            if (ids.length > 0) {
                cm.setTorchMode(ids[0],
                    state.equals("on"));
                r.onResult(true,
                    "Flashlight " + state);
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Flashlight error");
        }
    }

    static void brightness(Context c,
        int value, Result r) {
        try {
            if (Settings.System.canWrite(c)) {
                Settings.System.putInt(
                    c.getContentResolver(),
                    Settings.System
                    .SCREEN_BRIGHTNESS,
                    Math.min(255,
                    Math.max(0, value)));
                r.onResult(true,
                    "Brightness set");
            } else {
                Intent i = new Intent(
                    Settings
                    .ACTION_MANAGE_WRITE_SETTINGS);
                i.setData(Uri.parse(
                    "package:"
                    + c.getPackageName()));
                i.addFlags(Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(false,
                    "Allow write settings"
                    + " Boss then try again");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Brightness error");
        }
    }

    static void screenshot(Context c,
        Result r) {
        try {
            if (EchoAccessibility.instance
                != null) {
                EchoAccessibility.instance
                    .doScreenshot();
                r.onResult(true,
                    "Screenshot taken Boss");
            } else {
                Intent i = new Intent(
                    Settings
                    .ACTION_ACCESSIBILITY_SETTINGS);
                i.addFlags(Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(false,
                    "Enable Echo accessibility"
                    + " for screenshots Boss");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Screenshot failed");
        }
    }

    static void alarm(Context c,
        String time, String label,
        Result r) {
        try {
            String[] parts =
                time.trim().split(":");
            int hour = Integer
                .parseInt(parts[0].trim());
            int min = parts.length > 1
                ? Integer.parseInt(
                parts[1].trim()) : 0;
            Intent i = new Intent(
                AlarmClock.ACTION_SET_ALARM);
            i.putExtra(
                AlarmClock.EXTRA_HOUR, hour);
            i.putExtra(
                AlarmClock.EXTRA_MINUTES, min);
            i.putExtra(
                AlarmClock.EXTRA_MESSAGE,
                label);
            i.putExtra(
                AlarmClock.EXTRA_SKIP_UI,
                true);
            i.addFlags(Intent
                .FLAG_ACTIVITY_NEW_TASK);
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

    static void reminder(Context c,
        String text, int minutes,
        Result r) {
        try {
            Intent i = new Intent(
                AlarmClock.ACTION_SET_TIMER);
            i.putExtra(
                AlarmClock.EXTRA_LENGTH,
                minutes * 60);
            i.putExtra(
                AlarmClock.EXTRA_MESSAGE,
                text);
            i.putExtra(
                AlarmClock.EXTRA_SKIP_UI,
                true);
            i.addFlags(Intent
                .FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            note(c, "REMINDER in "
                + minutes + "min: " + text,
                (s, m) -> {});
            r.onResult(true,
                "Reminder set for "
                + minutes + " minutes");
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Reminder error");
        }
    }

    static void note(Context c,
        String text, Result r) {
        try {
            SharedPreferences p =
                c.getSharedPreferences(
                "echo_notes", 0);
            String ts = new SimpleDateFormat(
                "dd/MM HH:mm",
                Locale.getDefault())
                .format(new Date());
            p.edit().putString("notes",
                p.getString("notes","")
                + "\n[" + ts + "] " + text)
                .apply();
            r.onResult(true,
                "Note saved Boss");
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false, "Note error");
        }
    }

    static void volume(Context c,
        int level, Result r) {
        try {
            AudioManager am =
                (AudioManager) c
                .getSystemService(
                Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am
                    .getStreamMaxVolume(
                    AudioManager
                    .STREAM_MUSIC);
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
            r.onResult(false, "Volume error");
        }
    }

    static void mobileData(Context c,
        Result r) {
        try {
            Intent i = new Intent(
                Settings
                .ACTION_DATA_ROAMING_SETTINGS);
            i.addFlags(Intent
                .FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true,
                "Mobile data settings opened");
        } catch (Exception e) {
            try {
                Intent i = new Intent(
                    Settings
                    .ACTION_WIRELESS_SETTINGS);
                i.addFlags(Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(true,
                    "Network settings opened");
            } catch (Exception ex) {
                r.onResult(false,
                    "Cannot open data settings");
            }
        }
    }

    static void hotspot(Context c, Result r) {
        try {
            Intent i = new Intent();
            i.setClassName(
                "com.android.settings",
                "com.android.settings"
                + ".TetherSettings");
            i.addFlags(Intent
                .FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            r.onResult(true,
                "Hotspot settings opened");
        } catch (Exception e) {
            try {
                Intent i = new Intent(
                    Settings
                    .ACTION_WIRELESS_SETTINGS);
                i.addFlags(Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(true,
                    "Network settings opened");
            } catch (Exception ex) {
                r.onResult(false,
                    "Cannot open hotspot");
            }
        }
    }

    static void timeout(Context c,
        int seconds, Result r) {
        try {
            if (Settings.System.canWrite(c)) {
                Settings.System.putInt(
                    c.getContentResolver(),
                    Settings.System
                    .SCREEN_OFF_TIMEOUT,
                    seconds * 1000);
                r.onResult(true,
                    "Screen timeout "
                    + seconds + "s");
            } else {
                Intent i = new Intent(
                    Settings
                    .ACTION_MANAGE_WRITE_SETTINGS);
                i.setData(Uri.parse(
                    "package:"
                    + c.getPackageName()));
                i.addFlags(Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                r.onResult(false,
                    "Allow write settings Boss");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false, "Timeout error");
        }
    }
}
