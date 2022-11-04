package com.example.wordgame;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardActivity extends AppCompatActivity {
    private final String TAG = "ScoreboardActivity";
    private final String FIRESTORE_PREFIX = "wg_board_";

    private final List<HighscoreData> highScores = new ArrayList<>();
    private ScoreboardAdapter scoreBoardAdapter;

    // User values
    private String username;
    private HighscoreData highscoreData;

    // Ui components
    private TextView scoreTextView;
    private TextView bestPlayersTextView;
    private ConstraintLayout scoreBoardLayout;

    // Firebase
    private FirebaseFirestore mFireStore;
    private CollectionReference sessionReference;
    private String collectionPath;

    private Thread boardThread = null;
    Handler scoreBoardHandler = new Handler();
    Handler newBoardHandler = new Handler();
    Runnable newBoardRunnable = new Runnable() {
        @Override
        public void run() {
            if(boardThread.isAlive() || boardThread == null)
                newBoardHandler.postDelayed(this, 500);
            else {
                newBoardHandler.removeCallbacks(newBoardRunnable);
                startNewGame();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);

        // UI Components
        scoreTextView = findViewById(R.id.personalStatTextView);
        scoreBoardLayout = findViewById(R.id.scoreboardLayout);
        bestPlayersTextView = findViewById(R.id.scoreboardTitleTextView);

        // Get data from previous game
        Intent intent = getIntent();
        String intentStr = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        highscoreData = parseHighScore(intentStr);

        showPersonalScore(highscoreData);

        mFireStore = FirebaseFirestore.getInstance();
        collectionPath = FIRESTORE_PREFIX + BoardManager.getNextBoard().getBoardString();
        sessionReference = mFireStore.collection(collectionPath);

        // Initialize recyclerView
        RecyclerView scoreBoard = (RecyclerView) findViewById(R.id.scoreRecyclerView);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        scoreBoard.addItemDecoration(itemDecoration);

        // Set adapter for scoreboard
        scoreBoardAdapter = new ScoreboardAdapter(highScores);
        scoreBoard.setAdapter(scoreBoardAdapter);
        scoreBoard.setLayoutManager(new LinearLayoutManager(this));

        // Fetch high score data from Firebase
        updateHighScores();

        // Create board in background while in main menu
        boardThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BoardManager.generateBoard();
            }
        });

        boardThread.start();
        scoreBoardHandler.postDelayed(this::startGame, GameSettings.getScoreBoardDuration());

        if(GameSettings.getDarkModeEnabled() > 0) {
            setDarkMode();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cancel all runnables and threads
        if(newBoardHandler != null && newBoardRunnable != null)
            newBoardHandler.removeCallbacks(newBoardRunnable);

        if(boardThread != null)
            boardThread.interrupt();

        if(scoreBoardHandler != null)
            scoreBoardHandler.removeCallbacksAndMessages(null);
    }

    // Shows info from users previous game
    private void showPersonalScore(HighscoreData data) {
        double percentage = (double)data.getScore() / (double)BoardManager.getNextBoard().getMaxScore();
        String text = String.format("%s: %d pistettä, %.2f %s%nPisin löydetty sana: %s", data.getUserName(),
                data.getScore(), percentage * 100, "%", data.getBestWord());
        scoreTextView.setText(text);
    }

    private void startGame() {
        newBoardHandler.postDelayed(newBoardRunnable, 0);
    }

    // Start the MainActivity with a new board
    private void startNewGame() {
        limitScores();
        Log.d("ScoreBoardActivity", "Starting new game");
        Intent intent = new Intent(ScoreboardActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_MESSAGE, username);
        startActivity(intent);
        boardThread = null;
        finish();
    }

    // This may have to be removed for safety reasons (no valuable data is at danger here though)
    private void limitScores() {
        while(highScores.size() > GameSettings.getScoreBoardMaxCount()) {
            Log.d(TAG, "Removing lowest score from scoreboard");
            String uid = highScores.get(highScores.size() - 1).getUserId();
            sessionReference.document(uid).delete();
        }
    }

    // Parses highscore data from the string provided by MainActivity
    private HighscoreData parseHighScore(String dataStr) {
        String[] dataArr = dataStr.split("/");
        username = dataArr[0];
        int score = Integer.parseInt(dataArr[1]);
        String bestWord = dataArr[2];

        return new HighscoreData(username, score, bestWord);
    }

    private void setDarkMode() {
        scoreBoardLayout.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.background_gradient_dark));
        bestPlayersTextView.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.rounded_corner_black));
        bestPlayersTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        scoreTextView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        RecyclerView scoreRecyclerView = findViewById(R.id.scoreRecyclerView);
        scoreRecyclerView.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.background_gradient_dark));
    }

    // Sort and get highscore data from Firebase
    public void updateHighScores() {
        mFireStore.collection(collectionPath).orderBy("score", Query.Direction.DESCENDING)
                .limit(GameSettings.getScoreBoardMaxCount())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                highScores.clear();
                if (value != null) {
                    for (DocumentSnapshot snapshot : value) {
                        HighscoreData data = snapshot.toObject(HighscoreData.class);
                        highScores.add(data);
                        Log.d(TAG, "Fetched highscore from database");
                    }

                    // Not optimal, but other methods refuse to work properly
                    scoreBoardAdapter.notifyDataSetChanged();

                } else {
                    Log.d(TAG, "Snapshot is null");
                }
            }
        });
    }
}