package com.example.wordgame.gamemode;

import com.example.wordgame.R;
import com.example.wordgame.audio.AudioHandler;
import com.example.wordgame.models.Board;
import com.example.wordgame.models.HighScoreData;
import com.example.wordgame.gamemode.models.WordFoundReward;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for game modes
 */
public abstract class GameMode {
    protected Board gameBoard;
    protected int maxScore;

    public GameMode(Board board) {
        this.gameBoard = board;
        this.maxScore = getMaxScore();
    }

    /**
     * Gets the duration of a single match
     * @return game duration
     */
    public long getGameDuration() {
        return 90000L;
    }

    /**
     * Time remaining where the game will start the end-game counter
     * @return warning time limit
     */
    public long getEndTimeLimit() { return 10000L; }

    /**
     * Compares two high score of this given game mode
     * @param h1 first score to compare
     * @param h2 second score to compare
     * @return positive int if h1 > h2, 0 if h1 = h2, negative if h1 < h2
     */
    public int compareHighScores(HighScoreData h1, HighScoreData h2) {
        return h1.getScore() - h2.getScore();
    }

    /**
     * Gets the metric which is used for sorting high score data on the leaderboards
     * @return string representation of the HighScoreData property
     */
    public String getSortMetric() {
        return "score";
    }

    /**
     * Returns if the given word is valid
     * @param word word to check
     * @return true if word is valid
     */
    public boolean isValidWord(String word) {
        return gameBoard.containsWord(word);
    }

    /**
     * Register sounds that should be present in this game mode. Game modes should override
     * this method
     * @return sound mappings with key = raw file id, value = sound pool index
     */
    public Map<Integer, Integer> registerSounds(AudioHandler audioHandler) {
        final Map<Integer, Integer> audioMap = new HashMap<>();
        audioMap.put(R.raw.tick, audioHandler.addSoundToPool(R.raw.tick, 1));
        audioMap.put(R.raw.snd_deny, audioHandler.addSoundToPool(R.raw.snd_deny, 1));
        audioMap.put(R.raw.snd_found_short, audioHandler.addSoundToPool(R.raw.snd_found_short, 1));

        return audioMap;
    }

    /**
     * Gets the type of the game mode
     * @return type of game mode
     */
    public abstract GameModeType getGameModeType();

    /**
     * The maximum score from the board in this game mode
     * @return max score
     */
    public abstract int getMaxScore();

    /**
     * Gets the award for the given word
     * @param word word to check
     * @return score score awarded from the given word
     */
    public abstract WordFoundReward getWordReward(String word);

    /**
     * Gets the high score data for the game mode.
     * @return high score data
     */
    public abstract HighScoreData getHighScoreData();
}
