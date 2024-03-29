package com.example.wordgame;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScoreboardAdapter extends RecyclerView.Adapter<ScoreboardAdapter.ViewHolder> {
    private List<HighscoreData> localDataSet;
    private int scoreMax;
    private int scoreImprovement;
    private String userId = "";

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            textView = (TextView) view.findViewById(R.id.scoreDataTextView);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    // Initialize the dataset of the Adapter.
    public ScoreboardAdapter(List<HighscoreData> dataSet) {
        localDataSet = dataSet;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recyclerview_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element

        HighscoreData data = localDataSet.get(position);

        String text;
        if (UserSettings.getActiveGameMode().equals("rational")) {
            text = String.format("<b>%d</b>:   %s  -  %d sanaa &nbsp&nbsp<i>%s</i> &nbsp %.2f %s", position + 1,
                    data.getUserName(), data.getFoundWords(), data.getBestWord(),
                    ((float)data.getFoundWords() / (float)scoreMax) * 100, "%");
        }
        else {
            text = String.format("<b>%d</b>:   %s  -  %d pistettä &nbsp&nbsp<i>%s</i> &nbsp %.2f %s", position + 1,
                    data.getUserName(), data.getScore(), data.getBestWord(),
                    ((float)data.getScore() / (float)scoreMax) * 100, "%");
            if (scoreImprovement > 0 && userId.equals(data.getUserId()))
                text += String.format("&nbsp&nbsp &#11014 %d", scoreImprovement);
        }

        viewHolder.getTextView().setText(TextUtils.getSpannedText(text));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void setScoreMax(int scoreMax) {this.scoreMax = scoreMax;}

    public void setScoreImprovement(int improvement) {this.scoreImprovement = improvement;}

    public void setUserId(String id) {this.userId = id;}
}
