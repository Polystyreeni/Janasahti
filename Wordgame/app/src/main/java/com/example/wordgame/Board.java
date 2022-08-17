package com.example.wordgame;

import java.util.ArrayList;

// A class representing a single game board. Contains info on how to construct the board,
// and all the words that can be found on the board
public class Board {
    private final String boardString;
    private final int maxScore;
    private ArrayList<String> words = new ArrayList<>();

    public Board(String boardString, int maxScore, ArrayList<String> words) {
        this.boardString = boardString;
        this.maxScore = maxScore;
        this.words = words;
    }

    public String getBoardString() {
        return boardString;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public ArrayList<String> getWords() {
        return words;
    }
}
