package com.echo.assistant;

import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GroqAPI {

    public static String API_KEY = "";
    private static final String ENDPOINT =
        "https://api.groq.com/openai/v1"
        + "/chat/completions";
    private static final String MODEL =
        "llama-3.3-70b-versatile";

    public interface Callback {
        void onSuccess(String reply,
            String action);
        void onError(String error);
    }

    public static void ask(JSONArray history,
        String message, Callback cb) {
        new AsyncTask<Void, Void, String[]>() {
            protected String[] doInBackground(
                Void... v) {
                try {
                    JSONArray msgs =
                        new JSONArray();
                    JSONObject sys =
                        new JSONObject();
                    sys.put("role", "system");
                    sys.put("content",
                        getPrompt());
                    msgs.put(sys);
                    for (int i = 0;
                        i < history.length();
                        i++)
                        msgs.put(
                        history.getJSONObject(i));
                    JSONObject u =
                        new JSONObject();
                    u.put("role", "user");
                    u.put("content", message);
                    msgs.put(u);

                    JSONObject body =
                        new JSONObject();
                    body.put("model", MODEL);
                    body.put("messages", msgs);
                    body.put("max_tokens", 600);
                    body.put("temperature",
                        0.8);

                    HttpURLConnection c =
                        (HttpURLConnection)
                        new URL(ENDPOINT)
                        .openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty(
                        "Authorization",
                        "Bearer " + API_KEY);
                    c.setRequestProperty(
                        "Content-Type",
                        "application/json");
                    c.setDoOutput(true);
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.getOutputStream().write(
                        body.toString()
                        .getBytes("UTF-8"));

                    int code =
                        c.getResponseCode();
                    if (code != 200) {
                        BufferedReader er =
                            new BufferedReader(
                            new InputStreamReader(
                            c.getErrorStream(),
                            "UTF-8"));
                        StringBuilder eb =
                            new StringBuilder();
                        String line;
                        while ((line =
                            er.readLine())
                            != null)
                            eb.append(line);
                        return new String[]{
                            "err",
                            "HTTP " + code
                            + ": " + eb};
                    }

                    BufferedReader br =
                        new BufferedReader(
                        new InputStreamReader(
                        c.getInputStream(),
                        "UTF-8"));
                    StringBuilder sb =
                        new StringBuilder();
                    String line;
                    while ((line =
                        br.readLine()) != null)
                        sb.append(line);

                    String content =
                        new JSONObject(
                        sb.toString())
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                    return new String[]{
                        "ok", content};

                } catch (Exception e) {
                    return new String[]{
                        "err", e.getMessage()};
                }
            }

            protected void onPostExecute(
                String[] r) {
                if (r[0].equals("ok")) {
                    String full = r[1];
                    String action = "";
                    String reply = full;
                    try {
                        if (full.contains(
                            "ACTION:")) {
                            int idx =
                                full.indexOf(
                                "ACTION:");
                            String before =
                                full.substring(
                                0, idx).trim();
                            String after =
                                full.substring(
                                idx + 7).trim();
                            int start =
                                after.indexOf(
                                "{");
                            if (start >= 0) {
                                int depth = 0;
                                int end = -1;
                                for (int i =
                                    start;
                                    i < after
                                    .length();
                                    i++) {
                                    char ch =
                                        after
                                        .charAt(i);
                                    if (ch == '{')
                                        depth++;
                                    else if (
                                        ch == '}'){
                                        depth--;
                                        if (depth
                                            == 0){
                                            end = i;
                                            break;
                                        }
                                    }
                                }
                                if (end >= 0) {
                                    action =
                                        after
                                        .substring(
                                        start,
                                        end + 1);
                                    new JSONObject(
                                        action);
                                    reply =
                                        before
                                        .isEmpty()
                                        ? "On it Boss!"
                                        : before;
                                } else {
                                    reply =
                                        before
                                        .isEmpty()
                                        ? full
                                        : before;
                                }
                            }
                        }
                    } catch (Exception e) {
                        reply = full.contains(
                            "ACTION:")
                            ? full.substring(
                            0, full.indexOf(
                            "ACTION:")).trim()
                            : full;
                        action = "";
                    }
                    cb.onSuccess(reply, action);
                } else {
                    cb.onError(
                        "Boss error: " + r[1]);
                }
            }
        }.execute();
    }

    static String getPrompt() {
        return "You are Echo, a powerful AI"
            + " assistant on the user's"
            + " Android phone.\n\n"
            + "PERSONALITY:\n"
            + "Always call user Boss.\n"
            + "Respectful, funny, sarcastic,"
            + " strict when needed.\n"
            + "Speak Hinglish when Boss does.\n"
            + "Never say I dont know.\n"
            + "Short punchy replies.\n\n"
            + "CRITICAL RULES:\n"
            + "When Boss asks to do something"
            + " on phone, put ACTION at end.\n"
            + "Do not claim you did it.\n"
            + "The app executes it.\n\n"
            + "ACTION FORMAT:\n"
            + "ACTION:{\"type\":\"TYPE\","
            + "\"target\":\"TARGET\","
            + "\"params\":{\"key\":\"val\"}}\n\n"
            + "ACTION TYPES:\n"
            + "call - phone call\n"
            + "  target=contact name or number\n"
            + "whatsapp_msg - send WA message\n"
            + "  target=name or number\n"
            + "  params={\"text\":\"msg\"}\n"
            + "whatsapp_call - WA voice call\n"
            + "  target=name or number\n"
            + "whatsapp_video - WA video call\n"
            + "  target=name or number\n"
            + "open_app - open any app\n"
            + "  target=app name\n"
            + "youtube_search - search YouTube\n"
            + "  params={\"query\":\"search\"}\n"
            + "toggle_wifi\n"
            + "  params={\"state\":\"on/off\"}\n"
            + "toggle_flashlight\n"
            + "  params={\"state\":\"on/off\"}\n"
            + "set_brightness\n"
            + "  params={\"value\":128}\n"
            + "take_screenshot\n"
            + "set_alarm\n"
            + "  params={\"time\":\"07:00\","
            + "\"label\":\"label\"}\n"
            + "set_reminder\n"
            + "  params={\"text\":\"text\","
            + "\"minutes\":30}\n"
            + "save_note\n"
            + "  params={\"text\":\"note\"}\n"
            + "set_volume\n"
            + "  params={\"level\":5}\n"
            + "toggle_mobile_data\n"
            + "toggle_hotspot\n"
            + "set_screen_timeout\n"
            + "  params={\"seconds\":30}\n\n"
            + "EXAMPLES:\n"
            + "Call Rahul:\n"
            + "Calling Rahul Boss!"
            + " ACTION:{\"type\":\"call\","
            + "\"target\":\"Rahul\","
            + "\"params\":{}}\n\n"
            + "Open Instagram:\n"
            + "Opening Instagram Boss!"
            + " ACTION:{\"type\":\"open_app\","
            + "\"target\":\"instagram\","
            + "\"params\":{}}\n\n"
            + "Search cat videos on YouTube:\n"
            + "Searching YouTube Boss!"
            + " ACTION:{\"type\":"
            + "\"youtube_search\","
            + "\"target\":\"\","
            + "\"params\":{\"query\":"
            + "\"cat videos\"}}\n\n"
            + "Send hi to John on WhatsApp:\n"
            + "Sending hi to John Boss!"
            + " ACTION:{\"type\":"
            + "\"whatsapp_msg\","
            + "\"target\":\"John\","
            + "\"params\":{\"text\":\"hi\"}}\n\n"
            + "Set alarm 7 AM:\n"
            + "Alarm set Boss!"
            + " ACTION:{\"type\":\"set_alarm\","
            + "\"target\":\"\","
            + "\"params\":{\"time\":\"07:00\","
            + "\"label\":\"Echo Alarm\"}}\n\n"
            + "Save note buy milk:\n"
            + "Note saved Boss!"
            + " ACTION:{\"type\":\"save_note\","
            + "\"target\":\"\","
            + "\"params\":{\"text\":"
            + "\"buy milk\"}}\n\n"
            + "Always be Echo."
            + " Never break character.";
    }
}
