package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

    protected void onCreate(Bundle s) {
        super.onCreate(s);

        try {
            setContentView(R.layout.activity_login);

            EditText etEmail = findViewById(R.id.et_email);
            EditText etPass  = findViewById(R.id.et_password);

            findViewById(R.id.btn_login).setOnClickListener(v -> {
                try {
                    String email = etEmail.getText().toString().trim();
                    String pass  = etPass.getText().toString().trim();

                    if (email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this,
                            "Fill all fields Boss",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String savedEmail = AppPrefs.getEmail(this);
                    String savedPass  = getSharedPreferences(
                        "echo_user", 0).getString("password", "");

                    if (email.equals(savedEmail)
                        && pass.equals(savedPass)) {
                        getSharedPreferences("echo_user", 0)
                            .edit()
                            .putBoolean("logged_in", true)
                            .apply();

                        Intent i = new Intent(this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();

                    } else {
                        Toast.makeText(this,
                            "Wrong credentials Boss",
                            Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this,
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                }
            });

            if (findViewById(R.id.btn_google) != null) {
                findViewById(R.id.btn_google).setOnClickListener(v ->
                    Toast.makeText(this,
                        "Google Sign In coming soon Boss",
                        Toast.LENGTH_SHORT).show());
            }

            if (findViewById(R.id.tv_signup) != null) {
                findViewById(R.id.tv_signup).setOnClickListener(v -> {
                    startActivity(new Intent(this, SignupActivity.class));
                    finish();
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
