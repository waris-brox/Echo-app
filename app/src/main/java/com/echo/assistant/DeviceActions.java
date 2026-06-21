package com.echo.assistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
            String type =
                a.getString("type");
            String target =
                a.optString("target", "");
            JSONObject p =
                a.optJSONObject("params");
            if (p == null)
                p = new JSONObject();
            switch (type) {
                case "call":
                    makeCall(ctx, target,
                        result); break;
                case "whatsapp_msg":
                    waMsg(ctx, target,
                        p.optString("text",""),
                        result); break;
                case "whatsapp_call":
                    waVoiceCall(ctx, target,
                        result); break;
                case "whatsapp_video":
                    waVideoCall(ctx, target,
                        result); break;
                case "open_app":
                    openApp(ctx, target,
                        result); break;
                case "youtube_search":
                    ytSearch(ctx,
                        p.optString("query",""),
                        result); break;
                case "toggle_wifi":
                    wifi(ctx,
                        p.optString("state","on"),
                        result); break;
                case "toggle_flashlight":
                    flash(ctx,
                        p.optString("state","on"),
                        result); break;
                case "set_brightness":
                    brightness(ctx,
                        p.optInt("value", 128),
                        result); break;
                case "take_screenshot":
                    screenshot(ctx,
                        result); break;
                case "set_alarm":
                    alarm(ctx,
                        p.optString("time","07:00"),
                        p.optString("label","Echo Alarm"),
                        result); break;
                case "set_reminder":
                    reminder(ctx,
                        p.optString("text",""),
                        p.optInt("minutes", 30),
                        result); break;
                case "save_note":
                    note(ctx,
                        p.optString("text",""),
                        result); break;
                case "set_volume":
                    volume(ctx,
                        p.optInt("level", 5),
                        result); break;
                case "toggle_mobile_data":
                    mobileData(ctx,
                        result); break;
                case "toggle_hotspot":
                    hotspot(ctx,
                        result); break;
                case "set_screen_timeout":
                    timeout(ctx,
                        p.optInt("seconds", 30),
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

    // ── FIND CONTACT NUMBER ───────────────────

    static String findNum(Context c,
        String name) {
        if (name == null || name.isEmpty())
            return "";
        if (name.matches("[+\\d\\s\\-()]+"))
            return name;
        try {
            Cursor cur =
                c.getContentResolver().query(
                ContactsContract
                .CommonDataKinds
                .Phone.CONTENT_URI,
                new String[]{
                    ContactsContract
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

    static String formatWANumber(
        Context c, String target) {
        String num = findNum(c, target);
        num = num.replaceAll("[^0-9+]", "");
        if (!num.startsWith("+")
            && !num.startsWith("91")
            && num.length() == 10) {
            num = "91" + num;
        }
        return num;
    }

    // ── PHONE CALL ────────────────────────────

    static void makeCall(Context c,
        String target, Result r) {
        try {
            String num = findNum(c, target);
            Intent i = new Intent(
                Intent.ACTION_CALL);
            i.setData(
                Uri.parse("tel:" + num));
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

    // ── WHATSAPP MESSAGE ──────────────────────
    // Opens WhatsApp with message pre-typed
    // Accessibility will click send button

    static void waMsg(Context c,
        String target, String text,
        Result r) {
        try {
            String num =
                formatWANumber(c, target);

            // This URL opens WhatsApp chat
            // with message already typed
            // in the input box
            String url =
                "https://api.whatsapp.com"
                + "/send?phone=" + num
                + "&text="
                + Uri.encode(text);

            Intent i = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url));
            i.setPackage("com.whatsapp");
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent
                .FLAG_ACTIVITY_CLEAR_TOP);
            c.startActivity(i);

            // Wait for WhatsApp to load
            // then click send button
            // via Accessibility service
            new Handler(
                Looper.getMainLooper())
                .postDelayed(() -> {
                try {
                    if (EchoAccessibility
                        .instance != null) {
                        EchoAccessibility
                            .instance
                            .clickSendButton();
                        r.onResult(true,
                            "Message sent"
                            + " to " + target);
                    } else {
                        r.onResult(true,
                            "WhatsApp opened"
                            + " with message."
                            + " Enable Echo"
                            + " Accessibility"
                            + " to auto send.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    r.onResult(true,
                        "WhatsApp opened Boss");
                }
            }, 3500);

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback
            try {
                String num =
                    formatWANumber(c, target);
                Intent fb = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                    "https://wa.me/" + num
                    + "?text="
                    + Uri.encode(text)));
                fb.addFlags(
                    Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(fb);
                r.onResult(true,
                    "WhatsApp opened Boss");
            } catch (Exception ex) {
                r.onResult(false,
                    "WhatsApp not installed");
            }
        }
    }

    // ── WHATSAPP VOICE CALL ───────────────────
    // Uses direct dial intent to start
    // voice call immediately

    static void waVoiceCall(Context c,
        String target, Result r) {
        try {
            String num =
                formatWANumber(c, target);

            // Method 1: Direct WhatsApp
            // voice call intent
            try {
                Intent i = new Intent();
                i.setAction(
                    Intent.ACTION_VIEW);
                i.setData(Uri.parse(
                    "https://wa.me/" + num));
                i.setPackage("com.whatsapp");
                i.addFlags(
                    Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);

                // After contact opens
                // tap voice call button
                new Handler(
                    Looper.getMainLooper())
                    .postDelayed(() -> {
                    try {
                        if (EchoAccessibility
                            .instance != null) {
                            // Try multiple
                            // button texts
                            boolean clicked =
                                EchoAccessibility
                                .instance
                                .clickByTextReturns(
                                "Voice call");
                            if (!clicked)
                                clicked =
                                EchoAccessibility
                                .instance
                                .clickByTextReturns(
                                "Call");
                            if (!clicked)
                                EchoAccessibility
                                .instance
                                .clickByTextReturns(
                                "Audio call");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 3000);

                r.onResult(true,
                    "WhatsApp voice call"
                    + " to " + target);

            } catch (Exception e1) {
                // Fallback to phone call
                makeCall(c, target, r);
            }

        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "WhatsApp call failed");
        }
    }

    // ── WHATSAPP VIDEO CALL ───────────────────

    static void waVideoCall(Context c,
        String target, Result r) {
        try {
            String num =
                formatWANumber(c, target);
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(
                "https://wa.me/" + num));
            i.setPackage("com.whatsapp");
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);

            new Handler(
                Looper.getMainLooper())
                .postDelayed(() -> {
                try {
                    if (EchoAccessibility
                        .instance != null) {
                        boolean clicked =
                            EchoAccessibility
                            .instance
                            .clickByTextReturns(
                            "Video call");
                        if (!clicked)
                            EchoAccessibility
                            .instance
                            .clickByTextReturns(
                            "Video");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 3000);

            r.onResult(true,
                "WhatsApp video call"
                + " to " + target);

        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "WhatsApp video failed");
        }
    }

    // ── OPEN ANY APP ──────────────────────────

    static void openApp(Context c,
        String name, Result r) {
        try {
            if (name == null
                || name.isEmpty()) {
                r.onResult(false,
                    "No app name Boss");
                return;
            }

            String lname =
                name.toLowerCase()
                .trim()
                .replace(" ", "");

            // Check if YouTube with search
            if (lname.equals("youtube")
                || lname.contains("youtube")) {
                ytSearch(c, "", r);
                return;
            }

            // Try mapped package name first
            String pkg = mapApp(lname);
            if (tryLaunch(c, pkg)) {
                r.onResult(true,
                    "Opened " + name);
                return;
            }

            // Search all installed apps
            // by display name
            Intent si = new Intent(
                Intent.ACTION_MAIN);
            si.addCategory(
                Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps =
                c.getPackageManager()
                .queryIntentActivities(si, 0);

            String nameClean =
                name.toLowerCase().trim();

            // Exact match
            for (ResolveInfo app : apps) {
                String label =
                    app.loadLabel(
                    c.getPackageManager())
                    .toString()
                    .toLowerCase().trim();
                if (label.equals(nameClean)) {
                    if (tryLaunch(c,
                        app.activityInfo
                        .packageName)) {
                        r.onResult(true,
                            "Opened " + name);
                        return;
                    }
                }
            }

            // Partial match
            for (ResolveInfo app : apps) {
                String label =
                    app.loadLabel(
                    c.getPackageManager())
                    .toString()
                    .toLowerCase().trim();
                String pkgName =
                    app.activityInfo
                    .packageName
                    .toLowerCase();
                if (label.contains(nameClean)
                    || nameClean
                    .contains(label)
                    || pkgName
                    .contains(nameClean)) {
                    if (tryLaunch(c,
                        app.activityInfo
                        .packageName)) {
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
                    + " not installed Boss");
            }

        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Cannot open " + name);
        }
    }

    static boolean tryLaunch(Context c,
        String pkg) {
        try {
            Intent i = c.getPackageManager()
                .getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(
                    Intent
                    .FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static String mapApp(String n) {
        switch (n) {
            // Social
            case "instagram":
                return
                "com.instagram.android";
            case "whatsapp":
                return "com.whatsapp";
            case "facebook":
                return
                "com.facebook.katana";
            case "messenger":
                return
                "com.facebook.orca";
            case "snapchat":
                return
                "com.snapchat.android";
            case "twitter":
            case "x":
                return
                "com.twitter.android";
            case "telegram":
                return
                "org.telegram.messenger";
            case "linkedin":
                return
                "com.linkedin.android";
            case "pinterest":
                return
                "com.pinterest";
            case "reddit":
                return
                "com.reddit.frontpage";
            case "discord":
                return "com.discord";
            case "tiktok":
                return
                "com.zhiliaoapp.musically";
            case "threads":
                return
                "com.instagram.barcelona";
            // Video
            case "youtube":
                return
                "com.google.android.youtube";
            case "netflix":
                return
                "com.netflix.mediaclient";
            case "primevideo":
            case "amazon":
            case "amazonprime":
                return
                "com.amazon.avod"
                + ".thirdpartyclient";
            case "hotstar":
            case "disneyplushotstar":
                return
                "in.startv.hotstar";
            case "jiocinema":
                return
                "com.jio.media.ondemand";
            case "sonyliv":
                return
                "com.sonyliv";
            case "zee5":
                return
                "com.graymatrix.did";
            case "mxplayer":
                return
                "com.mxtech.videoplayer"
                + ".ad";
            case "vlc":
                return
                "org.videolan.vlc";
            // Music
            case "spotify":
                return "com.spotify.music";
            case "jiosaavn":
            case "saavn":
                return
                "com.jio.media.jiobeats";
            case "gaana":
                return
                "com.gaana";
            case "wynk":
                return
                "com.bsbportal.music";
            case "youtubemusic":
                return
                "com.google.android"
                + ".apps.youtube.music";
            case "amazonmusic":
                return
                "com.amazon.mp3";
            // Google Apps
            case "gmail":
                return
                "com.google.android.gm";
            case "drive":
            case "googledrive":
                return
                "com.google.android"
                + ".apps.docs";
            case "maps":
            case "googlemaps":
                return
                "com.google.android"
                + ".apps.maps";
            case "meet":
            case "googlemeet":
                return
                "com.google.android"
                + ".apps.tachyon";
            case "photos":
            case "googlephotos":
                return
                "com.google.android"
                + ".apps.photos";
            case "docs":
            case "googledocs":
                return
                "com.google.android"
                + ".apps.docs";
            case "sheets":
            case "googlesheets":
                return
                "com.google.android"
                + ".apps.spreadsheets";
            case "slides":
            case "googleslides":
                return
                "com.google.android"
                + ".apps.presentations";
            case "keep":
            case "googlekeep":
                return
                "com.google.android"
                + ".keep";
            case "translate":
            case "googletranslate":
                return
                "com.google.android"
                + ".apps.translate";
            case "chrome":
            case "googlechrome":
                return
                "com.android.chrome";
            case "lens":
            case "googlelens":
                return
                "com.google.ar.lens";
            case "pay":
            case "gpay":
            case "googlepay":
                return
                "com.google.android"
                + ".apps.nbu.paisa.user";
            // System Apps
            case "settings":
                return
                "com.android.settings";
            case "calculator":
            case "calc":
                return
                "com.android.calculator2";
            case "calendar":
                return
                "com.android.calendar";
            case "camera":
                return
                "com.android.camera2";
            case "gallery":
                return
                "com.google.android"
                + ".apps.photos";
            case "contacts":
                return
                "com.android.contacts";
            case "phone":
            case "dialer":
                return
                "com.android.dialer";
            case "messages":
            case "sms":
                return
                "com.android.mms";
            case "clock":
            case "alarmclock":
                return
                "com.android.deskclock";
            case "files":
            case "filemanager":
                return
                "com.android.documentsui";
            case "playstore":
            case "googleplay":
                return
                "com.android.vending";
            case "browser":
            case "internetbrowser":
                return
                "com.android.browser";
            // Payment
            case "paytm":
                return
                "net.one97.paytm";
            case "phonepe":
                return
                "com.phonepe.app";
            case "bhim":
                return
                "in.org.npci.upiapp";
            case "amazonpay":
                return
                "in.amazon.mShop"
                + ".android.shopping";
            // Shopping
            case "flipkart":
                return
                "com.flipkart.android";
            case "amazonshopping":
            case "amazonshop":
                return
                "in.amazon.mShop"
                + ".android.shopping";
            case "meesho":
                return
                "com.meesho.supply";
            case "myntra":
                return
                "com.myntra.android";
            case "nykaa":
                return "com.nykaa.user";
            // Food
            case "swiggy":
                return
                "in.swiggy.android";
            case "zomato":
                return
                "com.application.zomato";
            case "dunzo":
                return
                "co.dunzo.android";
            case "blinkit":
                return
                "com.grofers.customerapp";
            // Travel
            case "ola":
                return
                "com.olacabs.customer";
            case "uber":
                return
                "com.ubercab";
            case "rapido":
                return
                "com.rapido.passenger";
            case "irctc":
                return
                "cris.org.in.prs.ima";
            case "makemytrip":
                return
                "com.makemytrip";
            case "goibibo":
                return "com.goibibo";
            // Other
            case "zoom":
                return
                "us.zoom.videomeetings";
            case "microsoftteams":
            case "teams":
                return
                "com.microsoft.teams";
            case "outlook":
                return
                "com.microsoft.office"
                + ".outlook";
            case "word":
                return
                "com.microsoft.office.word";
            case "excel":
                return
                "com.microsoft.office"
                + ".excel";
            case "truecaller":
                return
                "com.truecaller";
            case "phonelink":
                return
                "com.microsoft.appmanager";
            default:
                return n;
        }
    }

    // ── YOUTUBE SEARCH ────────────────────────

    static void ytSearch(Context c,
        String query, Result r) {
        try {
            if (query != null
                && !query.isEmpty()) {
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
                } catch (Exception ex) {
                    Intent b = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                        "https://www.youtube"
                        + ".com/results"
                        + "?search_query="
                        + Uri.encode(query)));
                    b.addFlags(Intent
                        .FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(b);
                    r.onResult(true,
                        "YouTube search done");
                }
            } else {
                boolean opened =
                    tryLaunch(c,
                    "com.google.android"
                    + ".youtube");
                if (!opened) {
                    Intent b = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                        "https://youtube.com"));
                    b.addFlags(Intent
                        .FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(b);
                }
                r.onResult(true,
                    "YouTube opened Boss");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "YouTube error");
        }
    }

    // ── ALARM ─────────────────────────────────

    static void alarm(Context c,
        String time, String label,
        Result r) {
        try {
            // Parse time like 7AM, 7:30AM,
            // 7:30, 07:00 etc
            time = time.trim()
                .toUpperCase()
                .replace(" ", "");
            boolean isPM =
                time.contains("PM");
            boolean isAM =
                time.contains("AM");
            time = time
                .replace("AM", "")
                .replace("PM", "")
                .trim();

            int hour = 7, min = 0;
            if (time.contains(":")) {
                String[] parts =
                    time.split(":");
                hour = Integer
                    .parseInt(
                    parts[0].trim());
                min = Integer
                    .parseInt(
                    parts[1].trim());
            } else {
                hour = Integer
                    .parseInt(time.trim());
            }

            if (isPM && hour < 12)
                hour += 12;
            if (isAM && hour == 12)
                hour = 0;

            Intent i = new Intent(
                AlarmClock.ACTION_SET_ALARM);
            i.putExtra(
                AlarmClock.EXTRA_HOUR, hour);
            i.putExtra(
                AlarmClock.EXTRA_MINUTES,
                min);
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
                "Alarm set for "
                + hour + ":"
                + String.format("%02d", min));
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Alarm error: "
                + e.getMessage());
        }
    }

    // ── REMINDER ──────────────────────────────

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
            note(c,
                "REMINDER in "
                + minutes + "min: " + text,
                (s, m) -> {});
            r.onResult(true,
                "Reminder set for "
                + minutes + " minutes Boss");
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Reminder error: "
                + e.getMessage());
        }
    }

    // ── SAVE NOTE ─────────────────────────────

    static void note(Context c,
        String text, Result r) {
        try {
            if (text == null
                || text.trim().isEmpty()) {
                r.onResult(false,
                    "No text to save Boss");
                return;
            }
            SharedPreferences p =
                c.getSharedPreferences(
                "echo_notes", 0);
            String ts =
                new SimpleDateFormat(
                "dd/MM HH:mm",
                Locale.getDefault())
                .format(new Date());
            String existing =
                p.getString("notes", "");
            p.edit().putString("notes",
                existing + "\n["
                + ts + "] "
                + text.trim()).apply();
            r.onResult(true,
                "Note saved Boss: "
                + text);
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Note error: "
                + e.getMessage());
        }
    }

    // ── WIFI ──────────────────────────────────

    static void wifi(Context c,
        String state, Result r) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                Intent i = new Intent(
                    Settings.Panel
                    .ACTION_WIFI);
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

    // ── FLASHLIGHT ────────────────────────────

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

    // ── BRIGHTNESS ────────────────────────────

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
                    "Brightness set to "
                    + (value * 100 / 255)
                    + "%");
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
                    "Boss allow write"
                    + " settings permission"
                    + " then try again");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Brightness error");
        }
    }

    // ── SCREENSHOT ────────────────────────────

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
                    "Enable Echo"
                    + " accessibility"
                    + " for screenshots Boss");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Screenshot failed");
        }
    }

    // ── VOLUME ────────────────────────────────

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
                    "Volume " + level
                    + "/10 Boss");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false, "Volume error");
        }
    }

    // ── MOBILE DATA ───────────────────────────

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

    // ── HOTSPOT ───────────────────────────────

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

    // ── SCREEN TIMEOUT ────────────────────────

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
                    + seconds + "s Boss");
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
                    + " Boss first");
            }
        } catch (Exception e) {
            e.printStackTrace();
            r.onResult(false,
                "Timeout error");
        }
    }
}
