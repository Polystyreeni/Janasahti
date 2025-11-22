package com.example.wordgame.utility;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.wordgame.managers.UserSettingsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TextUtils {
    public static final String TAG = "TextUtils";
    public static final String NEWLINE_REGEX = "\\r?\\n|\\r";
    public static final String ELLIPSIS = "\u2026";

    @SuppressWarnings("deprecation")
    public static Spanned getSpannedText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(text);
        }
    }

    /**
     * Gets letter size for a single game board tile.
     * @param windowManager windowManager of the activity.
     * @param userSettingsManager userSettingsManager
     * @return text size as integer
     */
    public static int getTileTextSize(WindowManager windowManager, UserSettingsManager userSettingsManager) {
        final DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);

        // Totally 100% arbitrary equation that has not been tested at all
        final double width = dm.widthPixels / dm.xdpi;
        int scale = (Integer) Objects.requireNonNull(
                userSettingsManager.getSetting(UserSettingsManager.UserSetting.TEXT_SCALE)).getValue();
        return (int) (width * scale);
    }

    /**
     * Formats the given milliseconds to days, hours and minutes
     * @param millis time in milliseconds
     * @param showZeroDays whether to include days, if days = 0
     * @return formatted time
     */
    @SuppressLint("DefaultLocale")
    public static String dayHrMinFromLong(long millis, boolean showZeroDays) {
        int days = (int)TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        int hours = (int)TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        int minutes = (int)TimeUnit.MILLISECONDS.toMinutes(millis);

        if (days < 1 && !showZeroDays) {
            return String.format("%d h, %d min", hours, minutes);
        } else {
            return String.format("%d d, %d h, %d min", days, hours, minutes);
        }
    }

    /**
     * Formats the given milliseconds to minutes and seconds.
     * @param millis time in milliseconds
     * @return formatted time
     */
    @SuppressLint("DefaultLocale")
    public static String minSecFromLong(long millis) {
        final int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        final int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(millis);
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Formats the given milliseconds to format dd.mm.yyyy HH:mm
     * @param millis milliseconds to format
     * @return formatted time
     */

    public static String millisToFullDate(long millis) {
        return millisToFullDate(millis, false);
    }

    @SuppressLint("SimpleDateFormat")
    public static String millisToFullDate(long millis, boolean fileNameSafe) {
        final Date date = new Date(millis);
        final String pattern = fileNameSafe ? "dd-MM-yyyy-HH-mm" : "dd.MM.yyyy HH:mm";
        return new SimpleDateFormat(pattern).format(date);
    }

    /**
     * Converts the given float to a percentage string.
     * @param percentage actual percentage (decimal between 0-1)
     * @return formatted percentage
     */
    public static String formatPercentage(float percentage) {
        return String.format(Locale.ENGLISH, "%.2f %s", percentage * 100f, "%");
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
            } else if (v instanceof TextView) {
                ((TextView) v).setTextColor(color);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not set text color for views: ", e);
        }
    }
}