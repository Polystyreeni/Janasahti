package com.example.wordgame.config;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;

import com.example.wordgame.models.GameVersion;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Local configuration file that is loaded from the application package (.properties file).
 * This is provided in the installation of the application.
 */
public class AppLocalPropertiesConfig implements IApplicationConfiguration {
    private static final String LOCAL_PROPERTIES_NAME = "app_config.properties";
    private static Properties properties = null;

    public AppLocalPropertiesConfig(Context ctx) {
        initializeProperties(ctx);
    }

    private void initializeProperties(Context ctx) {
        properties = new Properties();
        AssetManager assetManager = ctx.getAssets();
        try (InputStream inputStream = assetManager.open(LOCAL_PROPERTIES_NAME)) {
            properties.load(inputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int getIntProperty(@NonNull String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    @Override
    public boolean getBooleanProperty(@NonNull String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    @Override
    public long getLongProperty(@NonNull String key) {
        return Long.parseLong(properties.getProperty(key));
    }

    @Override
    public String getStringProperty(@NonNull String key) {
        return properties.getProperty(key);
    }

    @Override
    public GameVersion getVersion() {
        return GameVersion.valueOf(properties.getProperty("appVersion"));
    }

    @Override
    public boolean isEmpty() {
        return properties == null || properties.isEmpty();
    }

    @Override
    public String asString() {
        List<String> lines = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            lines.add(value + ConfigUtility.DELIMITER + value);
        }

        return String.join(System.lineSeparator(), lines);
    }

    @Override
    public boolean hasProperty(@NonNull String propertyKey) {
        return properties.get(propertyKey) != null;
    }

    public Map<String, String> getPropertyMap() {
        Map<String, String> propertyMap = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            propertyMap.put(key, properties.getProperty(key));
        }
        return propertyMap;
    }
}
