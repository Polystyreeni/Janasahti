package com.example.wordgame;

public class UserStats {
    private long scoreTotal;
    private int numberOfGames;
    private int highestScore;
    private int firstPlaces;
    private float highestPercentage;
    private String longestWord;

    public UserStats() {
        this.scoreTotal = 0;
        this.numberOfGames = 0;
        this.highestScore = 0;
        this.firstPlaces = 0;
        this.highestPercentage = 0;
        this.longestWord = "-";
    }

    public long getScoreTotal() {
        return this.scoreTotal;
    }

    public int getNumberOfGames() {
        return this.numberOfGames;
    }

    public int getHighestScore() {
        return this.highestScore;
    }

    public int getFirstPlaces() {
        return this.firstPlaces;
    }

    public float getHighestPercentage() {
        return this.highestPercentage;
    }

    public String getLongestWord() {
        return this.longestWord;
    }

    public void setScoreTotal(long scoreTotal) {
        this.scoreTotal = scoreTotal;
    }

    public void setNumberOfGames(int numberOfGames) {
        this.numberOfGames = numberOfGames;
    }

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    public void setFirstPlaces(int firstPlaces) {
        this.firstPlaces = firstPlaces;
    }

    public void setHighestPercentage(float highestPercentage) {
        this.highestPercentage = highestPercentage;
    }

    public void setLongestWord(String word) {
        this.longestWord = word;
    }
}