package com.example.wordgame.utility;

import com.example.wordgame.gamemode.GameModeType;
import com.example.wordgame.models.HighScoreData;
import com.example.wordgame.models.HighScoreDataTimeChase;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class FirebaseUtils {
    public static final String USER_COLLECTION = "wg_usernames";
    public static final String BAN_LIST_COLLECTION = "wg_banlist";
    public static final String UNINITIALIZED_USER_ID = "-1";
    private static final String BOARD_COLLECTION_PREFIX = "wg_board_";
    private static final Map<GameModeType, String> DOCUMENT_PREFIX_MAP =
            new EnumMap<GameModeType, String>(GameModeType.class) {
                {
                    put(GameModeType.TIME_CHASE, "tc_");
                }
            };

    private static final Map<GameModeType, Class<? extends HighScoreData>> HIGHSCORE_TYPE_MAP =
            new EnumMap<GameModeType, Class<? extends HighScoreData>>(GameModeType.class) {
                {
                    put(GameModeType.NORMAL, HighScoreData.class);
                    put(GameModeType.RATIONAL, HighScoreData.class);
                    put(GameModeType.TIME_CHASE, HighScoreDataTimeChase.class);
                    put(GameModeType.EXTENDED, HighScoreData.class);
                }
            };

    public static String getDocumentId(String userId, GameModeType gameMode) {
        if (gameMode == GameModeType.TIME_CHASE) {
            return Objects.requireNonNull(DOCUMENT_PREFIX_MAP.get(gameMode)) + userId;
        }
        return userId;
    }

    public static String getBoardCollectionId(GameModeType gameModeType, String boardString) {
        String documentPrefix = DOCUMENT_PREFIX_MAP.get(gameModeType);
        if (documentPrefix != null) {
            return BOARD_COLLECTION_PREFIX + documentPrefix + boardString;
        }
        return BOARD_COLLECTION_PREFIX + boardString;
    }

    public static Class<? extends HighScoreData> getHighScoreDataClass(GameModeType gameModeType) {
        return Objects.requireNonNull(HIGHSCORE_TYPE_MAP.get(gameModeType));
    }
}
