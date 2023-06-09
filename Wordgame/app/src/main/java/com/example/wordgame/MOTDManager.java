package com.example.wordgame;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Message of the day service
 * Used for informing users about changes (for example, new content)
 */
public class MOTDManager {
    private static final String TAG = "MOTDManager";

    private static String MOTDMessage = "";

    public static void getLatestMessage(MenuActivity activity) {
        try {
            String messageUrl = NetworkConfig.getUrl("message");
            RequestQueue queue = Volley.newRequestQueue(activity.getApplicationContext());
            Utf8StringRequest request = new Utf8StringRequest(Request.Method.GET, messageUrl, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);
                    String[] content = response.split("\\|");
                    if (content.length == 2) {
                        MOTDMessage = content[1];
                        activity.onMOTDRetrieved(content[0]);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    activity.onMOTDRetrieved("");
                    Log.d(TAG, "Message retrieving failed");
                }
            });

            queue.add(request);
        }

        catch (Exception ex) {
            activity.onMOTDRetrieved("");
            Log.d(TAG, "Version file retrieving failed");
        }
    }

    public static String getMessageText() {return MOTDMessage;}
}
