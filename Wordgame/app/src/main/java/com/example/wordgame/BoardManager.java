package com.example.wordgame;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

// A class responsible for reading game boards from an exterior source
public class BoardManager {

    private static final String TAG = "BoardManager";

    private static String latestError = "";
    private static Board nextBoard = null;
    private static boolean shouldGenerateBoard = false;
    private static int previousBoard = -1;
    private static final Random RANDGEN = new Random();
    private static final Queue<Board> BOARD_QUEUE = new LinkedList<>();
    private static final int BOARD_QUEUE_SIZE = 5;

    // Fetch a random game board from the provided XML file and set it as next board
    public static void generateBoard() {
        String boardListUrl;
        try {
            boardListUrl = NetworkConfig.getUrl("boardList");
        }
        catch (InvalidUrlRequestException e) {
            latestError = e.getMessage();
            nextBoard = null;
            return;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            URL listUrl = new URL(boardListUrl);

            InputStream listStream = listUrl.openConnection().getInputStream();

            String urlString = getBoardUrl(listStream);
            URL url = new URL(urlString);

            // Set parser input
            parser.setInput(url.openConnection().getInputStream(), "UTF-8");

            String boardString = "";
            int score = 0;
            ArrayList<String> words = new ArrayList<>();

            int maxCount = 0;
            List<Integer> boardIndices = new ArrayList<Integer>();
            int boardCount = 0;
            int boardCurrentIndex = 0;

            int event = parser.getEventType();
            while(event != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch(event) {
                    case XmlPullParser.START_TAG:
                        if(name.equals("count")) {
                            maxCount = Integer.parseInt(parser.nextText());
                            for(int i = 0; i < BOARD_QUEUE_SIZE; i++) {
                                int boardIndex = RANDGEN.nextInt(maxCount);
                                if(boardIndex == previousBoard) {
                                    boardIndex = RANDGEN.nextInt(maxCount);
                                }
                                boardIndices.add(boardIndex);
                            }

                            previousBoard = boardIndices.get(BOARD_QUEUE_SIZE - 1);
                            Collections.sort(boardIndices, Integer::compare);
                        }
                        else if(name.equals("board")) {
                            if(boardCount == boardIndices.get(boardCurrentIndex)) {
                                Log.d(TAG, "Found board with index:");
                            }
                            else {
                                boardCount++;
                            }
                        }

                        // Get tiles string for board
                        else if(name.equals("tiles")) {
                            if(boardCount == boardIndices.get(boardCurrentIndex)) {
                                boardString = parser.nextText();
                            }
                        }

                        // Get max score for board
                        else if(name.equals("score")) {
                            if(boardCount == boardIndices.get(boardCurrentIndex)) {
                                score = Integer.parseInt(parser.nextText());
                            }
                        }

                        // Add word to board
                        else if(name.equals("word")) {
                            if(boardCount == boardIndices.get(boardCurrentIndex)) {
                                String word = parser.nextText();
                                words.add(word);
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if(name.equals("board")) {
                            // Check if wanted board has been read entirely
                            if(boardCount == boardIndices.get(boardCurrentIndex)) {
                                // Append board to the queue
                                Board board = new Board(boardString, score, new ArrayList<>(words));
                                BOARD_QUEUE.add(board);
                                boardCurrentIndex++;
                                boardCount++;
                                words.clear();
                                if(boardCurrentIndex >= boardIndices.size())
                                    event = XmlPullParser.END_DOCUMENT;
                            }
                        }
                        break;
                }

                if(event != XmlPullParser.END_DOCUMENT) {
                    event = parser.next();
                }
            }

            setNextBoard();
        }

        catch (FileNotFoundException ex) {
            latestError = "Pelilautapalveluun ei saada yhteyttä!";
            ex.printStackTrace();
            nextBoard = null;
        }

        catch (Exception ex) {
            latestError = ex.getClass().getSimpleName();
            ex.printStackTrace();
            nextBoard = null;
        }
    }

    public static void setNextBoard() {
        nextBoard = BOARD_QUEUE.remove();
    }

    public static int getBoardQueueSize() {
        return BOARD_QUEUE.size();
    }

    public static void clearActiveBoard() {
        nextBoard = null;
    }

    public static Board getNextBoard() {
        return nextBoard;
    }

    public static String getLatestError() {return latestError;}

    public static void setShouldGenerateBoard(boolean value) {
        shouldGenerateBoard = value;
    }

    public static boolean getShouldGenerateBoard() {
        return shouldGenerateBoard;
    }

    public static void setRandomSeed(long seedNum) {
        RANDGEN.setSeed(seedNum);
    }

    private static String getBoardUrl(InputStream data) {
        List<String> links = new ArrayList<>();

        String line;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(data));
            while((line = reader.readLine()) != null) {
                links.add(line);
            }

            reader.close();
        }

        catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        int index = RANDGEN.nextInt(links.size());
        Log.d(TAG, "Using gameBoard file: " + index);
        return links.get(index);
    }
}
