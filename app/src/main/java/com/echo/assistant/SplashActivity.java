package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class SplashActivity extends Activity {

    protected void onCreate(Bundle s) {
        super.onCreate(s);

        try {
            if (AppPrefs.isLoggedIn(this)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }
            setContentView(R.layout.activity_splash);
            findViewById(R.id.btn_get_started).setOnClickListener(v -> {
                startActivity(new Intent(this, SignupActivity.class));
            });
            findViewById(R.id.tv_login).setOnClickListener(v -> {
                startActivity(new Intent(this, LoginActivity.class));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

