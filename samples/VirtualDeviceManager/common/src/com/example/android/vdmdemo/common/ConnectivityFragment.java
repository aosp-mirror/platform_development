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

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.Manifest.permission;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.gms.nearby.connection.BandwidthInfo;
import com.google.android.gms.nearby.connection.BandwidthInfo.Quality;
import com.google.common.collect.ImmutableSet;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/** Fragment that holds the connectivity status UI. */
@AndroidEntryPoint(Fragment.class)
public final class ConnectivityFragment extends Hilt_ConnectivityFragment {

    private static final int REQUIRE_PERMISSIONS_FOR_APP_REQUEST_CODE = 0;

    private static final ImmutableSet<String> REQUIRED_PERMISSIONS_FOR_APP =
            ImmutableSet.of(
                    permission.BLUETOOTH_SCAN,
                    permission.BLUETOOTH_ADVERTISE,
                    permission.BLUETOOTH_CONNECT,
                    permission.ACCESS_WIFI_STATE,
                    permission.CHANGE_WIFI_STATE,
                    permission.NEARBY_WIFI_DEVICES);

    @Inject ConnectionManager connectionManager;

    private TextView status = null;
    private int defaultBackgroundColor;

    private final ConnectionManager.ConnectionCallback connectionCallback =
            new ConnectionManager.ConnectionCallback() {
                @Override
                public void onConnecting(String remoteDeviceName) {
                    status.setText(getContext().getString(R.string.connecting, remoteDeviceName));
                }

                @Override
                public void onConnected(String remoteDeviceName) {
                    status.setBackgroundColor(Color.GREEN);
                }

                @Override
                public void onBandwidthChanged(
                        String remoteDeviceName, BandwidthInfo bandwidthInfo) {
                    String quality = bandwidthQualityToString(bandwidthInfo.getQuality());
                    status.setText(
                            getContext().getString(R.string.connected, remoteDeviceName, quality));
                }

                @Override
                public void onDisconnected() {
                    status.setBackgroundColor(defaultBackgroundColor);
                    status.setText(getContext().getString(R.string.disconnected));
                }
            };

    public ConnectivityFragment() {
        super(R.layout.connectivity_fragment);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        status = getActivity().findViewById(R.id.connection_status);

        TypedValue background = new TypedValue();
        getActivity()
                .getTheme()
                .resolveAttribute(android.R.attr.windowBackground, background, true);
        defaultBackgroundColor = background.isColorType() ? background.data : Color.WHITE;

        CharSequence currentTitle = getActivity().getTitle();
        String localEndpointId = ConnectionManager.getLocalEndpointId();
        String title = getActivity().getString(R.string.this_device, currentTitle, localEndpointId);
        getActivity().setTitle(title);

        ConnectionManager.ConnectionStatus connectionStatus =
                connectionManager.getConnectionStatus();
        if (connectionStatus.bandwidthInfo != null) {
            connectionCallback.onConnected(connectionStatus.remoteDeviceName);
            connectionCallback.onBandwidthChanged(
                    connectionStatus.remoteDeviceName, connectionStatus.bandwidthInfo);
        } else if (connectionStatus.remoteDeviceName != null) {
            connectionCallback.onConnecting(connectionStatus.remoteDeviceName);
        } else {
            connectionCallback.onDisconnected();
        }

        connectionManager.addConnectionCallback(connectionCallback);
        requestPermissionsIfNeeded();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        connectionManager.removeConnectionCallback(connectionCallback);
    }

    private void requestPermissionsIfNeeded() {
        List<String> permissionsToBeRequested = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS_FOR_APP) {
            if (getActivity().checkSelfPermission(permission) == PERMISSION_DENIED) {
                permissionsToBeRequested.add(permission);
            }
        }
        if (!permissionsToBeRequested.isEmpty()) {
            getActivity()
                    .requestPermissions(
                            permissionsToBeRequested.toArray(new String[0]),
                            REQUIRE_PERMISSIONS_FOR_APP_REQUEST_CODE);
        }
    }

    private static String bandwidthQualityToString(@Quality int quality) {
        return switch (quality) {
            case Quality.LOW -> "LOW";
            case Quality.MEDIUM -> "MEDIUM";
            case Quality.HIGH -> "HIGH";
            default -> "UNKNOWN[" + quality + "]";
        };
    }
}
