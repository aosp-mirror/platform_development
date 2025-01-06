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

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.function.Consumer;

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
    private LauncherAdapter mLauncherAdapter = null;
    private final Consumer<Boolean> mVirtualDeviceListener = this::updateLauncherVisibility;

    private final ActivityResultLauncher<String> mRequestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                // no-op
            });

    private final ActivityResultLauncher<String> mVdmServicePermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (mVdmService == null) {
                    attemptStartVdmService();
                }
            });

    private final ServiceConnection mServiceConnection =
            new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    Log.i(TAG, "Connected to VDM Service");
                    mVdmService = ((VdmService.LocalBinder) binder).getService();
                    mVdmService.addVirtualDeviceListener(mVirtualDeviceListener);
                    updateLauncherVisibility(mVdmService.isVirtualDeviceActive());
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    Log.i(TAG, "Disconnected from VDM Service");
                    mVdmService = null;
                }
            };

    @Inject
    PreferenceController mPreferenceController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = requireViewById(R.id.main_tool_bar);
        setSupportActionBar(toolbar);

        mHomeDisplayButton = requireViewById(R.id.create_home_display);
        mHomeDisplayButton.setVisibility(View.GONE);
        mMirrorDisplayButton = requireViewById(R.id.create_mirror_display);
        mMirrorDisplayButton.setVisibility(View.GONE);

        mLauncherAdapter = new LauncherAdapter(this, mPreferenceController);
        mLauncher = requireViewById(R.id.app_grid);
        mLauncher.setVisibility(View.GONE);
        mLauncher.setAdapter(mLauncherAdapter);
        mLauncher.setOnItemClickListener(
                (parent, v, position, id) -> {
                    Intent intent = mLauncherAdapter.createPendingRemoteIntent(position);
                    if (intent == null || mVdmService == null) {
                        return;
                    }
                    mVdmService.startStreaming(intent);
                });
        mLauncher.setOnItemLongClickListener(
                (parent, v, position, id) -> {
                    Intent intent = mLauncherAdapter.createPendingRemoteIntent(position);
                    if (intent == null || mVdmService == null) {
                        return true;
                    }
                    int[] remoteDisplayIds = mVdmService.getRemoteDisplayIds();
                    if (remoteDisplayIds.length == 0) {
                        mVdmService.startStreaming(intent);
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
        attemptStartVdmService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            mRequestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mVdmService != null) {
            mVdmService.removeVirtualDeviceListener(mVirtualDeviceListener);
        }
        unbindService(mServiceConnection);
    }

    private void attemptStartVdmService() {
        // Need NEARBY_WIFI_DEVICES for WifiAware
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                == PackageManager.PERMISSION_DENIED) {
            mVdmServicePermissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES);
            return;
        }

        // Need POST_NOTIFICATIONS for the foreground Service Notification
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_DENIED) {
            mVdmServicePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        Log.i(TAG, "Starting Vdm Host service");
        Intent intent = new Intent(this, VdmService.class);
        startForegroundService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void updateLauncherVisibility(boolean virtualDeviceActive) {
        final int visibility = virtualDeviceActive ? View.VISIBLE : View.GONE;
        runOnUiThread(
                () -> {
                    if (mLauncher != null) {
                        mLauncherAdapter.update();
                        if (mPreferenceController.getBoolean(
                                R.string.internal_pref_home_displays_supported)) {
                            mLauncher.setVisibility(visibility);
                        } else {
                            mLauncher.setVisibility(View.GONE);
                        }
                    }
                    if (mHomeDisplayButton != null) {
                        mHomeDisplayButton.setEnabled(
                                mPreferenceController.getBoolean(
                                        R.string.internal_pref_home_displays_supported));
                        mHomeDisplayButton.setVisibility(visibility);
                    }
                    if (mMirrorDisplayButton != null) {
                        mMirrorDisplayButton.setEnabled(
                                VdmCompat.isMirrorDisplaySupported(this, mPreferenceController));
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
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.input:
                startActivity(new Intent(this, InputActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
