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

package com.example.android.wearable.agendadata;

import static com.example.android.wearable.agendadata.Constants.TAG;
import static com.example.android.wearable.agendadata.Constants.EXTRA_SILENT;

import static com.example.android.wearable.agendadata.Constants.ALL_DAY;
import static com.example.android.wearable.agendadata.Constants.BEGIN;
import static com.example.android.wearable.agendadata.Constants.DESCRIPTION;
import static com.example.android.wearable.agendadata.Constants.END;
import static com.example.android.wearable.agendadata.Constants.PROFILE_PIC;
import static com.example.android.wearable.agendadata.Constants.TITLE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Listens to DataItem events on the home device.
 */
public class HomeListenerService extends WearableListenerService {

    private static final Map<Uri, Integer> sNotificationIdByDataItemUri =
            new HashMap<Uri, Integer>();
    private static int sNotificationId = 1;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents + " for " + getPackageName());
        }
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                deleteDataItem(event.getDataItem());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                UpdateNotificationForDataItem(event.getDataItem());
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this.getApplicationContext())
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Posts a local notification to show calendar card.
     */
    private void UpdateNotificationForDataItem(DataItem dataItem) {
        DataMapItem mapDataItem = DataMapItem.fromDataItem(dataItem);
        DataMap data = mapDataItem.getDataMap();

        String description = data.getString(DESCRIPTION);
        if (TextUtils.isEmpty(description)) {
            description = "";
        } else {
            // Add a space between the description and the time of the event.
            description += " ";
        }
        String contentText;
        if (data.getBoolean(ALL_DAY)) {
            contentText = getString(R.string.desc_all_day, description);
        } else {
            String startTime = DateFormat.getTimeFormat(this).format(new Date(data.getLong(BEGIN)));
            String endTime = DateFormat.getTimeFormat(this).format(new Date(data.getLong(END)));
            contentText = getString(R.string.desc_time_period, description, startTime, endTime);
        }

        Intent deleteOperation = new Intent(this, DeleteService.class);
        // Use a unique identifier for the delete action.
        String deleteAction = "action_delete" + dataItem.getUri().toString() + sNotificationId;
        deleteOperation.setAction(deleteAction);
        deleteOperation.setData(dataItem.getUri());
        PendingIntent deleteIntent = PendingIntent.getService(this, 0, deleteOperation,
                PendingIntent.FLAG_ONE_SHOT);
        PendingIntent silentDeleteIntent = PendingIntent.getService(this, 1,
                deleteOperation.putExtra(EXTRA_SILENT, true), PendingIntent.FLAG_ONE_SHOT);

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(data.getString(TITLE))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .addAction(R.drawable.ic_menu_delete, getText(R.string.delete), deleteIntent)
                .setDeleteIntent(silentDeleteIntent)  // Delete DataItem if notification dismissed.
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_MIN);

        // Set the event owner's profile picture as the notification background.
        Asset asset = data.getAsset(PROFILE_PIC);
        if (null != asset) {
            if (mGoogleApiClient.isConnected()) {
                DataApi.GetFdForAssetResult assetFdResult =
                        Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await();
                if (assetFdResult.getStatus().isSuccess()) {
                    Bitmap profilePic = BitmapFactory.decodeStream(assetFdResult.getInputStream());
                    notificationBuilder.extend(new Notification.WearableExtender()
                            .setBackground(profilePic));
                } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "asset fetch failed with statusCode: "
                            + assetFdResult.getStatus().getStatusCode());
                }
            } else {
                Log.e(TAG, "Failed to set notification background"
                         + " - Client disconnected from Google Play Services");
            }
        }
        Notification card = notificationBuilder.build();

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(sNotificationId, card);

        sNotificationIdByDataItemUri.put(dataItem.getUri(), sNotificationId++);
    }

    /**
     * Deletes the calendar card associated with the data item.
     */
    private void deleteDataItem(DataItem dataItem) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onDataItemDeleted:DataItem=" + dataItem.getUri());
        }
        Integer notificationId = sNotificationIdByDataItemUri.remove(dataItem.getUri());
        if (notificationId != null) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notificationId);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onMessageReceived: " + messageEvent.getPath()
                     + " " + messageEvent.getData() + " for " + getPackageName());
        }
    }

}
