package com.example.wordgame;

import android.app.Activity;
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

    private AudioManager audioManager = null;
    private final AppCompatActivity activity;

    // Volume
    private float currentVolumeIndex;
    private float maxVolumeIndex;

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

    public void playSound(int soundId, float leftVolume, float rightVolume, int priority, int loop, float rate) {
        updateVolume();
        soundPool.play(soundId, leftVolume, rightVolume, priority, loop, rate);
    }

    private void updateVolume() {
        this.currentVolumeIndex = (float) audioManager.getStreamVolume(STREAM_TYPE);
        this.maxVolumeIndex  = (float) audioManager.getStreamMaxVolume(STREAM_TYPE);
        this.volume = currentVolumeIndex / maxVolumeIndex;
    }

    public float getVolume() {
        return this.volume;
    }
}
