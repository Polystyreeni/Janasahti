package com.example.wordgame.models;

import com.example.wordgame.R;
import com.example.wordgame.gamemode.GameMode;
import com.example.wordgame.gamemode.GameModeExtended;
import com.example.wordgame.gamemode.GameModeNormal;
import com.example.wordgame.gamemode.GameModeRational;
import com.example.wordgame.gamemode.GameModeTime;
import com.example.wordgame.gamemode.GameModeType;
import com.example.wordgame.gamemode.models.WordFoundResponse;
import com.example.wordgame.gamemode.models.WordFoundReward;
import com.example.wordgame.gamemode.models.WordType;

import java.util.HashSet;
import java.util.Set;

public class GameState {
    private static final WordFoundResponse INVALID_RESPONSE = new WordFoundResponse(
            WordType.INVALID, R.raw.snd_deny);
    private static final WordFoundResponse VALID_OLD_RESPONSE = new WordFoundResponse(
            WordType.VALID_OLD, R.raw.snd_deny);
    private final GameMode gameMode;
    private final Set<String> foundWords;
    private final long startTime;
    private final int maxScore;
    private long endTime;
    private int currentScore;

    public GameState(GameModeType gameModeType, Board board) {
        this.gameMode = initGameMode(gameModeType, board);
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + this.gameMode.getGameDuration();
        this.foundWords = new HashSet<>();
        this.maxScore = this.gameMode.getMaxScore();
    }

    public GameMode getGameMode() {
        return this.gameMode;
    }

    public WordFoundResponse onWordOffered(String word) {
        if (foundWords.contains(word)) {
            return VALID_OLD_RESPONSE;
        }

        if (gameMode.isValidWord(word)) {
            foundWords.add(word);
            WordFoundReward reward = gameMode.getWordReward(word);
            currentScore += reward.getScoreIncrement();
            endTime += reward.getTimeIncrement();
            return new WordFoundResponse(WordType.VALID_NEW, reward.getSoundId(),
                    reward.getRewardVisual());
        } else {
            return INVALID_RESPONSE;
        }
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getEndTime() { return this.endTime; }

    public int getCurrentScore() {
        return this.currentScore;
    }

    public int getMaxScore() { return this.maxScore; }

    public HighScoreData getScoreData() {
        HighScoreData baseScore = gameMode.getHighScoreData();
        baseScore.setFoundWords(foundWords.size());
        baseScore.setBestWord(getBestWord());
        baseScore.setScore(currentScore);
        return baseScore;
    }

    public String getBestWord() {
        String bestWord = "-";  // Default best word
        for (String word : foundWords) {
            if (word.length() > bestWord.length()) {
                bestWord = word;
            }
        }

        return bestWord;
    }

    public boolean hasFoundWord(String word) {
        return foundWords.contains(word);
    }

    public static GameMode initGameMode(GameModeType gameModeType, Board board) {
        return initGameMode(gameModeType, board, System.currentTimeMillis());
    }

    public static GameMode initGameMode(GameModeType gameModeType, Board board, long startTime) {
        switch (gameModeType) {
            case NORMAL:
                return new GameModeNormal(board);
            case RATIONAL:
                return new GameModeRational(board);
            case TIME_CHASE:
                return new GameModeTime(board, startTime);
            case EXTENDED:
                return new GameModeExtended(board);
            default:
                throw new IllegalArgumentException("Not a valid game mode: " + gameModeType);
        }
    }
}