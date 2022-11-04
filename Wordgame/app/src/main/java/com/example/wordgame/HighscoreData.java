package com.example.wordgame;

import java.util.ArrayList;

// A class representing user score from a single game
public class HighscoreData {
    private String userName;
    private String userId;
    private int score;
    private String bestWord;

    public HighscoreData(String userName, int score, String bestWord) {
        this.userName = userName;
        this.score = score;
        this.bestWord = bestWord;
    }

    public HighscoreData() {

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

    public void setUserName(String userName) {this.userName = userName;}
    public void setScore(int score) {this.score = score;}
    public void setBestWord(String bestWord) {this.bestWord = bestWord;}
    public void setUserId(String userId) {this.userId = userId;}
}
