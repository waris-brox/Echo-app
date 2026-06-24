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
import android.os.Bundle;

public class EchoWakeService extends Service {

    private SpeechRecognizer sr;
    private Handler h = new Handler(Looper.getMainLooper());
    private boolean listening = false;
    private boolean running = false;
    private AudioManager audioManager;

    public int onStartCommand(Intent i, int f, int s) {
        running = true;
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        showNotification();
        h.postDelayed(this::startListening, 1000);
        return START_STICKY;
    }

    void muteSystemBeeps(boolean mute) {
        try {
            if (audioManager != null) {
                audioManager.setStreamMute(
                    AudioManager.STREAM_SYSTEM, mute);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startListening() {
        if (!running || listening) return;

        try {
            if (checkSelfPermission(
                android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                h.postDelayed(this::startListening, 5000);
                return;
            }

            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                h.postDelayed(this::startListening, 5000);
                return;
            }

            Intent i = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
            i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getPackageName());
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            i.putExtra("android.speech.extra.DICTATION_MODE", true);
            i.putExtra(RecognizerIntent
                .EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                3000);
            i.putExtra(RecognizerIntent
                .EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                3000);

            sr = SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecognitionListener() {

                public void onReadyForSpeech(Bundle b) {
                    listening = true;
                }

                public void onBeginningOfSpeech() {}

                public void onRmsChanged(float r) {}

                public void onBufferReceived(byte[] b) {}

                public void onEndOfSpeech() {}

                public void onResults(Bundle b) {
                    listening = false;
                    ArrayList<String> r = b.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                    if (r != null) {
                        for (String str : r) {
                            if (str.toLowerCase()
                                .contains("echo")) {
                                wakeUp();
                                return;
                            }
                        }
                    }
                    h.postDelayed(() -> startListening(), 1500);
                }

                public void onPartialResults(Bundle b) {
                    ArrayList<String> r = b.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                    if (r != null) {
                        for (String str : r) {
                            if (str.toLowerCase()
                                .contains("echo")) {
                                wakeUp();
                                return;
                            }
                        }
                    }
                }

                public void onError(int e) {
                    listening = false;
                    long delay = (e == SpeechRecognizer.ERROR_NO_MATCH
                        || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        ? 1500 : 3000;
                    h.postDelayed(() -> startListening(), delay);
                }

                public void onEvent(int t, Bundle b) {}
            });

            muteSystemBeeps(true);
            sr.startListening(i);
            h.postDelayed(() -> muteSystemBeeps(false), 500);

        } catch (Exception e) {
            e.printStackTrace();
            muteSystemBeeps(false);
            h.postDelayed(this::startListening, 3000);
        }
    }

    void wakeUp() {
        try {
            listening = false;
            if (sr != null) sr.stopListening();

            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra("wake_word", true);
            startActivity(i);

            h.postDelayed(this::startListening, 4000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void showNotification() {
        try {
            String CH = "echo_wake";
            NotificationManager nm = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(
                    CH, "Echo Wake Word",
                    NotificationManager.IMPORTANCE_LOW);
                ch.setSound(null, null);
                nm.createNotificationChannel(ch);
            }
            Notification n = new Notification.Builder(this, CH)
                .setContentTitle("Echo is listening")
                .setContentText("Say Echo to wake me up Boss")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
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
        muteSystemBeeps(false);
        try {
            if (sr != null) sr.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        h.postDelayed(() -> {
            try {
                startService(new Intent(this, EchoWakeService.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 2000);
    }
}
