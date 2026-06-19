package com.echo.assistant;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {

    private static final String USER = "echo_user";
    private static final String PREFS = "echo_prefs";

    // Save login session
    public static void saveUser(Context c, String name, String email, String pass) {
        c.getSharedPreferences(USER, 0).edit()
            .putString("name", name)
            .putString("email", email)
            .putString("password", pass)
            .putBoolean("logged_in", true)
            .apply();
    }

    // Check if logged in
    public static boolean isLoggedIn(Context c) {
        return c.getSharedPreferences(USER, 0).getBoolean("logged_in", false);
    }

    // Get name
    public static String getName(Context c) {
        return c.getSharedPreferences(USER, 0).getString("name", "Boss");
    }

    // Get email
    public static String getEmail(Context c) {
        return c.getSharedPreferences(USER, 0).getString("email", "");
    }

    // Logout
    public static void logout(Context c) {
        c.getSharedPreferences(USER, 0).edit()
            .putBoolean("logged_in", false).apply();
    }

    // Save API key
    public static void saveApiKey(Context c, String key) {
        c.getSharedPreferences(PREFS, 0).edit()
            .putString("groq_api_key", key).apply();
        GroqAPI.API_KEY = key;
    }

    // Get API key
    public static String getApiKey(Context c) {
        return c.getSharedPreferences(PREFS, 0).getString("groq_api_key", "");
    }

    // Voice enabled
    public static boolean isVoiceEnabled(Context c) {
        return c.getSharedPreferences(PREFS, 0).getBoolean("voice_enabled", true);
    }

    public static void setVoiceEnabled(Context c, boolean val) {
        c.getSharedPreferences(PREFS, 0).edit()
            .putBoolean("voice_enabled", val).apply();
    }

    // Battery info
    public static String getBatteryInfo(Context c) {
        android.os.BatteryManager bm = (android.os.BatteryManager)
            c.getSystemService(Context.BATTERY_SERVICE);
        int level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return level + "%";
    }

    // Storage info
    public static String getStorageInfo() {
        android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getExternalStorageDirectory().getPath());
        long free = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        long total = stat.getBlockCountLong() * stat.getBlockSizeLong();
        long freeMB = free / (1024 * 1024);
        long totalMB = total / (1024 * 1024);
        if (freeMB > 1024) {
            return (freeMB/1024) + "GB free of " + (totalMB/1024) + "GB";
        }
        return freeMB + "MB free of " + (totalMB/1024) + "GB";
    }
}
