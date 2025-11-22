package com.example.wordgame.popups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.wordgame.R;
import com.mukesh.MarkdownView;

/**
 * Popup for displaying application license information.
 */
public class LicenseInfoPopup implements IPopupView {
    private final View view;

    public LicenseInfoPopup(LayoutInflater inflater, ViewGroup root,
                            View.OnClickListener returnButtonListener) {
        this.view = inflater.inflate(R.layout.license_info_popup, root);

        final MarkdownView markdownView = this.view.findViewById(R.id.licenseInfoView);
        markdownView.loadMarkdownFromAssets("3rdparty.md");
        final Button returnButton = this.view.findViewById(R.id.licensePopupReturnButton);
        returnButton.setOnClickListener(returnButtonListener);
    }

    @Override
    public View getView() {
        return this.view;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // NOP, always the same color
    }
}