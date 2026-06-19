package com.echo.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    private EditText etInput;
    private static final int PERM_CODE = 200;

    protected void onCreate(Bundle s) {
        super.onCreate(s);

        // Check login
        if (!AppPrefs.isLoggedIn(this)) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }

        // Load API key
        GroqAPI.API_KEY = AppPrefs.getApiKey(this);

        setContentView(R.layout.activity_main);

        etInput = findViewById(R.id.et_input);

        // Set greeting
        TextView tvGreeting = findViewById(R.id.tv_greeting);
        if (tvGreeting != null)
            tvGreeting.setText("Hello " + AppPrefs.getName(this) + " 👋");

        // Buttons
        findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_mic).setOnClickListener(v -> startVoice());
        findViewById(R.id.btn_menu).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        // Suggestion chips
        findViewById(R.id.chip_time).setOnClickListener(v ->
            goChat("What is the time and date today?"));
        findViewById(R.id.chip_device).setOnClickListener(v ->
            goChat("What is my battery level and storage?"));
        findViewById(R.id.chip_call).setOnClickListener(v ->
            goChat("I want to make a call"));
        findViewById(R.id.chip_alarm).setOnClickListener(v ->
            goChat("Set an alarm for me"));
        findViewById(R.id.chip_app).setOnClickListener(v ->
            goChat("Open an app for me"));

        etInput.setOnEditorActionListener((v, a, e) -> {
            sendMessage();
            return true;
        });

        // Request permissions
        requestPermissions();

        // Start wake word service
        startService(new Intent(this, EchoWakeService.class));

        // Check wake word
        if (getIntent().getBooleanExtra("wake_word", false)) {
            startVoice();
        }
    }

    void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        etInput.setText("");
        goChat(text);
    }

    void goChat(String msg) {
        startActivity(new Intent(this, ChatActivity.class)
            .putExtra("message", msg));
    }

    void startVoice() {
        startActivity(new Intent(this, VoiceActivity.class));
    }

    void requestPermissions() {
        List<String> perms = new ArrayList<>();
        String[] needed = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
        };
        for (String p : needed) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                perms.add(p);
            }
        }
        if (!perms.isEmpty()) {
            requestPermissions(perms.toArray(new String[0]), PERM_CODE);
        }
    }

    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i.getBooleanExtra("wake_word", false)) startVoice();
    }

    protected void onResume() {
        super.onResume();
        // Reload API key in case user updated it in settings
        GroqAPI.API_KEY = AppPrefs.getApiKey(this);
    }
}
