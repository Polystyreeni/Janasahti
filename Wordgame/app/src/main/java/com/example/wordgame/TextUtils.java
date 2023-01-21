package com.example.wordgame;

import android.graphics.Point;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

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
        //double height = dm.heightPixels / dm.ydpi;
        //double baseSize = Math.max(width, height);

        //Log.d(TAG, "The width of screen is: " + width);

        // Totally 100% arbitrary equation that has not been tested at all
        return (int)(width * 14);
    }

    /*public static double getScreenWidth(WindowManager windowManager) {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);

        final Point size = new Point();
        windowManager.getDefaultDisplay().getRealSize(size);

        double width = dm.widthPixels;
        return width;

        //return size.x;

        //double width = dm.widthPixels / dm.xdpi;
        //return width;
    }*/
}
