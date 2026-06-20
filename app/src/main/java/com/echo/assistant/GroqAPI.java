package com.echo.assistant;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;

public class GroqAPI {
    public static String API_KEY = "";
    private static final String URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    public interface Callback {
        void onSuccess(String reply, String action);
        void onError(String error);
    }

    public static void ask(JSONArray history, String message, Callback cb) {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            cb.onError("Boss, Groq API key set nahi hai. Settings me jaake daal do.");
            return;
        }

        new AsyncTask<Void, Void, String[]>() {
            protected String[] doInBackground(Void... v) {
                try {
                    JSONArray msgs = new JSONArray();
                    JSONObject sys = new JSONObject();
                    sys.put("role", "system");
                    sys.put("content", getPrompt());
                    msgs.put(sys);
                    for (int i = 0; i < history.length(); i++) msgs.put(history.getJSONObject(i));
                    JSONObject u = new JSONObject();
                    u.put("role", "user");
                    u.put("content", message);
                    msgs.put(u);

                    JSONObject body = new JSONObject();
                    body.put("model", MODEL);
                    body.put("messages", msgs);
                    body.put("max_tokens", 512);
                    body.put("temperature", 0.8);

                    HttpURLConnection c = (HttpURLConnection)
                        new java.net.URL(URL).openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Authorization", "Bearer " + API_KEY.trim());
                    c.setRequestProperty("Content-Type", "application/json");
                    c.setDoOutput(true);
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.getOutputStream().write(body.toString().getBytes("UTF-8"));

                    int status = c.getResponseCode();
                    InputStream stream = (status >= 200 && status < 300)
                        ? c.getInputStream() : c.getErrorStream();

                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(stream, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);

                    if (status < 200 || status >= 300) {
                        return new String[]{"err", "HTTP " + status + ": " + sb.toString()};
                    }

                    String content = new JSONObject(sb.toString())
                        .getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content");
                    return new String[]{"ok", content};
                } catch (Exception e) {
                    return new String[]{"err", e.getMessage()};
                }
            }

            protected void onPostExecute(String[] r) {
                if (r[0].equals("ok")) {
                    String full = r[1], action = "", reply = full;
                    if (full.contains("ACTION:")) {
                        int idx = full.indexOf("ACTION:");
                        try {
                            String raw = full.substring(idx + 7).trim();
                            action = raw.substring(0, raw.indexOf("}") + 1);
                            reply = full.substring(0, idx).trim();
                        } catch (Exception ignored) {}
                    }
                    cb.onSuccess(reply, action);
                } else {
                    cb.onError("Boss, kuch toh gadbad hai: " + r[1]);
                }
            }
        }.execute();
    }

    static String getPrompt() {
        return "You are Echo, a smart and powerful AI assistant living inside the user's Android phone.\n"
            + "Always call the user Boss.\n"
            + "You are respectful, funny, witty, sarcastic, and strict when needed.\n"
            + "Speak Hinglish naturally when Boss does.\n"
            + "Never say I don't know. Be confident always.\n"
            + "Keep replies short and punchy unless Boss asks for detail.\n"
            + "When Boss asks to control the phone, reply naturally AND end with:\n"
            + "ACTION:{\"type\":\"...\",\"target\":\"...\",\"params\":{}}\n"
            + "Types: call, whatsapp_msg, whatsapp_call, open_app, toggle_wifi,\n"
            + "toggle_hotspot, toggle_flashlight, set_brightness,\n"
            + "take_screenshot, set_alarm, set_reminder, save_note\n"
            + "Do NOT claim an action succeeded unless you are simply issuing the command.\n"
            + "For WiFi, hotspot, and screenshot say you are opening/attempting it,\n"
            + "not that it is guaranteed done, since you cannot verify the result.\n"
            + "Never break character. You are Echo.";
    }
}
