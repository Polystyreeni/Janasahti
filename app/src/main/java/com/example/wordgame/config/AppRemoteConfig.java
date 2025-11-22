package com.example.wordgame.config;

import androidx.annotation.NonNull;

import com.example.wordgame.models.GameVersion;
import com.example.wordgame.utility.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Remote configuration that is downloaded when the application is started.
 * Allows app authors to modify app functionality remotely without the need for version updates
 */
public class AppRemoteConfig implements IApplicationConfiguration {
    public static final String PROPERTY_TIME_STAMP = "timeStamp";
    private final Map<String, String> propertyMap;

    private AppRemoteConfig(Map<String, String> properties) {
        this.propertyMap = new HashMap<>(properties);

        // If properties are loaded remotely, they should not come with a time stamp
        if (!this.propertyMap.containsKey(PROPERTY_TIME_STAMP)) {
            long now = System.currentTimeMillis();
            this.propertyMap.put(PROPERTY_TIME_STAMP, Long.toString(now));
        }
    }

    /**
     * Parses content from the string representation of the file.
     * String representation should follow the normal .properties file format
     * @param fileContent file contents as string
     * @return parsed configuration
     */
    public static AppRemoteConfig parseFromFile(String fileContent) {
        List<String> lines = Arrays.asList(fileContent.split(TextUtils.NEWLINE_REGEX));
        Map<String, String> properties = ConfigUtility.createPropertyMap(lines);
        return new AppRemoteConfig(properties);
    }

    @Override
    public int getIntProperty(@NonNull String key) {
        return Integer.parseInt(Objects.requireNonNull(propertyMap.get(key)));
    }

    @Override
    public long getLongProperty(@NonNull String key) {
        return Long.parseLong(Objects.requireNonNull(propertyMap.get(key)));
    }

    @Override
    public boolean getBooleanProperty(@NonNull String key) {
        return Boolean.parseBoolean(propertyMap.get(key));
    }

    @Override
    public GameVersion getVersion() {
        return GameVersion.valueOf(Objects.requireNonNull(propertyMap.get("latestVersion")));
    }

    @Override
    public String getStringProperty(@NonNull String property) {
        return Objects.requireNonNull(propertyMap.get(property));
    }

    @Override
    public boolean isEmpty() {
        return propertyMap.isEmpty();
    }

    @Override
    public String asString() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            lines.add(entry.getKey() + ConfigUtility.DELIMITER + entry.getValue());
        }

        return String.join(System.lineSeparator(), lines);
    }

    @Override
    public Map<String, String> getPropertyMap() {
        return Collections.unmodifiableMap(propertyMap);
    }

    @Override
    public boolean hasProperty(@NonNull String propertyKey) {
        return propertyMap.get(propertyKey) != null;
    }
}
