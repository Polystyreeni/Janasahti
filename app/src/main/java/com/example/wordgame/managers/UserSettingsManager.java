package com.example.wordgame.managers;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.wordgame.compat.IVersionMigratable;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.gamemode.GameModeType;
import com.example.wordgame.models.GameVersion;
import com.example.wordgame.utility.IoUtils;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class UserSettingsManager implements IVersionMigratable {
    public static final String TAG = "UserSettingsManager";
    public static final String SETTINGS_FILE_NAME = "wg_settings";
    public enum UserSetting {
        DARK_MODE,
        SCREEN_PROTECTION,
        TEXT_SCALE,
        ACTIVE_GAME_MODES,
        USE_SCORE_BOARD,
        DAILY_MESSAGE_ID,
        LEGACY_COLORS,   // Unused
        BOARD_SCALE,
    }

    public interface IGameSetting {
        Object getValue();
        void setValue(Object arg);
    }

    // User settings
    private UserSettings userSettings;

    public UserSettingsManager() {
        userSettings = new UserSettings();
    }

    public void setSetting(UserSetting settingId, Object value) {
        IGameSetting setting = userSettings.settingsMap.get(settingId);
        if (setting == null) {
            throw new IllegalArgumentException("Setting not found: " + settingId);
        }
        setting.setValue(value);
    }

    public IGameSetting getSetting(UserSetting settingId) {
        return userSettings.settingsMap.get(settingId);
    }

    public void writeToFile(Context ctx) {
        String content = userSettings.toString();
        try {
            IoUtils.writeFile(ctx, SETTINGS_FILE_NAME, content);
        } catch (IOException e) {
            Logger.getInstance().warn(TAG, "Failed to write settings file", e);
        }
    }

    public void readFromFile(Context ctx) {
        List<String> lines = IoUtils.readLinesFromFile(ctx, SETTINGS_FILE_NAME);
        if (lines.isEmpty()) {
            Logger.getInstance().info(TAG, "Settings file could not be read - using defaults");
            userSettings = new UserSettings();
        } else {
            userSettings = new UserSettings(lines);
        }
    }

    @Override
    public void migrateTo(Context ctx, @Nullable GameVersion from, GameVersion to) {
        // Pre version 2.0.0
        final GameVersion minPrevious = new GameVersion(1, 4, 1);
        final GameVersion maxPrevious = new GameVersion(1, 5, 1);
        if (from == null || (from.compareTo(minPrevious) >= 0 && from.compareTo(maxPrevious) <= 0)) {
            Logger.getInstance().info(TAG, "Migrating old settings to latest version");
            String line = IoUtils.readLineFromFile(ctx, "settings");
            if (!line.isEmpty()) {
                String[] values = line.split("/");
                if (values.length == 6) {
                    setSetting(UserSetting.DARK_MODE,
                            intStringToBooleanString(values[0]));
                    setSetting(UserSetting.SCREEN_PROTECTION,
                            intStringToBooleanString(values[1]));
                    setSetting(UserSetting.TEXT_SCALE,
                            Integer.parseInt(values[2]));
                    setSetting(UserSetting.DAILY_MESSAGE_ID, values[3]);
                    setSetting(UserSetting.ACTIVE_GAME_MODES,
                            oldGameModeToCurrent(values[4]));
                    setSetting(UserSetting.USE_SCORE_BOARD,
                            intStringToBooleanString(values[5]));
                    writeToFile(ctx);
                }
            }
            // Dark mode (0/1)
            // OledProtection (0/1)
            // TextScale (int)
            // MotdId (string)
            // Active gamemode (string)
            // Use score board (0/1)
        }
    }

    private String intStringToBooleanString(String str) {
        int val = Integer.parseInt(str);
        return val < 1 ? "false" : "true";
    }

    private GameModeType[] oldGameModeToCurrent(String gameModeStr) {
        if (gameModeStr.equals("normal")) {
            return new GameModeType[] { GameModeType.NORMAL };
        } else if (gameModeStr.equals("rational")) {
            return new GameModeType[] { GameModeType.RATIONAL };
        } else {
            return new GameModeType[] { GameModeType.NORMAL };
        }
    }

    public static class UserSettings {
        private static final String VALUE_DELIMITER = "=";
        private boolean darkModeEnabled;
        private boolean screenProtectionEnabled;
        private int textScale;
        private String messageOfTheDayId;
        private GameModeType[] activeGameModes;
        private boolean useScoreBoard;
        private boolean useLegacyColorTheme;
        private float boardScale;
        private final Map<UserSetting, IGameSetting> settingsMap = new EnumMap<>(UserSetting.class);

        private UserSettings() {
            initializeSettings();
            initWithDefaults();
        }

        private UserSettings(List<String> lines) {
            initializeSettings();

            // This will set default values for when game settings are updated in newer versions
            // while still supporting the older versions
            initWithDefaults();
            try {
                parseFromLines(lines);
            } catch (Exception e) {
                Logger.getInstance().warn(TAG, "Failed to read user settings, using defaults", e);
            }
        }

        private void initializeSettings() {
            settingsMap.put(UserSetting.DARK_MODE, new IGameSetting() {
                @Override
                public Object getValue() {
                    return darkModeEnabled;
                }

                @Override
                public void setValue(Object arg) {
                    darkModeEnabled = Boolean.parseBoolean(arg.toString());
                }

                @NonNull
                @Override
                public String toString() {
                    return String.valueOf(darkModeEnabled);
                }
            });

            settingsMap.put(UserSetting.SCREEN_PROTECTION, new IGameSetting() {
                @Override
                public Object getValue() {
                    return screenProtectionEnabled;
                }

                @Override
                public void setValue(Object arg) {
                    screenProtectionEnabled = Boolean.parseBoolean(arg.toString());
                }

                @NonNull
                @Override
                public String toString() {
                    return String.valueOf(screenProtectionEnabled);
                }
            });

            settingsMap.put(UserSetting.TEXT_SCALE, new IGameSetting() {
                @Override
                public Object getValue() {
                    return textScale;
                }

                @Override
                public void setValue(Object arg) {
                    textScale = Integer.parseInt(arg.toString());
                }

                @NonNull
                @Override
                public String toString() {
                    return String.valueOf(textScale);
                }
            });

            settingsMap.put(UserSetting.USE_SCORE_BOARD, new IGameSetting() {
                @Override
                public Object getValue() {
                    return useScoreBoard;
                }

                @Override
                public void setValue(Object arg) {
                    boolean value = Boolean.parseBoolean(arg.toString());
                    useScoreBoard = value;
                }

                @NonNull
                @Override
                public String toString() {
                    return String.valueOf(useScoreBoard);
                }
            });

            settingsMap.put(UserSetting.DAILY_MESSAGE_ID, new IGameSetting() {
                @Override
                public Object getValue() {
                    return messageOfTheDayId;
                }

                @Override
                public void setValue(Object arg) {
                    messageOfTheDayId = (String) arg;
                }

                @NonNull
                @Override
                public String toString() {
                    return messageOfTheDayId;
                }
            });

            settingsMap.put(UserSetting.ACTIVE_GAME_MODES, new IGameSetting() {
                @Override
                public Object getValue() {
                    return activeGameModes;
                }

                @Override
                public void setValue(Object arg) {
                    if (arg instanceof String) {
                        String[] gameModeStrs = ((String) arg).split(",");
                        activeGameModes = new GameModeType[gameModeStrs.length];
                        for (int i = 0; i < gameModeStrs.length; i++) {
                            activeGameModes[i] = GameModeType.valueOf(gameModeStrs[i]);
                        }
                    } else if (arg instanceof GameModeType[]) {
                        activeGameModes = (GameModeType[]) arg;
                    }
                }

                @Override
                @NonNull
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    for (GameModeType type : activeGameModes) {
                        sb.append(type.toString());
                        sb.append(",");
                    }

                    sb.deleteCharAt(sb.length() - 1);
                    return sb.toString();
                }
            });

            settingsMap.put(UserSetting.LEGACY_COLORS, new IGameSetting() {
                @Override
                public Object getValue() {
                    return useLegacyColorTheme;
                }

                @Override
                public void setValue(Object arg) {
                    useLegacyColorTheme = Boolean.parseBoolean(arg.toString());
                }

                @NonNull
                @Override
                public String toString() {
                    return String.valueOf(useLegacyColorTheme);
                }
            });

            settingsMap.put(UserSetting.BOARD_SCALE, new IGameSetting() {
                @Override
                public Object getValue() {
                    return boardScale;
                }

                @Override
                public void setValue(Object arg) {
                    boardScale = Float.parseFloat(arg.toString());
                }

                @Override
                @NonNull
                public String toString() {
                    return String.valueOf(boardScale);
                }
            });
        }

        private void initWithDefaults() {
            this.darkModeEnabled = false;
            this.screenProtectionEnabled = true;
            this.textScale = 14;
            this.messageOfTheDayId = "";
            this.activeGameModes = new GameModeType[] { GameModeType.NORMAL };
            this.useScoreBoard = true;
            this.boardScale = 1f;
        }

        public void parseFromLines(List<String> files) {
            for (String line : files) {
                String[] keyValuePair = line.split(VALUE_DELIMITER);
                if (keyValuePair.length == 2) {
                    UserSetting setting = UserSetting.valueOf(keyValuePair[0]);
                    if (settingsMap.containsKey(setting)) {
                        settingsMap.get(setting).setValue(keyValuePair[1]);
                    }
                }
            }
        }

        @NonNull
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (Map.Entry<UserSetting, IGameSetting> entry : settingsMap.entrySet()) {
                sb.append(entry.getKey()).append(VALUE_DELIMITER).append(entry.getValue().toString());
                sb.append(System.lineSeparator());
            }

            return sb.toString();
        }
    }
}
