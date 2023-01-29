package com.example.wordgame;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
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

        // Totally 100% arbitrary equation that has not been tested at all
        return (int)(width * UserSettings.getTextScale());
    }
}
