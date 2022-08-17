package com.example.wordgame;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

// A class responsible for reading game boards from an exterior source
public class BoardManager {

    // Fetch game boards from following url
    private static final String urlString
            = "https://drive.google.com/uc?export=download&id=1Om7xFqP4fQR5Qk8hqdvrmudOTCcbk1-X";
    // Fetch game board count from a separate file
    private static final String countString =
            "https://drive.google.com/uc?export=download&id=14Eoqw3J8ILHcfhYo_IIPp67_1g_9MK6k";
    private static final String TAG = "BoardManager";

    private static String latestError = "";
    private static Board nextBoard = null;
    private static Random randGen = new Random();

    // Fetch a random game board from the provided XML file and set it as next board
    public static void generateBoard() {
        try {
            XmlPullParser parser = Xml.newPullParser();
            URL countUrl = new URL(countString);
            URL url = new URL(urlString);

            // Set parser input
            parser.setInput(url.openConnection().getInputStream(), "UTF-8");

            String boardString = "";
            int score = 0;
            ArrayList<String> words = new ArrayList<>();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(countUrl.openConnection().getInputStream()));
            int maxCount = Integer.parseInt(reader.readLine());

            int boardIndex = randGen.nextInt(maxCount);
            int boardCount = 0;

            int event = parser.getEventType();
            while(event != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch(event) {
                    case XmlPullParser.START_TAG:
                        if(name.equals("board")) {
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

        catch (Exception ex) {
            latestError = ex.getClass().getSimpleName();
            ex.printStackTrace();
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
}
