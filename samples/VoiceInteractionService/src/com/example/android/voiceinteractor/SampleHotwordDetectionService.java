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

import android.media.AudioAttributes;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.SharedMemory;
import android.os.Trace;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.HotwordDetectedResult;
import android.service.voice.HotwordDetectionService;
import android.service.voice.HotwordRejectedResult;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.IntConsumer;

public class SampleHotwordDetectionService extends HotwordDetectionService {
    static final String TAG = "SHotwordDetectionSrvc";

    // AudioRecord config
    private static final Duration AUDIO_RECORD_BUFFER_DURATION = Duration.ofSeconds(5);
    private static final Duration DSP_AUDIO_READ_DURATION = Duration.ofSeconds(3);
    private static final Duration AUDIO_RECORD_RELEASE_TIMEOUT = Duration.ofSeconds(10);

    private static AudioRecord createAudioRecord(AlwaysOnHotwordDetector.EventPayload eventPayload,
            int bytesPerSecond,
            int sessionId) {
        int audioRecordBufferSize = getBufferSizeInBytes(bytesPerSecond,
                AUDIO_RECORD_BUFFER_DURATION.getSeconds());
        Log.d(TAG, "creating AudioRecord: bytes=" + audioRecordBufferSize
                + ", lengthSeconds=" + (audioRecordBufferSize / bytesPerSecond));
        return new AudioRecord.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setInternalCapturePreset(MediaRecorder.AudioSource.HOTWORD)
                                // TODO see what happens if this is too small
                                .build())
                .setAudioFormat(eventPayload.getCaptureAudioFormat())
                .setBufferSizeInBytes(audioRecordBufferSize)
                .setSessionId(sessionId)
                .setMaxSharedAudioHistoryMillis(AudioRecord.getMaxSharedAudioHistoryMillis())
                .build();
    }

    private static int getBufferSizeInBytes(int bytesPerSecond, float bufferLengthSeconds) {
        return (int) (bytesPerSecond * bufferLengthSeconds);
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
    public void onDetect(
            @NonNull AlwaysOnHotwordDetector.EventPayload eventPayload,
            long timeoutMillis,
            @NonNull Callback callback) {
        Log.d(TAG, "onDetect (Hardware trigger): " + eventPayload);
        Trace.beginAsyncSection("HDS.onDetected", 0);

        int sampleRate = eventPayload.getCaptureAudioFormat().getSampleRate();
        int bytesPerSecond =
                eventPayload.getCaptureAudioFormat().getFrameSizeInBytes() * sampleRate;

        Integer captureSession = 0;
        try {
            Method getCaptureSessionMethod = eventPayload.getClass().getMethod("getCaptureSession");
            captureSession = (Integer) getCaptureSessionMethod.invoke(eventPayload);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        int sessionId =
//                generateSessionId ?
//                AudioManager.AUDIO_SESSION_ID_GENERATE :
                captureSession;
        Trace.beginAsyncSection("HDS.createAudioRecord", 1);
        AudioRecord record = createAudioRecord(eventPayload, bytesPerSecond, sessionId);
        Trace.endAsyncSection("HDS.createAudioRecord", 1);
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Failed to init first AudioRecord.");
            callback.onRejected(new HotwordRejectedResult.Builder().build());
            return;
        }

        byte[] buffer = new byte[bytesPerSecond * (int) DSP_AUDIO_READ_DURATION.getSeconds()];
        Log.d(TAG, "starting read: bytesPerSecond=" + bytesPerSecond
                + ", totalBufferSize=" + buffer.length);
        Trace.beginAsyncSection("HDS.startRecording", 1);
        record.startRecording();
        Trace.endAsyncSection("HDS.startRecording", 1);
        Trace.beginAsyncSection("AudioUtils.read", 1);
        AudioUtils.read(record, bytesPerSecond, DSP_AUDIO_READ_DURATION.getSeconds(), buffer);
        Trace.endAsyncSection("AudioUtils.read", 1);

        callback.onDetected(
                new HotwordDetectedResult.Builder()
                        .setMediaSyncEvent(
                                record.shareAudioHistory("com.example.android.voiceinteractor", 0))
                        .setHotwordPhraseId(getKeyphraseId(eventPayload))
                        .build());
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.i(TAG, "Releasing audio record");
            record.stop();
            record.release();
        }, AUDIO_RECORD_RELEASE_TIMEOUT.toMillis());
        Trace.endAsyncSection("HDS.onDetected", 0);
    }

    private int getKeyphraseId(AlwaysOnHotwordDetector.EventPayload payload) {
        return 0;
//        if (payload.getKeyphraseRecognitionExtras().isEmpty()) {
//            return 0;
//        }
//        return payload.getKeyphraseRecognitionExtras().get(0).getKeyphraseId();
    }

    @Override
    public void onDetect(@NonNull Callback callback) {
        Log.w(TAG, "onDetect called for microphone trigger");
    }

}
