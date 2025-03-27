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

package com.example.android.vdmdemo.host;

import android.annotation.SuppressLint;
import android.companion.virtual.VirtualDeviceManager;
import android.companion.virtual.camera.VirtualCamera;
import android.companion.virtual.camera.VirtualCameraCallback;
import android.companion.virtual.camera.VirtualCameraConfig;
import android.companion.virtualdevice.flags.Flags;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.example.android.vdmdemo.common.RemoteEventProto;
import com.example.android.vdmdemo.common.RemoteIo;
import com.example.android.vdmdemo.common.VideoManager;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressLint("NewApi")
final class RemoteCameraManager implements AutoCloseable {
    private static final String TAG = RemoteCameraManager.class.getSimpleName();

    private final RemoteIo mRemoteIo;
    private final VirtualDeviceManager.VirtualDevice mVirtualDevice;
    private final ArrayMap<String, VirtualCamera> mVirtualCameras = new ArrayMap<>();

    RemoteCameraManager(@NonNull VirtualDeviceManager.VirtualDevice virtualDevice,
            @NonNull RemoteIo remoteIo) {
        mVirtualDevice = Objects.requireNonNull(virtualDevice);
        mRemoteIo = Objects.requireNonNull(remoteIo);
    }

    void createCameras(List<RemoteEventProto.CameraCapabilities> cameraCapabilities,
            boolean duplicateFrontCamera, boolean duplicateBackCamera) {
        boolean supportExternal = VdmCompat.isAtLeastB() && Flags.externalVirtualCameras();
        for (RemoteEventProto.CameraCapabilities capabilities : cameraCapabilities) {
            // filter out external cameras if not supported
            int lensFacing = capabilities.getLensFacing();
            if (lensFacing == CameraCharacteristics.LENS_FACING_EXTERNAL && !supportExternal) {
                continue;
            }

            createVirtualCamera(capabilities.getCameraId(), lensFacing, capabilities);

            if (supportExternal) {
                if (duplicateFrontCamera && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    createVirtualCamera(capabilities.getCameraId(),
                            CameraCharacteristics.LENS_FACING_EXTERNAL, capabilities);
                }

                if (duplicateBackCamera && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    createVirtualCamera(capabilities.getCameraId(),
                            CameraCharacteristics.LENS_FACING_EXTERNAL, capabilities);
                }
            }
        }

        try {
            final CameraManager cameraManager = mVirtualDevice.createContext().getSystemService(
                    CameraManager.class);
            if (cameraManager != null) {
                Log.d(TAG, "CameraManager on deviceId: " + mVirtualDevice.getDeviceId()
                        + " has available camera ids: " + Arrays.toString(
                        cameraManager.getCameraIdList()));
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Exception getting the list of camera ids: " + e);
        }
    }

    private void createVirtualCamera(String cameraId, int lensFacing,
            RemoteEventProto.CameraCapabilities capabilities) {
        final String lensFacingText = switch (lensFacing) {
            case CameraCharacteristics.LENS_FACING_FRONT -> "front";
            case CameraCharacteristics.LENS_FACING_BACK -> "back";
            case CameraCharacteristics.LENS_FACING_EXTERNAL -> "external";
            default -> "unknown";
        };
        @SuppressLint("WrongConstant")
        VirtualCameraConfig config = new VirtualCameraConfig.Builder(
                "Remote " + lensFacingText + " camera " + cameraId)
                .addStreamConfig(capabilities.getWidth(), capabilities.getHeight(),
                        ImageFormat.YUV_420_888, capabilities.getFps())
                .setVirtualCameraCallback(MoreExecutors.directExecutor(),
                        new RemoteCamera(cameraId))
                .setLensFacing(lensFacing)
                .setSensorOrientation(capabilities.getSensorOrientation())
                .build();
        VirtualCamera camera = mVirtualDevice.createVirtualCamera(config);
        mVirtualCameras.put(cameraId, camera);

        Log.d(TAG, "Created virtual camera from client cameraId: " + cameraId
                + " with name: " + camera.getConfig().getName()
                + " and lensFacing: " + lensFacing);
    }

    @Override
    public void close() {
        for (int i = 0; i < mVirtualCameras.size(); ++i) {
            mVirtualCameras.valueAt(i).close();
        }
    }


    private class RemoteCamera implements VirtualCameraCallback {
        private final String mRemoteCameraId;

        private final Object mLock = new Object();

        @GuardedBy("mLock")
        private VideoManager mVideoManager;

        RemoteCamera(String remoteCameraId) {
            mRemoteCameraId = remoteCameraId;
        }


        @Override
        public void onStreamConfigured(int streamId, @NonNull Surface surface, int width,
                int height, int format) {
            Log.d(TAG, "onStreamConfigured " + width + " x " + height);
            synchronized (mLock) {
                if (mVideoManager != null) {
                    mVideoManager.stop();
                }
                mVideoManager = VideoManager.createCameraDecoder(mRemoteCameraId, mRemoteIo);
                mVideoManager.startDecoding(surface, width, height);
            }

            mRemoteIo.sendMessage(RemoteEventProto.RemoteEvent.newBuilder()
                    .setStartCameraStream(
                            RemoteEventProto.StartCameraStream.newBuilder().setCameraId(
                                    mRemoteCameraId))
                    .build());
        }

        @Override
        public void onStreamClosed(int streamId) {
            Log.d(TAG, "onStreamClosed " + streamId);

            synchronized (mLock) {
                if (mVideoManager != null) {
                    mVideoManager.stop();
                    mVideoManager = null;
                }
            }

            mRemoteIo.sendMessage(RemoteEventProto.RemoteEvent.newBuilder()
                    .setStopCameraStream(RemoteEventProto.StopCameraStream.newBuilder().setCameraId(
                            mRemoteCameraId))
                    .build());
        }
    }
}
