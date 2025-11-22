package com.example.wordgame.utility;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

public class IoUtils {
    public static final String SETTING_FILE_DELIMITER = "/";

    /**
     * Reads the first line from the given file. Returns empty if file is not found or is empty
     * @param ctx Application context used for opening file
     * @param file File to read from
     * @return first line of text
     */
    public static String readLineFromFile(@Nonnull Context ctx, @Nonnull String file) {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(file);
        try (InputStreamReader streamReader = new InputStreamReader(ctx.openFileInput(file));
        BufferedReader bufferedReader = new BufferedReader(streamReader)) {
            return bufferedReader.readLine();
        } catch (IOException ex) {
            return "";
        }
    }

    /**
     * Reads all lines from the given file. Returns empty list if file is not found or is empty.
     * @param ctx Application context used for opening file
     * @param file File to read from
     * @return List containing all file lines
     */
    public static List<String> readLinesFromFile(@Nonnull Context ctx, @Nonnull String file) {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(file);

        try (InputStreamReader streamReader = new InputStreamReader(ctx.openFileInput(file));
             BufferedReader bufferedReader = new BufferedReader(streamReader)) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    /**
     * Writes the given content to a file. Will override the file if it already exists.
     * @param ctx Application context used for opening the file
     * @param fileName Name of the file to create
     * @param content Content to write to the file
     * @throws IOException if opening the file fails
     */
    public static void writeFile(@Nonnull Context ctx, @Nonnull String fileName, String content)
            throws IOException {
        writeFile(ctx, fileName, content, false);
    }

    public static void writeFile(@Nonnull Context ctx, @Nonnull String fileName,
                                 String content, boolean append) throws IOException {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(fileName);

        final int mode = append ? Context.MODE_APPEND : Context.MODE_PRIVATE;
        try (FileOutputStream outputStream = ctx.openFileOutput(fileName, mode)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Opens file output stream in the context file directory. NOTE: It's the responsibility of the
     * caller to close this file stream.
     * @param ctx context
     * @param fileName file name
     * @return file output stream
     * @throws IOException if an IO error occurs
     */
    public static FileOutputStream getFileOutputStream(@Nonnull Context ctx,
                                                       @Nonnull String fileName) throws IOException {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(fileName);

        return ctx.openFileOutput(fileName, Context.MODE_PRIVATE);
    }

    /**
     * Opens file output input in the context file directory. NOTE: It's the responsibility of the
     * caller to close this file stream.
     * @param ctx context
     * @param fileName file name
     * @return file input stream
     * @throws IOException if an IO error occurs
     */
    public static FileInputStream getFileInputStream(@Nonnull Context ctx,
                                                     @Nonnull String fileName) throws IOException {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(fileName);

        return ctx.openFileInput(fileName);
    }

    /**
     * Reads all bytes from the given input stream. Equivalent to getAllBytes in newer Java versions
     * @param inputStream input stream to read
     * @return all bytes from the input stream
     * @throws IOException if an IO error occurs
     */
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int read;
        byte[] tempBuffer = new byte[1024];

        while ((read = inputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
            bos.write(tempBuffer, 0, read);
        }

        bos.flush();
        return bos.toByteArray();
    }

    /**
     * Gets the file size of the given file (in bytes)
     * @param ctx context
     * @param fileName name of the file to check
     * @return content size in bytes. -1 if file is not found
     */
    public static int getFileSize(@Nonnull Context ctx, @Nonnull String fileName) {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(fileName);

        final File fileToCheck = new File(ctx.getFilesDir(), fileName);
        if (fileToCheck.exists() && fileToCheck.isFile()) {
            return (int) (fileToCheck.length() / 1024);
        }
        return -1;
    }

    /**
     * Reads contents from one file and copies them onto a second file
     * @param ctx context
     * @param fromFile file to copy from
     * @param toFile file to copy to
     * @throws IOException if an IO-error occurs
     */
    public static void copyFileContents(@Nonnull Context ctx, @Nonnull String fromFile,
                                        @Nonnull String toFile) throws IOException {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(fromFile);
        Objects.requireNonNull(toFile);

        try (final FileInputStream fileInputStream = ctx.openFileInput(fromFile);
             final FileOutputStream fileOutputStream = ctx.openFileOutput(toFile, Context.MODE_PRIVATE)) {
            int read;
            final byte[] buffer = new byte[1024];
            while ((read = fileInputStream.read(buffer, 0, buffer.length)) != -1) {
                fileOutputStream.write(buffer, 0, read);
            }
        }
    }
}
