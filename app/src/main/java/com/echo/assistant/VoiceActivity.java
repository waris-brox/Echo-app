package com.echo.assistant;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.speech.*;
import android.view.animation.*;
import android.widget.*;
import java.util.*;

public class VoiceActivity extends Activity {

    private TextView tvStatus, tvVoiceText;
    private SpeechRecognizer sr;
    private Handler h = new Handler(Looper.getMainLooper());

    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_voice);

        tvStatus = findViewById(R.id.tv_status);
        tvVoiceText = findViewById(R.id.et_voice_text);

        findViewById(R.id.btn_back).setOnClickListener(v -> { stopListening(); finish(); });
        findViewById(R.id.btn_stop).setOnClickListener(v -> { stopListening(); finish(); });

        animateBars();
        startListening();
    }

    void startListening() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) { tvStatus.setText("Listening..."); }
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { tvStatus.setText("Processing..."); }
            public void onPartialResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) tvVoiceText.setText(r.get(0));
            }
            public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) {
                    String text = r.get(0);
                    tvVoiceText.setText(text);
                    h.postDelayed(() -> {
                        startActivity(new Intent(VoiceActivity.this, ChatActivity.class)
                            .putExtra("message", text));
                        finish();
                    }, 400);
                }
            }
            public void onError(int e) { tvStatus.setText("Tap mic to try again"); }
            public void onEvent(int t, Bundle b) {}
        });
        sr.startListening(i);
    }

    void stopListening() {
        if (sr != null) { sr.stopListening(); sr.destroy(); sr = null; }
    }

    void animateBars() {
        int[] bars = {R.id.bar1, R.id.bar2, R.id.bar3, R.id.bar4, R.id.bar5};
        int[] delays = {0, 100, 150, 250, 300};
        for (int i = 0; i < bars.length; i++) {
            android.view.View bar = findViewById(bars[i]);
            ScaleAnimation anim = new ScaleAnimation(1f, 1f, 0.3f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1f);
            anim.setDuration(600);
            anim.setStartOffset(delays[i]);
            anim.setRepeatCount(Animation.INFINITE);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            bar.startAnimation(anim);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        stopListening();
    }
}
