package com.example.wordgame;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class WordSolver {

    public static int[] getTiles(String word, String boardString) {
        HashSet<Integer> tileIndexes = new HashSet<>();

        String firstLetter = word.substring(0, 1);

        List<Tile> tiles = new ArrayList<>();
        List<Tile> activeTiles = new ArrayList<>();
        List<Integer> startPositions = new ArrayList<>();
        for(int i = 0; i < boardString.length(); i++) {
            Tile tile = new Tile(boardString.substring(i, i + 1), i % 4, i / 4);
            tiles.add(tile);
            if(tile.getLetter().equals(firstLetter)) {
                startPositions.add(i);
            }
        }

        for(int i = 0; i < startPositions.size(); i++) {
            tileIndexes.add(startPositions.get(i));
            walkTile(startPositions.get(i), tiles, activeTiles, word, boardString, tileIndexes);
            if(tileIndexes.size() < word.length()) {
                for(Tile tile : tiles) {
                    tile.setOccupied(false);
                }
                tileIndexes.clear();
            }
            else {
                break;
            }
        }

        int[] tileIndexArray = new int[word.length()];
        int counter = 0;
        Log.d("WordSolver", "Tile indexes size: " + tileIndexes.size() + " word len: " + word.length());
        if(tileIndexes.size() > word.length()) {
            DebugPrint(tileIndexes, boardString);
            return new int[1];
        }
        for(Integer value : tileIndexes) {
            tileIndexArray[counter] = value;
            counter++;
        }

        DebugPrint(tileIndexArray, boardString);

        return tileIndexArray;
    }

    private static void DebugPrint(int[] arr, String boardString) {
        StringBuilder sb = new StringBuilder();
        for(int val : arr) {
            sb.append(boardString.charAt(val));
        }

        Log.d("WordSolver", "Solved word: " + sb.toString() + " from boardString " + boardString);
    }

    private static void DebugPrint(HashSet<Integer> set, String boardString) {
        StringBuilder sb = new StringBuilder();
        for(int val : set) {
            sb.append(boardString.charAt(val));
        }

        Log.d("WordSolver", "Solved word: " + sb.toString() + " from boardString " + boardString);
    }

    private static void walkTile(int boardPos, List<Tile> tiles, List<Tile> activeTiles, String word, String boardString, HashSet<Integer> tileIndexes) {
        if(tileIndexes.size() >= word.length())
            return;

        tiles.get(boardPos).setOccupied(true);

        List<Integer> neighbours = getValidNeighbourTiles(boardPos, tileIndexes.size(), word, boardString, tiles);
        for(Integer position : neighbours) {
            tileIndexes.add(position);
            walkTile(position, tiles, activeTiles, word, boardString, tileIndexes);
            if(tileIndexes.size() < word.length() || tileIndexes.size() > word.length()) {
                tileIndexes.remove(position);
                tiles.get(boardPos).setOccupied(false);
            }
        }
        tiles.get(boardPos).setOccupied(false);
    }

    private static List<Integer> getValidNeighbourTiles(int boardPos, int wordPos, String word, String boardString, List<Tile> tiles) {
        List<Integer> validNeighbours = new ArrayList<>();
        int[] neighbourIndexes = {boardPos - 1, boardPos + 1, boardPos - 4, boardPos + 4,
                boardPos - 4 - 1, boardPos - 4 + 1, boardPos + 4 - 1, boardPos + 4 + 1};

        for(int i = 0; i < neighbourIndexes.length; i++) {
            if(neighbourIndexes[i] >= 0 && neighbourIndexes[i] < boardString.length()
                    && Math.abs(tiles.get(boardPos).getX() - tiles.get(neighbourIndexes[i]).getX()) <= 1
                    && Math.abs(tiles.get(boardPos).getY() - tiles.get(neighbourIndexes[i]).getY()) <= 1) {
                if(tiles.get(neighbourIndexes[i]).getLetter().equals(word.substring(wordPos, wordPos + 1)) ) {
                    if(!tiles.get(neighbourIndexes[i]).isOccupied()) {
                        validNeighbours.add(neighbourIndexes[i]);
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
