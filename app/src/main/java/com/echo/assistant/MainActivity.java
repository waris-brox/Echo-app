package com.echo.assistant;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;                                                       import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LinearLayout messagesContainer;
    private NestedScrollView scrollView;
    private EditText inputMessage;
    private View greetingLayout;
    private View typingIndicator;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private JSONArray chatHistory = new JSONArray();
    private boolean isTtsSpeaking = false;
    private String userName = "Boss";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get user name
        userName = getIntent().getStringExtra("user_name");
        if (userName == null || userName.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("echo_prefs", MODE_PRIVATE);
            userName = prefs.getString("user_name", "Boss");
        }

        // Load API key
        SharedPreferences prefs = getSharedPreferences("echo_prefs", MODE_PRIVATE);
        GroqAPI.API_KEY = prefs.getString("groq_api_key", "");

        // Views
        messagesContainer = findViewById(R.id.messages_container);
        scrollView = findViewById(R.id.scroll_messages);
        inputMessage = findViewById(R.id.input_message);
        greetingLayout = findViewById(R.id.greeting_layout);
        typingIndicator = findViewById(R.id.typing_indicator);

        // Update greeting
        TextView greetingText = findViewById(R.id.greeting_text);
        greetingText.setText("Hello, " + userName);

        // Send button
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());

        // Voice button
        findViewById(R.id.btn_voice).setOnClickListener(v -> startVoiceInput());

        // Settings button
        findViewById(R.id.btn_settings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        // TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi", "IN"));
            }
        });

        // Request mic permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        // Handle wake word trigger
        if (getIntent().getBooleanExtra("wake_word", false)) {
            startVoiceInput();
        }
    }

    void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        inputMessage.setText("");
        addMessage(text, true);
        showTyping(true);

        try {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", text);
            chatHistory.put(userMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        GroqAPI.ask(chatHistory, text, new GroqAPI.Callback() {
            public void onSuccess(String reply, String action) {
                showTyping(false);
                addMessage(reply, false);
                speak(reply);

                try {
                    JSONObject assistantMsg = new JSONObject();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", reply);
                    chatHistory.put(assistantMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (action != null && !action.isEmpty()) {
                    DeviceActions.handle(ChatActivity.this, action,
                        (success, msg) -> {
                            if (!success) {
                                runOnUiThread(() -> addMessage("⚠️ " + msg, false));
                            }
                        });
                }
            }

            public void onError(String error) {
                showTyping(false);
                addMessage("⚠️ " + error, false);
            }
        });
    }

    void addMessage(String text, boolean isUser) {
        runOnUiThread(() -> {
            greetingLayout.setVisibility(View.GONE);

            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextSize(15);
            tv.setTextColor(Color.parseColor("#2D2A26"));
            tv.setPadding(dpToPx(18), dpToPx(12), dpToPx(18), dpToPx(12));
            tv.setLineSpacing(0, 1.4f);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dpToPx(10));
            params.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.80);

            if (isUser) {
                params.gravity = Gravity.END;
                tv.setBackground(ContextCompat.getDrawable(this, R.drawable.user_bubble_bg));
            } else {
                params.gravity = Gravity.START;
                tv.setBackground(ContextCompat.getDrawable(this, R.drawable.assistant_bubble_bg));
            }

            tv.setLayoutParams(params);
            messagesContainer.addView(tv);

            // Scroll to bottom
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    void showTyping(boolean show) {
        runOnUiThread(() ->
            typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    void startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return;

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle p) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onPartialResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) {
                    inputMessage.setText(r.get(0));
                }
            }
            public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) {
                    inputMessage.setText(r.get(0));
                    sendMessage();
                }
            }
            public void onError(int e) {}
            public void onEvent(int t, Bundle b) {}
        });

        speechRecognizer.startListening(intent);
    }

    @Override
    public void onRequestPermissionsResult(int code,
        String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
    }

    int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("wake_word", false)) {
            startVoiceInput();
        }
    }
}
