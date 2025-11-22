package com.example.wordgame.volley.extensions;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import java.nio.charset.StandardCharsets;

/**
 * String request extension to support UTF-8 Charset.
 */
public class Utf8StringRequest extends StringRequest {
    public Utf8StringRequest(int method, String url, Response.Listener<String> listener,
                             @Nullable Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    public Utf8StringRequest(String url, Response.Listener<String> listener,
                             @Nullable Response.ErrorListener errorListener) {
        super(url, listener, errorListener);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        parsed = new String(response.data, StandardCharsets.UTF_8);

        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }
}
