package com.example.wordgame.managers;

import android.content.Context;

import androidx.core.util.Consumer;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.wordgame.volley.extensions.InputStreamRequest;
import com.example.wordgame.volley.extensions.NetworkByteResponse;
import com.example.wordgame.volley.extensions.NetworkStringResponse;
import com.example.wordgame.volley.extensions.Utf8StringRequest;

public class NetworkManager {
    private final RequestQueue requestQueue;

    public NetworkManager(Context ctx) {
        requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
    }

    /**
     * Creates a network GET request with a string response on the given url. Performs the given
     * consumer function on successful request. If the request fails, the consumer function will receive
     * a response with null content and error message
     * @param url url of request
     * @param onComplete function to perform on completed request
     */
    public void createStringGetRequest(String url, Consumer<NetworkStringResponse> onComplete) {
        StringRequest stringRequest = new Utf8StringRequest(Request.Method.GET,
                url,
                response -> sendValidResponse(response, onComplete),
                error -> onComplete.accept(new NetworkStringResponse(null,
                        error.getCause(), error.getLocalizedMessage())));

        requestQueue.add(stringRequest);
    }

    /**
     * Creates a network GET request with an byte array response on the given url. Performs the given
     * consumer function on successful request. If the request fails, the consumer function will be given
     * a response with null content and error message
     * @param url url of request
     * @param onComplete function to perform on completed successful request
     */
    public void createInputStreamGetRequest(String url, Consumer<NetworkByteResponse> onComplete) {
        InputStreamRequest request = new InputStreamRequest(Request.Method.GET,
                url,
                response -> onComplete.accept(new NetworkByteResponse(response, null, null)),
                error -> onComplete.accept(new NetworkByteResponse(null,
                        error.getCause(), error.getLocalizedMessage())),
                null);

        requestQueue.add(request);
    }

    private static void sendValidResponse(String response, Consumer<NetworkStringResponse> onComplete) {
        // For some reason, Volley string request seems to return 404 as a successful response with
        // the return value of null - we'll check that here ourselves
        if (response == null) {
            onComplete.accept(new NetworkStringResponse(null, null,
                    "Unhandled network error"));
        } else {
            onComplete.accept(new NetworkStringResponse(response, null, null));
        }
    }
}
