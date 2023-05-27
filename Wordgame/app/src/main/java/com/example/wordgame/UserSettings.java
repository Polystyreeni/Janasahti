package com.example.wordgame;

import java.util.ArrayList;
import java.util.List;

public class UserSettings {
    public interface MethodRunner {
        void run(Object arg);
    }

    // User settings
    private static int darkModeEnabled = 0;
    private static int oledProtectionEnabled = 0;
    private static int textScale = 14;
    private static String MOTDId = "";
    private static String activeGameMode = "classic";

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
        userSettings.add(new MethodRunner() {
            @Override
            public void run(Object arg) {
                if (arg != null)
                    setMOTDId((String)arg);
            }
        });
        userSettings.add(new MethodRunner() {
            @Override
            public void run(Object arg) {
                if (arg != null)
                    setActiveGameMode((String)arg);
            }
        });
    }

    public static int getDarkModeEnabled() { return darkModeEnabled; }
    public static int getOledProtectionEnabled() {return oledProtectionEnabled;}
    public static int getTextScale() {return textScale;}
    public static String getMOTDId() {return MOTDId;}
    public static String getActiveGameMode() {return activeGameMode;}

    public static void setDarkModeEnabled(int value) {darkModeEnabled = value;}
    public static void setOledProtectionEnabled(int value) {oledProtectionEnabled = value;}
    public static void setTextScale(int value) {textScale = value;}
    public static void setMOTDId(String value) {MOTDId = value;}
    public static void setActiveGameMode(String value) {activeGameMode = value;}
}
