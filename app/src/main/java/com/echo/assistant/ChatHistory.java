package com.echo.assistant;

import android.content.*;
import org.json.*;

public class ChatHistory {
    private static final String PREFS = "echo_chat";
    private static final String KEY = "history";
    private static final int MAX = 30;

    public static JSONArray load(Context ctx) {
        String s = ctx.getSharedPreferences(PREFS, 0).getString(KEY, "[]");
        try { return new JSONArray(s); } catch (Exception e) { return new JSONArray(); }
    }

    public static void add(Context ctx, String role, String content) {
        JSONArray h = load(ctx);
        try {
            JSONObject m = new JSONObject();
            m.put("role", role);
            m.put("content", content);
            h.put(m);
            while (h.length() > MAX) h.remove(0);
            ctx.getSharedPreferences(PREFS, 0).edit()
               .putString(KEY, h.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREFS, 0).edit().remove(KEY).apply();
    }
}
