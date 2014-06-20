package com.example.android.support.wearable.notifications;

import android.app.Notification;
import android.content.Context;

/**
 * Base class for notification preset generators.
 */
public abstract class NotificationPreset {
    public final int nameResId;

    public NotificationPreset(int nameResId) {
        this.nameResId = nameResId;
    }

    /** Start building a notification with this preset */
    public abstract Notification buildNotification(Context context);
}
