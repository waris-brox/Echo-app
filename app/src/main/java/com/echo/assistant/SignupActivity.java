package com.echo.assistant;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.widget.*;

public class SignupActivity extends Activity {
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_signup);

        EditText etName = findViewById(R.id.et_name);
        EditText etEmail = findViewById(R.id.et_email);
        EditText etPass = findViewById(R.id.et_password);

        findViewById(R.id.btn_signup).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields Boss", Toast.LENGTH_SHORT).show();
                return;
            }

            getSharedPreferences("echo_user", MODE_PRIVATE).edit()
                .putString("name", name)
                .putString("email", email)
                .putString("password", pass)
                .putBoolean("logged_in", true)
                .apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.tv_login).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
