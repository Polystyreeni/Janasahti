package com.example.wordgame.managers;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.example.wordgame.activities.INetworkComponentInitListener;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.models.BoardDataWrapper;
import com.example.wordgame.models.BoardWord;
import com.example.wordgame.utility.IoUtils;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.CheckForNull;

/**
 * Base manager component that handles the creation of game boards.
 */
public abstract class BoardManager {
    private static final String TAG = "BoardManager";

    // Board file XML tags
    private static final String XML_TAG_BOARD = "board";
    private static final String XML_TAG_COUNT = "count";
    private static final String XML_TAG_TILES = "tiles";
    private static final String XML_TAG_WORD = "word";
    private static final String XML_ATTRIBUTE_WORD = "word";
    private static final String XML_ATTRIBUTE_X_MAX = "xMax";
    private static final String XML_ATTRIBUTE_Y_MAX = "yMax";

    // Application settings
    protected final Context appContext;
    protected final int boardQueueSize;
    private final Queue<BoardDataWrapper> boardQueue;

    protected Throwable latestError;
    protected BoardDataWrapper activeBoard;
    protected int previousBoard;
    protected final Random rng;

    public BoardManager(Context appContext, int boardQueueSize) {
        this.appContext = appContext;
        this.boardQueueSize = boardQueueSize;

        this.latestError = null;
        this.activeBoard = null;
        this.previousBoard = -1;
        this.rng = new Random();
        this.boardQueue = new LinkedList<>();
    }


    /**
     * Sets the next active board from the remaining queue.
     */
    public void dequeueBoard() {
        activeBoard = boardQueue.remove();
        Logger.getInstance().debug(TAG, "Next board: " + activeBoard.toString());
    }

    /**
     * Checks if there are boards remaining in the queue.
     * @return true if queue is not empty.
     */
    public boolean hasBoardsRemaining() {
        return boardQueue.peek() != null;
    }

    /**
     * Resets the active game board.
     */
    public void clearActiveBoard() {
        activeBoard = null;
    }

    /**
     * Gets the active game board.
     * @return active board, null if active board is not set.
     */
    @CheckForNull
    public BoardDataWrapper getActiveBoard() {
        return activeBoard;
    }

    /**
     * Gets the last error message.
     * @return last error message.
     */
    @CheckForNull
    public Throwable getLatestError() { return latestError; }

    /**
     * Initialized the board manager
     */
    public abstract void init(INetworkComponentInitListener listener);

    /**
     * Initializes the creation of game boards.
     */
    public abstract void requestBoards();

    /**
     * Checks if board manager is generating boards.
     * @return true if boards are being generated.
     */
    public abstract boolean isFetchingBoards();

    protected byte[] uncompressBoardFile(byte[] compressedBytes) {
        try (InputStream byteInputStream = new ByteArrayInputStream(compressedBytes);
             ZipInputStream zipInputStream = new ZipInputStream(byteInputStream)) {

            // This is needed so zipInputStream actually reads the first file contents
            ZipEntry ignored = zipInputStream.getNextEntry();

            return IoUtils.readAllBytes(zipInputStream);
        } catch (IOException e) {
            latestError = e;
            Log.w(TAG, "Failed to uncompress board files", e);
            return new byte[0];
        }
    }
    

    /**
     * Fetches a random game board from the provided XML input stream and sets next boards.
     * @param inputStream XML input stream
     */
    protected void generateBoards(InputStream inputStream) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, "UTF-8");

            String boardString = "";
            ArrayList<BoardWord> words = new ArrayList<>();

            int maxCount;
            List<Integer> boardIndices = new ArrayList<>();
            int boardCount = 0;
            int boardCurrentIndex = 0;

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:
                        switch (name) {
                            case XML_TAG_COUNT:
                                maxCount = Integer.parseInt(parser.nextText());
                                for (int i = 0; i < boardQueueSize; i++) {
                                    int boardIndex = rng.nextInt(maxCount);
                                    if (boardIndex == previousBoard) {
                                        boardIndex = rng.nextInt(maxCount);
                                    }
                                    boardIndices.add(boardIndex);
                                }

                                previousBoard = boardIndices.get(boardQueueSize - 1);
                                Collections.sort(boardIndices, Integer::compare);
                                break;
                            case XML_TAG_BOARD:
                                if (boardCount != boardIndices.get(boardCurrentIndex)) {
                                    boardCount++;
                                }
                                break;

                            // Get tiles string for board
                            case XML_TAG_TILES:
                                if (boardCount == boardIndices.get(boardCurrentIndex)) {
                                    boardString = parser.nextText();
                                }
                                break;

                            // Add word to board
                            case XML_TAG_WORD:
                                if (boardCount == boardIndices.get(boardCurrentIndex)) {
                                    final String word = parser.getAttributeValue(null, XML_ATTRIBUTE_WORD);
                                    final int xMax = Integer.parseInt(
                                            parser.getAttributeValue(null, XML_ATTRIBUTE_X_MAX));
                                    final int yMax = Integer.parseInt(
                                            parser.getAttributeValue(null, XML_ATTRIBUTE_Y_MAX));
                                    words.add(new BoardWord(word, xMax, yMax));
                                }
                                break;
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if (name.equals(XML_TAG_BOARD)) {
                            // Check if wanted board has been read entirely
                            if (boardCount == boardIndices.get(boardCurrentIndex)) {
                                // Append board to the queue
                                BoardDataWrapper boardWrapper = new BoardDataWrapper(boardString,
                                        new ArrayList<>(words));
                                boardQueue.add(boardWrapper);
                                boardCurrentIndex++;
                                boardCount++;
                                words.clear();
                                if (boardCurrentIndex >= boardIndices.size())
                                    event = XmlPullParser.END_DOCUMENT;
                            }
                        }
                        break;
                }

                if (event != XmlPullParser.END_DOCUMENT) {
                    event = parser.next();
                }
            }

            dequeueBoard();
        } catch (Exception e) {
            latestError = e;
            Logger.getInstance().warn(TAG, "Exception occurred when parsing board files", e);
            activeBoard = null;
        }
    }
}
