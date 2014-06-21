package com.example.android.support.wearable.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationUtil {
    public static final String EXTRA_MESSAGE =
            "com.example.android.support.wearable.notifications.MESSAGE";
    public static final String EXTRA_REPLY =
            "com.example.android.support.wearable.notifications.REPLY";

    public static PendingIntent getExamplePendingIntent(Context context, int messageResId) {
        Intent intent = new Intent(NotificationIntentReceiver.ACTION_EXAMPLE)
                .setClass(context, NotificationIntentReceiver.class);
        intent.putExtra(EXTRA_MESSAGE, context.getString(messageResId));
        return PendingIntent.getBroadcast(context, messageResId /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
