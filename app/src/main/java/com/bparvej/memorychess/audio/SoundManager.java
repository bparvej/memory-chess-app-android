package com.bparvej.memorychess.audio;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class SoundManager {

    private static SoundManager instance;
    private final ToneGenerator toneGenerator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SoundManager() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    public void playDiceRollSound() {
        try {
            for (int i = 0; i < 5; i++) {
                final int delay = i * 90;
                handler.postDelayed(() -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 60), delay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playWinSound() {
        try {
            toneGenerator.startTone(ToneGenerator.TONE_DTMF_1, 150);
            handler.postDelayed(() -> toneGenerator.startTone(ToneGenerator.TONE_DTMF_3, 150), 150);
            handler.postDelayed(() -> toneGenerator.startTone(ToneGenerator.TONE_DTMF_5, 150), 300);
            handler.postDelayed(() -> toneGenerator.startTone(ToneGenerator.TONE_DTMF_9, 300), 450);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playDefeatSound() {
        try {
            toneGenerator.startTone(ToneGenerator.TONE_DTMF_8, 200);
            handler.postDelayed(() -> toneGenerator.startTone(ToneGenerator.TONE_DTMF_5, 200), 200);
            handler.postDelayed(() -> toneGenerator.startTone(ToneGenerator.TONE_DTMF_2, 350), 400);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
