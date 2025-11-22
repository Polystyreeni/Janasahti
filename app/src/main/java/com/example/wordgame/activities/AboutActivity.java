package com.example.wordgame.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wordgame.R;
import com.example.wordgame.config.IApplicationConfiguration;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.managers.ApplicationManager;
import com.example.wordgame.models.AppRemoteState;
import com.example.wordgame.models.GameVersion;
import com.example.wordgame.models.RemoteLoadType;
import com.example.wordgame.popups.LicenseInfoPopup;
import com.example.wordgame.utility.ActivityUtils;
import com.example.wordgame.utility.IoUtils;
import com.example.wordgame.utility.TextUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

public class AboutActivity extends AppCompatActivity implements IRemoteConfigInitListener {
    private static final String TAG = "AboutActivity";
    private static final String GITHUB_BASE_URL = "https://github.com/";
    private static final String PROPERTY_EMAIL = "supportEmail";
    private static final String PROPERTY_GITHUB = "supportGitHub";
    private static final String PROPERTY_SOURCE_CODE = "gameSourceLink";

    // UI components
    private ConstraintLayout baseLayout;
    private TextView versionTextView;
    private TextView lastFetchedTextView;
    private View contactEmailIcon;
    private TextView contactEmailTextView;
    private View contactGitHubIcon;
    private TextView gitHubTextView;
    private TextView sourceCodeTextView;
    private Button licenseInfoButton;
    private Button exportLogButton;
    private Button returnButton;

    // Debug layout
    private LinearLayout debugLayout;
    private Button clearCacheButton;

    private ApplicationManager applicationManager;
    private PopupWindow currentPopup = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        initComponents();
        applicationManager = initApplicationManager();

        switch (applicationManager.getRemoteState()) {
            case READY:
                initValuesWithRemote();
                break;
            case OFFLINE:
            case UNAVAILABLE:
                initValuesOffline();
                break;
            default:
                // Pending state - will be updated through callback
                break;
        }

        final GameVersion version = applicationManager.getLocalConfig().getVersion();
        debugLayout.setVisibility(version.isBeta() ? View.VISIBLE : View.GONE);

        returnButton.setOnClickListener(l -> finish());
        exportLogButton.setOnClickListener(l -> createLogFile());
        clearCacheButton.setOnClickListener(l -> clearCache());

        setDarkMode();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (currentPopup != null) {
            currentPopup.dismiss();
            currentPopup = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (applicationManager != null) {
            Log.d(TAG, "Removed this activity from config init listeners");
            applicationManager.removeConfigInitListener(this);
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

    @Override
    public void onRemoteConfigLoadSuccess(RemoteLoadType loadType) {
        new Thread(() -> runOnUiThread(this::initValuesWithRemote));
    }

    @Override
    public void onRemoteConfigLoadFailure() {
        new Thread(() -> runOnUiThread(this::initValuesOffline));
    }

    @Override
    public void onOfflineModeSet() {
        // NOP, cannot happen
    }

    private void initComponents() {
        baseLayout = findViewById(R.id.aboutActivityLayout);
        versionTextView = findViewById(R.id.versionText);
        lastFetchedTextView = findViewById(R.id.lastFetchValue);
        contactEmailIcon = findViewById(R.id.contactEmailIcon);
        contactEmailTextView = findViewById(R.id.contactEmail);
        contactGitHubIcon = findViewById(R.id.contactGitHubIcon);
        gitHubTextView = findViewById(R.id.contactGitHub);
        sourceCodeTextView = findViewById(R.id.sourceCodeValue);
        licenseInfoButton = findViewById(R.id.thirdPartyInfoButton);
        exportLogButton = findViewById(R.id.exportLogButton);
        returnButton = findViewById(R.id.returnButton);
        debugLayout = findViewById(R.id.debugLayout);
        clearCacheButton = findViewById(R.id.clearCacheButton);
    }

    private ApplicationManager initApplicationManager() {
        final ApplicationManager manager = ApplicationManager.getInstance(this);
        if (AppRemoteState.PENDING == manager.getRemoteState()) {
            manager.addRemoteInitListener(this);
        }
        return manager;
    }

    private void initValuesWithRemote() {
        Objects.requireNonNull(this.applicationManager);
        final IApplicationConfiguration localConfig = applicationManager.getLocalConfig();
        final IApplicationConfiguration remoteConfig = applicationManager.getRemoteConfig();

        final GameVersion installedVersion = localConfig.getVersion();
        final GameVersion latestVersion = remoteConfig.getVersion();

        if (!Objects.equals(installedVersion, latestVersion)) {
            versionTextView.setText(getResources().getString(R.string.version_mismatch, installedVersion, latestVersion));
        } else {
            versionTextView.setText(installedVersion.toString());
        }

        final long timeStamp = remoteConfig.getLongProperty("timeStamp");
        lastFetchedTextView.setText(TextUtils.millisToFullDate(timeStamp));
        setContactInfoValues(localConfig);
        setLicenseInfoValues(localConfig);
    }

    private void initValuesOffline() {
        Objects.requireNonNull(this.applicationManager);
        final IApplicationConfiguration localConfig = applicationManager.getLocalConfig();
        final GameVersion installedVersion = localConfig.getVersion();

        versionTextView.setText(installedVersion.toString());
        lastFetchedTextView.setText("-");
        setContactInfoValues(localConfig);
        setLicenseInfoValues(localConfig);
    }

    private void setContactInfoValues(IApplicationConfiguration localConfig) {
        if (localConfig.hasProperty(PROPERTY_EMAIL)) {
            final String email = localConfig.getStringProperty(PROPERTY_EMAIL);
            contactEmailTextView.setText(email);
            contactEmailTextView.setClickable(true);
        } else {
            contactEmailTextView.setVisibility(View.GONE);
            contactEmailIcon.setVisibility(View.GONE);
        }

        if (localConfig.hasProperty(PROPERTY_GITHUB)) {
            final String gitHubUser = localConfig.getStringProperty(PROPERTY_GITHUB);
            final String linkText = String.format("<a href='%s%s'>%s </a>", GITHUB_BASE_URL,
                    gitHubUser, gitHubUser);
            gitHubTextView.setClickable(true);
            gitHubTextView.setText(TextUtils.getSpannedText(linkText));

        } else {
            gitHubTextView.setVisibility(View.GONE);
            contactGitHubIcon.setVisibility(View.GONE);
        }
    }

    private void setLicenseInfoValues(IApplicationConfiguration localConfig) {
        if (localConfig.hasProperty(PROPERTY_SOURCE_CODE)) {
            final String srcCodeUrl = localConfig.getStringProperty(PROPERTY_SOURCE_CODE);
            sourceCodeTextView.setText(srcCodeUrl);
            Linkify.addLinks(sourceCodeTextView, Linkify.ALL);
        } else {
            sourceCodeTextView.setVisibility(View.GONE);
        }

        licenseInfoButton.setOnClickListener(l -> openLicensePopup());
    }

    private void openLicensePopup() {
        if (currentPopup != null) {
            return;
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        LicenseInfoPopup popup = new LicenseInfoPopup(inflater, null, l -> {
            if (currentPopup != null) {
                currentPopup.dismiss();
                currentPopup = null;
            }
        });

        currentPopup = new ActivityUtils.PopupWindowBuilder(popup)
                .withDimensionsWrapContent()
                .build();
    }

    private void setDarkMode() {
        if (ActivityUtils.isDarkModeEnabled(applicationManager)) {
            baseLayout.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                    R.drawable.background_gradient_dark));

            TextUtils.setTextColorForViews(baseLayout, Color.WHITE);
            contactEmailIcon.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
            contactGitHubIcon.getBackground().mutate().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        } else {
            contactEmailIcon.getBackground().mutate().setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
            contactGitHubIcon.getBackground().mutate().setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
        }
    }

    // Debug functionality
    private void clearCache() {
        try {
            IoUtils.writeFile(getApplicationContext(), ApplicationManager.REMOTE_CONFIG_FILE, "");
            Toast.makeText(this, R.string.debug_cache_cleared_info, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Export log functionality
    private static final int CREATE_FILE = 666;
    private void createLogFile() {
        final String fileName = String.format("wglog-%s.txt",
                TextUtils.millisToFullDate(System.currentTimeMillis(), true));
        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, CREATE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, requestCode, data);
        if (requestCode == CREATE_FILE) {
            try (OutputStream os = getContentResolver().openOutputStream(data.getData());
                 final Writer writer = new OutputStreamWriter(os)
            ) {
                try {
                    final List<String> lines = IoUtils.readLinesFromFile(getApplicationContext(),
                            Logger.LOG_FILE_PREV);
                    for (String line : lines) {
                        writer.write(line);
                        writer.write(System.lineSeparator());
                    }
                    Toast.makeText(getApplicationContext(), getResources().getString(
                            R.string.export_log_status_text, data.getData().getPath()),
                            Toast.LENGTH_SHORT).show();
                } catch (IOException e2) {
                    Log.w(TAG, "Failed to read previous log file", e2);
                    Toast.makeText(getApplicationContext(), R.string.error_prev_log_file_not_found,
                            Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), getResources().getString(
                        R.string.error_exporting_log_file, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}