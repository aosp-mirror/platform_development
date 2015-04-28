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

package com.example.android.wearable.delayedconfirmation;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.os.Bundle;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;

public class MainActivity extends Activity implements
        DelayedConfirmationView.DelayedConfirmationListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks,
        CapabilityApi.CapabilityListener {

    private static final String TAG = "DelayedConfirmation";
    private static final int NUM_SECONDS = 5;

    private static final String TIMER_SELECTED_PATH = "/timer_selected";
    private static final String TIMER_FINISHED_PATH = "/timer_finished";

    /* name of the capability that the phone side provides */
    private static final String CONFIRMATION_HANDLER_CAPABILITY_NAME = "confirmation_handler";

    private DelayedConfirmationView delayedConfirmationView;
    private GoogleApiClient mGoogleApiClient;

    /* the preferred note that can handle the confirmation capability */
    private Node mConfirmationHandlerNode;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main_activity);
        delayedConfirmationView = (DelayedConfirmationView) findViewById(R.id.delayed_confirmation);
        delayedConfirmationView.setTotalTimeMs(NUM_SECONDS * 1000);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient.isConnected()) {
            Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, this,
                    CONFIRMATION_HANDLER_CAPABILITY_NAME);
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    /**
     * Starts the DelayedConfirmationView when user presses "Start Timer" button.
     */
    public void onStartTimer(View view) {
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_timer_selected))
                .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(0, notification);
        sendMessageToCompanion(TIMER_SELECTED_PATH);
        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);
        finish();
    }

    @Override
    public void onTimerFinished(View v) {
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_timer_finished))
                .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(0, notification);
        sendMessageToCompanion(TIMER_FINISHED_PATH);
        finish();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google Api Client");
        mConfirmationHandlerNode = null;
    }

    private void sendMessageToCompanion(final String path) {
        if (mConfirmationHandlerNode != null) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mConfirmationHandlerNode.getId(),
                    path, new byte[0])
                    .setResultCallback(getSendMessageResultCallback(mConfirmationHandlerNode));
        } else {
            Toast.makeText(this, R.string.no_device_found, Toast.LENGTH_SHORT).show();
        }
    }

    private ResultCallback<MessageApi.SendMessageResult> getSendMessageResultCallback(
            final Node node) {
        return new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Log.e(TAG, "Failed to send message with status "
                            + sendMessageResult.getStatus());
                } else {
                    Log.d(TAG, "Sent confirmation message to node " + node.getDisplayName());
                }
            }
        };
    }

    private void setupConfirmationHandlerNode() {
        Wearable.CapabilityApi.addCapabilityListener(
                mGoogleApiClient, this, CONFIRMATION_HANDLER_CAPABILITY_NAME);

        Wearable.CapabilityApi.getCapability(
                mGoogleApiClient, CONFIRMATION_HANDLER_CAPABILITY_NAME,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(
                new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(TAG, "setupConfirmationHandlerNode() Failed to get capabilities, "
                                    + "status: " + result.getStatus().getStatusMessage());
                            return;
                        }
                        updateConfirmationCapability(result.getCapability());
                    }
                });
    }

    private void updateConfirmationCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        if (connectedNodes.isEmpty()) {
            mConfirmationHandlerNode = null;
        } else {
            mConfirmationHandlerNode = pickBestNode(connectedNodes);
        }
    }

    /**
     * We pick a node that is capabale of handling the confirmation. If there is more than one,
     * then we would prefer the one that is directly connected to this device. In general,
     * depending on the situation and requirements, the "best" node might be picked based on other
     * criteria.
     */
    private Node pickBestNode(Set<Node> connectedNodes) {
        Node best = null;
        if (connectedNodes != null) {
            for (Node node : connectedNodes) {
                if (node.isNearby()) {
                    return node;
                }
                best = node;
            }
        }
        return best;
    }

    @Override
    public void onConnected(Bundle bundle) {
        setupConfirmationHandlerNode();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mConfirmationHandlerNode = null;
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        updateConfirmationCapability(capabilityInfo);
    }
}
