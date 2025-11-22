package com.example.wordgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class WordSolver {
    private final String boardString;
    private final int dimension;
    private final List<Tile> tiles;
    public WordSolver(String boardString) {
        this.boardString = boardString;
        this.dimension = (int) (Math.sqrt(boardString.length()));
        this.tiles = Collections.unmodifiableList(createTiles());
    }

    public int[] findWord(String word) {
        HashSet<Integer> tileIndices = new HashSet<>();

        final String firstLetter = word.substring(0, 1);
        final List<Integer> startPositions = getStartPositions(firstLetter);

        for (int i = 0; i < startPositions.size(); i++) {
            tileIndices.add(startPositions.get(i));
            walkTile(startPositions.get(i), tiles, word, boardString, tileIndices);
            if (tileIndices.size() < word.length()) {
                for (Tile tile : tiles) {
                    tile.setOccupied(false);
                }
                tileIndices.clear();
            } else {
                break;
            }
        }

        int[] tileIndexArray = new int[word.length()];
        int counter = 0;
        if (tileIndices.size() > word.length()) {
            return new int[1];
        }
        for (Integer value : tileIndices) {
            tileIndexArray[counter] = value;
            counter++;
        }

        return tileIndexArray;
    }

    private List<Tile> createTiles() {
        List<Tile> tiles = new ArrayList<>(boardString.length());
        for (int i = 0; i < boardString.length(); i++) {
            Tile tile = new Tile(boardString.substring(i, i+ 1), i % dimension, i / dimension);
            tiles.add(tile);
        }

        return tiles;
    }

    private List<Integer> getStartPositions(String firstLetter) {
        List<Integer> firstLetterIndices = new ArrayList<>();
        final String[] characters = boardString.split("(?!^)");
        for (int i = 0; i < characters.length; i++) {
            if (characters[i].equals(firstLetter)) {
                firstLetterIndices.add(i);
            }
        }

        return firstLetterIndices;
    }

    private void walkTile(int boardPos, List<Tile> tiles, String word, String boardString, HashSet<Integer> tileIndexes) {
        if (tileIndexes.size() >= word.length())
            return;

        tiles.get(boardPos).setOccupied(true);

        List<Integer> neighbours = getValidNeighbourTiles(boardPos, tileIndexes.size(), word, boardString, tiles);
        for(Integer position : neighbours) {
            tileIndexes.add(position);
            walkTile(position, tiles, word, boardString, tileIndexes);
            if (tileIndexes.size() < word.length() || tileIndexes.size() > word.length()) {
                tileIndexes.remove(position);
                tiles.get(boardPos).setOccupied(false);
            }
        }
        tiles.get(boardPos).setOccupied(false);
    }

    private List<Integer> getValidNeighbourTiles(int boardPos, int wordPos, String word, String boardString, List<Tile> tiles) {
        List<Integer> validNeighbours = new ArrayList<>();
        int[] neighbourIndexes = { boardPos - 1, boardPos + 1, boardPos - dimension,
                boardPos + dimension, boardPos - dimension - 1, boardPos - dimension + 1,
                boardPos + dimension - 1, boardPos + dimension + 1 };

        for (int neighbourIndex : neighbourIndexes) {
            if (neighbourIndex >= 0 && neighbourIndex < boardString.length()
                    && Math.abs(tiles.get(boardPos).getX() - tiles.get(neighbourIndex).getX()) <= 1
                    && Math.abs(tiles.get(boardPos).getY() - tiles.get(neighbourIndex).getY()) <= 1) {
                if (tiles.get(neighbourIndex).getLetter().equals(word.substring(wordPos, wordPos + 1))) {
                    if (!tiles.get(neighbourIndex).isOccupied()) {
                        validNeighbours.add(neighbourIndex);
                    }
                }
            }
        }
        return validNeighbours;
    }

    private static class Tile {
        private final String letter;
        private boolean occupied = false;
        private final int x;
        private final int y;
        public Tile(String letter, int x, int y) {
            this.letter = letter;
            this.x = x;
            this.y = y;
        }

        public void setOccupied(boolean value) {
            this.occupied = value;
        }

        public boolean isOccupied() {
            return this.occupied;
        }

        public String getLetter() {
            return this.letter;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }
    }
}
