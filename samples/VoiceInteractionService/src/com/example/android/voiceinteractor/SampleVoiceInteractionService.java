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
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Trace;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.AlwaysOnHotwordDetector.EventPayload;
import android.service.voice.HotwordDetector;
import android.service.voice.HotwordRejectedResult;
import android.service.voice.SandboxedDetectionInitializer;
import android.service.voice.VisualQueryDetectionServiceFailure;
import android.service.voice.VisualQueryDetector;
import android.service.voice.VoiceInteractionService;
import android.util.Log;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Executors;

public class SampleVoiceInteractionService extends VoiceInteractionService {
    public static final String DSP_MODEL_KEYPHRASE = "X Google";
    private static final String TAG = "VIS";

    // AudioRecord config
    private static final Duration AUDIO_RECORD_BUFFER_DURATION = Duration.ofSeconds(5);
    private static final Duration AUDIO_READ_DURATION = Duration.ofSeconds(3);

    // DSP model config
    private static final Locale DSP_MODEL_LOCALE = Locale.US;

    private final IBinder binder = new LocalBinder();

    HotwordDetector mHotwordDetector;
    VisualQueryDetector mVisualQueryDetector;
    Callback mHotwordDetectorCallback;
    VisualQueryDetector.Callback mVisualQueryDetectorCallback;
    Bundle mData = new Bundle();
    AudioFormat mAudioFormat;
    EventPayload mLastPayload;

    private static AudioRecord createAudioRecord(EventPayload eventPayload, int bytesPerSecond) {
        int audioRecordBufferSize = getBufferSizeInBytes(bytesPerSecond,
                AUDIO_RECORD_BUFFER_DURATION.getSeconds());
        Log.d(TAG, "creating AudioRecord: bytes=" + audioRecordBufferSize
                + ", lengthSeconds=" + (audioRecordBufferSize / bytesPerSecond));
        return new AudioRecord.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setInternalCapturePreset(MediaRecorder.AudioSource.HOTWORD)
                                .build())
                .setAudioFormat(eventPayload.getCaptureAudioFormat())
                .setBufferSizeInBytes(audioRecordBufferSize)
                .setSharedAudioEvent(eventPayload.getHotwordDetectedResult().getMediaSyncEvent())
                .build();
    }

    private static int getBufferSizeInBytes(int bytesPerSecond, float bufferLengthSeconds) {
        return (int) (bytesPerSecond * bufferLengthSeconds);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if ("local".equals(intent.getAction())) {
            return binder;
        }
        return super.onBind(intent);
    }

    @Override
    public void onReady() {
        super.onReady();
        Log.i(TAG, "onReady");
        mHotwordDetectorCallback = new Callback();
        mVisualQueryDetectorCallback = new VisualQueryDetectorCallback();
        mHotwordDetector = createAlwaysOnHotwordDetector(DSP_MODEL_KEYPHRASE,
                        DSP_MODEL_LOCALE, null, null, mHotwordDetectorCallback);

    }

    @Override
    public void onShutdown() {
        super.onShutdown();
        Log.i(TAG, "onShutdown");
    }

    public class LocalBinder extends Binder {
        SampleVoiceInteractionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SampleVoiceInteractionService.this;
        }
    }

    class VisualQueryDetectorCallback implements VisualQueryDetector.Callback {
            @Override
            public void onQueryDetected(@NonNull String partialQuery) {
                Log.i(TAG, "VQD partial query detected: "+ partialQuery);
            }

            @Override
            public void onQueryRejected() {
                Log.i(TAG, "VQD query rejected");
            }

            @Override
            public void onQueryFinished() {
                Log.i(TAG, "VQD query finished");
            }

            @Override
            public void onVisualQueryDetectionServiceInitialized(int status) {
                Log.i(TAG, "VQD init: "+ status);
                if (status == SandboxedDetectionInitializer.INITIALIZATION_STATUS_SUCCESS) {
                    mVisualQueryDetector.startRecognition();
                }
            }

            @Override
            public void onVisualQueryDetectionServiceRestarted() {
                Log.i(TAG, "VQD restarted");
                mVisualQueryDetector.startRecognition();
            }

        @Override
        public void onFailure(
                VisualQueryDetectionServiceFailure visualQueryDetectionServiceFailure) {
            Log.i(TAG, "VQD onFailure visualQueryDetectionServiceFailure: "
                    + visualQueryDetectionServiceFailure);
        }

        @Override
        public void onUnknownFailure(String errorMessage) {
            Log.i(TAG, "VQD onUnknownFailure errorMessage: " + errorMessage);
        }
        };

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
                enrollIntent = ((AlwaysOnHotwordDetector) mHotwordDetector).createEnrollIntent();
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
            mHotwordDetector.startRecognition();
        }

        @Override
        public void onDetected(@NonNull EventPayload eventPayload) {
            Trace.beginAsyncSection("VIS.onDetected", 0);
            onDetected(eventPayload, false);
            Trace.endAsyncSection("VIS.onDetected", 0);
        }

        public void onDetected(@NonNull EventPayload eventPayload, boolean generateSessionId) {
            Log.i(TAG, "onDetected: " + eventPayload);
            Log.i(TAG, "minBufferSize: "
                    + AudioRecord.getMinBufferSize(
                    eventPayload.getCaptureAudioFormat().getSampleRate(),
                    eventPayload.getCaptureAudioFormat().getChannelMask(),
                    eventPayload.getCaptureAudioFormat().getEncoding()));

            int sampleRate = eventPayload.getCaptureAudioFormat().getSampleRate();
            int bytesPerSecond =
                    eventPayload.getCaptureAudioFormat().getFrameSizeInBytes() * sampleRate;

            Trace.beginAsyncSection("VIS.createAudioRecord", 1);

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
            Trace.endAsyncSection("VIS.createAudioRecord", 1);
            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                Trace.setCounter("VIS AudioRecord.getState",
                        record.getState());
                Log.e(TAG, "Failed to init first AudioRecord.");
                mHotwordDetector.startRecognition();
                return;
            }

            byte[] buffer = new byte[bytesPerSecond * (int) AUDIO_READ_DURATION.getSeconds()];
            Trace.beginAsyncSection("VIS.startRecording", 1);
            record.startRecording();
            Trace.endAsyncSection("VIS.startRecording", 1);
            Trace.beginAsyncSection("AudioUtils.read", 1);
            int numBytes = AudioUtils.read(record, bytesPerSecond, AUDIO_READ_DURATION.getSeconds(),
                    buffer);
            Trace.endAsyncSection("AudioUtils.read", 1);

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
            mHotwordDetector.startRecognition();
        }

        @Override
        public void onError() {
            Log.i(TAG, "onError");
            mHotwordDetector.startRecognition();
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
                mHotwordDetector.startRecognition();
            }
            //TODO(b/265535257): Provide two services independent lifecycle.
            mVisualQueryDetector = createVisualQueryDetector(null, null,
                Executors.newSingleThreadExecutor(), mVisualQueryDetectorCallback);
        }
    }
}
