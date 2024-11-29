package com.hygnus.spectre;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;

public class Storage {

    private static final String WORK_DIRECTORY = "Spectre";

    public static final String MAIN_LOG_FILE = "Log.html";
    public static final String NOTIFICATIONS_LOG_FILE = "Notifications.html";
    private static final String LOG_HTML_HEADER = "<meta charset=\"UTF-8\"><style>body{margin:0;font-family:monospace;font-size:11px;white-space:nowrap;}</style>";

    public static final String MEDIA_AUDIO_DIRECTORY = "Audio";
    public static final String MEDIA_AUDIO_FILE_EXTENSION = ".wav";
    public static final long MEDIA_AUDIO_MAX_SIZE = 3221225472L; // ~3GB / 3221225472 bytes

    public static BufferedWriter stateWriter;
    public static BufferedWriter notificationWriter;
    private static final String TAG = "Storage";

    public static void logMessage(BufferedWriter writer, String message, boolean timestamp) {
        try {
            switch (SettingDriver.screenState) {
                case OFF:
                    writer.write("<div style=\"background-color:darkgray\">");
                    break;
                case ON:
                    writer.write("<div style=\"background-color:aliceblue\">");
                    break;
            }

            if (timestamp) {
                writer.write("<span style=\"color:gainsboro\">" + ZonedDateTime.now() + " > </span>");
            }

            writer.write(message + "</div>");
            writer.newLine(); // Add a new line after each message
        } catch (IOException e) {
            // Not handling this or we will cause a dead end since this method is used by the Kernel.HandleException() one
            Log.e(TAG, "Error writing to log file", e);
        }
    }

    public static void logException(Exception e) {
        logMessage(stateWriter, "<span style=\"color:red\">❌ <strong>" + e.getMessage() + "</strong></span>" +
                "<span style=\"color:red\">❌ <em>" + Log.getStackTraceString(e) + "</em></span>", true);
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    public static void setupWorkDirectory() {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File workDir = new File(downloadsDir, WORK_DIRECTORY);
            if (!workDir.exists()) {
                workDir.mkdirs();
            }

            if (AudioRecorder.ENABLE_AUDIO_RECORDER) {
                File audioDir = new File(workDir, MEDIA_AUDIO_DIRECTORY);
                if (!audioDir.exists()) {
                    audioDir.mkdirs();
                }

                // Create .nomedia file to prevent media scanning
                File nomediaFile = new File(audioDir, ".nomedia");
                if (!nomediaFile.exists()) {
                    nomediaFile.createNewFile();
                }
            }

            stateWriter = new BufferedWriter(new FileWriter(new File(workDir, MAIN_LOG_FILE), true));
            notificationWriter = new BufferedWriter(new FileWriter(new File(workDir, NOTIFICATIONS_LOG_FILE), true));

            logMessage(stateWriter, LOG_HTML_HEADER, false);
            logMessage(notificationWriter, LOG_HTML_HEADER, false);
        } catch (IOException e) {
            // Handle exception appropriately (e.g., log, display error message)
            Log.e(TAG, "Error setting up work directory", e);
            Kernel.handleException(e);
        }
    }
}