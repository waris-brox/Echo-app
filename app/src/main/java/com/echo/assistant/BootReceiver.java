package com.echo.assistant;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            c.startService(new Intent(c, EchoWakeService.class));
        }
    }
}
