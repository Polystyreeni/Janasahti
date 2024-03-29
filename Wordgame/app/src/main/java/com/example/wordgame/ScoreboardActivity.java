package com.example.wordgame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
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

    // State
    private final List<HighscoreData> highScores = new ArrayList<>();
    private ScoreboardAdapter scoreBoardAdapter;
    private boolean activityStopped = false;
    private Board playedBoard = null;
    private boolean shouldWriteStats = true;
    private long startTime;

    // User values
    private String username;
    private HighscoreData highscoreData;
    private int scoreImprovement = 0;

    // Ui components
    private TextView scoreTextView;
    private TextView bestPlayersTextView;
    private ConstraintLayout scoreBoardLayout;
    private ProgressBar progressBar;
    private TextView timerTextView;

    // Audio
    private AudioHandler audioHandler;
    private int soundIdFirst;

    // Firebase
    private FirebaseFirestore mFireStore;
    private CollectionReference sessionReference;
    private FirebaseAuth firebaseAuth;
    private String collectionPath;

    private Thread boardThread = null;
    Handler scoreBoardHandler = new Handler();
    Handler newBoardHandler = new Handler();
    Runnable newBoardRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            millis = GameSettings.getScoreBoardDuration() - millis;

            if(millis < 0) {
                if(boardThread != null && boardThread.isAlive()) {
                    newBoardHandler.postDelayed(this, 500);
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    newBoardHandler.removeCallbacks(newBoardRunnable);
                    startNewGame();
                }
                return;
            }

            int seconds = (int) (millis / 1000);
            seconds = seconds % 60;

            timerTextView.setText(getResources().getString(R.string.timer_next_round, seconds));
            newBoardHandler.postDelayed(this, 500);
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
        progressBar = findViewById(R.id.statsProgressBar);
        progressBar.setVisibility(View.VISIBLE);
        timerTextView = findViewById(R.id.scoreBoardTimer);
        startTime = System.currentTimeMillis();

        bestPlayersTextView.setText(getResources().getString(R.string.best_players,
                GameSettings.getGameModeName(UserSettings.getActiveGameMode())));

        // Audio handler
        audioHandler = new AudioHandler(this);
        soundIdFirst = audioHandler.addSoundToPool(R.raw.snd_score_first, 1);

        // Get data from previous game
        Intent intent = getIntent();
        String intentStr = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        highscoreData = parseHighScore(intentStr);
        playedBoard = BoardManager.getNextBoard();

        showPersonalScore(highscoreData);

        if(GameSettings.UseFirebase()) {
            mFireStore = FirebaseFirestore.getInstance();
            collectionPath = FIRESTORE_PREFIX + playedBoard.getBoardString();
            sessionReference = mFireStore.collection(collectionPath);
            firebaseAuth = FirebaseAuth.getInstance();
            highscoreData.setUserId(firebaseAuth.getUid());
        }

        // Initialize recyclerView
        RecyclerView scoreBoard = (RecyclerView) findViewById(R.id.scoreRecyclerView);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        scoreBoard.addItemDecoration(itemDecoration);

        // Set adapter for scoreboard
        scoreBoardAdapter = new ScoreboardAdapter(highScores);
        scoreBoard.setAdapter(scoreBoardAdapter);
        int scoreMax = ScoreUtils.getMaximumBoardScore(playedBoard);
        scoreBoardAdapter.setScoreMax(scoreMax);
        scoreBoardAdapter.setScoreImprovement(scoreImprovement);
        scoreBoardAdapter.setUserId(highscoreData.getUserId());
        scoreBoard.setLayoutManager(new LinearLayoutManager(this));

        // Fetch high score data from Firebase
        updateHighScores();

        if(BoardManager.getBoardQueueSize() > 0) {
           // Still have boards in the queue, no need to fetch anything from server
            BoardManager.setNextBoard();
        }

        else {
            // Create board in background while in main menu
            boardThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    BoardManager.generateBoard();
                }
            });

            boardThread.start();
        }

        scoreBoardHandler.postDelayed(this::startGame, 0);

        // Update stats
        updateUserStats();

        if(UserSettings.getDarkModeEnabled() > 0) {
            setDarkMode();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cancel all runnable and threads
        if(newBoardHandler != null && newBoardRunnable != null)
            newBoardHandler.removeCallbacks(newBoardRunnable);
        if(boardThread != null)
            boardThread.interrupt();
        if(scoreBoardHandler != null)
            scoreBoardHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        activityStopped = true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        activityStopped = false;

        if(boardThread == null || !boardThread.isAlive()) {
            startNewGame();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (shouldWriteStats) {
            Log.d(TAG, "Saving user stats");
            UserStatsManager.saveStats(getApplicationContext());
        }
    }

    // Shows info from users previous game
    private void showPersonalScore(HighscoreData data) {
        String text = getPerformanceText(data);
        scoreTextView.setText(TextUtils.getSpannedText(text));
    }

    private void startGame() {
        newBoardHandler.postDelayed(newBoardRunnable, 0);
    }

    // Start the MainActivity with a new board
    private void startNewGame() {
        limitScores();

        if(activityStopped) {
            return;
        }

        Intent intent = new Intent(ScoreboardActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_MESSAGE, username);
        startActivity(intent);
        boardThread = null;
        shouldWriteStats = false;
        finish();
    }

    // This may have to be removed for safety reasons (no valuable data is at danger here though)
    private void limitScores() {
        if(!GameSettings.UseFirebase())
            return;
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
        int foundWords = Integer.parseInt(dataArr[3]);

        if (dataArr.length > 4) {
            scoreImprovement = Integer.parseInt(dataArr[4]);
        }

        return new HighscoreData(username, score, bestWord, foundWords);
    }

    private void checkRanking() {
        int maxscore = ScoreUtils.getHighScore(highscoreData);
        for(HighscoreData data : highScores) {
            if(ScoreUtils.getHighScore(data) > maxscore) {
                return;
            }
        }

        // You are the first one !!!
        String text = new String(Character.toChars(0x1F451));
        text += getPerformanceText(highscoreData);
        scoreTextView.setText(TextUtils.getSpannedText(text));
        audioHandler.playSound(soundIdFirst, audioHandler.getVolume(), audioHandler.getVolume(), 1, 0, 1.0f);

        if (UserSettings.getActiveGameMode().equals("rational"))
            UserStatsManager.Instance.setFirstPlacesRational(UserStatsManager.Instance.getFirstPlacesRational() + 1);
        else
            UserStatsManager.Instance.setFirstPlaces(UserStatsManager.Instance.getFirstPlaces() + 1);
        UserStatsManager.userStatsSaved = false;
    }

    private String getPerformanceText(HighscoreData data) {
        if (UserSettings.getActiveGameMode().equals("rational")) {
            float percentage = (float)data.getFoundWords() / (float)playedBoard.getWords().size();
            return getResources().getString(R.string.scoreboard_info_rational,
                    data.getUserName(), data.getFoundWords(), percentage * 100, "%", data.getBestWord());
        }

        else {
            float percentage = (float)data.getScore() / (float)playedBoard.getMaxScore();
            return getResources().getString(R.string.scoreboard_info,
                    data.getUserName(), data.getScore(), percentage * 100, "%", data.getBestWord());
        }
    }

    private void updateUserStats() {
        if (UserSettings.getActiveGameMode().equals("rational")) {
            long totalScore = UserStatsManager.Instance.getScoreTotalRational();
            int numberOfGames = UserStatsManager.Instance.getNumberOfGamesRational();
            int highestScore = UserStatsManager.Instance.getHighestScoreRational();
            double highestPercentage = UserStatsManager.Instance.getHighestPercentageRational();

            UserStatsManager.Instance.setScoreTotalRational(totalScore + highscoreData.getFoundWords());
            UserStatsManager.Instance.setNumberOfGamesRational(numberOfGames + 1);
            UserStatsManager.Instance.setAverageScoreRational();

            if(highscoreData.getFoundWords() > highestScore)
                UserStatsManager.Instance.setHighestScoreRational(highscoreData.getFoundWords());

            float percentage = (float)highscoreData.getFoundWords() / (float)playedBoard.getWords().size();
            if(percentage > highestPercentage)
                UserStatsManager.Instance.setHighestPercentageRational(percentage);
        }
        else {
            long totalScore = UserStatsManager.Instance.getScoreTotal();
            int numberOfGames = UserStatsManager.Instance.getNumberOfGames();
            int highestScore = UserStatsManager.Instance.getHighestScore();
            double highestPercentage = UserStatsManager.Instance.getHighestPercentage();

            UserStatsManager.Instance.setScoreTotal(totalScore + highscoreData.getScore());
            UserStatsManager.Instance.setNumberOfGames(numberOfGames + 1);
            UserStatsManager.Instance.setAverageScore();

            if(highscoreData.getScore() > highestScore)
                UserStatsManager.Instance.setHighestScore(highscoreData.getScore());

            float percentage = (float)highscoreData.getScore() / (float)playedBoard.getMaxScore();
            if(percentage > highestPercentage)
                UserStatsManager.Instance.setHighestPercentage(percentage);
        }

        // Common stats for all game modes
        long totalGameTime = UserStatsManager.Instance.getTotalGameTime();
        UserStatsManager.Instance.setTotalGameTime(totalGameTime + GameSettings.getGameDuration());
        String longestWord = UserStatsManager.Instance.getLongestWord();

        if(highscoreData.getBestWord().length() > longestWord.length())
            UserStatsManager.Instance.setLongestWord(highscoreData.getBestWord());

        UserStatsManager.userStatsSaved = false;
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
        timerTextView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.rounded_corner_black));
        timerTextView.setTextColor(Color.WHITE);
    }

    public void updateHighScores() {
        if(!GameSettings.UseFirebase()) {
            progressBar.setVisibility(View.INVISIBLE);
            return;
        }

        String field = ScoreUtils.getScoreSortMetric();

        // Sort and get data from Firebase
        mFireStore.collection(collectionPath).orderBy(field, Query.Direction.DESCENDING)
                .limit(GameSettings.getScoreBoardMaxCount())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Log.d(TAG, "FIREBASE: Fetched high score data");
                        progressBar.setVisibility(View.GONE);
                        if(!task.isSuccessful()) {
                            Log.d(TAG, "Error retrieving high score data!");
                            Toast.makeText(ScoreboardActivity.this, R.string.error_scoreboard,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        highScores.clear();

                        QuerySnapshot snapshot = task.getResult();
                        List<DocumentSnapshot> documents = snapshot.getDocuments();

                        for(DocumentSnapshot document : documents) {
                            if(!document.exists()) {
                                Log.d(TAG, "Requested document does not exist");
                                continue;
                            }
                            HighscoreData data = document.toObject(HighscoreData.class);
                            highScores.add(data);
                            scoreBoardAdapter.notifyItemInserted(highScores.size() - 1);
                        }

                        // Don't check ranking if player is offline -> no cheating first places
                        if(highScores.size() <= 1 && !isNetworkConnected()) {
                            Log.d(TAG, "No network connection available!");
                            return;
                        }
                        checkRanking();
                    }
                });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}