package com.example.wordgame.popups;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.wordgame.R;

import javax.annotation.Nonnull;

public class UserBannedPopup implements IPopupView {
    private final View popupView;
    private final TextView banTextView;
    private final Button exitButton;

    public UserBannedPopup(Context ctx, LayoutInflater inflater, ViewGroup root) {
        this.popupView = inflater.inflate(R.layout.player_banned_popup, root);
        this.banTextView = this.popupView.findViewById(R.id.playerBannedTextView);
        this.exitButton = this.popupView.findViewById(R.id.playerBannedExitButton);
    }

    @Override
    public View getView() {
        return this.popupView;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // Popup is always the same color
    }

    public void setBanText(String text) {
        this.banTextView.setText(text);
    }

    public void addDismissListener(@Nonnull View.OnClickListener listener) {
        this.exitButton.setOnClickListener(listener);
    }
}
