package com.example.wordgame.utility;

import com.example.wordgame.R;
import com.example.wordgame.gamemode.GameModeType;
import com.example.wordgame.managers.ApplicationManager;
import com.example.wordgame.managers.UserSettingsManager;
import com.example.wordgame.models.AppRemoteState;
import com.example.wordgame.models.Board;
import com.example.wordgame.models.HighScoreData;

import java.util.Map;
import java.util.Objects;

public class ScoreUtils {
    public static int getLeaderboardContent(GameModeType gameModeType) {
        switch (gameModeType) {
            case NORMAL:
            case EXTENDED:
                return R.string.scoreboard_text_normal;
            case RATIONAL:
                return R.string.scoreboard_text_rational;
            case TIME_CHASE:
                return R.string.scoreboard_text_time_chase;
            default:
                throw new IllegalArgumentException("Not a valid game mode " + gameModeType);
        }
    }

    public static int calculateScoreForBoard(final Map<Integer, Integer> scoreMap, final Board board) {
        int score = 0;
        for (String word : board.getWords()) {
            score += Objects.requireNonNull(scoreMap.get(word.length()));
        }

        return score;
    }

    /**
     * Checks if the settings have score board enabled.
     * @param applicationManager application manager
     * @return true if score board setting is on and application is not in offline mode
     */
    public static boolean useScoreBoard(ApplicationManager applicationManager) {
        return applicationManager.getRemoteState() != AppRemoteState.OFFLINE &&
                (Boolean) applicationManager.getUserSettingsManager()
                .getSetting(UserSettingsManager.UserSetting.USE_SCORE_BOARD).getValue();
    }
}