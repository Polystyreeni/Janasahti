package com.example.wordgame.gamemode;

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
 * Extended game mode. Extended game mode is played on a 5x5 grid with extended playtime and
 * longer words.
 */
public class GameModeExtended extends GameMode {
    private static final GameModeType gameModeType = GameModeType.EXTENDED;
    private static final long GAME_DURATION = 120_000L;
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
            put(11, 75);
            put(12, 92);
            put(13, 105);
            put(14, 130);
            put(15, 155);
            put(16, 180);
        }
    });

    @Override
    public GameModeType getGameModeType() {
        return gameModeType;
    }
    public GameModeExtended(Board board) {
        super(board);
    }
    @Override
    public long getGameDuration() {
        return GAME_DURATION;
    }
    @Override
    public int getMaxScore() {
        return ScoreUtils.calculateScoreForBoard(SCORE_MAP, gameBoard);
    }

    @Override
    public Map<Integer, Integer> registerSounds(AudioHandler audioHandler) {
        final Map<Integer, Integer> audioMap = super.registerSounds(audioHandler);
        audioMap.put(R.raw.snd_found_mid, audioHandler.addSoundToPool(R.raw.snd_found_mid, 1));
        audioMap.put(R.raw.snd_found_medium, audioHandler.addSoundToPool(R.raw.snd_found_medium, 1));
        audioMap.put(R.raw.snd_found_long, audioHandler.addSoundToPool(R.raw.snd_found_long, 1));
        audioMap.put(R.raw.snd_found_xlong, audioHandler.addSoundToPool(R.raw.snd_found_xlong, 1));
        return audioMap;
    }

    @Override
    public WordFoundReward getWordReward(String word) {
        Integer reward = Objects.requireNonNull(SCORE_MAP.get(word.length()));
        return new WordFoundReward(reward, 0, getRewardSoundId(word));
    }

    @Override
    public HighScoreData getHighScoreData() {
        return new HighScoreData();
    }

    private static int getRewardSoundId(String word) {
        final int wordLength = word.length();
        if (wordLength > 13) {
            return R.raw.snd_found_xlong;
        } else if (wordLength > 10) {
            return R.raw.snd_found_long;
        } else if (wordLength > 7) {
            return R.raw.snd_found_medium;
        } else if (wordLength > 4) {
            return R.raw.snd_found_mid;
        } else {
            return R.raw.snd_found_short;
        }
    }
}
