package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

public class LoginActivity extends Activity {
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.et_email);
        EditText etPass = findViewById(R.id.et_password);

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields Boss",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            String savedEmail = AppPrefs.getEmail(this);
            String savedPass = getSharedPreferences("echo_user", 0)
                .getString("password", "");

            if (email.equals(savedEmail) && pass.equals(savedPass)) {
                getSharedPreferences("echo_user", 0)
                    .edit().putBoolean("logged_in", true).apply();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Wrong credentials Boss",
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Google sign in placeholder
        findViewById(R.id.btn_google).setOnClickListener(v -> {
            Toast.makeText(this,
                "Google Sign In coming soon Boss",
                Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.tv_signup).setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });
    }
}
