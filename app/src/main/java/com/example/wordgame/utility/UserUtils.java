package com.example.wordgame.utility;

import com.example.wordgame.gamemode.GameModeType;

import java.util.Random;

public class UserUtils {
    public static final int USER_NAME_MAX_LENGTH = 30;
    private static final String[] ILLEGAL_CHARACTERS = new String[] { "/", "=", "|" };
    private static final Random RNG = new Random();

    private UserUtils() {
        // Static utility class
    }

    /**
     * Sanitizes the given user name. Returns the provided defaultName if name cannot be sanitized.
     * @param name name to sanitize.
     * @param defaultName default name to use.
     * @return sanitized name or defaultName if sanitization fails.
     */
    public static String sanitizeUserName(String name, String defaultName) {
        String sanitized = name.trim();
        if (sanitized.isEmpty() || sanitized.equals(" ")) {
            return defaultName;
        }

        for (String character : ILLEGAL_CHARACTERS) {
            if (sanitized.contains(character)) {
                sanitized = sanitized.replace(character, "");
            }
        }

        if (sanitized.length() > USER_NAME_MAX_LENGTH) {
            sanitized = sanitized.substring(0, USER_NAME_MAX_LENGTH);
        }

        return sanitized;
    }

    /**
     * Gets a random game mode from the provided list of game modes.
     * @param types types to include in selections.
     * @return random element from the provided list
     */
    public static GameModeType getRandomGameModeType(GameModeType[] types) {
        if (types.length == 1) {
            return types[0];
        } else {
            return types[RNG.nextInt(types.length)];
        }
    }
}