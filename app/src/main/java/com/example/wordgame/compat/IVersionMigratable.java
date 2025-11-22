package com.example.wordgame.compat;

import android.content.Context;

import com.example.wordgame.models.GameVersion;

/**
 * Interface for identifying resources that need a version conversion.
 * For example, old settings files or stats files
 */
public interface IVersionMigratable {
    void migrateTo(Context ctx, GameVersion from, GameVersion to);
}