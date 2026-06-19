package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

public class SignupActivity extends Activity {

    protected void onCreate(Bundle s) {
        super.onCreate(s);

        try {
            setContentView(R.layout.activity_signup);

            EditText etName     = findViewById(R.id.et_name);
            EditText etEmail    = findViewById(R.id.et_email);
            EditText etPassword = findViewById(R.id.et_password);

            findViewById(R.id.btn_signup).setOnClickListener(v -> {
                try {
                    String name  = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String pass  = etPassword.getText().toString().trim();

                    if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this,
                            "Fill all fields Boss",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AppPrefs.saveUser(this, name, email, pass);

                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this,
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                }
            });

            if (findViewById(R.id.tv_login) != null) {
                findViewById(R.id.tv_login).setOnClickListener(v -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
