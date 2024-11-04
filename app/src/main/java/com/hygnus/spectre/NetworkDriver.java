package com.hygnus.spectre;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkDriver {

    private enum NetworkState {
        UNKNOWN,
        ENABLED,
        DISABLED
    }

    private static NetworkState networkState = NetworkState.UNKNOWN;

    public static void reportNetworkInterface() {
        if (isNetworkAvailable() && networkState != NetworkState.ENABLED) {
            Storage.logMessage(Storage.stateWriter, "<span style=\"color:seagreen\">📡 NETWORK ENABLED</span>", true);
            networkState = NetworkState.ENABLED;
        } else if (!isNetworkAvailable() && networkState != NetworkState.DISABLED) {
            Storage.logMessage(Storage.stateWriter, "<span style=\"color:indianred\">📡 NETWORK DISABLED</span>", true);
            networkState = NetworkState.DISABLED;
        }
    }

    private static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) Kernel.serviceContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } else {
            // Handle case where ConnectivityManager is null (e.g., in tests)
            return false;
        }
    }
}