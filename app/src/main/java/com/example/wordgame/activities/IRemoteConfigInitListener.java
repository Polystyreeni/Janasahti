package com.example.wordgame.activities;

import com.example.wordgame.models.RemoteLoadType;

/**
 * Interface for listening remote configuration events.
 * Activities that use ApplicationManager should implement this interface if they contain
 * functionality that depends on remote access.
 */
public interface IRemoteConfigInitListener {
    void onRemoteConfigLoadSuccess(RemoteLoadType loadType);
    void onRemoteConfigLoadFailure();
    void onOfflineModeSet();
}