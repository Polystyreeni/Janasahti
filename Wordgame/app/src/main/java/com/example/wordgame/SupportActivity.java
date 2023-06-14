package com.example.wordgame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wordgame.usermanagement.EmailSendData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class SupportActivity extends AppCompatActivity {

    private static final String TAG = "SupportActivity";

    // The subject of the composed email (message type will be appended)
    private final String messageHeader = "Janasahti-Support: ";

    public static final String EXTRA_MESSAGE =
            "com.example.wordgame.extra.MESSAGE";

    // UI components
    private RelativeLayout baseLayout;
    private TextView supportHeaderText;
    private TextView hintHeaderText;

    // Activity state
    private String activeMessageType;

    private CollectionReference sessionReference;
    private FirebaseAuth firebaseAuth;

    private boolean emailDataWritten = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        Spinner messageTypeSpinner = findViewById(R.id.supportTypeSpinner);
        EditText contentText = findViewById(R.id.supportEditText);
        Button sendMessageButton = findViewById(R.id.sendSupportMessage);
        baseLayout = findViewById(R.id.layout_support);
        supportHeaderText = findViewById(R.id.supportHeader);
        hintHeaderText = findViewById(R.id.supportTypeHint);

        final String[] supportTypes = new String[]{"Avunpyyntö", "Bugi", "Palaute", "Puuttuva sana", "Muu"};

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, supportTypes);
        arrayAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        messageTypeSpinner.setAdapter(arrayAdapter);

        messageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                activeMessageType = supportTypes[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Intent intent = getIntent();
        String userId = intent.getStringExtra(SupportActivity.EXTRA_MESSAGE);

        sendMessageButton.setOnClickListener(view -> {
            if (!GameSettings.UseFirebase() || firebaseAuth == null) {
                Toast.makeText(SupportActivity.this, getString(R.string.player_banned_not_available),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String[] address = NetworkConfig.getSupportEmail();
            String subject = messageHeader + activeMessageType;
            String content = userId + System.lineSeparator() + contentText.getText().toString();
            composeEmail(address, subject, content);
        });

        // Initialize Firebase
        FirebaseFirestore mFireStore = FirebaseFirestore.getInstance();
        String collectionName = "wg_emails";
        sessionReference = mFireStore.collection(collectionName);
        firebaseAuth = FirebaseAuth.getInstance();

        setDarkMode();
    }

    /**
     * Compose an email based on user written content
     * @param address the addresses to send this email to
     * @param subject the 'title' of the email
     * @param content the body of the email
     */
    private void composeEmail(String[] address, String subject, String content) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, address);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, content);

        // Write data to Firebase, this way we'll know who sends emails
        if (!emailDataWritten) {
            EmailSendData data = new EmailSendData();
            data.setUserId(firebaseAuth.getUid());
            data.setDate(Calendar.getInstance().getTime().toString());
            String documentId = data.getUserId();

            sessionReference.document(documentId).set(data)
                    .addOnSuccessListener(aVoid -> emailDataWritten = true)
                    .addOnFailureListener(e -> Log.w(TAG, "Error writing sent data", e));
        }

        startActivity(Intent.createChooser(intent, getResources().getString(R.string.support_email_via)));
    }

    private void setDarkMode() {
        if (UserSettings.getDarkModeEnabled() > 0) {
            baseLayout.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_gradient_dark));
            supportHeaderText.setTextColor(Color.WHITE);
            hintHeaderText.setTextColor(Color.WHITE);
        }
    }
}