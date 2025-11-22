package com.example.wordgame.debug;

import android.content.Context;
import android.util.Log;

import androidx.annotation.GuardedBy;

import com.example.wordgame.utility.IoUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wrapper for the base Android logger that also support saving logs to files.
 */
public class Logger {
    public enum LoggerMode {
        UNINITIALIZED(false),
        OFF(false),
        DEFAULT(false),
        WARN_FILE(true),
        INFO_FILE(true),
        DEBUG_FILE(true);

        private final boolean fileBased;
        LoggerMode(boolean fileBased) {
            this.fileBased = fileBased;
        }
    }

    private enum LogLevel {
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E"),
        FORCE_WRITE("FC"),
        POISON("P");

        private final String name;
        LogLevel(String name) {
            this.name = name;
        }
    }
    public static final String LOG_FILE_NAME = "logfile.log";
    public static final String LOG_FILE_PREV = "logfile.prev.log";
    private static final String TAG = "Logger";
    private static final LogMessage FORCE_WRITE = new LogMessage(TAG, LogLevel.FORCE_WRITE, "FC", null);
    private static final LogMessage POISON = new LogMessage(TAG, LogLevel.POISON,"POISON", null);
    private static final int QUEUE_SIZE = 50;
    private static final int UNINITIALIZED_BUFFER_CAPACITY = 30;
    private final Context appCtx;
    private volatile LoggerMode loggerMode;
    private final BlockingQueue<LogMessage> messageQueue;

    // Uninitialized buffer - for capturing log messages before remote configuration is fetched
    private final Object bufferLock = new Object();
    @GuardedBy("bufferLock")
    private final List<LogMessage> uninitializedBuffer = new ArrayList<>(UNINITIALIZED_BUFFER_CAPACITY);

    private Thread loggerThread = null;
    private static Logger instance;

    public static Logger initialize(@Nonnull Context appCtx, LoggerMode mode) {
        instance = new Logger(Objects.requireNonNull(appCtx), mode);
        return instance;
    }

    public static Logger getInstance() {
        return instance;
    }

    private Logger(Context ctx, LoggerMode mode) {
        this.appCtx = ctx.getApplicationContext();
        this.loggerMode = mode;
        this.messageQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);

        if (mode.fileBased) {
            initLoggerThread();
        }
    }

    public void setLoggerMode(LoggerMode mode) {
        if (this.loggerMode == mode) {
            return;
        }

        this.loggerMode = mode;
        Log.d(TAG, "Logger mode set to " + mode);
        if (this.loggerThread != null && !mode.fileBased) {
            stop();
            synchronized (this.bufferLock) {
                this.uninitializedBuffer.clear();
            }
        } else if (mode.fileBased) {
            initLoggerThread();
        }
    }

    public void stop() {
        if (this.loggerThread != null) {
            try {
                this.messageQueue.put(POISON);
                this.loggerThread.join();
                this.loggerThread = null;
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted");
            }
        }
    }

    public void debug(@Nonnull String tag, @Nonnull String msg) {
        switch (this.loggerMode) {
            case UNINITIALIZED:
                queueUninitializedMessage(tag, LogLevel.DEBUG, msg, null);
                break;
            case OFF:
                break;
            case DEFAULT:
            case WARN_FILE:
            case INFO_FILE:
                Log.d(TAG, msg);
                break;
            case DEBUG_FILE:
                queueLogMessage(tag, LogLevel.DEBUG, msg, null);
                Log.d(TAG, msg);
                break;
            default:
                throw new IllegalArgumentException("Unknown log level" + this.loggerMode);
        }
    }

    public void info(@Nonnull String tag, @Nonnull String msg) {
        switch (this.loggerMode) {
            case UNINITIALIZED:
                queueUninitializedMessage(tag, LogLevel.INFO, msg, null);
                break;
            case OFF:
                break;
            case DEFAULT:
            case WARN_FILE:
                Log.i(tag, msg);
                break;
            case INFO_FILE:
            case DEBUG_FILE:
                Log.i(tag, msg);
                queueLogMessage(tag, LogLevel.INFO, msg, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown log level " + this.loggerMode);
        }
    }

    public void warn(@Nonnull String tag, @Nonnull String msg) {
        warn(tag, msg, null);
    }

    public void warn(@Nonnull String tag, @Nonnull String msg, @Nullable Throwable cause) {
        switch (this.loggerMode) {
            case UNINITIALIZED:
                queueUninitializedMessage(tag, LogLevel.WARN, msg, null);
                break;
            case OFF:
                break;
            case DEFAULT:
                Log.w(tag, msg, cause);
                break;
            case WARN_FILE:
            case INFO_FILE:
            case DEBUG_FILE:
                Log.w(tag, msg, cause);
                queueLogMessage(tag, LogLevel.WARN, msg, cause);
                break;
            default:
                throw new IllegalArgumentException("Unknown log level " + this.loggerMode);
        }
    }

    public void error(@Nonnull String tag, @Nonnull String msg) {
        error(tag, msg, null);
    }

    public void error(@Nonnull String tag, @Nonnull String msg, @Nullable Throwable cause) {
        switch (this.loggerMode) {
            case UNINITIALIZED:
                queueUninitializedMessage(tag, LogLevel.ERROR, msg, cause);
                break;
            case OFF:
                break;
            case DEFAULT:
                Log.e(tag, msg);
                break;
            case WARN_FILE:
            case INFO_FILE:
            case DEBUG_FILE:
                Log.e(tag, msg, cause);
                queueLogMessage(tag, LogLevel.ERROR, msg, cause);
                break;
            default:
                throw new IllegalArgumentException("Unknown log level " + this.loggerMode);
        }
    }

    private void queueUninitializedMessage(@Nonnull String tag, @Nonnull LogLevel logLevel,
                                           @Nonnull String msg, @Nullable Throwable cause) {
        synchronized (this.bufferLock) {
            if (this.uninitializedBuffer.size() < UNINITIALIZED_BUFFER_CAPACITY) {
                this.uninitializedBuffer.add(new LogMessage(tag, logLevel, msg, cause));
            }
        }
    }

    private void queueLogMessage(@Nonnull String tag, @Nonnull LogLevel logLevel,
                                 @Nonnull String msg, @Nullable Throwable cause) {
        final LogMessage logMessage = new LogMessage(tag, logLevel, msg, cause);
        try {
            this.messageQueue.put(logMessage);
        } catch (InterruptedException e) {
            Log.w(TAG, "Failed to queue log message to file", e);
        }
    }

    private void writeLogToFile(List<LogMessage> buffer) {
        final StringBuilder sb = new StringBuilder();
        for (LogMessage msg : buffer) {
            sb.append(msg.getFormattedMessage());
            sb.append(System.lineSeparator());
        }

        try {
            IoUtils.writeFile(this.appCtx, LOG_FILE_NAME, sb.toString(), true);
        } catch (IOException e) {
            Log.w(TAG, "Failed to write log to file", e);
        }
    }

    private void initLoggerThread() {
        final int logFileSize = IoUtils.getFileSize(this.appCtx, LOG_FILE_NAME);
        if (logFileSize >= 0) {
            // Moves current log file to prev and clears the new log file
            Log.d(TAG, "Copying previous log file to prev");
            try {
                IoUtils.copyFileContents(this.appCtx, LOG_FILE_NAME, LOG_FILE_PREV);
                IoUtils.writeFile(this.appCtx, LOG_FILE_NAME, "");
            } catch (IOException e) {
                Log.w(TAG, "Failed to reset log files", e);
            }
        }

        final Set<LogLevel> levelsToInclude = getLevelsToInclude(this.loggerMode);
        synchronized (this.bufferLock) {
            Log.d(TAG, "Buffer contains " + this.uninitializedBuffer.size() + " messages");
            for (LogMessage msg : this.uninitializedBuffer) {
                if (levelsToInclude.contains(msg.logLevel)) {
                    queueLogMessage(msg.tag, msg.logLevel, msg.message, msg.cause);
                }
            }
            this.uninitializedBuffer.clear();
        }

        this.loggerThread = new Thread(new QueueProcessor());
        this.loggerThread.start();
    }

    private static Set<LogLevel> getLevelsToInclude(LoggerMode loggerMode) {
        switch (loggerMode) {
            case WARN_FILE:
                return EnumSet.of(LogLevel.WARN, LogLevel.ERROR);
            case INFO_FILE:
                return EnumSet.of(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR);
            case DEBUG_FILE:
                return EnumSet.allOf(LogLevel.class);
            default:
                return Collections.emptySet();
        }
    }

    private class QueueProcessor implements Runnable {
        private static final int BUFFER_CAPACITY = 30;
        private final List<LogMessage> messageBuffer = new ArrayList<>(BUFFER_CAPACITY);
        @Override
        public void run() {
            try {
                LogMessage msg;
                while (!(msg = messageQueue.take()).equals(POISON)) {
                    this.messageBuffer.add(msg);
                    if (this.messageBuffer.size() >= BUFFER_CAPACITY || FORCE_WRITE.equals(msg)) {
                        writeLogToFile(this.messageBuffer);
                        this.messageBuffer.clear();
                    }
                }
                if (!this.messageBuffer.isEmpty()) {
                    writeLogToFile(this.messageBuffer);
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Queue processor interrupted", e);
            }
        }
    }

    private static class LogMessage {
        private static final String TIME_PATTERN = "yyyy-MM-DD HH:mm:ss.SSS";
        private final String tag;
        private final LogLevel logLevel;
        private final String message;
        private final Throwable cause;
        private final long timeStamp;

        public LogMessage(@Nonnull String tag, @Nonnull LogLevel logLevel, @Nonnull String message,
                          @Nullable Throwable cause) {
            this.tag = Objects.requireNonNull(tag);
            this.logLevel = logLevel;
            this.message = Objects.requireNonNull(message);
            this.cause = cause;
            this.timeStamp = System.currentTimeMillis();
        }

        public String getFormattedMessage() {
            return String.format("%s %s %s %s", new SimpleDateFormat(TIME_PATTERN, Locale.ENGLISH)
                    .format(new Date(this.timeStamp)), this.tag, this.logLevel.name,
                    getMessageString());
        }

        private String getMessageString() {
            if (this.cause != null) {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                this.cause.printStackTrace(pw);
                return String.format("%s%n%s%n%s", this.message, this.cause.getMessage(), sw);
            } else {
                return this.message;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.tag, this.logLevel, this.cause, this.message, this.timeStamp);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LogMessage that = (LogMessage) o;
            return timeStamp == that.timeStamp && tag.equals(that.tag)
                    && logLevel == that.logLevel && message.equals(that.message)
                    && cause.equals(that.cause);
        }
    }
}