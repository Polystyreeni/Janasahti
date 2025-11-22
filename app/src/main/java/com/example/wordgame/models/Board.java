package com.example.wordgame.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

/**
 * A class representing a single game board. Contains info on how to construct the board,
 * and all the words that can be found on the board
 */
@Immutable
public class Board {
    private final String boardString;
    private final Set<String> words;

    public Board(String boardString, List<String> words) {
        this.boardString = boardString;
        this.words = Collections.unmodifiableSet(new HashSet<>(words));
    }

    public String getBoardString() {
        return boardString;
    }

    public Set<String> getWords() {
        return words;
    }

    public int getDimension() {
        return (int) Math.sqrt(boardString.length());
    }

    public boolean containsWord(String word) {
        return words.contains(word);
    }
}
