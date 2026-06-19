package com.echo.assistant;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.widget.*;

public class SettingsActivity extends Activity {

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);

        SharedPreferences userPrefs = getSharedPreferences("echo_user", MODE_PRIVATE);
        SharedPreferences appPrefs = getSharedPreferences("echo_prefs", MODE_PRIVATE);

        TextView tvName = findViewById(R.id.tv_name);
        TextView tvEmail = findViewById(R.id.tv_email);
        EditText etApiKey = findViewById(R.id.et_api_key);
        Switch swVoice = findViewById(R.id.sw_voice);

        tvName.setText(userPrefs.getString("name", "Boss"));
        tvEmail.setText(userPrefs.getString("email", ""));
        etApiKey.setText(appPrefs.getString("groq_api_key", ""));
        swVoice.setChecked(appPrefs.getBoolean("voice_enabled", true));

        swVoice.setOnCheckedChangeListener((btn, checked) ->
            appPrefs.edit().putBoolean("voice_enabled", checked).apply());

        findViewById(R.id.btn_save_key).setOnClickListener(v -> {
            String key = etApiKey.getText().toString().trim();
            appPrefs.edit().putString("groq_api_key", key).apply();
            GroqAPI.API_KEY = key;
            Toast.makeText(this, "API key saved Boss", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            userPrefs.edit().putBoolean("logged_in", false).apply();
            startActivity(new Intent(this, SplashActivity.class));
            finishAffinity();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}
