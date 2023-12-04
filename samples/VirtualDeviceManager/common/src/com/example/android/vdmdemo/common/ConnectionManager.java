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

  private final RemoteIo remoteIo;

  private final ConnectionsClient client;

  /** Simple data structure to allow clients to query the current status. */
  public static final class ConnectionStatus {
    public String remoteDeviceName = null;
    public boolean connected = false;
    public BandwidthInfo bandwidthInfo = null;
  }

  private final ConnectionStatus connectionStatus = new ConnectionStatus();

  /** Simple callback to notify connection and disconnection events. */
  public interface ConnectionCallback {
    default void onConnecting(String remoteDeviceName) {}

    default void onConnected(String remoteDeviceName) {}

    default void onBandwidthChanged(String remoteDeviceName, BandwidthInfo quality) {}

    default void onDisconnected() {}
  }

  private final List<ConnectionCallback> callbacks =
      Collections.synchronizedList(new ArrayList<>());

  @Inject
  ConnectionManager(@ApplicationContext Context context, RemoteIo remoteIo) {
    this.remoteIo = remoteIo;
    client = Nearby.getConnectionsClient(context);
  }

  static String getLocalEndpointId() {
    return Build.MODEL;
  }

  public void addConnectionCallback(ConnectionCallback callback) {
    callbacks.add(callback);
  }

  public void removeConnectionCallback(ConnectionCallback callback) {
    callbacks.remove(callback);
  }

  public ConnectionStatus getConnectionStatus() {
    return connectionStatus;
  }

  public void startAdvertising() {
    if (connectionStatus.connected) {
      return;
    }
    client
        .startAdvertising(
            getLocalEndpointId(),
            CONNECTION_SERVICE_ID,
            connectionLifecycleCallback,
            new AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                .build())
        .addOnFailureListener(
            (Exception e) -> connectionLifecycleCallback.onDisconnected(/* endpointId= */ null));
  }

  public void stopAdvertising() {
    client.stopAdvertising();
  }

  public void startDiscovery() {
    if (connectionStatus.connected) {
      return;
    }
    client
        .startDiscovery(
            CONNECTION_SERVICE_ID,
            endpointDiscoveryCallback,
            new DiscoveryOptions.Builder()
                .setStrategy(Strategy.P2P_POINT_TO_POINT)
                .build())
        .addOnFailureListener(
            (Exception e) -> connectionLifecycleCallback.onDisconnected(/* endpointId= */ null));
  }

  public void stopDiscovery() {
    client.stopDiscovery();
  }

  public void disconnect() {
    connectionLifecycleCallback.onDisconnected(connectionStatus.remoteDeviceName);
  }

  private final EndpointDiscoveryCallback endpointDiscoveryCallback =
      new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
          if (connectionStatus.connected) {
            return;
          }
          connectionStatus.remoteDeviceName = info.getEndpointName();
          for (ConnectionCallback callback : callbacks) {
            callback.onConnecting(connectionStatus.remoteDeviceName);
          }
          client
              .requestConnection(
                  getLocalEndpointId(),
                  endpointId,
                  connectionLifecycleCallback)
              .addOnFailureListener(
                  (Exception e) -> connectionLifecycleCallback.onDisconnected(endpointId));
        }

        @Override
        public void onEndpointLost(String endpointId) {
          if (connectionStatus.connected) {
            return;
          }
          connectionLifecycleCallback.onDisconnected(endpointId);
        }
      };

  private final RemoteIo.StreamClosedCallback streamClosedCallback = this::disconnect;

  private final ConnectionLifecycleCallback connectionLifecycleCallback =
      new ConnectionLifecycleCallback() {

        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
          connectionStatus.remoteDeviceName = connectionInfo.getEndpointName();
          for (ConnectionCallback callback : callbacks) {
            callback.onConnecting(connectionStatus.remoteDeviceName);
          }
          client.acceptConnection(
              endpointId,
              new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                  remoteIo.initialize(
                      Objects.requireNonNull(payload.asStream()).asInputStream(),
                      streamClosedCallback);
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
            client.sendPayload(endpointId, Payload.fromStream(inputStream));
            remoteIo.initialize(outputStream, streamClosedCallback);
            for (ConnectionCallback callback : callbacks) {
              callback.onConnected(connectionStatus.remoteDeviceName);
            }
            connectionStatus.connected = true;
          } catch (IOException e) {
            throw new AssertionError("Unhandled exception", e);
          }
        }

        @Override
        public void onBandwidthChanged(String endpointId, @NonNull BandwidthInfo bandwidthInfo) {
          connectionStatus.bandwidthInfo = bandwidthInfo;
          for (ConnectionCallback callback : callbacks) {
            callback.onBandwidthChanged(
                connectionStatus.remoteDeviceName, connectionStatus.bandwidthInfo);
          }
        }

        @Override
        public void onDisconnected(String endpointId) {
          client.stopAllEndpoints();
          connectionStatus.remoteDeviceName = null;
          connectionStatus.bandwidthInfo = null;
          connectionStatus.connected = false;
          for (ConnectionCallback callback : callbacks) {
            callback.onDisconnected();
          }
        }
      };
}
