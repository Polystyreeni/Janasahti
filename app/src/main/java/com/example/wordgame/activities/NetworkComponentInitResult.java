package com.example.wordgame.activities;

import com.example.wordgame.volley.extensions.INetworkResponse;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class NetworkComponentInitResult {
    private final boolean success;
    private final String errorMessage;
    private final Throwable throwable;


    private NetworkComponentInitResult(boolean success, @Nullable String errorMessage,
                                      @Nullable Throwable throwable) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.throwable = throwable;
    }

    public boolean isSuccess() {
        return this.success;
    }

    @CheckForNull
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @CheckForNull
    public Throwable getThrowable() {
        return this.throwable;
    }

    public static NetworkComponentInitResult successResponse() {
        return new NetworkComponentInitResult(true, null, null);
    }

    public static NetworkComponentInitResult errorResponse(String errorMessage) {
        return new NetworkComponentInitResult(false, errorMessage, null);
    }

    public static NetworkComponentInitResult errorResponse(Throwable error) {
        return new NetworkComponentInitResult(false, error.getMessage(), error);
    }

    public static NetworkComponentInitResult fromNetworkResponse(INetworkResponse response) {
        if (response.isError()) {
            if (response.getErrorCause() != null) {
                return errorResponse(response.getErrorCause());
            } else {
                return errorResponse(response.getErrorMessage());
            }
        } else {
            return successResponse();
        }
    }
}
