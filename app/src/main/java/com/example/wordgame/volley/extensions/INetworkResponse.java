package com.example.wordgame.volley.extensions;

import javax.annotation.CheckForNull;

public interface INetworkResponse {
    boolean isError();
    @CheckForNull
    Throwable getErrorCause();

    @CheckForNull
    String getErrorMessage();
}
