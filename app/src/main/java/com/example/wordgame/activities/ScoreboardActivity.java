package com.example.wordgame.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wordgame.audio.AudioHandler;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.gamemode.GameMode;
import com.example.wordgame.gamemode.GameModeType;
import com.example.wordgame.managers.ApplicationManager;
import com.example.wordgame.managers.UserSettingsManager;
import com.example.wordgame.models.AppRemoteState;
import com.example.wordgame.models.Board;
import com.example.wordgame.managers.BoardManager;
import com.example.wordgame.models.BoardDataWrapper;
import com.example.wordgame.models.GameState;
import com.example.wordgame.models.HighScoreData;
import com.example.wordgame.R;
import com.example.wordgame.models.HighScoreDataTimeChase;
import com.example.wordgame.models.RemoteLoadType;
import com.example.wordgame.models.UserStats;
import com.example.wordgame.utility.ActivityUtils;
import com.example.wordgame.utility.AppConstants;
import com.example.wordgame.utility.FirebaseUtils;
import com.example.wordgame.ScoreboardAdapter;
import com.example.wordgame.utility.ScoreUtils;
import com.example.wordgame.utility.TextUtils;
import com.example.wordgame.managers.UserStatsManager;
import com.example.wordgame.utility.UserUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.base.Preconditions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScoreboardActivity extends AppCompatActivity implements
        IRemoteConfigInitListener, IConfigChangeListener {
    private static final String TAG = "ScoreboardActivity";

    // State
    private final List<HighScoreData> highScores = new ArrayList<>();
    private ScoreboardAdapter scoreBoardAdapter;
    private boolean activityStopped = false;
    private Board playedBoard = null;
    private boolean shouldWriteStats = true;
    private long startTime;

    // User values
    private HighScoreData currentScore;
    private HighScoreData previousHighScore;
    private int playedGameMaxScore;

    // Ui components
    private TextView scoreTextView;
    private TextView playedGameModeTextView;
    private TextView playedGameMaxScoreTextView;
    private ConstraintLayout scoreBoardLayout;
    private LinearLayout bestPlayersLayout;
    private ProgressBar progressBar;
    private TextView timerTextView;
    private View networkStatusIcon;
    private TextView offlineModeTextView;

    // Audio
    private AudioHandler audioHandler;
    private int soundIdFirst;

    // Firebase
    private FirebaseFirestore fireStore;
    private String collectionPath;
    private ApplicationManager applicationManager;
    private BoardManager boardManager;
    private UserStatsManager userStatsManager;
    private GameModeType gameModeType;
    private GameMode playedGameMode;

    private Context appCtx;
    final Handler scoreBoardHandler = new Handler();
    final Handler newBoardHandler = new Handler();
    final Runnable newBoardRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            millis = AppConstants.SCORE_BOARD_DURATION_MS - millis;

            if (millis < 0) {
                if (boardManager.isFetchingBoards()) {
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

        initUiComponents();

        // Get data from previous game
        Intent intent = getIntent();
        final String currentHighScoreData = intent.getStringExtra(MainActivity.EXTRA_MESSAGE_NEW_SCORE);
        final String prevHighScoreData = intent.getStringExtra(MainActivity.EXTRA_MESSAGE_PREV_SCORE);
        final String gameModeName = intent.getStringExtra(MainActivity.EXTRA_MESSAGE_GAME_MODE);

        gameModeType = GameModeType.valueOf(gameModeName);
        currentScore = parseHighScore(currentHighScoreData);
        previousHighScore = prevHighScoreData.isEmpty() ? null : parseHighScore(prevHighScoreData);

        appCtx = getApplicationContext();
        initApplicationManager();

        // Audio handler
        audioHandler = new AudioHandler(this);
        soundIdFirst = audioHandler.addSoundToPool(R.raw.snd_first_place, 1);

        startTime = System.currentTimeMillis();
        playedGameModeTextView.setText(AppConstants.getGameModeName(gameModeType));

        // Start next game timer
        scoreBoardHandler.postDelayed(this::startGame, 0);

        // Update stats
        updateUserStats();

        if (ActivityUtils.isDarkModeEnabled(applicationManager)) {
            setDarkMode();
        }
    }

    private void initUiComponents() {
        // UI Components
        scoreTextView = findViewById(R.id.personalStatTextView);
        scoreBoardLayout = findViewById(R.id.scoreboardLayout);
        bestPlayersLayout = findViewById(R.id.bestPlayersLayout);
        playedGameModeTextView = findViewById(R.id.gameModeNameText);
        playedGameMaxScoreTextView = findViewById(R.id.gameModeScoreText);
        progressBar = findViewById(R.id.statsProgressBar);
        progressBar.setVisibility(View.INVISIBLE);
        timerTextView = findViewById(R.id.scoreBoardTimer);
        networkStatusIcon = findViewById(R.id.networkStatusIcon);
        offlineModeTextView = findViewById(R.id.offlineModeText);
    }

    private void initApplicationManager() {
        applicationManager = ApplicationManager.getInstance(appCtx);
        userStatsManager = applicationManager.getUserStatsManager();
        applicationManager.addRemoteInitListener(this);
        if (applicationManager.getRemoteState() == AppRemoteState.READY) {
            initRemoteComponents(false);
            applicationManager.addRemoteChangeListener(this);
        } else if (applicationManager.getRemoteState() == AppRemoteState.OFFLINE) {
            initRemoteComponents(true);
        } else {
            Toast.makeText(ScoreboardActivity.this,
                    R.string.scoreboard_manager_instance_changed, Toast.LENGTH_SHORT).show();
        }
    }

    private void initRemoteComponents(boolean offlineMode) {
        boardManager = applicationManager.getBoardManager();
        BoardDataWrapper boardDataWrapper = boardManager.getActiveBoard();

        networkStatusIcon.setVisibility(offlineMode ? View.VISIBLE : View.GONE);

        if (boardDataWrapper != null) {
            playedBoard = boardDataWrapper.getBoard(gameModeType.getBoardWidth(),
                    gameModeType.getMinWordLength(), gameModeType.getMaxWordLength());
            playedGameMode = GameState.initGameMode(gameModeType, playedBoard);
            playedGameMaxScore = playedGameMode.getMaxScore();
            playedGameMaxScoreTextView.setText(getResources().getString(
                    R.string.scoreboard_round_max_score, playedGameMaxScore));

            showPersonalScore(currentScore);

            final boolean useScore = ScoreUtils.useScoreBoard(applicationManager) && !offlineMode;
            offlineModeTextView.setVisibility(useScore ? View.GONE : View.VISIBLE);
            if (useScore) {
                fireStore = FirebaseFirestore.getInstance();
                collectionPath = FirebaseUtils.getBoardCollectionId(gameModeType, playedBoard.getBoardString());
                initHighScoreView();
            }
        }

        setNextBoard();
    }

    private void setNextBoard() {
        Preconditions.checkState(boardManager != null, "Board manager is null");
        if (boardManager.hasBoardsRemaining()) {
                // Boards still remaining in the queue
                boardManager.dequeueBoard();
            } else {
                boardManager.requestBoards();
        }
    }

    private void initHighScoreView() {
        RecyclerView scoreBoard = findViewById(R.id.scoreRecyclerView);
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        scoreBoard.addItemDecoration(itemDecoration);

        // Set adapter for scoreboard
        scoreBoardAdapter = new ScoreboardAdapter(highScores, gameModeType, getResources());
        scoreBoardAdapter.setScoreMax(playedGameMaxScore);
        scoreBoard.setAdapter(scoreBoardAdapter);

        if (previousHighScore != null) {
            // TODO: Separate methods for different game modes?
            scoreBoardAdapter.setScoreImprovement(currentScore.getScore() - previousHighScore.getScore());
        }

        scoreBoard.setLayoutManager(new LinearLayoutManager(this));

        // Fetch high score data from Firebase
        fetchHighScores();
    }

    private HighScoreData parseHighScore(String intentStr) {
        HighScoreData highScore = gameModeType == GameModeType.TIME_CHASE
                ? HighScoreDataTimeChase.parseFromString(intentStr)
                : HighScoreData.parseFromString(intentStr);
        if (highScore == null) {
            Logger.getInstance().warn(TAG, "Invalid high score " + intentStr);
            throw new IllegalStateException("Invalid score data received");
        }
        return highScore;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        applicationManager.removeConfigInitListener(this);
        applicationManager.removeConfigChangeListener(this);

        // Cancel all runnables and threads
        if (newBoardHandler != null && newBoardRunnable != null)
            newBoardHandler.removeCallbacks(newBoardRunnable);
        if (scoreBoardHandler != null)
            scoreBoardHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        activityStopped = true;
        audioHandler.release();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        activityStopped = false;

        if (boardManager != null && boardManager.hasBoardsRemaining()) {
            startNewGame();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (shouldWriteStats) {
            Logger.getInstance().debug(TAG, "Saving user stats");
            userStatsManager.saveStats(appCtx);
        }
    }

    // Shows info from users previous game
    private void showPersonalScore(HighScoreData data) {
        String text = getPerformanceText(data);
        scoreTextView.setText(TextUtils.getSpannedText(text));
    }

    private void startGame() {
        newBoardHandler.postDelayed(newBoardRunnable, 0);
    }

    // Start the MainActivity with a new board
    private void startNewGame() {
        if (activityStopped) {
            return;
        }

        Intent intent = new Intent(ScoreboardActivity.this, MainActivity.class);
        intent.putExtra(MenuActivity.EXTRA_MESSAGE_USERNAME, currentScore.getUserName());

        GameModeType[] types = (GameModeType[]) Objects.requireNonNull(applicationManager.getUserSettingsManager()
                        .getSetting(UserSettingsManager.UserSetting.ACTIVE_GAME_MODES))
                .getValue();

        GameModeType nextGameMode = UserUtils.getRandomGameModeType(types);
        intent.putExtra(MainActivity.EXTRA_MESSAGE_GAME_MODE, nextGameMode.toString());

        startActivity(intent);
        shouldWriteStats = false;
        finish();
    }

    private void checkRanking() {
        for (HighScoreData score : highScores) {
            if (playedGameMode.compareHighScores(score, currentScore) > 0) {
                // Not the first
                return;
            }
        }

        // Player is the first
        String text = new String(Character.toChars(0x1F451));
        text += getPerformanceText(currentScore);
        scoreTextView.setText(TextUtils.getSpannedText(text));
        audioHandler.playSound(soundIdFirst, audioHandler.getVolume(), audioHandler.getVolume(),
                1, 0, 1.0f);

        updateFirstPlacesForGameMode(gameModeType);
    }

    private String getPerformanceText(HighScoreData data) {
        float percentage = (float) data.getScore() / (float) playedGameMaxScore;
        switch (gameModeType) {
            case NORMAL:
            case EXTENDED:
                return getResources().getString(R.string.scoreboard_info,
                        data.getUserName(), data.getScore(), percentage * 100, "%",
                        data.getBestWord());
            case RATIONAL:
                return getResources().getString(R.string.scoreboard_info_rational,
                        data.getUserName(), data.getFoundWords(), percentage * 100, "%",
                        data.getBestWord());
            case TIME_CHASE:
                HighScoreDataTimeChase dataTc = (HighScoreDataTimeChase) data;
                return getResources().getString(R.string.scoreboard_info_time, dataTc.getUserName(),
                        dataTc.getScore(), TextUtils.minSecFromLong(dataTc.getGameDuration()),
                        percentage * 100, "%", dataTc.getBestWord());
            default:
                throw new IllegalArgumentException("Unknown game mode " + gameModeType);
        }
    }

    private void updateUserStats() {
        final UserStats.Stat totalScoreIndex;
        final UserStats.Stat numberOfGamesIndex;
        final UserStats.Stat highestScoreIndex;
        final UserStats.Stat highestPercentageIndex;
        switch (gameModeType) {
            case NORMAL:
                totalScoreIndex = UserStats.Stat.SCORE_TOTAL_NORMAL;
                numberOfGamesIndex = UserStats.Stat.NUMBER_OF_GAMES_NORMAL;
                highestScoreIndex = UserStats.Stat.HIGHEST_SCORE_NORMAL;
                highestPercentageIndex = UserStats.Stat.PERCENTAGE_NORMAL;
                break;
            case RATIONAL:
                totalScoreIndex = UserStats.Stat.SCORE_TOTAL_RATIONAL;
                numberOfGamesIndex = UserStats.Stat.NUMBER_OF_GAMES_RATIONAL;
                highestScoreIndex = UserStats.Stat.HIGHEST_SCORE_RATIONAL;
                highestPercentageIndex = UserStats.Stat.PERCENTAGE_RATIONAL;
                break;
            case TIME_CHASE:
                totalScoreIndex = UserStats.Stat.SCORE_TOTAL_TIME;
                numberOfGamesIndex = UserStats.Stat.NUMBER_OF_GAMES_TIME;
                highestScoreIndex = UserStats.Stat.HIGHEST_SCORE_TIME;
                highestPercentageIndex = UserStats.Stat.PERCENTAGE_TIME;
                break;
            case EXTENDED:
                totalScoreIndex = UserStats.Stat.SCORE_TOTAL_EXTENDED;
                numberOfGamesIndex = UserStats.Stat.NUMBER_OF_GAMES_EXTENDED;
                highestScoreIndex = UserStats.Stat.HIGHEST_SCORE_EXTENDED;
                highestPercentageIndex = UserStats.Stat.PERCENTAGE_EXTENDED;
                break;
            default:
                throw new IllegalArgumentException("Unknown game mode " + gameModeType);
        }

        long totalScore = userStatsManager.getStatLong(totalScoreIndex);
        int numberOfGames = userStatsManager.getStatInt(numberOfGamesIndex);
        int highestScore = userStatsManager.getStatInt(highestScoreIndex);
        float highestPercentage = userStatsManager.getStatFloat(highestPercentageIndex);

        totalScore += currentScore.getScore();
        numberOfGames += 1;
        highestScore = Math.max(currentScore.getScore(), highestScore);

        highestPercentage = Math.max(highestPercentage,
                (float) currentScore.getScore() / (float) playedGameMaxScore);

        long totalGameTime = userStatsManager.getStatLong(UserStats.Stat.TOTAL_GAME_TIME);
        String longestWord = userStatsManager.getStat(UserStats.Stat.LONGEST_WORD);

        Logger.getInstance().debug(TAG, "Game duration of previous game was " + getGameDuration());

        totalGameTime += getGameDuration();
        if (currentScore.getBestWord().length() > longestWord.length()) {
            longestWord = currentScore.getBestWord();
        }

        userStatsManager.setStat(totalScoreIndex, String.valueOf(totalScore));
        userStatsManager.setStat(numberOfGamesIndex, String.valueOf(numberOfGames));
        userStatsManager.setStat(highestScoreIndex, String.valueOf(highestScore));
        userStatsManager.setStat(highestPercentageIndex, String.valueOf(highestPercentage));
        userStatsManager.setStat(UserStats.Stat.TOTAL_GAME_TIME, String.valueOf(totalGameTime));
        userStatsManager.setStat(UserStats.Stat.LONGEST_WORD, longestWord);

        setGameTypeSpecificStats();
    }

    private void setGameTypeSpecificStats() {
        switch (gameModeType) {
            case TIME_CHASE:
                final int prevLongestGame = userStatsManager.getStatInt(UserStats.Stat.LONGEST_GAME_TIME);
                // Game times are max a few minutes, this cast should always be safe
                final int currentGameDuration = (int) getGameDuration();
                if (currentGameDuration > prevLongestGame) {
                    userStatsManager.setStat(UserStats.Stat.LONGEST_GAME_TIME,
                            String.valueOf(currentGameDuration));
                }
                break;
            default:
                break;
        }
    }

    private void updateFirstPlacesForGameMode(GameModeType type) {
        UserStats.Stat statToUpdate;
        switch (type) {
            case NORMAL:
                statToUpdate = UserStats.Stat.FIRST_PLACES_NORMAL;
                break;
            case RATIONAL:
                statToUpdate = UserStats.Stat.FIRST_PLACES_RATIONAL;
                break;
            case TIME_CHASE:
                statToUpdate = UserStats.Stat.FIRST_PLACES_TIME;
                break;
            case EXTENDED:
                statToUpdate = UserStats.Stat.FIRST_PLACES_EXTENDED;
                break;
            default:
                throw new IllegalArgumentException("Not a valid game mode " + type);
        }

        int prevValue = Integer.parseInt(userStatsManager.getStat(statToUpdate));
        userStatsManager.setStat(statToUpdate, String.valueOf(prevValue + 1));
    }

    private long getGameDuration() {
        if (gameModeType == GameModeType.TIME_CHASE) {
            HighScoreDataTimeChase highScoreTC = (HighScoreDataTimeChase) currentScore;
            return highScoreTC.getGameDuration();
        } else {
            return GameState.initGameMode(gameModeType, playedBoard).getGameDuration();
        }
    }

    private void setDarkMode() {
        final int whiteColor = ContextCompat.getColor(getApplicationContext(),
                R.color.white);
        scoreBoardLayout.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.background_gradient_dark));
        bestPlayersLayout.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.rounded_corner_black));
        playedGameModeTextView.setTextColor(whiteColor);
        playedGameMaxScoreTextView.setTextColor(whiteColor);
        scoreTextView.setTextColor(whiteColor);
        RecyclerView scoreRecyclerView = findViewById(R.id.scoreRecyclerView);
        scoreRecyclerView.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.background_gradient_dark));
        timerTextView.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.rounded_corner_black));
        timerTextView.setTextColor(whiteColor);
        offlineModeTextView.setTextColor(whiteColor);
    }

    private void fetchHighScores() {
        Objects.requireNonNull(playedGameMode);

        progressBar.setVisibility(View.VISIBLE);
        int maxCount = applicationManager.getLocalConfig().getIntProperty("scoreBoardMaxPlayers");
        final String field = playedGameMode.getSortMetric();

        // Sort and get data from Firebase
        fireStore.collection(collectionPath).orderBy(field, Query.Direction.DESCENDING)
                .limit(maxCount)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Logger.getInstance().debug(TAG, "FIREBASE: Fetched high score data");
                        progressBar.setVisibility(View.GONE);
                        if (!task.isSuccessful()) {
                            Logger.getInstance().warn(TAG, "Error retrieving high score data!");
                            Toast.makeText(ScoreboardActivity.this, R.string.error_scoreboard,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        highScores.clear();

                        final QuerySnapshot snapshot = task.getResult();
                        final List<DocumentSnapshot> documents = snapshot.getDocuments();

                        for (DocumentSnapshot document : documents) {
                            if (!document.exists()) {
                                Logger.getInstance().warn(TAG, "FIREBASE: Requested document does not exist");
                                continue;
                            }
                            Class<? extends HighScoreData> dataClass =
                                    FirebaseUtils.getHighScoreDataClass(gameModeType);
                            HighScoreData data = document.toObject(dataClass);
                            highScores.add(data);
                            scoreBoardAdapter.notifyItemInserted(highScores.size() - 1);
                        }

                        // Don't check ranking if player is offline -> no cheating first places
                        if (highScores.size() <= 1 && !isNetworkConnected()) {
                            Logger.getInstance().debug(TAG,
                                    "No network connection available, skipping rankings");
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

    @Override
    public void onBeginRemoteConfigChange() {
        Toast.makeText(ScoreboardActivity.this, R.string.error_network_change_connection,
                        Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onRemoteConfigLoadSuccess(RemoteLoadType loadType) {
        initRemoteComponents(false);
    }

    @Override
    public void onRemoteConfigLoadFailure() {
        Toast.makeText(ScoreboardActivity.this, R.string.error_backup_connection_failed,
                Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onOfflineModeSet() {
        // NOP, cannot happen
    }
}