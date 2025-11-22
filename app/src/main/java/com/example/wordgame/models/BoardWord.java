package com.example.wordgame.models;

import javax.annotation.concurrent.Immutable;

/**
 * Single word object of a game board. Includes the position on the game board.
 */
@Immutable
public class BoardWord {
    private final String word;
    private final int maxX, maxY;

    public BoardWord(String word, int maxX, int maxY) {
        this.word = word;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public String getWord() {
        return word;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }
}
