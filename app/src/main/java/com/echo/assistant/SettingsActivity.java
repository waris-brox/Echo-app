package com.echo.assistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("echo_prefs", MODE_PRIVATE);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        updateApiStatus();
        findViewById(R.id.item_api_key).setOnClickListener(v -> showApiKeyDialog());

        findViewById(R.id.item_accessibility).setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.item_write_settings).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            i.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });

        Switch wakeSwitch = findViewById(R.id.switch_wake);
        boolean wakeEnabled = prefs.getBoolean("wake_enabled", true);
        wakeSwitch.setChecked(wakeEnabled);
        wakeSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("wake_enabled", checked).apply();
            if (checked) {
                startService(new Intent(this, EchoWakeService.class));
                Toast.makeText(this, "Wake word enabled", Toast.LENGTH_SHORT).show();
            } else {
                stopService(new Intent(this, EchoWakeService.class));
                Toast.makeText(this, "Wake word disabled", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.item_support).setOnClickListener(v ->
            Toast.makeText(this,
                "Email: echo.ai.virtual.assistant@gmail.com",
                Toast.LENGTH_LONG).show());
    }

    void showApiKeyDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter Groq API key");
        input.setText(prefs.getString("groq_api_key", ""));
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(this)
            .setTitle("Groq API Key")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String key = input.getText().toString().trim();
                prefs.edit().putString("groq_api_key", key).apply();
                GroqAPI.API_KEY = key;
                updateApiStatus();
                Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    void updateApiStatus() {
        String key = prefs.getString("groq_api_key", "");
        TextView status = findViewById(R.id.tv_api_status);
        if (status == null) return;
        if (key.isEmpty()) {
            status.setText("⚠️ Not set — tap to add");
        } else {
            status.setText("✓ Set — tap to update");
        }
    }
}
