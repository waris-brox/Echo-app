package com.echo.assistant;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import org.json.JSONObject;
import java.io.*;
import java.net.*;

public class CoquiTTS {

    private static final String TTS_URL = "http://127.0.0.1:5002/tts";
    private static MediaPlayer player;
    private static boolean speaking = false;

    public interface Callback {
        void onDone();
        void onError(String error);
    }

    public static void speak(Context ctx, String text, Callback cb) {
        if (speaking) stop();
        new AsyncTask<Void, Void, File>() {
            protected File doInBackground(Void... v) {
                try {
                    JSONObject body = new JSONObject();
                    body.put("text", text);

                    HttpURLConnection c = (HttpURLConnection)
                        new URL(TTS_URL).openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Content-Type", "application/json");
                    c.setDoOutput(true);
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(30000);
                    c.getOutputStream().write(body.toString().getBytes("UTF-8"));

                    File tmp = File.createTempFile("echo_tts", ".wav",
                        ctx.getCacheDir());
                    InputStream in = c.getInputStream();
                    FileOutputStream out = new FileOutputStream(tmp);
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    out.close(); in.close();
                    return tmp;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            protected void onPostExecute(File file) {
                if (file == null) {
                    if (cb != null) cb.onError("TTS failed");
                    return;
                }
                try {
                    speaking = true;
                    player = new MediaPlayer();
                    player.setDataSource(file.getAbsolutePath());
                    player.prepare();
                    player.start();
                    player.setOnCompletionListener(mp -> {
                        speaking = false;
                        file.delete();
                        if (cb != null) cb.onDone();
                    });
                } catch (Exception e) {
                    speaking = false;
                    if (cb != null) cb.onError(e.getMessage());
                }
            }
        }.execute();
    }

    public static void stop() {
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); player.release(); }
            catch (Exception ignored) {}
            player = null;
        }
        speaking = false;
    }

    public static boolean isSpeaking() { return speaking; }
}
