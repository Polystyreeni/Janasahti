package com.example.wordgame.popups;

import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.wordgame.R;

public class AppUpdatePopupView implements IPopupView {
    private final TextView versionTextView;
    private final TextView linkTextView;
    private final TextView requiredTextView;
    private final Button cancelButton;
    private final View view;

    public AppUpdatePopupView(LayoutInflater inflater,
                              ViewGroup root) {
        View popupView = inflater.inflate(R.layout.update_popup, root);

        versionTextView = popupView.findViewById(R.id.versionComparison);
        linkTextView = popupView.findViewById(R.id.updateLink);
        requiredTextView = popupView.findViewById(R.id.updateRequiredText);
        cancelButton = popupView.findViewById(R.id.updateReturnButton);

        this.view = popupView;
    }

    @Override
    public View getView() {
        return this.view;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // NOP
    }

    public void setVersionText(String text) {
        versionTextView.setText(text);
    }

    public void setUpdateLink(String linkText) {
        linkTextView.setText(linkText);
        Linkify.addLinks(linkTextView, Linkify.ALL);
    }

    public void setRequired(boolean required) {
        requiredTextView.setVisibility(required ? View.VISIBLE : View.GONE);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        cancelButton.setOnClickListener(listener);
    }
}
