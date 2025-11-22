package com.example.wordgame.gamemode.models;

import javax.annotation.concurrent.Immutable;

/**
 * Response data from inputting a word.
 */

@Immutable
public class WordFoundResponse {
    private final WordType wordType;
    private final int soundId;
    private final RewardVisual rewardVisual;

    public WordFoundResponse(WordType wordType, int soundId) {
        this(wordType, soundId, null);
    }

    public WordFoundResponse(WordType wordType, int soundId, RewardVisual rewardVisual) {
        this.wordType = wordType;
        this.soundId = soundId;
        this.rewardVisual = rewardVisual;
    }

    public WordType getWordType() {
        return this.wordType;
    }

    public int getSoundId() {
        return this.soundId;
    }

    public RewardVisual getRewardVisual() {
        return this.rewardVisual;
    }
}