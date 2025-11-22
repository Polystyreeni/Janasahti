package com.example.wordgame.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wordgame.debug.Logger;
import com.example.wordgame.gamemode.GameModeType;
import com.example.wordgame.managers.ApplicationManager;
import com.example.wordgame.managers.BoardManager;
import com.example.wordgame.R;
import com.example.wordgame.models.AppRemoteState;
import com.example.wordgame.models.DailyMessage;
import com.example.wordgame.models.GameVersion;
import com.example.wordgame.models.RemoteLoadType;
import com.example.wordgame.popups.AppUpdatePopupView;
import com.example.wordgame.popups.DailyMessagePopupView;
import com.example.wordgame.popups.GameUnavailablePopup;
import com.example.wordgame.popups.NoConnectionPopupView;
import com.example.wordgame.popups.SettingsPopupView;
import com.example.wordgame.popups.StatsPopupView;
import com.example.wordgame.popups.UserBannedPopup;
import com.example.wordgame.utility.ActivityUtils;
import com.example.wordgame.utility.AppConstants;
import com.example.wordgame.utility.FirebaseUtils;
import com.example.wordgame.utility.IoUtils;
import com.example.wordgame.utility.ScoreUtils;
import com.example.wordgame.utility.TextUtils;
import com.example.wordgame.managers.UserSettingsManager;
import com.example.wordgame.usermanagement.BlackListData;
import com.example.wordgame.utility.UserUtils;
import com.example.wordgame.volley.extensions.NetworkStringResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Main menu activity. Initializes application manager.
 */
public class MenuActivity extends AppCompatActivity implements IRemoteConfigInitListener {
    public static final String EXTRA_MESSAGE_USERNAME = "com.example.wordgame.extra.username";
    public static final String USER_FILE_NAME = "userprofile";
    private final String TAG = "MenuActivity";

    // UI components
    private Button startButton;
    private EditText userNameField;
    private ProgressBar boardLoadProgressBar;
    private TextView boardLoadStatusText;
    private ConstraintLayout layout;
    private View mainMenuBackground;
    private View settingsButton;
    private View userStatsButton;
    private TextView gameModeSpinner;
    private TextView gameModeDescription;
    private View networkStatusIcon;

    // Network checks
    private boolean versionChecked = false;
    private boolean banCheckComplete = false;
    private boolean messageOfTheDayReceived = false;

    // Firebase
    private FirebaseFirestore mFireStore;
    private CollectionReference sessionReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private boolean userRestricted = false;

    // Activity state
    private Context appCtx;
    private GameVersion currentVersion;
    private GameVersion latestVersion;
    private String userName = "";
    private PopupWindow currentPopup = null;
    private boolean shouldGenerateBoards = false;
    private DailyMessage receivedDailyMessage = null;
    private boolean networkModePending = false;

    // Game mode settings
    private final Map<GameModeType, Integer> gameModeDescriptionMap = new EnumMap<>(GameModeType.class);
    private final boolean[] checkedGameModes = new boolean[GameModeType.values().length];
    private final Set<GameModeType> selectedGameModes = EnumSet.noneOf(GameModeType.class);

    private ApplicationManager applicationManager;
    private final Handler boardLoadHandler = new Handler();
    private final Runnable boardLoadRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isBoardManagerReady() || !versionChecked || !messageOfTheDayReceived || !banCheckComplete || currentPopup != null) {
                Logger.getInstance().debug(TAG,
                        String.format("BM ready = %s, verchk = %s, motd = %s, " +
                                "ban check = %s, popup = %s",
                        isBoardManagerReady(),
                        versionChecked, messageOfTheDayReceived,
                        banCheckComplete, currentPopup == null));

                setLoadStatusText();
                boardLoadHandler.postDelayed(this, 500);
            } else {
                boardLoadProgressBar.setVisibility(View.GONE);
                boardLoadStatusText.setVisibility(View.GONE);
                startGame();
                boardLoadHandler.removeCallbacks(boardLoadRunnable);
            }
        }
    };

    private final Handler messageHandler = new Handler();
    private final Runnable messageRunnable = new Runnable() {
        @Override
        public void run() {
            if (!versionChecked) {
                messageHandler.postDelayed(this, 500);
            }
            else {
                createMessagePopup(receivedDailyMessage);
                messageHandler.removeCallbacks(messageRunnable);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Logger.getInstance().debug(TAG, "onCreate()");

        appCtx = getApplicationContext();

        initializeLocalizations();
        initializeComponents();
        initializeApplicationManager();

        mFireStore = FirebaseFirestore.getInstance();
        sessionReference = mFireStore.collection(FirebaseUtils.USER_COLLECTION);
        firebaseAuth = FirebaseAuth.getInstance();

        readUserProfile();

        // Initialize application settings
        loadUserSettings();
        applicationManager.getUserStatsManager().loadStats(appCtx);

        updateBackground();
        signInAnonymously();
    }

    private void initializeLocalizations() {
        gameModeDescriptionMap.put(GameModeType.NORMAL, R.string.gamemode_description_normal);
        gameModeDescriptionMap.put(GameModeType.RATIONAL, R.string.gamemode_description_rational);
        gameModeDescriptionMap.put(GameModeType.TIME_CHASE, R.string.gamemode_description_time_chase);
        gameModeDescriptionMap.put(GameModeType.EXTENDED, R.string.gamemode_description_extended);
    }

    private void initializeApplicationManager() {
        setApplicationManager(ApplicationManager.getInstance(appCtx));
    }

    private void setApplicationManager(ApplicationManager manager) {
        this.applicationManager = manager;
        this.currentVersion = manager.getLocalConfig().getVersion();
        this.applicationManager.addRemoteInitListener(this);
        Logger.getInstance().debug(TAG, "Set current version to "
                + this.currentVersion.toString());

        final AppRemoteState state = applicationManager.getRemoteState();
        if (state == AppRemoteState.UNAVAILABLE) {
            // Can happen when we return to the application from another activity
            // where remote config has become invalid and a new configuration cannot be fetched
            Logger.getInstance().info(TAG, "Network error occurred, requesting offline");
            onRemoteConfigLoadFailure();
        } else if (state == AppRemoteState.READY) {
            // Using cached version of remote config
            onRemoteConfigLoadSuccess(RemoteLoadType.CACHED_LOAD);
        }

        // Set version text
        final TextView versionTextView = findViewById(R.id.versionText);
        versionTextView.setText(this.currentVersion.toString());
    }

    private void initializeComponents() {
        startButton = findViewById(R.id.startGameButton);
        userNameField = findViewById(R.id.usernameEditText);
        boardLoadProgressBar = findViewById(R.id.boardLoadProgressBar);
        boardLoadProgressBar.setVisibility(View.GONE);
        boardLoadStatusText = findViewById(R.id.boardLoadStatusText);
        boardLoadStatusText.setVisibility(View.GONE);
        layout = findViewById(R.id.menuLayout);
        mainMenuBackground = findViewById(R.id.mainMenuBackView);
        gameModeSpinner = findViewById(R.id.gameModeSpinner);
        gameModeDescription = findViewById(R.id.gameModeDescription);
        networkStatusIcon = findViewById(R.id.networkStatusIcon);
        networkStatusIcon.setVisibility(View.VISIBLE);

        initGameModeSelection();

        settingsButton = findViewById(R.id.settingsButton);
        userStatsButton = findViewById(R.id.userStatsButton);

        // Set username to the stored profile name (if exists)
        userName = readUserProfile();
        if (!userName.isEmpty()) {
            userNameField.setText(userName);
        }

        addComponentListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            applicationManager.removeConfigInitListener(this);
            Logger.getInstance().stop();
        } catch (Exception e) {
            Logger.getInstance().warn(TAG, "Failed to clean application manager listeners", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.getInstance().debug(TAG, "Main menu on stop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.getInstance().debug(TAG, "Main menu on resume");
        if (userRestricted) {
            startButton.setClickable(false);
            return;
        }

        if (latestVersion != null &&
                GameVersion.areVersionsIncompatible(currentVersion, latestVersion)) {
            startButton.setClickable(false);
            return;
        }

        startButton.setClickable(true);

        // Application manager instance changed
        if (ApplicationManager.getInstance(appCtx) != this.applicationManager) {
            setApplicationManager(ApplicationManager.getInstance(appCtx));
        }

        if (AppRemoteState.UNAVAILABLE == applicationManager.getRemoteState()) {
            setOfflineMode();
        } else {
            // Create a new board when entering main menu
            if (shouldGenerateBoards) {
                BoardManager boardManager = applicationManager.getBoardManager();
                if (boardManager.hasBoardsRemaining()) {
                    boardManager.dequeueBoard();
                } else {
                    boardManager.requestBoards();
                }
                shouldGenerateBoards = false;
            }
        }

        applicationManager.getUserStatsManager().saveStats(appCtx);

        // Dark mode needs to be re-enabled when resuming from other activities
        if (ActivityUtils.isDarkModeEnabled(applicationManager)) {
            updateBackground();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (ScoreUtils.useScoreBoard(applicationManager)) {
            firebaseUser = firebaseAuth.getCurrentUser();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentPopup != null) {
            currentPopup.dismiss();
            currentPopup = null;
        } else {
            super.onBackPressed();
        }
    }

    private void addComponentListeners() {
        startButton.setOnClickListener(view -> {
            if (!isBoardManagerReady()) {
                Logger.getInstance().debug(TAG, "Board manager is fetching boards");
                boardLoadProgressBar.setVisibility(View.VISIBLE);
                boardLoadStatusText.setVisibility(View.VISIBLE);
                boardLoadStatusText.setText(getString(R.string.load_status_board));
                boardLoadHandler.postDelayed(boardLoadRunnable, 0);
                startButton.setClickable(false);
                if (!banCheckComplete) {
                    checkUserRestricted();
                }
                return;
            }

            if (!banCheckComplete) {
                Logger.getInstance().debug(TAG, "Ban check incomplete");
                checkUserRestricted();
                boardLoadProgressBar.setVisibility(View.VISIBLE);
                boardLoadHandler.postDelayed(boardLoadRunnable, 0);
                startButton.setClickable(false);
                return;
            }

            startGame();
        });

        settingsButton.setOnClickListener(view -> createSettingsPopup());
        userStatsButton.setOnClickListener(view -> openUserStatsWindow());
    }

    private void initGameModeSelection() {
        // Initialize ArrayAdapter for game mode selection
        final GameModeType[] gameModeTypes = GameModeType.values();
        final String[] gameModeNames = new String[gameModeTypes.length];
        for (int i = 0; i < gameModeTypes.length; i++) {
            gameModeNames[i] = getString(AppConstants.getGameModeName(gameModeTypes[i]));
        }

        final ArrayAdapter<GameModeType> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        gameModeSpinner.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog)
                .setTitle(R.string.gamemode_select_header)
                .setCancelable(false)
                .setMultiChoiceItems(gameModeNames, checkedGameModes, (dialog, index, checked) -> {
                    checkedGameModes[index] = checked;
                    if (checked) {
                        selectedGameModes.add(gameModeTypes[index]);
                    } else {
                        selectedGameModes.remove(gameModeTypes[index]);
                    }
                });
            builder.setPositiveButton(R.string.general_confirm, (dialog, index) -> {
                // Force first game mode if nothing is selected
                if (selectedGameModes.isEmpty()) {
                    checkedGameModes[0] = true;
                    selectedGameModes.add(gameModeTypes[0]);
                }
                onGameModesSelected();
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    private void onGameModesSelected() {
        final List<String> selectedModes = new ArrayList<>(selectedGameModes.size());
        for (GameModeType type : selectedGameModes) {
            selectedModes.add(getString(AppConstants.getGameModeName(type)));
        }

        final int maxLength = 26;
        String modesAsString = String.join(", ", selectedModes.toArray(new String[0]));
        if (modesAsString.length() > maxLength) {
            modesAsString = modesAsString.substring(0, maxLength).concat(TextUtils.ELLIPSIS);
        }

        gameModeSpinner.setText(modesAsString);

        if (selectedGameModes.size() == 1) {
            for (GameModeType type : selectedGameModes) {
                Integer resId = gameModeDescriptionMap.get(type);
                if (resId != null) {
                    gameModeDescription.setText(resId);
                    break;
                }
            }
        } else {
            gameModeDescription.setText(R.string.gamemode_description_random);
        }

        applicationManager.getUserSettingsManager().setSetting(
                UserSettingsManager.UserSetting.ACTIVE_GAME_MODES,
                selectedGameModes.toArray(new GameModeType[0]));
        Logger.getInstance().debug(TAG, "Game modes set to " + selectedGameModes);
    }

    private void onVersionRetrieved(@Nullable GameVersion retrieved) {
        versionChecked = true;
        if (retrieved == null) {
            Toast.makeText(MenuActivity.this, R.string.error_version, Toast.LENGTH_SHORT).show();
            return;
        }

        latestVersion = retrieved;
        if (!currentVersion.equals(latestVersion)) {
            // Clear all popups, when version is fetched
            if (currentPopup != null) {
                currentPopup.dismiss();
            }

            // Create popup with update prompt
            showUpdatePopup();
        }
    }

    private void onMessageRetrieved(NetworkStringResponse response) {
        messageOfTheDayReceived = true;
        if (response.getContent() == null) {
            Toast.makeText(MenuActivity.this,
                    R.string.error_message_service, Toast.LENGTH_SHORT).show();
            return;
        }

        DailyMessage message = DailyMessage.parseFromString(response.getContent(), currentVersion);
        if (message != null) {
            String existingMsgId = (String) Objects.requireNonNull(applicationManager.getUserSettingsManager()
                    .getSetting(UserSettingsManager.UserSetting.DAILY_MESSAGE_ID)).getValue();

            // Message already acknowledged
            if (existingMsgId.equals(message.getId())) {
                return;
            }
            applicationManager.getUserSettingsManager().setSetting(
                    UserSettingsManager.UserSetting.DAILY_MESSAGE_ID, message.getId());
            receivedDailyMessage = message;
            messageHandler.postDelayed(messageRunnable, 0);
        }
    }

    private void updateBackground() {
        if (ActivityUtils.isDarkModeEnabled(applicationManager)) {
            layout.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                    R.drawable.background_gradient_dark));
            mainMenuBackground.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                    R.drawable.rounded_corner_black));
            userNameField.setTextColor(ContextCompat.getColor(getApplicationContext(),
                    R.color.white));
            settingsButton.getBackground().mutate().setColorFilter(Color.WHITE,
                    PorterDuff.Mode.MULTIPLY);
            userStatsButton.getBackground().mutate().setColorFilter(Color.WHITE,
                    PorterDuff.Mode.MULTIPLY);
            gameModeDescription.setTextColor(Color.WHITE);
        } else {
            layout.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                    R.drawable.background_gradient));
            mainMenuBackground.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                    R.drawable.rounded_corner_gold));
            userNameField.setTextColor(ContextCompat.getColor(getApplicationContext(),
                    R.color.black));
            settingsButton.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(),
                    R.color.pico_void), PorterDuff.Mode.MULTIPLY);
            userStatsButton.getBackground().mutate().setColorFilter(ContextCompat.getColor(getApplicationContext(),
                    R.color.pico_void), PorterDuff.Mode.MULTIPLY);
            gameModeDescription.setTextColor(Color.BLACK);
        }
    }

    private void startGame() {
        // Cannot start game with incompatible version
        if (GameVersion.areVersionsIncompatible(currentVersion, latestVersion)) {
            Toast.makeText(MenuActivity.this, getString(R.string.new_version_block_action),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (userRestricted) {
            Toast.makeText(MenuActivity.this, getString(R.string.player_banned_not_available),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        userName = UserUtils.sanitizeUserName(userNameField.getText().toString(),
                getResources().getString(R.string.unknown_user_name));
        if (firebaseUser != null && ScoreUtils.useScoreBoard(applicationManager)) {
            sessionReference = mFireStore.collection(FirebaseUtils.USER_COLLECTION);
            String document = firebaseUser.getUid();
            String lastLogin = TextUtils.millisToFullDate(System.currentTimeMillis());

            final Map<String, Object> docData = new HashMap<>();
            docData.put("userName", userName);
            docData.put("lastLogin", lastLogin);
            sessionReference.document(document).set(docData)
                    .addOnSuccessListener(aVoid -> Logger.getInstance().warn(TAG, "Userprofile successfully written!"))
                    .addOnFailureListener(e -> Logger.getInstance().warn(TAG, "Error writing Userprofile", e));
        }

        createUserProfile(userName);
        applicationManager.getUserSettingsManager().writeToFile(appCtx);

        // Start game activity
        Intent intent = new Intent(MenuActivity.this, MainActivity.class);
        intent.putExtra(MenuActivity.EXTRA_MESSAGE_USERNAME, userName);
        intent.putExtra(MainActivity.EXTRA_MESSAGE_GAME_MODE, getGameMode().toString());
        shouldGenerateBoards = true;
        startActivity(intent);
    }

    private void createUserProfile(String name) {
        try {
            IoUtils.writeFile(appCtx, USER_FILE_NAME, name);
        } catch (IOException e) {
            Logger.getInstance().warn(TAG, "Failed to write user profile", e);
        }
    }

    private GameModeType getGameMode() {
        GameModeType[] types = (GameModeType[]) Objects.requireNonNull(
                applicationManager.getUserSettingsManager()
                        .getSetting(UserSettingsManager.UserSetting.ACTIVE_GAME_MODES))
                .getValue();

        return UserUtils.getRandomGameModeType(types);
    }

    private String readUserProfile() {
        return IoUtils.readLineFromFile(appCtx, USER_FILE_NAME);
    }

    private void loadUserSettings() {
        final UserSettingsManager userSettingsManager = applicationManager.getUserSettingsManager();
        userSettingsManager.readFromFile(appCtx);

        List<GameModeType> prevGameModes = Arrays.asList((GameModeType[]) Objects.requireNonNull(userSettingsManager
                .getSetting(UserSettingsManager.UserSetting.ACTIVE_GAME_MODES)).getValue());

        Logger.getInstance().debug(TAG, "Selected game modes = " + prevGameModes);
        for (GameModeType type : prevGameModes) {
            selectedGameModes.add(type);
            checkedGameModes[type.ordinal()] = true;
        }

        onGameModesSelected();
    }

    // Sign in anonymously to Firebase
    private void signInAnonymously() {
        if (!ScoreUtils.useScoreBoard(applicationManager))
            return;
        firebaseAuth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                firebaseUser = firebaseAuth.getCurrentUser();
            } else {
                // If sign in fails, display a message to the user.
                Logger.getInstance().warn(TAG, "signInAnonymously:failure", task.getException());
                Toast.makeText(MenuActivity.this, R.string.error_authentication_failed,
                        Toast.LENGTH_SHORT).show();
                applicationManager.getUserSettingsManager().setSetting(
                        UserSettingsManager.UserSetting.USE_SCORE_BOARD, false);
            }
        });
    }

    private void checkUserRestricted() {
        Logger.getInstance().debug(TAG, "Checking player ban list");

        mFireStore.collection(FirebaseUtils.BAN_LIST_COLLECTION).get().addOnCompleteListener(task -> {
            banCheckComplete = true;
            if (!task.isSuccessful()) {
                Logger.getInstance().warn(TAG, "Could not read ban list data", task.getException());
                return;
            }

            QuerySnapshot snapshot = task.getResult();
            List<DocumentSnapshot> documents = snapshot.getDocuments();
            for (DocumentSnapshot document : documents) {
                if (document.exists()) {
                    BlackListData blackListData = document.toObject(BlackListData.class);
                    if (blackListData.getUserID().equals(firebaseAuth.getUid())) {
                        restrictUser(blackListData);
                    }
                }
            }
        });
    }

    private void restrictUser(BlackListData data) {
        applicationManager.getUserSettingsManager().setSetting(
                UserSettingsManager.UserSetting.USE_SCORE_BOARD, false);
        userRestricted = true;
        startButton.setClickable(false);
        showBannedPopup(data.getRestrictionReason(), data.getRestrictionDate());
    }

    private void createSettingsPopup() {
        if (currentPopup != null)
            return;
        final boolean darkModeEnabled = ActivityUtils.isDarkModeEnabled(applicationManager);
        if (darkModeEnabled) {
            settingsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        }

        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        SettingsPopupView settingsPopupView = new SettingsPopupView(inflater, null,
                applicationManager,
                view -> onSupportActivityPressed(),
                view -> {
                        dismissPopup();
                        updateBackground();
                        if (ActivityUtils.isDarkModeEnabled(applicationManager)) {
                            settingsButton.getBackground().mutate().setColorFilter(Color.WHITE,
                                    PorterDuff.Mode.MULTIPLY);
                        }
                    },
                (checkbox, value) -> toggleOfflineMode(value));

        // Emails not allowed, when user is restricted
        settingsPopupView.setSupportButtonEnabled(!userRestricted);

        currentPopup = new ActivityUtils.PopupWindowBuilder(settingsPopupView)
                .withDimensions(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT).build();
    }

    private void onSupportActivityPressed() {
        if (userRestricted) {
            Toast.makeText(MenuActivity.this,
                    getResources().getString(R.string.player_banned_not_available),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MenuActivity.this, AboutActivity.class);
        startActivity(intent);
        dismissPopup();
    }

    private void createMessagePopup(DailyMessage message) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        DailyMessagePopupView popupView = new DailyMessagePopupView(inflater, null);
        popupView.setMessage(message);
        PopupWindow popupWindow = new ActivityUtils.PopupWindowBuilder(popupView)
                .withDimensionsWrapContent()
                .build();

        currentPopup = popupWindow;

        popupView.addDismissListener(view -> {
            messageOfTheDayReceived = true;
            currentPopup = null;
            popupWindow.dismiss();
        });
    }

    private void showBannedPopup(String banReason, String banDate) {
        if (currentPopup != null) {
            currentPopup.dismiss();
            currentPopup = null;
        }

        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        UserBannedPopup popup = new UserBannedPopup(appCtx, inflater, null);
        popup.setBanText(getResources().getString(R.string.player_banned_text, banReason, banDate));
        popup.addDismissListener(view -> dismissPopup());

        currentPopup = new ActivityUtils.PopupWindowBuilder(popup)
                .withDimensionsWrapContent()
                .build();
    }

    private void showUpdatePopup() {
        // Initialize the popup-window to show update info
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

        AppUpdatePopupView popupView = new AppUpdatePopupView(inflater, null);
        popupView.setVersionText(getResources().getString(R.string.new_version_number,
                currentVersion.toString(), latestVersion.toString()));

        popupView.setUpdateLink(applicationManager.getLocalConfig().getStringProperty("gameSourceLink"));
        popupView.setRequired(GameVersion.areVersionsIncompatible(currentVersion, latestVersion));

        final PopupWindow popupWindow = new ActivityUtils.PopupWindowBuilder(popupView)
                .withDimensions(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .build();

        currentPopup = popupWindow;
        popupView.setOnClickListener(view -> {
            versionChecked = true;
            currentPopup = null;
            popupWindow.dismiss();
        });
    }

    private void openUserStatsWindow() {
        if (currentPopup != null)
            return;
        if (ActivityUtils.isDarkModeEnabled(applicationManager)) {
            userStatsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        }

        // Initialize the popup-window to show user stats
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        StatsPopupView popup = new StatsPopupView(this, inflater, null,
                applicationManager.getUserStatsManager());
        popup.onDarkModeSet(ActivityUtils.isDarkModeEnabled(applicationManager));
        popup.setHeaderText(userName);

        final PopupWindow popupWindow = new ActivityUtils.PopupWindowBuilder(popup).build();
        currentPopup = popupWindow;

        popup.addDismissListener(view -> {
            popupWindow.dismiss();
            currentPopup = null;
            if (ActivityUtils.isDarkModeEnabled(applicationManager))
                userStatsButton.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        });
    }

    private void setLoadStatusText() {
        int resId = -1;
        if (!isBoardManagerReady())
            resId = R.string.load_status_board;
        else if (!versionChecked)
            resId = R.string.load_status_version;
        else if (!messageOfTheDayReceived)
            resId = R.string.load_status_message;
        else if (!banCheckComplete)
            resId = R.string.load_status_ban;

        if (resId > 0) {
            boardLoadStatusText.setVisibility(View.VISIBLE);
            boardLoadStatusText.setText(getString(resId));
        }
    }

    @Override
    public void onRemoteConfigLoadSuccess(RemoteLoadType loadType) {
        Logger.getInstance().debug(TAG, "onRemoteConfigLoadSuccess, loadType" + loadType);
        onRemoteConfigLoadSuccessInternal(loadType);
    }

    private void onRemoteConfigLoadSuccessInternal(RemoteLoadType loadType) {
        // Network checks are only required if the configuration file is new
        if (loadType == RemoteLoadType.CACHED_LOAD) {
            Logger.getInstance().debug(TAG, "Use cached version of remote configuration");
            messageOfTheDayReceived = true;
        } else if (loadType == RemoteLoadType.FULL_LOAD) {
            String messageUrl = applicationManager.getRemoteConfig().getStringProperty("messageUrl");
            applicationManager.getNetworkManager()
                    .createStringGetRequest(messageUrl, this::onMessageRetrieved);
        }

        onVersionRetrieved(applicationManager.getRemoteConfig().getVersion());
        this.networkModePending = false;
        this.networkStatusIcon.setVisibility(View.GONE);
        if (!this.applicationManager.getBoardManager().hasBoardsRemaining()) {
            this.applicationManager.getBoardManager().requestBoards();
        }
    }

    @Override
    public void onRemoteConfigLoadFailure() {
        Logger.getInstance().debug(TAG, "Remote config load failure");
        // Initialize the popup-window to show user stats
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        NoConnectionPopupView popup = new NoConnectionPopupView(inflater, null,
                view -> setOfflineMode(), view -> finish());

        popup.onDarkModeSet(ActivityUtils.isDarkModeEnabled(applicationManager));
        popup.setErrorCause(applicationManager.getRemoteError());
        this.networkModePending = false;

        // Run on UI thread so this doesn't get called before the activity is finished creating
        new Thread(() -> runOnUiThread(() -> currentPopup = new ActivityUtils.PopupWindowBuilder(popup)
                .withDimensionsWrapContent()
                .build())).start();
    }

    @Override
    public void onOfflineModeSet() {
        Toast.makeText(MenuActivity.this, R.string.info_offline_mode_active,
                Toast.LENGTH_SHORT).show();
        networkStatusIcon.setVisibility(View.VISIBLE);
        networkStatusIcon.setBackground(ResourcesCompat.getDrawable(getResources(),
                R.drawable.baseline_public_off_24, null));
        this.networkModePending = false;
    }

    private void dismissPopup() {
        if (currentPopup != null) {
            currentPopup.dismiss();
        }
        currentPopup = null;
    }

    private void toggleOfflineMode(boolean offline) {
        if (this.networkModePending) {
            return;
        }
        this.networkModePending = true;
        dismissPopup();
        if (offline) {
            setOfflineMode();
        } else {
            Toast.makeText(this, R.string.info_online_mode_active, Toast.LENGTH_SHORT).show();
            setOnlineMode();
        }
    }

    private void setOfflineMode() {
        applicationManager.enableOfflineMode();
        if (applicationManager.getRemoteState() == AppRemoteState.OFFLINE) {
            // Force set all properties so the game can start in offline mode even
            // without initial network connection
            this.versionChecked = true;
            this.messageOfTheDayReceived = true;
            this.banCheckComplete = true;
            this.latestVersion = currentVersion;
            dismissPopup();
        } else {
            createGameUnavailablePopup(applicationManager.getBoardManager().getLatestError());
        }
    }

    private void setOnlineMode() {
        // Callback will be invoked when online mode is enabled
        applicationManager.enableOnlineMode();
        if (AppRemoteState.PENDING == applicationManager.getRemoteState()) {
            networkStatusIcon.setVisibility(View.VISIBLE);
            networkStatusIcon.setBackground(ResourcesCompat.getDrawable(getResources(),
                    R.drawable.baseline_sync_24, null));
        }
    }

    private void createGameUnavailablePopup(Throwable errorMessage) {
        dismissPopup();
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        final GameUnavailablePopup popup = new GameUnavailablePopup(inflater, null, errorMessage,
                e -> {
            dismissPopup();
            finish();
        });

        this.currentPopup = new ActivityUtils.PopupWindowBuilder(popup)
                .withDimensions(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .build();
    }

    private boolean isBoardManagerReady() {
        final BoardManager boardManager = applicationManager.getBoardManager();
        return boardManager != null && !boardManager.isFetchingBoards();
    }
}