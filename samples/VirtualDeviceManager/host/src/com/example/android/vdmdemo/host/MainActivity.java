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

    private VdmService vdmService = null;
    private GridView launcher = null;
    private Button homeDisplayButton = null;
    private Button mirrorDisplayButton = null;

    private final ServiceConnection connection =
            new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    Log.i(TAG, "Connected to VDM Service");
                    vdmService = ((VdmService.LocalBinder) binder).getService();
                    connectionManager.startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    Log.i(TAG, "Disconnected from VDM Service");
                    vdmService = null;
                }
            };

    private final ConnectionManager.ConnectionCallback connectionCallback =
            new ConnectionManager.ConnectionCallback() {
                @Override
                public void onBandwidthChanged(
                        String remoteDeviceName, BandwidthInfo bandwidthInfo) {
                    updateLauncherVisibility(bandwidthInfo.getQuality());
                }

                @Override
                public void onConnecting(String remoteDeviceName) {
                    connectionManager.stopDiscovery();
                }

                @Override
                public void onDisconnected() {
                    updateLauncherVisibility(BandwidthInfo.Quality.UNKNOWN);
                    connectionManager.startDiscovery();
                }
            };

    @Inject ConnectionManager connectionManager;
    @Inject Settings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.main_tool_bar);
        toolbar.setOverflowIcon(getDrawable(R.drawable.settings));
        setSupportActionBar(toolbar);

        homeDisplayButton = requireViewById(R.id.create_home_display);
        mirrorDisplayButton = requireViewById(R.id.create_mirror_display);
        launcher = requireViewById(R.id.app_grid);
        launcher.setVisibility(View.GONE);
        LauncherAdapter launcherAdapter = new LauncherAdapter(getPackageManager());
        launcher.setAdapter(launcherAdapter);
        launcher.setOnItemClickListener(
                (parent, v, position, id) -> {
                    Intent intent = launcherAdapter.createPendingRemoteIntent(position);
                    if (intent == null || vdmService == null) {
                        return;
                    }
                    int[] remoteDisplayIds = vdmService.getRemoteDisplayIds();
                    if (settings.immersiveMode && remoteDisplayIds.length > 0) {
                        vdmService.startIntentOnDisplayIndex(intent, 0);
                    } else {
                        vdmService.startStreaming(intent);
                    }
                });
        launcher.setOnItemLongClickListener(
                (parent, v, position, id) -> {
                    Intent intent = launcherAdapter.createPendingRemoteIntent(position);
                    if (intent == null || vdmService == null) {
                        return true;
                    }
                    int[] remoteDisplayIds = vdmService.getRemoteDisplayIds();
                    if (remoteDisplayIds.length == 0) {
                        vdmService.startStreaming(intent);
                    } else if (settings.immersiveMode) {
                        vdmService.startIntentOnDisplayIndex(intent, 0);
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
                                        vdmService.startStreaming(intent);
                                    } else {
                                        vdmService.startIntentOnDisplayIndex(intent, which);
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
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        ConnectionManager.ConnectionStatus connectionStatus =
                connectionManager.getConnectionStatus();
        updateLauncherVisibility(
                connectionStatus.bandwidthInfo != null
                        ? connectionStatus.bandwidthInfo.getQuality()
                        : BandwidthInfo.Quality.UNKNOWN);
        connectionManager.addConnectionCallback(connectionCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        connectionManager.stopDiscovery();
        unbindService(connection);
        connectionManager.removeConnectionCallback(connectionCallback);
    }

    private void updateLauncherVisibility(@BandwidthInfo.Quality int quality) {
        runOnUiThread(
                () -> {
                    int visibility =
                            quality == BandwidthInfo.Quality.HIGH ? View.VISIBLE : View.GONE;
                    if (launcher != null) {
                        launcher.setVisibility(visibility);
                    }
                    if (homeDisplayButton != null) {
                        homeDisplayButton.setVisibility(visibility);
                    }
                    if (mirrorDisplayButton != null) {
                        mirrorDisplayButton.setVisibility(visibility);
                    }
                });
    }

    public void onCreateHomeDisplay(View view) {
        vdmService.startStreamingHome();
    }

    public void onCreateMirrorDisplay(View view) {
        vdmService.startMirroring();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                case R.id.enable_sensors:
                    item.setChecked(settings.sensorsEnabled);
                    break;
                case R.id.enable_audio:
                    item.setChecked(settings.audioEnabled);
                    break;
                case R.id.enable_recents:
                    item.setChecked(settings.includeInRecents);
                    break;
                case R.id.enable_clipboard:
                    item.setChecked(settings.crossDeviceClipboardEnabled);
                    break;
                case R.id.enable_rotation:
                    item.setChecked(settings.displayRotationEnabled);
                    break;
                case R.id.always_unlocked:
                    item.setChecked(settings.alwaysUnlocked);
                    break;
                case R.id.use_device_streaming:
                    item.setChecked(settings.deviceStreaming);
                    break;
                case R.id.show_pointer_icon:
                    item.setChecked(settings.showPointerIcon);
                    break;
                case R.id.immersive_mode:
                    item.setChecked(settings.immersiveMode);
                    break;
                case R.id.record_encoder_output:
                    item.setChecked(settings.recordEncoderOutput);
                    break;
                case R.id.custom_home:
                    item.setChecked(settings.customHome);
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
                vdmService.setSensorsEnabled(item.isChecked());
                return true;
            case R.id.enable_audio:
                vdmService.setAudioEnabled(item.isChecked());
                return true;
            case R.id.enable_recents:
                vdmService.setIncludeInRecents(item.isChecked());
                return true;
            case R.id.enable_clipboard:
                vdmService.setCrossDeviceClipboardEnabled(item.isChecked());
                return true;
            case R.id.enable_rotation:
                vdmService.setDisplayRotationEnabled(item.isChecked());
                return true;
            case R.id.always_unlocked:
                vdmService.setAlwaysUnlocked(item.isChecked());
                return true;
            case R.id.use_device_streaming:
                vdmService.setDeviceStreaming(item.isChecked());
                return true;
            case R.id.record_encoder_output:
                vdmService.setRecordEncoderOutput(item.isChecked());
                return true;
            case R.id.show_pointer_icon:
                vdmService.setShowPointerIcon(item.isChecked());
                return true;
            case R.id.immersive_mode:
                vdmService.setImmersiveMode(item.isChecked());
                return true;
            case R.id.custom_home:
                vdmService.setCustomHome(item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
