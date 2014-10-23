/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.speedtracker.db;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.wearable.speedtracker.LocationDataManager;
import com.example.android.wearable.speedtracker.PhoneApplication;
import com.example.android.wearable.speedtracker.common.Constants;
import com.example.android.wearable.speedtracker.common.LocationEntry;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link com.google.android.gms.wearable.WearableListenerService} that is responsible for
 * reading location data that gets added to the Data Layer storage.
 */
public class UpdateService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DeleteDataItemsResult> {

    private static final String TAG = "UpdateService";
    private LocationDataManager mDataManager;
    private GoogleApiClient mGoogleApiClient;
    private final Set<Uri> mToBeDeletedUris = new HashSet<Uri>();
    public static final String ACTION_NOTIFY = "com.example.android.wearable.speedtracker.Message";
    public static final String EXTRA_ENTRY = "entry";
    public static final String EXTRA_LOG = "log";

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        mDataManager = ((PhoneApplication) getApplicationContext()).getDataManager();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                Uri dataItemUri = dataEvent.getDataItem().getUri();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Received a data item with uri: " + dataItemUri.getPath());
                }
                if (dataItemUri.getPath().startsWith(Constants.PATH)) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem())
                            .getDataMap();
                    double longitude = dataMap.getDouble(Constants.KEY_LONGITUDE);
                    double latitude = dataMap.getDouble(Constants.KEY_LATITUDE);
                    long time = dataMap.getLong(Constants.KEY_TIME);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(time);
                    mDataManager.addPoint(
                            new LocationEntry(calendar, latitude, longitude));
                    if (mGoogleApiClient.isConnected()) {
                        Wearable.DataApi.deleteDataItems(
                                mGoogleApiClient, dataItemUri).setResultCallback(this);
                    } else {
                        synchronized (mToBeDeletedUris) {
                            mToBeDeletedUris.add(dataItemUri);
                        }
                    }
                }
            }
        }
    }

    @Override // ConnectionCallbacks
    public void onConnected(Bundle bundle) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected(): api client is connected now");
        }
        synchronized (mToBeDeletedUris) {
            if (!mToBeDeletedUris.isEmpty()) {
                for (Uri dataItemUri : mToBeDeletedUris) {
                    Wearable.DataApi.deleteDataItems(
                            mGoogleApiClient, dataItemUri).setResultCallback(this);
                }
            }
        }
    }

    @Override // ConnectionCallbacks
    public void onConnectionSuspended(int i) {
    }

    @Override // OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to the Google API client");
    }

    @Override // ResultCallback
    public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
        if (!deleteDataItemsResult.getStatus().isSuccess()) {
            Log.e(TAG,
                    "Failed to delete a dataItem, status code: " + deleteDataItemsResult.getStatus()
                            .getStatusCode() + deleteDataItemsResult.getStatus()
                            .getStatusMessage());
        }
    }
}
