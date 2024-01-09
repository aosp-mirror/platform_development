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
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

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

    private final Context mContext;
    private final RemoteIo mRemoteIo;
    private final AudioManager mAudioManager;
    private final int mPlaybackSessionId;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private AudioPolicy mAudioPolicy;

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
                        if (mAudioPolicy == null) {
                            Log.d(
                                    TAG,
                                    "There's no active audio policy, ignoring playback "
                                            + "config callback");
                            return;
                        }

                        if (mSessionIdAudioMix != null && shouldStream
                                && mStreamingThread == null) {
                            mRemoteIo.sendMessage(
                                    RemoteEvent.newBuilder()
                                            .setStartAudio(StartAudio.newBuilder())
                                            .build());
                            mStreamingThread =
                                    new StreamingThread(
                                            mAudioPolicy.createAudioRecordSink(mSessionIdAudioMix),
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
        mContext = context;
        mRemoteIo = remoteIo;
        mAudioManager = context.getSystemService(AudioManager.class);
        mPlaybackSessionId = mAudioManager.generateAudioSessionId();
    }

    public void start() {
        mAudioManager.registerAudioPlaybackCallback(mAudioPlaybackCallback, null);
        registerAudioPolicy();
    }

    public int getPlaybackSessionId() {
        return mPlaybackSessionId;
    }

    private void registerAudioPolicy() {
        AudioMixingRule mixingRule =
                new AudioMixingRule.Builder()
                        .addMixRule(AudioMixingRule.RULE_MATCH_AUDIO_SESSION_ID, mPlaybackSessionId)
                        .build();
        AudioMix audioMix =
                new AudioMix.Builder(mixingRule)
                        .setRouteFlags(AudioMix.ROUTE_FLAG_LOOP_BACK)
                        .setFormat(AUDIO_FORMAT)
                        .build();

        synchronized (mLock) {
            if (mAudioPolicy != null) {
                Log.w(TAG, "AudioPolicy is already registered");
                return;
            }
            mAudioPolicy = new AudioPolicy.Builder(mContext).addMix(audioMix).build();
            int ret = mAudioManager.registerAudioPolicy(mAudioPolicy);
            if (ret != AudioManager.SUCCESS) {
                Log.e(TAG, "Failed to register audio policy, error code " + ret);
                mAudioPolicy = null;
                return;
            }

            // This is a hacky way to determine audio device associated with audio mix.
            // once audio record for the policy is initialized, audio policy manager
            // will create remote submix instance so we can compare devices before and after
            // to determine which device corresponds to this particular mix.
            // The ghost audio record needs to be kept alive, releasing it would cause
            // destruction of remote submix instances and potential problems when updating the
            // UID-based render policy pointing to the remote submix device.
            List<AudioDeviceInfo> preexistingRemoteSubmixDevices = getRemoteSubmixDevices();
            mGhostRecord = mAudioPolicy.createAudioRecordSink(audioMix);
            mGhostRecord.startRecording();
            mRemoteSubmixDevice = getNewRemoteSubmixAudioDevice(preexistingRemoteSubmixDevices);
            mSessionIdAudioMix = audioMix;
            if (!mReroutedUids.isEmpty()) {
                registerUidPolicy(mReroutedUids);
            }
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
        if (newDevices.size() == 0) {
            Log.e(TAG, "Didn't find new remote submix device");
            return null;
        }
        return getOnlyElement(newDevices);
    }

    public void updateVdmUids(Set<Integer> uids) {
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

    // TODO(b/293611855) Use finer grained audio policy + mix controls once bugs are addressed
    // This shouldn't unregister all audio policies just to re-register them again.
    // That's inefficient and leads to audio leaks. But this is the most correct way to do this at
    // this time.
    @GuardedBy("mLock")
    private void updateAudioPolicies(Set<Integer> uids) {
        // TODO(b/293279299) Use Flagged API
        // if (com.android.media.audio.flags.Flags.FLAG_AUDIO_POLICY_UPDATE_MIXING_RULES_API &&
        // uidAudioMix != null && (!reroutedUids.isEmpty() && !uids.isEmpty())) {
        //   Pair<AudioMix, AudioMixingRule> update = Pair.create(uidAudioMix,
        // createUidMixingRule(uids));
        //   uidAudioPolicy.updateMixingRules(Collections.singletonList(update));
        //   return;
        // }

        if (mAudioPolicy != null) {
            mAudioManager.unregisterAudioPolicy(mAudioPolicy);
            mAudioPolicy = null;
        }
        if (mUidAudioPolicy != null) {
            mAudioManager.unregisterAudioPolicy(mUidAudioPolicy);
            mUidAudioPolicy = null;
        }

        mReroutedUids = ImmutableSet.copyOf(uids);
        registerAudioPolicy();
    }

    @GuardedBy("mLock")
    private void registerUidPolicy(ImmutableSet<Integer> uids) {
        mUidAudioMix =
                new AudioMix.Builder(createUidMixingRule(uids))
                        .setRouteFlags(AudioMix.ROUTE_FLAG_RENDER)
                        .setDevice(mRemoteSubmixDevice)
                        .setFormat(AUDIO_FORMAT)
                        .build();
        AudioPolicy uidPolicy = new AudioPolicy.Builder(mContext).addMix(mUidAudioMix).build();
        int ret = mAudioManager.registerAudioPolicy(uidPolicy);
        if (ret != AudioManager.SUCCESS) {
            Log.e(TAG, "Error " + ret + " while trying to register UID policy");
            return;
        }

        mUidAudioPolicy = uidPolicy;
        mReroutedUids = ImmutableSet.copyOf(uids);
    }

    public void stop() {
        synchronized (mLock) {
            if (mStreamingThread != null) {
                mStreamingThread.stopStreaming();
                joinUninterruptibly(mStreamingThread);
                mStreamingThread = null;
            }
            if (mUidAudioPolicy != null) {
                mAudioManager.unregisterAudioPolicy(mUidAudioPolicy);
                mUidAudioPolicy = null;
            }
            if (mAudioPolicy != null) {
                mAudioManager.unregisterAudioPolicy(mAudioPolicy);
                mAudioPolicy = null;
            }
            if (mGhostRecord != null) {
                mGhostRecord.stop();
                mGhostRecord.release();
                mGhostRecord = null;
            }

            mAudioManager.unregisterAudioPlaybackCallback(mAudioPlaybackCallback);
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
