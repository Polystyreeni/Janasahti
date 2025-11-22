package com.example.wordgame.models;

/**
 * Remote configuration load response type.
 * <p>FULL_LOAD = Full configuration was loaded</p>
 * <p>CACHED_LOAD = Cached configuration was loaded</p>
 * <p>BACKGROUND_LOAD = Configuration was loaded during offline mode</p>
 */
public enum RemoteLoadType {
    FULL_LOAD,
    CACHED_LOAD,
    BACKGROUND_LOAD
}