package com.example.wordgame.volley.extensions;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NetworkStringResponse implements INetworkResponse {
    @Nullable
    final String content;
    @Nullable final Throwable errorCause;
    @Nullable final String errorMessage;

    public NetworkStringResponse(@Nullable String content,
                               @Nullable Throwable errorCause, @Nullable String errorMessage) {
        this.content = content;
        this.errorCause = errorCause;
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean isError() {
        return this.content == null;
    }

    @CheckForNull
    public String getContent() {
        return this.content;
    }

    @CheckForNull
    @Override
    public Throwable getErrorCause() {
        return this.errorCause;
    }

    @CheckForNull
    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    @Nonnull
    public String toString() {
        return "NetworkStringResponse{" +
                "content='" + content + '\'' +
                ", errorCause=" + errorCause +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
