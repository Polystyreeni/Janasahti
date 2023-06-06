package com.example.wordgame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private Board activeBoard;

    // Extra message used to pass data to the scoreboard activity
    public static final String TAG = "WordgameMain";

    public static final String EXTRA_MESSAGE =
            "com.example.wordgame.extra.MESSAGE";

    // Prefix for all boards in database
    private final String FIRESTORE_PREFIX = "wg_board_";

    // UI components
    private TextView timerTextView;
    private LinearLayout linearLayout;
    private final List<Button> tiles = new ArrayList<>();
    private final List<Button> selectedButtons = new ArrayList<>();
    private ScrollView foundWordsScrollView;
    private TextView scoreText;
    private LinearLayout wordsLayout;
    private ConstraintLayout baseLayout;
    private PopupWindow wordPopupWindow;
    private PopupWindow displayWordPopup;
    private PopupWindow wordDefinitionPopup;

    // Visual attributes
    private int foundWordColor = Color.DKGRAY;
    private int defaultLetterColor = Color.BLACK;
    private int highlightLetterColor = Color.BLACK;
    private Drawable defaultTile = null;

    // Game state
    private String username;
    private int currentScore = 0;
    private long startTime = 0;
    private boolean gameActive = false;
    private boolean selectingActive = false;
    private final HashSet<String> wordsFound = new HashSet<>();
    private String boardString;
    private Random random;
    private boolean gameStopped = false;
    private boolean shouldWriteStats = true;

    // Definition Web Scraper
    private Thread definitionThread = null;

    // Sound
    private AudioHandler audioHandler;
    private int soundIdTimer;
    private int soundIdAccept;
    private int soundIdDeny;
    private int tickCounter = 0;

    // Firebase
    private FirebaseFirestore mFireStore;
    private CollectionReference sessionReference;
    private FirebaseAuth firebaseAuth;
    private String collectionPath;
    private String fireBaseUid;
    private int previousHighScore = 0;

    Handler timerHandler = new Handler();

    // Runnable for timer
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            millis = GameSettings.getGameDuration() - millis;

            // Game has ended
            if(millis < 0) {
                gameEnd();
                return;
            }

            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            //timerTextView.setText(String.format("%d:%02d", minutes, seconds));
            timerTextView.setText(getResources().getString(R.string.gameboard_timer, minutes, seconds));
            if(millis < 10000 && !gameStopped) {
                timerTextView.setTextColor(Color.RED);

                // Play tick sound every other iteration of this function call
                if(tickCounter % 2 == 0) {
                    //soundPool.play(soundIdTimer, volume, volume, 1,0, 1f);
                    audioHandler.playSound(soundIdTimer, audioHandler.getVolume(), audioHandler.getVolume(), 1, 0, 1f);
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
                Log.d(TAG, "Skipping timer");
                startTime = System.currentTimeMillis();
                scoreBoardHandler.postDelayed(this, 500);
                return;
            }

            if (wordDefinitionPopup != null) {
                Log.d(TAG, "Skipping timer");
                startTime = System.currentTimeMillis();
                scoreBoardHandler.postDelayed(this, 500);
                return;
            }

            long millis = System.currentTimeMillis() - startTime;
            millis = GameSettings.getScoreBoardDuration() - millis;

            // Game has ended
            if(millis < 0 && !gameStopped) {
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

        // Find UI components
        linearLayout = findViewById(R.id.linear_vert);
        scoreText = findViewById(R.id.gameScoreTextView);
        timerTextView = findViewById(R.id.timerTextView);
        wordsLayout = findViewById(R.id.wordsLinearLayout);
        foundWordsScrollView = findViewById(R.id.wordsScrollView);
        baseLayout = findViewById(R.id.gameBaseLayout);

        defaultTile = AppCompatResources.getDrawable(this, R.drawable.wordgame_tile);

        // Set UI component colors to match selected theme
        if(UserSettings.getDarkModeEnabled() > 0) {
            setDarkMode();
        }

        // Get username from previous activity
        Intent intent = getIntent();
        username = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        activeBoard = BoardManager.getNextBoard();
        BoardManager.setShouldGenerateBoard(true);

        random = new Random();
        random.setSeed(Calendar.getInstance().getTimeInMillis());

        // Receiving board failed, go back to main menu
        if(activeBoard == null) {
            String errorMessage = getApplicationContext().getString(R.string.general_error) + BoardManager.getLatestError();
            Toast toast = Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG);
            toast.show();

            returnToMain();
            return;
        }

        String boardString = activeBoard.getBoardString();
        int maxScore = UserSettings.getActiveGameMode().equals("rational") ? activeBoard.getWords().size() : activeBoard.getMaxScore();
        scoreText.setText(getResources().getString(R.string.gameboard_current_score, currentScore,
                maxScore));

        audioHandler = new AudioHandler(this);
        soundIdTimer = audioHandler.addSoundToPool(R.raw.tick, 1);
        soundIdAccept = audioHandler.addSoundToPool(R.raw.snd_word_accept, 1);
        soundIdDeny = audioHandler.addSoundToPool(R.raw.snd_word_deny, 1);

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        // Initialize game board
        final int boardWidth = 4;
        final int boardHeight = 4;
        initializeGameBoard(boardString, boardWidth, boardHeight);

        // Set buttons to proper size after layout is drawn
        linearLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                linearLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = linearLayout.getWidth();
                int height = linearLayout.getHeight();

                if(UserSettings.getOledProtectionEnabled() > 0) {
                    modifyLayoutParameters();
                }

                int textSize = TextUtils.getTileTextSize(getWindowManager());
                for(Button btn : tiles) {
                    btn.setWidth(width / boardWidth);
                    btn.setHeight(height / boardHeight);
                    btn.setTextSize(textSize);
                }
            }
        });

        gameActive = true;

        // Drag to select letters functionality
        linearLayout.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(gameActive) {
                    if(motionEvent.getAction() == MotionEvent.ACTION_DOWN && !selectingActive) {
                        enterSelection(motionEvent);
                    }

                    else if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        selectionMoved(motionEvent);
                    }

                    else if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        exitSelection(motionEvent);
                    }

                    return true;
                }
                return false;
            }
        });

        // Initialize firebase
        if(GameSettings.UseFirebase()) {
            mFireStore = FirebaseFirestore.getInstance();
            collectionPath = FIRESTORE_PREFIX + activeBoard.getBoardString();
            firebaseAuth = FirebaseAuth.getInstance();
            fireBaseUid = firebaseAuth.getUid();
            getPreviousHighScore();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Main Board OnDestroy");
        timerHandler.removeCallbacks(timerRunnable);
        scoreBoardHandler.removeCallbacks(scoreBoardRunnable);

        if(wordPopupWindow != null) {
            wordPopupWindow.dismiss();
        }
        if(displayWordPopup != null) {
            displayWordPopup.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "Main Board OnStop()");
        gameStopped = true;

        if(shouldWriteStats) {
            Log.d(TAG, "Saving user stats");
            UserStatsManager.saveStats(getApplicationContext());
            shouldWriteStats = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (shouldWriteStats) {
            Log.d(TAG, "Saving user stats");
            UserStatsManager.saveStats(getApplicationContext());
            shouldWriteStats = false;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Log.d(TAG, "Main Board OnRestart()");
        gameStopped = false;

        if(!gameActive) {
            returnToMain();
        }
    }

    /**
     * Adding bias values to game board UI-elements, when OLED-protection mode is on
     * This makes sure elements are not always in the same place
     */
    private void modifyLayoutParameters() {
        int widthBias = random.nextInt(5);
        int heightBias = random.nextInt(5);

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
        linearLayout.setWeightSum(4);
        for(int col = 0; col < boardHeight; col++) {
            LinearLayout horzLayout = new LinearLayout(this);
            horzLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(0, 0, 0, 0);
            horzLayout.setLayoutParams(lp);
            horzLayout.setWeightSum(4);

            linearLayout.addView(horzLayout);

            int textSize = TextUtils.getTileTextSize(getWindowManager());
            for(int row = 0; row < boardWidth; row++) {
                Button tile = new Button(this);
                tile.setTextSize(textSize);
                tile.setClickable(false);
                tile.setMinimumWidth(0);
                tile.setMinimumHeight(1);
                tile.setTextColor(defaultLetterColor);

                tile.setBackground(defaultTile);
                String buttonText = Character.toString(boardString.charAt(strCounter));
                tile.setText(buttonText);
                horzLayout.addView(tile);
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
        int randInt = random.nextInt(100);
        if(randInt < 25) {
            return boardString;
        }
        else if(randInt > 25 && randInt < 50) {
            return new StringBuilder(boardString).reverse().toString();
        }
        else if(randInt > 50 && randInt < 75) {
            return getComplementBoard(boardString).toString();
        }
        else {
            return getComplementBoard(boardString).reverse().toString();
        }
    }

    private StringBuilder getComplementBoard(String boardString) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                sb.append(boardString.charAt(i + j * 4));
            }
        }

        return sb;
    }

    // Logic to use, when user taps on the screen for the first time
    private void enterSelection(MotionEvent motionEvent) {
        selectingActive = true;
        for(Button btn : selectedButtons) {
            btn.setBackground(defaultTile);
            btn.setTextColor(defaultLetterColor);
        }
        selectedButtons.clear();

        // Error margins (can be this much outside the button center)
        int widthError = tiles.get(0).getWidth() / 2 - 10;
        int heightError = tiles.get(0).getHeight() / 2 - 10;

        for(Button tile : tiles) {
            int[] location = getCenterLocation(tile);
            if(Math.abs(location[0] - Math.round(motionEvent.getRawX())) < widthError &&
                    Math.abs(location[1] - Math.round(motionEvent.getRawY())) < heightError) {
                selectedButtons.add(tile);
                tile.setBackground(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.wordgame_tile_blue));
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
        if(selectedButtons.size() > 0) {
            prevTile = selectedButtons.get(selectedButtons.size() - 1);
        }
        for(Button tile : tiles) {
            int[] location = getCenterLocation(tile);

            // Only allow selection of buttons that are next to previous button
            if(prevTile != null) {
                int[] prevLocation = getCenterLocation(prevTile);
                if(Math.abs(location[0] - prevLocation[0]) > tile.getWidth() + widthError ||
                        Math.abs(location[1] - prevLocation[1]) > tile.getHeight() + heightError) {
                    continue;
                }
            }

            // Found valid tile to select
            if(Math.abs(location[0] - Math.round(motionEvent.getRawX())) < widthError &&
                    Math.abs(location[1] - Math.round(motionEvent.getRawY())) < heightError) {

                // User has selected a tile that has already been selected
                if(selectedButtons.contains(tile)) {
                    // Get buttons that come after re-selected tile
                    int tileIndex = 0;
                    for(int i = 0; i < selectedButtons.size(); i++) {
                        if(selectedButtons.get(i).equals(tile)) {
                            tileIndex = i;
                            break;
                        }
                    }

                    // Clear buttons that come after re-selected tile
                    if(tileIndex < selectedButtons.size() - 1) {
                        while(tileIndex <= selectedButtons.size() - 1) {
                            selectedButtons.get(tileIndex).setBackground(defaultTile);
                            selectedButtons.get(tileIndex).setTextColor(defaultLetterColor);
                            selectedButtons.remove(tileIndex);
                        }
                    }
                }
                // Tile was valid and not previously selected
                else {
                    selectedButtons.add(tile);
                }

                tile.setBackground(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.wordgame_tile_blue));
                tile.setTextColor(highlightLetterColor);
                break;
            }
        }
    }

    // Called, when user lifts the cursor/finger from the screen
    private void exitSelection(MotionEvent motionEvent) {
        // Attempt to form a word from the selected tiles
        StringBuilder sb = new StringBuilder();
        for(Button button : selectedButtons) {
            sb.append(button.getText());
        }
        String formedWord = sb.toString();

        // Check if this word is valid
        ArrayList<String> words = activeBoard.getWords();
        boolean isValidWord = false;

        if(words.contains(formedWord) && !wordsFound.contains(formedWord)) {
            TextView wordView = new TextView(getApplicationContext());
            wordView.setTextColor(foundWordColor);

            String wordText = getResources().getString(R.string.gameboard_found_word,
                    GameSettings.getScoreForLength(formedWord.length()), formedWord);
            wordView.setText(TextUtils.getSpannedText(wordText));
            wordsLayout.addView(wordView);
            currentScore += GameSettings.getScoreForLength(formedWord.length());
            int maxScore = UserSettings.getActiveGameMode().equals("rational") ? activeBoard.getWords().size() : activeBoard.getMaxScore();
            scoreText.setText(getResources().getString(R.string.gameboard_current_score, currentScore,
                    maxScore));
            wordsFound.add(formedWord);
            isValidWord = true;
        }

        // Select drawable & sound to display
        Drawable drawable =  isValidWord ? AppCompatResources.getDrawable(getApplicationContext(), R.drawable.wordgame_tile_green)
                : AppCompatResources.getDrawable(getApplicationContext(), R.drawable.wordgame_tile_red);
        if(!isValidWord && wordsFound.contains(formedWord))
            drawable = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.wordgame_tile_blue);

        int soundId = isValidWord ? soundIdAccept : soundIdDeny;
        audioHandler.playSound(soundId, audioHandler.getVolume(), audioHandler.getVolume(), 1, 0, 1f);

        for(TextView button : selectedButtons) {
            button.setBackground(drawable);
        }

        selectingActive = false;

        // Clear coloring after 1 second
        new Handler().postDelayed(this::resetButtons, 1000);
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
        updateFoundWordsPosition();
        if(selectingActive)
            return;

        for(Button btn : selectedButtons) {
            btn.setBackground(defaultTile);
            btn.setTextColor(defaultLetterColor);
        }
    }

    private void resetAllButtons() {
        for(Button btn : tiles) {
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
                    AppCompatResources.getDrawable(getApplicationContext(), R.drawable.wordgame_tile_blue));
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

        // scoreBoardHandler.postDelayed(scoreBoardRunnable, GameSettings.getScoreBoardDuration());
        startTime = System.currentTimeMillis();
        scoreBoardHandler.postDelayed(scoreBoardRunnable, 0);
    }

    private void createWordListPopup() {
        // Initialize the popup-window to show all words and score
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.game_end_popup, null);

        // Display user score and max score
        TextView scoreText = popupView.findViewById(R.id.endScoreTextView);
        int maxScore = UserSettings.getActiveGameMode().equals("rational") ? activeBoard.getWords().size() : activeBoard.getMaxScore();
        scoreText.setText(getResources().getString(R.string.gameboard_current_score, currentScore,
                maxScore));
        // scoreText.setText(String.format("%d / %d", currentScore, activeBoard.getMaxScore()));

        // Show all words
        LinearLayout layout = popupView.findViewById(R.id.endScoreWordLayout);
        ArrayList<String> words = activeBoard.getWords();
        Collections.sort(words, this::sortWords);

        // Group words by their length
        int previousLen = 0;
        for(String word : activeBoard.getWords()) {
            if(word.length() != previousLen) {
                TextView header = new TextView(this);
                header.setText(getResources().getString(R.string.wordlist_word_len, word.length()));
                header.setTextColor(Color.BLACK);
                header.setAllCaps(true);
                header.setTypeface(header.getTypeface(), Typeface.BOLD);
                layout.addView(header);
                previousLen = word.length();
            }
            TextView view = new TextView(this);
            view.setAllCaps(true);
            view.setText(word);
            view.setPadding(0, 1, 0, 1);
            int textColor = wordsFound.contains(word) ? getColor(R.color.dark_green) : Color.BLACK;
            view.setTextColor(textColor);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(wordPopupWindow.getContentView().getAlpha() < 0.9)
                        return false;
                    int[] colorTiles = WordSolver.getTiles(word, boardString);
                    highLightButtons(colorTiles);
                    TextView textView = (TextView)view;
                    textView.setTextColor(Color.BLUE);
                    wordPopupWindow.getContentView().setAlpha(0.1f);
                    CreateWordDisplayPopup(textView.getText());
                    return true;
                }
            });
            layout.addView(view);
        }

        // Show popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        wordPopupWindow = new PopupWindow(popupView, width, height, false);
        wordPopupWindow.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        wordPopupWindow.setOutsideTouchable(true);

        // Show words on board functionality
        wordPopupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    if(wordPopupWindow.getContentView().getAlpha() < 0.9) {
                        wordPopupWindow.getContentView().setAlpha(1);
                        resetAllButtons();
                        if(displayWordPopup != null) {
                            displayWordPopup.dismiss();
                        }
                    }

                    return true;
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if(wordPopupWindow.getContentView().getAlpha() > 0.9)
                        return false;

                    wordPopupWindow.getContentView().setAlpha(1);
                    resetAllButtons();

                    if(displayWordPopup != null)
                        displayWordPopup.dismiss();
                    if (definitionThread != null && definitionThread.isAlive())
                        definitionThread.interrupt();
                    return true;
                }
                return false;
            }
        });

        wordPopupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
    }

    // Create a popup showing the currently highlighted word
    private void CreateWordDisplayPopup(CharSequence word) {
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.found_word_popup, null);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        displayWordPopup = new PopupWindow(popupView, width, height, false);

        displayWordPopup.setAnimationStyle(R.style.Animation_AppCompat_Tooltip);
        TextView textView = popupView.findViewById(R.id.displayWordTextView);
        textView.setText(word);

        Button definitionButton = popupView.findViewById(R.id.definitionSearchButton);
        definitionButton.setOnClickListener(view -> {
            Log.d(TAG, "Definition button onClick()");
            definitionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    WordDefinitionService.getWordDefinition(word.toString());
                }
            });
            definitionThread.start();
            createDefinitionPopup(word);
        });

        //displayWordPopup.showAsDropDown(wordPopupWindow.getContentView());
        displayWordPopup.showAtLocation(popupView, Gravity.BOTTOM, 0, 300);
    }

    private void createDefinitionPopup(CharSequence word) {
        // Initialize the popup-window to show all words and score
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.word_description_popup, null);

        TextView definitionHeader = popupView.findViewById(R.id.wordDefinitionHeader);
        ProgressBar progressBar = popupView.findViewById(R.id.wordDefinitionProgressBar);
        TextView definitionExitButton = popupView.findViewById(R.id.wordDefinitionExitButton);

        definitionHeader.setText(word);
        progressBar.setVisibility(ProgressBar.VISIBLE);

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        wordDefinitionPopup = new PopupWindow(popupView, width, height, false);
        wordDefinitionPopup.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        wordDefinitionPopup.setOutsideTouchable(false);

        wordDefinitionPopup.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        Log.d(TAG, "Created definition popup");

        definitionExitButton.setOnClickListener(view -> {
            if (definitionThread.isAlive())
                definitionThread.interrupt();
            wordDefinitionPopup.dismiss();
            wordDefinitionPopup = null;
            // startTime = System.currentTimeMillis();
        });

        try {
            definitionThread.join();
            wordDefinitionPopup.getContentView().findViewById(R.id.wordDefinitionProgressBar).setVisibility(ProgressBar.GONE);
            TextView textView = wordDefinitionPopup.getContentView().findViewById(R.id.wordDefinitionText);
            textView.setText(TextUtils.getSpannedText(WordDefinitionService.getDefinitionText()));

        } catch (InterruptedException e) {
            e.printStackTrace();
            wordDefinitionPopup.dismiss();
            wordDefinitionPopup = null;
        }
    }

    // Sort words according to length
    private int sortWords(String a, String b) {
        if(a.length() == b.length())
            return a.compareTo(b);

        return Integer.compare(b.length(), a.length());
    }

    // Start scoreboard activity
    private void enterScoreboard() {
        // Clear the popups (if exist)
        if(wordPopupWindow != null)
            wordPopupWindow.dismiss();
        if(displayWordPopup != null) {
            displayWordPopup.dismiss();
        }

        String roundData = username + "/" +
                currentScore + "/" + findBestWord()
                + "/" + String.valueOf(wordsFound.size());

        if (previousHighScore > 0 && currentScore > previousHighScore) {
            roundData += "/" + String.valueOf(currentScore - previousHighScore);
        }

        Intent intent = new Intent(MainActivity.this, ScoreboardActivity.class);
        intent.putExtra(EXTRA_MESSAGE, roundData);
        startActivity(intent);
        shouldWriteStats = false;
        finish();
    }

    // Return to main menu
    private void returnToMain() {
        BoardManager.clearActiveBoard();
        shouldWriteStats = false;
        finish();
    }

    // Find the longest word the user has found
    private String findBestWord() {
        String bestWord = "-";  // Default best word
        for(String word : wordsFound) {
            if(word.length() > bestWord.length()) {
                bestWord = word;
            }
        }

        return bestWord;
    }

    // Get the user's previous high score on this game board
    private void getPreviousHighScore() {
        if(!GameSettings.UseFirebase())
            return;
        sessionReference = mFireStore.collection(collectionPath);
        sessionReference.document(fireBaseUid).get().addOnSuccessListener(documentSnapshot -> {
            HighscoreData previousData = documentSnapshot.toObject(HighscoreData.class);
            if(previousData != null) {
                previousHighScore = ScoreUtils.getHighScore(previousData);
            }
        });
    }

    // Update user data after game has ended
    private void updateLeaderBoards() {
        if(!GameSettings.UseFirebase())
            return;

        // Don't update score table, if previous score is greater
        if (isScoreGreater())
            return;

        int score = calculateScore();

        Log.d(TAG, "Score from game: " + score);

        sessionReference = mFireStore.collection(collectionPath);
        HighscoreData data = new HighscoreData(username, score, findBestWord(), wordsFound.size());
        data.setUserId(fireBaseUid);
        sessionReference.document(fireBaseUid).set(data);
    }

    private int calculateScore() {
        // Force update current score to classic mode -> avoids messing up avg score
        if (UserSettings.getActiveGameMode().equals("rational")) {
            int score = 0;
            for (String word : wordsFound) {
                score += GameSettings.getScoreForLength(word.length());
            }
            return score;
        }

        return currentScore;
    }

    private boolean isScoreGreater() {
        if (UserSettings.getActiveGameMode().equals("rational")) {
            return wordsFound.size() > currentScore;
        }
        else {
            return previousHighScore > currentScore;
        }
    }

    private void setDarkMode() {
        foundWordColor = Color.WHITE;
        defaultLetterColor = Color.WHITE;
        highlightLetterColor = Color.BLACK;
        defaultTile = AppCompatResources.getDrawable(this, R.drawable.wordgame_tile_black);

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
    }
}