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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.function.Consumer;

import javax.inject.Inject;

/** Fragment that holds the connectivity status UI. */
@AndroidEntryPoint(Fragment.class)
public final class ConnectivityFragment extends Hilt_ConnectivityFragment {

    @Inject ConnectionManager mConnectionManager;

    private TextView mStatus = null;
    private int mDefaultBackgroundColor;

    private final Consumer<ConnectionManager.ConnectionStatus> mConnectionCallback =
            (status) -> {
                switch (status.state) {
                    case DISCONNECTED -> updateStatus(mDefaultBackgroundColor,
                            R.string.disconnected);
                    case INITIALIZED -> updateStatus(mDefaultBackgroundColor,
                            R.string.initialized);
                    case CONNECTING -> updateStatus(mDefaultBackgroundColor,
                            R.string.connecting, status.remoteDeviceName);
                    case CONNECTED -> updateStatus(Color.GREEN, R.string.connected,
                            status.remoteDeviceName);
                    case ERROR -> updateStatus(Color.RED, R.string.error, status.errorMessage);
                }
            };

    public ConnectivityFragment() {
        super(R.layout.connectivity_fragment);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        mStatus = requireActivity().requireViewById(R.id.connection_status);

        TypedValue background = new TypedValue();
        requireActivity()
                .getTheme()
                .resolveAttribute(android.R.attr.windowBackground, background, true);
        mDefaultBackgroundColor = background.isColorType() ? background.data : Color.WHITE;

        CharSequence currentTitle = requireActivity().getTitle();
        String localEndpointId = ConnectionManager.getLocalEndpointId();
        String title =
                requireActivity().getString(R.string.this_device, currentTitle, localEndpointId);
        requireActivity().setTitle(title);

        mConnectionCallback.accept(mConnectionManager.getConnectionStatus());
        mConnectionManager.addConnectionCallback(mConnectionCallback);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mConnectionManager.removeConnectionCallback(mConnectionCallback);
    }

    private void updateStatus(int backgroundColor, int resId, Object... formatArgs) {
        Activity activity = requireActivity();
        activity.runOnUiThread(() -> {
            mStatus.setText(activity.getString(resId, formatArgs));
            mStatus.setBackgroundColor(backgroundColor);
        });
    }
}
