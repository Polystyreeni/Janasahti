package com.example.wordgame.managers;

import android.content.Context;
import android.util.Log;

import com.example.wordgame.activities.INetworkComponentInitListener;
import com.example.wordgame.activities.NetworkComponentInitResult;
import com.example.wordgame.utility.IoUtils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.CheckForNull;

/**
 * Board manager that uses local files to generate boards. Used when the application is in offline mode.
 * This class will attempt to create boards instantly from the constructor. Failure can be checked
 * by the return value of {@link #hasBoardsRemaining()}.
 */
public class LocalBoardManager extends BoardManager {
    public static final String LOCAL_BOARD_FILE_NAME = "wg_board_cache";
    private static final String TAG = "LocalBoardManager";
    private static final int BOARD_QUEUE_SIZE = 8;

    public LocalBoardManager(Context appContext) {
        super(appContext, BOARD_QUEUE_SIZE);
    }

    @Override
    public void init(INetworkComponentInitListener listener) {
        requestBoards();
        final NetworkComponentInitResult result = latestError == null
                ? NetworkComponentInitResult.successResponse()
                : NetworkComponentInitResult.errorResponse(latestError);
        listener.initialized(result);
    }

    @Override
    public void requestBoards() {
        if (hasBoardsRemaining()) {
            return;
        }
        readBoards();
    }

    @Override
    public boolean isFetchingBoards() {
        return false;
    }

    private void readBoards() {
        final CacheData cacheData = readCache();
        if (cacheData == null) {
            // Could not generate boards - application stop handled by application manager
            return;
        }

        final byte[] dataBytes = cacheData.compressed
                ? uncompressBoardFile(cacheData.bytes) : cacheData.bytes;
        try (InputStream uncompressedInputStream = new ByteArrayInputStream(dataBytes)) {
            generateBoards(uncompressedInputStream);
        } catch (IOException e) {
            Log.w(TAG, "Could not read cached data to boards", e);
        }
     }

    @CheckForNull
    private CacheData readCache() {
        // Cache data format: timestamp|isCompressed|byteCount|bytes
        try (final FileInputStream fis = IoUtils.getFileInputStream(this.appContext, LOCAL_BOARD_FILE_NAME);
             DataInputStream dis = new DataInputStream(fis)) {
            dis.readLong();
            final boolean isCompressed = dis.readBoolean();
            final int size = dis.readInt();
            final byte[] buffer = new byte[size];
            dis.readFully(buffer);
            return new CacheData(isCompressed, buffer);
        } catch (IOException e) {
            Log.w(TAG, "Board cache could not be read", e);
            latestError = e;
            return null;
        }
    }

    private static class CacheData {
        private final boolean compressed;
        private final byte[] bytes;

        public CacheData(boolean compressed, byte[] bytes) {
            this.compressed = compressed;
            this.bytes = bytes;
        }
    }
}
