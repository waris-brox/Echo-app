package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {
    protected void onCreate(Bundle s) {
        super.onCreate(s);

        // Check if already logged in
        if (AppPrefs.isLoggedIn(this)) {
            // Go directly to main screen
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_splash);

        findViewById(R.id.btn_get_started).setOnClickListener(v ->
            startActivity(new Intent(this, SignupActivity.class)));

        findViewById(R.id.tv_login).setOnClickListener(v ->
            startActivity(new Intent(this, LoginActivity.class)));
    }
}
