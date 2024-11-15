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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.util.concurrent.Uninterruptibles.joinUninterruptibly;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.AudioPlaybackCallback;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecord;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioMixingRule;
import android.media.audiopolicy.AudioPolicy;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;

import com.example.android.vdmdemo.common.RemoteEventProto.AudioFrame;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.StartAudio;
import com.example.android.vdmdemo.common.RemoteEventProto.StopAudio;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
final class AudioStreamer {
    private static final String TAG = AudioStreamer.class.getSimpleName();

    private static final int SAMPLE_RATE = 44000;
    private static final AudioFormat AUDIO_FORMAT =
            new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();

    private static final ImmutableSet<Integer> STREAMING_PLAYER_STATES =
            ImmutableSet.of(
                    AudioPlaybackConfiguration.PLAYER_STATE_IDLE,
                    AudioPlaybackConfiguration.PLAYER_STATE_STARTED);

    private final Context mApplicationContext;
    private Context mDeviceContext;
    private final RemoteIo mRemoteIo;
    private AudioManager mAudioManager;
    private int mPlaybackSessionId;

    @Inject
    PreferenceController mPreferenceController;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private AudioPolicy mAudioSessionPolicy;

    @GuardedBy("mLock")
    private AudioMix mSessionIdAudioMix;

    @GuardedBy("mLock")
    private AudioPolicy mUidAudioPolicy;

    @GuardedBy("mLock")
    private AudioMix mUidAudioMix;

    @GuardedBy("mLock")
    private StreamingThread mStreamingThread;

    @GuardedBy("mLock")
    private AudioDeviceInfo mRemoteSubmixDevice;

    @GuardedBy("mLock")
    private AudioRecord mGhostRecord;

    private ImmutableSet<Integer> mReroutedUids = ImmutableSet.of();

    private final AudioPlaybackCallback mAudioPlaybackCallback =
            new AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    super.onPlaybackConfigChanged(configs);

                    synchronized (mLock) {
                        boolean shouldStream = configs.stream().anyMatch(
                                c -> STREAMING_PLAYER_STATES.contains(c.getPlayerState())
                                        && (mReroutedUids.contains(c.getClientUid())
                                        || c.getSessionId() == mPlaybackSessionId));

                        if (mAudioSessionPolicy == null) {
                            Log.d(TAG, "There's no active audio policy, ignoring playback "
                                            + "config callback");
                            return;
                        }

                        if (mSessionIdAudioMix != null && shouldStream
                                && mStreamingThread == null) {
                            mRemoteIo.sendMessage(
                                    RemoteEvent.newBuilder()
                                            .setStartAudio(StartAudio.newBuilder())
                                            .build());
                            mStreamingThread = new StreamingThread(
                                    mAudioSessionPolicy.createAudioRecordSink(mSessionIdAudioMix),
                                            mRemoteIo);
                            mStreamingThread.start();
                        } else if (!shouldStream && mStreamingThread != null) {
                            mRemoteIo.sendMessage(
                                    RemoteEvent.newBuilder()
                                            .setStopAudio(StopAudio.newBuilder())
                                            .build());
                            mStreamingThread.stopStreaming();
                            joinUninterruptibly(mStreamingThread);
                            mStreamingThread = null;
                        }
                    }
                }
            };

    @Inject
    AudioStreamer(@ApplicationContext Context context, RemoteIo remoteIo) {
        mApplicationContext = context;
        mRemoteIo = remoteIo;
    }

    public void start(int deviceId, int audioSessionId) {
        Log.d(TAG, "AudioStreamer start with deviceId " + deviceId + " and audioSessionId "
                + audioSessionId);
        mDeviceContext = mApplicationContext.createDeviceContext(deviceId)
                .createDeviceContext(deviceId);

        mAudioManager = mDeviceContext.getSystemService(AudioManager.class);
        if (mAudioManager != null) {
            mAudioManager.registerAudioPlaybackCallback(mAudioPlaybackCallback, null);
            registerAudioPolicies(audioSessionId, ImmutableSet.of());
        }
    }

    private void registerAudioPolicies(int audioSessionId, ImmutableSet<Integer> uids) {
        synchronized (mLock) {
            // order is important, the Session ID audio policy creates
            // the remote submix audio device used in the uid policy
            if (registerSessionPolicy(audioSessionId)) {
                registerUidPolicy(uids);
            }
        }
    }

    private void unregisterAudioPolicies() {
        synchronized (mLock) {
            unregisterUidPolicy();
            unregisterSessionPolicy();
        }
    }

    private @Nullable AudioDeviceInfo getNewRemoteSubmixAudioDevice(
            Collection<AudioDeviceInfo> preexistingDevices) {
        Set<String> preexistingAddresses =
                preexistingDevices.stream()
                        .map(AudioDeviceInfo::getAddress)
                        .collect(Collectors.toSet());

        List<AudioDeviceInfo> newDevices =
                getRemoteSubmixDevices().stream()
                        .filter(dev -> !preexistingAddresses.contains(dev.getAddress()))
                        .collect(Collectors.toList());

        if (newDevices.size() > 1) {
            Log.e(TAG, "There's more than 1 new remote submix device");
            return null;
        }
        if (newDevices.isEmpty()) {
            Log.e(TAG, "Didn't find new remote submix device");
            return null;
        }
        return getOnlyElement(newDevices);
    }

    public void updateVdmUids(ImmutableSet<Integer> uids) {
        Log.w(TAG, "Updating mixing rule to reroute uids " + uids);
        synchronized (mLock) {
            if (mRemoteSubmixDevice == null) {
                Log.e(TAG, "Cannot update audio policy - remote submix device not known");
                return;
            }

            if (mReroutedUids.equals(uids)) {
                Log.d(TAG, "Not updating UID audio policy for same set of UIDs");
                return;
            }

            updateAudioPolicies(uids);
        }
    }

    @SuppressLint("MissingPermission")
    @GuardedBy("mLock")
    private void updateAudioPolicies(ImmutableSet<Integer> uids) {
        long startUpdateTimeMs = SystemClock.uptimeMillis();

        Log.d(TAG, "Started updateAudioPolicies. Already rerouted uids: "
                + mReroutedUids + " -> updated uids: " + uids);

        if (BuildCompat.isAtLeastV() && mPreferenceController.getBoolean(
                R.string.pref_enable_update_audio_policy_mixes)) {
            if (mUidAudioPolicy != null && mUidAudioMix != null && !mReroutedUids.isEmpty()) {
                if (uids.isEmpty()) {
                    unregisterUidPolicy();

                    Log.d(TAG, "Unregistered UID AudioPolicy since uid set is empty in "
                            + (SystemClock.uptimeMillis() - startUpdateTimeMs) + "ms.");
                } else {
                    Pair<AudioMix, AudioMixingRule> update =
                            Pair.create(mUidAudioMix, createUidMixingRule(uids));
                    mReroutedUids = ImmutableSet.copyOf(uids);
                    mUidAudioPolicy.updateMixingRules(Collections.singletonList(update));

                    Log.d(TAG, "Updated UID AudioPolicy mixes in "
                            + (SystemClock.uptimeMillis() - startUpdateTimeMs) + "ms.");
                }
            } else {
                registerUidPolicy(uids);

                Log.d(TAG, "Registered UID AudioPolicy since there was none previous in "
                        + (SystemClock.uptimeMillis() - startUpdateTimeMs) + "ms.");
            }
        } else {
            // Legacy and inefficient way to unregister the UID audio policy and then just
            // re-register it again. Leads to audio leaks.
            synchronized (mLock) {
                unregisterUidPolicy();
                registerUidPolicy(uids);
            }

            Log.d(TAG, "Unregistered / re-registered UID AudioPolicy in "
                    + (SystemClock.uptimeMillis() - startUpdateTimeMs) + "ms.");
        }
    }

    @SuppressLint("MissingPermission")
    @GuardedBy("mLock")
    // returns true if successfully registered, false otherwise
    private boolean registerSessionPolicy(int audioSessionId) {
        AudioMixingRule mixingRule = new AudioMixingRule.Builder()
                .addMixRule(AudioMixingRule.RULE_MATCH_AUDIO_SESSION_ID, audioSessionId)
                .build();

        AudioMix audioMix = new AudioMix.Builder(mixingRule)
                .setRouteFlags(AudioMix.ROUTE_FLAG_LOOP_BACK)
                .setFormat(AUDIO_FORMAT)
                .build();

        synchronized (mLock) {
            if (mAudioSessionPolicy != null) {
                Log.w(TAG, "MediaSession AudioPolicy is already registered.");
                return false;
            }

            AudioPolicy sessionPolicy = new AudioPolicy.Builder(mApplicationContext)
                    .addMix(audioMix)
                    .build();

            int ret = mAudioManager.registerAudioPolicy(sessionPolicy);
            if (ret != AudioManager.SUCCESS) {
                Log.e(TAG, "Failed to register media session audio policy, error code " + ret);
                return false;
            }

            mAudioSessionPolicy = sessionPolicy;
            mSessionIdAudioMix = audioMix;
            mPlaybackSessionId = audioSessionId;

            // This is a hacky way to determine audio device associated with audio mix.
            // once audio record for the policy is initialized, audio policy manager
            // will create remote submix instance so we can compare devices before and after
            // to determine which device corresponds to this particular mix.
            // The ghost audio record needs to be kept alive, releasing it would cause
            // destruction of remote submix instances and potential problems when updating the
            // UID-based render policy pointing to the remote submix device.
            List<AudioDeviceInfo> preexistingRemoteSubmixDevices = getRemoteSubmixDevices();
            mGhostRecord = mAudioSessionPolicy.createAudioRecordSink(audioMix);
            mGhostRecord.startRecording();
            mRemoteSubmixDevice = getNewRemoteSubmixAudioDevice(preexistingRemoteSubmixDevices);
        }

        Log.i(TAG, "Registered MediaSession audio policy successfully.");
        return true;
    }

    @SuppressLint("MissingPermission")
    @GuardedBy("mLock")
    private void registerUidPolicy(ImmutableSet<Integer> uids) {
        // we can't create an UID audio policy with no uids to redirect
        // nor without a remote submix device created by the media session policy
        if (uids.isEmpty() || mRemoteSubmixDevice == null) {
            return;
        }

        AudioMix uidAudioMix = new AudioMix.Builder(createUidMixingRule(uids))
                        .setRouteFlags(AudioMix.ROUTE_FLAG_RENDER)
                        .setDevice(mRemoteSubmixDevice)
                        .setFormat(AUDIO_FORMAT)
                        .build();

        AudioPolicy uidPolicy = new AudioPolicy.Builder(mApplicationContext)
                .addMix(uidAudioMix)
                .build();

        int ret = mAudioManager.registerAudioPolicy(uidPolicy);
        if (ret != AudioManager.SUCCESS) {
            Log.e(TAG, "Failed to register UID audio policy, error code " + ret);
            return;
        }

        mUidAudioMix = uidAudioMix;
        mUidAudioPolicy = uidPolicy;
        mReroutedUids = ImmutableSet.copyOf(uids);

        Log.d(TAG, "Registered UID audio policy successfully.");
    }

    @SuppressLint("MissingPermission")
    @GuardedBy("mLock")
    private void unregisterUidPolicy() {
        if (mUidAudioPolicy != null) {
            mAudioManager.unregisterAudioPolicy(mUidAudioPolicy);
            mUidAudioPolicy = null;
            mUidAudioMix = null;
            mReroutedUids = ImmutableSet.of();
        }

        Log.d(TAG, "Unregistered UID audio policy done.");
    }

    @SuppressLint("MissingPermission")
    @GuardedBy("mLock")
    private void unregisterSessionPolicy() {
        if (mAudioSessionPolicy != null) {
            if (mGhostRecord != null) {
                mGhostRecord.stop();
                mGhostRecord.release();
                mGhostRecord = null;
            }

            mAudioManager.unregisterAudioPolicy(mAudioSessionPolicy);
            mPlaybackSessionId = 0;
            mAudioSessionPolicy = null;
            mSessionIdAudioMix = null;
            mRemoteSubmixDevice = null;
        }

        Log.i(TAG, "Unregistered MediaSession audio policy done.");
    }
    public void stop() {
        Log.d(TAG, "AudioStreamer stop.");
        synchronized (mLock) {
            if (mStreamingThread != null) {
                mStreamingThread.stopStreaming();
                joinUninterruptibly(mStreamingThread);
                mStreamingThread = null;
            }

            if (mAudioManager != null) {
                unregisterAudioPolicies();
                mAudioManager.unregisterAudioPlaybackCallback(mAudioPlaybackCallback);
            }
        }
    }

    private AudioMixingRule createUidMixingRule(Collection<Integer> uids) {
        AudioMixingRule.Builder builder = new AudioMixingRule.Builder();
        uids.forEach(uid -> builder.addMixRule(AudioMixingRule.RULE_MATCH_UID, uid));
        return builder.build();
    }

    private List<AudioDeviceInfo> getRemoteSubmixDevices() {
        return Arrays.stream(mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
                .filter(AudioStreamer::deviceIsRemoteSubmixOut)
                .collect(Collectors.toList());
    }

    private static boolean deviceIsRemoteSubmixOut(AudioDeviceInfo info) {
        return info != null
                && info.getType() == AudioDeviceInfo.TYPE_REMOTE_SUBMIX
                && info.isSink();
    }

    private static class StreamingThread extends Thread {
        private static final int BUFFER_SIZE =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
        private final RemoteIo mRemoteIo;
        private final AudioRecord mAudioRecord;
        private final AtomicBoolean mIsRunning = new AtomicBoolean(true);

        StreamingThread(AudioRecord audioRecord, RemoteIo remoteIo) {
            super();
            mRemoteIo = Objects.requireNonNull(remoteIo);
            mAudioRecord = Objects.requireNonNull(audioRecord);
        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "Starting audio streaming");

            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Audio record is not initialized");
                return;
            }

            mAudioRecord.startRecording();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (mIsRunning.get()) {
                int ret = mAudioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
                if (ret <= 0) {
                    Log.e(TAG, "AudioRecord.read returned error code " + ret);
                    continue;
                }

                mRemoteIo.sendMessage(
                        RemoteEvent.newBuilder()
                                .setAudioFrame(
                                        AudioFrame.newBuilder()
                                                .setData(ByteString.copyFrom(buffer, 0, ret)))
                                .build());
            }
            Log.d(TAG, "Stopping audio streaming");
            mAudioRecord.stop();
            mAudioRecord.release();
        }

        void stopStreaming() {
            mIsRunning.set(false);
        }
    }
}
