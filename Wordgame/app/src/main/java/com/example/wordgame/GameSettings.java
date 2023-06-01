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

    private static final String[] gameModes = new String[]{"classic", "rational"};

    private static final HashMap<String, String> gameModeTable = new HashMap<String, String>() {
        {
            put(gameModes[0], "Klassikko");
            put(gameModes[1], "Rationaalinen");
        }
    };

    private static final HashMap<String, String> gameModeDescriptions = new HashMap<String, String>() {
        {
            put(gameModes[0], "Pidemmät sanat tuottavat enemmän pisteitä");
            put(gameModes[1], "Kaikki sanat ovat saman arvoisia, pistemäärä perustuu löydettyjen sanojen lukumäärään");
        }
    };

    // Debug settings
    private static boolean useFirebase = true;

    public static long getGameDuration() {
        return gameDuration;
    }

    public static int getScoreForLength(int len) {
        if (UserSettings.getActiveGameMode().equals("rational"))
            return 1;
        return scoreTable.get(len);
    }

    public static long getScoreBoardDuration() { return scoreBoardDuration; }

    public static int getScoreBoardMaxCount() {
        return scoreBoardMaxCount;
    }
    public static int getUsernameMaxLength() { return usernameMaxLength; }

    public static boolean UseFirebase() { return useFirebase; }
    public static void SetUseFirebase(boolean value) {useFirebase = value;}

    public static String[] getGameModes() { return gameModes; }
    public static String getGameModeDescription(String id) {return gameModeDescriptions.get(id);}
    public static String getGameModeName(String id) {return gameModeTable.get(id);}
}
