package com.example.wordgame.models;

import com.example.wordgame.utility.AppConstants;
import com.example.wordgame.utility.FirebaseUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Data class for storing high score data in Firebase
 */
public class HighScoreData {
    private String userId;
    private String userName;
    private int score;
    private String bestWord;
    private int foundWords;

    public HighScoreData() {
        // Empty constructor for Firebase
    }

    public String getUserName() {
        return userName;
    }

    public int getScore() {
        return score;
    }

    public String getBestWord() {
        return bestWord;
    }

    public String getUserId() { return userId; }

    public int getFoundWords() { return foundWords; }

    public void setUserName(String userName) { this.userName = userName; }
    public void setScore(int score) { this.score = score; }
    public void setBestWord(String bestWord) { this.bestWord = bestWord; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setFoundWords(int foundWords) { this.foundWords = foundWords; }

    @Nonnull
    @Override
    public String toString() {
        final String delim = AppConstants.INTENT_EXTRA_MESSAGE_DELIMITER;
        StringBuilder sb = new StringBuilder();
        sb.append(userId == null ? FirebaseUtils.UNINITIALIZED_USER_ID : userId).append(delim);
        sb.append(userName).append(delim);
        sb.append(score).append(delim);
        sb.append(bestWord).append(delim);
        sb.append(foundWords);
        return sb.toString();
    }

    @CheckForNull
    public static HighScoreData parseFromString(String highScoreStr) {
        HighScoreData highScoreData = new HighScoreData();
        final String delim = AppConstants.INTENT_EXTRA_MESSAGE_DELIMITER;
        final String[] parts = highScoreStr.split(delim);
        try {
            highScoreData.userId = parts[0];
            highScoreData.userName = parts[1];
            highScoreData.score = Integer.parseInt(parts[2]);
            highScoreData.bestWord = parts[3];
            highScoreData.foundWords = Integer.parseInt(parts[4]);
            return highScoreData;
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
