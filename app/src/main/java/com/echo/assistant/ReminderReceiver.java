package com.echo.assistant;

import android.app.*;
import android.content.*;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public void onReceive(Context c, Intent i) {
        String text = i.getStringExtra("text");
        if (text == null) text = "Echo Reminder";
        String CH = "echo_remind";
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CH, "Reminders", NotificationManager.IMPORTANCE_HIGH));
        }
        nm.notify((int) System.currentTimeMillis(),
            new Notification.Builder(c, CH)
                .setContentTitle("Echo Reminder")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true).build());
    }
}
