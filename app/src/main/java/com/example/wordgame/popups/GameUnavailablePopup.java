package com.example.wordgame.popups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.wordgame.R;

public class GameUnavailablePopup implements IPopupView {
    private final View view;

    public GameUnavailablePopup(LayoutInflater inflater, ViewGroup root,
                                Throwable errorMessage, View.OnClickListener clickListener) {
        this.view = inflater.inflate(R.layout.game_unavailable_popup, root);

        final TextView errorTextView = this.view.findViewById(R.id.unrecoverablePopupErrorMessage);
        final Button exitButton = this.view.findViewById(R.id.unrecoverablePopupReturnButton);

        errorTextView.setText(errorMessage.getMessage());
        exitButton.setOnClickListener(clickListener);
    }

    @Override
    public View getView() {
        return this.view;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // NOP, view is always the same color
    }
}
