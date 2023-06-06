package com.example.wordgame;

public class UserStats {
    private long scoreTotal;
    private int numberOfGames;
    private long totalGameTime;
    private int highestScore;
    private int averageScore;
    private int firstPlaces;
    private float highestPercentage;
    private String longestWord;

    // Rational
    private long scoreTotalRational;
    private int numberOfGamesRational;
    private int highestScoreRational;
    private int averageScoreRational;
    private int firstPlacesRational;
    private float highestPercentageRational;

    public UserStats() {
        this.scoreTotal = 0;
        this.numberOfGames = 0;
        this.totalGameTime = 0;
        this.highestScore = 0;
        this.averageScore = 0;
        this.firstPlaces = 0;
        this.highestPercentage = 0;
        this.longestWord = "-";
        this.scoreTotalRational = 0;
        this.numberOfGamesRational = 0;
        this.highestScoreRational = 0;
        this.averageScoreRational = 0;
        this.firstPlacesRational = 0;
        this.highestPercentageRational = 0;
    }

    public long getScoreTotal() {
        return this.scoreTotal;
    }

    public int getNumberOfGames() {
        return this.numberOfGames;
    }

    public long getTotalGameTime() {
        return this.totalGameTime;
    }

    public int getHighestScore() {
        return this.highestScore;
    }

    public int getAverageScore() {return this.averageScore;}

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
    public void setTotalGameTime(long gameTime) {this.totalGameTime = gameTime;}

    public void setHighestScore(int highestScore) {
        this.highestScore = highestScore;
    }

    public void setAverageScore() {this.averageScore = (int)(scoreTotal / numberOfGames);}

    public void setFirstPlaces(int firstPlaces) {
        this.firstPlaces = firstPlaces;
    }

    public void setHighestPercentage(float highestPercentage) {
        this.highestPercentage = highestPercentage;
    }

    public void setLongestWord(String word) {
        this.longestWord = word;
    }

    public long getScoreTotalRational() {
        return scoreTotalRational;
    }

    public void setScoreTotalRational(long scoreTotalRational) {
        this.scoreTotalRational = scoreTotalRational;
    }

    public int getNumberOfGamesRational() {
        return numberOfGamesRational;
    }

    public void setNumberOfGamesRational(int numberOfGamesRational) {
        this.numberOfGamesRational = numberOfGamesRational;
    }

    public int getHighestScoreRational() {
        return highestScoreRational;
    }

    public void setHighestScoreRational(int highestScoreRational) {
        this.highestScoreRational = highestScoreRational;
    }

    public int getAverageScoreRational() {
        return averageScoreRational;
    }

    public void setAverageScoreRational() {
        if (numberOfGamesRational <= 0) this.averageScoreRational = 0;
        else this.averageScoreRational = (int)(this.scoreTotalRational / this.numberOfGamesRational);
    }

    public int getFirstPlacesRational() {
        return firstPlacesRational;
    }

    public void setFirstPlacesRational(int firstPlacesRational) {
        this.firstPlacesRational = firstPlacesRational;
    }

    public float getHighestPercentageRational() {
        return highestPercentageRational;
    }

    public void setHighestPercentageRational(float highestPercentageRational) {
        this.highestPercentageRational = highestPercentageRational;
    }
}
