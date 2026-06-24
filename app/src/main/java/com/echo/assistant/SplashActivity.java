package com.echo.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.view.View;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SharedPreferences prefs =
        getSharedPreferences("echo_prefs", MODE_PRIVATE);

    if (!prefs.getString("user_name", "").isEmpty()) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
        return;
    }

    setContentView(R.layout.activity_splash);

        View root = findViewById(R.id.splash_logo);
        AnimationSet anim = new AnimationSet(true);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(700);
        ScaleAnimation scale = new ScaleAnimation(
            0.92f, 1f, 0.92f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(700);
        anim.addAnimation(fade);
        anim.addAnimation(scale);
        root.startAnimation(anim);

        findViewById(R.id.btn_get_started).setOnClickListener(v -> {
    startActivity(new Intent(this, LoginActivity.class));
    finish();
	});
    }
}
