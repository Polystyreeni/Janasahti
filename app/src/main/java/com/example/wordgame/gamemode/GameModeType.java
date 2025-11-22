package com.example.wordgame.gamemode;

/**
 * Game mode type data class.
 */
public enum GameModeType {
    NORMAL(4, 3, 10),
    RATIONAL(4, 3, 10),
    TIME_CHASE(4, 3, 10),
    EXTENDED(5, 3, 16);

    final int boardWidth;
    final int minWordLength;
    final int maxWordLength;
    GameModeType(int boardWidth, int minWordLength, int maxWordLength) {
        this.boardWidth = boardWidth;
        this.minWordLength = minWordLength;
        this.maxWordLength = maxWordLength;
    }

    public int getBoardWidth() {
        return this.boardWidth;
    }
    public int getMinWordLength() {
        return this.minWordLength;
    }
    public int getMaxWordLength() {
        return this.maxWordLength;
    }
}
