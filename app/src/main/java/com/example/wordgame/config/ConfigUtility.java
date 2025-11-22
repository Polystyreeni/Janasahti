package com.example.wordgame.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for configuration files.
 */
public class ConfigUtility {
    public static final String DELIMITER = "=";
    private static final Set<String> OVERRIDABLE_PROPERTIES = new HashSet<String>() {
        {
            add("configurationUrls");
            add("supportEmail");
            add("supportGitHub");
            add("gameSourceLink");
        }
    };

    private ConfigUtility() {
        // Static utility class
    }

    /**
     * Creates a property file mapping from the given text lines. The implementation assumes
     * the given lines are in format key=value
     * Lines starting with '#' are treated as comments and ignored
     * @param lines lines of a file
     * @return property map
     */
    public static Map<String, String> createPropertyMap(List<String> lines) {
        final Map<String, String> propertyMap = new HashMap<>();
        for (String line : lines) {
             if (line.isEmpty() || isCommentLine(line)) {
                 continue;
             }

            final String[] propertyKeyPair = line.split(DELIMITER);
            if (propertyKeyPair.length == 2) {
                propertyMap.put(propertyKeyPair[0], propertyKeyPair[1]);
            } else if (propertyKeyPair.length == 1) {
                propertyMap.put(propertyKeyPair[0], null);
            } else if (propertyKeyPair.length > 2) {
                // URLs can have '=', so we'll recreate the url string here
                String[] tgtArray = new String[propertyKeyPair.length - 1];
                System.arraycopy(propertyKeyPair, 1, tgtArray, 0, tgtArray.length);
                propertyMap.put(propertyKeyPair[0], joinStringValues(tgtArray));
            }
        }

        return propertyMap;
    }

    /**
     * Checks if the given property can be overridden by remote config
     * @return true if property is overridable
     */
    public static boolean isOverridableProperty(String property) {
        return OVERRIDABLE_PROPERTIES.contains(property);
    }

    /**
     * Gets all properties in the given configuration file that are marked as overridable
     * @param config config file to check
     * @return overridable properties in the given config. Empty if no properties are present
     */
    public static Collection<String> getOverridablePropertiesFrom(IApplicationConfiguration config) {
        final Map<String, String> propertyMap = config.getPropertyMap();
        List<String> overridable = new ArrayList<>();
        for (String property : propertyMap.keySet()) {
            if (isOverridableProperty(property)) {
                overridable.add(property);
            }
        }

        return overridable;
    }

    private static boolean isCommentLine(String line) {
        return line.trim().startsWith("#");
    }

    private static String joinStringValues(String[] arr) {
        return String.join(DELIMITER, arr);
    }
}
