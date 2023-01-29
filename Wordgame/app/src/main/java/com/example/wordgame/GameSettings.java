package com.example.wordgame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// A class containing game specific constats and game rules
public class GameSettings {
    private static final long gameDuration = 90000; // Time in ms of one game (default 90000)

    // How much score is granted from given length of word
    private static final HashMap<Integer, Integer> scoreTable = new HashMap<Integer, Integer>() {
        {
            put(3, 1);
            put(4, 3);
            put(5, 7);
            put(6, 12);   // Non-confirmed
            put(7, 21);   // Non-confirmed
            put(8, 31);
            put(9, 42);   // Non-confirmed
            put(10, 57);
        }
    };

    private static final long scoreBoardDuration = 12000;   // Time in ms of how long scoreboards are shown
    private static final int scoreBoardMaxCount = 100;      // How many score board entries are shown

    private static final int usernameMaxLength = 30;        // How long the username is allowed to be

    // Debug settings
    private static final boolean useFirebase = true;

    public static long getGameDuration() {
        return gameDuration;
    }

    public static int getScoreForLength(int len) {
        return scoreTable.get(len);
    }

    public static long getScoreBoardDuration() { return scoreBoardDuration; }

    public static int getScoreBoardMaxCount() {
        return scoreBoardMaxCount;
    }
    public static int getUsernameMaxLength() { return usernameMaxLength; }

    public static boolean UseFirebase() { return useFirebase; }
}
