/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.service.voice.AlwaysOnHotwordDetector.STATE_HARDWARE_UNAVAILABLE;
import static android.service.voice.AlwaysOnHotwordDetector.STATE_KEYPHRASE_ENROLLED;
import static android.service.voice.AlwaysOnHotwordDetector.STATE_KEYPHRASE_UNENROLLED;

import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.AlwaysOnHotwordDetector.EventPayload;
import android.service.voice.HotwordDetector;
import android.service.voice.HotwordRejectedResult;
import android.service.voice.VoiceInteractionService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

public class SampleVoiceInteractionService extends VoiceInteractionService {
    private static final String TAG = "VIS";

    // Number of bytes per sample of audio (which is a short).
    private static final int BYTES_PER_SAMPLE = 2;
    public static final String KEYPHRASE = "X Android";

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        SampleVoiceInteractionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SampleVoiceInteractionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if ("local".equals(intent.getAction())) {
            return binder;
        }
        return super.onBind(intent);
    }

    HotwordDetector mDetector;
    Callback mCallback;

    Bundle mData = new Bundle();
    AudioFormat mAudioFormat;
    EventPayload mLastPayload;

    @Override
    public void onReady() {
        super.onReady();
        Log.i(TAG, "onReady");
        mCallback = new Callback();
        mDetector = createAlwaysOnHotwordDetector(KEYPHRASE, Locale.US, null, null, mCallback);
    }

    @Override
    public void onShutdown() {
        super.onShutdown();
        Log.i(TAG, "onShutdown");
    }

    class Callback extends AlwaysOnHotwordDetector.Callback {

        private boolean mAvailable = false;

        @Override
        public void onAvailabilityChanged(int status) {
            Log.i(TAG, "onAvailabilityChanged: " + status);
            if (status == STATE_HARDWARE_UNAVAILABLE) {
                // adb shell dumpsys package com.example.android.voiceinteractor | grep HOTWO
                Log.w(
                        TAG,
                        "Hotword hardware unavailable. You may need to pre-grant "
                                + "CAPTURE_AUDIO_HOTWORD to this app, grant record audio to the app"
                                + "in settings, and/or change the keyphrase "
                                + "to one supported by the device's default assistant.");
            }
            if (status == STATE_KEYPHRASE_UNENROLLED) {
                Intent enrollIntent = null;
                try {
                    enrollIntent = ((AlwaysOnHotwordDetector) mDetector).createEnrollIntent();
                } catch (HotwordDetector.IllegalDetectorStateException e) {
                    e.printStackTrace();
                }
                if (enrollIntent == null) {
                    Log.w(TAG, "No enroll intent found. Try enrolling the keyphrase using the"
                            + " device's default assistant.");
                    return;
                }
                ComponentName component = startForegroundService(enrollIntent);
                Log.i(TAG, "Start enroll intent: " + component);
            }
            if (status == STATE_KEYPHRASE_ENROLLED) {
                Log.i(TAG, "Keyphrase enrolled; ready to recognize.");
                mAvailable = true;
            }
        }

        @Override
        public void onRejected(@NonNull HotwordRejectedResult result) {
            try {
                mDetector.startRecognition();
            } catch (HotwordDetector.IllegalDetectorStateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDetected(@NonNull EventPayload eventPayload) {
            onDetected(eventPayload, false);
        }

        public void onDetected(@NonNull EventPayload eventPayload, boolean generateSessionId) {
            Log.i(TAG, "onDetected: " + eventPayload);
            Log.i(TAG, "minBufferSize: "
                    + AudioRecord.getMinBufferSize(
                    eventPayload.getCaptureAudioFormat().getSampleRate(),
                    eventPayload.getCaptureAudioFormat().getChannelMask(),
                    eventPayload.getCaptureAudioFormat().getEncoding()));

            int sampleRate = eventPayload.getCaptureAudioFormat().getSampleRate();
            int bytesPerSecond = BYTES_PER_SAMPLE * sampleRate;

            // For Non-trusted:
//            Integer captureSession = 0;
//            try {
//                Method getCaptureSessionMethod = eventPayload.getClass().getMethod("getCaptureSession");
//                captureSession = (Integer) getCaptureSessionMethod.invoke(eventPayload);
//            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//                e.printStackTrace();
//            }
//            int sessionId = generateSessionId ?
//                    AudioManager.AUDIO_SESSION_ID_GENERATE : captureSession;
//            AudioRecord record = createAudioRecord(eventPayload, bytesPerSecond, sessionId);
            AudioRecord record = createAudioRecord(eventPayload, bytesPerSecond);
            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to init first AudioRecord.");
                try {
                    mDetector.startRecognition();
                } catch (HotwordDetector.IllegalDetectorStateException e) {
                    e.printStackTrace();
                }
                return;
            }

            byte[] buffer = new byte[bytesPerSecond * 6];
            record.startRecording();
            int numBytes = AudioUtils.read(record, bytesPerSecond, 5, buffer);

//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                Thread.interrupted();
//                throw new RuntimeException(e);
//            }

            record.stop();
            record.release();

            Log.i(TAG, "numBytes=" + numBytes + " audioSeconds=" + numBytes * 1.0 / bytesPerSecond);
            mData.putByteArray("1", buffer);
            mAudioFormat = eventPayload.getCaptureAudioFormat();
            mLastPayload = eventPayload;

            try {
                mDetector.startRecognition();
            } catch (HotwordDetector.IllegalDetectorStateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError() {
            Log.i(TAG, "onError");
            try {
                mDetector.startRecognition();
            } catch (HotwordDetector.IllegalDetectorStateException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRecognitionPaused() {
            Log.i(TAG, "onRecognitionPaused");
        }

        @Override
        public void onRecognitionResumed() {
            Log.i(TAG, "onRecognitionResumed");
        }

        @Override
        public void onHotwordDetectionServiceInitialized(int status) {
            Log.i(TAG, "onHotwordDetectionServiceInitialized: " + status
                    + ". mAvailable=" + mAvailable);
            if (mAvailable) {
                try {
                    mDetector.startRecognition();
                } catch (HotwordDetector.IllegalDetectorStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static AudioRecord createAudioRecord(EventPayload eventPayload, int bytesPerSecond) {
        return new AudioRecord.Builder()
                .setAudioAttributes(
                new AudioAttributes.Builder()
                        .setInternalCapturePreset(MediaRecorder.AudioSource.HOTWORD)
                        .build())
                .setAudioFormat(eventPayload.getCaptureAudioFormat())
                .setBufferSizeInBytes(getBufferSizeInBytes(bytesPerSecond, 2))
                .setSharedAudioEvent(eventPayload.getHotwordDetectedResult().getMediaSyncEvent())
                .build();
    }

    private static AudioRecord createAudioRecord(EventPayload eventPayload, int bytesPerSecond,
            int sessionId) {
        return new AudioRecord(
                new AudioAttributes.Builder()
                        .setInternalCapturePreset(MediaRecorder.AudioSource.HOTWORD)
                        .build(),
                eventPayload.getCaptureAudioFormat(),
                getBufferSizeInBytes(bytesPerSecond, 2),
                sessionId);
    }

    private static int getBufferSizeInBytes(int bytesPerSecond, float bufferLengthSeconds) {
        return (int) (bytesPerSecond * bufferLengthSeconds);
    }
}
