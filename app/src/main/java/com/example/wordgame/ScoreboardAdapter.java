package com.example.wordgame;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wordgame.gamemode.GameModeType;
import com.example.wordgame.models.HighScoreData;
import com.example.wordgame.models.HighScoreDataTimeChase;
import com.example.wordgame.utility.ScoreUtils;
import com.example.wordgame.utility.TextUtils;

import java.util.List;

public class ScoreboardAdapter extends RecyclerView.Adapter<ScoreboardAdapter.ViewHolder> {
    private final List<HighScoreData> localDataSet;
    private final GameModeType gameModeType;
    private final Resources resources;
    private int scoreMax;
    private int scoreImprovement;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            textView = view.findViewById(R.id.scoreDataTextView);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    // Initialize the dataset of the Adapter.
    public ScoreboardAdapter(List<HighScoreData> dataSet, GameModeType gameModeType, Resources resources) {
        this.localDataSet = dataSet;
        this.gameModeType = gameModeType;
        this.resources = resources;
    }

    // Create new views (invoked by the layout manager)
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recyclerview_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        HighScoreData data = localDataSet.get(position);

        final int resourceId = ScoreUtils.getLeaderboardContent(gameModeType);
        // Position is 0-indexed, leaderboard should start from index 1
        String text = getResourceText(resourceId, data, position + 1);

        viewHolder.getTextView().setText(TextUtils.getSpannedText(text));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void setScoreMax(int scoreMax) {
        this.scoreMax = scoreMax;
    }

    public void setScoreImprovement(int improvement) {
        this.scoreImprovement = improvement;
    }

    private String getResourceText(int resourceId, HighScoreData data, int position) {
        switch (gameModeType) {
            case NORMAL:
            case EXTENDED:
            case RATIONAL:
                return resources.getString(resourceId,
                        position, data.getUserName(),
                        data.getScore(),
                        data.getBestWord(),
                        ((float) data.getScore() / (float) scoreMax) * 100,
                        "%");
            case TIME_CHASE:
                HighScoreDataTimeChase dataTC = (HighScoreDataTimeChase) data;
                return resources.getString(resourceId, position, dataTC.getUserName(),
                        TextUtils.minSecFromLong(dataTC.getGameDuration()),
                        dataTC.getScore(),
                        dataTC.getBestWord(),
                        ((float) dataTC.getScore() / (float) scoreMax) * 100,
                        "%");
            default:
                throw new IllegalArgumentException("Unknown game mode " + gameModeType);
        }
    }
}
