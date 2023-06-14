package com.example.wordgame;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class VersionManager {
    private static final String TAG = "VersionManager";
    private static final String version = "1.5.1";

    // Will be overridden once version file is fetched
    private static String latestVersion = "1.5.0";

    public static String getVersion() {
        return version;
    }
    public static void setLatestVersion(String version) {latestVersion = version;}
    public static String getLatestVersion() {return latestVersion;}

    public static void getLatestVersion(MenuActivity activity) {
        try {
            String versionLink = NetworkConfig.getUrl("version");
            RequestQueue queue = Volley.newRequestQueue(activity.getApplicationContext());
            StringRequest request = new StringRequest(Request.Method.GET, versionLink, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    activity.onVersionRetrieved(response);
                }
            }, error -> {
                activity.onVersionRetrieved("");
                Log.d(TAG, "Version file retrieving failed");
            });

            queue.add(request);
        }
        catch (InvalidUrlRequestException e) {
            activity.onVersionRetrieved("");
            Log.d(TAG, "Version file retrieving failed");
        }
    }
}
