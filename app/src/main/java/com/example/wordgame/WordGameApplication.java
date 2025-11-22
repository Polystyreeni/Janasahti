package com.example.wordgame;

import android.app.Application;
import android.util.Log;

import com.example.wordgame.debug.Logger;

public class WordGameApplication extends Application {
    private static final String TAG = "WordGameApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WordGameApplication onCreate()");

        // Will be set based on ApplicationManager settings
        // Cannot be set only here, because remote config override can change debug level
        Logger.initialize(getApplicationContext(), Logger.LoggerMode.UNINITIALIZED);

        final Thread.UncaughtExceptionHandler defaultExceptionHandler
                = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Write error to log file and stop logger
            Logger.getInstance().error(TAG, "Unhandled exception caught", throwable);
            Logger.getInstance().stop();
            if (defaultExceptionHandler != null) {
                defaultExceptionHandler.uncaughtException(thread, throwable);
            } else {
                Log.w(TAG, "No default exception handler defined, using System.exit");
                System.exit(1);
            }
        });
    }
}