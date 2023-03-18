package com.example.wordgame;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class UserStatsManager {

    private static final String STATS_FILE = "userstats";
    private static final String TAG = "UserStatsManager";

    public static UserStats Instance = null;
    public static boolean userStatsSaved = false;

    public static void initialze() {
        Instance = new UserStats();
        Log.d(TAG, "Initialized user stats");
    }

    public static void loadStats(Context ctx) {
        FileInputStream inputStream;
        try {
            inputStream = ctx.openFileInput(STATS_FILE);
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            String settingsStr = bufferedReader.readLine();
            String[] settings = settingsStr.split("/");

            Instance.setScoreTotal(Long.parseLong(getSetting(settings, 0)));
            Instance.setNumberOfGames(Integer.parseInt(getSetting(settings, 1)));
            Instance.setTotalGameTime(Integer.parseInt(getSetting(settings, 2)));
            Instance.setHighestScore(Integer.parseInt(getSetting(settings, 3)));
            Instance.setFirstPlaces(Integer.parseInt(getSetting(settings, 4)));
            Instance.setHighestPercentage(Float.parseFloat(getSetting(settings, 5)));
            Instance.setLongestWord(getSetting(settings, 6));
            Instance.setAverageScore();

            userStatsSaved = true;

            /*
            private long scoreTotal;
            private int numberOfGames;
            private long totalGameTime;
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
        if(userStatsSaved)
            return;

        FileOutputStream stream;
        try {
            stream = ctx.openFileOutput(STATS_FILE, Context.MODE_PRIVATE);

            String settings = String.valueOf(Instance.getScoreTotal())
                    + "/" + String.valueOf(Instance.getNumberOfGames())
                    + "/" + String.valueOf(Instance.getTotalGameTime())
                    + "/" + String.valueOf(Instance.getHighestScore())
                    + "/" + String.valueOf(Instance.getFirstPlaces())
                    + "/" + String.valueOf(Instance.getHighestPercentage())
                    + "/" + Instance.getLongestWord()
                    + "/" + String.valueOf(Instance.getAverageScore());

            stream.write(settings.getBytes(StandardCharsets.UTF_8));
            stream.close();

            userStatsSaved = true;
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String getSetting(String[] settings, int index) {
        try {
            return settings[index];
        }
        catch (Exception e) {
            return "0";
        }
    }
}
