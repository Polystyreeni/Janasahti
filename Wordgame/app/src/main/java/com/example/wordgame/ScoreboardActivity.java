package com.example.wordgame;

import androidx.annotation.NonNull;
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

    // User values
    private String username;
    private HighscoreData highscoreData;

    // Ui components
    private TextView scoreTextView;
    private TextView bestPlayersTextView;
    private ConstraintLayout scoreBoardLayout;

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
            if(boardThread != null && boardThread.isAlive())
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
        scoreBoardAdapter.setScoreMax(playedBoard.getMaxScore());
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

        scoreBoardHandler.postDelayed(this::startGame, GameSettings.getScoreBoardDuration());

        // Update stats
        updateUserStats();

        if(UserSettings.getDarkModeEnabled() > 0) {
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

    @Override
    protected void onStop() {
        super.onStop();
        activityStopped = true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        activityStopped = false;

        // Return to main menu if
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

        Log.d(TAG, "Starting new game");
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

        return new HighscoreData(username, score, bestWord);
    }

    private void checkRanking() {
        int maxscore = highscoreData.getScore();
        for(HighscoreData data : highScores) {
            if(data.getScore() > maxscore) {
                return;
            }
        }

        // You are the first one !!!
        String text = new String(Character.toChars(0x1F451));
        text += getPerformanceText(highscoreData);
        scoreTextView.setText(TextUtils.getSpannedText(text));
        audioHandler.playSound(soundIdFirst, audioHandler.getVolume(), audioHandler.getVolume(), 1, 0, 1.0f);
        UserStatsManager.Instance.setFirstPlaces(UserStatsManager.Instance.getFirstPlaces() + 1);
        UserStatsManager.userStatsSaved = false;
    }

    private String getPerformanceText(HighscoreData data) {
        float percentage = (float)data.getScore() / (float)playedBoard.getMaxScore();

        return getResources().getString(R.string.scoreboard_info,
                data.getUserName(), data.getScore(), percentage * 100, "%", data.getBestWord());
    }

    private void updateUserStats() {
        long totalScore = UserStatsManager.Instance.getScoreTotal();
        int numberOfGames = UserStatsManager.Instance.getNumberOfGames();
        long totalGameTime = UserStatsManager.Instance.getTotalGameTime();
        int highestScore = UserStatsManager.Instance.getHighestScore();
        double highestPercentage = UserStatsManager.Instance.getHighestPercentage();
        String longestWord = UserStatsManager.Instance.getLongestWord();

        UserStatsManager.Instance.setScoreTotal(totalScore + highscoreData.getScore());
        UserStatsManager.Instance.setNumberOfGames(numberOfGames + 1);
        UserStatsManager.Instance.setTotalGameTime(totalGameTime + GameSettings.getGameDuration());

        if(highscoreData.getScore() > highestScore)
            UserStatsManager.Instance.setHighestScore(highscoreData.getScore());
        if(highscoreData.getBestWord().length() > longestWord.length())
            UserStatsManager.Instance.setLongestWord(highscoreData.getBestWord());

        float percentage = (float)highscoreData.getScore() / (float)playedBoard.getMaxScore();
        if(percentage > highestPercentage)
            UserStatsManager.Instance.setHighestPercentage(percentage);

        UserStatsManager.Instance.setAverageScore();
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
    }

    public void updateHighScores() {
        if(!GameSettings.UseFirebase())
            return;

        // Sort and get data from Firebase
        mFireStore.collection(collectionPath).orderBy("score", Query.Direction.DESCENDING)
                .limit(GameSettings.getScoreBoardMaxCount())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(!task.isSuccessful()) {
                            Log.d(TAG, "Error retrieving high score data!");
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
                            Log.d(TAG, "Fetched highscore data from firebase");
                        }
                        checkRanking();
                    }
                });

    }

}