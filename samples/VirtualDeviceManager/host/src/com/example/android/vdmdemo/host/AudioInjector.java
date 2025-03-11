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

import static android.media.AudioTrack.STATE_INITIALIZED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.AudioTrack;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioMixingRule;
import android.media.audiopolicy.AudioPolicy;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.core.os.BuildCompat;

import com.example.android.vdmdemo.common.RemoteEventProto;
import com.example.android.vdmdemo.common.RemoteEventProto.AudioFrame;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.common.collect.ImmutableSet;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public final class AudioInjector implements Consumer<RemoteEvent> {
    private static final String TAG = AudioInjector.class.getSimpleName();

    private static final int SAMPLE_RATE = 48000;
    public static final AudioFormat AUDIO_FORMAT_IN =
            new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();

    @Inject
    PreferenceController mPreferenceController;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final List<AudioTrack> mAudioTracks = new ArrayList<>();
    private final RemoteIo mRemoteIo;
    private int mRecordingSessionId;
    private boolean mIsPlaying;
    @GuardedBy("mLock")
    private ImmutableSet<Integer> mReroutedUids = ImmutableSet.of();
    private AudioPolicy mAudioPolicy;
    private AudioMix mUidAudioMix;
    private final Context mApplicationContext;
    private Context mDeviceContext;
    private AudioManager mAudioManager;

    private final AudioManager.AudioRecordingCallback mAudioRecordingCallback =
            new AudioManager.AudioRecordingCallback() {
                @SuppressLint("MissingPermission")
                @Override
                public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                    super.onRecordingConfigChanged(configs);

                    synchronized (AudioInjector.this.mLock) {
                        boolean shouldStream = false;
                        for (AudioRecordingConfiguration config : configs) {
                            if (mReroutedUids.contains(config.getClientUid())
                                    || config.getClientAudioSessionId() == mRecordingSessionId) {
                                shouldStream = true;
                            }
                        }
                        if (mAudioPolicy == null) {
                            Log.d(TAG, "There's no active audio policy, ignoring recording "
                                    + "config callback");
                            return;
                        }

                        if (shouldStream && !mIsPlaying) {
                            mRemoteIo.sendMessage(RemoteEvent.newBuilder()
                                    .setStartAudioInput(
                                            RemoteEventProto.StartAudioInput.newBuilder()
                                                    .setSampleRate(AUDIO_FORMAT_IN.getSampleRate())
                                                    .setChannelMask(
                                                            AUDIO_FORMAT_IN.getChannelMask())
                                                    .setEncoding(AUDIO_FORMAT_IN.getEncoding())
                                                    .build())
                                    .build());
                        } else if (!shouldStream && mIsPlaying) {
                            mRemoteIo.sendMessage(RemoteEvent.newBuilder().setStopAudioInput(
                                    RemoteEventProto.StopAudioInput.newBuilder().build()).build());
                        }
                    }
                }
            };

    @Inject
    AudioInjector(@ApplicationContext Context context, RemoteIo remoteIo) {
        mApplicationContext = context;
        mRemoteIo = remoteIo;
    }

    private void playAudioFrame(AudioFrame audioFrame) {
        synchronized (mLock) {
            if (mAudioTracks.isEmpty()) {
                Log.e(TAG, "Received audio frame, but no audio track was initialized.");
            }

            for (AudioTrack audioTrack : mAudioTracks) {
                playAudioFrame(audioFrame, audioTrack);
            }
        }
    }

    private void playAudioFrame(AudioFrame audioFrame, AudioTrack audioTrack) {
        byte[] data = audioFrame.getData().toByteArray();
        int bytesToWrite = data.length;
        if (bytesToWrite == 0) {
            return;
        }
        int bytesWritten = 0;
        if (audioTrack == null) {
            Log.e(TAG, "Received audio frame, but no audio track was initialized.");
            return;
        }

        while (bytesToWrite > 0 && mIsPlaying) {
            int ret = audioTrack.write(data, bytesWritten, bytesToWrite);
            if (ret < 0) {
                Log.e(TAG, "AudioTrack.write returned error code " + ret);
                break;
            }
            bytesToWrite -= ret;
            bytesWritten += ret;
        }
    }

    private void startPlayback() {
        mIsPlaying = true;
        synchronized (mLock) {
            for (AudioTrack audioTrack : mAudioTracks) {
                audioTrack.play();
            }
        }
    }

    private void stopPlayback() {
        mIsPlaying = false;
        synchronized (mLock) {
            for (AudioTrack audioTrack : mAudioTracks) {
                audioTrack.stop();
            }
        }
    }

    /**
     * Setup the AudioInjector
     */
    public void start(int deviceId, int audioSessionId) {
        Log.d(TAG, "AudioInjector start with deviceId " + deviceId
                + " and audioSessionId " + audioSessionId);
        mDeviceContext = mApplicationContext.createDeviceContext(deviceId)
                .createDeviceContext(deviceId);
        mRecordingSessionId = audioSessionId;
        mRemoteIo.addMessageConsumer(this);

        mAudioManager = mDeviceContext.getSystemService(AudioManager.class);
        if (mAudioManager != null) {
            mAudioManager.registerAudioRecordingCallback(mAudioRecordingCallback, null);
            registerAudioPolicy(ImmutableSet.of());
        }
    }

    /**
     * Stop the AudioInjector
     */
    public void stop() {
        Log.d(TAG, "AudioInjector stop.");
        synchronized (mLock) {
            if (mIsPlaying) {
                mIsPlaying = false;
                mRemoteIo.sendMessage(RemoteEvent.newBuilder().setStopAudioInput(
                        RemoteEventProto.StopAudioInput.newBuilder().build()).build());
            }

            closeAndReleaseAllTracks();
            mRemoteIo.removeMessageConsumer(this);

            if (mAudioManager != null) {
                mAudioManager.unregisterAudioRecordingCallback(mAudioRecordingCallback);
                unregisterAudioPolicy();
                mAudioManager = null;
            }
        }
    }

    @Override
    public void accept(RemoteEvent remoteEvent) {
        if (remoteEvent.hasStartAudio()) {
            startPlayback();
        }
        if (remoteEvent.hasAudioFrame()) {
            playAudioFrame(remoteEvent.getAudioFrame());
        }
        if (remoteEvent.hasStopAudio()) {
            stopPlayback();
        }
    }

    /**
     * @param updatedUids uids that are now relevant to the AudioInjector
     */
    public void updateVdmUids(ImmutableSet<Integer> updatedUids) {
        synchronized (mLock) {
            if (mAudioPolicy == null) {
                Log.e(TAG, "Not updating AudioPolicy - no audio policy configured");
                return;
            }

            if (mReroutedUids.equals(updatedUids)) {
                Log.d(TAG, "Not updating UID audio policy for same set of UIDs");
                return;
            }

            updateAudioPolicies(updatedUids);
        }
    }

    @SuppressLint("MissingPermission")
    @GuardedBy("mLock")
    private void updateAudioPolicies(ImmutableSet<Integer> updatedUids) {
        long startUpdateTimeMs = SystemClock.uptimeMillis();

        Log.d(TAG, "Started updateAudioPolicies. Already reroutedUids: "
                + mReroutedUids + " -> updatedUids: " + updatedUids);

        if (BuildCompat.isAtLeastV() && mPreferenceController.getBoolean(
                R.string.pref_enable_update_audio_policy_mixes)) {
            if (mAudioPolicy != null) {
                // we have an audio policy, so we just need to update the uid audio mix
                if (updatedUids.isEmpty()) {
                    if (mUidAudioMix != null) {
                        mAudioPolicy.detachMixes(Collections.singletonList(mUidAudioMix));
                        mUidAudioMix = null;
                        mReroutedUids = ImmutableSet.of();
                    }

                    Log.d(TAG, "Detached UID audio mixes since updatedUids set is empty in "
                            + (SystemClock.uptimeMillis() - startUpdateTimeMs) + "ms.");
                } else {
                    if (mUidAudioMix != null) {
                        // we have an uid audio mix to update
                        Pair<AudioMix, AudioMixingRule> update =
                                Pair.create(mUidAudioMix, createUidMixingRule(updatedUids));
                        mReroutedUids = ImmutableSet.copyOf(updatedUids);
                        mAudioPolicy.updateMixingRules(Collections.singletonList(update));

                        Log.d(TAG, "Updated AudioPolicy mixes in "
                                + (SystemClock.uptimeMillis() - startUpdateTimeMs) + "ms.");
                    } else {
                        // no uid audio mix so add one to the policy
                        AudioMix uidAudioMix = getUidAudioMix(updatedUids);
                        if (uidAudioMix != null) {
                            mAudioPolicy.attachMixes(Collections.singletonList(uidAudioMix));
                            mUidAudioMix = uidAudioMix;
                            mReroutedUids = ImmutableSet.copyOf(updatedUids);

                            Log.d(TAG, "Attached UID audio mix since there was none previous in "
                                    + (SystemClock.uptimeMillis() - startUpdateTimeMs) + "ms.");
                        }
                    }
                }
            } else {
                // no audio policy set, shouldn't reach here, so register the full policy
                registerAudioPolicy(updatedUids);
            }
        } else {
            synchronized (mLock) {
                closeAndReleaseAllTracks();

                unregisterAudioPolicy();
                registerAudioPolicy(updatedUids);

                Log.d(TAG, "Unregistered / re-registered full AudioPolicy in "
                        + (SystemClock.uptimeMillis() - startUpdateTimeMs) + "ms.");
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void registerAudioPolicy(ImmutableSet<Integer> uids) {
        synchronized (mLock) {
            if (mAudioPolicy != null) {
                Log.w(TAG, "AudioInjector AudioPolicy is already registered.");
                return;
            }

            AudioMix sessionIdAudioMix = getSessionIdAudioMix(mRecordingSessionId);
            AudioMix uidAudioMix = getUidAudioMix(uids);

            AudioPolicy.Builder audioPolicyBuilder = new AudioPolicy.Builder(mDeviceContext)
                    .addMix(sessionIdAudioMix);
            if (uidAudioMix != null) {
                audioPolicyBuilder.addMix(uidAudioMix);
            }
            AudioPolicy audioPolicy = audioPolicyBuilder.build();
            int ret = mAudioManager.registerAudioPolicy(audioPolicy);
            if (ret != AudioManager.SUCCESS) {
                Log.e(TAG, "Error " + ret + " while trying to register audio policy");
                return;
            }

            mAudioPolicy = audioPolicy;
            mUidAudioMix = uidAudioMix;
            mReroutedUids = ImmutableSet.copyOf(uids);

            addAudioTrack(mAudioPolicy.createAudioTrackSource(sessionIdAudioMix));
            if (uidAudioMix != null) {
                addAudioTrack(mAudioPolicy.createAudioTrackSource(uidAudioMix));
            }

            Log.i(TAG, "Registered AudioInjector audio policy successfully.");
        }
    }

    @SuppressLint("MissingPermission")
    private void unregisterAudioPolicy() {
        if (mAudioPolicy != null) {
            mAudioManager.unregisterAudioPolicy(mAudioPolicy);
            mReroutedUids = ImmutableSet.of();
            mUidAudioMix = null;
            mAudioPolicy = null;
        }

        Log.i(TAG, "Unregistered AudioInjector audio policy done.");
    }

    private void addAudioTrack(AudioTrack audioTrack) {
        synchronized (mLock) {
            if (audioTrack.getState() != STATE_INITIALIZED) {
                throw new IllegalStateException("Set an uninitialized AudioTrack.");
            }

            if (mIsPlaying) {
                audioTrack.play();
            }
            mAudioTracks.add(audioTrack);
        }
    }

    private void closeAndReleaseAllTracks() {
        for (AudioTrack audioTrack : mAudioTracks) {
            audioTrack.stop();
            audioTrack.release();
        }
        mAudioTracks.clear();
    }

    private static AudioMix getSessionIdAudioMix(int sessionId) {
        AudioMixingRule sessionIdMixingRule =
                new AudioMixingRule.Builder()
                        .setTargetMixRole(AudioMixingRule.MIX_ROLE_INJECTOR)
                        .addMixRule(AudioMixingRule.RULE_MATCH_AUDIO_SESSION_ID,
                                sessionId)
                        .build();

        return new AudioMix.Builder(sessionIdMixingRule)
                .setRouteFlags(AudioMix.ROUTE_FLAG_LOOP_BACK)
                .setFormat(AUDIO_FORMAT_IN)
                .build();
    }

    private static AudioMix getUidAudioMix(ImmutableSet<Integer> reroutedUids) {
        if (reroutedUids.isEmpty()) {
            return null;
        }

        return new AudioMix.Builder(createUidMixingRule(reroutedUids))
                .setRouteFlags(AudioMix.ROUTE_FLAG_LOOP_BACK)
                .setFormat(AUDIO_FORMAT_IN)
                .build();
    }

    private static AudioMixingRule createUidMixingRule(ImmutableSet<Integer> uids) {
        AudioMixingRule.Builder builder = new AudioMixingRule.Builder().setTargetMixRole(
                AudioMixingRule.MIX_ROLE_INJECTOR);
        uids.forEach(uid -> builder.addMixRule(AudioMixingRule.RULE_MATCH_UID, uid));
        return builder.build();
    }
}
