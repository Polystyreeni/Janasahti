package com.example.wordgame.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import androidx.appcompat.app.AppCompatActivity;

public class AudioHandler {
    private final SoundPool soundPool;
    private float volume;
    private static final int STREAM_TYPE = android.media.AudioManager.STREAM_MUSIC;
    private static final int MAX_STREAMS = 5;

    private final AudioManager audioManager;
    private final AppCompatActivity activity;


    public AudioHandler(AppCompatActivity activity) {
        this.activity = activity;
        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

        updateVolume();

        activity.setVolumeControlStream(STREAM_TYPE);

        AudioAttributes audioAttrib = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setAudioAttributes(audioAttrib).setMaxStreams(MAX_STREAMS);

        this.soundPool = builder.build();
    }

    public int addSoundToPool(int soundId, int priority) {
        return soundPool.load(activity, soundId, priority);
    }

    public void playSound(int soundId, float leftVolume, float rightVolume, int priority,
                          int loop, float rate) {
        if (audioManager.getMode() == AudioManager.MODE_IN_CALL) {
            // Avoid playing audio on top of calls
            return;
        }
        updateVolume();
        soundPool.play(soundId, leftVolume, rightVolume, priority, loop, rate);
    }

    public float getVolume() {
        return this.volume;
    }

    public void release() {
        this.soundPool.release();
    }

    private void updateVolume() {
        float currentVolumeIndex = (float) audioManager.getStreamVolume(STREAM_TYPE);
        float maxVolumeIndex  = (float) audioManager.getStreamMaxVolume(STREAM_TYPE);
        this.volume = currentVolumeIndex / maxVolumeIndex;
    }
}
