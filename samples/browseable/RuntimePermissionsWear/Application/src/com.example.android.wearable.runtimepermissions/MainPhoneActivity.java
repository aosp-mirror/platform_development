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
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.wearable.runtimepermissions.common.Constants;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Displays data that requires runtime permissions both locally (READ_EXTERNAL_STORAGE) and
 * remotely on wear (BODY_SENSORS).
 *
 * The class also handles sending back the results of a permission request from a remote wear device
 * when the permission has not been approved yet on the phone (uses EXTRA as trigger). In that case,
 * the IncomingRequestPhoneService launches the splash Activity (PhonePermissionRequestActivity) to
 * inform user of permission request. After the user decides what to do, it falls back to this
 * Activity (which has all the GoogleApiClient code) to handle sending data across and keeps user
 * in app experience.
 */
public class MainPhoneActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener,
        MessageApi.MessageListener,
        ResultCallback<MessageApi.SendMessageResult> {

    private static final String TAG = "MainPhoneActivity";

    /*
     * Alerts Activity that the initial request for permissions came from wear, and the Activity
     * needs to send back the results (data or permission rejection).
     */
    public static final String EXTRA_PROMPT_PERMISSION_FROM_WEAR =
            "com.example.android.wearable.runtimepermissions.extra.PROMPT_PERMISSION_FROM_WEAR";

    private static final int REQUEST_WEAR_PERMISSION_RATIONALE = 1;

    private boolean mWearBodySensorsPermissionApproved;
    private boolean mPhoneStoragePermissionApproved;

    private boolean mWearRequestingPhoneStoragePermission;

    private Button mWearBodySensorsPermissionButton;
    private Button mPhoneStoragePermissionButton;
    private TextView mOutputTextView;

    private Set<Node> mWearNodeIds;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        /*
         * Since this is a remote permission, we initialize it to false and then check the remote
         * permission once the GoogleApiClient is connected.
         */
        mWearBodySensorsPermissionApproved = false;

        setContentView(R.layout.activity_main);

        // Checks if wear app requested phone permission (permission request opens later if true).
        mWearRequestingPhoneStoragePermission =
                getIntent().getBooleanExtra(EXTRA_PROMPT_PERMISSION_FROM_WEAR, false);

        mPhoneStoragePermissionButton =
                (Button) findViewById(R.id.phoneStoragePermissionButton);

        mWearBodySensorsPermissionButton =
                (Button) findViewById(R.id.wearBodySensorsPermissionButton);

        mOutputTextView = (TextView) findViewById(R.id.output);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void onClickWearBodySensors(View view) {

        logToUi("Requested info from wear device(s). New approval may be required.");

        DataMap dataMap = new DataMap();
        dataMap.putInt(Constants.KEY_COMM_TYPE, Constants.COMM_TYPE_REQUEST_DATA);
        sendMessage(dataMap);
    }

    public void onClickPhoneStorage(View view) {

        if (mPhoneStoragePermissionApproved) {
            logToUi(getPhoneStorageInformation());

        } else {
            // On 23+ (M+) devices, Storage permission not granted. Request permission.
            Intent startIntent = new Intent(this, PhonePermissionRequestActivity.class);
            startActivity(startIntent);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
            Wearable.CapabilityApi.removeCapabilityListener(
                    mGoogleApiClient,
                    this,
                    Constants.CAPABILITY_WEAR_APP);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        /* Enables app to handle 23+ (M+) style permissions. It also covers user changing
         * permission in settings and coming back to the app.
         */
        mPhoneStoragePermissionApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (mPhoneStoragePermissionApproved) {
            mPhoneStoragePermissionButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_permission_approved, 0, 0, 0);
        }

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult()");
        if (requestCode == REQUEST_WEAR_PERMISSION_RATIONALE) {

            if (resultCode == Activity.RESULT_OK) {
                logToUi("Requested permission on wear device(s).");

                DataMap dataMap = new DataMap();
                dataMap.putInt(Constants.KEY_COMM_TYPE,
                        Constants.COMM_TYPE_REQUEST_PROMPT_PERMISSION);
                sendMessage(dataMap);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected()");

        // Set up listeners for capability and message changes.
        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient,
                this,
                Constants.CAPABILITY_WEAR_APP);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        // Initial check of capabilities to find the wear nodes.
        PendingResult<CapabilityApi.GetCapabilityResult> pendingResult =
                Wearable.CapabilityApi.getCapability(
                        mGoogleApiClient,
                        Constants.CAPABILITY_WEAR_APP,
                        CapabilityApi.FILTER_REACHABLE);

        pendingResult.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(CapabilityApi.GetCapabilityResult getCapabilityResult) {

                CapabilityInfo capabilityInfo = getCapabilityResult.getCapability();
                String capabilityName = capabilityInfo.getName();

                boolean wearSupportsSampleApp =
                        capabilityName.equals(Constants.CAPABILITY_WEAR_APP);

                if (wearSupportsSampleApp) {
                    mWearNodeIds = capabilityInfo.getNodes();

                    /*
                     * Upon getting all wear nodes, we now need to check if the original request to
                     * launch this activity (and PhonePermissionRequestActivity) was initiated by
                     * a wear device. If it was, we need to send back the permission results (data
                     * or rejection of permission) to the wear device.
                     *
                     * Also, note we set variable to false, this enables the user to continue
                     * changing permissions without sending updates to the wear every time.
                     */
                    if (mWearRequestingPhoneStoragePermission) {
                        mWearRequestingPhoneStoragePermission = false;
                        sendWearPermissionResults();
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): connection to location client suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed(): connection to location client failed");
    }


    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): " + capabilityInfo);

        mWearNodeIds = capabilityInfo.getNodes();
    }

    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived(): " + messageEvent);

        String messagePath = messageEvent.getPath();

        if (messagePath.equals(Constants.MESSAGE_PATH_PHONE)) {
            DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());

            int commType = dataMap.getInt(Constants.KEY_COMM_TYPE, 0);

            if (commType == Constants.COMM_TYPE_RESPONSE_PERMISSION_REQUIRED) {
                mWearBodySensorsPermissionApproved = false;
                updateWearButtonOnUiThread();

                /* Because our request for remote data requires a remote permission, we now launch
                 * a splash activity informing the user we need those permissions (along with
                 * other helpful information to approve).
                 */
                Intent wearPermissionRationale =
                        new Intent(this, WearPermissionRequestActivity.class);
                startActivityForResult(wearPermissionRationale, REQUEST_WEAR_PERMISSION_RATIONALE);

            } else if (commType == Constants.COMM_TYPE_RESPONSE_USER_APPROVED_PERMISSION) {
                mWearBodySensorsPermissionApproved = true;
                updateWearButtonOnUiThread();
                logToUi("User approved permission on remote device, requesting data again.");
                DataMap outgoingDataRequestDataMap = new DataMap();
                outgoingDataRequestDataMap.putInt(Constants.KEY_COMM_TYPE,
                        Constants.COMM_TYPE_REQUEST_DATA);
                sendMessage(outgoingDataRequestDataMap);

            } else if (commType == Constants.COMM_TYPE_RESPONSE_USER_DENIED_PERMISSION) {
                mWearBodySensorsPermissionApproved = false;
                updateWearButtonOnUiThread();
                logToUi("User denied permission on remote device.");

            } else if (commType == Constants.COMM_TYPE_RESPONSE_DATA) {
                mWearBodySensorsPermissionApproved = true;
                String storageDetails = dataMap.getString(Constants.KEY_PAYLOAD);
                updateWearButtonOnUiThread();
                logToUi(storageDetails);

            } else {
                Log.d(TAG, "Unrecognized communication type received.");
            }
        }
    }

    @Override
    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
        if (!sendMessageResult.getStatus().isSuccess()) {
            Log.d(TAG, "Sending message failed, onResult: " + sendMessageResult);
            updateWearButtonOnUiThread();
            logToUi("Sending message failed.");

        } else {
            Log.d(TAG, "Message sent.");
        }
    }

    private void sendMessage(DataMap dataMap) {
        Log.d(TAG, "sendMessage(): " + mWearNodeIds);

        if ((mWearNodeIds != null) && (!mWearNodeIds.isEmpty())) {

            PendingResult<MessageApi.SendMessageResult> pendingResult;

            for (Node node : mWearNodeIds) {

                pendingResult = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient,
                        node.getId(),
                        Constants.MESSAGE_PATH_WEAR,
                        dataMap.toByteArray());

                pendingResult.setResultCallback(this, Constants.CONNECTION_TIME_OUT_MS,
                        TimeUnit.SECONDS);
            }
        } else {
            // Unable to retrieve node with proper capability
            mWearBodySensorsPermissionApproved = false;
            updateWearButtonOnUiThread();
            logToUi("Wear devices not available to send message.");
        }
    }

    private void updateWearButtonOnUiThread() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mWearBodySensorsPermissionApproved) {
                    mWearBodySensorsPermissionButton.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_permission_approved, 0, 0, 0);
                } else {
                    mWearBodySensorsPermissionButton.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_permission_denied, 0, 0, 0);
                }
            }
        });
    }

    /*
     * Handles all messages for the UI coming on and off the main thread. Not all callbacks happen
     * on the main thread.
     */
    private void logToUi(final String message) {

        boolean mainUiThread = (Looper.myLooper() == Looper.getMainLooper());

        if (mainUiThread) {

            if (!message.isEmpty()) {
                Log.d(TAG, message);
                mOutputTextView.setText(message);
            }

        } else {
            if (!message.isEmpty()) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d(TAG, message);
                        mOutputTextView.setText(message);
                    }
                });
            }
        }
    }

    private String getPhoneStorageInformation() {

        StringBuilder stringBuilder = new StringBuilder();

        String state = Environment.getExternalStorageState();
        boolean isExternalStorageReadable = Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);

        if (isExternalStorageReadable) {
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            String[] fileList = externalStorageDirectory.list();

            if (fileList.length > 0) {

                stringBuilder.append("List of files\n");
                for (String file : fileList) {
                    stringBuilder.append(" - " + file + "\n");
                }

            } else {
                stringBuilder.append("No files in external storage.");
            }

        } else {
            stringBuilder.append("No external media is available.");
        }

        return stringBuilder.toString();
    }

    private void sendWearPermissionResults() {

        Log.d(TAG, "sendWearPermissionResults()");

        DataMap dataMap = new DataMap();

        if (mPhoneStoragePermissionApproved) {
            dataMap.putInt(Constants.KEY_COMM_TYPE,
                    Constants.COMM_TYPE_RESPONSE_USER_APPROVED_PERMISSION);
        } else {
            dataMap.putInt(Constants.KEY_COMM_TYPE,
                    Constants.COMM_TYPE_RESPONSE_USER_DENIED_PERMISSION);
        }
        sendMessage(dataMap);
    }
}
