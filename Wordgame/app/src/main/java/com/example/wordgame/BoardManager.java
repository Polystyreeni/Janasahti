package com.example.wordgame;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// A class responsible for reading game boards from an exterior source
public class BoardManager {

    private static final String boardListUrl = "https://drive.google.com/uc?export=download&id=1OdjRb5bXxBlwwOnD6qzp_eh2o3Q4F23a";
    private static final String TAG = "BoardManager";

    private static String latestError = "";
    private static Board nextBoard = null;
    private static Random randGen = new Random();
    private static boolean shouldGenerateBoard = false;
    private static int previousBoard = -1;

    // Fetch a random game board from the provided XML file and set it as next board
    public static void generateBoard() {
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
            int boardIndex = 0;
            int boardCount = 0;

            int event = parser.getEventType();
            while(event != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch(event) {
                    case XmlPullParser.START_TAG:
                        if(name.equals("count")) {
                            maxCount = Integer.parseInt(parser.nextText());
                            boardIndex = randGen.nextInt(maxCount);
                            if(boardIndex == previousBoard) {
                                boardIndex = randGen.nextInt(maxCount);
                            }

                            previousBoard = boardIndex;
                        }
                        else if(name.equals("board")) {
                            if(boardCount >= boardIndex) {
                                Log.d(TAG, "Found board with index: " + boardIndex);
                            }
                            else {
                                boardCount++;
                            }
                        }

                        // Get tiles string for board
                        else if(name.equals("tiles")) {
                            if(boardCount >= boardIndex) {
                                boardString = parser.nextText();
                                Log.d(TAG, "Tiles: " + boardString);
                            }
                        }

                        // Get max score for board
                        else if(name.equals("score")) {
                            if(boardCount >= boardIndex) {
                                score = Integer.parseInt(parser.nextText());
                                Log.d(TAG, "Score: " + score);
                            }
                        }

                        // Add word to board
                        else if(name.equals("word")) {
                            if(boardCount >= boardIndex) {
                                words.add(parser.nextText());
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if(name.equals("board")) {
                            // Check if wanted board has been read entirely
                            if(boardCount >= boardIndex)
                                event = XmlPullParser.END_DOCUMENT;
                        }
                        break;
                }

                if(event != XmlPullParser.END_DOCUMENT) {
                    event = parser.next();
                }
            }

            Board board = new Board(boardString, score, words);
            setNextBoard(board);
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

    private static void setNextBoard(Board board) {
        nextBoard = board;
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
        randGen.setSeed(seedNum);
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

        int index = randGen.nextInt(links.size());
        Log.d(TAG, "Using gameBoard file: " + index);
        return links.get(index);
    }
}
