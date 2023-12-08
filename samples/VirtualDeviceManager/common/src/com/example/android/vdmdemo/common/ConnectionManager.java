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
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareNetworkInfo;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Shared class between the client and the host, managing the connection between them. */
@Singleton
public class ConnectionManager {

    private static final String TAG = "VdmConnectionManager";
    private static final String CONNECTION_SERVICE_ID = "com.example.android.vdmdemo";

    private final RemoteIo mRemoteIo;

    @ApplicationContext private final Context mContext;
    private final ConnectivityManager mConnectivityManager;
    private final Handler mBackgroundHandler;
    private WifiAwareSession mWifiAwareSession;
    private DiscoverySession mDiscoverySession;

    /** Simple data structure to allow clients to query the current status. */
    public static final class ConnectionStatus {
        public String remoteDeviceName = null;
        public boolean connected = false;
    }

    private final ConnectionStatus mConnectionStatus = new ConnectionStatus();

    /** Simple callback to notify connection and disconnection events. */
    public interface ConnectionCallback {
        /** A connection has been initiated. */
        default void onConnecting(String remoteDeviceName) {}

        /** A connection has been established. */
        default void onConnected(String remoteDeviceName) {}

        /** The connection has been lost. */
        default void onDisconnected() {}

        /** An unrecoverable error has occurred. */
        default void onError(String message) {}
    }

    private final List<ConnectionCallback> mConnectionCallbacks =
            Collections.synchronizedList(new ArrayList<>());

    private final RemoteIo.StreamClosedCallback mStreamClosedCallback = this::disconnect;

    @Inject
    ConnectionManager(@ApplicationContext Context context, RemoteIo remoteIo) {
        mRemoteIo = remoteIo;
        mContext = context;

        mConnectivityManager = context.getSystemService(ConnectivityManager.class);
        final HandlerThread backgroundThread = new HandlerThread("ConnectionThread");
        backgroundThread.start();
        mBackgroundHandler = new Handler(backgroundThread.getLooper());
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

    /** Publish a local service so remote devices can discover this device. */
    public void startHostSession() {
        if (mConnectionStatus.connected) {
            return;
        }

        Runnable publishRunnable = () ->
                mWifiAwareSession.publish(
                        new PublishConfig.Builder().setServiceName(CONNECTION_SERVICE_ID).build(),
                        new HostDiscoverySessionCallback(),
                        mBackgroundHandler);

        if (mWifiAwareSession == null) {
            createSession(publishRunnable);
        } else {
            publishRunnable.run();
        }
    }

    /** Looks for published services from remote devices and subscribes to them. */
    public void startClientSession() {
        if (mConnectionStatus.connected) {
            return;
        }

        Runnable subscribeRunnable = () ->
                mWifiAwareSession.subscribe(
                        new SubscribeConfig.Builder().setServiceName(CONNECTION_SERVICE_ID).build(),
                        new ClientDiscoverySessionCallback(),
                        mBackgroundHandler);

        if (mWifiAwareSession == null) {
            createSession(subscribeRunnable);
        } else {
            subscribeRunnable.run();
        }
    }

    private void createSession(Runnable runnable) {
        WifiAwareManager wifiAwareManager = mContext.getSystemService(WifiAwareManager.class);
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
                || wifiAwareManager == null
                || !wifiAwareManager.isAvailable()) {
            onError("Wifi Aware is not available.");
        } else {
            wifiAwareManager.attach(
                    new AttachCallback() {
                        @Override
                        public void onAttached(WifiAwareSession session) {
                            mWifiAwareSession = session;
                            Log.e("vladokom", "Created wifi session");
                            runnable.run();
                        }

                        @Override
                        public void onAttachFailed() {
                            onError("Failed to attach Wifi Aware session.");
                        }
                    },
                    mBackgroundHandler);
        }
    }

    /** Explicitly terminate any existing connection. */
    public void disconnect() {
        if (mDiscoverySession != null) {
            mDiscoverySession.close();
            mDiscoverySession = null;
        }
        mConnectionStatus.remoteDeviceName = null;
        mConnectionStatus.connected = false;
        for (ConnectionCallback callback : mConnectionCallbacks) {
            callback.onDisconnected();
        }
    }

    private void onSocketAvailable(Socket socket) throws IOException {
        mRemoteIo.initialize(socket.getInputStream(), mStreamClosedCallback);
        mRemoteIo.initialize(socket.getOutputStream(), mStreamClosedCallback);
        mConnectionStatus.connected = true;
        for (ConnectionCallback callback : mConnectionCallbacks) {
            callback.onConnected(mConnectionStatus.remoteDeviceName);
        }
    }

    private void onError(String message) {
        Log.e(TAG, "Error: " + message);
        for (ConnectionCallback callback : mConnectionCallbacks) {
            callback.onError(message);
        }
    }

    private class VdmDiscoverySessionCallback extends DiscoverySessionCallback {
        private NetworkCallback mNetworkCallback;

        @Override
        public void onSessionTerminated() {
            disconnect();
            if (mNetworkCallback != null) {
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            }
        }

        void sendLocalEndpointId(PeerHandle peerHandle) {
            mDiscoverySession.sendMessage(peerHandle, 0, getLocalEndpointId().getBytes());
        }

        void onConnecting(byte[] remoteDeviceName) {
            mConnectionStatus.remoteDeviceName = new String(remoteDeviceName);
            Log.e(TAG, "Connecting to " + mConnectionStatus.remoteDeviceName);
            for (ConnectionCallback callback : mConnectionCallbacks) {
                callback.onConnecting(mConnectionStatus.remoteDeviceName);
            }
        }

        void requestNetwork(
                PeerHandle peerHandle, Optional<Integer> port, NetworkCallback networkCallback) {
            WifiAwareNetworkSpecifier.Builder networkSpecifierBuilder =
                    new WifiAwareNetworkSpecifier.Builder(mDiscoverySession, peerHandle)
                            .setPskPassphrase(CONNECTION_SERVICE_ID);
            port.ifPresent(networkSpecifierBuilder::setPort);

            NetworkRequest networkRequest =
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                            .setNetworkSpecifier(networkSpecifierBuilder.build())
                            .build();
            mNetworkCallback = networkCallback;
            mConnectivityManager.requestNetwork(networkRequest, networkCallback);
        }
    }

    private final class HostDiscoverySessionCallback extends VdmDiscoverySessionCallback {

        @Override
        public void onPublishStarted(@NonNull PublishDiscoverySession session) {
            Log.e("vladokom", "Created publish");
            mDiscoverySession = session;
        }

        @Override
        public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
            if (mConnectionStatus.connected) {
                return;
            }

            onConnecting(message);

            try {
                ServerSocket serverSocket = new ServerSocket(0);
                requestNetwork(
                        peerHandle,
                        Optional.of(serverSocket.getLocalPort()),
                        new NetworkCallback());
                sendLocalEndpointId(peerHandle);
                onSocketAvailable(serverSocket.accept());
            } catch (IOException e) {
                onError("Failed to establish connection.");
            }
        }
    }

    private final class ClientDiscoverySessionCallback extends VdmDiscoverySessionCallback {

        @Override
        public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
            Log.e("vladokom", "Created subscribe");
            mDiscoverySession = session;
        }

        @Override
        public void onServiceDiscovered(
                PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
            Log.e("vladokom", "service discovered");
            sendLocalEndpointId(peerHandle);
        }

        @Override
        public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
            if (mConnectionStatus.connected) {
                return;
            }
            onConnecting(message);
            requestNetwork(peerHandle, /* port= */ Optional.empty(), new ClientNetworkCallback());
        }
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {

        @Override
        public void onLost(@NonNull Network network) {
            disconnect();
        }
    }

    private class ClientNetworkCallback extends NetworkCallback {

        @Override
        public void onCapabilitiesChanged(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities) {
            if (mConnectionStatus.connected) {
                return;
            }

            WifiAwareNetworkInfo peerAwareInfo =
                    (WifiAwareNetworkInfo) networkCapabilities.getTransportInfo();
            Inet6Address peerIpv6 = peerAwareInfo.getPeerIpv6Addr();
            int peerPort = peerAwareInfo.getPort();
            try {
                Socket socket = network.getSocketFactory().createSocket(peerIpv6, peerPort);
                onSocketAvailable(socket);
            } catch (IOException e) {
                Log.e(TAG, "Failed to establish connection.", e);
                onError("Failed to establish connection.");
            }
        }
    }
}
