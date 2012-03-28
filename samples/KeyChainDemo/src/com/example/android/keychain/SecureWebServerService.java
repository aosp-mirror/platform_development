/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.keychain;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SecureWebServerService extends Service {

    // Log tag for this class
    private static final String TAG = "SecureWebServerService";

    // A special ID assigned to this on-going notification.
    private static final int ONGOING_NOTIFICATION = 1248;

    // A handle to the simple SSL web server
    private SecureWebServer sws;

    /**
     * Start the SSL web server and set an on-going notification
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sws = new SecureWebServer(this);
        sws.start();
        createNotification();
    }

    /**
     * Stop the SSL web server and remove the on-going notification
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        sws.stop();
        stopForeground(true);
    }

    /**
     * Return null as there is nothing to bind
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Create an on-going notification. It will stop the server when the user
     * clicks on the notification.
     */
    private void createNotification() {
        Log.d(TAG, "Create an ongoing notification");
        Intent notificationIntent = new Intent(this,
                KeyChainDemoActivity.class);
        notificationIntent.putExtra(KeyChainDemoActivity.EXTRA_STOP_SERVER,
                true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Notification notification = new Notification.Builder(this).
                setContentTitle(getText(R.string.notification_title)).
                setContentText(getText(R.string.notification_message)).
                setSmallIcon(android.R.drawable.ic_media_play).
                setTicker(getText(R.string.ticker_text)).
                setOngoing(true).
                setContentIntent(pendingIntent).
                getNotification();
        startForeground(ONGOING_NOTIFICATION, notification);
    }

}
