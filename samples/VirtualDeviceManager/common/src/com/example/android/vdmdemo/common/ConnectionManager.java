/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.common;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.BandwidthInfo;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Shared class between the client and the host, managing the connection between them. */
@Singleton
public class ConnectionManager {

    private static final String CONNECTION_SERVICE_ID = "com.example.android.vdmdemo";

    private final RemoteIo mRemoteIo;

    private final ConnectionsClient mClient;

    /** Simple data structure to allow clients to query the current status. */
    public static final class ConnectionStatus {
        public String remoteDeviceName = null;
        public boolean connected = false;
        public BandwidthInfo bandwidthInfo = null;
    }

    private final ConnectionStatus mConnectionStatus = new ConnectionStatus();

    /** Simple callback to notify connection and disconnection events. */
    public interface ConnectionCallback {
        /** A connection has been initiated. */
        default void onConnecting(String remoteDeviceName) {}

        /** A connection has been established. */
        default void onConnected(String remoteDeviceName) {}

        /** The bandwidth of the established connection has changed. */
        default void onBandwidthChanged(String remoteDeviceName, BandwidthInfo quality) {}

        /** The connection has been lost. */
        default void onDisconnected() {}
    }

    private final List<ConnectionCallback> mConnectionCallbacks =
            Collections.synchronizedList(new ArrayList<>());

    private final RemoteIo.StreamClosedCallback mStreamClosedCallback = this::disconnect;

    @Inject
    ConnectionManager(@ApplicationContext Context context, RemoteIo remoteIo) {
        mRemoteIo = remoteIo;
        mClient = Nearby.getConnectionsClient(context);
    }

    static String getLocalEndpointId() {
        return Build.MODEL;
    }

    /** Registers a listener for connection events. */
    public void addConnectionCallback(ConnectionCallback callback) {
        mConnectionCallbacks.add(callback);
    }

    /** Registers a listener for connection events. */
    public void removeConnectionCallback(ConnectionCallback callback) {
        mConnectionCallbacks.remove(callback);
    }

    /** Returns the current connection status. */
    public ConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    /** Starts advertising so remote devices can discover this device. */
    public void startAdvertising() {
        if (mConnectionStatus.connected) {
            return;
        }
        mClient.startAdvertising(
                        getLocalEndpointId(),
                        CONNECTION_SERVICE_ID,
                        mConnectionLifecycleCallback,
                        new AdvertisingOptions.Builder()
                                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                                .build())
                .addOnFailureListener(
                        (Exception e) ->
                                mConnectionLifecycleCallback.onDisconnected(
                                        /* endpointId= */ null));
    }

    /** Stops advertising making this device not discoverable. */
    public void stopAdvertising() {
        mClient.stopAdvertising();
    }

    /** Starts discovering remote devices that are advertising. */
    public void startDiscovery() {
        if (mConnectionStatus.connected) {
            return;
        }
        mClient.startDiscovery(
                        CONNECTION_SERVICE_ID,
                        mEndpointDiscoveryCallback,
                        new DiscoveryOptions.Builder()
                                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                                .build())
                .addOnFailureListener(
                        (Exception e) ->
                                mConnectionLifecycleCallback.onDisconnected(
                                        /* endpointId= */ null));
    }

    /** Stops discovering remote devices. */
    public void stopDiscovery() {
        mClient.stopDiscovery();
    }

    /** Explicitly terminate any existing connection. */
    public void disconnect() {
        mConnectionLifecycleCallback.onDisconnected(mConnectionStatus.remoteDeviceName);
    }

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    if (mConnectionStatus.connected) {
                        return;
                    }
                    mConnectionStatus.remoteDeviceName = info.getEndpointName();
                    for (ConnectionCallback callback : mConnectionCallbacks) {
                        callback.onConnecting(mConnectionStatus.remoteDeviceName);
                    }
                    mClient.requestConnection(
                                    getLocalEndpointId(), endpointId, mConnectionLifecycleCallback)
                            .addOnFailureListener(
                                    (Exception e) ->
                                            mConnectionLifecycleCallback.onDisconnected(
                                                    endpointId));
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    if (mConnectionStatus.connected) {
                        return;
                    }
                    mConnectionLifecycleCallback.onDisconnected(endpointId);
                }
            };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        String endpointId, ConnectionInfo connectionInfo) {
                    mConnectionStatus.remoteDeviceName = connectionInfo.getEndpointName();
                    for (ConnectionCallback callback : mConnectionCallbacks) {
                        callback.onConnecting(mConnectionStatus.remoteDeviceName);
                    }
                    mClient.acceptConnection(
                            endpointId,
                            new PayloadCallback() {
                                @Override
                                public void onPayloadReceived(String endpointId, Payload payload) {
                                    mRemoteIo.initialize(
                                            Objects.requireNonNull(payload.asStream())
                                                    .asInputStream(),
                                            mStreamClosedCallback);
                                }

                                @Override
                                public void onPayloadTransferUpdate(
                                        String endpointId, PayloadTransferUpdate update) {}
                            });
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (!result.getStatus().isSuccess()) {
                        onDisconnected(endpointId);
                        return;
                    }
                    PipedOutputStream outputStream = new PipedOutputStream();
                    try {
                        PipedInputStream inputStream = new PipedInputStream(outputStream);
                        mClient.sendPayload(endpointId, Payload.fromStream(inputStream));
                        mRemoteIo.initialize(outputStream, mStreamClosedCallback);
                        for (ConnectionCallback callback : mConnectionCallbacks) {
                            callback.onConnected(mConnectionStatus.remoteDeviceName);
                        }
                        mConnectionStatus.connected = true;
                    } catch (IOException e) {
                        throw new AssertionError("Unhandled exception", e);
                    }
                }

                @Override
                public void onBandwidthChanged(
                        String endpointId, @NonNull BandwidthInfo bandwidthInfo) {
                    mConnectionStatus.bandwidthInfo = bandwidthInfo;
                    for (ConnectionCallback callback : mConnectionCallbacks) {
                        callback.onBandwidthChanged(
                                mConnectionStatus.remoteDeviceName,
                                mConnectionStatus.bandwidthInfo);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    mClient.stopAllEndpoints();
                    mConnectionStatus.remoteDeviceName = null;
                    mConnectionStatus.bandwidthInfo = null;
                    mConnectionStatus.connected = false;
                    for (ConnectionCallback callback : mConnectionCallbacks) {
                        callback.onDisconnected();
                    }
                }
            };
}
