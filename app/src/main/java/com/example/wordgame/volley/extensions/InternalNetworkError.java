package com.example.wordgame.volley.extensions;

import androidx.annotation.Nullable;

/**
 * Network response created internally by ApplicationManager when an invalid remote configuration
 * is created.
 */
public class InternalNetworkError implements INetworkResponse {
    private final String errorMessage;
    private final Throwable cause;

    public InternalNetworkError(String errorMessage, @Nullable Throwable cause) {
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Nullable
    @Override
    public Throwable getErrorCause() {
        return this.cause;
    }

    @Nullable
    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }
}
