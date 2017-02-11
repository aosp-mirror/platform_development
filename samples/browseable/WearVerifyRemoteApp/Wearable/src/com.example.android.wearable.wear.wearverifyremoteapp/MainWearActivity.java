/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
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
package com.example.android.wearable.wear.wearverifyremoteapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.ConfirmationOverlay;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.wearable.intent.RemoteIntent;
import com.google.android.wearable.playstore.PlayStoreAvailability;

import java.util.Set;

/**
 * Checks if the phone app is installed on remote device. If it is not, allows user to open app
 * listing on the phone's Play or App Store.
 */
public class MainWearActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener {

    private static final String TAG = "MainWearActivity";

    private static final String WELCOME_MESSAGE = "Welcome to our Wear app!\n\n";

    private static final String CHECKING_MESSAGE =
            WELCOME_MESSAGE + "Checking for Mobile app...\n";

    private static final String MISSING_MESSAGE =
            WELCOME_MESSAGE
                    + "You are missing the required phone app, please click on the button below to "
                    + "install it on your phone.\n";

    private static final String INSTALLED_MESSAGE =
            WELCOME_MESSAGE
                    + "Mobile app installed on your %s!\n\nYou can now use MessageApi, "
                    + "DataApi, etc.";

    // Name of capability listed in Phone app's wear.xml.
    // IMPORTANT NOTE: This should be named differently than your Wear app's capability.
    private static final String CAPABILITY_PHONE_APP = "verify_remote_example_phone_app";

    // Links to install mobile app for both Android (Play Store) and iOS.
    // TODO: Replace with your links/packages.
    private static final String PLAY_STORE_APP_URI =
            "market://details?id=com.example.android.wearable.wear.wearverifyremoteapp";

    // TODO: Replace with your links/packages.
    private static final String APP_STORE_APP_URI =
            "https://itunes.apple.com/us/app/android-wear/id986496028?mt=8";

    // Result from sending RemoteIntent to phone to open app in play/app store.
    private final ResultReceiver mResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode == RemoteIntent.RESULT_OK) {
                new ConfirmationOverlay().showOn(MainWearActivity.this);

            } else if (resultCode == RemoteIntent.RESULT_FAILED) {
                new ConfirmationOverlay()
                        .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                        .showOn(MainWearActivity.this);

            } else {
                throw new IllegalStateException("Unexpected result " + resultCode);
            }
        }
    };

    private TextView mInformationTextView;
    private Button mRemoteOpenButton;

    private Node mAndroidPhoneNodeWithApp;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mInformationTextView = (TextView) findViewById(R.id.information_text_view);
        mRemoteOpenButton = (Button) findViewById(R.id.remote_open_button);

        mInformationTextView.setText(CHECKING_MESSAGE);

        mRemoteOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAppInStoreOnPhone();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
            Wearable.CapabilityApi.removeCapabilityListener(
                    mGoogleApiClient,
                    this,
                    CAPABILITY_PHONE_APP);

            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected()");

        // Set up listeners for capability changes (install/uninstall of remote app).
        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient,
                this,
                CAPABILITY_PHONE_APP);

        checkIfPhoneHasApp();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): connection to location client suspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed(): " + connectionResult);
    }

    /*
     * Updates UI when capabilities change (install/uninstall phone app).
     */
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): " + capabilityInfo);

        mAndroidPhoneNodeWithApp = pickBestNodeId(capabilityInfo.getNodes());
        verifyNodeAndUpdateUI();
    }

    private void checkIfPhoneHasApp() {
        Log.d(TAG, "checkIfPhoneHasApp()");

        PendingResult<CapabilityApi.GetCapabilityResult> pendingResult =
                Wearable.CapabilityApi.getCapability(
                        mGoogleApiClient,
                        CAPABILITY_PHONE_APP,
                        CapabilityApi.FILTER_ALL);

        pendingResult.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {

            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                Log.d(TAG, "onResult(): " + getCapabilityResult);

                if (getCapabilityResult.getStatus().isSuccess()) {
                    CapabilityInfo capabilityInfo = getCapabilityResult.getCapability();
                    mAndroidPhoneNodeWithApp = pickBestNodeId(capabilityInfo.getNodes());
                    verifyNodeAndUpdateUI();

                } else {
                    Log.d(TAG, "Failed CapabilityApi: " + getCapabilityResult.getStatus());
                }
            }
        });
    }

    private void verifyNodeAndUpdateUI() {

        if (mAndroidPhoneNodeWithApp != null) {

            // TODO: Add your code to communicate with the phone app via
            // Wear APIs (MessageApi, DataApi, etc.)

            String installMessage =
                    String.format(INSTALLED_MESSAGE, mAndroidPhoneNodeWithApp.getDisplayName());
            Log.d(TAG, installMessage);
            mInformationTextView.setText(installMessage);
            mRemoteOpenButton.setVisibility(View.INVISIBLE);

        } else {
            Log.d(TAG, MISSING_MESSAGE);
            mInformationTextView.setText(MISSING_MESSAGE);
            mRemoteOpenButton.setVisibility(View.VISIBLE);
        }
    }

    private void openAppInStoreOnPhone() {
        Log.d(TAG, "openAppInStoreOnPhone()");

        int playStoreAvailabilityOnPhone =
                PlayStoreAvailability.getPlayStoreAvailabilityOnPhone(getApplicationContext());

        switch (playStoreAvailabilityOnPhone) {

            // Android phone with the Play Store.
            case PlayStoreAvailability.PLAY_STORE_ON_PHONE_AVAILABLE:
                Log.d(TAG, "\tPLAY_STORE_ON_PHONE_AVAILABLE");

                // Create Remote Intent to open Play Store listing of app on remote device.
                Intent intentAndroid =
                        new Intent(Intent.ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .setData(Uri.parse(PLAY_STORE_APP_URI));

                RemoteIntent.startRemoteActivity(
                        getApplicationContext(),
                        intentAndroid,
                        mResultReceiver);
                break;

            // Assume iPhone (iOS device) or Android without Play Store (not supported right now).
            case PlayStoreAvailability.PLAY_STORE_ON_PHONE_UNAVAILABLE:
                Log.d(TAG, "\tPLAY_STORE_ON_PHONE_UNAVAILABLE");

                // Create Remote Intent to open App Store listing of app on iPhone.
                Intent intentIOS =
                        new Intent(Intent.ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .setData(Uri.parse(APP_STORE_APP_URI));

                RemoteIntent.startRemoteActivity(
                        getApplicationContext(),
                        intentIOS,
                        mResultReceiver);
                break;

            case PlayStoreAvailability.PLAY_STORE_ON_PHONE_ERROR_UNKNOWN:
                Log.d(TAG, "\tPLAY_STORE_ON_PHONE_ERROR_UNKNOWN");
                break;
        }
    }

    /*
     * There should only ever be one phone in a node set (much less w/ the correct capability), so
     * I am just grabbing the first one (which should be the only one).
     */
    private Node pickBestNodeId(Set<Node> nodes) {
        Log.d(TAG, "pickBestNodeId(): " + nodes);

        Node bestNodeId = null;
        // Find a nearby node/phone or pick one arbitrarily. Realistically, there is only one phone.
        for (Node node : nodes) {
            bestNodeId = node;
        }
        return bestNodeId;
    }
}