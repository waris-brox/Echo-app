package com.echo.assistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    String[] requiredPermissions = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        updatePermissionStatus();

        findViewById(R.id.btn_grant_permissions).setOnClickListener(v -> {
            requestMissingPermissions();
        });

        findViewById(R.id.btn_notification_permission).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                Toast.makeText(this, "Notifications already enabled on your Android version",
                    Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_accessibility).setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        findViewById(R.id.btn_write_settings).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });

        findViewById(R.id.btn_continue).setOnClickListener(v -> {
            goNext();
        });
    }

    void requestMissingPermissions() {
        List<String> missing = new ArrayList<>();
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                != PackageManager.PERMISSION_GRANTED) {
                missing.add(perm);
            }
        }
        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                missing.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "All permissions already granted ✅",
                Toast.LENGTH_SHORT).show();
            updatePermissionStatus();
        }
    }

    void updatePermissionStatus() {
        TextView status = findViewById(R.id.tv_permission_status);
        int granted = 0;
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                == PackageManager.PERMISSION_GRANTED) {
                granted++;
            }
        }

        boolean accessibilityOn = EchoAccessibility.instance != null;
        boolean writeSettings = Settings.System.canWrite(this);

        String statusText = "✅ " + granted + "/" + requiredPermissions.length
            + " permissions granted\n"
            + (accessibilityOn ? "✅" : "❌") + " Accessibility service\n"
            + (writeSettings ? "✅" : "❌") + " Write settings (brightness/timeout)";

        status.setText(statusText);

        if (granted == requiredPermissions.length) {
            findViewById(R.id.btn_grant_permissions)
                .setEnabled(false);
        }
    }

    void goNext() {
    getSharedPreferences("echo_prefs", MODE_PRIVATE)
        .edit().putBoolean("permissions_done", true).apply();
    startActivity(new Intent(this, SplashActivity.class));
    finish();
}

    @Override
    public void onRequestPermissionsResult(int code,
        String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        updatePermissionStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }
}
