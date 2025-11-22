package com.example.wordgame.popups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.example.wordgame.R;
import com.example.wordgame.managers.ApplicationManager;
import com.example.wordgame.managers.UserSettingsManager;
import com.example.wordgame.models.AppRemoteState;
import com.google.android.material.slider.Slider;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

public class SettingsPopupView implements IPopupView {
    private final View view;
    private final Button supportButton;
    private final Map<UserSettingsManager.UserSetting, View> settingsButtons =
            new EnumMap<>(UserSettingsManager.UserSetting.class);

    public SettingsPopupView(LayoutInflater inflater, ViewGroup root,
                             ApplicationManager applicationManager,
                             View.OnClickListener supportClickListener,
                             View.OnClickListener returnClickListener,
                             CompoundButton.OnCheckedChangeListener offlineCheckListener) {
        View popupView = inflater.inflate(R.layout.game_settings_popup, root);

        final CheckBox darkBox = popupView.findViewById(R.id.settingCheckboxColor);
        final CheckBox oledBox = popupView.findViewById(R.id.settingCheckboxOled);
        final CheckBox scoreBox = popupView.findViewById(R.id.settingCheckboxScore);
        final CheckBox offlineBox = popupView.findViewById(R.id.settingOfflineMode);
        final Slider textSizeSlider = popupView.findViewById(R.id.settingsTextSizeSlider);
        supportButton = popupView.findViewById(R.id.supportButton);
        final Slider boardSizeSlider = popupView.findViewById(R.id.settingsBoardSizeSlider);
        final Button returnButton = popupView.findViewById(R.id.settingsReturnButton);

        settingsButtons.put(UserSettingsManager.UserSetting.DARK_MODE, darkBox);
        settingsButtons.put(UserSettingsManager.UserSetting.SCREEN_PROTECTION, oledBox);
        settingsButtons.put(UserSettingsManager.UserSetting.USE_SCORE_BOARD, scoreBox);
        settingsButtons.put(UserSettingsManager.UserSetting.TEXT_SCALE, textSizeSlider);
        settingsButtons.put(UserSettingsManager.UserSetting.BOARD_SCALE, boardSizeSlider);

        supportButton.setOnClickListener(supportClickListener);
        returnButton.setOnClickListener(returnClickListener);
        offlineBox.setChecked(applicationManager.getRemoteState() == AppRemoteState.OFFLINE);
        offlineBox.setOnCheckedChangeListener(offlineCheckListener);

        setDefaults(applicationManager.getUserSettingsManager());
        addListeners(applicationManager.getUserSettingsManager());

        this.view = popupView;
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void onDarkModeSet(boolean value) {
        // Handled elsewhere
    }

    private void addListeners(UserSettingsManager settingsManager) {
        final UserSettingsManager.UserSetting darkId = UserSettingsManager.UserSetting.DARK_MODE;
        addListenerForSetting(darkId, (l, b) -> settingsManager.setSetting(darkId, b));

        final UserSettingsManager.UserSetting oledId = UserSettingsManager.UserSetting.SCREEN_PROTECTION;
        addListenerForSetting(oledId, (l, b) -> settingsManager.setSetting(oledId, b));

        final UserSettingsManager.UserSetting scoreId = UserSettingsManager.UserSetting.USE_SCORE_BOARD;
        addListenerForSetting(scoreId, (l, b) -> settingsManager.setSetting(scoreId, b));

        final UserSettingsManager.UserSetting textSizeId = UserSettingsManager.UserSetting.TEXT_SCALE;
        addListenerForSetting(textSizeId, (slider, value, fromUser) ->
                settingsManager.setSetting(textSizeId, (int) value));

        final UserSettingsManager.UserSetting boardScaleId = UserSettingsManager.UserSetting.BOARD_SCALE;
        addListenerForSetting(boardScaleId, (slider, value, fromUser)
                -> settingsManager.setSetting(boardScaleId, (float) (10f / value)));
    }

    private void setDefaults(UserSettingsManager settingsManager) {
        boolean darkMode = (Boolean) Objects.requireNonNull(
                settingsManager.getSetting(UserSettingsManager.UserSetting.DARK_MODE)).getValue();
        boolean oled = (Boolean) Objects.requireNonNull(
                settingsManager.getSetting(UserSettingsManager.UserSetting.SCREEN_PROTECTION)).getValue();
        boolean score = (Boolean) Objects.requireNonNull(
                settingsManager.getSetting(UserSettingsManager.UserSetting.USE_SCORE_BOARD)).getValue();
        int textSize = (Integer) Objects.requireNonNull(
                settingsManager.getSetting(UserSettingsManager.UserSetting.TEXT_SCALE)).getValue();
        float boardScale = (Float) Objects.requireNonNull(
                settingsManager.getSetting(UserSettingsManager.UserSetting.BOARD_SCALE).getValue());

        CheckBox darkBox = (CheckBox) settingsButtons.get(UserSettingsManager.UserSetting.DARK_MODE);
        CheckBox oledBox = (CheckBox) settingsButtons.get(UserSettingsManager.UserSetting.SCREEN_PROTECTION);
        CheckBox scoreBox = (CheckBox) settingsButtons.get(UserSettingsManager.UserSetting.USE_SCORE_BOARD);
        Slider textSlider = (Slider) settingsButtons.get(UserSettingsManager.UserSetting.TEXT_SCALE);
        Slider boardScaleSlider = (Slider) settingsButtons.get(UserSettingsManager.UserSetting.BOARD_SCALE);

        Objects.requireNonNull(darkBox).setChecked(darkMode);
        Objects.requireNonNull(oledBox).setChecked(oled);
        Objects.requireNonNull(scoreBox).setChecked(score);
        Objects.requireNonNull(textSlider).setValue(textSize);
        Objects.requireNonNull(boardScaleSlider).setValue((int) Math.round(10f / boardScale));
    }

    public void setSupportButtonEnabled(boolean enabled) {
        supportButton.setClickable(enabled);
    }

    private void addListenerForSetting(UserSettingsManager.UserSetting id,
                                       @Nonnull View.OnClickListener listener) {
        Button button = Objects.requireNonNull((Button) settingsButtons.get(id));
        button.setOnClickListener(listener);
    }

    private void addListenerForSetting(UserSettingsManager.UserSetting id,
                                       @Nonnull CompoundButton.OnCheckedChangeListener listener) {
        CheckBox checkBox = Objects.requireNonNull((CheckBox) settingsButtons.get(id));
        checkBox.setOnCheckedChangeListener(listener);
    }

    private void addListenerForSetting(UserSettingsManager.UserSetting id, @Nonnull Slider.OnChangeListener listener) {
        Slider slider = Objects.requireNonNull((Slider) settingsButtons.get(id));
        slider.addOnChangeListener(listener);
    }
}
