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
import android.graphics.ImageFormat;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.example.android.vdmdemo.common.RemoteEventProto;
import com.example.android.vdmdemo.common.RemoteIo;
import com.example.android.vdmdemo.common.VideoManager;
import com.google.common.util.concurrent.MoreExecutors;

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

    void createCameras(List<RemoteEventProto.CameraCapabilities> cameraCapabilities) {
        for (RemoteEventProto.CameraCapabilities capabilities : cameraCapabilities) {
            VirtualCameraConfig config = new VirtualCameraConfig.Builder(
                    "Remote camera " + capabilities.getCameraId())
                        .addStreamConfig(capabilities.getWidth(), capabilities.getHeight(),
                                            ImageFormat.YUV_420_888, capabilities.getFps())
                        .setVirtualCameraCallback(MoreExecutors.directExecutor(),
                                            new RemoteCamera(capabilities.getCameraId()))
                        .setLensFacing(capabilities.getLensFacing())
                        .setSensorOrientation(capabilities.getSensorOrientation()).build();
            VirtualCamera camera = mVirtualDevice.createVirtualCamera(config);
            mVirtualCameras.put(capabilities.getCameraId(), camera);

            Log.d(TAG, "Created virtual camera " + capabilities.getCameraId()
                        + "(" + camera.getConfig().getName() + ")");
        }
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
