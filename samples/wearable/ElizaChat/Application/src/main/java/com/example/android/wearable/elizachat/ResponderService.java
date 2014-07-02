/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.elizachat;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

/**
 * A service that runs in the background and provides responses to the incoming messages from the
 * wearable. It also keeps a record of the chat session history, which it can provide upon request.
 */
public class ResponderService extends Service {

    public static final String ACTION_INCOMING = "com.example.android.wearable.elizachat.INCOMING";

    public static final String ACTION_RESPONSE = "com.example.android.wearable.elizachat.REPLY";

    public static final String EXTRA_REPLY = "reply";

    private static final String TAG = "ResponderService";

    private ElizaResponder mResponder;

    private String mLastResponse = null;

    private StringBuffer mCompleteConversation = new StringBuffer();

    private LocalBroadcastManager mBroadcastManager;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Chat Service started");
        }
        mResponder = new ElizaResponder();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        processIncoming(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent || null == intent.getAction()) {
            return Service.START_STICKY;
        }
        String action = intent.getAction();
        if (action.equals(ACTION_RESPONSE)) {
            Bundle remoteInputResults = RemoteInput.getResultsFromIntent(intent);
            CharSequence replyMessage = "";
            if (remoteInputResults != null) {
                replyMessage = remoteInputResults.getCharSequence(EXTRA_REPLY);
            }
            processIncoming(replyMessage.toString());
        } else if (action.equals(MainActivity.ACTION_GET_CONVERSATION)) {
            broadcastMessage(mCompleteConversation.toString());
        }
        return Service.START_STICKY;
    }

    private void showNotification() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Sent: " + mLastResponse);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.eliza))
                .setContentText(mLastResponse)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.bg_eliza))
                .setSmallIcon(R.drawable.bg_eliza)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        Intent intent = new Intent(ACTION_RESPONSE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification = builder
                .extend(new NotificationCompat.WearableExtender()
                        .addAction(new NotificationCompat.Action.Builder(
                                R.drawable.ic_full_reply, getString(R.string.reply), pendingIntent)
                                .addRemoteInput(new RemoteInput.Builder(EXTRA_REPLY)
                                        .setLabel(getString(R.string.reply))
                                        .build())
                                .build()))
                .build();
        NotificationManagerCompat.from(this).notify(0, notification);
    }

    private void processIncoming(String text) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Received: " + text);
        }
        mLastResponse = mResponder.elzTalk(text);
        String line = TextUtils.isEmpty(text) ? mLastResponse : text + "\n" + mLastResponse;

        // Send a new line of conversation to update the Activity, unless the incoming text was
        // empty.
        if (!TextUtils.isEmpty(text)) {
            broadcastMessage(line);
        }
        NotificationManagerCompat.from(this).cancelAll();
        showNotification();
        mCompleteConversation.append("\n" + line);
    }

    private void broadcastMessage(String message) {
        Intent intent = new Intent(MainActivity.ACTION_NOTIFY);
        intent.putExtra(MainActivity.EXTRA_MESSAGE, message);
        mBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Chat Service stopped");
        }
        NotificationManagerCompat.from(this).cancel(0);
        mBroadcastManager = null;
        super.onDestroy();
    }
}
