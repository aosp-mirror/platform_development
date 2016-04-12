/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.runtimepermissions;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.example.android.wearable.runtimepermissions.common.Constants;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Handles all incoming requests for wear data (and permissions) from phone devices.
 */
public class IncomingRequestWearService extends WearableListenerService {

    private static final String TAG = "IncomingRequestService";

    public IncomingRequestWearService() {
        Log.d(TAG, "IncomingRequestWearService()");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived(): " + messageEvent);

        String messagePath = messageEvent.getPath();

        if (messagePath.equals(Constants.MESSAGE_PATH_WEAR)) {
            DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());

            int requestType = dataMap.getInt(Constants.KEY_COMM_TYPE);

            if (requestType == Constants.COMM_TYPE_REQUEST_PROMPT_PERMISSION) {
                promptUserForSensorPermission();

            } else if (requestType == Constants.COMM_TYPE_REQUEST_DATA) {
                respondWithSensorInformation();
            }
        }
    }

    private void promptUserForSensorPermission() {
        Log.d(TAG, "promptUserForSensorPermission()");

        boolean sensorPermissionApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                        == PackageManager.PERMISSION_GRANTED;

        if (sensorPermissionApproved) {
            DataMap dataMap = new DataMap();
            dataMap.putInt(Constants.KEY_COMM_TYPE,
                    Constants.COMM_TYPE_RESPONSE_USER_APPROVED_PERMISSION);
            sendMessage(dataMap);
        } else {
            // Launch Activity to grant sensor permissions.
            Intent startIntent = new Intent(this, MainWearActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra(MainWearActivity.EXTRA_PROMPT_PERMISSION_FROM_PHONE, true);
            startActivity(startIntent);
        }
    }

    private void respondWithSensorInformation() {
        Log.d(TAG, "respondWithSensorInformation()");

        boolean sensorPermissionApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                        == PackageManager.PERMISSION_GRANTED;

        if (!sensorPermissionApproved) {
            DataMap dataMap = new DataMap();
            dataMap.putInt(Constants.KEY_COMM_TYPE,
                    Constants.COMM_TYPE_RESPONSE_PERMISSION_REQUIRED);
            sendMessage(dataMap);
        } else {
            /* To keep the sample simple, we are only displaying the number of sensors. You could do
             * something much more complicated.
             */
            SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            int numberOfSensorsOnDevice = sensorList.size();

            String sensorSummary = numberOfSensorsOnDevice + " sensors on wear device(s)!";
            DataMap dataMap = new DataMap();
            dataMap.putInt(Constants.KEY_COMM_TYPE,
                    Constants.COMM_TYPE_RESPONSE_DATA);
            dataMap.putString(Constants.KEY_PAYLOAD, sensorSummary);
            sendMessage(dataMap);
        }
    }

    private void sendMessage(DataMap dataMap) {

        Log.d(TAG, "sendMessage(): " + dataMap);

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(
                        Constants.CONNECTION_TIME_OUT_MS,
                        TimeUnit.MILLISECONDS);

        if (!connectionResult.isSuccess()) {
            Log.d(TAG, "Google API Client failed to connect.");
            return;
        }

        PendingResult<CapabilityApi.GetCapabilityResult> pendingCapabilityResult =
                Wearable.CapabilityApi.getCapability(
                        googleApiClient,
                        Constants.CAPABILITY_PHONE_APP,
                        CapabilityApi.FILTER_REACHABLE);

        CapabilityApi.GetCapabilityResult getCapabilityResult =
                pendingCapabilityResult.await(
                        Constants.CONNECTION_TIME_OUT_MS,
                        TimeUnit.MILLISECONDS);

        if (!getCapabilityResult.getStatus().isSuccess()) {
            Log.d(TAG, "CapabilityApi failed to return any results.");
            googleApiClient.disconnect();
            return;
        }

        CapabilityInfo capabilityInfo = getCapabilityResult.getCapability();
        String phoneNodeId = pickBestNodeId(capabilityInfo.getNodes());

        PendingResult<MessageApi.SendMessageResult> pendingMessageResult =
                Wearable.MessageApi.sendMessage(
                        googleApiClient,
                        phoneNodeId,
                        Constants.MESSAGE_PATH_PHONE,
                        dataMap.toByteArray());

        MessageApi.SendMessageResult sendMessageResult =
                pendingMessageResult.await(Constants.CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);

        if (!sendMessageResult.getStatus().isSuccess()) {
            Log.d(TAG, "Sending message failed, onResult: " + sendMessageResult.getStatus());
        } else {
            Log.d(TAG, "Message sent successfully");
        }

        googleApiClient.disconnect();
    }

    /*
     * There should only ever be one phone in a node set (much less w/ the correct capability), so
     * I am just grabbing the first one (which should be the only one).
     */
    private String pickBestNodeId(Set<Node> nodes) {

        Log.d(TAG, "pickBestNodeId: " + nodes);


        String bestNodeId = null;
        /* Find a nearby node or pick one arbitrarily. There should be only one phone connected
         * that supports this sample.
         */
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }
}