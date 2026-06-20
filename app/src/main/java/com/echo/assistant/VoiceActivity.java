package com.echo.assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import android.os.Bundle;

public class VoiceActivity extends Activity {

    private SpeechRecognizer sr;
    private Handler h =
        new Handler(Looper.getMainLooper());
    private TextView tvStatus;
    private EditText etVoiceText;
    private boolean resultReceived = false;

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(
                R.layout.activity_voice);

            tvStatus =
                findViewById(R.id.tv_status);
            etVoiceText =
                findViewById(R.id.et_voice_text);

            if (findViewById(R.id.btn_back)
                != null)
                findViewById(R.id.btn_back)
                    .setOnClickListener(v -> {
                    stopListening();
                    finish();
                });

            if (findViewById(R.id.btn_stop)
                != null)
                findViewById(R.id.btn_stop)
                    .setOnClickListener(v -> {
                    stopListening();
                    finish();
                });

            animateBars();
            startListening();

        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    void startListening() {
        try {
            resultReceived = false;

            if (!SpeechRecognizer
                .isRecognitionAvailable(this)) {
                Toast.makeText(this,
                    "Speech recognition not"
                    + " available Boss",
                    Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

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
                .EXTRA_PARTIAL_RESULTS,
                true);
            i.putExtra(
                RecognizerIntent
                .EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                3000L);

            sr = SpeechRecognizer
                .createSpeechRecognizer(this);
            sr.setRecognitionListener(
                new RecognitionListener() {

                public void onReadyForSpeech(
                    Bundle b) {
                    if (tvStatus != null)
                        tvStatus.setText(
                        "Listening...");
                }

                public void onBeginningOfSpeech()
                    {}

                public void onRmsChanged(
                    float r) {}

                public void onBufferReceived(
                    byte[] b) {}

                public void onEndOfSpeech() {
                    if (tvStatus != null)
                        tvStatus.setText(
                        "Processing...");
                }

                public void onPartialResults(
                    Bundle b) {
                    try {
                        ArrayList<String> r =
                            b.getStringArrayList(
                            SpeechRecognizer
                            .RESULTS_RECOGNITION);
                        if (r != null
                            && !r.isEmpty()
                            && etVoiceText != null)
                            etVoiceText.setText(
                            r.get(0));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onResults(Bundle b) {
                    try {
                        resultReceived = true;
                        ArrayList<String> r =
                            b.getStringArrayList(
                            SpeechRecognizer
                            .RESULTS_RECOGNITION);
                        if (r != null
                            && !r.isEmpty()) {
                            String text =
                                r.get(0);
                            if (etVoiceText
                                != null)
                                etVoiceText
                                .setText(text);
                            h.postDelayed(() -> {
                                try {
                                    Intent chat =
                                        new Intent(
                                        VoiceActivity
                                        .this,
                                        ChatActivity
                                        .class);
                                    chat.putExtra(
                                        "message",
                                        text);
                                    startActivity(
                                        chat);
                                    finish();
                                } catch (
                                    Exception e) {
                                    e.printStackTrace();
                                }
                            }, 300);
                        } else {
                            if (tvStatus != null)
                                tvStatus.setText(
                                "Didn't catch"
                                + " that Boss."
                                + " Try again.");
                            h.postDelayed(
                                () -> startListening(),
                                1000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onError(int error) {
                    try {
                        if (resultReceived)
                            return;
                        String msg;
                        switch (error) {
                            case SpeechRecognizer
                                .ERROR_NO_MATCH:
                                msg = "Didn't"
                                + " catch that"
                                + " Boss."
                                + " Try again.";
                                break;
                            case SpeechRecognizer
                                .ERROR_SPEECH_TIMEOUT:
                                msg = "No speech"
                                + " detected.";
                                break;
                            case SpeechRecognizer
                                .ERROR_NETWORK:
                                msg = "Network"
                                + " error Boss.";
                                break;
                            default:
                                msg = "Error "
                                + error
                                + ". Try again.";
                        }
                        if (tvStatus != null)
                            tvStatus.setText(msg);
                        h.postDelayed(
                            () -> startListening(),
                            1500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onEvent(int t,
                    Bundle b) {}
            });

            sr.startListening(i);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                "Voice error: "
                + e.getMessage(),
                Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void stopListening() {
        try {
            if (sr != null) {
                sr.stopListening();
                sr.destroy();
                sr = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void animateBars() {
        try {
            int[] barIds = {
                R.id.bar1,
                R.id.bar2,
                R.id.bar3,
                R.id.bar4,
                R.id.bar5
            };
            long[] delays = {0, 100, 150, 250, 300};

            for (int i = 0;
                i < barIds.length; i++) {
                android.view.View bar =
                    findViewById(barIds[i]);
                if (bar == null) continue;
                ScaleAnimation anim =
                    new ScaleAnimation(
                    1f, 1f, 0.3f, 1f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    1f);
                anim.setDuration(600);
                anim.setStartOffset(delays[i]);
                anim.setRepeatCount(
                    Animation.INFINITE);
                anim.setRepeatMode(
                    Animation.REVERSE);
                anim.setInterpolator(
                    new AccelerateDecelerateInterpolator());
                bar.startAnimation(anim);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        stopListening();
    }

    protected void onPause() {
        super.onPause();
        stopListening();
    }
}
