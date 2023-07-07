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

package com.example.android.voiceinteractor;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PersistableBundle;
import android.os.SharedMemory;
import android.os.SystemClock;
import android.service.voice.VisualQueryDetectionService;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;


/**
 * Sample VisualQueryDetectionService that captures camera frame and sends back partial query.
 */
public class SampleVisualQueryDetectionService extends VisualQueryDetectionService {
    static final String TAG = "SVisualQueryDetectionSrvc";

    private final String FAKE_QUERY = "What is the weather today?";

    // Camera module related variables
    // Set this to different values for different modes
    private final int CAPTURE_MODE = CameraDevice.TEMPLATE_RECORD;
    private String mCameraId;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private Handler mCameraBackgroundHandler;
    private HandlerThread mCameraBackgroundThread;

    // audio module related variables
    private static final int AUDIO_SAMPLE_RATE_IN_HZ = 16000;
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_DEFAULT;
    private static final int BUFFER_SIZE = AUDIO_SAMPLE_RATE_IN_HZ;
    private AudioRecord mAudioRecord;
    private Handler mAudioBackgroundHandler;
    private HandlerThread mAudioBackgroundThread;


    @Override
    public void onStartDetection() {
        Log.i(TAG, "onStartDetection");
        startBackgroundThread();
        openCamera();
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                AUDIO_SAMPLE_RATE_IN_HZ, AUDIO_CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
    }

    @Override
    public void onStopDetection() {
        Log.i(TAG, "onStopDetection");
        releaseResources();
        stopBackgroundThread();
    }

    @Override
    public void onUpdateState(@Nullable PersistableBundle options,
            @Nullable SharedMemory sharedMemory, long callbackTimeoutMillis,
            @Nullable IntConsumer statusCallback) {
        Log.i(TAG, "onUpdateState");
        if (statusCallback != null) {
            statusCallback.accept(0);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying visual query detection service");
        onStopDetection();
    }

    /* Main logics of the system */
    private void onReceiveImage(ImageReader reader){
        Log.i(TAG, "Image received.");
        Image image = null;
        try {
            image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            // Camera frame triggers attention
            Log.i(TAG, "Image bytes received: " + Arrays.toString(bytes));
            gainedAttention();
            openMicrophone();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (image != null) {
                image.close();
            }
        }
        SystemClock.sleep(2_000); // wait 2 second to turn off attention
        closeMicrophone();
        lostAttention();
    }

    private void onReceiveAudio(){
        try {
            byte[] bytes = new byte[BUFFER_SIZE];
            int result = mAudioRecord.read(bytes, 0, BUFFER_SIZE);
            if (result != AudioRecord.ERROR_INVALID_OPERATION) {
                // The buffer can be all zeros due to initialization and reading delay
                Log.i(TAG, "Audio bytes received: " + Arrays.toString(bytes));
                streamQuery(FAKE_QUERY);
                finishQuery();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SystemClock.sleep(2_000); //sleep so the buffer is a stable value
    }

    /* Sample Camera Module */
    private void openCamera() {
        CameraManager manager = getSystemService(CameraManager.class);
        Log.i(TAG, "Attempting to open camera");
        try {
            mCameraId = manager.getCameraIdList()[0]; //get front facing camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            Size imageSize = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG)[0];
            initializeImageReader(imageSize.getWidth(), imageSize.getHeight());
            manager.openCamera(mCameraId, stateCallback, mCameraBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Camera opened.");
    }

    private void initializeImageReader(int width, int height) {
        // Initialize image reader
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        ImageReader.OnImageAvailableListener readerListener =
                new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                onReceiveImage(reader);
            }
        } ;
        mImageReader.setOnImageAvailableListener(readerListener, mCameraBackgroundHandler);
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            // This is called when the camera is open
            Log.i(TAG, "onCameraOpened");
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };

    private void createCameraPreview() {
        Range<Integer> fpsRange = new Range<>(1,2);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CAPTURE_MODE);
            Surface imageSurface = mImageReader.getSurface();
            mCaptureRequestBuilder.addTarget(imageSurface);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
            mCameraDevice.createCaptureSession(List.of(imageSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            //The camera is already closed
                            if (mCameraDevice == null) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            mCameraCaptureSession = cameraCaptureSession;
                            updatePreview();
                            Log.i(TAG, "Capture session configured.");
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            //No-op
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Camera preview created.");
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            if (CAPTURE_MODE == CameraDevice.TEMPLATE_STILL_CAPTURE
                    || CAPTURE_MODE == CameraDevice.TEMPLATE_VIDEO_SNAPSHOT) {
                mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), null,
                        mCameraBackgroundHandler);
            } else if (CAPTURE_MODE == CameraDevice.TEMPLATE_RECORD
                    || CAPTURE_MODE == CameraDevice.TEMPLATE_PREVIEW){
                mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null,
                        mCameraBackgroundHandler);
            } else {
                throw new IllegalStateException("Capture mode not supported.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Sample Microphone Module */
    private void openMicrophone() {
        mAudioRecord.startRecording();
        mAudioBackgroundHandler.post(this::onReceiveAudio);
    }

    private void closeMicrophone() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
        }
    }

    private void releaseResources() {
        mCameraId = null;
        mCaptureRequestBuilder = null;
        // Release mCameraCaptureSession
        mCameraCaptureSession.close();
        mCameraCaptureSession = null;
        // Release mCameraDevice
        mCameraDevice.close();
        mCameraDevice = null;
        // Release mImageReader
        mImageReader.close();
        mImageReader = null;
        // Release mAudioRecord
        mAudioRecord.release();
        mAudioRecord = null;
    }
    // Handlers
    private void startBackgroundThread() {
        mCameraBackgroundThread = new HandlerThread("Camera Background Thread");
        mCameraBackgroundThread.start();
        mCameraBackgroundHandler = new Handler(mCameraBackgroundThread.getLooper());
        mAudioBackgroundThread = new HandlerThread("Audio Background Thread");
        mAudioBackgroundThread.start();
        mAudioBackgroundHandler = new Handler(mAudioBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mCameraBackgroundThread.quitSafely();
        try {
            mCameraBackgroundThread.join();
            mCameraBackgroundThread = null;
            mCameraBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to stop camera thread.");
        }
        mAudioBackgroundThread.quitSafely();
        try {
            mAudioBackgroundThread.join();
            mAudioBackgroundThread = null;
            mAudioBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to stop audio thread.");
        }
    }
}