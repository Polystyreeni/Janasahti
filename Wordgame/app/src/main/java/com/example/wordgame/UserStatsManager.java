package com.example.wordgame;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class UserStatsManager {

    private static final String STATS_FILE = "userstats";

    public static UserStats Instance = null;

    public static void initialze() {
        Instance = new UserStats();
    }

    public static void loadStats(Context ctx) {
        FileInputStream inputStream;
        try {
            inputStream = ctx.openFileInput(STATS_FILE);
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            String settingsStr = bufferedReader.readLine();
            String[] settings = settingsStr.split("/");

            Instance.setScoreTotal(Long.parseLong(settings[0]));
            Instance.setNumberOfGames(Integer.parseInt(settings[1]));
            Instance.setHighestScore(Integer.parseInt(settings[2]));
            Instance.setFirstPlaces(Integer.parseInt(settings[3]));
            Instance.setHighestPercentage(Float.parseFloat(settings[4]));
            Instance.setLongestWord(settings[5]);

            /*
            private long scoreTotal;
            private int numberOfGames;
            private int highestScore;
            private int firstPlaces;
            private double highestPercentage;
             */
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void saveStats(Context ctx) {
        FileOutputStream stream;
        try {
            stream = ctx.openFileOutput(STATS_FILE, Context.MODE_PRIVATE);

            String settings = String.valueOf(Instance.getScoreTotal())
                    + "/" + String.valueOf(Instance.getNumberOfGames())
                    + "/" + String.valueOf(Instance.getHighestScore())
                    + "/" + String.valueOf(Instance.getFirstPlaces())
                    + "/" + String.valueOf(Instance.getHighestPercentage())
                    + "/" + Instance.getLongestWord();

            stream.write(settings.getBytes(StandardCharsets.UTF_8));
            stream.close();
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
