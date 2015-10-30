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

package com.example.android.wearable.findphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.Set;

/**
 * Listens for changes in connectivity between this wear device and the phone. More precisely, we
 * need to distinguish the case that the wear device and the phone are connected directly from all
 * other possible cases. To this end, the phone app has registered itself to provide the "find_me"
 * capability and we need to look for connected nodes that provide this capability AND are nearby,
 * to exclude a connection through the cloud. The proper way would have been to use the
 * {@code onCapabilitiesChanged()} callback but currently that callback cannot discover the case
 * where a connection switches from wifi to direct; this shortcoming will be addressed in future
 * updates but for now we will use the {@code onConnectedNodes()} callback.
 */
public class DisconnectListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "ExampleFindPhoneApp";

    private static final int FORGOT_PHONE_NOTIFICATION_ID = 1;

    /* the capability that the phone app would provide */
    private static final String FIND_ME_CAPABILITY_NAME = "find_me";

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
    }

    @Override
    public void onConnectedNodes(List<Node> connectedNodes) {
        // After we are notified by this callback, we need to query for the nodes that provide the
        // "find_me" capability and are directly connected.
        if (mGoogleApiClient.isConnected()) {
            setOrUpdateNotification();
        } else if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    private void setOrUpdateNotification() {
        Wearable.CapabilityApi.getCapability(
                mGoogleApiClient, FIND_ME_CAPABILITY_NAME,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(
                new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        if (result.getStatus().isSuccess()) {
                            updateFindMeCapability(result.getCapability());
                        } else {
                            Log.e(TAG,
                                    "setOrUpdateNotification() Failed to get capabilities, "
                                            + "status: "
                                            + result.getStatus().getStatusMessage());
                        }
                    }
                });
    }

    private void updateFindMeCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        if (connectedNodes.isEmpty()) {
            setupLostConnectivityNotification();
        } else {
            for (Node node : connectedNodes) {
                // we are only considering those nodes that are directly connected
                if (node.isNearby()) {
                    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                            .cancel(FORGOT_PHONE_NOTIFICATION_ID);
                }
            }
        }
    }

    /**
     * Creates a notification to inform user that the connectivity to phone has been lost (possibly
     * left the phone behind).
     */
    private void setupLostConnectivityNotification() {
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.left_phone_title))
                .setContentText(getString(R.string.left_phone_content))
                .setVibrate(new long[]{0, 200})  // Vibrate for 200 milliseconds.
                .setSmallIcon(R.drawable.ic_launcher)
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_MAX);
        Notification card = notificationBuilder.build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(FORGOT_PHONE_NOTIFICATION_ID, card);
    }

    @Override
    public void onConnected(Bundle bundle) {
        setOrUpdateNotification();
    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }
}
