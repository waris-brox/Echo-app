package com.echo.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatHistory {

    private static final String PREFS =
        "echo_chat";
    private static final String KEY =
        "history";
    private static final int MAX = 50;

    // Save message with timestamp
    // stored separately from role/content
    public static void add(Context ctx,
        String role, String content) {
        try {
            JSONArray h = load(ctx);
            JSONObject m = new JSONObject();
            m.put("role", role);
            m.put("content", content);
            // Store timestamp in separate
            // field that we strip before
            // sending to Groq
            m.put("ts",
                new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault())
                .format(new Date()));
            h.put(m);
            while (h.length() > MAX)
                h.remove(0);
            ctx.getSharedPreferences(
                PREFS, 0).edit()
                .putString(KEY,
                h.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load full history with timestamps
    // for display in history screen
    public static JSONArray load(Context ctx) {
        try {
            String s = ctx
                .getSharedPreferences(
                PREFS, 0)
                .getString(KEY, "[]");
            return new JSONArray(s);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    // Load clean history for Groq API
    // Only role and content no extras
    public static JSONArray loadForGroq(
        Context ctx) {
        try {
            JSONArray all = load(ctx);
            JSONArray clean = new JSONArray();
            for (int i = 0;
                i < all.length(); i++) {
                try {
                    JSONObject orig =
                        all.getJSONObject(i);
                    // Only send role
                    // and content to Groq
                    JSONObject msg =
                        new JSONObject();
                    msg.put("role",
                        orig.getString("role"));
                    msg.put("content",
                        orig.getString(
                        "content"));
                    clean.put(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return clean;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS, 0)
            .edit().remove(KEY).apply();
    }

    // Get only user messages for
    // history screen display
    public static JSONArray getUserMessages(
        Context ctx) {
        try {
            JSONArray all = load(ctx);
            JSONArray user = new JSONArray();
            for (int i = 0;
                i < all.length(); i++) {
                try {
                    JSONObject m =
                        all.getJSONObject(i);
                    if ("user".equals(
                        m.optString("role"))) {
                        user.put(m);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return user;
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
