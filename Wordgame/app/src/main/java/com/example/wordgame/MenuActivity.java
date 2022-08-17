package com.example.wordgame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import java.util.HashMap;
import java.util.Map;

public class MenuActivity extends AppCompatActivity {

    public static final String USERFILENAME = "userprofile";
    private final String userCollectionName = "wg_usernames";
    private final String TAG = "WordGame Menu";

    // Ui components
    private Button startButton;
    private EditText userNameField;
    private ProgressBar boardLoadProgressBar;

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

        mFireStore = FirebaseFirestore.getInstance();
        sessionReference = mFireStore.collection(userCollectionName);
        firebaseAuth = FirebaseAuth.getInstance();

        // Create the board in background while in main menu
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

        // Set username to the stored profile name (if exists)
        String userName = readUserProfile();
        if (!userName.isEmpty()) {
            userNameField.setText(userName);
        }

        signInAnonymously();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startButton.setClickable(true);

        // Create a new board when entering main menu
        if(boardThread == null) {
            boardThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    BoardManager.generateBoard();
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        firebaseUser = firebaseAuth.getCurrentUser();
    }

    private void startGame() {
        String username = userNameField.getText().toString();
        if(username.isEmpty()) {
            username = "Nimetön";
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

}