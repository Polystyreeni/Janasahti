package com.example.wordgame;

import java.util.ArrayList;
import java.util.List;

public class UserSettings {
    public interface MethodRunner {
        public void run(Object arg);
    }

    // User settings
    private static int darkModeEnabled = 0;
    private static int oledProtectionEnabled = 0;
    private static int textScale = 14;

    public static List<MethodRunner> userSettings = new ArrayList<>();

    public static void initializeSettings() {
        userSettings.add(new MethodRunner() {
            @Override
            public void run(Object arg) {
                int value = Integer.parseInt((String)arg);
                setDarkModeEnabled(value);
            }
        });
        userSettings.add(new MethodRunner() {
            @Override
            public void run(Object arg) {
                int value = Integer.parseInt((String)arg);
                setOledProtectionEnabled(value);
            }
        });
        userSettings.add(new MethodRunner() {
            @Override
            public void run(Object arg) {
                int value = Integer.parseInt((String)arg);
                setTextScale(value);
            }
        });
    }

    public static int getDarkModeEnabled() { return darkModeEnabled; }
    public static int getOledProtectionEnabled() {return oledProtectionEnabled;}
    public static int getTextScale() {return textScale;}

    public static void setDarkModeEnabled(int value) {darkModeEnabled = value;}
    public static void setOledProtectionEnabled(int value) {oledProtectionEnabled = value;}
    public static void setTextScale(int value) {textScale = value;}


}
