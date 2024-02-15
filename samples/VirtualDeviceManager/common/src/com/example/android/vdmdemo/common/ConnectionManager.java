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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Shared class between the client and the host, managing the connection between them. */
@Singleton
public class ConnectionManager {

    private static final String TAG = "VdmConnectionManager";
    private static final String CONNECTION_SERVICE_ID = "com.example.android.vdmdemo";
    private static final int NETWORK_TIMEOUT_MS = 5000;

    private final RemoteIo mRemoteIo;

    @ApplicationContext private final Context mContext;
    private final ConnectivityManager mConnectivityManager;
    private final Handler mBackgroundHandler;

    private CompletableFuture<WifiAwareSession> mWifiAwareSessionFuture = new CompletableFuture<>();

    private DiscoverySession mDiscoverySession;

    /** Simple data structure to allow clients to query the current status. */
    public static final class ConnectionStatus {
        public String remoteDeviceName = null;
        public String errorMessage = null;
        public State state = State.DISCONNECTED;

        /** Enum indicating the current connection state. */
        public enum State {
            DISCONNECTED, INITIALIZED, CONNECTING, CONNECTED, ERROR
        }
    }

    @GuardedBy("mConnectionStatus")
    private final ConnectionStatus mConnectionStatus = new ConnectionStatus();

    @GuardedBy("mConnectionCallbacks")
    private final List<Consumer<ConnectionStatus>> mConnectionCallbacks = new ArrayList<>();

    private final RemoteIo.StreamClosedCallback mStreamClosedCallback = this::onInitialized;

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
    public void addConnectionCallback(Consumer<ConnectionStatus> callback) {
        synchronized (mConnectionCallbacks) {
            mConnectionCallbacks.add(callback);
        }
    }

    /** Registers a listener for connection events. */
    public void removeConnectionCallback(Consumer<ConnectionStatus> callback) {
        synchronized (mConnectionCallbacks) {
            mConnectionCallbacks.remove(callback);
        }
    }

    /** Returns the current connection status. */
    public ConnectionStatus getConnectionStatus() {
        synchronized (mConnectionStatus) {
            return mConnectionStatus;
        }
    }

    /** Publish a local service so remote devices can discover this device. */
    public void startHostSession() {
        var unused = createWifiAwareSession().thenAccept(session -> session.publish(
                    new PublishConfig.Builder().setServiceName(CONNECTION_SERVICE_ID).build(),
                    new HostDiscoverySessionCallback(),
                    mBackgroundHandler));
    }

    /** Looks for published services from remote devices and subscribes to them. */
    public void startClientSession() {
        var unused = createWifiAwareSession().thenAccept(session -> session.subscribe(
                new SubscribeConfig.Builder().setServiceName(CONNECTION_SERVICE_ID).build(),
                new ClientDiscoverySessionCallback(),
                mBackgroundHandler));
    }

    private boolean isConnected() {
        synchronized (mConnectionStatus) {
            return mConnectionStatus.state == ConnectionStatus.State.CONNECTED;
        }
    }

    private CompletableFuture<WifiAwareSession> createWifiAwareSession() {
        if (mWifiAwareSessionFuture.isDone()
                && !mWifiAwareSessionFuture.isCompletedExceptionally()) {
            return mWifiAwareSessionFuture;
        }

        Log.d(TAG, "Creating a new Wifi Aware session.");
        WifiAwareManager wifiAwareManager = mContext.getSystemService(WifiAwareManager.class);
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
                || wifiAwareManager == null
                || !wifiAwareManager.isAvailable()) {
            mWifiAwareSessionFuture.completeExceptionally(
                    new Exception("Wifi Aware is not available."));
        } else {
            wifiAwareManager.attach(
                    new AttachCallback() {
                        @Override
                        public void onAttached(WifiAwareSession session) {
                            Log.d(TAG, "New Wifi Aware attached.");
                            mWifiAwareSessionFuture.complete(session);
                        }

                        @Override
                        public void onAttachFailed() {
                            mWifiAwareSessionFuture.completeExceptionally(
                                    new Exception("Failed to attach Wifi Aware session."));
                        }
                    },
                    mBackgroundHandler);
        }
        mWifiAwareSessionFuture = mWifiAwareSessionFuture
                .exceptionally(e -> {
                    Log.e(TAG, "Failed to create Wifi Aware session", e);
                    onError("Failed to create Wifi Aware session");
                    return null;
                });
        return mWifiAwareSessionFuture;
    }

    /** Explicitly terminate any existing connection. */
    public void disconnect() {
        Log.d(TAG, "Terminating connections.");
        if (mDiscoverySession != null) {
            mDiscoverySession.close();
            mDiscoverySession = null;
        }
    }

    private void onSocketAvailable(Socket socket) throws IOException {
        mRemoteIo.initialize(socket.getInputStream(), mStreamClosedCallback);
        mRemoteIo.initialize(socket.getOutputStream(), mStreamClosedCallback);
        synchronized (mConnectionStatus) {
            mConnectionStatus.state = ConnectionStatus.State.CONNECTED;
            notifyStateChangedLocked();
        }
    }

    private void onInitialized() {
        synchronized (mConnectionStatus) {
            mConnectionStatus.state = ConnectionStatus.State.INITIALIZED;
            notifyStateChangedLocked();
        }
    }

    private void onConnecting(byte[] remoteDeviceName) {
        synchronized (mConnectionStatus) {
            mConnectionStatus.state = ConnectionStatus.State.CONNECTING;
            mConnectionStatus.remoteDeviceName = new String(remoteDeviceName);
            Log.d(TAG, "Connecting to " + mConnectionStatus.remoteDeviceName);
            notifyStateChangedLocked();
        }
    }

    private void onError(String message) {
        Log.e(TAG, "Error: " + message);
        synchronized (mConnectionStatus) {
            mConnectionStatus.state = ConnectionStatus.State.ERROR;
            mConnectionStatus.errorMessage = message;
            notifyStateChangedLocked();
        }
    }

    @GuardedBy("mConnectionStatus")
    private void notifyStateChangedLocked() {
        Log.d(TAG, "Connection state changed: " + mConnectionStatus.state);
        synchronized (mConnectionCallbacks) {
            for (Consumer<ConnectionStatus> callback : mConnectionCallbacks) {
                callback.accept(mConnectionStatus);
            }
        }
    }

    private abstract class VdmDiscoverySessionCallback extends DiscoverySessionCallback {

        private NetworkCallback mNetworkCallback;

        @Override
        public void onPublishStarted(@NonNull PublishDiscoverySession session) {
            mDiscoverySession = session;
            onInitialized();
        }

        @Override
        public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
            mDiscoverySession = session;
            onInitialized();
        }

        @Override
        public void onServiceDiscovered(
                PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
            Log.d(TAG, "Discovered service: " + new String(serviceSpecificInfo));
            sendLocalEndpointId(peerHandle);
        }

        @Override
        public void onSessionTerminated() {
            Log.d(TAG, "Discovery session terminated.");
            if (mNetworkCallback != null) {
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
                mNetworkCallback = null;
            }
        }

        void sendLocalEndpointId(PeerHandle peerHandle) {
            mDiscoverySession.sendMessage(peerHandle, 0, getLocalEndpointId().getBytes());
        }

        @Override
        public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
            Log.d(TAG, "Received message: " + new String(message));
            if (isConnected()) {
                return;
            }
            onConnecting(message);
            establishConnection(peerHandle);
        }

        protected abstract void establishConnection(PeerHandle peerHandle);

        void requestNetwork(
                PeerHandle peerHandle, Optional<Integer> port, NetworkCallback networkCallback) {
            WifiAwareNetworkSpecifier.Builder networkSpecifierBuilder;
            networkSpecifierBuilder =
                    new WifiAwareNetworkSpecifier.Builder(mDiscoverySession, peerHandle)
                            .setPskPassphrase(CONNECTION_SERVICE_ID);
            if (mNetworkCallback != null) {
                mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            }
            mNetworkCallback = networkCallback;
            port.ifPresent(networkSpecifierBuilder::setPort);

            NetworkRequest networkRequest =
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                            .setNetworkSpecifier(networkSpecifierBuilder.build())
                            .build();
            Log.d(TAG, "Requesting network");
            mConnectivityManager.requestNetwork(
                    networkRequest, mNetworkCallback, NETWORK_TIMEOUT_MS);
        }
    }

    private final class HostDiscoverySessionCallback extends VdmDiscoverySessionCallback {
        @Override
        protected void establishConnection(PeerHandle peerHandle) {
            try {
                ServerSocket serverSocket = new ServerSocket(0);
                serverSocket.setSoTimeout(NETWORK_TIMEOUT_MS);
                requestNetwork(peerHandle, Optional.of(serverSocket.getLocalPort()),
                        new NetworkCallback());
                sendLocalEndpointId(peerHandle);
                onSocketAvailable(serverSocket.accept());
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Socket timeout: " + e.getMessage());
            } catch (IOException e) {
                onError("Failed to establish connection.");
            }
        }
    }

    private final class ClientDiscoverySessionCallback extends VdmDiscoverySessionCallback {
        @Override
        protected void establishConnection(PeerHandle peerHandle) {
            requestNetwork(peerHandle, /* port= */ Optional.empty(), new ClientNetworkCallback());
        }
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {

        @Override
        public void onLost(@NonNull Network network) {
            Log.d(TAG, "Network lost");
            onInitialized();
        }

        @Override
        public void onUnavailable() {
            Log.d(TAG, "Network unavailable");
        }
    }

    private class ClientNetworkCallback extends NetworkCallback {

        @Override
        public void onCapabilitiesChanged(@NonNull Network network,
                @NonNull NetworkCapabilities networkCapabilities) {
            if (isConnected()) {
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
