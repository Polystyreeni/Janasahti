package com.example.wordgame.gamemode;

import com.example.wordgame.R;
import com.example.wordgame.audio.AudioHandler;
import com.example.wordgame.gamemode.models.RewardVisual;
import com.example.wordgame.models.Board;
import com.example.wordgame.models.HighScoreData;
import com.example.wordgame.models.HighScoreDataTimeChase;
import com.example.wordgame.gamemode.models.WordFoundReward;
import com.example.wordgame.utility.ExpiringSet;
import com.example.wordgame.utility.ScoreUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Time chase game mode. In time chase, words yield more game time in addition to points.
 * Quickly finding words reward bonus time.
 */
public class GameModeTime extends GameMode {
    private static final GameModeType GAME_MODE_TYPE = GameModeType.TIME_CHASE;

    // Game mode settings
    private static final long BASE_GAME_DURATION = 30_000L;
    private static final long COMBO_EXPIRATION_TIME = 6000L;
    private static final int COMBO_REQUIRED_WORDS = 3;

    private static final Map<Integer, Integer> TIME_MAP = new HashMap<Integer, Integer>() {
        {
            // Format: word length, time in ms
            put(3, 1500);
            put(4, 3000);
            put(5, 6000);
            put(6, 9000);
            put(7, 14000);
            put(8, 20000);
            put(9, 25000);
            put(10, 30000);
        }
    };

    private final long startTime;
    private long endTime;
    private final ExpiringSet<String> comboSet = new ExpiringSet<>(new HashSet<>(),
            COMBO_EXPIRATION_TIME);

    public GameModeTime(Board board, long startTime) {
        super(board);
        this.startTime = startTime;
        this.endTime = startTime + BASE_GAME_DURATION;
    }

    @Override
    public GameModeType getGameModeType() {
        return GAME_MODE_TYPE;
    }

    @Override
    public long getGameDuration() {
        return BASE_GAME_DURATION;
    }

    @Override
    public int getMaxScore() {
        return ScoreUtils.calculateScoreForBoard(GameModeNormal.SCORE_MAP, gameBoard);
    }

    @Override
    public WordFoundReward getWordReward(String word) {
        final int score = Objects.requireNonNull(GameModeNormal.SCORE_MAP.get(word.length()));
        final int timeIncrease = Objects.requireNonNull(TIME_MAP.get(word.length()));
        comboSet.add(word);

        final int comboReward = getComboReward();
        final int soundId = getRewardSoundId(word);
        endTime += (timeIncrease + comboReward);

        if (comboReward > 0) {
            // Reset expiration time after a combo
            comboSet.resetExpiration();

            final RewardVisual rewardVisual = new RewardVisual(R.id.timerRewardText,
                    R.anim.time_reward_popup, getRewardSumText(comboReward));
            return new WordFoundReward(score, timeIncrease, soundId, rewardVisual);
        } else {
            return new WordFoundReward(score, timeIncrease, soundId);
        }
    }

    @Override
    public HighScoreData getHighScoreData() {
        HighScoreDataTimeChase highScoreData = new HighScoreDataTimeChase();
        highScoreData.setGameDuration(endTime - startTime);
        return highScoreData;
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

    private int getComboReward() {
        final int size = comboSet.size();

        // Combo rule: every n words yields a reward
        if (size < COMBO_REQUIRED_WORDS || size % COMBO_REQUIRED_WORDS != 0) {
            return 0;
        }

        final int wordSum = getRewardSum(comboSet.getWrappedSet());
        final int multiplier = size / COMBO_REQUIRED_WORDS;

        return wordSum * multiplier;
    }

    private int getRewardSum(Set<String> wordSet) {
        int sum = 0;
        for (String word : wordSet) {
            sum += Objects.requireNonNull(TIME_MAP.get(word.length()));
        }

        return sum / 3;
    }

    private String getRewardSumText(int comboReward) {
        return String.format(Locale.ENGLISH, "+ %d s", comboReward / 1000);
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