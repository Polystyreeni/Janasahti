package com.example.wordgame.volley.extensions;

import java.util.Arrays;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NetworkByteResponse implements INetworkResponse {
    @Nullable final byte[] content;
    @Nullable final Throwable errorCause;
    @Nullable final String errorMessage;

    public NetworkByteResponse(@Nullable byte[] content,
                               @Nullable Throwable errorCause, @Nullable String errorMessage) {
        this.content = content;
        this.errorCause = errorCause;
        this.errorMessage = errorMessage;
    }

    public boolean isError() {
        return this.content == null;
    }

    @CheckForNull
    public byte[] getContent() {
        return this.content;
    }

    @Override
    @CheckForNull
    public Throwable getErrorCause() {
        return this.errorCause;
    }

    @Override
    @CheckForNull
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    @Nonnull
    public String toString() {
        return "NetworkStringResponse{" +
                "content='" + Arrays.toString(content) + '\'' +
                ", errorCause=" + errorCause +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
