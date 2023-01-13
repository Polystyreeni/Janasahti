package com.example.wordgame;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

public class TextUtils {
    public static Spanned getSpannedText(String text) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        }
        else {
            return Html.fromHtml(text);
        }
    }
}
