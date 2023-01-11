package com.example.wordgame;

import java.util.HashMap;

// A class containing game specific constats and game rules
public class GameSettings {
    private static final long gameDuration = 20000; // Time in ms of one game

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

    // User settings
    private static int darkModeEnabled = 0;
    private static int oledProtectionEnabled = 0;

    // Debug settings
    private static final boolean useFirebase = false;

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

    public static int getDarkModeEnabled() { return darkModeEnabled; }
    public static int getOledProtectionEnabled() {return oledProtectionEnabled;}

    public static void setDarkModeEnabled(int value) {darkModeEnabled = value;}
    public static void setOledProtectionEnabled(int value) {oledProtectionEnabled = value;}

    public static boolean UseFirebase() { return useFirebase; }
}
