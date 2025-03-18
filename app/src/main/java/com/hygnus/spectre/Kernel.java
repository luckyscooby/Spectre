package com.hygnus.spectre;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;

public class Kernel extends AccessibilityService {

    private static final String TAG = "Kernel";
    private static final String DEBUG_CHANNEL_ID = "debug";
    private static NotificationManager notificationManager;
    static Context serviceContext;
    public static SettingDriver settingDriverReceiver = new SettingDriver();
    public static Ticker timeDriverReceiver = new Ticker();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo();
        accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        accessibilityServiceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        accessibilityServiceInfo.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(accessibilityServiceInfo);

        // DEBUG NOTIFICATION CHANNEL
        createNotificationChannel();

        initialize();
    }

    private void createNotificationChannel() {
        NotificationChannel notificationChannel = null;
        notificationChannel = new NotificationChannel(DEBUG_CHANNEL_ID,
                "Exception notifications", NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription("Exception notifications");
        notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(notificationChannel);
        } else {
            Log.e(TAG, "NotificationManager is null, cannot create notification channel");
        }
    }

    @Override
    public void onInterrupt() {
        terminate();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        terminate();
        return super.onUnbind(intent);
    }

    @SuppressLint("SwitchIntDef")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null) {
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    NotificationDriver.captureNotification(event);
                    break;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    ActivityDriver.captureActivity(event);
                    break;
            }
        }
    }

    public static void beep(int toneType) {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGenerator.startTone(toneType, 1000);
        toneGenerator.release(); // Release the tone generator when done
    }

    private void initialize() {
        serviceContext = getApplicationContext();

        Storage.setupWorkDirectory(serviceContext.getContentResolver());

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            assert packageInfo.versionName != null;
            Storage.logMessage(Storage.stateWriter, "<strong>👁️‍🗨️ " + getPackageName().toUpperCase() + " " +
                    packageInfo.versionName.toUpperCase() + "</strong>", true);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
            handleException(e);
        }

        registerReceivers();

        beep(ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE);

        // Extra initialization routines
        NetworkDriver.reportNetworkInterface();
        if (AudioRecorder.ENABLE_AUDIO_RECORDER) { AudioRecorder.start(); }
    }

    private void registerReceivers() {
        IntentFilter settingIntentFilter = new IntentFilter();
        settingIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        settingIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        settingIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        settingIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        settingIntentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        settingIntentFilter.addAction(Intent.ACTION_SHUTDOWN);
        settingIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        settingIntentFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
        registerReceiver(settingDriverReceiver, settingIntentFilter);

        IntentFilter timeIntentFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        registerReceiver(timeDriverReceiver, timeIntentFilter);
    }

    public static void terminate() {
        if (AudioRecorder.ENABLE_AUDIO_RECORDER) { AudioRecorder.stop(); }

        Storage.logMessage(Storage.stateWriter, "<strong>🛑 TERMINATED</strong>", true);
        Storage.logMessage(Storage.stateWriter, "<br />", false);

        beep(ToneGenerator.TONE_CDMA_CALLDROP_LITE);
    }

    public static void handleException(Exception e) {
        handleException(e, true, true);
    }

    public static void handleException(Exception e, boolean beep, boolean notify) {
        if (e != null) {
            Storage.logException(e);
        }
        if (beep) {
            beep(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE);
        }
        if (notify && e != null) {
            try {
                Notification notification = new NotificationCompat.Builder(serviceContext, DEBUG_CHANNEL_ID)
                        .setContentTitle("Spectre Exception")
                        .setContentText(e.getMessage()) // Use the exception message
                        .build();
                if (notificationManager != null) {
                    notificationManager.notify(0, notification);
                } else {
                    Log.e(TAG, "NotificationManager is null, cannot show notification");
                }
            } catch (Exception notificationException) {
                Log.e(TAG, "Error showing exception notification", notificationException);
            }
        }
    }
}