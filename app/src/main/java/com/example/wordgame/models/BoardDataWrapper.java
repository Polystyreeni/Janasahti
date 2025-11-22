package com.example.wordgame.models;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * A wrapper class that will create a Board based on the given game settings.
 */
@Immutable
public class BoardDataWrapper {
    private final String boardString;
    private final List<BoardWord> boardWords;

    public BoardDataWrapper(String boardString, List<BoardWord> boardWords) {
        this.boardString = boardString;
        this.boardWords = Collections.unmodifiableList(boardWords);
    }

    @Nonnull
    public Board getBoard(int boardWidth, int minWordLength, int maxWordLength) {
        final int widthSq = boardWidth * boardWidth;
        Preconditions.checkState(widthSq <= boardString.length(),
                "Requested board width is too big!");
        Preconditions.checkState(widthSq > 1,
                "Requested board width is too small!");

        final int widthOffset = (int) (Math.sqrt(boardString.length())) - boardWidth;
        if (widthOffset <= 0) {
            return new Board(boardString, getAllWords(minWordLength, maxWordLength));
        } else {
            return new Board(getSizeAdjustedBoardString(boardWidth),
                    getFilteredWords(boardWidth, minWordLength, maxWordLength));
        }
    }

    private String getSizeAdjustedBoardString(int width) {
        final int stringLength = (int) (Math.sqrt(boardString.length()));
        final int offset = stringLength - width; // Always > 0
        final int tilesToCheck = boardString.length() - stringLength;

        List<Integer> offsets = new ArrayList<>(offset);
        for (int i = 0; i < offset; i++) {
            offsets.add(width + i + 1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= tilesToCheck; i++) {
            boolean skip = false;
            for (int off : offsets) {
                if (i % off == 0) {
                    skip = true;
                    break;
                }
            }

            if (!skip) {
                sb.append(boardString.charAt(i - 1));
            }
        }

        return sb.toString();
    }

    private List<String> getFilteredWords(int width, int minWordLength, int maxWordLength) {
        List<String> words = new ArrayList<>();
        for (BoardWord word : boardWords) {
            if (word.getWord().length() < minWordLength || word.getWord().length() > maxWordLength) {
                continue;
            }
            if (word.getMaxX() < width && word.getMaxY() < width) {
                words.add(word.getWord());
            }
        }

        return words;
    }

    private List<String> getAllWords(int minWordLength, int maxWordLength) {
        List<String> words = new ArrayList<>(boardWords.size());
        for (BoardWord word : boardWords) {
            if (word.getWord().length() < minWordLength || word.getWord().length() > maxWordLength) {
                continue;
            }
            words.add(word.getWord());
        }

        return words;
    }

    @Nonnull
    @Override
    public String toString() {
        return boardString;
    }
}
