package com.example.wordgame.popups;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import com.example.wordgame.R;
import com.example.wordgame.managers.UserStatsManager;
import com.example.wordgame.models.UserStats;
import com.example.wordgame.utility.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class StatsPopupView implements IPopupView {
    private final Context ctx;
    private final LayoutInflater inflater;
    private final UserStatsManager userStatsManager;
    private final View view;
    private final TextView popupHeader;
    private final Button returnButton;

    public StatsPopupView(Context ctx, LayoutInflater inflater, ViewGroup root,
                          UserStatsManager userStatsManager) {
        this.ctx = ctx;
        this.inflater = inflater;
        this.userStatsManager = userStatsManager;

        View popupView = inflater.inflate(R.layout.user_stats_popup, root);

        this.view = popupView;
        this.popupHeader = popupView.findViewById(R.id.userStatsName);
        this.returnButton = popupView.findViewById(R.id.userStatsReturnButton);

        LinearLayout scrollLayout = popupView.findViewById(R.id.userStatsScrollLayout);
        populateStatsLayout(scrollLayout);
    }

    @Override
    public void onDarkModeSet(boolean value) {
        if (value) {
            this.view.setBackground(ContextCompat.getDrawable(ctx.getApplicationContext(),
                    R.drawable.background_gradient_dark));
            TextUtils.setTextColorForViews(this.view, Color.WHITE);
        }
    }

    @Override
    public View getView() {
        return this.view;
    }

    public void setHeaderText(String text) {
        this.popupHeader.setText(text);
    }

    public void addDismissListener(Consumer<View> onDismiss) {
        this.returnButton.setOnClickListener(onDismiss::accept);
    }

    private void populateStatsLayout(LinearLayout layout) {
        final List<View> viewsToAdd = new ArrayList<>();

        for (UserStats.Stat stat : UserStats.Stat.values()) {
            Integer header = UserStats.DELIMITER_STATS.get(stat);
            if (header != null) {
                viewsToAdd.add(createStatHeaderView(ctx.getString(header)));
            }

            viewsToAdd.add(createStatValueView(ctx.getString(stat.getDescriptionId()),
                    userStatsManager.getFormattedStat(stat)));
        }

        for (View v : viewsToAdd) {
            layout.addView(v);
        }
    }

    private View createStatHeaderView(String headerText) {
        View view = inflater.inflate(R.layout.stats_list_header, null);
        TextView header = view.findViewById(R.id.statListHeaderText);
        header.setText(headerText);
        return view;
    }

    private View createStatValueView(String statName, String statValue) {
        View view = inflater.inflate(R.layout.stats_list_item, null);
        TextView statNameText = view.findViewById(R.id.userStatName);
        TextView statValueText = view.findViewById(R.id.userStatValue);
        statNameText.setText(statName);
        statValueText.setText(statValue);
        return view;
    }
}
