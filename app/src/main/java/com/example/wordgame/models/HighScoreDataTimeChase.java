package com.example.wordgame.models;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.wordgame.utility.AppConstants;

public class HighScoreDataTimeChase extends HighScoreData {
    private static final String TAG = "HighScoreDataTimeChase";
    private long gameDuration;

    public HighScoreDataTimeChase() {
        // Empty constructor for Firebase
    }

    public long getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(long gameDuration) {
        this.gameDuration = gameDuration;
    }

    @NonNull
    @Override
    public String toString() {
        final String delimiter = AppConstants.INTENT_EXTRA_MESSAGE_DELIMITER;
        String baseStr = super.toString();

        return baseStr + delimiter + gameDuration;
    }

    public static HighScoreDataTimeChase parseFromString(String highScoreStr) {
        HighScoreDataTimeChase highScoreData = new HighScoreDataTimeChase();
        final String delimiter = AppConstants.INTENT_EXTRA_MESSAGE_DELIMITER;
        final String[] parts = highScoreStr.split(delimiter);
        try {
            highScoreData.setUserId(parts[0]);
            highScoreData.setUserName(parts[1]);
            highScoreData.setScore(Integer.parseInt(parts[2]));
            highScoreData.setBestWord(parts[3]);
            highScoreData.setFoundWords(Integer.parseInt(parts[4]));
            highScoreData.setGameDuration(Long.parseLong(parts[5]));
            return highScoreData;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            Log.w(TAG, "Error parsing high score data", e);
            return null;
        }
    }
}
