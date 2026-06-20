package com.echo.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;

public class AppPrefs {

    private static final String USER =
        "echo_user";
    private static final String PREFS =
        "echo_prefs";

    public static void saveUser(Context c,
        String name, String email,
        String pass) {
        c.getSharedPreferences(USER, 0)
            .edit()
            .putString("name", name)
            .putString("email", email)
            .putString("password", pass)
            .putBoolean("logged_in", true)
            .apply();
    }

    public static boolean isLoggedIn(
        Context c) {
        return c.getSharedPreferences(USER, 0)
            .getBoolean("logged_in", false);
    }

    public static String getName(Context c) {
        return c.getSharedPreferences(USER, 0)
            .getString("name", "Boss");
    }

    public static String getEmail(Context c) {
        return c.getSharedPreferences(USER, 0)
            .getString("email", "");
    }

    public static void logout(Context c) {
        c.getSharedPreferences(USER, 0)
            .edit()
            .putBoolean("logged_in", false)
            .apply();
    }

    public static void saveApiKey(Context c,
        String key) {
        c.getSharedPreferences(PREFS, 0)
            .edit()
            .putString("groq_api_key", key)
            .apply();
        GroqAPI.API_KEY = key;
    }

    public static String getApiKey(Context c) {
        return c.getSharedPreferences(PREFS, 0)
            .getString("groq_api_key", "");
    }

    public static boolean isVoiceEnabled(
        Context c) {
        return c.getSharedPreferences(PREFS, 0)
            .getBoolean("voice_enabled", true);
    }

    public static void setVoiceEnabled(
        Context c, boolean val) {
        c.getSharedPreferences(PREFS, 0)
            .edit()
            .putBoolean("voice_enabled", val)
            .apply();
    }

    public static String getStorageInfo() {
        try {
            StatFs stat = new StatFs(
                Environment
                .getExternalStorageDirectory()
                .getPath());
            long free =
                stat.getAvailableBlocksLong()
                * stat.getBlockSizeLong();
            long total =
                stat.getBlockCountLong()
                * stat.getBlockSizeLong();
            long freeGB = free
                / (1024 * 1024 * 1024);
            long totalGB = total
                / (1024 * 1024 * 1024);
            if (freeGB > 0) {
                return "Storage: "
                    + freeGB + "GB free of "
                    + totalGB + "GB";
            } else {
                long freeMB = free
                    / (1024 * 1024);
                return "Storage: "
                    + freeMB + "MB free of "
                    + totalGB + "GB";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Storage info unavailable";
        }
    }
}
