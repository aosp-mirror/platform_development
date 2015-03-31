/*
 * Copyright 2015 Google Inc. All rights reserved.
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

package com.example.android.xyztouristattractions.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.example.android.xyztouristattractions.common.Constants;
import com.example.android.xyztouristattractions.common.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * A utility IntentService, used for a variety of asynchronous background
 * operations that do not necessarily need to be tied to a UI.
 */
public class UtilityService extends IntentService {
    private static final String TAG = UtilityService.class.getSimpleName();

    private static final String ACTION_CLEAR_NOTIFICATION = "clear_notification";
    private static final String ACTION_CLEAR_REMOTE_NOTIFICATIONS = "clear_remote_notifications";
    private static final String ACTION_START_DEVICE_ACTIVITY = "start_device_activity";
    private static final String EXTRA_START_PATH = "start_path";
    private static final String EXTRA_START_ACTIVITY_INFO = "start_activity_info";

    public static void clearNotification(Context context) {
        Intent intent = new Intent(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_CLEAR_NOTIFICATION);
        context.startService(intent);
    }

    public static void clearRemoteNotifications(Context context) {
        context.startService(getClearRemoteNotificationsIntent(context));
    }

    public static Intent getClearRemoteNotificationsIntent(Context context) {
        Intent intent = new Intent(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_CLEAR_REMOTE_NOTIFICATIONS);
        return intent;
    }

    /**
     * Trigger a message that asks the master device to start an activity.
     *
     * @param context the context
     * @param path the path that will be sent via the wearable message API
     * @param name the tourist attraction name
     * @param city the tourist attraction city
     */
    public static void startDeviceActivity(Context context, String path, String name, String city) {
        Intent intent = new Intent(context, UtilityService.class);
        intent.setAction(UtilityService.ACTION_START_DEVICE_ACTIVITY);
        String extraInfo;
        if (Constants.START_ATTRACTION_PATH.equals(path)) {
            extraInfo = name;
        } else {
            extraInfo = name + ", " + city;
        }
        intent.putExtra(EXTRA_START_ACTIVITY_INFO, extraInfo);
        intent.putExtra(EXTRA_START_PATH, path);
        context.startService(intent);
    }

    public UtilityService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_CLEAR_NOTIFICATION.equals(action)) {
            clearNotificationInternal();
        } else if (ACTION_CLEAR_REMOTE_NOTIFICATIONS.equals(action)) {
            clearRemoteNotificationsInternal();
        } else if (ACTION_START_DEVICE_ACTIVITY.equals(action)) {
            startDeviceActivityInternal(intent.getStringExtra(EXTRA_START_PATH),
                    intent.getStringExtra(EXTRA_START_ACTIVITY_INFO));
        }
    }

    /**
     * Clear the local notifications
     */
    private void clearNotificationInternal() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.WEAR_NOTIFICATION_ID);
    }

    /**
     * Trigger a message to ask other devices to clear their notifications
     */
    private void clearRemoteNotificationsInternal() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

        if (connectionResult.isSuccess() && googleApiClient.isConnected()) {
            Iterator<String> itr = Utils.getNodes(googleApiClient).iterator();
            while (itr.hasNext()) {
                // Loop through all connected nodes
                Wearable.MessageApi.sendMessage(
                        googleApiClient, itr.next(), Constants.CLEAR_NOTIFICATIONS_PATH, null);
            }
        }

        googleApiClient.disconnect();
    }

    /**
     * Sends the actual message to ask other devices to start an activity
     *
     * @param path the path to pass to the wearable message API
     * @param extraInfo extra info that varies based on the path being sent
     */
    private void startDeviceActivityInternal(String path, String extraInfo) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

        if (connectionResult.isSuccess() && googleApiClient.isConnected()) {
            Iterator<String> itr = Utils.getNodes(googleApiClient).iterator();
            while (itr.hasNext()) {
                // Loop through all connected nodes
                Wearable.MessageApi.sendMessage(
                        googleApiClient, itr.next(), path, extraInfo.getBytes());
            }
        }
        googleApiClient.disconnect();
    }

}
