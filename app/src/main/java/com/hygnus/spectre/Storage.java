package com.hygnus.spectre;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Storage {

    private static final String WORK_DIRECTORY = "Spectre";
    private static final String MAIN_LOG_FILE = "Activity.html";
    private static final String NOTIFICATIONS_LOG_FILE = "Notifications.html";
    private static final String LOG_HTML_HEADER = "<meta charset=\"UTF-8\"><style>body{margin:0;font-family:monospace;font-size:11px;white-space:nowrap;}</style>";

    public static final String MEDIA_AUDIO_DIRECTORY = "Audio";
    public static final String MEDIA_AUDIO_FILE_EXTENSION = ".wav";
    public static final long MEDIA_AUDIO_MAX_SIZE = 3221225472L; // ~3GB

    private static final String TAG = "Storage";

    public static BufferedWriter stateWriter;
    public static BufferedWriter notificationWriter;

    public static void setupWorkDirectory(ContentResolver resolver) {
        try {
            getOrCreateFolder(resolver, WORK_DIRECTORY);

            Uri logFileUri = getOrCreateFile(resolver, MAIN_LOG_FILE);
            Uri notificationsFileUri = getOrCreateFile(resolver, NOTIFICATIONS_LOG_FILE);

            stateWriter = new BufferedWriter(new OutputStreamWriter(getOutputStream(resolver, logFileUri)));
            notificationWriter = new BufferedWriter(new OutputStreamWriter(getOutputStream(resolver, notificationsFileUri)));

            logMessage(stateWriter, LOG_HTML_HEADER, false);
            logMessage(notificationWriter, LOG_HTML_HEADER, false);

            if (AudioRecorder.ENABLE_AUDIO_RECORDER) {
                getOrCreateFolder(resolver, WORK_DIRECTORY + "/" + MEDIA_AUDIO_DIRECTORY);
                createNomediaFile(resolver);
            }

        } catch (IOException e) {
            Log.e(TAG, "Erro ao configurar diretório de trabalho", e);
        }
    }

    private static OutputStream getOutputStream(ContentResolver resolver, Uri fileUri) throws IOException {
        return resolver.openOutputStream(fileUri, "rwt"); // "rwt" permite escrita e append corretamente
    }

    private static void getOrCreateFolder(ContentResolver resolver, String folderName) {
        Uri collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + folderName;

        // Verifica se a pasta já existe
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
        String[] selectionArgs = {relativePath};

        try (Cursor cursor = resolver.query(collection, new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return; // Pasta já existe, não precisa criar
            }
        }

        // Criar nova pasta
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        resolver.insert(collection, values);
    }

    private static Uri getOrCreateFile(ContentResolver resolver, String fileName) {
        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri fileUri = getFileUri(resolver, fileName);

        // Se o arquivo já existir, retorna o URI existente
        if (fileUri != null) {
            return fileUri;
        }

        // Caso contrário, cria um novo arquivo
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/html");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + WORK_DIRECTORY);

        return resolver.insert(collection, values);
    }

    private static Uri getFileUri(ContentResolver resolver, String fileName) {
        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " + MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        String[] selectionArgs = {Environment.DIRECTORY_DOWNLOADS + "/" + WORK_DIRECTORY, fileName};

        try (Cursor cursor = resolver.query(collection, new String[]{MediaStore.MediaColumns._ID}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                return Uri.withAppendedPath(collection, String.valueOf(id));
            }
        }
        return null; // Retorna null caso o arquivo não exista
    }

    private static void createNomediaFile(ContentResolver resolver) {
        if (getFileUri(resolver, ".nomedia") != null) return; // Se já existe, não cria novamente

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, ".nomedia");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + WORK_DIRECTORY + "/" + MEDIA_AUDIO_DIRECTORY);

        resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
    }

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
                DateTimeFormatter dtformat = DateTimeFormatter.ofPattern("yyyy/MM/dd - HH:mm:ss z");
                writer.write("<span style=\"color:gainsboro\">" + ZonedDateTime.now().format(dtformat) + " > </span>");
            }

            writer.write(message + "</div>");
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            Log.e(TAG, "Erro ao escrever no log", e);
        }
    }

    public static void logException(Exception e) {
        logMessage(stateWriter, "<span style=\"color:crimson\">❌ <strong>" + e.getMessage() + "</strong></span>" +
                "<span style=\"color:indianred\"> ❌ <em>" + Log.getStackTraceString(e) + "</em></span>", true);
    }
}
