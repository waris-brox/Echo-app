package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Locale;
import android.os.Bundle;

public class VoiceActivity extends Activity {

    private SpeechRecognizer sr;
    private TextToSpeech tts;
    private Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Transparent — no UI, just listens and acts
        // Load API key
        SharedPreferences prefs = getSharedPreferences("echo_prefs", MODE_PRIVATE);
        GroqAPI.API_KEY = prefs.getString("groq_api_key", "");

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
            }
            startListening();
        });
    }

    void startListening() {
        try {
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening Boss...");

            sr.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) {}
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float r) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() {}
                public void onPartialResults(Bundle b) {}
                public void onEvent(int t, Bundle b) {}

                public void onResults(Bundle b) {
                    ArrayList<String> r = b.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                    if (r != null && !r.isEmpty()) {
                        String query = r.get(0);
                        askEcho(query);
                    } else {
                        finish();
                    }
                }

                public void onError(int e) {
                    finish();
                }
            });

            sr.startListening(intent);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    void askEcho(String query) {
        GroqAPI.ask(new org.json.JSONArray(), query, new GroqAPI.Callback() {
            public void onSuccess(String reply, String action) {
                if (tts != null && !reply.isEmpty()) {
                    tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "echo_reply");
                }
                if (action != null && !action.isEmpty()) {
                    DeviceActions.handle(VoiceActivity.this, action,
                        (success, msg) -> {});
                }
                h.postDelayed(() -> finish(), 3000);
            }

            public void onError(String error) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (sr != null) sr.destroy(); } catch (Exception e) {}
        try { if (tts != null) tts.shutdown(); } catch (Exception e) {}
    }
}
