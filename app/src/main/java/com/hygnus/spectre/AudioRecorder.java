package com.hygnus.spectre;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

public class AudioRecorder {

    private static final int SAMPLING_RATE = 44100;
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN, ENCODING);
    private static final byte[] audioSample = new byte[bufferSize];
    private static final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLING_RATE, CHANNEL_IN, ENCODING, bufferSize);
    private static String currentAudioFile = "";
    private static final String TAG = "AudioRecorder";

    public static void start() {
        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            return;
        }
        try {
            audioRecord.startRecording();

            Thread thread = new Thread(AudioRecorder::writeToDisk);
            thread.start();
        } catch (Exception e) {
            // Handle exceptions appropriately (e.g., log, display error message)
            Log.e(TAG, "Error starting recording", e);
            // Consider using a tone generator or vibrator for feedback
            // Kernel.beep(Tone.CdmaSoftErrorLite);
            Storage.logException(e);
        }
    }

    public static void stop() {
        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
            return;
        }
        try {
            audioRecord.stop();
        } catch (Exception e) {
            // Handle exceptions appropriately (e.g., log, display error message)
            Log.e(TAG, "Error stopping recording", e);
            // Consider using a tone generator or vibrator for feedback
            // Kernel.beep(Tone.CdmaSoftErrorLite);
            Storage.logException(e);
        }
    }

    public static void schedule() {
         // Get current time using Calendar or other suitable methods
         Calendar now = Calendar.getInstance();
         if (now.get(Calendar.HOUR_OF_DAY) == 0 && now.get(Calendar.MINUTE) == 0) {
             stop();
             start();
         } else {
             start();
         }
    }

    private static void writeToDisk() {
        try {
            // Create new audio file
            currentAudioFile = Storage.MEDIA_AUDIO_DIRECTORY + "/" + SettingDriver.getFormattedDateTime() + Storage.MEDIA_AUDIO_FILE_EXTENSION;
            File file = new File(currentAudioFile);
            file.createNewFile(); // Ensure the file is created

            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                // PCM sample data flushing loop
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    int bytesRead = audioRecord.read(audioSample, 0, bufferSize);
                    if (bytesRead > 0) {
                        try {
                            fileOutputStream.write(audioSample, 0, bytesRead);
                            fileOutputStream.flush();
                        } catch (IOException e) {
                            // Ignore errors; this will create gaps in audio file for each error, but prevent full session failure
                            Log.w(TAG, "Error writing audio data", e);
                        }
                    }

                    if (file.length() >= Storage.MEDIA_AUDIO_MAX_SIZE) {
                        fileOutputStream.close();
                        writeWavHeader();
                        writeToDisk(); // Start a new file
                        return; // Exit the current writeToDisk call
                    }
                }

                // After loop finished (RecordState.Stopped)
                fileOutputStream.close();
                writeWavHeader();
            }
        } catch (Exception e) {
            // Handle exceptions appropriately (e.g., log, display error message)
            Log.e(TAG, "Error writing audio to disk", e);
            // Consider using a tone generator or vibrator for feedback
            // Kernel.beep(Tone.CdmaSoftErrorLite);
            Storage.logException(e);
        }
    }

    private static void writeWavHeader() throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(currentAudioFile);
             FileOutputStream fileOutputStream = new FileOutputStream(currentAudioFile)) {

            long fileSize = fileInputStream.getChannel().size();

            ByteBuffer headerBuffer = ByteBuffer.allocate(44);
            headerBuffer.order(ByteOrder.LITTLE_ENDIAN);

            // ChunkID
            headerBuffer.put("RIFF".getBytes());
            // ChunkSize
            headerBuffer.putInt((int) (fileSize + 36));
            // Format
            headerBuffer.put("WAVE".getBytes());
            // SubChunk1ID
            headerBuffer.put("fmt ".getBytes());
            // SubChunk1Size
            headerBuffer.putInt(16);
            // AudioFormat
            headerBuffer.putShort((short) 1);
            // NumChannels
            headerBuffer.putShort((short) 1);
            // SampleRate
            headerBuffer.putInt(AudioRecorder.SAMPLING_RATE);
            // ByteRate
            headerBuffer.putInt(AudioRecorder.SAMPLING_RATE * (short) 16 / 8);
            // BlockAlign
            headerBuffer.putShort((short) ((short) 16 / 8));
            // BitsPerSample
            headerBuffer.putShort((short) 16);
            // SubChunk2ID
            headerBuffer.put("data".getBytes());
            // SubChunk2Size
            headerBuffer.putInt((int) fileSize);

            fileOutputStream.write(headerBuffer.array());
        }
    }
}