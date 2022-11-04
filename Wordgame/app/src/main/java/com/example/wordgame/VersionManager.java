package com.example.wordgame;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class VersionManager {
    private static final String TAG = "VersionManager";

    private static final String version = "1.2.0";
    private static final String versionLink = "https://drive.google.com/uc?export=download&id=1KusEpu5D6fUBFWZnDFUeA0A7FWGKiMDX";

    public static String getVersion() {
        return version;
    }

    public static void getLatestVersion(MenuActivity activity) {
        RequestQueue queue = Volley.newRequestQueue(activity.getApplicationContext());
        StringRequest request = new StringRequest(Request.Method.GET, versionLink, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                activity.onVersionRetrieved(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                activity.onVersionRetrieved("");
                Log.d(TAG, "Version file retrieving failed");
            }
        });

        queue.add(request);
    }
}
