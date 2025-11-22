package com.example.wordgame.utility;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.example.wordgame.R;
import com.example.wordgame.managers.ApplicationManager;
import com.example.wordgame.managers.UserSettingsManager;
import com.example.wordgame.popups.IPopupView;
import com.google.common.base.Preconditions;

import java.util.Objects;

public class ActivityUtils {
    private ActivityUtils() {
        // Static utility class
    }

    private static PopupWindow createPopupWindow(PopupWindowBuilder builder) {
        Objects.requireNonNull(builder.popupView);
        final PopupWindow popupWindow = new PopupWindow(builder.popupView.getView(),
                builder.width, builder.height, builder.focusable);
        popupWindow.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        popupWindow.showAtLocation(builder.popupView.getView(),
                builder.gravity, builder.xOffset, builder.yOffset);
        return popupWindow;
    }

    public static boolean isDarkModeEnabled(ApplicationManager applicationManager) {
        return (Boolean) Objects.requireNonNull(applicationManager.getUserSettingsManager()
                .getSetting(UserSettingsManager.UserSetting.DARK_MODE)).getValue();
    }

    public static class PopupWindowBuilder {
        private final IPopupView popupView;
        private boolean focusable = false;
        private int gravity = Gravity.CENTER;
        private int width = LinearLayout.LayoutParams.MATCH_PARENT;
        private int height = LinearLayout.LayoutParams.MATCH_PARENT;
        private int xOffset;
        private int yOffset;

        public PopupWindowBuilder(final IPopupView popupView) {
            this.popupView = popupView;
        }

        public PopupWindowBuilder withFocusable(boolean focusable) {
            this.focusable = focusable;
            return this;
        }

        public PopupWindowBuilder withGravity(int gravity) {
            Preconditions.checkState(gravity > 0, "Invalid gravity value");
            this.gravity = gravity;
            return this;
        }

        public PopupWindowBuilder withDimensions(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public PopupWindowBuilder withDimensionsWrapContent() {
            this.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            this.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            return this;
        }

        public PopupWindowBuilder withOffsets(int xOffset, int yOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            return this;
        }

        public PopupWindow build() {
            return createPopupWindow(this);
        }
    }
}
