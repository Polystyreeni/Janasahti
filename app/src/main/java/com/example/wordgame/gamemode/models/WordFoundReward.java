package com.example.wordgame.gamemode.models;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Data class for word rewards. Used for passing data between game modes and game state.
 */
@Immutable
public class WordFoundReward {
    private final int scoreIncrement;
    private final int timeIncrement;
    private final int soundId;
    private final RewardVisual rewardVisual;

    public WordFoundReward(int scoreIncrement, int timeIncrement, int soundId) {
        this(scoreIncrement, timeIncrement, soundId, null);
    }

    public WordFoundReward(int scoreIncrement, int timeIncrement, int soundId,
                           @Nullable RewardVisual rewardVisual) {
        this.scoreIncrement = scoreIncrement;
        this.timeIncrement = timeIncrement;
        this.soundId = soundId;
        this.rewardVisual = rewardVisual;
    }

    public int getScoreIncrement() {
        return this.scoreIncrement;
    }

    public int getTimeIncrement() {
        return this.timeIncrement;
    }

    public int getSoundId() {
        return this.soundId;
    }

    @CheckForNull
    public RewardVisual getRewardVisual() {
        return this.rewardVisual;
    }
}
