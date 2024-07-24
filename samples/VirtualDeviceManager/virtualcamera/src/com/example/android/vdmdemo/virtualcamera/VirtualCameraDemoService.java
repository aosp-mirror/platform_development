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

import static android.companion.virtual.VirtualDeviceParams.DEVICE_POLICY_CUSTOM;
import static android.companion.virtual.VirtualDeviceParams.POLICY_TYPE_CAMERA;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.role.RoleManager;
import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.CompanionDeviceManager;
import android.companion.virtual.VirtualDeviceManager;
import android.companion.virtual.VirtualDeviceParams;
import android.companion.virtual.camera.VirtualCamera;
import android.companion.virtual.camera.VirtualCameraCallback;
import android.companion.virtual.camera.VirtualCameraConfig;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.ImageFormat;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Virtual camera service. */
@AndroidEntryPoint(Service.class)
public final class VirtualCameraDemoService extends Hilt_VirtualCameraDemoService {
    public static final String TAG = VirtualCameraDemoService.class.getSimpleName();

    private static final String CHANNEL_ID =
            "com.example.android.vdmdemo.virtualcamera.VirtualCameraDemoService";

    private static final String ACTION_STOP =
            "com.example.android.vdmdemo.virtualcamera.VirtualCameraDemoService.STOP";

    private static final int NOTIFICATION_ID = 1;

    private static final String VIRTUAL_DEVICE_NAME = "VirtualDevice - Virtual Camera demo";

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final ArrayList<VirtualCamera> mVirtualCameras = new ArrayList<>();

    /** Provides an instance of this service to bound clients. */
    public class LocalBinder extends Binder {
        VirtualCameraDemoService getService() {
            return VirtualCameraDemoService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    private VirtualDeviceManager.VirtualDevice mVirtualDevice = null;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Log.i(TAG, "Stopping VDM Camera Demo Service.");
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        NotificationChannel notificationChannel =
                new NotificationChannel(
                        CHANNEL_ID, "VDM Service Channel", NotificationManager.IMPORTANCE_LOW);
        notificationChannel.enableVibration(false);
        NotificationManager notificationManager = Objects.requireNonNull(
                getSystemService(NotificationManager.class));
        notificationManager.createNotificationChannel(notificationChannel);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntentOpen =
                PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent(this, VirtualCameraDemoService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingIntentStop =
                PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.connected)
                        .setContentTitle("Virtual camera running")
                        .setContentText("Click to open")
                        .setContentIntent(pendingIntentOpen)
                        .addAction(
                                new Notification.Action.Builder(
                                        R.drawable.close, "Stop", pendingIntentStop)
                                        .build())
                        .setOngoing(true)
                        .build();
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        associateAndCreateVirtualDevice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mVirtualDevice != null) {
            mVirtualDevice.close();
        }
    }

    private void associateAndCreateVirtualDevice() {
        CompanionDeviceManager cdm = getSystemService(CompanionDeviceManager.class);
        RoleManager rm = getSystemService(RoleManager.class);
        for (AssociationInfo associationInfo : cdm.getMyAssociations()) {
            // Flashing the device clears the role and the permissions, but not the CDM
            // associations.
            // TODO(b/290596625): Remove the workaround to clear the associations if the role is not
            // held.
            if (!rm.isRoleHeld(AssociationRequest.DEVICE_PROFILE_APP_STREAMING)) {
                cdm.disassociate(associationInfo.getId());
            } else if (Objects.equals(associationInfo.getPackageName(), getPackageName())
                    && Objects.equals(
                    associationInfo.getDisplayName().toString(),
                    VIRTUAL_DEVICE_NAME)) {
                createVirtualDevice(associationInfo);
                return;
            }
        }

        @SuppressLint("MissingPermission")
        AssociationRequest.Builder associationRequest =
                new AssociationRequest.Builder()
                        .setDeviceProfile(AssociationRequest.DEVICE_PROFILE_APP_STREAMING)
                        .setDisplayName(VIRTUAL_DEVICE_NAME)
                        .setSelfManaged(true);
        cdm.associate(
                associationRequest.build(),
                new CompanionDeviceManager.Callback() {
                    @Override
                    public void onAssociationPending(@NonNull IntentSender intentSender) {
                        try {
                            startIntentSender(intentSender, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(
                                    TAG,
                                    "onAssociationPending: Failed to send device selection intent",
                                    e);
                        }
                    }

                    @Override
                    public void onAssociationCreated(@NonNull AssociationInfo associationInfo) {
                        Log.i(TAG, "onAssociationCreated: ID " + associationInfo.getId());
                        createVirtualDevice(associationInfo);
                    }

                    @Override
                    public void onFailure(CharSequence error) {
                        Log.e(TAG, "onFailure: RemoteDevice Association failed " + error);
                    }
                },
                null);
        Log.i(TAG, "createCdmAssociation: Waiting for association to happen");
    }

    private void createVirtualDevice(AssociationInfo associationInfo) {
        VirtualDeviceParams.Builder virtualDeviceBuilder =
                new VirtualDeviceParams.Builder()
                        .setName(VIRTUAL_DEVICE_NAME)
                        .setDevicePolicy(POLICY_TYPE_CAMERA, DEVICE_POLICY_CUSTOM);

        VirtualDeviceManager vdm = getSystemService(VirtualDeviceManager.class);
        mVirtualDevice = Objects.requireNonNull(vdm).createVirtualDevice(associationInfo.getId(),
                virtualDeviceBuilder.build());
    }

    VirtualCamera createVirtualCamera(String name, VirtualCameraCallback cameraCallback) {
        if (mVirtualDevice == null) {
            Log.d(TAG, "No virtual device - cannot create camera");
        }

        VirtualCamera camera = mVirtualDevice.createVirtualCamera(
                new VirtualCameraConfig.Builder(name)
                        .addStreamConfig(640, 480, ImageFormat.YUV_420_888, 30)
                        .setLensFacing(LENS_FACING_FRONT)
                        .setVirtualCameraCallback(getMainExecutor(), cameraCallback)
                        .build());

        synchronized (mLock) {
            mVirtualCameras.add(camera);
        }

        return camera;
    }

    void removeCamera(VirtualCamera camera) {
        synchronized (mLock) {
            mVirtualCameras.remove(camera);
        }
    }

    List<VirtualCamera> getCameras() {
        synchronized (mLock) {
            return List.copyOf(mVirtualCameras);
        }
    }
}
