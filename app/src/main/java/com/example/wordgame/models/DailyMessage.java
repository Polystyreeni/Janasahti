package com.example.wordgame.models;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class DailyMessage {
    private static final String TAG = "DailyMessage";
    private static final String DELIMITER = "\\|";
    private static final int PROPERTY_COUNT = 4;
    private final String id;
    private final GameVersion requiredVersion;
    private final String header;
    private final String content;

    public DailyMessage(String id, GameVersion requiredVersion, String header, String content) {
        this.id = id;
        this.requiredVersion = requiredVersion;
        this.header = header;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public GameVersion getRequiredVersion() {
        return requiredVersion;
    }

    public String getHeader() {
        return header;
    }

    public String getContent() {
        return content;
    }

    /**
     * Creates a message from the given string input. The input may contain multiple messages
     * and in such cases the first message matching the provided app version is returned.
     * @param str string to parse message from.
     * @param appVersion Required version of the message
     * @return parsed message or null if the string is invalid, or no message for the given version is found
     */
    @CheckForNull
    public static DailyMessage parseFromString(@NonNull String str, @NonNull GameVersion appVersion) {
        Objects.requireNonNull(str);
        Objects.requireNonNull(appVersion);
        final String[] parts = str.split(DELIMITER);

        if (parts.length % PROPERTY_COUNT != 0) {
            Log.w(TAG, "Invalid daily message format, parts = " + parts.length);
            return null;
        }

        final int messageCount = parts.length / PROPERTY_COUNT;
        for (int i = 0; i < messageCount; i++) {
            try {
                final int baseIndex = PROPERTY_COUNT * i;
                final String id = parts[baseIndex];
                final GameVersion version = GameVersion.valueOf(parts[baseIndex + 1]);

                // Versions are incompatible
                if (!Objects.equals(version, appVersion)) {
                    continue;
                }

                final String header = parts[baseIndex + 2];
                final String content = parts[baseIndex + 3];
                return new DailyMessage(id, version, header, content);
            } catch (IndexOutOfBoundsException e) {
                Log.w(TAG, "Invalid format for single message, skipping message", e);
            }
        }

        return null;
    }
}