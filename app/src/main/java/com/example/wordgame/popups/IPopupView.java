package com.example.wordgame.popups;

import android.view.View;

public interface IPopupView {
    View getView();
    void onDarkModeSet(boolean value);
}
