package com.echo.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private EditText etInput;
    private static final int PERM_CODE = 200;

    protected void onCreate(Bundle s) {
        super.onCreate(s);

        try {
            if (!AppPrefs.isLoggedIn(this)) {
                startActivity(new Intent(this, SplashActivity.class));
                finish();
                return;
            }

            GroqAPI.API_KEY = AppPrefs.getApiKey(this);
            setContentView(R.layout.activity_main);
            etInput = findViewById(R.id.et_input);

            TextView tvGreeting = findViewById(R.id.tv_greeting);
            if (tvGreeting != null) {
                tvGreeting.setText(
                    "Hello " + AppPrefs.getName(this) + " 👋");
            }

            if (findViewById(R.id.btn_send) != null)
                findViewById(R.id.btn_send)
                    .setOnClickListener(v -> sendMessage());

            if (findViewById(R.id.btn_mic) != null)
                findViewById(R.id.btn_mic)
                    .setOnClickListener(v -> startVoice());

            if (findViewById(R.id.btn_menu) != null)
                findViewById(R.id.btn_menu).setOnClickListener(v ->
                    startActivity(new Intent(this,
                        SettingsActivity.class)));

            if (findViewById(R.id.chip_time) != null)
                findViewById(R.id.chip_time).setOnClickListener(v ->
                    goChat("What is the time and date today?"));

            if (findViewById(R.id.chip_device) != null)
                findViewById(R.id.chip_device).setOnClickListener(v ->
                    goChat("What is my battery and storage?"));

            if (findViewById(R.id.chip_call) != null)
                findViewById(R.id.chip_call).setOnClickListener(v ->
                    goChat("I want to make a call"));

            if (findViewById(R.id.chip_alarm) != null)
                findViewById(R.id.chip_alarm).setOnClickListener(v ->
                    goChat("Set an alarm for me"));

            if (findViewById(R.id.chip_app) != null)
                findViewById(R.id.chip_app).setOnClickListener(v ->
                    goChat("Open an app for me"));

            if (etInput != null) {
                etInput.setOnEditorActionListener((v, a, e) -> {
                    sendMessage();
                    return true;
                });
            }

            askPermissions();

            try {
                startService(new Intent(this, EchoWakeService.class));
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (getIntent() != null
                && getIntent().getBooleanExtra("wake_word", false)) {
                startVoice();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                "Error starting Echo: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    void sendMessage() {
        try {
            if (etInput == null) return;
            String text = etInput.getText().toString().trim();
            if (text.isEmpty()) return;
            etInput.setText("");
            goChat(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void goChat(String msg) {
        try {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("message", msg);
            startActivity(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startVoice() {
        try {
            startActivity(new Intent(this, VoiceActivity.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void askPermissions() {
        try {
            List<String> perms = new ArrayList<>();
            String[] needed = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_PHONE_STATE
            };
            for (String p : needed) {
                if (checkSelfPermission(p)
                    != PackageManager.PERMISSION_GRANTED) {
                    perms.add(p);
                }
            }
            if (!perms.isEmpty()) {
                requestPermissions(
                    perms.toArray(new String[0]), PERM_CODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onRequestPermissionsResult(int code,
        String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        // App continues even if permissions denied
        // Features that need them will ask when used
    }

    protected void onResume() {
        super.onResume();
        try {
            GroqAPI.API_KEY = AppPrefs.getApiKey(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        try {
            if (i != null && i.getBooleanExtra("wake_word", false)) {
                startVoice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
