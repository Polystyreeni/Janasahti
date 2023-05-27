package com.example.wordgame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.Slider;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MenuActivity extends AppCompatActivity {

    public static final String USERFILENAME = "userprofile";
    public static final String SETTINGSFILENAME = "settings";
    private final String userCollectionName = "wg_usernames";
    private final String TAG = "WordGame Menu";

    // Ui components
    private Button startButton;
    private EditText userNameField;
    private ProgressBar boardLoadProgressBar;
    private ConstraintLayout layout;
    private View mainMenuBackground;
    private Button settingsButton;
    private Button userStatsButton;
    private Spinner gameModeSpinner;
    private TextView gameModeDescription;
    private ArrayAdapter<String> gameModeArrayAdaper;

    // Menu state
    private Thread boardThread = null;
    private boolean versionChecked = false;
    private String userName = "";
    private PopupWindow currentPopup = null;
    private boolean MOTDReceived = false;

    // Firebase
    private FirebaseFirestore mFireStore;
    private CollectionReference sessionReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    Handler boardLoadHandler = new Handler();
    Runnable boardLoadRunnable = new Runnable() {
        @Override
        public void run() {
            if(boardThread.isAlive() || !versionChecked || !MOTDReceived) {
                boardLoadHandler.postDelayed(this, 500);
            }

            else {
                boardLoadProgressBar.setVisibility(View.GONE);

                startGame();
                boardLoadHandler.removeCallbacks(boardLoadRunnable);
            }
        }
    };

    Handler messageHandler = new Handler();
    Runnable messageRunnable = new Runnable() {
        @Override
        public void run() {
            if (!versionChecked) {
                messageHandler.postDelayed(this, 500);
            }
            else {
                createMessagePopup();
                messageHandler.removeCallbacks(messageRunnable);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        startButton = findViewById(R.id.startGameButton);
        userNameField = findViewById(R.id.usernameEditText);
        boardLoadProgressBar = findViewById(R.id.boardLoadProgressBar);
        boardLoadProgressBar.setVisibility(View.GONE);
        layout = findViewById(R.id.menuLayout);
        mainMenuBackground = findViewById(R.id.mainMenuBackView);
        gameModeSpinner = findViewById(R.id.gameModeSpinner);
        gameModeDescription = findViewById(R.id.gameModeDescription);

        // Initialize ArrayAdapter for game mode selection
        String[] gameModeNames = new String[GameSettings.getGameModes().length];
        for (int i = 0; i < gameModeNames.length; i++) {
            gameModeNames[i] = GameSettings.getGameModeName(GameSettings.getGameModes()[i]);
        }

        gameModeArrayAdaper = new ArrayAdapter<>(this, R.layout.spinner_item, gameModeNames);
        gameModeArrayAdaper.setDropDownViewResource(R.layout.spinner_item_dropdown);
        gameModeSpinner.setAdapter(gameModeArrayAdaper);

        settingsButton = findViewById(R.id.settingsButton);
        userStatsButton = findViewById(R.id.userStatsButton);
        final TextView versionTextView = findViewById(R.id.versionText);

        gameModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String gameMode = GameSettings.getGameModes()[i];
                UserSettings.setActiveGameMode(gameMode);
                gameModeDescription.setText(GameSettings.getGameModeDescription(gameMode));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mFireStore = FirebaseFirestore.getInstance();
        sessionReference = mFireStore.collection(userCollectionName);
        firebaseAuth = FirebaseAuth.getInstance();

        // Create the board in background while in main menu
        BoardManager.setRandomSeed(Calendar.getInstance().getTimeInMillis());
        boardThread = new Thread(BoardManager::generateBoard);

        boardThread.start();

        startButton.setOnClickListener(view -> {
            if(boardThread != null && boardThread.isAlive()) {
                Log.d(TAG, "Board thread alive, waiting...");
                boardLoadProgressBar.setVisibility(View.VISIBLE);
                boardLoadHandler.postDelayed(boardLoadRunnable, 0);
                startButton.setClickable(false);
                return;
            }

            startGame();
        });

        settingsButton.setOnClickListener(view -> createSettingsPopup());
        userStatsButton.setOnClickListener(view -> openUserStatsWindow());

        // Set username to the stored profile name (if exists)
        userName = readUserProfile();
        if (!userName.isEmpty()) {
            userNameField.setText(userName);
        }

        versionTextView.setText(VersionManager.getVersion());

        readSettingsFile();
        updateBackground();
        VersionManager.getLatestVersion(this);
        MOTDManager.getLatestMessage(this);
        Log.d(TAG, "Game Menu loaded");
        if(UserStatsManager.Instance == null) {
            UserStatsManager.initialze();
            UserStatsManager.loadStats(getApplicationContext());
        }

        else
            UserStatsManager.saveStats(getApplicationContext());

        signInAnonymously();
        layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Do this with delay, because not working otherwise
                if(UserSettings.getDarkModeEnabled() > 0) {
                    settingsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
                    userStatsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Main menu on stop");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Main menu on resume");
        startButton.setClickable(true);
        UserStatsManager.saveStats(getApplicationContext());

        // Create a new board when entering main menu
        if(BoardManager.getShouldGenerateBoard()) {
            if (BoardManager.getBoardQueueSize() > 0)
                BoardManager.setNextBoard();
            else {
                boardThread = new Thread(BoardManager::generateBoard);
                boardThread.start();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if(GameSettings.UseFirebase()) {
            firebaseUser = firebaseAuth.getCurrentUser();
        }
    }

    @Override
    public void onBackPressed() {
        if(currentPopup != null) {
            currentPopup.dismiss();
            currentPopup = null;
        }
        else {
            super.onBackPressed();
        }
    }

    private void updateBackground() {
        if(UserSettings.getDarkModeEnabled() > 0) {
            layout.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_gradient_dark));
            mainMenuBackground.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.rounded_corner_black));
            userNameField.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            settingsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
            userStatsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
            gameModeDescription.setTextColor(Color.WHITE);
        }
        else {
            layout.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_gradient));
            mainMenuBackground.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.rounded_corner_gold));
            userNameField.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
            settingsButton.getBackground().mutate().setColorFilter(R.attr.colorPrimary, PorterDuff.Mode.MULTIPLY);
            userStatsButton.getBackground().mutate().setColorFilter(R.attr.colorPrimary, PorterDuff.Mode.MULTIPLY);
            gameModeDescription.setTextColor(Color.BLACK);
        }
    }

    private void startGame() {
        userName = userNameField.getText().toString();
        if(userName.isEmpty()) {
            userName = "Nimetön";
        }

        // We're using the / sign for passing data, don't allow user to have it in their username
        if(userName.contains("/"))
            userName = userName.replaceAll("/", " ");

        // Name can't be too long, will cause issues with scaling
        if(userName.length() > GameSettings.getUsernameMaxLength()) {
            userName = userName.substring(0, GameSettings.getUsernameMaxLength());
        }

        if(firebaseUser != null && GameSettings.UseFirebase()) {
            sessionReference = mFireStore.collection(userCollectionName);
            String document = firebaseUser.getUid();

            Map<String, Object> docData = new HashMap<>();
            docData.put("userName", userName);
            sessionReference.document(document).set(docData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Userprofile successfully written!"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error writing Userprofile", e));
        }

        createUserProfile(userName);
        writeSettingsFile();

        // Start game activity
        Intent intent = new Intent(MenuActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_MESSAGE, userName);
        BoardManager.setShouldGenerateBoard(false);
        startActivity(intent);
        boardThread = null;
    }

    // Creates a local file containing the username
    private void createUserProfile(String name) {
        FileOutputStream stream;
        try {
            Context ctx = getApplicationContext();
            stream = ctx.openFileOutput(USERFILENAME, Context.MODE_PRIVATE);
            stream.write(name.getBytes(StandardCharsets.UTF_8));
            stream.close();
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Reads the local file containing username (if exists)
    private String readUserProfile() {
        FileInputStream inputStream;
        try {
            Context ctx = getApplicationContext();
            inputStream = ctx.openFileInput(USERFILENAME);
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputReader);
            return bufferedReader.readLine();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return "";
    }

    private void writeSettingsFile() {
        FileOutputStream stream;
        try {
            Context ctx = getApplicationContext();
            stream = ctx.openFileOutput(SETTINGSFILENAME, Context.MODE_PRIVATE);

            String settings = String.valueOf(UserSettings.getDarkModeEnabled())
                    + "/" + String.valueOf(UserSettings.getOledProtectionEnabled())
                    + "/" + String.valueOf(UserSettings.getTextScale())
                    + "/" + UserSettings.getMOTDId()
                    + "/" + UserSettings.getActiveGameMode();

            stream.write(settings.getBytes(StandardCharsets.UTF_8));
            stream.close();
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readSettingsFile() {
        UserSettings.initializeSettings();

        FileInputStream inputStream;
        try {
            Context ctx = getApplicationContext();
            inputStream = ctx.openFileInput(SETTINGSFILENAME);
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            String settingsStr = bufferedReader.readLine();
            String[] settings = settingsStr.split("/");

            for(int i = 0; i < settings.length; i++) {
                UserSettings.userSettings.get(i).run(settings[i]);
            }

            // Set game mode to saved value
            for(int i = 0; i < GameSettings.getGameModes().length; i++)
            {
                if (UserSettings.getActiveGameMode().equals(GameSettings.getGameModes()[i])) {
                    gameModeSpinner.setSelection(i);
                }
            }
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Sign in anonymously to Firebase
    private void signInAnonymously() {
        if(!GameSettings.UseFirebase())
            return;
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInAnonymously:success");
                        firebaseUser = firebaseAuth.getCurrentUser();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(MenuActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void onVersionRetrieved(String latestVersion) {
        if(latestVersion.isEmpty()) {
            Toast.makeText(MenuActivity.this, R.string.error_version, Toast.LENGTH_SHORT).show();
            versionChecked = true;
            return;
        }

        String currentVersion = VersionManager.getVersion();
        if(!currentVersion.equals(latestVersion)) {
            // Create popup with update prompt
            showUpdatePopup(latestVersion);
            return;
        }

        versionChecked = true;
    }

    public void onMOTDRetrieved(String id, String message) {
        if (id.isEmpty())
            return;

        // User has already seen this message
        if (UserSettings.getMOTDId().equals(id))
             return;
        UserSettings.setMOTDId(id);

        // Display message popup
        messageHandler.postDelayed(messageRunnable, 0);
    }

    private void createSettingsPopup() {
        if(currentPopup != null)
            return;
        if(UserSettings.getDarkModeEnabled() > 0)
            settingsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.game_settings_popup, null);

        CheckBox darkBox = popupView.findViewById(R.id.settingCheckboxColor);
        CheckBox oledBox = popupView.findViewById(R.id.settingCheckboxOled);
        Slider textSizeSlider = popupView.findViewById(R.id.settingsTextSizeSlider);
        Button returnButton = popupView.findViewById(R.id.settingsReturnButton);

        darkBox.setChecked(UserSettings.getDarkModeEnabled() > 0);
        oledBox.setChecked(UserSettings.getOledProtectionEnabled() > 0);
        textSizeSlider.setValue(UserSettings.getTextScale());

        darkBox.setOnCheckedChangeListener((compoundButton, b) -> {
            int value = b ? 1 : 0;
            UserSettings.setDarkModeEnabled(value);
            updateBackground();
        });

        oledBox.setOnCheckedChangeListener((compoundButton, b) -> {
            int value = b ? 1 : 0;
            UserSettings.setOledProtectionEnabled(value);
        });

        textSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            int intVal = Math.round(value);
            UserSettings.setTextScale(intVal);
        });

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);
        popupWindow.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        currentPopup = popupWindow;

        returnButton.setOnClickListener(view1 -> {
            popupWindow.dismiss();
            currentPopup = null;
            if(UserSettings.getDarkModeEnabled() > 0)
                settingsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        });
    }

    private void createMessagePopup() {
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.daily_message_popup, null);

        TextView motdText = popupView.findViewById(R.id.motdTextView);
        Button motdExitButton = popupView.findViewById(R.id.motdExitButton);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);
        popupWindow.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        currentPopup = popupWindow;

        motdText.setText(TextUtils.getSpannedText(MOTDManager.getMessageText()));
        motdExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MOTDReceived = true;
                currentPopup = null;
                popupWindow.dismiss();
            }
        });
    }

    private void showUpdatePopup(String latestVersion) {
        // Initialize the popup-window to show update info
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.update_popup, null);

        TextView versionText = popupView.findViewById(R.id.versionComparison);
        versionText.setText(getResources().getString(R.string.new_version_number,
                String.valueOf(VersionManager.getVersion()), latestVersion));
        TextView linkView = popupView.findViewById(R.id.updateLink);
        Linkify.addLinks(linkView, Linkify.ALL);
        Button cancelButton = popupView.findViewById(R.id.updateReturnButton);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);
        popupWindow.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        currentPopup = popupWindow;

        cancelButton.setOnClickListener(view -> {
            versionChecked = true;
            currentPopup = null;
            popupWindow.dismiss();
        });
    }

    private void openUserStatsWindow() {
        if(currentPopup != null)
            return;
        if(UserSettings.getDarkModeEnabled() > 0)
            userStatsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        // Initialize the popup-window to show user stats
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.user_stats_popup, null);

        TextView statsScoreTextHeader = popupView.findViewById(R.id.userStatsScoreTotalHeader);
        TextView statsGamesTextHeader = popupView.findViewById(R.id.userStatsGamesPlayedHeader);
        TextView statsTimeTextHeader = popupView.findViewById(R.id.userStatsGameTimeHeader);
        TextView statsAvgScoreTextHeader = popupView.findViewById(R.id.userStatsAvgScoreHeader);
        TextView statsHighScoreTextHeader = popupView.findViewById(R.id.userStatsHighestScoreHeader);
        TextView statsPercentageTextHeader = popupView.findViewById(R.id.userStatsHighestPercentageHeader);
        TextView statsFirstTextHeader = popupView.findViewById(R.id.userStatsFirstPlacesHeader);
        TextView statsLongestWordTextHeader = popupView.findViewById(R.id.userStatsLongestWordHeader);

        TextView statsNameText = popupView.findViewById(R.id.userStatsName);
        TextView statsScoreText = popupView.findViewById(R.id.userStatsScoreTotal);
        TextView statsGamesText = popupView.findViewById(R.id.userStatsGamesPlayed);
        TextView statsTimeText = popupView.findViewById(R.id.userStatsGameTime);
        TextView statsAvgScoreText = popupView.findViewById(R.id.userStatsAvgScore);
        TextView statsHighScoreText = popupView.findViewById(R.id.userStatsHighestScore);
        TextView statsPercentageText = popupView.findViewById(R.id.userStatsHighestPercentage);
        TextView statsFirstText = popupView.findViewById(R.id.userStatsFirstPlaces);
        TextView statsLongestWordText = popupView.findViewById(R.id.userStatsLongestWord);
        Button returnButton = popupView.findViewById(R.id.userStatsReturnButton);

        // Need to do this ugly list to handle dark mode text color change
        ArrayList<TextView> textViews = new ArrayList<>(Arrays.asList(
                statsNameText, statsScoreText, statsGamesText, statsHighScoreText,
                statsPercentageText, statsFirstText, statsLongestWordText,
                statsScoreTextHeader, statsGamesTextHeader, statsHighScoreTextHeader,
                statsPercentageTextHeader, statsFirstTextHeader, statsLongestWordTextHeader,
                statsTimeTextHeader, statsTimeText, statsAvgScoreTextHeader, statsAvgScoreText
        ));

        if(UserSettings.getDarkModeEnabled() > 0) {
            popupView.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_gradient_dark));
            for(TextView t : textViews) {
                t.setTextColor(Color.WHITE);
            }
        }

        statsNameText.setText(userName);
        statsScoreText.setText(String.valueOf(UserStatsManager.Instance.getScoreTotal()));
        statsGamesText.setText(String.valueOf(UserStatsManager.Instance.getNumberOfGames()));
        statsTimeText.setText(TextUtils.hrMinSecFromLong(UserStatsManager.Instance.getTotalGameTime()));
        statsHighScoreText.setText(String.valueOf(UserStatsManager.Instance.getHighestScore()));
        statsAvgScoreText.setText(String.valueOf(UserStatsManager.Instance.getAverageScore()));
        statsPercentageText.setText(getResources().getString(R.string.user_stats_percentage,
                String.format("%.2f", UserStatsManager.Instance.getHighestPercentage() * 100), " %"));
        statsLongestWordText.setText(UserStatsManager.Instance.getLongestWord());
        statsFirstText.setText(String.valueOf(UserStatsManager.Instance.getFirstPlaces()));

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);
        popupWindow.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        currentPopup = popupWindow;

        returnButton.setOnClickListener(view1 -> {
            popupWindow.dismiss();
            currentPopup = null;
            if(UserSettings.getDarkModeEnabled() > 0)
                userStatsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        });
    }
}