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

package com.example.android.vdmdemo.client;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.example.android.vdmdemo.common.RemoteEventProto;
import com.example.android.vdmdemo.common.RemoteIo;
import com.example.android.vdmdemo.common.VideoManager;

import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ActivityScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.inject.Inject;

@ActivityScoped
final class VirtualCameraController implements AutoCloseable {
    private static final String TAG = VirtualCameraController.class.getSimpleName();

    private static final String NO_CAMERA_PERMISSION_LOG_MSG =
            "VDM Client app doesn't have access to camera";

    private static final int CAMERA_WIDTH = 640;
    private static final int CAMERA_HEIGHT = 480;
    private static final int FPS = 30;

    private final RemoteIo mRemoteIo;
    private final Consumer<RemoteEventProto.RemoteEvent> mRemoteEventConsumer =
            this::processRemoteEvent;
    private final CameraManager mCameraManager;

    private final ArrayList<String> mExposedCameras = new ArrayList<>(2);

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final ArrayMap<String, CameraStreamer> mCameraStreamerMap = new ArrayMap<>();

    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    @Inject
    VirtualCameraController(@ApplicationContext Context context, RemoteIo remoteIo) {
        mRemoteIo = remoteIo;
        mRemoteIo.addMessageConsumer(mRemoteEventConsumer);
        mCameraManager = Objects.requireNonNull(context.getSystemService(CameraManager.class));

        try {
            getFrontCamera().ifPresent(mExposedCameras::add);
            getBackCamera().ifPresent(mExposedCameras::add);
        } catch (CameraAccessException exception) {
            Log.e(TAG, NO_CAMERA_PERMISSION_LOG_MSG, exception);
        }
    }

    List<RemoteEventProto.CameraCapabilities> getCameraCapabilities() {
        ArrayList<RemoteEventProto.CameraCapabilities> cameraCapabilities = new ArrayList<>(
                mExposedCameras.size());
        try {
            for (String cameraId : mExposedCameras) {
                cameraCapabilities.add(getCapabilitiesForCamera(cameraId));
            }
        } catch (CameraAccessException exception) {
            Log.e(TAG, "VDM Client doesn't have camera permission", exception);
        }
        Log.d(TAG, "Camera capabilities" + cameraCapabilities);
        return cameraCapabilities;
    }

    Optional<String> getFrontCamera() throws CameraAccessException {
        return getCameraWithLensFacing(CameraCharacteristics.LENS_FACING_FRONT);
    }

    Optional<String> getBackCamera() throws CameraAccessException {
        return getCameraWithLensFacing(CameraCharacteristics.LENS_FACING_BACK);
    }

    Optional<String> getCameraWithLensFacing(int lensFacing) throws CameraAccessException {
        for (String cameraId : mCameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(
                    cameraId);

            if (Objects.equals(characteristics.get(CameraCharacteristics.LENS_FACING),
                    lensFacing)) {
                return Optional.of(cameraId);
            }
        }
        return Optional.empty();
    }

    RemoteEventProto.CameraCapabilities getCapabilitiesForCamera(String cameraId)
            throws CameraAccessException {
        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);


        return RemoteEventProto.CameraCapabilities.newBuilder()
                .setCameraId(cameraId)
                .setWidth(CAMERA_WIDTH)
                .setHeight(CAMERA_HEIGHT)
                .setFps(FPS)
                .setLensFacing(Objects.requireNonNull(
                        characteristics.get(
                                CameraCharacteristics.LENS_FACING)))
                .setSensorOrientation(Objects.requireNonNull(
                        characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)))
                .build();
    }

    private void processRemoteEvent(RemoteEventProto.RemoteEvent remoteEvent) {
        try {
            if (remoteEvent.hasStartCameraStream()) {
                startCameraStream(remoteEvent.getStartCameraStream().getCameraId());
            }
            if (remoteEvent.hasStopCameraStream()) {
                stopCameraStream(remoteEvent.getStopCameraStream().getCameraId());
            }
        } catch (CameraAccessException exception) {
            Log.e(TAG, NO_CAMERA_PERMISSION_LOG_MSG, exception);
        }
    }

    private void startCameraStream(String cameraId) throws CameraAccessException {
        Log.d(TAG, "Start camera stream " + cameraId);
        synchronized (mLock) {
            if (mCameraStreamerMap.containsKey(cameraId)) {
                return;
            }
            CameraStreamer cameraStreamer = new CameraStreamer(cameraId);
            mCameraStreamerMap.put(cameraId, cameraStreamer);
            cameraStreamer.start();
        }
    }

    private void stopCameraStream(String cameraId) {
        Log.d(TAG, "Stop camera stream " + cameraId);
        synchronized (mLock) {
            if (!mCameraStreamerMap.containsKey(cameraId)) {
                return;
            }

            CameraStreamer streamer = Objects.requireNonNull(mCameraStreamerMap.get(cameraId));
            streamer.stop();
            mCameraStreamerMap.remove(cameraId);
        }
    }

    @Override
    public void close() throws Exception {
        mRemoteIo.removeMessageConsumer(mRemoteEventConsumer);
        synchronized (mLock) {
            mCameraStreamerMap.values().forEach(CameraStreamer::stop);
            mCameraStreamerMap.clear();
        }
    }

    private class CameraStreamer {
        private final String mCameraId;
        private final VideoManager mVideoManager;

        private final Object mLock = new Object();

        @GuardedBy("mLock")
        private CameraDevice mCameraDevice;

        @GuardedBy("mLock")
        private Surface mSurface;

        @GuardedBy("mLock")
        private CameraCaptureSession mCameraCaptureSession;

        private final CameraDevice.StateCallback mDeviceStateCallback =
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        synchronized (mLock) {
                            mCameraDevice = camera;
                            mSurface = mVideoManager.createInputSurface(CAMERA_WIDTH, CAMERA_HEIGHT,
                                    FPS);
                            mVideoManager.startEncoding();

                            OutputConfiguration outputConfiguration = new OutputConfiguration(
                                    mSurface);
                            SessionConfiguration sessionConfig = new SessionConfiguration(
                                    SessionConfiguration.SESSION_REGULAR,
                                    List.of(outputConfiguration), mExecutor, mSessionStateCallback);
                            try {
                                camera.createCaptureSession(sessionConfig);
                            } catch (CameraAccessException exception) {
                                Log.e(TAG, NO_CAMERA_PERMISSION_LOG_MSG, exception);
                            }
                        }
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        Log.d(TAG, "Camera device " + mCameraId + " disconnected");
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        Log.e(TAG, "Camera device " + mCameraId + " error: " + error);
                    }
                };

        private final CameraCaptureSession.StateCallback mSessionStateCallback =
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        synchronized (mLock) {
                            try {
                                mCameraCaptureSession = session;
                                CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(
                                        CameraDevice.TEMPLATE_PREVIEW);
                                builder.addTarget(mSurface);

                                session.setSingleRepeatingRequest(builder.build(), mExecutor,
                                        mCaptureCallback);
                            } catch (CameraAccessException exception) {
                                Log.e(TAG, NO_CAMERA_PERMISSION_LOG_MSG, exception);
                            }
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "Configuration failed!");
                    }
                };

        private final CameraCaptureSession.CaptureCallback mCaptureCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Log.d(TAG, "onCaptureCompleted");
                    }
                };

        CameraStreamer(String cameraId) {
            mCameraId = cameraId;
            mVideoManager = VideoManager.createCameraEncoder(cameraId, mRemoteIo, false);
        }

        void start() throws CameraAccessException {
            mCameraManager.openCamera(mCameraId, mExecutor, mDeviceStateCallback);
        }

        void stop() {
            synchronized (mLock) {
                try {
                    mVideoManager.stop();

                    if (mCameraCaptureSession != null) {
                        mCameraCaptureSession.stopRepeating();
                        mCameraCaptureSession.close();
                    }

                    if (mCameraDevice != null) {
                        mCameraDevice.close();
                    }
                } catch (CameraAccessException exception) {
                    Log.e(TAG, NO_CAMERA_PERMISSION_LOG_MSG, exception);
                }
            }
        }
    }
}
