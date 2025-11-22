package com.example.wordgame.managers;

import android.content.Context;
import android.util.Log;

import com.example.wordgame.compat.IVersionMigratable;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.models.GameVersion;
import com.example.wordgame.models.UserStats;
import com.example.wordgame.utility.IoUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

public class UserStatsManager implements IVersionMigratable {
    private static final String STATS_FILE = "wg_userstats";
    private static final String TAG = "UserStatsManager";

    private final UserStats userStats;
    private boolean userStatsSaved = false;

    public UserStatsManager() {
        this.userStats = new UserStats();
    }

    public String getStat(UserStats.Stat stat) {
        return userStats.getStat(stat);
    }
    public String getFormattedStat(UserStats.Stat stat) {
        return userStats.getFormattedStat(stat);
    }
    public int getStatInt(UserStats.Stat stat) { return userStats.getStatInt(stat); }
    public long getStatLong(UserStats.Stat stat) { return userStats.getStatLong(stat); }
    public boolean getStatBoolean(UserStats.Stat stat) { return userStats.getStatBoolean(stat); }
    public float getStatFloat(UserStats.Stat stat) { return userStats.getStatFloat(stat); }
    public void setStat(UserStats.Stat stat, String value) {
        Log.d(TAG, "Set stat" + stat + " to value " + value);
        userStats.setStat(stat, value);
        userStatsSaved = false;
    }

    public void loadStats(Context ctx) {
        List<String> lines =  IoUtils.readLinesFromFile(ctx, STATS_FILE);
        List<String> failed = userStats.loadStats(lines);

        if (!failed.isEmpty()) {
            Log.d(TAG, "Settings file contained " + failed.size() + " invalid lines:");
            for (String line : lines) {
                Log.d(TAG, line);
            }
        }

        userStatsSaved = true;
    }

    public void saveStats(Context ctx) {
        // Stats have not been modified
        if (userStatsSaved) {
            return;
        }

        calculateAverages();

        final String stats = userStats.toString();
        try {
            IoUtils.writeFile(ctx, STATS_FILE, stats);
        } catch (IOException e) {
            Log.d(TAG, "Failed to write stats file", e);
        }

        userStatsSaved = true;
    }

    @Override
    public void migrateTo(Context ctx, @Nullable GameVersion from, GameVersion to) {
        // Pre version 2.0.0
        final GameVersion minCompatVersion = new GameVersion(1, 5, 0);
        final GameVersion maxCompatVersion = new GameVersion(1, 5, 1);
        if (from == null || (Objects.equals(minCompatVersion, from) && Objects.equals(maxCompatVersion, from))) {
            // TODO: Should remove the old file when done
            Logger.getInstance().info(TAG, "Transferring stats from old version to latest");
            final String oldFileName = "userstats";

            String line = IoUtils.readLineFromFile(ctx, oldFileName);
            String[] values = line.split("/");
            if (values.length == 13) {
                userStats.setStat(UserStats.Stat.SCORE_TOTAL_NORMAL, values[0]);
                userStats.setStat(UserStats.Stat.NUMBER_OF_GAMES_NORMAL, values[1]);
                userStats.setStat(UserStats.Stat.TOTAL_GAME_TIME, values[2]);
                userStats.setStat(UserStats.Stat.HIGHEST_SCORE_NORMAL, values[3]);
                userStats.setStat(UserStats.Stat.FIRST_PLACES_NORMAL, values[4]);
                userStats.setStat(UserStats.Stat.PERCENTAGE_NORMAL, values[5]);
                userStats.setStat(UserStats.Stat.LONGEST_WORD, values[6]);
                userStats.setStat(UserStats.Stat.AVERAGE_SCORE_NORMAL, values[7]);
                userStats.setStat(UserStats.Stat.SCORE_TOTAL_RATIONAL, values[8]);
                userStats.setStat(UserStats.Stat.NUMBER_OF_GAMES_RATIONAL, values[9]);
                userStats.setStat(UserStats.Stat.HIGHEST_SCORE_RATIONAL, values[10]);
                userStats.setStat(UserStats.Stat.FIRST_PLACES_RATIONAL, values[11]);
                userStats.setStat(UserStats.Stat.PERCENTAGE_RATIONAL, values[12]);
            }

            saveStats(ctx);
        }
    }

    private void calculateAverages() {
        final long scoreNormal = userStats.getStatLong(UserStats.Stat.SCORE_TOTAL_NORMAL);
        final long scoreRational = userStats.getStatLong(UserStats.Stat.SCORE_TOTAL_RATIONAL);
        final long scoreTime = userStats.getStatLong(UserStats.Stat.SCORE_TOTAL_TIME);
        final long scoreExtended = userStats.getStatLong(UserStats.Stat.SCORE_TOTAL_EXTENDED);

        final int gamesNormal = userStats.getStatInt(UserStats.Stat.NUMBER_OF_GAMES_NORMAL);
        final int gamesRational = userStats.getStatInt(UserStats.Stat.NUMBER_OF_GAMES_RATIONAL);
        final int gamesTime = userStats.getStatInt(UserStats.Stat.NUMBER_OF_GAMES_TIME);
        final int gamesExtended = userStats.getStatInt(UserStats.Stat.NUMBER_OF_GAMES_EXTENDED);

        final long avgNormal = gamesNormal > 0 ? scoreNormal / gamesNormal : 0;
        final long avgRational = gamesRational > 0 ? scoreRational / gamesRational : 0;
        final long avgTime = gamesTime > 0 ? scoreTime / gamesTime : 0;
        final long avgExtended = gamesExtended > 0 ? scoreExtended / gamesExtended : 0;

        userStats.setStat(UserStats.Stat.AVERAGE_SCORE_NORMAL, String.valueOf(avgNormal));
        userStats.setStat(UserStats.Stat.AVERAGE_SCORE_RATIONAL, String.valueOf(avgRational));
        userStats.setStat(UserStats.Stat.AVERAGE_SCORE_TIME, String.valueOf(avgTime));
        userStats.setStat(UserStats.Stat.AVERAGE_SCORE_EXTENDED, String.valueOf(avgExtended));
    }
}
