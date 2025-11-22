package com.example.wordgame.models;

import androidx.annotation.NonNull;

import com.example.wordgame.R;
import com.example.wordgame.utility.TextUtils;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.CheckForNull;

/**
 * Data class for storing user stats. Supports the following value types:
 * - String
 * - Integer/Long
 * - Float
 * - Boolean
 */
public class UserStats {
    public enum StatType {
        STRING,
        INTEGER,
        LONG,
        FLOAT,
        BOOLEAN
    }

    public enum Stat {
        TOTAL_GAME_TIME(StatType.LONG, R.string.stats_common_game_time),
        LONGEST_WORD(StatType.STRING, R.string.stats_common_longest_word),

        SCORE_TOTAL_NORMAL(StatType.LONG, R.string.stats_common_score_total),
        NUMBER_OF_GAMES_NORMAL(StatType.INTEGER, R.string.stats_common_games_played),
        HIGHEST_SCORE_NORMAL(StatType.INTEGER, R.string.stats_common_highest_score),
        AVERAGE_SCORE_NORMAL(StatType.INTEGER, R.string.stats_common_avg_score),
        FIRST_PLACES_NORMAL(StatType.INTEGER, R.string.stats_common_first_places),
        PERCENTAGE_NORMAL(StatType.FLOAT, R.string.stats_common_highest_percentage),

        SCORE_TOTAL_RATIONAL(StatType.LONG, R.string.stats_rational_score_total),
        NUMBER_OF_GAMES_RATIONAL(StatType.INTEGER, R.string.stats_common_games_played),
        HIGHEST_SCORE_RATIONAL(StatType.INTEGER, R.string.stats_rational_highest_score),
        AVERAGE_SCORE_RATIONAL(StatType.INTEGER, R.string.stats_common_avg_score),
        FIRST_PLACES_RATIONAL(StatType.INTEGER, R.string.stats_common_first_places),
        PERCENTAGE_RATIONAL(StatType.FLOAT, R.string.stats_common_highest_percentage),

        SCORE_TOTAL_TIME(StatType.LONG, R.string.stats_common_score_total),
        NUMBER_OF_GAMES_TIME(StatType.INTEGER, R.string.stats_common_games_played),
        LONGEST_GAME_TIME(StatType.INTEGER, R.string.stats_time_chase_highest_playtime),
        HIGHEST_SCORE_TIME(StatType.INTEGER, R.string.stats_common_highest_score),
        AVERAGE_SCORE_TIME(StatType.INTEGER, R.string.stats_common_avg_score),
        FIRST_PLACES_TIME(StatType.INTEGER, R.string.stats_common_first_places),
        PERCENTAGE_TIME(StatType.FLOAT, R.string.stats_common_highest_percentage),

        SCORE_TOTAL_EXTENDED(StatType.LONG, R.string.stats_common_score_total),
        NUMBER_OF_GAMES_EXTENDED(StatType.INTEGER, R.string.stats_common_games_played),
        HIGHEST_SCORE_EXTENDED(StatType.INTEGER, R.string.stats_common_highest_score),
        AVERAGE_SCORE_EXTENDED(StatType.INTEGER, R.string.stats_common_avg_score),
        FIRST_PLACES_EXTENDED(StatType.INTEGER, R.string.stats_common_first_places),
        PERCENTAGE_EXTENDED(StatType.FLOAT, R.string.stats_common_highest_percentage);

        private final StatType statType;
        private final int descriptionId;
        Stat(StatType type, int descriptionId) {
            this.statType = type;
            this.descriptionId = descriptionId;
        }

        public StatType getStatType() {
            return this.statType;
        }
        public int getDescriptionId() {
            return this.descriptionId;
        }
    }

    /** First stats that define a category of stats. The given header should be added above
     *  when displaying in UI
     */
    public static final Map<Stat, Integer> DELIMITER_STATS =
            Collections.unmodifiableMap(new EnumMap<Stat, Integer>(Stat.class) {
        {
            put(Stat.TOTAL_GAME_TIME, R.string.stats_common_header);
            put(Stat.SCORE_TOTAL_NORMAL, R.string.stats_normal_header);
            put(Stat.SCORE_TOTAL_RATIONAL, R.string.stats_rational_header);
            put(Stat.SCORE_TOTAL_TIME, R.string.stats_time_chase_header);
            put(Stat.SCORE_TOTAL_EXTENDED, R.string.stats_extended_header);
        }
    });

    public static final String STATS_DELIMITER = "=";
    private final Map<Stat, String> stats;

    public UserStats() {
        this.stats = initStats();
    }

    /**
     * Loads stats from the given list of index=value pairs
     * @param statsLines lines to read stats from
     * @return list of invalid lines
     */
    public List<String> loadStats(List<String> statsLines) {
        List<String> erroneousLines = new ArrayList<>();
        for (String line : statsLines) {
            String[] parts = line.split(STATS_DELIMITER);
            if (parts.length == 2) {
                try {
                    int statIndex = Integer.parseInt(parts[0]);
                    Stat stat = Stat.values()[statIndex];
                    setStat(stat, parts[1]);
                } catch (Exception e) {
                    erroneousLines.add(line);
                }
            }
        }

        return erroneousLines;
    }

    /**
     * Sets the value matching of the given stat. Will throw an exception if the value is not suitable
     * @param stat type of stat to set
     * @param value string representation of the value, the result of toString from a supported type
     */
    public void setStat(Stat stat, String value) {
        if (validate(stat, value)) {
            stats.put(stat, value);
        }
    }

    /**
     * Gets the stat matching the given index. Returns null if the given index is invalid
     * @param stat type of the stat
     * @return stat value as string or null if stat is invalid
     */
    @CheckForNull
    public String getStat(Stat stat) {
        return stats.get(stat);
    }

    public int getStatInt(@NonNull Stat stat) {
        Preconditions.checkState(stat.getStatType() == StatType.INTEGER,
                "Stat is not an integer");
        return Integer.parseInt(stats.get(stat));
    }

    public long getStatLong(@NonNull Stat stat) {
        Preconditions.checkState(stat.getStatType() == StatType.LONG,
                "Stat is not a long");
        return Long.parseLong(stats.get(stat));
    }

    public float getStatFloat(@NonNull Stat stat) {
        Preconditions.checkState(stat.getStatType() == StatType.FLOAT,
                "Stat is not a float");
        return Float.parseFloat(stats.get(stat));
    }

    public boolean getStatBoolean(@NonNull Stat stat) {
        Preconditions.checkState(stat.getStatType() == StatType.BOOLEAN,
                "Stat is not a boolean");
        return Boolean.parseBoolean(stats.get(stat));
    }

    public String getFormattedStat(@NonNull Stat stat) {
        final String statValue = Objects.requireNonNull(stats.get(stat));
        switch (stat) {
            case TOTAL_GAME_TIME:
                return TextUtils.dayHrMinFromLong(Long.parseLong(statValue), true);
            case LONGEST_GAME_TIME:
                return TextUtils.minSecFromLong(Long.parseLong(statValue));
            case PERCENTAGE_NORMAL:
            case PERCENTAGE_RATIONAL:
            case PERCENTAGE_TIME:
            case PERCENTAGE_EXTENDED:
                return TextUtils.formatPercentage(Float.parseFloat(statValue));
            default:
                return statValue;
        }
    }

    private Map<Stat, String> initStats() {
        final Map<Stat, String> stats = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) {
            stats.put(stat, getDefaultValue(stat.statType));
        }

        return stats;
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<Stat, String> entry : stats.entrySet()) {
            sb.append(entry.getKey().ordinal());
            sb.append(STATS_DELIMITER);
            sb.append(entry.getValue());
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    private static String getDefaultValue(StatType type) {
        switch (type) {
            case INTEGER:
            case LONG:
                return "0";
            case FLOAT:
                return "0f";
            case BOOLEAN:
                return "false";
            case STRING:
                return "-";
            default:
                throw new IllegalArgumentException("No default value defined for " + type);
        }
    }

    private static boolean validate(Stat stat, String value) {
        // If string representation is invalid, will throw an exception
        switch (stat.getStatType()) {
            case STRING:
                break;
            case INTEGER:
                Integer.parseInt(value);
                break;
            case LONG:
                Long.parseLong(value);
                break;
            case FLOAT:
                Float.parseFloat(value);
                break;
            case BOOLEAN:
                Boolean.parseBoolean(value);
                break;
            default:
                throw new IllegalArgumentException("Unknown stat type: " + stat.getStatType());
        }

        return true;
    }
}
