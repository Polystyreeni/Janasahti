package com.example.wordgame.popups;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.wordgame.R;

import javax.annotation.Nonnull;

public class WordDisplayPopup implements IPopupView {
    private final View popupView;
    private final TextView wordTextView;
    private final Button searchButton;

    public WordDisplayPopup(Context ctx, LayoutInflater inflater, ViewGroup root) {
        this.popupView = inflater.inflate(R.layout.found_word_popup, root);
        this.wordTextView = this.popupView.findViewById(R.id.displayWordTextView);
        this.searchButton = this.popupView.findViewById(R.id.definitionSearchButton);

        // FIXME: Service used for fetching definitions has changed its authorization policy
        // and can no longer be read using Jsoup. Currently I have no workaround for this
        // so this feature is disabled for now (and likely indefinitely)
        this.searchButton.setVisibility(View.GONE);
    }

    @Override
    public View getView() {
        return this.popupView;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // Word display popup is always the same color
    }

    public void setWord(CharSequence word) {
        wordTextView.setText(word);
    }

    public void addButtonListener(@Nonnull View.OnClickListener listener) {
        this.searchButton.setOnClickListener(listener);
    }
}
