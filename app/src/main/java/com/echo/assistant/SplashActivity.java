package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            if (AppPrefs.isLoggedIn(this)) {
                Intent i = new Intent(
                    this, MainActivity.class);
                i.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent
                    .FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
                return;
            }
            setContentView(
                R.layout.activity_splash);
            findViewById(
                R.id.btn_get_started)
                .setOnClickListener(v ->
                startActivity(new Intent(this,
                SignupActivity.class)));
            findViewById(R.id.tv_login)
                .setOnClickListener(v ->
                startActivity(new Intent(this,
                LoginActivity.class)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
