package com.example.wordgame.config;

import androidx.annotation.NonNull;

import com.example.wordgame.models.GameVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Local configuration file loaded from a file. This class allows the modification of application
 * properties using remote updates
 * This should contain all the same properties as {@link AppLocalPropertiesConfig}.
 */
public class AppLocalFileConfig implements IApplicationConfiguration {
    private static final String DELIMITER = "=";
    private final Map<String, String> properties;

    public AppLocalFileConfig(List<String> lines) {
        this(ConfigUtility.createPropertyMap(lines));
    }

    public AppLocalFileConfig(Map<String, String> properties) {
        this.properties = new HashMap<>(properties);
    }

    @Override
    public int getIntProperty(@NonNull String key) {
        return Integer.parseInt(Objects.requireNonNull(properties.get(key)));
    }

    @Override
    public boolean getBooleanProperty(@NonNull String key) {
        return Boolean.parseBoolean(Objects.requireNonNull(properties.get(key)));
    }

    @Override
    public long getLongProperty(@NonNull String key) {
        return Long.parseLong(Objects.requireNonNull(properties.get(key)));
    }

    @Override
    public String getStringProperty(@NonNull String key) {
        return Objects.requireNonNull(properties.get(key));
    }

    @Override
    public GameVersion getVersion() {
        return GameVersion.valueOf(Objects.requireNonNull(properties.get("appVersion")));
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public String asString() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            lines.add(entry.getKey() + DELIMITER + entry.getValue());
        }

        return String.join(System.lineSeparator(), lines);
    }

    @Override
    public Map<String, String> getPropertyMap() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public boolean hasProperty(@NonNull String propertyKey) {
        return properties.get(propertyKey) != null;
    }
}
