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

package com.example.android.vdmdemo.host;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.android.vdmdemo.common.ConnectionManager;
import com.google.android.gms.nearby.connection.BandwidthInfo;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/**
 * VDM Host activity, streaming apps to a remote device and processing the input coming from there.
 */
@AndroidEntryPoint(AppCompatActivity.class)
public class MainActivity extends Hilt_MainActivity {
    public static final String TAG = "VdmHost";

    private VdmService mVdmService = null;
    private GridView mLauncher = null;
    private Button mHomeDisplayButton = null;
    private Button mMirrorDisplayButton = null;

    private final ServiceConnection mServiceConnection =
            new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    Log.i(TAG, "Connected to VDM Service");
                    mVdmService = ((VdmService.LocalBinder) binder).getService();
                    mConnectionManager.startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    Log.i(TAG, "Disconnected from VDM Service");
                    mVdmService = null;
                }
            };

    private final ConnectionManager.ConnectionCallback mConnectionCallback =
            new ConnectionManager.ConnectionCallback() {
                @Override
                public void onBandwidthChanged(
                        String remoteDeviceName, BandwidthInfo bandwidthInfo) {
                    updateLauncherVisibility(bandwidthInfo.getQuality());
                }

                @Override
                public void onConnecting(String remoteDeviceName) {
                    mConnectionManager.stopDiscovery();
                }

                @Override
                public void onDisconnected() {
                    updateLauncherVisibility(BandwidthInfo.Quality.UNKNOWN);
                    mConnectionManager.startDiscovery();
                }
            };

    @Inject ConnectionManager mConnectionManager;
    @Inject Settings mSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.main_tool_bar);
        toolbar.setOverflowIcon(getDrawable(R.drawable.settings));
        setSupportActionBar(toolbar);

        mHomeDisplayButton = requireViewById(R.id.create_home_display);
        mMirrorDisplayButton = requireViewById(R.id.create_mirror_display);
        mLauncher = requireViewById(R.id.app_grid);
        mLauncher.setVisibility(View.GONE);
        LauncherAdapter launcherAdapter = new LauncherAdapter(getPackageManager());
        mLauncher.setAdapter(launcherAdapter);
        mLauncher.setOnItemClickListener(
                (parent, v, position, id) -> {
                    Intent intent = launcherAdapter.createPendingRemoteIntent(position);
                    if (intent == null || mVdmService == null) {
                        return;
                    }
                    int[] remoteDisplayIds = mVdmService.getRemoteDisplayIds();
                    if (mSettings.immersiveMode && remoteDisplayIds.length > 0) {
                        mVdmService.startIntentOnDisplayIndex(intent, 0);
                    } else {
                        mVdmService.startStreaming(intent);
                    }
                });
        mLauncher.setOnItemLongClickListener(
                (parent, v, position, id) -> {
                    Intent intent = launcherAdapter.createPendingRemoteIntent(position);
                    if (intent == null || mVdmService == null) {
                        return true;
                    }
                    int[] remoteDisplayIds = mVdmService.getRemoteDisplayIds();
                    if (remoteDisplayIds.length == 0) {
                        mVdmService.startStreaming(intent);
                    } else if (mSettings.immersiveMode) {
                        mVdmService.startIntentOnDisplayIndex(intent, 0);
                    } else {
                        String[] displays = new String[remoteDisplayIds.length + 1];
                        for (int i = 0; i < remoteDisplayIds.length; ++i) {
                            displays[i] = "Display " + remoteDisplayIds[i];
                        }
                        displays[remoteDisplayIds.length] = "New display";
                        AlertDialog.Builder alertDialogBuilder =
                                new AlertDialog.Builder(MainActivity.this);
                        alertDialogBuilder.setTitle("Choose display");
                        alertDialogBuilder.setItems(
                                displays,
                                (dialog, which) -> {
                                    if (which == remoteDisplayIds.length) {
                                        mVdmService.startStreaming(intent);
                                    } else {
                                        mVdmService.startIntentOnDisplayIndex(intent, which);
                                    }
                                });
                        alertDialogBuilder.show();
                    }
                    return true;
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, VdmService.class);
        startForegroundService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        ConnectionManager.ConnectionStatus connectionStatus =
                mConnectionManager.getConnectionStatus();
        updateLauncherVisibility(
                connectionStatus.bandwidthInfo != null
                        ? connectionStatus.bandwidthInfo.getQuality()
                        : BandwidthInfo.Quality.UNKNOWN);
        mConnectionManager.addConnectionCallback(mConnectionCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mConnectionManager.stopDiscovery();
        unbindService(mServiceConnection);
        mConnectionManager.removeConnectionCallback(mConnectionCallback);
    }

    private void updateLauncherVisibility(@BandwidthInfo.Quality int quality) {
        runOnUiThread(
                () -> {
                    int visibility =
                            quality == BandwidthInfo.Quality.HIGH ? View.VISIBLE : View.GONE;
                    if (mLauncher != null) {
                        mLauncher.setVisibility(visibility);
                    }
                    if (mHomeDisplayButton != null) {
                        mHomeDisplayButton.setVisibility(visibility);
                    }
                    if (mMirrorDisplayButton != null) {
                        mMirrorDisplayButton.setVisibility(visibility);
                    }
                });
    }

    /** Process a home display request. */
    public void onCreateHomeDisplay(View view) {
        mVdmService.startStreamingHome();
    }

    /** Process a mirror display request. */
    public void onCreateMirrorDisplay(View view) {
        mVdmService.startMirroring();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                case R.id.enable_sensors:
                    item.setChecked(mSettings.sensorsEnabled);
                    break;
                case R.id.enable_audio:
                    item.setChecked(mSettings.audioEnabled);
                    break;
                case R.id.enable_recents:
                    item.setChecked(mSettings.includeInRecents);
                    break;
                case R.id.enable_clipboard:
                    item.setChecked(mSettings.crossDeviceClipboardEnabled);
                    break;
                case R.id.enable_rotation:
                    item.setChecked(mSettings.displayRotationEnabled);
                    break;
                case R.id.always_unlocked:
                    item.setChecked(mSettings.alwaysUnlocked);
                    break;
                case R.id.use_device_streaming:
                    item.setChecked(mSettings.deviceStreaming);
                    break;
                case R.id.show_pointer_icon:
                    item.setChecked(mSettings.showPointerIcon);
                    break;
                case R.id.immersive_mode:
                    item.setChecked(mSettings.immersiveMode);
                    break;
                case R.id.record_encoder_output:
                    item.setChecked(mSettings.recordEncoderOutput);
                    break;
                case R.id.custom_home:
                    item.setChecked(mSettings.customHome);
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        item.setChecked(!item.isChecked());

        switch (item.getItemId()) {
            case R.id.enable_sensors:
                mVdmService.setSensorsEnabled(item.isChecked());
                return true;
            case R.id.enable_audio:
                mVdmService.setAudioEnabled(item.isChecked());
                return true;
            case R.id.enable_recents:
                mVdmService.setIncludeInRecents(item.isChecked());
                return true;
            case R.id.enable_clipboard:
                mVdmService.setCrossDeviceClipboardEnabled(item.isChecked());
                return true;
            case R.id.enable_rotation:
                mVdmService.setDisplayRotationEnabled(item.isChecked());
                return true;
            case R.id.always_unlocked:
                mVdmService.setAlwaysUnlocked(item.isChecked());
                return true;
            case R.id.use_device_streaming:
                mVdmService.setDeviceStreaming(item.isChecked());
                return true;
            case R.id.record_encoder_output:
                mVdmService.setRecordEncoderOutput(item.isChecked());
                return true;
            case R.id.show_pointer_icon:
                mVdmService.setShowPointerIcon(item.isChecked());
                return true;
            case R.id.immersive_mode:
                mVdmService.setImmersiveMode(item.isChecked());
                return true;
            case R.id.custom_home:
                mVdmService.setCustomHome(item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
