package com.example.wordgame;

public class ScoreUtils {
    public static int getMaximumBoardScore(Board board) {
        return UserSettings.getActiveGameMode().equals("rational") ? board.getWords().size() : board.getMaxScore();
    }

    public static int getHighScore(HighscoreData data) {
        return UserSettings.getActiveGameMode().equals("rational") ? data.getFoundWords() : data.getScore();
    }

    public static String getScoreSortMetric() {
        return UserSettings.getActiveGameMode().equals("rational") ? "foundWords" : "score";
    }
}
