package com.example.wordgame.popups;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.wordgame.R;
import com.example.wordgame.models.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordListPopupView implements IPopupView {

    private final Context ctx;
    private final View view;

    // Popup components
    private final TextView scoreText;
    private final LinearLayout wordListLayout;

    public WordListPopupView(Context ctx, LayoutInflater inflater, ViewGroup root) {
        this.ctx = ctx;

        View popupView = inflater.inflate(R.layout.game_end_popup, root);
        this.view = popupView;

        this.scoreText = popupView.findViewById(R.id.endScoreTextView);
        this.wordListLayout = popupView.findViewById(R.id.endScoreWordLayout);
    }

    @Override
    public View getView() {
        return this.view;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // NOP, WordListPopup is the same color always
    }

    public void setScoreText(CharSequence text) {
        this.scoreText.setText(text);
    }

    public void addWordHeader(String headerText) {
        final TextView header = new TextView(ctx);
        header.setText(headerText);
        header.setTextColor(Color.BLACK);
        header.setAllCaps(true);
        header.setTypeface(header.getTypeface(), Typeface.BOLD);
        wordListLayout.addView(header);
    }

    public void setWords(GameState gameState, List<String> words,
                         Resources resources,
                         View.OnLongClickListener listener) {
        Collections.sort(words, WordListPopupView::sortWords);

        // Group words by their length
        int previousLen = 0;
        for (String word : words) {
            if (word.length() != previousLen) {
                addWordHeader(resources.getString(R.string.wordlist_word_len, word.length()));
                previousLen = word.length();
            }
            TextView view = new TextView(this.ctx);
            view.setAllCaps(true);
            view.setText(word);
            view.setPadding(0, 1, 0, 1);
            int textColor = gameState.hasFoundWord(word)
                    ? this.ctx.getColor(R.color.dark_green) : Color.BLACK;
            view.setTextColor(textColor);
            view.setOnLongClickListener(listener);
            addWordView(view);
        }
    }

    private void addWordView(View wordView) {
        this.wordListLayout.addView(wordView);
    }

    // Sort words according to length
    private static int sortWords(String a, String b) {
        if (a.length() == b.length())
            return a.compareTo(b);

        return Integer.compare(b.length(), a.length());
    }
}
