package com.echo.assistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.telecom.TelecomManager;
import org.json.JSONObject;
import java.util.Date;

public class DeviceActions {

    public static void handle(Context ctx, String json) {
        try {
            JSONObject a = new JSONObject(json);
            String type = a.getString("type");
            String target = a.optString("target", "");
            JSONObject p = a.optJSONObject("params");
            if (p == null) p = new JSONObject();

            switch (type) {
                // ── Calls ──
                case "call":
                    makeCall(ctx, target); break;
                case "call_speaker":
                    callSpeaker(ctx); break;
                case "call_mute":
                    muteCall(ctx); break;
                case "call_hold":
                    holdCall(ctx); break;
                case "end_call":
                    endCall(ctx); break;

                // ── WhatsApp ──
                case "whatsapp_msg":
                    whatsappMsg(ctx, target, p.optString("text","")); break;
                case "whatsapp_call":
                    whatsappCall(ctx, target); break;
                case "whatsapp_video":
                    whatsappVideo(ctx, target); break;

                // ── Apps ──
                case "open_app":
                    openApp(ctx, target); break;

                // ── Settings ──
                case "toggle_wifi":
                    toggleWifi(ctx, p.optString("state","on")); break;
                case "toggle_mobile_data":
                    openMobileData(ctx); break;
                case "toggle_hotspot":
                    openHotspot(ctx); break;
                case "toggle_flashlight":
                    toggleFlash(ctx, p.optString("state","on")); break;
                case "set_brightness":
                    setBrightness(ctx, p.optInt("value",128)); break;
                case "take_screenshot":
                    takeScreenshot(ctx); break;
                case "set_screen_timeout":
                    setScreenTimeout(ctx, p.optInt("seconds",30)); break;
                case "set_volume":
                    setVolume(ctx, p.optInt("level",5)); break;

                // ── Info ──
                case "get_battery":
                    getBattery(ctx); break;
                case "get_storage":
                    getStorage(ctx); break;
                case "get_time":
                    getTime(ctx); break;

                // ── Tasks ──
                case "set_alarm":
                    setAlarm(ctx,
                        p.optString("time","07:00"),
                        p.optString("label","Echo Alarm")); break;
                case "set_reminder":
                    saveNote(ctx, "REMINDER: " + p.optString("text","")); break;
                case "save_note":
                    saveNote(ctx, p.optString("text","")); break;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── CALLS ─────────────────────────────────────────────

    static void makeCall(Context c, String number) {
        // Try to find contact by name first
        if (!number.matches(".*\\d.*")) {
            number = getNumberByName(c, number);
        }
        Intent i = new Intent(Intent.ACTION_CALL);
        i.setData(Uri.parse("tel:" + number));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    static String getNumberByName(Context c, String name) {
        android.database.Cursor cursor = c.getContentResolver().query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER},
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            new String[]{"%" + name + "%"}, null);
        if (cursor != null && cursor.moveToFirst()) {
            String num = cursor.getString(0);
            cursor.close();
            return num;
        }
        return name;
    }

    static void callSpeaker(Context c) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setSpeakerphoneOn(!am.isSpeakerphoneOn());
    }

    static void muteCall(Context c) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setMicrophoneMute(!am.isMicrophoneMute());
    }

    static void holdCall(Context c) {
        // Requires telecom permission — send keycode
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0);
    }

    static void endCall(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TelecomManager tm = (TelecomManager)
                c.getSystemService(Context.TELECOM_SERVICE);
            if (tm != null) tm.endCall();
        }
    }

    // ── WHATSAPP ───────────────────────────────────────────

    static void whatsappMsg(Context c, String number, String text) {
        // If name not number find it
        if (!number.matches(".*\\d.*")) {
            number = getNumberByName(c, number);
        }
        // Remove spaces and dashes
        number = number.replaceAll("[^0-9+]", "");
        String url = "https://wa.me/" + number + "?text=" + Uri.encode(text);
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setPackage("com.whatsapp");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
            // After 2 seconds click send via accessibility
            new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> {
                    Intent acc = new Intent("com.echo.ACCESSIBILITY");
                    acc.putExtra("action", "whatsapp_send");
                    acc.putExtra("text", text);
                    c.sendBroadcast(acc);
                }, 2500);
        } catch (Exception e) {
            // WhatsApp not installed
            Intent fallback = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + number));
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(fallback);
        }
    }

    static void whatsappCall(Context c, String number) {
        if (!number.matches(".*\\d.*")) {
            number = getNumberByName(c, number);
        }
        number = number.replaceAll("[^0-9+]", "");
        Intent i = new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/" + number));
        i.setPackage("com.whatsapp");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    static void whatsappVideo(Context c, String number) {
        if (!number.matches(".*\\d.*")) {
            number = getNumberByName(c, number);
        }
        number = number.replaceAll("[^0-9+]", "");
        Intent i = new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/" + number));
        i.setPackage("com.whatsapp");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    // ── OPEN APP ───────────────────────────────────────────

    static void openApp(Context c, String name) {
        String pkg = mapApp(name);
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } else {
            // Try opening by searching all installed apps
            Intent search = new Intent(Intent.ACTION_MAIN);
            search.addCategory(Intent.CATEGORY_LAUNCHER);
            search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            java.util.List<android.content.pm.ResolveInfo> apps =
                c.getPackageManager().queryIntentActivities(search, 0);
            for (android.content.pm.ResolveInfo app : apps) {
                if (app.loadLabel(c.getPackageManager()).toString()
                    .toLowerCase().contains(name.toLowerCase())) {
                    Intent launch = new Intent(Intent.ACTION_MAIN);
                    launch.addCategory(Intent.CATEGORY_LAUNCHER);
                    launch.setPackage(app.activityInfo.packageName);
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(launch);
                    return;
                }
            }
        }
    }

    static String mapApp(String n) {
        switch (n.toLowerCase().replace(" ","")) {
            case "instagram":    return "com.instagram.android";
            case "whatsapp":     return "com.whatsapp";
            case "youtube":      return "com.google.android.youtube";
            case "camera":       return "com.android.camera2";
            case "settings":     return "com.android.settings";
            case "calculator":   return "com.android.calculator2";
            case "chrome":       return "com.android.chrome";
            case "maps":         return "com.google.android.apps.maps";
            case "spotify":      return "com.spotify.music";
            case "netflix":      return "com.netflix.mediaclient";
            case "telegram":     return "org.telegram.messenger";
            case "snapchat":     return "com.snapchat.android";
            case "facebook":     return "com.facebook.katana";
            case "twitter":      return "com.twitter.android";
            case "gallery":      return "com.google.android.gallery3d";
            case "contacts":     return "com.android.contacts";
            case "messages":     return "com.android.mms";
            case "clock":        return "com.android.deskclock";
            case "calendar":     return "com.android.calendar";
            case "files":        return "com.android.documentsui";
            case "playstore":    return "com.android.vending";
            case "gmail":        return "com.google.android.gm";
            case "drive":        return "com.google.android.apps.docs";
            case "photos":       return "com.google.android.apps.photos";
            case "phone":        return "com.android.dialer";
            default:             return n;
        }
    }

    // ── SYSTEM SETTINGS ────────────────────────────────────

    static void toggleWifi(Context c, String state) {
        try {
            WifiManager wm = (WifiManager)
                c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) wm.setWifiEnabled(state.equals("on"));
        } catch (Exception e) {
            Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        }
    }

    static void openMobileData(Context c) {
        Intent i = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    static void openHotspot(Context c) {
        try {
            Intent i = new Intent();
            i.setClassName("com.android.settings",
                "com.android.settings.TetherSettings");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        }
    }

    static void toggleFlash(Context c, String state) {
        try {
            CameraManager cm = (CameraManager)
                c.getSystemService(Context.CAMERA_SERVICE);
            cm.setTorchMode(cm.getCameraIdList()[0], state.equals("on"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void setBrightness(Context c, int value) {
        try {
            Settings.System.putInt(c.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                Math.min(255, Math.max(0, value)));
        } catch (Exception e) {
            Intent i = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        }
    }

    static void setScreenTimeout(Context c, int seconds) {
        try {
            Settings.System.putInt(c.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, seconds * 1000);
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void setVolume(Context c, int level) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int vol = (int)((level / 10.0) * max);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, vol,
                AudioManager.FLAG_SHOW_UI);
        }
    }

    static void takeScreenshot(Context c) {
        Intent i = new Intent("com.echo.ACCESSIBILITY");
        i.putExtra("action", "screenshot");
        c.sendBroadcast(i);
    }

    // ── INFO ───────────────────────────────────────────────

    static void getBattery(Context c) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent bi = c.registerReceiver(null, ifilter);
        int level = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = bi.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int pct = (int)((level / (float)scale) * 100);
        // This gets picked up by the AI system prompt context
        android.util.Log.d("ECHO_INFO", "Battery: " + pct + "%");
    }

    static void getStorage(Context c) {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long free = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        long freeMB = free / (1024 * 1024);
        android.util.Log.d("ECHO_INFO", "Free storage: " + freeMB + "MB");
    }

    static void getTime(Context c) {
        java.text.SimpleDateFormat sdf =
            new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy hh:mm a",
                java.util.Locale.getDefault());
        android.util.Log.d("ECHO_INFO", "Time: " + sdf.format(new Date()));
    }

    // ── ALARM AND NOTES ────────────────────────────────────

    static void setAlarm(Context c, String time, String label) {
        try {
            String[] parts = time.split(":");
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(parts[0]));
            i.putExtra(AlarmClock.EXTRA_MINUTES,
                parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, label);
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) { e.printStackTrace(); }
    }

    static void saveNote(Context c, String text) {
        SharedPreferences p = c.getSharedPreferences("echo_notes", 0);
        String ts = new java.text.SimpleDateFormat("dd/MM HH:mm",
            java.util.Locale.getDefault()).format(new Date());
        p.edit().putString("notes",
            p.getString("notes","") + "\n[" + ts + "] " + text).apply();
    }
}
