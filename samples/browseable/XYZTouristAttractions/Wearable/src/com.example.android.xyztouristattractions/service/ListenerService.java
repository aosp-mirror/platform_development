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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.example.android.xyztouristattractions.R;
import com.example.android.xyztouristattractions.common.Constants;
import com.example.android.xyztouristattractions.common.Utils;
import com.example.android.xyztouristattractions.ui.AttractionsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A Wear listener service, used to receive inbound messages from
 * other devices.
 */
public class ListenerService extends WearableListenerService {
    private static final String TAG = ListenerService.class.getSimpleName();

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED
                    && event.getDataItem() != null
                    && Constants.ATTRACTION_PATH.equals(event.getDataItem().getUri().getPath())) {

                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                ArrayList<DataMap> attractionsData =
                        dataMapItem.getDataMap().getDataMapArrayList(Constants.EXTRA_ATTRACTIONS);
                showNotification(dataMapItem.getUri(), attractionsData);
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.v(TAG, "onMessageReceived: " + messageEvent);

        if (Constants.CLEAR_NOTIFICATIONS_PATH.equals(messageEvent.getPath())) {
            // Clear the local notification
            UtilityService.clearNotification(this);
        }
    }

    private void showNotification(Uri attractionsUri, ArrayList<DataMap> attractions) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess() || !googleApiClient.isConnected()) {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.getErrorCode()));
            return;
        }

        Intent intent = new Intent(this, AttractionsActivity.class);
        // Pass through the data Uri as an extra
        intent.putExtra(Constants.EXTRA_ATTRACTIONS_URI, attractionsUri);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        int count = attractions.size();

        DataMap attraction = attractions.get(0);

        Bitmap bitmap = Utils.loadBitmapFromAsset(
                googleApiClient, attraction.getAsset(Constants.EXTRA_IMAGE));

        PendingIntent deletePendingIntent = PendingIntent.getService(
                this, 0, UtilityService.getClearRemoteNotificationsIntent(this), 0);

        Notification notification = new Notification.Builder(this)
                .setContentText(getResources().getQuantityString(
                        R.plurals.attractions_found, count, count))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDeleteIntent(deletePendingIntent)
                .addAction(R.drawable.ic_full_explore,
                        getString(R.string.action_explore),
                        pendingIntent)
                .extend(new Notification.WearableExtender()
                        .setBackground(bitmap)
                )
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.WEAR_NOTIFICATION_ID, notification);

        googleApiClient.disconnect();
    }
}
