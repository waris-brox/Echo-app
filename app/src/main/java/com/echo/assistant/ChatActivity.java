package com.echo.assistant;

import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatActivity extends Activity {

    private LinearLayout layoutMessages;
    private ScrollView scrollChat;
    private EditText etInput;
    private JSONArray chatHistory;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean voiceEnabled = true;
    private SpeechRecognizer interruptSR;

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_chat);

        layoutMessages = findViewById(R.id.layout_messages);
        scrollChat = findViewById(R.id.scroll_chat);
        etInput = findViewById(R.id.et_input);

        chatHistory = ChatHistory.load(this);
        GroqAPI.API_KEY = AppPrefs.getApiKey(this);
        voiceEnabled = AppPrefs.isVoiceEnabled(this);

        // Init TTS
        initTTS();

        // Buttons
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_menu).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_mic).setOnClickListener(v -> startVoice());

        etInput.setOnEditorActionListener((v, a, e) -> {
            sendMessage();
            return true;
        });

        // Handle first message
        String firstMsg = getIntent().getStringExtra("message");
        if (firstMsg != null && !firstMsg.isEmpty()) {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                sendToEcho(firstMsg), 300);
        }
    }

    void initTTS() {
        // Try Coqui first, fallback to system TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Try Indian English voice
                int result = tts.setLanguage(new Locale("en", "IN"));
                if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.ENGLISH);
                }
                // Set speech rate and pitch for natural sound
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.1f);
                ttsReady = true;
            }
        });
    }

    void speakText(String text) {
        if (!voiceEnabled) return;
        // Try Coqui TTS first
        CoquiTTS.speak(this, text, new CoquiTTS.Callback() {
            @Override
            public void onDone() {}
            @Override
            public void onError(String error) {
                // Fallback to system TTS if Coqui fails
                if (ttsReady && tts != null) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });
    }

    void stopSpeaking() {
        CoquiTTS.stop();
        if (tts != null && tts.isSpeaking()) tts.stop();
    }

    void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        etInput.setText("");
        // Stop Echo if speaking
        stopSpeaking();
        sendToEcho(text);
    }

    void sendToEcho(String text) {
        // Add device context to message if asking for info
        String enriched = enrichWithContext(text);

        addUserBubble(text);
        View typing = addTyping();

        ChatHistory.add(this, "user", text);
        chatHistory = ChatHistory.load(this);

        GroqAPI.ask(chatHistory, enriched, new GroqAPI.Callback() {
            public void onSuccess(String reply, String action) {
                layoutMessages.removeView(typing);
                addEchoBubble(reply);
                ChatHistory.add(ChatActivity.this, "assistant", reply);
                speakText(reply);
                if (action != null && !action.isEmpty()) {
                    DeviceActions.handle(ChatActivity.this, action);
                }
            }
            public void onError(String error) {
                layoutMessages.removeView(typing);
                addEchoBubble(error);
                speakText(error);
            }
        });
    }

    // Add real device context to queries
    String enrichWithContext(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("battery")) {
            IntentFilter ifilter = new IntentFilter(
                android.content.Intent.ACTION_BATTERY_CHANGED);
            Intent bi = registerReceiver(null, ifilter);
            int level = bi.getIntExtra(
                android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = bi.getIntExtra(
                android.os.BatteryManager.EXTRA_SCALE, -1);
            int pct = (int)((level / (float)scale) * 100);
            return text + " [Battery is currently at " + pct + "%]";
        }
        if (lower.contains("time") || lower.contains("date")) {
            String dt = new SimpleDateFormat(
                "EEEE dd MMMM yyyy, hh:mm a",
                Locale.getDefault()).format(new Date());
            return text + " [Current date and time is " + dt + "]";
        }
        if (lower.contains("storage") || lower.contains("space")) {
            return text + " [" + AppPrefs.getStorageInfo() + "]";
        }
        return text;
    }

    void addUserBubble(String text) {
        View v = getLayoutInflater().inflate(
            R.layout.item_bubble_user, layoutMessages, false);
        ((TextView) v.findViewById(R.id.tv_message)).setText(text);
        ((TextView) v.findViewById(R.id.tv_time)).setText(
            "You · " + time());
        layoutMessages.addView(v);
        scrollBottom();
    }

    void addEchoBubble(String text) {
        View v = getLayoutInflater().inflate(
            R.layout.item_bubble_echo, layoutMessages, false);
        ((TextView) v.findViewById(R.id.tv_message)).setText(text);
        ((TextView) v.findViewById(R.id.tv_time)).setText(
            "Echo · " + time());
        layoutMessages.addView(v);
        scrollBottom();
    }

    View addTyping() {
        TextView tv = new TextView(this);
        tv.setText("Echo is thinking...");
        tv.setTextColor(getResources().getColor(R.color.text_dim));
        tv.setTextSize(13);
        tv.setPadding(32, 16, 32, 8);
        layoutMessages.addView(tv);
        scrollBottom();
        return tv;
    }

    void scrollBottom() {
        scrollChat.post(() ->
            scrollChat.fullScroll(ScrollView.FOCUS_DOWN));
    }

    String time() {
        return new SimpleDateFormat("hh:mm a",
            Locale.getDefault()).format(new Date());
    }

    void startVoice() {
        stopSpeaking();
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,
            "Listening Boss...");
        startActivityForResult(i, 100);
    }

    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 100 && res == RESULT_OK && data != null) {
            ArrayList<String> r = data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS);
            if (r != null && !r.isEmpty()) {
                sendToEcho(r.get(0));
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        CoquiTTS.stop();
        if (tts != null) { tts.stop(); tts.shutdown(); }
    }
}
