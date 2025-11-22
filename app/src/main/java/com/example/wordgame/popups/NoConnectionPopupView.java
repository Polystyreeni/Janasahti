package com.example.wordgame.popups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.util.Consumer;

import com.example.wordgame.R;

public class NoConnectionPopupView implements IPopupView {
    private final View view;
    private final TextView errorCauseText;

    public NoConnectionPopupView(LayoutInflater inflater,
                                 ViewGroup root,
                                 Consumer<View> offlineButtonListener,
                                 Consumer<View> returnButtonListener) {
        this.view = inflater.inflate(R.layout.network_unavailable_popup, root);
        this.errorCauseText = this.view.findViewById(R.id.gameUnavailablePopupErrorMessage);

        final Button playOfflineButton = this.view.findViewById(R.id.gameUnavailablePopupOfflineButton);
        final Button returnButton = this.view.findViewById(R.id.gameUnavailablePopupReturnButton);

        playOfflineButton.setOnClickListener(offlineButtonListener::accept);
        returnButton.setOnClickListener(returnButtonListener::accept);
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // No dark mode specific settings
    }

    public void setErrorCause(String message) {
        this.errorCauseText.setText(message);
    }
}
