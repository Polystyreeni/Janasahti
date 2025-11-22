package com.example.wordgame.utility;

import com.example.wordgame.R;
import com.example.wordgame.gamemode.GameModeType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class AppConstants {
    public static final String INTENT_EXTRA_MESSAGE_DELIMITER = "/";
    public static final long SCORE_BOARD_DURATION_MS = 12000;

    private static final Map<GameModeType, Integer> GAME_MODE_NAMES =
            new EnumMap<GameModeType, Integer>(GameModeType.class) {
        {
            put(GameModeType.NORMAL, R.string.gamemode_name_normal);
            put(GameModeType.RATIONAL, R.string.gamemode_name_rational);
            put(GameModeType.TIME_CHASE, R.string.gamemode_name_time_chase);
            put(GameModeType.EXTENDED, R.string.gamemode_name_extended);
        }
    };

    private static final Map<GameModeType, Integer> GAME_MODE_DESCRIPTIONS =
            new EnumMap<GameModeType, Integer>(GameModeType.class) {
                {
                    put(GameModeType.NORMAL, R.string.gamemode_description_normal);
                    put(GameModeType.RATIONAL, R.string.gamemode_description_rational);
                    put(GameModeType.TIME_CHASE, R.string.gamemode_description_time_chase);
                    put(GameModeType.EXTENDED, R.string.gamemode_description_extended);
                }
            };

    public static int getGameModeName(GameModeType type) {
        return Objects.requireNonNull(GAME_MODE_NAMES.get(type));
    }

    public static int getGameModeDescription(GameModeType type) {
        return Objects.requireNonNull(GAME_MODE_DESCRIPTIONS.get(type));
    }
}
