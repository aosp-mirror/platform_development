package com.example.android.wearable.findphone;

import android.app.Notification;
import android.app.NotificationManager;

import com.google.android.gms.wearable.WearableListenerService;

/**
 * Listens for disconnection from home device.
 */
public class DisconnectListenerService extends WearableListenerService {

    private static final String TAG = "ExampleFindPhoneApp";

    private static final int FORGOT_PHONE_NOTIFICATION_ID = 1;

    @Override
    public void onPeerDisconnected(com.google.android.gms.wearable.Node peer) {
        // Create a "forgot phone" notification when phone connection is broken.
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.left_phone_title))
                .setContentText(getString(R.string.left_phone_content))
                .setVibrate(new long[] {0, 200})  // Vibrate for 200 milliseconds.
                .setSmallIcon(R.drawable.ic_launcher)
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_MAX);
        Notification card = notificationBuilder.build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(FORGOT_PHONE_NOTIFICATION_ID, card);
    }

    @Override
    public void onPeerConnected(com.google.android.gms.wearable.Node peer) {
        // Remove the "forgot phone" notification when connection is restored.
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .cancel(FORGOT_PHONE_NOTIFICATION_ID);
    }

}
