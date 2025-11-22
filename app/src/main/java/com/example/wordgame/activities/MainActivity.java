package com.example.wordgame.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wordgame.audio.AudioHandler;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.gamemode.GameModeType;
import com.example.wordgame.gamemode.models.RewardVisual;
import com.example.wordgame.gamemode.models.WordFoundResponse;
import com.example.wordgame.managers.ApplicationManager;
import com.example.wordgame.models.AppRemoteState;
import com.example.wordgame.models.Board;
import com.example.wordgame.managers.BoardManager;
import com.example.wordgame.models.BoardDataWrapper;
import com.example.wordgame.models.GameState;
import com.example.wordgame.models.HighScoreData;
import com.example.wordgame.R;
import com.example.wordgame.popups.WordDisplayPopup;
import com.example.wordgame.popups.WordListPopupView;
import com.example.wordgame.utility.ActivityUtils;
import com.example.wordgame.utility.AnimationCache;
import com.example.wordgame.utility.AppConstants;
import com.example.wordgame.utility.FirebaseUtils;
import com.example.wordgame.utility.ScoreUtils;
import com.example.wordgame.utility.TextUtils;
import com.example.wordgame.managers.UserSettingsManager;
import com.example.wordgame.webscraper.WebScraperService;
import com.example.wordgame.WordSolver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Extra message used to pass data to the scoreboard activity
    public static final String TAG = "WordGameMain";
    public static final String EXTRA_MESSAGE_NEW_SCORE =
            "com.example.wordgame.extra.NEW_SCORE";
    public static final String EXTRA_MESSAGE_PREV_SCORE =
            "com.example.wordgame.extra.PREV_SCORE";
    public static final String EXTRA_MESSAGE_GAME_MODE =
            "com.example.wordgame.extra.GAME_MODE";

    // Finals
    private final List<Button> tiles = new ArrayList<>();
    private final List<Button> selectedButtons = new ArrayList<>();
    private final AnimationCache animationCache = new AnimationCache();
    private final Random random = new Random();

    // UI components
    private TextView timerTextView;
    private LinearLayout linearLayout;
    private ScrollView foundWordsScrollView;
    private TextView scoreText;
    private LinearLayout wordsLayout;
    private ConstraintLayout baseLayout;
    private TextView gameModeTextView;
    private PopupWindow wordPopupWindow;
    private PopupWindow displayWordPopup;
    private PopupWindow wordDefinitionPopup;

    // Visual attributes
    private int foundWordColor = Color.DKGRAY;
    private int defaultLetterColor = Color.BLACK;
    private int highlightLetterColor = Color.BLACK;
    private int defaultTextColor = Color.BLACK;
    private Drawable defaultTile = null;

    // Game state
    private Board activeBoard;
    private String userName;
    private long startTime = 0;
    private volatile boolean gameActive = false;
    private boolean selectingActive = false;
    private String boardString;
    private boolean gameStopped = false;

    // Definition Web Scraper
    private Thread definitionThread = null;
    // Sound
    private AudioHandler audioHandler;
    private Map<Integer, Integer> audioMap;

    private int tickCounter = 0;

    // Firebase
    private FirebaseFirestore mFireStore;
    private CollectionReference sessionReference;
    private String collectionPath;
    private String fireBaseUid;
    private HighScoreData previousHighScore = null;

    private Context appCtx;
    private GameState gameState;
    private ApplicationManager applicationManager;

    private final Handler timerHandler = new Handler();

    // Runnable for timer
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long remainingTime = gameState.getEndTime() - System.currentTimeMillis();

            // Game has ended
            if (remainingTime < 0) {
                gameEnd();
                return;
            }

            int seconds = (int) (remainingTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextView.setText(getResources().getString(R.string.gameboard_timer, minutes, seconds));

            boolean nearEnd = remainingTime < gameState.getGameMode().getEndTimeLimit();
            timerTextView.setTextColor(nearEnd ? Color.RED : defaultTextColor);

            if (nearEnd && !gameStopped) {
                // Play tick sound every other iteration of this function call
                if (tickCounter % 2 == 0) {
                    audioHandler.playSound(audioMap.get(R.raw.tick), audioHandler.getVolume(),
                            audioHandler.getVolume(), 1, 0, 1f);
                }
                tickCounter++;
            }

            timerHandler.postDelayed(this, 500);
        }
    };

    final Handler scoreBoardHandler = new Handler();
    final Runnable scoreBoardRunnable = new Runnable() {
        @Override
        public void run() {
            if (definitionThread != null && definitionThread.isAlive()) {
                startTime = System.currentTimeMillis();
                scoreBoardHandler.postDelayed(this, 500);
                return;
            }

            if (wordDefinitionPopup != null) {
                startTime = System.currentTimeMillis();
                scoreBoardHandler.postDelayed(this, 500);
                return;
            }

            long millis = System.currentTimeMillis() - startTime;
            millis = AppConstants.SCORE_BOARD_DURATION_MS - millis;

            // Game has ended
            if (millis < 0 && !gameStopped) {
                enterScoreboard();
                return;
            }

            int seconds = (int) (millis / 1000) % 60;
            TextView scoreTimer = wordPopupWindow.getContentView().findViewById(R.id.endScoreTimer);
            scoreTimer.setText(getResources().getString(R.string.timer_scoreboard, seconds));
            scoreBoardHandler.postDelayed(this, 500);
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!initializeApplicationManager() || !isApplicationManagerValid()) {
            returnToMain();
            return;
        }

        initComponents();

        // Set UI component colors to match selected theme
        if (ActivityUtils.isDarkModeEnabled(applicationManager)) {
            setDarkMode();
        }

        // Get username from previous activity
        final Intent intent = getIntent();
        userName = intent.getStringExtra(MenuActivity.EXTRA_MESSAGE_USERNAME);

        final View networkStatusIcon = findViewById(R.id.networkStatusIcon);
        networkStatusIcon.setVisibility(applicationManager.getRemoteState() ==
                AppRemoteState.OFFLINE ? View.VISIBLE : View.GONE);

        GameModeType gameModeType = GameModeType.valueOf(
                intent.getStringExtra(MainActivity.EXTRA_MESSAGE_GAME_MODE));

        // Create board for the given game mode
        BoardDataWrapper wrapper = Objects.requireNonNull(
                applicationManager.getBoardManager().getActiveBoard());

        activeBoard = wrapper.getBoard(gameModeType.getBoardWidth(),
                gameModeType.getMinWordLength(), gameModeType.getMaxWordLength());

        displayCurrentGameMode(gameModeType);

        this.gameState = new GameState(gameModeType, activeBoard);


        final String boardString = activeBoard.getBoardString();
        updateScoreText();

        this.audioHandler = new AudioHandler(this);
        this.audioMap = this.gameState.getGameMode().registerSounds(this.audioHandler);

        this.startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        // Initialize game board
        final int boardWidth = activeBoard.getDimension();
        final int boardHeight = activeBoard.getDimension();
        initializeGameBoard(boardString, boardWidth, boardHeight);

        // Set buttons to proper size after layout is drawn
        linearLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                MainActivity.this.onGlobalLayout(this, boardWidth, boardHeight);
            }
        });

        gameActive = true;

        // Drag to select letters functionality
        linearLayout.setOnTouchListener((view, motionEvent) -> {
            if (gameActive) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN && !selectingActive) {
                    enterSelection(motionEvent);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    selectionMoved(motionEvent);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    exitSelection();
                }

                return true;
            }
            return false;
        });

        // Initialize firebase
        if (ScoreUtils.useScoreBoard(applicationManager)) {
            mFireStore = FirebaseFirestore.getInstance();
            collectionPath = FirebaseUtils.getBoardCollectionId(gameModeType, boardString);
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            fireBaseUid = firebaseAuth.getUid();
            getPreviousHighScore();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        timerHandler.removeCallbacks(timerRunnable);
        scoreBoardHandler.removeCallbacks(scoreBoardRunnable);

        if (wordPopupWindow != null) {
            wordPopupWindow.dismiss();
        }
        if (displayWordPopup != null) {
            displayWordPopup.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        gameStopped = true;

        applicationManager.getUserStatsManager().saveStats(appCtx);
        audioHandler.release();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.getInstance().debug(TAG, "Saving user stats");
        applicationManager.getUserStatsManager().saveStats(appCtx);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Logger.getInstance().debug(TAG, "Main Board OnRestart()");
        gameStopped = false;

        if (!gameActive) {
            returnToMain();
        }
    }

    private void initComponents() {
        linearLayout = findViewById(R.id.tileContainer);
        scoreText = findViewById(R.id.gameScoreTextView);
        timerTextView = findViewById(R.id.timerTextView);
        wordsLayout = findViewById(R.id.wordsLinearLayout);
        foundWordsScrollView = findViewById(R.id.wordsScrollView);
        baseLayout = findViewById(R.id.gameBaseLayout);
        gameModeTextView = findViewById(R.id.gameModeDisplayText);

        defaultTile = isScreenProtectionOn() && random.nextBoolean()
                ? AppCompatResources.getDrawable(this, R.drawable.tile_grey_alt)
                : AppCompatResources.getDrawable(this, R.drawable.tile_gray);

        // Apply new margins before content drawing
        final float marginMultiplier = (Float) this.applicationManager.getUserSettingsManager()
                .getSetting(UserSettingsManager.UserSetting.BOARD_SCALE).getValue();

        if (Math.abs(marginMultiplier - 1f) > 0.01f) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) linearLayout.getLayoutParams();
            final int leftMargin = (int) Math.round(layoutParams.leftMargin * marginMultiplier);
            final int rightMargin = (int) Math.round(layoutParams.rightMargin * marginMultiplier);

            layoutParams.setMargins(leftMargin, layoutParams.topMargin,
                    rightMargin, layoutParams.bottomMargin);
            linearLayout.setLayoutParams(layoutParams);
        }
    }

    private void onGlobalLayout(ViewTreeObserver.OnGlobalLayoutListener listener,
                                final int boardWidth, final int boardHeight) {
        linearLayout.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        linearLayout.setWeightSum(boardWidth);

        final int width = linearLayout.getWidth();
        final int height = linearLayout.getHeight();

        if (isScreenProtectionOn()) {
            modifyLayoutParameters();
        }

        int textSize = TextUtils.getTileTextSize(getWindowManager(),
                applicationManager.getUserSettingsManager());
        if (GameModeType.EXTENDED == gameState.getGameMode().getGameModeType()) {
            textSize = (int) Math.round(textSize * 0.8d);
        }
        final int sizeOffset = -4;  // in px
        for (Button btn : tiles) {
            btn.setWidth(width / boardWidth + sizeOffset);
            btn.setHeight(height / boardHeight + sizeOffset);
            btn.setTextSize(textSize);
        }
    }

    private void displayCurrentGameMode(GameModeType gameModeType) {
        final GameModeType[] activeGameModes = (GameModeType[]) applicationManager.getUserSettingsManager()
                .getSetting(UserSettingsManager.UserSetting.ACTIVE_GAME_MODES).getValue();

        // No need to display the game mode if it remains the same all the time
        if (activeGameModes.length <= 1) {
            gameModeTextView.setVisibility(View.GONE);
            return;
        }

        // Only called once, so no caching for these animations
        final Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        final Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        fadeOut.setStartOffset(2000L);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // NOP
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                gameModeTextView.startAnimation(fadeOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // NOP
            }
        });

        gameModeTextView.setText(AppConstants.getGameModeName(gameModeType));
        gameModeTextView.startAnimation(fadeIn);
    }

    /**
     * Initializes the application manager instance
     * @return true if remote configuration is fetched
     */
    private boolean initializeApplicationManager() {
        this.appCtx = getApplicationContext();
        applicationManager = ApplicationManager.getInstance(appCtx);

        final AppRemoteState remoteState = applicationManager.getRemoteState();
        return remoteState == AppRemoteState.READY
                || remoteState == AppRemoteState.OFFLINE;
    }

    private boolean isApplicationManagerValid() {
        // Failsafe - somehow we've ended up to main activity with no board manager
        if (applicationManager.getBoardManager() == null) {
            Toast.makeText(MainActivity.this, "BoardManager = null. THIS SHOULD NOT HAPPEN!",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (applicationManager.getBoardManager().getLatestError() != null) {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.error_general,
                            applicationManager.getBoardManager().getLatestError()),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Adding bias values to game board UI-elements, when screen protection is on
     * This makes sure elements are not always in the same place
     */
    private void modifyLayoutParameters() {
        final int widthBias = random.nextInt(4) * (random.nextBoolean() ? -1 : 1);
        final int heightBias = random.nextInt(4) * (random.nextBoolean() ? -1 : 1);

        ConstraintLayout.LayoutParams wordsLp = (ConstraintLayout.LayoutParams) foundWordsScrollView.getLayoutParams();
        ConstraintLayout.LayoutParams scoreLp = (ConstraintLayout.LayoutParams) scoreText.getLayoutParams();

        wordsLp.topMargin += heightBias * 5;
        scoreLp.topMargin += heightBias * 5;
        wordsLp.leftMargin += widthBias * 5;

        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) linearLayout.getLayoutParams();
        lp.topMargin += widthBias * 10;
        foundWordsScrollView.setLayoutParams(wordsLp);
        scoreText.setLayoutParams(scoreLp);
        linearLayout.setLayoutParams(lp);
    }

    // Create layout for the tile grid
    private void initializeGameBoard(String boardStr, int boardWidth, int boardHeight) {
        int strCounter = 0;

        boardString = randomizeBoardString(boardStr);
        linearLayout.setWeightSum(activeBoard.getDimension());
        for (int col = 0; col < boardHeight; col++) {
            LinearLayout horizontalLayout = new LinearLayout(this);
            horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(0, 0, 0, 0);
            horizontalLayout.setLayoutParams(lp);
            horizontalLayout.setWeightSum(4);

            linearLayout.addView(horizontalLayout);

            for (int row = 0; row < boardWidth; row++) {
                Button tile = new Button(this);
                tile.setClickable(false);
                tile.setMinimumWidth(0);
                tile.setMinimumHeight(1);
                tile.setTextColor(defaultLetterColor);

                tile.setBackground(defaultTile);
                String buttonText = Character.toString(boardString.charAt(strCounter));
                tile.setText(buttonText);
                horizontalLayout.addView(tile);
                selectedButtons.add(tile);
                tiles.add(tile);
                strCounter++;
            }
        }
    }

    /**
     * Creates a mirrored version of the game board to prevent same board showing up all the time
     * @param boardString the board to mirror
     * @return mirrored board
     */
    private String randomizeBoardString(String boardString) {
        final int randInt = random.nextInt(4);
        switch (randInt) {
            case 1:
                return new StringBuilder(boardString).reverse().toString();
            case 2:
                return getComplementBoard(boardString).toString();
            case 3:
                return getComplementBoard(boardString).reverse().toString();
            default:
                return boardString;
        }
    }

    private StringBuilder getComplementBoard(String boardString) {
        final StringBuilder sb = new StringBuilder();
        int dimension = (int) (Math.sqrt(boardString.length()));
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                sb.append(boardString.charAt(i + j * dimension));
            }
        }

        return sb;
    }

    // Logic to use, when user taps on the screen for the first time
    private void enterSelection(MotionEvent motionEvent) {
        selectingActive = true;
        for (Button btn : selectedButtons) {
            btn.setBackground(defaultTile);
            btn.setTextColor(defaultLetterColor);
        }
        selectedButtons.clear();

        // Error margins (can be this much outside the button center)
        int widthError = tiles.get(0).getWidth() / 2 - 10;
        int heightError = tiles.get(0).getHeight() / 2 - 10;

        for (Button tile : tiles) {
            int[] location = getCenterLocation(tile);
            if (Math.abs(location[0] - Math.round(motionEvent.getRawX())) < widthError &&
                    Math.abs(location[1] - Math.round(motionEvent.getRawY())) < heightError) {
                selectedButtons.add(tile);
                tile.setBackground(AppCompatResources.getDrawable(getApplicationContext(),
                        R.drawable.tile_blue));
                tile.setTextColor(highlightLetterColor);
                break;
            }
        }
    }

    // Called, when user moves the pointer while holding down (selecting tiles)
    private void selectionMoved(MotionEvent motionEvent) {
        // Error margins
        int widthError = tiles.get(0).getWidth() / 3;
        int heightError = tiles.get(0).getHeight() / 3;

        Button prevTile = null;
        if (selectedButtons.size() > 0) {
            prevTile = selectedButtons.get(selectedButtons.size() - 1);
        }
        for (Button tile : tiles) {
            int[] location = getCenterLocation(tile);

            // Only allow selection of buttons that are next to previous button
            if (prevTile != null) {
                int[] prevLocation = getCenterLocation(prevTile);
                if (Math.abs(location[0] - prevLocation[0]) > tile.getWidth() + widthError ||
                        Math.abs(location[1] - prevLocation[1]) > tile.getHeight() + heightError) {
                    continue;
                }
            }

            // Found valid tile to select
            if (Math.abs(location[0] - Math.round(motionEvent.getRawX())) < widthError &&
                    Math.abs(location[1] - Math.round(motionEvent.getRawY())) < heightError) {

                // User has selected a tile that has already been selected
                if (selectedButtons.contains(tile)) {
                    // Get buttons that come after re-selected tile
                    int tileIndex = 0;
                    for (int i = 0; i < selectedButtons.size(); i++) {
                        if (selectedButtons.get(i).equals(tile)) {
                            tileIndex = i;
                            break;
                        }
                    }

                    // Clear buttons that come after re-selected tile
                    if (tileIndex < selectedButtons.size() - 1) {
                        while (tileIndex <= selectedButtons.size() - 1) {
                            selectedButtons.get(tileIndex).setBackground(defaultTile);
                            selectedButtons.get(tileIndex).setTextColor(defaultLetterColor);
                            selectedButtons.remove(tileIndex);
                        }
                    }
                } else {
                    // Tile was valid and not previously selected
                    selectedButtons.add(tile);
                }

                tile.setBackground(AppCompatResources.getDrawable(getApplicationContext(),
                        R.drawable.tile_blue));
                tile.setTextColor(highlightLetterColor);
                break;
            }
        }
    }

    // Called, when user lifts the cursor/finger from the screen
    private void exitSelection() {
        // Attempt to form a word from the selected tiles
        final StringBuilder sb = new StringBuilder();
        for (Button button : selectedButtons) {
            sb.append(button.getText());
        }

        final String formedWord = sb.toString();

        Drawable drawable;
        final WordFoundResponse response = gameState.onWordOffered(formedWord);
        switch (response.getWordType()) {
            case INVALID:
                drawable = AppCompatResources.getDrawable(appCtx, R.drawable.tile_red);
                break;
            case VALID_OLD:
                drawable = AppCompatResources.getDrawable(appCtx, R.drawable.tile_blue);
                break;
            case VALID_NEW:
                drawable = AppCompatResources.getDrawable(appCtx, R.drawable.tile_green);
                addWordToList(formedWord);
                updateScoreText();
                playRewardAnimation(response);
                break;
            default:
                throw new IllegalArgumentException("Not a valid response " + response);
        }

        final int soundId =  Objects.requireNonNull(audioMap.get(response.getSoundId()));
        audioHandler.playSound(soundId, audioHandler.getVolume(), audioHandler.getVolume(),
                1, 0, 1f);

        for (TextView button : selectedButtons) {
            button.setBackground(drawable);
        }

        selectingActive = false;

        // Clear coloring after 1 second
        // TODO: Cosmetic issue when there is input between this delay
        new Handler().postDelayed(this::resetButtons, 1000);
    }

    private void addWordToList(String word) {
        final TextView wordView = new TextView(getApplicationContext());
        wordView.setTextColor(foundWordColor);

        String wordText = getResources().getString(R.string.gameboard_found_word,
                gameState.getGameMode().getWordReward(word).getScoreIncrement(), word);
        wordView.setText(TextUtils.getSpannedText(wordText));
        wordsLayout.addView(wordView);
    }

    private void updateScoreText() {
        updateScoreText(scoreText);
    }

    private void updateScoreText(TextView view) {
        view.setText(getResources().getString(R.string.gameboard_current_score,
                gameState.getCurrentScore(), gameState.getGameMode().getMaxScore()));
    }

    private void playRewardAnimation(WordFoundResponse response) {
        final RewardVisual rewardVisual = response.getRewardVisual();
        if (rewardVisual == null) {
            return;
        }

        final TextView view = findViewById(rewardVisual.getViewId());
        if (view == null) {
            return;
        }

        view.setText(rewardVisual.getText());
        if (rewardVisual.getAnimationId() != RewardVisual.UNINITIALIZED_ID) {
            final Animation animation = animationCache.get(this, rewardVisual.getAnimationId());
            view.startAnimation(animation);
            // TODO: Does this work for multiple animations?
        }
    }

    // Get the center point of the button
    private int[] getCenterLocation(Button button) {
        int[] location = new int[2];
        button.getLocationOnScreen(location);
        location[0] += button.getWidth() / 2;
        location[1] += button.getHeight() / 2;
        return location;
    }

    // Reset selected buttons to default color state
    private void resetButtons() {
        // TODO: Reset background of buttons that have not been selected
        updateFoundWordsPosition();
        if (selectingActive)
            return;

        for (Button btn : selectedButtons) {
            btn.setBackground(defaultTile);
            btn.setTextColor(defaultLetterColor);
        }
    }

    private void resetAllButtons() {
        for (Button btn : tiles) {
            btn.setBackground(defaultTile);
            btn.setTextColor(defaultLetterColor);
        }
    }

    // Scroll to the bottom of found words view to always show latest found word
    private void updateFoundWordsPosition() {
        View lastChild = foundWordsScrollView.getChildAt(foundWordsScrollView.getChildCount() - 1);
        int bottom = lastChild.getBottom() + foundWordsScrollView.getPaddingBottom();
        int sy = foundWordsScrollView.getScrollY();
        int sh = foundWordsScrollView.getHeight();
        int delta = bottom - (sy + sh);

        foundWordsScrollView.smoothScrollBy(0, delta);
    }

    private void highLightButtons(int[] buttons) {
        resetAllButtons();
        StringBuilder sb = new StringBuilder();
        for (int button : buttons) {
            sb.append(tiles.get(button).getText());
            tiles.get(button).setBackground(
                    AppCompatResources.getDrawable(getApplicationContext(), R.drawable.tile_blue));
        }
    }

    // Game end functionality
    @SuppressLint("ClickableViewAccessibility")
    private void gameEnd() {
        gameActive = false;

        resetAllButtons();
        createWordListPopup();

        // Update leaderboards
        updateLeaderBoards();

        startTime = System.currentTimeMillis();
        scoreBoardHandler.postDelayed(scoreBoardRunnable, 0);
    }

    // TODO: Refactor for new popup system
    @SuppressLint("ClickableViewAccessibility")
    private void createWordListPopup() {
        final WordSolver wordSolver = new WordSolver(boardString);

        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        WordListPopupView popup = new WordListPopupView(appCtx, inflater, null);
        popup.setScoreText(scoreText.getText());
        popup.setWords(this.gameState, new ArrayList<>(this.activeBoard.getWords()), getResources(),
                view -> {
                    if (wordPopupWindow.getContentView().getAlpha() < 0.9) {
                        return false;
                    }

                    final TextView textView = (TextView) view;
                    final int[] colorTiles = wordSolver.findWord(textView.getText().toString().toLowerCase());
                    highLightButtons(colorTiles);
                    textView.setTextColor(Color.BLUE);
                    wordPopupWindow.getContentView().setAlpha(0.1f);
                    createWordDisplayPopup(textView.getText().toString());
                    return true;
                });

        wordPopupWindow = new ActivityUtils.PopupWindowBuilder(popup)
                .withDimensionsWrapContent()
                .build();

        // Show words on board functionality
        wordPopupWindow.setTouchInterceptor((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                if (wordPopupWindow.getContentView().getAlpha() < 0.9) {
                    wordPopupWindow.getContentView().setAlpha(1);
                    resetAllButtons();
                    if (displayWordPopup != null) {
                        displayWordPopup.dismiss();
                    }
                }

                return true;
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                if (wordPopupWindow.getContentView().getAlpha() > 0.9) {
                    return false;
                }
                wordPopupWindow.getContentView().setAlpha(1);
                resetAllButtons();

                if (displayWordPopup != null)
                    displayWordPopup.dismiss();
                if (definitionThread != null && definitionThread.isAlive())
                    definitionThread.interrupt();
                return true;
            }
            return false;
        });
    }

    // Create a popup showing the currently highlighted word
    private void createWordDisplayPopup(String word) {
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        WordDisplayPopup popup = new WordDisplayPopup(appCtx, inflater, null);
        popup.setWord(word);
        final WebScraperService webScraperService = new WebScraperService(this);

        // Since word fetching uses Jsoup, we can't use network manager to offload network tasks
        // => Need to handle threads manually
        popup.addButtonListener(view -> {
            if (definitionThread != null) {
                return;
            }
            definitionThread = new Thread(() -> {
                final String content = webScraperService.getWordDefinition(word);
                runOnUiThread(() -> setWordDefinitionText(content));
            });

            definitionThread.start();
            createDefinitionPopup(word);
        });

        displayWordPopup = new ActivityUtils.PopupWindowBuilder(popup)
                .withDimensionsWrapContent()
                .withGravity(Gravity.BOTTOM)
                .withOffsets(0, 300)
                .build();
    }

    private void createDefinitionPopup(CharSequence word) {
        // Initialize the popup-window to show word definition
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.word_description_popup, null);

        TextView definitionHeader = popupView.findViewById(R.id.wordDefinitionHeader);
        ProgressBar progressBar = popupView.findViewById(R.id.wordDefinitionProgressBar);
        View definitionExitButton = popupView.findViewById(R.id.wordDefinitionExitButton);

        definitionHeader.setText(word);
        progressBar.setVisibility(ProgressBar.VISIBLE);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        wordDefinitionPopup = new PopupWindow(popupView, width, height, false);
        wordDefinitionPopup.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        wordDefinitionPopup.setOutsideTouchable(false);

        wordDefinitionPopup.showAtLocation(popupView, Gravity.CENTER, 0, 0);
        definitionExitButton.setOnClickListener(view -> {
            if (definitionThread.isAlive())
                definitionThread.interrupt();
            definitionThread = null;
            wordDefinitionPopup.dismiss();
            wordDefinitionPopup = null;
        });
    }

    private void setWordDefinitionText(String text) {
        if (wordDefinitionPopup != null) {
            wordDefinitionPopup.getContentView().findViewById(R.id.wordDefinitionProgressBar)
                    .setVisibility(ProgressBar.GONE);
            TextView textView = wordDefinitionPopup.getContentView().findViewById(R.id.wordDefinitionText);
            textView.setText(TextUtils.getSpannedText(text));
        }
    }

    // Start scoreboard activity
    private void enterScoreboard() {
        // Clear the popups (if exist)
        if (this.wordPopupWindow != null) {
            this.wordPopupWindow.dismiss();
            this.wordPopupWindow = null;

        }
        if (this.displayWordPopup != null) {
            this.displayWordPopup.dismiss();
            this.displayWordPopup = null;
        }

        String currentScore = generateHighScoreData(gameState.getScoreData()).toString();
        String oldScore = previousHighScore != null ? previousHighScore.toString() : "";

        final Intent intent = new Intent(MainActivity.this, ScoreboardActivity.class);
        intent.putExtra(EXTRA_MESSAGE_NEW_SCORE, currentScore);
        intent.putExtra(EXTRA_MESSAGE_PREV_SCORE, oldScore);
        intent.putExtra(EXTRA_MESSAGE_GAME_MODE, gameState.getGameMode().getGameModeType().name());
        startActivity(intent);
        finish();
    }

    private HighScoreData generateHighScoreData(HighScoreData scoreData) {
        scoreData.setUserName(userName);
        scoreData.setUserId(fireBaseUid);
        return scoreData;
    }

    // Return to main menu
    private void returnToMain() {
        BoardManager boardManager = applicationManager.getBoardManager();

        // Board manager not initialized if remote config is not accessible
        if (boardManager != null) {
            boardManager.clearActiveBoard();
        }

        finish();
    }

    // Get the user's previous high score on this game board
    private void getPreviousHighScore() {
        // Not using Firebase, or previous high score is already fetched
        if (!ScoreUtils.useScoreBoard(applicationManager) || previousHighScore != null) {
            return;
        }

        final GameModeType gameModeType = gameState.getGameMode().getGameModeType();
        final String documentName = FirebaseUtils.getDocumentId(fireBaseUid, gameModeType);

        sessionReference = mFireStore.collection(collectionPath);
        sessionReference.document(documentName).get().addOnSuccessListener(documentSnapshot -> {
            Logger.getInstance().debug(TAG, "FIREBASE: Fetched high score data");
            Class<? extends HighScoreData> dataClass =
                    FirebaseUtils.getHighScoreDataClass(gameModeType);
            HighScoreData previousData = documentSnapshot.toObject(dataClass);
            if (previousData != null) {
                previousHighScore = previousData;
            }
        });
    }

    // Update user data after game has ended
    private void updateLeaderBoards() {
        if (!ScoreUtils.useScoreBoard(applicationManager)
                || applicationManager.getRemoteState() == AppRemoteState.OFFLINE) {
            return;
        }

        HighScoreData data = generateHighScoreData(gameState.getScoreData());

        // Don't update score table, if previous score is better
        if (previousHighScore != null && gameState.getGameMode()
                .compareHighScores(data, previousHighScore) < 0) {
            return;
        }

        sessionReference = mFireStore.collection(collectionPath);
        sessionReference.document(fireBaseUid).set(data);
        Logger.getInstance().debug(TAG, "FIREBASE: Wrote high score data");
    }

    private boolean isScreenProtectionOn() {
        return (Boolean) Objects.requireNonNull(
                applicationManager.getUserSettingsManager().getSetting(
                        UserSettingsManager.UserSetting.SCREEN_PROTECTION)).getValue();
    }

    private void setDarkMode() {
        foundWordColor = Color.WHITE;
        defaultLetterColor = Color.WHITE;
        highlightLetterColor = Color.BLACK;
        defaultTextColor = Color.WHITE;
        defaultTile = AppCompatResources.getDrawable(this, R.drawable.tile_black);

        baseLayout.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.background_gradient_dark));

        foundWordsScrollView.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.rounded_corner_black));
        linearLayout.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.rounded_corner_black));

        scoreText.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.rounded_corner_black));
        scoreText.setTextColor(Color.WHITE);
        timerTextView.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                R.drawable.rounded_corner_black));
        timerTextView.setTextColor(Color.WHITE);
        gameModeTextView.setTextColor(Color.WHITE);
    }
}