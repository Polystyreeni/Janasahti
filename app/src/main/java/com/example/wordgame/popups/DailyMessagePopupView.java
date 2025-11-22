package com.example.wordgame.popups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.util.Consumer;

import com.example.wordgame.R;
import com.example.wordgame.models.DailyMessage;
import com.example.wordgame.utility.TextUtils;

import java.util.Objects;

import javax.annotation.Nonnull;

public class DailyMessagePopupView implements IPopupView {
    private final View view;
    private final TextView messageHeaderView;
    private final TextView messageTextView;
    private final Button returnButton;

    public DailyMessagePopupView(LayoutInflater inflater, ViewGroup root) {
        View popupView = inflater.inflate(R.layout.daily_message_popup, root);

        this.messageHeaderView = popupView.findViewById(R.id.motdHeaderView);
        this.messageTextView = popupView.findViewById(R.id.motdTextView);
        this.returnButton = popupView.findViewById(R.id.motdExitButton);

        this.view = popupView;
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // NOP
    }

    public void setMessage(@Nonnull DailyMessage message) {
        Objects.requireNonNull(message);

        if (!message.getHeader().isEmpty()) {
            messageHeaderView.setText(message.getHeader());
        }

        messageTextView.setText(TextUtils.getSpannedText(message.getContent()));
    }

    public void addDismissListener(Consumer<View> listener) {
        returnButton.setOnClickListener(listener::accept);
    }
}
