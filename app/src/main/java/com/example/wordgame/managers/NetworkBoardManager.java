package com.example.wordgame.managers;

import android.content.Context;

import com.example.wordgame.activities.INetworkComponentInitListener;
import com.example.wordgame.activities.NetworkComponentInitResult;
import com.example.wordgame.config.IApplicationConfiguration;
import com.example.wordgame.debug.Logger;
import com.example.wordgame.utility.IoUtils;
import com.example.wordgame.volley.extensions.INetworkErrorListener;
import com.example.wordgame.volley.extensions.INetworkResponse;
import com.example.wordgame.volley.extensions.NetworkByteResponse;
import com.example.wordgame.volley.extensions.NetworkStringResponse;
import com.google.common.base.Preconditions;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Board manager that fetches board files remotely.
 */
public class NetworkBoardManager extends BoardManager {
    private static final String TAG = "NetworkBoardManager";
    private static final String PROPERTY_BOARD_LIST_URL = "boardListUrl";
    private static final String PROPERTY_BOARDS_PER_LOAD = "boardsPerLoad";
    private static final String PROPERTY_BOARDS_COMPRESSED = "boardsCompressed";
    private static final long BOARD_FILE_CACHE_EXPIRE_MS = 86_400_000L;

    // Application settings
    private final boolean boardFilesCompressed;
    private final NetworkManager networkManager;
    private final INetworkErrorListener errorListener;
    private final String boardListUrl;
    private final List<String> boardUrls;

    // State
    private volatile boolean fetchingBoards;
    private boolean boardFileCached;

    public NetworkBoardManager(Context appContext, IApplicationConfiguration config,
                               NetworkManager networkManager, INetworkErrorListener errorListener) {
        super(appContext, config.getIntProperty(PROPERTY_BOARDS_PER_LOAD));

        this.boardFilesCompressed = config.getBooleanProperty(PROPERTY_BOARDS_COMPRESSED);
        this.boardListUrl = config.getStringProperty(PROPERTY_BOARD_LIST_URL);
        this.networkManager = networkManager;
        this.errorListener = errorListener;

        this.boardUrls = new CopyOnWriteArrayList<>();
        this.boardFileCached = false;
    }

    @Override
    public void init(INetworkComponentInitListener listener) {
        networkManager.createStringGetRequest(boardListUrl, response -> {
            onBoardListUrlRequested(response);
            listener.initialized(NetworkComponentInitResult.fromNetworkResponse(response));
        });
    }

    @Override
    public void requestBoards() {
        Preconditions.checkState(!boardUrls.isEmpty(), "Board urls not defined");
        fetchingBoards = true;

        final String url = boardUrls.get(rng.nextInt(boardUrls.size()));
        Logger.getInstance().debug(TAG, "Boards compressed = " + boardFilesCompressed);
        if (boardFilesCompressed) {
            networkManager.createInputStreamGetRequest(url, this::onBoardFileFetched);
        } else {
            networkManager.createStringGetRequest(url, this::onBoardFileFetched);
        }
    }

    @Override
    public boolean isFetchingBoards() {
        return fetchingBoards;
    }

    /**
     * Callback for when board url list is requested
     * @param response string network response
     */
    private void onBoardListUrlRequested(NetworkStringResponse response) {
        if (response.getContent() == null) {
            Logger.getInstance().warn(TAG, "Failed to fetch board files: " + response);
            errorListener.onNetworkError(response);
        } else {
            String content = response.getContent();
            boardUrls.clear();
            String[] urls = content.split(System.lineSeparator());
            boardUrls.addAll(Arrays.asList(urls));

            // Re-call requestBoards to load the boards
            if (fetchingBoards) {
                requestBoards();
            }
        }
    }

    /**
     * Callback for uncompressed boards from network request
     * @param response string response containing board file xml content as string
     */
    private void onBoardFileFetched(NetworkStringResponse response) {
        if (validateNetworkResponse(response)) {
            final String content = Objects.requireNonNull(response.getContent());
            try (InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
                generateBoards(inputStream);
                cacheBoardFile(content.getBytes(StandardCharsets.UTF_8), false);
            } catch (IOException e) {
                latestError = e;
                Logger.getInstance().warn(TAG, "Failed to parse board files", e);
            }
        }

        fetchingBoards = false;
    }

    /**
     * Callback for compressed boards from network request.
     * @param response network byte response containing zipped board file contents
     */
    private void onBoardFileFetched(NetworkByteResponse response) {
        if (validateNetworkResponse(response)) {
            final byte[] uncompressed = uncompressBoardFile(response.getContent());
            if (uncompressed.length == 0) {
                // Failed to generate boards
                return;
            }

            // Generate boards from uncompressed input
            try (InputStream uncompressedInputStream = new ByteArrayInputStream(uncompressed)) {
                generateBoards(uncompressedInputStream);
                cacheBoardFile(response.getContent(), true);
            } catch (IOException e) {
                latestError = e;
                Logger.getInstance().warn(TAG, "Failed to parse board files", e);
            }
        }

        fetchingBoards = false;
    }

    private boolean validateNetworkResponse(INetworkResponse response) {
        if (response.isError()) {
            errorListener.onNetworkError(response);
            return false;
        }
        return true;
    }

    private void cacheBoardFile(byte[] content, boolean isCompressed) {
        if (boardFileCached) {
            return;
        }
        final String fileName = LocalBoardManager.LOCAL_BOARD_FILE_NAME;

        // Check file and see if cached value is still valid
        boolean useCached;
        try (final FileInputStream fis =
                     IoUtils.getFileInputStream(this.appContext, fileName);
        DataInputStream dis = new DataInputStream(fis)) {
            final long timeStamp = dis.readLong();
            useCached = System.currentTimeMillis() - timeStamp < BOARD_FILE_CACHE_EXPIRE_MS;
        } catch (IOException e) {
            useCached = false;
        }

        if (useCached) {
            boardFileCached = true;
            return;
        }

        // Cache new file
        try (final FileOutputStream fos = IoUtils.getFileOutputStream(this.appContext, fileName);
             final BufferedOutputStream bos = new BufferedOutputStream(fos);
             final DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeLong(System.currentTimeMillis());
            dos.writeBoolean(isCompressed);
            dos.writeInt(content.length);
            dos.write(content);
            boardFileCached = true;
        } catch (IOException e) {
            Logger.getInstance().warn(TAG, "Failed to write board cache", e);
        }
    }
}
