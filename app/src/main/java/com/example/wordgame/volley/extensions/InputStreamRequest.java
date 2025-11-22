package com.example.wordgame.volley.extensions;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.HashMap;
import java.util.Map;

public class InputStreamRequest extends Request<byte[]> {
    private final Response.Listener<byte[]> listener;
    private final Map<String, String> params;
    public Map<String, String> responseHeaders;

    public InputStreamRequest(int method, String url,
                              Response.Listener<byte[]> listener,
                              Response.ErrorListener errorListener,
                              HashMap<String, String> params) {
        super(method, url, errorListener);
        setShouldCache(false);
        this.listener = listener;
        this.params = params;
    }

    @Override
    protected Map<String, String> getParams() {
        return params;
    }

    @Override
    protected void deliverResponse(byte[] response) {
        listener.onResponse(response);
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse networkResponse) {
        this.responseHeaders = networkResponse.headers;
        return Response.success(networkResponse.data,
                HttpHeaderParser.parseCacheHeaders(networkResponse));
    }
}
