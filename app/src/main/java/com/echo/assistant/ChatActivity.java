package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends Activity {

    private LinearLayout layoutMessages;
    private ScrollView scrollChat;
    private EditText etInput;
    private JSONArray chatHistory;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean voiceEnabled = true;

    protected void onCreate(Bundle s) {
        super.onCreate(s);

        try {
            setContentView(R.layout.activity_chat);

            layoutMessages = findViewById(R.id.layout_messages);
            scrollChat     = findViewById(R.id.scroll_chat);
            etInput        = findViewById(R.id.et_input);

            chatHistory  = ChatHistory.load(this);
            GroqAPI.API_KEY = AppPrefs.getApiKey(this);
            voiceEnabled = AppPrefs.isVoiceEnabled(this);

            initTTS();

            if (findViewById(R.id.btn_back) != null)
                findViewById(R.id.btn_back)
                    .setOnClickListener(v -> finish());

            if (findViewById(R.id.btn_menu) != null)
                findViewById(R.id.btn_menu).setOnClickListener(v ->
                    startActivity(new Intent(this,
                        SettingsActivity.class)));

            if (findViewById(R.id.btn_send) != null)
                findViewById(R.id.btn_send)
                    .setOnClickListener(v -> sendMessage());

            if (findViewById(R.id.btn_mic) != null)
                findViewById(R.id.btn_mic)
                    .setOnClickListener(v -> startVoice());

            if (etInput != null) {
                etInput.setOnEditorActionListener((v, a, e) -> {
                    sendMessage();
                    return true;
                });
            }

            String firstMsg = getIntent() != null
                ? getIntent().getStringExtra("message") : null;
            if (firstMsg != null && !firstMsg.isEmpty()) {
                new Handler(Looper.getMainLooper())
                    .postDelayed(() -> sendToEcho(firstMsg), 400);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                "Chat error: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    void initTTS() {
        try {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int r = tts.setLanguage(new Locale("en", "IN"));
                    if (r == TextToSpeech.LANG_MISSING_DATA
                        || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.ENGLISH);
                    }
                    tts.setSpeechRate(0.95f);
                    tts.setPitch(1.1f);
                    ttsReady = true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void speakText(String text) {
        try {
            if (!voiceEnabled) return;
            if (ttsReady && tts != null) {
                tts.speak(text,
                    TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stopSpeaking() {
        try {
            if (tts != null && tts.isSpeaking()) tts.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendMessage() {
        try {
            if (etInput == null) return;
            String text = etInput.getText().toString().trim();
            if (text.isEmpty()) return;
            etInput.setText("");
            stopSpeaking();
            sendToEcho(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendToEcho(String text) {
        try {
            if (GroqAPI.API_KEY == null
                || GroqAPI.API_KEY.isEmpty()) {
                addEchoBubble("Boss please set your Groq API key"
                    + " in Settings first!");
                speakText("Boss please set your Groq API key"
                    + " in Settings first!");
                return;
            }

            String enriched = addContext(text);
            addUserBubble(text);
            View typing = addTyping();

            ChatHistory.add(this, "user", text);
            chatHistory = ChatHistory.load(this);

            GroqAPI.ask(chatHistory, enriched,
                new GroqAPI.Callback() {
                    public void onSuccess(String reply,
                        String action) {
                        try {
                            if (layoutMessages != null)
                                layoutMessages.removeView(typing);
                            addEchoBubble(reply);
                            ChatHistory.add(ChatActivity.this,
                                "assistant", reply);
                            speakText(reply);
                            if (action != null
                                && !action.isEmpty()) {
                                DeviceActions.handle(
                                    ChatActivity.this, action);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    public void onError(String error) {
                        try {
                            if (layoutMessages != null)
                                layoutMessages.removeView(typing);
                            addEchoBubble(error);
                            speakText(error);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String addContext(String text) {
        try {
            String lower = text.toLowerCase();
            if (lower.contains("battery")) {
                try {
                    IntentFilter f = new IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED);
                    Intent bi = registerReceiver(null, f);
                    int lvl = bi.getIntExtra(
                        BatteryManager.EXTRA_LEVEL, -1);
                    int scl = bi.getIntExtra(
                        BatteryManager.EXTRA_SCALE, -1);
                    int pct = (int)((lvl / (float)scl) * 100);
                    return text + " [Battery: " + pct + "%]";
                } catch (Exception e) { return text; }
            }
            if (lower.contains("time")
                || lower.contains("date")
                || lower.contains("day")) {
                String dt = new SimpleDateFormat(
                    "EEEE dd MMMM yyyy hh:mm a",
                    Locale.getDefault()).format(new Date());
                return text + " [Now: " + dt + "]";
            }
            if (lower.contains("storage")
                || lower.contains("space")) {
                return text + " ["
                    + AppPrefs.getStorageInfo() + "]";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }

    void addUserBubble(String text) {
        try {
            if (layoutMessages == null) return;
            View v = getLayoutInflater().inflate(
                R.layout.item_bubble_user,
                layoutMessages, false);
            TextView msg = v.findViewById(R.id.tv_message);
            TextView time = v.findViewById(R.id.tv_time);
            if (msg != null) msg.setText(text);
            if (time != null)
                time.setText("You · " + getTime());
            layoutMessages.addView(v);
            scrollBottom();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void addEchoBubble(String text) {
        try {
            if (layoutMessages == null) return;
            View v = getLayoutInflater().inflate(
                R.layout.item_bubble_echo,
                layoutMessages, false);
            TextView msg = v.findViewById(R.id.tv_message);
            TextView time = v.findViewById(R.id.tv_time);
            if (msg != null) msg.setText(text);
            if (time != null)
                time.setText("Echo · " + getTime());
            layoutMessages.addView(v);
            scrollBottom();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    View addTyping() {
        try {
            if (layoutMessages == null) return new View(this);
            TextView tv = new TextView(this);
            tv.setText("Echo is thinking...");
            tv.setTextColor(
                getResources().getColor(R.color.text_dim));
            tv.setTextSize(13);
            tv.setPadding(32, 16, 32, 8);
            layoutMessages.addView(tv);
            scrollBottom();
            return tv;
        } catch (Exception e) {
            e.printStackTrace();
            return new View(this);
        }
    }

    void scrollBottom() {
        try {
            if (scrollChat != null)
                scrollChat.post(() ->
                    scrollChat.fullScroll(
                        ScrollView.FOCUS_DOWN));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getTime() {
        return new SimpleDateFormat("hh:mm a",
            Locale.getDefault()).format(new Date());
    }

    void startVoice() {
        try {
            stopSpeaking();
            Intent i = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
            i.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Listening Boss...");
            startActivityForResult(i, 100);
        } catch (Exception e) {
            Toast.makeText(this,
                "Voice not available Boss",
                Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int req, int res,
        Intent data) {
        super.onActivityResult(req, res, data);
        try {
            if (req == 100 && res == RESULT_OK
                && data != null) {
                ArrayList<String> r =
                    data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                if (r != null && !r.isEmpty()) {
                    sendToEcho(r.get(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onResume() {
        super.onResume();
        try {
            GroqAPI.API_KEY = AppPrefs.getApiKey(this);
            voiceEnabled = AppPrefs.isVoiceEnabled(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
