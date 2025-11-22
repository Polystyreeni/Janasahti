package com.example.wordgame.managers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.GuardedBy;

import com.example.wordgame.activities.NetworkComponentInitResult;
import com.example.wordgame.config.AppLocalFileConfig;
import com.example.wordgame.config.AppLocalPropertiesConfig;
import com.example.wordgame.activities.IConfigChangeListener;
import com.example.wordgame.activities.IRemoteConfigInitListener;
import com.example.wordgame.config.AppRemoteConfig;
import com.example.wordgame.config.ConfigUtility;
import com.example.wordgame.config.IApplicationConfiguration;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.models.AppRemoteState;
import com.example.wordgame.models.GameVersion;
import com.example.wordgame.models.RemoteLoadType;
import com.example.wordgame.utility.ExpiringSet;
import com.example.wordgame.utility.IoUtils;
import com.example.wordgame.volley.extensions.INetworkErrorListener;
import com.example.wordgame.volley.extensions.INetworkResponse;
import com.example.wordgame.volley.extensions.InternalNetworkError;
import com.example.wordgame.volley.extensions.NetworkStringResponse;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Manager for the main logic of the application. This class manages the global state of the
 * application, managing access to game operation mode, application configurations and
 * persistent data.
 */
public class ApplicationManager {
    private static final String TAG = "ApplicationManager";
    public static final String REMOTE_CONFIG_FILE = "wg_remote_config";
    private static final String LOCAL_OVERRIDE_FILE = "wg_local_config_override";
    private static final long CONFIG_URL_REFRESH_MS = 3_600_000L;
    private static ApplicationManager instance;
    private final Context appContext;

    // App configurations
    private IApplicationConfiguration localConfig;
    private IApplicationConfiguration remoteConfig;

    // Managers
    private NetworkManager networkManager;
    private BoardManager boardManager;
    private UserSettingsManager userSettingsManager;
    private UserStatsManager userStatsManager;
    private VersionManager versionManager;

    // Manager state
    private volatile AppRemoteState remoteState;
    private List<String> configFileUrls;
    private String remoteErrorMessage = "";

    /** Keeps track of configuration urls that have already been used. */
    private final ExpiringSet<Integer> usedRemoteConfigurations =
            new ExpiringSet<>(new HashSet<>(), CONFIG_URL_REFRESH_MS);

    /** Cached remote state when user toggles between offline/online mode */
    private RemoteStateCache remoteStateCache = null;

    private final Object initListenerLock = new Object();
    @GuardedBy("initListenerLock")
    private final Set<IRemoteConfigInitListener> configInitListeners = new HashSet<>();
    private final Set<IConfigChangeListener> configChangeListeners = new HashSet<>();

    private final INetworkErrorListener networkErrorListener = new INetworkErrorListener() {
        public void onNetworkError(INetworkResponse errorResponse) {
            Preconditions.checkArgument(errorResponse.isError(),
                    "onNetworkError called without an error!");

            if (remoteState == AppRemoteState.PENDING) {
                Logger.getInstance().debug(TAG, "Already started fetching new remote config");
                return;
            }

            remoteErrorMessage = errorResponse.getErrorMessage();
            remoteState = AppRemoteState.PENDING;
            remoteStateCache = null;

            for (IConfigChangeListener listener : configChangeListeners) {
                Logger.getInstance().debug(TAG, "Notify config change listener " + listener.getClass());
                listener.onBeginRemoteConfigChange();
            }

            // Reset remote config
            requestRemoteConfig();
        }
    };

    private ApplicationManager(Context ctx) {
        this.appContext = ctx.getApplicationContext();
        this.remoteState = AppRemoteState.UNAVAILABLE;
        this.localConfig = initLocalConfig(ctx);

        // Set logger mode based on config
        setLoggerMode();

        initializeConfigUrls();
        initializeLocalManagers();
        initializeRemoteConfig();
    }

    public static synchronized ApplicationManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new ApplicationManager(ctx);
        }

        return instance;
    }

    public AppRemoteState getRemoteState() { return remoteState; }

    /**
     * Adds a listener that will be invoked once the remote configuration has been fetched.
     * @param listener listener to add
     * @return true if listener was added
     */
    public boolean addRemoteInitListener(IRemoteConfigInitListener listener) {
        synchronized (initListenerLock) {
            Logger.getInstance().debug(TAG, "Add listener: " + listener.getClass());
            return this.configInitListeners.add(listener);
        }
    }

    /**
     * Removes a config initialization listener.
     * @param listener listener to remove
     * @return true if listener was removed.
     */
    public boolean removeConfigInitListener(IRemoteConfigInitListener listener) {
        synchronized (this.initListenerLock) {
            Logger.getInstance().debug(TAG, "Removing listener: " + listener.getClass());
            return this.configInitListeners.remove(listener);
        }
    }

    /**
     * Adds a listener that gets notified when a backup connection is invoked
     * @param listener listener to add
     */
    public void addRemoteChangeListener(IConfigChangeListener listener) {
        this.configChangeListeners.add(listener);
    }

    /**
     * Removes a listener from receiving notifications on backup connection changes
     * @param listener listener to remove
     * @return true if the listener was found and removed
     */
    public boolean removeConfigChangeListener(IConfigChangeListener listener) {
        final boolean removed =  this.configChangeListeners.remove(listener);
        Logger.getInstance().debug(TAG, "removeConfigChangeListener() - removed listener: " + removed);
        return removed;
    }

    public BoardManager getBoardManager() {
        return this.boardManager;
    }
    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }
    public IApplicationConfiguration getLocalConfig() {
        return this.localConfig;
    }
    public IApplicationConfiguration getRemoteConfig() {
        return this.remoteConfig;
    }
    public UserStatsManager getUserStatsManager() {
        return this.userStatsManager;
    }
    public UserSettingsManager getUserSettingsManager() {
        return this.userSettingsManager;
    }

    /**
     * If remote state is not available, returns the latest remote error message.
     * @return latest error message, or empty string if no errors have occurred.
     */
    public String getRemoteError() {
        if (remoteState == AppRemoteState.UNAVAILABLE) {
            return remoteErrorMessage;
        }

        return "";
    }

    /**
     * Sets the application to offline mode. Invokes action <code>onOfflineModeSet</code> if
     * enabling is successful
     */
    public void enableOfflineMode() {
        this.boardManager = new LocalBoardManager(appContext);
        this.remoteState = AppRemoteState.OFFLINE;

        // This is called instantly on the same thread
        this.boardManager.init(result -> {
            if (result.isSuccess()) {
                notifyOfflineInit();
            } else {
                this.remoteState = AppRemoteState.UNAVAILABLE;
            }
        });
    }

    /**
     * Sets the application to online mode. Invokes action <code>onRemoteConfigLoadSuccess</code>
     * if online mode is set successfully and <code>onRemoteConfigLoadFailure</code> if not
     * successful.
     */
    public void enableOnlineMode() {
        Logger.getInstance().debug(TAG, "enableOnlineMode()");
        if (this.remoteStateCache == null || this.remoteStateCache.remoteState != AppRemoteState.READY) {
            Logger.getInstance().info(TAG, "Cache is invalid, attempting to re-fetch remote configuration");
            resetRemoteState();
            initializeRemoteConfig();
            return;
        }

        this.boardManager = this.remoteStateCache.boardManager;
        this.remoteState = this.remoteStateCache.remoteState;
        notifyConfigInitListeners(true, RemoteLoadType.CACHED_LOAD);
    }

    private IApplicationConfiguration initLocalConfig(Context ctx) {
        final AppLocalPropertiesConfig propertiesConfig = new AppLocalPropertiesConfig(ctx);
        IApplicationConfiguration overrideConfig =
                new AppLocalFileConfig(IoUtils.readLinesFromFile(ctx, LOCAL_OVERRIDE_FILE));

        // Override does not exist or is empty - use properties file
        if (overrideConfig.isEmpty()) {
            return propertiesConfig;
        }

        final GameVersion propertiesVersion = propertiesConfig.getVersion();
        final GameVersion overrideVersion = overrideConfig.getVersion();

        Logger.getInstance().debug(TAG, "Comparing local property file version "
                + propertiesVersion.toString() + " to override version " + overrideVersion.toString());

        // This can happen with new version updates where the stored override is still from an old
        // app version. In these cases we want to clear the override configuration
        if (propertiesVersion.compareTo(overrideVersion) > 0) {
            Logger.getInstance().debug(TAG, "Override configuration is old - replace with default configuration");
            overrideConfig = new AppLocalFileConfig(propertiesConfig.getPropertyMap());
            writeLocalConfig(ctx, overrideConfig);
            return propertiesConfig;
        } else {
            return overrideConfig;
        }
    }

    private void initializeConfigUrls() {
        this.configFileUrls = new ArrayList<>();
        String urlProperty = this.localConfig.getStringProperty("configurationUrls");
        final String delimiter = ", ";
        if (urlProperty.contains(delimiter)) {
            String[] urls = urlProperty.split(delimiter);
            configFileUrls.addAll(Arrays.asList(urls));
        } else {
            configFileUrls.add(urlProperty);
        }
    }

    private void initializeLocalManagers() {
        this.networkManager = new NetworkManager(this.appContext);
        this.userSettingsManager = new UserSettingsManager();
        this.userStatsManager = new UserStatsManager();
        this.versionManager = new VersionManager(this.userSettingsManager, this.userStatsManager,
                (ctx, from, to) -> {
            try {
                // Resets cached configs so they're re-fetched for new version
                IoUtils.writeFile(ctx, REMOTE_CONFIG_FILE, "");
            } catch (IOException e) {
                Logger.getInstance().warn(TAG, "Failed to clear config cache after version change", e);
            }
        });
        this.versionManager.performMigrationIfNeeded(this.appContext, this.localConfig.getVersion());
    }

    /**
     * Initialize managers that require a remote config instance for initialization
     * Runs initialization on each manager to see if they are valid.
     * @param backgroundLoad if true, result is cached in background instead of setting actively.
     * @param cached was the configuration loaded from a cached file?
     */
    private void initializeRemoteManagers(final boolean backgroundLoad, final boolean cached) {
        Logger.getInstance().debug(TAG, "InitializeRemoteManagers()");

        final BoardManager boardManager1 = new NetworkBoardManager(this.appContext,
                this.remoteConfig, this.networkManager, this.networkErrorListener);

        final RemoteLoadType loadType = backgroundLoad ? RemoteLoadType.BACKGROUND_LOAD
                : (cached ? RemoteLoadType.CACHED_LOAD : RemoteLoadType.FULL_LOAD);

        // NOTE: May be handled in a worked thread and be delayed
        boardManager1.init(result -> finalizeRemoteManagers(result, boardManager1, loadType));
    }

    private void finalizeRemoteManagers(NetworkComponentInitResult result,
                                        BoardManager boardManager, RemoteLoadType loadType) {
        if (result.isSuccess()) {
            this.remoteStateCache = new RemoteStateCache(boardManager, AppRemoteState.READY);
            if (loadType == RemoteLoadType.BACKGROUND_LOAD) {
                Logger.getInstance().debug(TAG, "Remote config loaded in background, not setting active state");
            } else {
                this.boardManager = boardManager;
                this.remoteState = AppRemoteState.READY;
            }
            notifyConfigInitListeners(true, loadType);
        } else {
            // Force set unavailable state so that error listener is allowed to run
            this.remoteState = AppRemoteState.UNAVAILABLE;
            this.networkErrorListener.onNetworkError(new InternalNetworkError(result.getErrorMessage(),
                    result.getThrowable()));
        }
    }

    private void initializeRemoteConfig() {
        long now = System.currentTimeMillis();
        List<String> remoteConfigLines = IoUtils.readLinesFromFile(appContext, REMOTE_CONFIG_FILE);
        if (remoteConfigLines.isEmpty()) {
            Logger.getInstance().debug(TAG, "Remote configuration does not exist - fetching...");
            requestRemoteConfig();
        } else {
            IApplicationConfiguration config = AppRemoteConfig.parseFromFile(
                    String.join(System.lineSeparator(), remoteConfigLines));
            long configTimeStamp = config.getLongProperty(AppRemoteConfig.PROPERTY_TIME_STAMP);
            if (now - configTimeStamp > localConfig.getLongProperty("localFileCacheExpirationTimeMs")) {
                Logger.getInstance().debug(TAG, "Remote configuration expired - refreshing...");
                requestRemoteConfig();
            } else {
                Logger.getInstance().debug(TAG, String.format("Use cached remote config file, time diff = %d ms",
                        now - configTimeStamp));
                this.remoteState = AppRemoteState.PENDING;
                setRemoteConfigFile(config, true);
            }
        }
    }

    private void requestRemoteConfig() {
        remoteState = AppRemoteState.PENDING;
        if (usedRemoteConfigurations.size() >= configFileUrls.size()) {
            failRemoteConfigFetch();
            return;
        }

        final String urlToFetch = findNextConfigUrl();
        if (urlToFetch.isEmpty()) {
            failRemoteConfigFetch();
        } else {
            networkManager.createStringGetRequest(urlToFetch, this::onConfigurationFetched);
        }
    }

    private void failRemoteConfigFetch() {
        Logger.getInstance().debug(TAG, "Failed to acquire any remote configuration!");
        remoteState = AppRemoteState.UNAVAILABLE;
        notifyConfigInitListeners(false, RemoteLoadType.FULL_LOAD);
    }

    private String findNextConfigUrl() {
        for (String url : configFileUrls) {
            final int hashCode = url.hashCode();
            if (usedRemoteConfigurations.add(hashCode)) {
                return url;
            }
        }

        return "";
    }

    private void onConfigurationFetched(NetworkStringResponse response) {
        final String config = response.getContent();
        if (config == null) {
            this.remoteErrorMessage = response.getErrorMessage();
            Logger.getInstance().debug(TAG,
                    "Failed to fetch remote configuration, trying next backup connection");
            requestRemoteConfig();
        } else {
            setRemoteConfigFile(AppRemoteConfig.parseFromFile(config), false);
        }
    }

    private void setRemoteConfigFile(@Nonnull IApplicationConfiguration config, boolean cached) {
        this.remoteConfig = config;

        // If file contains override properties, override local configuration
        if (!cached) {
            final Collection<String> overrides = ConfigUtility.getOverridablePropertiesFrom(config);
            if (!overrides.isEmpty()) {
                Logger.getInstance().debug(TAG, String.format("Overriding the following local properties: %s",
                        overrides));
                overrideLocalFile(config, overrides);
            }

            // Cache remote config
            writeRemoteConfig(config);
        }

        // Offline = user has explicitly set offline mode before configuration was fetched
        final boolean backgroundLoad = AppRemoteState.OFFLINE == this.remoteState;
        initializeRemoteManagers(backgroundLoad, cached);
    }

    private void overrideLocalFile(IApplicationConfiguration newConfig,
                                   Collection<String> overridableProperties) {

        final GameVersion currentVersion = this.localConfig.getVersion();
        final GameVersion newConfigVersion = newConfig.getVersion();
        if (newConfigVersion.compareTo(currentVersion) != 0) {
            Logger.getInstance().debug(TAG, "Version file mismatch - not overriding local config");
            return;
        }

        final Map<String, String> localProperties = new HashMap<>(this.localConfig.getPropertyMap());
        for (String property : overridableProperties) {
            String value = newConfig.getStringProperty(property);
            localProperties.put(property, value);
        }

        this.localConfig = new AppLocalFileConfig(localProperties);
        writeLocalConfig(this.appContext, this.localConfig);
        setLoggerMode();
    }

    private void writeLocalConfig(Context ctx, IApplicationConfiguration config) {
        final String fileContent = config.asString();
        try {
            IoUtils.writeFile(ctx, LOCAL_OVERRIDE_FILE, fileContent);
        } catch (IOException e) {
            Log.w(TAG, "Failed to write new properties to override file", e);
        }
    }

    private void writeRemoteConfig(IApplicationConfiguration config) {
        final String content = config.asString();
        try {
            IoUtils.writeFile(appContext, REMOTE_CONFIG_FILE, content);
        } catch (IOException e) {
            Log.w(TAG, "Failed to write remote config to cache", e);
        }
    }

    private void notifyConfigInitListeners(boolean success, RemoteLoadType loadType) {
        synchronized (initListenerLock) {
            Logger.getInstance().debug(TAG, "Notifying listeners, success = " + success);
            for (IRemoteConfigInitListener l : this.configInitListeners) {
                Logger.getInstance().debug(TAG, "Notify listener " + l.getClass());
                if (success) {
                    l.onRemoteConfigLoadSuccess(loadType);
                } else {
                    l.onRemoteConfigLoadFailure();
                }
            }
        }
    }

    private void notifyOfflineInit() {
        synchronized (initListenerLock) {
            for (IRemoteConfigInitListener l : this.configInitListeners) {
                l.onOfflineModeSet();
            }
        }
    }

    private void setLoggerMode() {
        final String logModeProperty = "logLevel";
        if (this.localConfig.hasProperty(logModeProperty)) {
            try {
                final Logger.LoggerMode mode = Logger.LoggerMode.valueOf(
                        this.localConfig.getStringProperty(logModeProperty));
                Logger.getInstance().setLoggerMode(mode);
            } catch (Exception e) {
                Log.w(TAG, "Invalid logger mode, logger is off", e);
            }
        }
    }

    private void resetRemoteState() {
        this.usedRemoteConfigurations.clear();
        this.boardManager = null;
    }

    @Immutable
    private static class RemoteStateCache {
        private final BoardManager boardManager;
        private final AppRemoteState remoteState;

        public RemoteStateCache(BoardManager boardManager, AppRemoteState remoteState) {
            this.boardManager = boardManager;
            this.remoteState = remoteState;
        }
    }
}