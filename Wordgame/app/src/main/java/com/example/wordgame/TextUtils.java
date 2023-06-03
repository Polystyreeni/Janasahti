package com.example.wordgame;

import android.graphics.Color;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.concurrent.TimeUnit;

public class TextUtils {

    public static final String TAG = "TextUtils";

    public static Spanned getSpannedText(String text) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        }
        else {
            return Html.fromHtml(text);
        }
    }

    public static int getTileTextSize(WindowManager windowManager) {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);

        double width = dm.widthPixels / dm.xdpi;

        // Totally 100% arbitrary equation that has not been tested at all
        return (int)(width * UserSettings.getTextScale());
    }

    public static String hrMinSecFromLong(long millis) {
        Log.d(TAG, "Current millis: " + millis);
        int days = (int)TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        int hours = (int)TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        int minutes = (int)TimeUnit.MILLISECONDS.toMinutes(millis);

        return String.format("%d d, %d h, %d min", days, hours, minutes);
    }

    /**
     * Recursive function to set color of all text views
     * Used for setting dark mode
     * @param v the parent view
     * @param color the color to apply to all text views
     */
    public static void setTextColorForViews(View v, int color) {
        try {
            if (v instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) v;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    setTextColorForViews(child, color);
                }
            }
            else if (v instanceof TextView) {
                ((TextView) v).setTextColor(color);
            }

        } catch (Exception e) {
            Log.d(TAG, "Could not set text color for views: " + e);
        }

    }
}
