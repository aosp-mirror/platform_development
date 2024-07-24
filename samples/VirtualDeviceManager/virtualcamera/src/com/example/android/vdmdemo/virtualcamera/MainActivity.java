/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.example.android.vdmdemo.virtualcamera;

import android.companion.virtual.camera.VirtualCamera;
import android.companion.virtual.camera.VirtualCameraCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.Objects;

/** Main activity of virtual camera demo app. */
@AndroidEntryPoint(AppCompatActivity.class)
public class MainActivity extends Hilt_MainActivity {
    public static final String TAG = "VirtualCameraDemo";

    private VirtualCameraDemoService mVirtualCameraDemoService;
    private EditText mCameraNameEditText;
    private Button mCameraButton;
    private ListView mCamerasListView;
    private RadioGroup mCameraInputTypeRadioGroup;

    private VirtualCameraArrayAdapter mVirtualCameraArrayAdapter = null;

    private final ServiceConnection mServiceConnection =
            new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    Log.i(TAG, "Connected to Virtual Camera Demo Service");
                    mVirtualCameraDemoService =
                            ((VirtualCameraDemoService.LocalBinder) binder).getService();
                    mVirtualCameraArrayAdapter.setVirtualCameraDemoService(
                            mVirtualCameraDemoService);
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    Log.i(TAG, "Disconnected from Virtual Camera Demo Service");
                    mVirtualCameraDemoService = null;
                    mVirtualCameraArrayAdapter.setVirtualCameraDemoService(null);
                }
            };

    VirtualCameraCallback getCameraCallback() {
        int checkedId = mCameraInputTypeRadioGroup.getCheckedRadioButtonId();
        Log.d(TAG, "checkedId = " + checkedId);
        switch (checkedId) {
            case R.id.radio_canvas -> {
                return new CanvasVirtualCamera(mCameraNameEditText.getText().toString());
            }
            case R.id.radio_video_file -> {
                return new VideoFileVirtualCamera(getApplicationContext());
            }
        }
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mCameraNameEditText = Objects.requireNonNull(findViewById(R.id.camera_name_edit_text));
        mCameraButton = Objects.requireNonNull(findViewById(R.id.create_camera_button));
        mCameraButton.setOnClickListener(v -> {
            Log.d(TAG, "onClick");
            VirtualCameraCallback callback = getCameraCallback();
            if (callback == null) {
                Toast.makeText(this, "No camera input type selected", Toast.LENGTH_SHORT).show();
            } else {
                VirtualCamera virtualCamera = mVirtualCameraDemoService.createVirtualCamera(
                        mCameraNameEditText.getText().toString(),
                        callback);
                mVirtualCameraArrayAdapter.add(virtualCamera);
                Log.d(TAG, "Created virtual camera " + virtualCamera.getId());
            }
        });
        mCamerasListView = Objects.requireNonNull(findViewById(R.id.cameras_list_view));
        mVirtualCameraArrayAdapter = new VirtualCameraArrayAdapter(this);
        mCamerasListView.setAdapter(mVirtualCameraArrayAdapter);

        mCameraInputTypeRadioGroup = Objects.requireNonNull(
                findViewById(R.id.camera_type_radio_group));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        Intent intent = new Intent(this, VirtualCameraDemoService.class);
        startForegroundService(intent);
        Log.d(TAG, "Starting foreground service");
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        unbindService(mServiceConnection);
    }
}
