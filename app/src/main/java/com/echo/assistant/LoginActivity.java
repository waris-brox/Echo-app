package com.echo.assistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip login if already logged in
        SharedPreferences prefs = getSharedPreferences("echo_prefs", MODE_PRIVATE);
        String savedName = prefs.getString("user_name", "");
        if (!savedName.isEmpty()) {
            goToChat(savedName);
            return;
        }

        setContentView(R.layout.activity_login);

        EditText inputName = findViewById(R.id.input_name);
        Button btnEnter = findViewById(R.id.btn_enter);

        btnEnter.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) name = "Boss";
            prefs.edit().putString("user_name", name).apply();
            goToChat(name);
        });
    }

    void goToChat(String name) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("user_name", name);
        startActivity(i);
        finish();
    }
}
