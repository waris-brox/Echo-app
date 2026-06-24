package com.echo.assistant;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;
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
    private String userName = "Boss";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_chat);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Layout missing: activity_chat", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Get user name safely
        try {
            userName = getIntent().getStringExtra("user_name");
            if (userName == null || userName.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences("echo_prefs", MODE_PRIVATE);
                userName = prefs.getString("user_name", "Boss");
            }
        } catch (Exception e) {
            userName = "Boss";
        }

        // Load API key safely
        try {
            SharedPreferences prefs = getSharedPreferences("echo_prefs", MODE_PRIVATE);
            String apiKey = prefs.getString("groq_api_key", "");
            if (GroqAPI.API_KEY != null || apiKey != null) {
                GroqAPI.API_KEY = apiKey;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Bind views safely
        try {
            messagesContainer = findViewById(R.id.messages_container);
            scrollView = findViewById(R.id.scroll_messages);
            inputMessage = findViewById(R.id.input_message);
            greetingLayout = findViewById(R.id.greeting_layout);
            typingIndicator = findViewById(R.id.typing_indicator);

            if (messagesContainer == null || scrollView == null ||
                inputMessage == null || greetingLayout == null || typingIndicator == null) {
                Toast.makeText(this, "UI views missing — check activity_chat.xml IDs",
                    Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }

        // Update greeting
        try {
            TextView greetingText = findViewById(R.id.greeting_text);
            if (greetingText != null) {
                greetingText.setText("Hello, " + userName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Send button
        try {
            View btnSend = findViewById(R.id.btn_send);
            if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Voice button
        try {
            View btnVoice = findViewById(R.id.btn_voice);
            if (btnVoice != null) btnVoice.setOnClickListener(v -> startVoiceInput());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Settings button — safe check if SettingsActivity exists
        try {
            View btnSettings = findViewById(R.id.btn_settings);
            if (btnSettings != null) {
                btnSettings.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(this, SettingsActivity.class));
                    } catch (Exception ex) {
                        Toast.makeText(this, "Settings not available", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TTS init
        try {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(new Locale("en", "IN"));
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.ENGLISH);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Request mic permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        // Handle wake word trigger
        try {
            if (getIntent().getBooleanExtra("wake_word", false)) {
                startVoiceInput();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendMessage() {
        if (inputMessage == null) return;
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

        try {
            GroqAPI.ask(chatHistory, text, new GroqAPI.Callback() {
                public void onSuccess(String reply, String action) {
                    runOnUiThread(() -> {
                        showTyping(false);
                        addMessage(reply, false);
                        speak(reply);
                    });

                    try {
                        JSONObject assistantMsg = new JSONObject();
                        assistantMsg.put("role", "assistant");
                        assistantMsg.put("content", reply);
                        chatHistory.put(assistantMsg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (action != null && !action.isEmpty()) {
                        try {
                            DeviceActions.handle(MainActivity.this, action,
                                (success, msg) -> {
                                    if (!success) {
                                        runOnUiThread(() -> addMessage("⚠️ " + msg, false));
                                    }
                                });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                public void onError(String error) {
                    runOnUiThread(() -> {
                        showTyping(false);
                        addMessage("⚠️ " + error, false);
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showTyping(false);
            addMessage("⚠️ Could not reach AI. Check your API key in Settings.", false);
        }
    }

    void addMessage(String text, boolean isUser) {
        runOnUiThread(() -> {
            try {
                if (greetingLayout != null)
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
                params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.80);

                if (isUser) {
                    params.gravity = Gravity.END;
                    // Safe drawable load
                    try {
                        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.user_bubble_bg));
                    } catch (Exception e) {
                        tv.setBackgroundColor(Color.parseColor("#DCF8C6"));
                    }
                } else {
                    params.gravity = Gravity.START;
                    try {
                        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.assistant_bubble_bg));
                    } catch (Exception e) {
                        tv.setBackgroundColor(Color.parseColor("#EFEFEF"));
                    }
                }

                tv.setLayoutParams(params);
                if (messagesContainer != null) {
                    messagesContainer.addView(tv);
                }

                if (scrollView != null) {
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    void showTyping(boolean show) {
        runOnUiThread(() -> {
            if (typingIndicator != null)
                typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }

    void speak(String text) {
        try {
            if (tts != null && text != null && !text.isEmpty()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
                speechRecognizer = null;
            }

            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Speech recognition not available on this device",
                    Toast.LENGTH_SHORT).show();
                return;
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
                    try {
                        ArrayList<String> r = b.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);
                        if (r != null && !r.isEmpty() && inputMessage != null) {
                            runOnUiThread(() -> inputMessage.setText(r.get(0)));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onResults(Bundle b) {
                    try {
                        ArrayList<String> r = b.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);
                        if (r != null && !r.isEmpty() && inputMessage != null) {
                            runOnUiThread(() -> {
                                inputMessage.setText(r.get(0));
                                sendMessage();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onError(int e) {
                    runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                            "Voice error: " + e, Toast.LENGTH_SHORT).show());
                }

                public void onEvent(int t, Bundle b) {}
            });

            speechRecognizer.startListening(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Voice input failed: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code,
            String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
    }

    int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (tts != null) tts.shutdown(); } catch (Exception e) { e.printStackTrace(); }
        try { if (speechRecognizer != null) speechRecognizer.destroy(); } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            if (intent.getBooleanExtra("wake_word", false)) {
                startVoiceInput();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
