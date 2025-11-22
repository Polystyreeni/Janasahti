package com.example.wordgame.managers;

import android.content.Context;
import android.util.Log;

import com.example.wordgame.compat.IVersionMigratable;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.models.GameVersion;
import com.example.wordgame.utility.IoUtils;

import java.io.IOException;

import javax.annotation.CheckForNull;

public class VersionManager {
    public static final String GAME_VERSION_FILE = "wg_version";
    private static final String TAG = "VersionManager";
    private final IVersionMigratable[] migratables;

    public VersionManager(IVersionMigratable... migratables) {
        this.migratables = migratables;
    }
    public void performMigrationIfNeeded(Context ctx, GameVersion currentVersion) {
        final GameVersion oldVersion = loadCachedGameVersion(ctx);
        if (oldVersion == null || oldVersion.compareTo(currentVersion) < 0) {
            Logger.getInstance().debug(TAG, "Performing version migration to current version");
            migrateVersion(ctx, oldVersion, currentVersion);
        }

        cacheCurrentVersion(ctx, currentVersion);
    }

    private void migrateVersion(Context ctx, GameVersion oldVersion, GameVersion newVersion) {
        for (IVersionMigratable migratable : this.migratables) {
            migratable.migrateTo(ctx, oldVersion, newVersion);
        }
    }

    @CheckForNull
    private static GameVersion loadCachedGameVersion(Context ctx) {
        final String versionStr = IoUtils.readLineFromFile(ctx, GAME_VERSION_FILE);
        if (versionStr.isEmpty()) {
            return null;
        }

        return GameVersion.valueOf(versionStr);
    }

    private static void cacheCurrentVersion(Context ctx, GameVersion currentVersion) {
        try {
            IoUtils.writeFile(ctx, GAME_VERSION_FILE, currentVersion.toString());
        } catch (IOException e) {
            Log.d(TAG, "Failed to write version file", e);
        }
    }
}
