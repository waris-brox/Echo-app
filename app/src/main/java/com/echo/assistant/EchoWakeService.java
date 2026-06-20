package com.echo.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.Bundle;

public class EchoWakeService extends Service {

    private SpeechRecognizer sr;
    private Handler h =
        new Handler(Looper.getMainLooper());
    private boolean listening = false;
    private boolean running = false;
    private AudioManager am;

    public int onStartCommand(Intent i,
        int f, int s) {
        running = true;
        am = (AudioManager) getSystemService(
            AUDIO_SERVICE);
        showNotification();
        h.postDelayed(
            this::startListening, 1500);
        return START_STICKY;
    }

    void silenceBeep() {
        try {
            if (am == null) return;
            // Silence notification stream
            // to remove mic beep sound
            am.setStreamVolume(
                AudioManager.STREAM_NOTIFICATION,
                0, 0);
            am.setStreamMute(
                AudioManager.STREAM_NOTIFICATION,
                true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void restoreSound() {
        try {
            if (am == null) return;
            am.setStreamMute(
                AudioManager.STREAM_NOTIFICATION,
                false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startListening() {
        if (!running || listening) return;
        try {
            if (checkSelfPermission(
                android.Manifest.permission
                .RECORD_AUDIO)
                != PackageManager
                .PERMISSION_GRANTED) {
                h.postDelayed(
                    this::startListening,
                    5000);
                return;
            }
            if (!SpeechRecognizer
                .isRecognitionAvailable(this)) {
                h.postDelayed(
                    this::startListening,
                    5000);
                return;
            }

            silenceBeep();

            Intent i = new Intent(
                RecognizerIntent
                .ACTION_RECOGNIZE_SPEECH);
            i.putExtra(
                RecognizerIntent
                .EXTRA_LANGUAGE_MODEL,
                RecognizerIntent
                .LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN");
            i.putExtra(
                RecognizerIntent
                .EXTRA_CALLING_PACKAGE,
                getPackageName());
            i.putExtra(
                RecognizerIntent
                .EXTRA_PARTIAL_RESULTS, true);
            i.putExtra(
                RecognizerIntent
                .EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                5000L);
            i.putExtra(
                RecognizerIntent
                .EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                5000L);
            i.putExtra(
                RecognizerIntent
                .EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1000L);

            sr = SpeechRecognizer
                .createSpeechRecognizer(this);
            sr.setRecognitionListener(
                new RecognitionListener() {

                public void onReadyForSpeech(
                    Bundle b) {
                    listening = true;
                    // Restore sound after
                    // beep would have played
                    h.postDelayed(
                        () -> restoreSound(),
                        500);
                }

                public void onBeginningOfSpeech()
                    {}

                public void onRmsChanged(
                    float r) {}

                public void onBufferReceived(
                    byte[] b) {}

                public void onEndOfSpeech() {}

                public void onPartialResults(
                    Bundle b) {
                    try {
                        ArrayList<String> r =
                            b.getStringArrayList(
                            SpeechRecognizer
                            .RESULTS_RECOGNITION);
                        if (r != null) {
                            for (String s : r) {
                                if (isWakeWord(
                                    s)) {
                                    wakeUp();
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onResults(
                    Bundle b) {
                    try {
                        listening = false;
                        ArrayList<String> r =
                            b.getStringArrayList(
                            SpeechRecognizer
                            .RESULTS_RECOGNITION);
                        if (r != null) {
                            for (String s : r) {
                                if (isWakeWord(
                                    s)) {
                                    wakeUp();
                                    return;
                                }
                            }
                        }
                        h.postDelayed(
                            () -> startListening(),
                            300);
                    } catch (Exception e) {
                        e.printStackTrace();
                        h.postDelayed(
                            () -> startListening(),
                            1000);
                    }
                }

                public void onError(int error) {
                    listening = false;
                    restoreSound();
                    long delay = 1000;
                    if (error ==
                        SpeechRecognizer
                        .ERROR_RECOGNIZER_BUSY)
                        delay = 2000;
                    if (error ==
                        SpeechRecognizer
                        .ERROR_NETWORK)
                        delay = 3000;
                    h.postDelayed(
                        () -> startListening(),
                        delay);
                }

                public void onEvent(int t,
                    Bundle b) {}
            });

            sr.startListening(i);

        } catch (Exception e) {
            e.printStackTrace();
            listening = false;
            h.postDelayed(
                this::startListening, 2000);
        }
    }

    boolean isWakeWord(String s) {
        if (s == null) return false;
        String lower = s.toLowerCase().trim();
        return lower.equals("echo")
            || lower.contains("hello echo")
            || lower.contains("hey echo")
            || lower.contains("ok echo")
            || lower.startsWith("echo ")
            || lower.endsWith(" echo");
    }

    void wakeUp() {
        try {
            listening = false;
            if (sr != null)
                sr.stopListening();
            Intent i = new Intent(
                this, MainActivity.class);
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent
                .FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra("wake_word", true);
            startActivity(i);
            h.postDelayed(
                this::startListening, 5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void showNotification() {
        try {
            String CH = "echo_wake";
            NotificationManager nm =
                (NotificationManager)
                getSystemService(
                NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch =
                    new NotificationChannel(
                    CH, "Echo Listening",
                    NotificationManager
                    .IMPORTANCE_LOW);
                ch.setSound(null, null);
                ch.enableVibration(false);
                nm.createNotificationChannel(ch);
            }
            Notification n =
                new Notification.Builder(
                this, CH)
                .setContentTitle("Echo")
                .setContentText(
                "Listening for wake word...")
                .setSmallIcon(
                android.R.drawable
                .ic_btn_speak_now)
                .setOngoing(true)
                .build();
            startForeground(1, n);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IBinder onBind(Intent i) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        running = false;
        try {
            if (sr != null) sr.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        h.postDelayed(() -> {
            try {
                startService(new Intent(
                    this,
                    EchoWakeService.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 2000);
    }
}
