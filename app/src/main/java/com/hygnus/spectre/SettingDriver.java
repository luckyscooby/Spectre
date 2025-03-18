package com.hygnus.spectre;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.provider.Settings;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingDriver extends BroadcastReceiver {

    private static final String TAG = "SettingDriver";

    public enum ScreenState {
        OFF,
        ON
    }

    public static ScreenState screenState = ScreenState.ON; // Assume screen is on at boot
    private static int previousGPSState = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY; // Assume GPS mode is high accuracy

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case Intent.ACTION_SCREEN_ON:
                    if (screenState == ScreenState.OFF) {
                        screenState = ScreenState.ON;
                        Storage.logMessage(Storage.stateWriter, "<span style=\"color:dodgerblue\">👁️ ️ ON</span>", true);
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (screenState == ScreenState.ON) {
                        screenState = ScreenState.OFF;
                        Storage.logMessage(Storage.stateWriter, "<span style=\"color:seashell\">💤 OFF</span>", true);
                    }
                    break;
                case Intent.ACTION_POWER_CONNECTED:
                    Storage.logMessage(Storage.stateWriter, "<span style=\"color:Khaki\">⚡ CHARGING @ " + getBatteryLevel(context) + "</span>", true);
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    Storage.logMessage(Storage.stateWriter, "<span style=\"color:Khaki\">⚡ DISCHARGING @ " + getBatteryLevel(context) + "</span>", true);
                    break;
                case Intent.ACTION_BATTERY_LOW:
                    Storage.logMessage(Storage.stateWriter, "<span style=\"color:orange\">🪫 BATTERY LOW / SAVING MODE @ " + getBatteryLevel(context) + "</span>", true);
                    break;
                case Intent.ACTION_SHUTDOWN:
                case Intent.ACTION_REBOOT:
                    Storage.logMessage(Storage.stateWriter, "<strong><span style=\"color:black\">📴 SHUTDOWN / REBOOT @ " + getBatteryLevel(context) + "</span></strong>", true);
                    Kernel.terminate();
                    break;
                case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                    Storage.logMessage(Storage.stateWriter, "<span style=\"color:indianred\">✈️ AIRPLANE MODE [" + isAirplaneModeOn(context) + "]</span>", true);
                    break;
                case LocationManager.MODE_CHANGED_ACTION:
                    int currentGPSState = getGPSProviderState(context);
                    if (currentGPSState != previousGPSState) {
                        Storage.logMessage(Storage.stateWriter, "<span style=\"color:indianred\">🛰️ GPS PROVIDER [" + currentGPSState + "]</span>", true);
                        previousGPSState = currentGPSState;
                    }
                    break;
            }
        }
    }

    private static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private static int getGPSProviderState(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
    }

    private static String getBatteryLevel(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) Math.floor(level * 100D / scale);
            return batteryPct + "%";
        } else {
            Log.w(TAG, "Battery status intent is null");
            return "Unknown";
        }
    }

    public static String getFormattedDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.getDefault());
        return dateFormat.format(new Date());
    }
}