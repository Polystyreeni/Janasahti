package com.example.wordgame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
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
    private Button settingsButton;
    private ConstraintLayout layout;
    private View mainMenuBackground;
    private TextView versionTextView;

    private Thread boardThread = null;

    // Firebase
    private FirebaseFirestore mFireStore;
    private CollectionReference sessionReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    Handler boardLoadHandler = new Handler();
    Runnable boardLoadRunnable = new Runnable() {
        @Override
        public void run() {
            if(boardThread.isAlive()) {
                boardLoadHandler.postDelayed(this, 500);
            }

            else {
                boardLoadProgressBar.setVisibility(View.GONE);
                startGame();
                boardLoadHandler.removeCallbacks(boardLoadRunnable);
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
        settingsButton = findViewById(R.id.settingsButton);
        layout = findViewById(R.id.menuLayout);
        mainMenuBackground = findViewById(R.id.mainMenuBackView);
        versionTextView = findViewById(R.id.versionText);

        mFireStore = FirebaseFirestore.getInstance();
        sessionReference = mFireStore.collection(userCollectionName);
        firebaseAuth = FirebaseAuth.getInstance();

        // Create the board in background while in main menu
        BoardManager.setRandomSeed(Calendar.getInstance().getTimeInMillis());
        boardThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BoardManager.generateBoard();
            }
        });

        boardThread.start();

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(boardThread.isAlive()) {
                    Log.d(TAG, "Board thread alive, waiting...");
                    boardLoadProgressBar.setVisibility(View.VISIBLE);
                    boardLoadHandler.postDelayed(boardLoadRunnable, 0);
                    startButton.setClickable(false);
                    return;
                }

                startGame();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initialize the popup-window to show all words and score
                LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.game_settings_popup, null);

                CheckBox darkBox = popupView.findViewById(R.id.settingCheckboxColor);
                CheckBox oledBox = popupView.findViewById(R.id.settingCheckboxOled);
                Button returnButton = popupView.findViewById(R.id.settingsReturnButton);

                darkBox.setChecked(GameSettings.getDarkModeEnabled() > 0);
                oledBox.setChecked(GameSettings.getOledProtectionEnabled() > 0);

                darkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        int value = b ? 1 : 0;
                        GameSettings.setDarkModeEnabled(value);
                        updateBackground();
                    }
                });

                oledBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        int value = b ? 1 : 0;
                        GameSettings.setOledProtectionEnabled(value);
                    }
                });

                int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, false);
                popupWindow.setAnimationStyle(R.style.Animation_AppCompat_Dialog);
                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

                returnButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        popupWindow.dismiss();
                    }
                });
            }
        });

        // Set username to the stored profile name (if exists)
        String userName = readUserProfile();
        if (!userName.isEmpty()) {
            userNameField.setText(userName);
        }

        versionTextView.setText(VersionManager.getVersion());

        readSettingsFile();
        updateBackground();
        VersionManager.getLatestVersion(this);

        signInAnonymously();
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

        // Create a new board when entering main menu
        if(BoardManager.getShouldGenerateBoard()) {
                boardThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BoardManager.generateBoard();
                    }
                });
                boardThread.start();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        firebaseUser = firebaseAuth.getCurrentUser();
    }

    private void updateBackground() {
        if(GameSettings.getDarkModeEnabled() > 0) {
            layout.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_gradient_dark));
            mainMenuBackground.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.rounded_corner_black));
            userNameField.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        }
        else {
            layout.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_gradient));
            mainMenuBackground.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.rounded_corner_gold));
            userNameField.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        }
    }

    private void startGame() {
        String username = userNameField.getText().toString();
        if(username.isEmpty()) {
            username = "Nimet??n";
        }

        // We're using the / sign for passing data, don't allow user to have it in their username
        if(username.contains("/"))
            username = username.replaceAll("/", " ");

        // Name can't be too long, will cause issues with scaling
        if(username.length() > GameSettings.getUsernameMaxLength()) {
            username = username.substring(0, GameSettings.getUsernameMaxLength());
        }

        if(firebaseUser != null) {
            createUserProfile(username);
            writeSettingsFile();
            sessionReference = mFireStore.collection(userCollectionName);
            String document = firebaseUser.getUid();

            Map<String, Object> docData = new HashMap<>();
            docData.put("userName", username);
            sessionReference.document(document).set(docData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Userprofile successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing Userprofile", e);
                        }
                    });
        }

        // Start game activity
        Intent intent = new Intent(MenuActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_MESSAGE, username);
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
            String name = bufferedReader.readLine();
            return name;
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

            String settings = String.valueOf(GameSettings.getDarkModeEnabled())
                    + "/" + String.valueOf(GameSettings.getOledProtectionEnabled());

            stream.write(settings.getBytes(StandardCharsets.UTF_8));
            stream.close();
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readSettingsFile() {
        FileInputStream inputStream;
        try {
            Context ctx = getApplicationContext();
            inputStream = ctx.openFileInput(SETTINGSFILENAME);
            InputStreamReader inputReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            String settingsStr = bufferedReader.readLine();
            String[] settings = settingsStr.split("/");
            if(settings.length == 2) {
                GameSettings.setDarkModeEnabled(Integer.parseInt(settings[0]));
                GameSettings.setOledProtectionEnabled(Integer.parseInt(settings[1]));
            }
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Sign in anonymously to Firebase
    private void signInAnonymously() {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
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
                    }
                });
    }

    public void onVersionRetrieved(String latestVersion) {
        if(latestVersion.isEmpty()) {
            Toast.makeText(MenuActivity.this, R.string.error_version, Toast.LENGTH_SHORT).show();
            return;
        }

        String currentVersion = VersionManager.getVersion();
        if(!currentVersion.equals(latestVersion)) {
            // Create popup with update prompt
            showUpdatePopup(latestVersion);
        }
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

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });
    }
}