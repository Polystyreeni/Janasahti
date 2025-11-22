package com.example.wordgame.config;

import androidx.annotation.NonNull;

import com.example.wordgame.models.GameVersion;

import java.util.Map;

public interface IApplicationConfiguration {

    /**
     * Gets the given property value as integer. Will throw if property is not found.
     * @param key property to search
     * @return property value as integer
     */
    int getIntProperty(@NonNull String key);

    /**
     * Gets the given property value as boolean. Will throw if property is not found.
     * @param key property to search
     * @return property value as boolean
     */
    boolean getBooleanProperty(@NonNull String key);

    /**
     * Gets the given property value as long. Will throw if property is not found.
     * @param key property to search
     * @return property value as long
     */
    long getLongProperty(@NonNull String key);
    String getStringProperty(@NonNull String key);

    /**
     * Gets the version of the configuration file
     * @return version of configuration file
     */
    GameVersion getVersion();

    /**
     * Checks if the configuration contains any items
     * @return true if configuration contains no items
     */
    boolean isEmpty();

    /**
     * Returns the configuration file in a string format
     * @return property file contents as string
     */
    String asString();

    /**
     * Gets all the properties with their values. NOTE: The returned map should not be allowed
     * to modify the properties.
     * @return property map
     */
    Map<String, String> getPropertyMap();

    /**
     * Checks if the given property exists in the configuration.
     * @param propertyKey property to find
     * @return true if property is found.
     */
    boolean hasProperty(String propertyKey);
}
