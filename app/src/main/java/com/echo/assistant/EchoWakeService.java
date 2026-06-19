package com.echo.assistant;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.*;
import java.util.*;

public class EchoWakeService extends Service {
    private SpeechRecognizer sr;
    private Handler h = new Handler(Looper.getMainLooper());
    private boolean listening = false;

    public int onStartCommand(Intent i, int f, int s) {
        notify_();
        startListening();
        return START_STICKY;
    }

    void startListening() {
        if (listening) return;
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) { listening = true; }
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onResults(Bundle b) {
                listening = false;
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null) for (String s : r) if (s.toLowerCase().contains("echo")) { wake(); return; }
                h.postDelayed(() -> startListening(), 500);
            }
            public void onPartialResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null) for (String s : r) if (s.toLowerCase().contains("echo")) { wake(); return; }
            }
            public void onError(int e) { listening = false; h.postDelayed(() -> startListening(), 2000); }
            public void onEvent(int t, Bundle b) {}
        });
        try { sr.startListening(i); } catch (Exception e) { h.postDelayed(() -> startListening(), 2000); }
    }

    void wake() {
        listening = false;
        if (sr != null) sr.stopListening();
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("wake_word", true);
        startActivity(i);
        h.postDelayed(() -> startListening(), 4000);
    }

    void notify_() {
        String CH = "echo_wake";
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "Echo Wake", NotificationManager.IMPORTANCE_LOW);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
        startForeground(1, new Notification.Builder(this, CH)
            .setContentTitle("Echo is listening")
            .setContentText("Say Echo to wake me up Boss")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build());
    }

    public IBinder onBind(Intent i) { return null; }

    public void onDestroy() {
        super.onDestroy();
        if (sr != null) sr.destroy();
        h.postDelayed(() -> startService(new Intent(this, EchoWakeService.class)), 1000);
    }
}
