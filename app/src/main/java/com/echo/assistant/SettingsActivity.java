package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    protected void onCreate(Bundle s) {
        super.onCreate(s);

        try {
            setContentView(R.layout.activity_settings);

            TextView tvName  = findViewById(R.id.tv_name);
            TextView tvEmail = findViewById(R.id.tv_email);
            EditText etKey   = findViewById(R.id.et_api_key);
            Switch swVoice   = findViewById(R.id.sw_voice);

            if (tvName != null)
                tvName.setText(AppPrefs.getName(this));
            if (tvEmail != null)
                tvEmail.setText(AppPrefs.getEmail(this));
            if (etKey != null)
                etKey.setText(AppPrefs.getApiKey(this));
            if (swVoice != null) {
                swVoice.setChecked(
                    AppPrefs.isVoiceEnabled(this));
                swVoice.setOnCheckedChangeListener(
                    (btn, checked) ->
                        AppPrefs.setVoiceEnabled(
                            this, checked));
            }

            if (findViewById(R.id.btn_save_key) != null)
                findViewById(R.id.btn_save_key)
                    .setOnClickListener(v -> {
                    try {
                        if (etKey == null) return;
                        String key = etKey.getText()
                            .toString().trim();
                        if (key.isEmpty()) {
                            Toast.makeText(this,
                                "Enter API key Boss",
                                Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppPrefs.saveApiKey(this, key);
                        Toast.makeText(this,
                            "API key saved Boss!",
                            Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            if (findViewById(R.id.btn_logout) != null)
                findViewById(R.id.btn_logout)
                    .setOnClickListener(v -> {
                    try {
                        AppPrefs.logout(this);
                        Intent i = new Intent(this,
                            SplashActivity.class);
                        i.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            if (findViewById(R.id.btn_back) != null)
                findViewById(R.id.btn_back)
                    .setOnClickListener(v -> finish());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                "Settings error: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }
}
