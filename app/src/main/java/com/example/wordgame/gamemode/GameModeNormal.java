package com.example.wordgame.gamemode;

import android.util.Log;

import com.example.wordgame.R;
import com.example.wordgame.audio.AudioHandler;
import com.example.wordgame.models.Board;
import com.example.wordgame.models.HighScoreData;
import com.example.wordgame.gamemode.models.WordFoundReward;
import com.example.wordgame.utility.ScoreUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Normal game mode. Normal game mode consists of a 4x4 board. Longer words provide more points.
 */
public class GameModeNormal extends GameMode {
    private static final GameModeType gameModeType = GameModeType.NORMAL;
    public static final Map<Integer, Integer> SCORE_MAP = Collections.unmodifiableMap(
            new HashMap<Integer, Integer>() {
        {
            put(3, 1);
            put(4, 3);
            put(5, 7);
            put(6, 13);
            put(7, 21);
            put(8, 31);
            put(9, 42);
            put(10, 57);
        }
    });
    public GameModeNormal(Board board) {
        super(board);
    }

    @Override
    public GameModeType getGameModeType() {
        return gameModeType;
    }

    @Override
    public int getMaxScore() {
        return ScoreUtils.calculateScoreForBoard(SCORE_MAP, gameBoard);
    }

    @Override
    public Map<Integer, Integer> registerSounds(AudioHandler audioHandler) {
        Log.d("GameModeNormal", "Register sounds");
        final Map<Integer, Integer> audioMap = super.registerSounds(audioHandler);
        final int foundMid = audioHandler.addSoundToPool(R.raw.snd_found_mid, 1);
        final int foundMed = audioHandler.addSoundToPool(R.raw.snd_found_medium, 1);
        final int foundLong = audioHandler.addSoundToPool(R.raw.snd_found_long, 1);
        final int foundXLong = audioHandler.addSoundToPool(R.raw.snd_found_xlong, 1);
        audioMap.put(R.raw.snd_found_mid, foundMid);
        audioMap.put(R.raw.snd_found_medium, foundMed);
        audioMap.put(R.raw.snd_found_long, foundLong);
        audioMap.put(R.raw.snd_found_xlong, foundXLong);
        return audioMap;
    }

    @Override
    public WordFoundReward getWordReward(String word) {
        int score = Objects.requireNonNull(SCORE_MAP.get(word.length()));
        return new WordFoundReward(score, 0, getRewardSoundId(word));
    }

    @Override
    public HighScoreData getHighScoreData() {
        return new HighScoreData();
    }

    private static int getRewardSoundId(String word) {
        final int wordLength = word.length();
        if (wordLength > 9) {
            return R.raw.snd_found_xlong;
        } else if (wordLength > 7) {
            return R.raw.snd_found_long;
        } else if (wordLength > 6) {
            return R.raw.snd_found_medium;
        } else if (wordLength > 4) {
            return R.raw.snd_found_mid;
        } else {
            return R.raw.snd_found_short;
        }
    }
}