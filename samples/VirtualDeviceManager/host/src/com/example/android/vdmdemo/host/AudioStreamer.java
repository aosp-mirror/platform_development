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
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import com.example.android.vdmdemo.common.RemoteEventProto.AudioFrame;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.StartAudio;
import com.example.android.vdmdemo.common.RemoteEventProto.StopAudio;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
final class AudioStreamer implements AutoCloseable {
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

    private final Context context;
    private final RemoteIo remoteIo;
    private final AudioManager audioManager;
    private final int playbackSessionId;

    private final Object lock = new Object();

    private final HandlerThread handlerThread = new HandlerThread("PolicyUpdater");
    private final Handler handler;

    @GuardedBy("lock")
    private AudioPolicy audioPolicy;

    @GuardedBy("lock")
    private AudioMix sessionIdAudioMix;

    @GuardedBy("lock")
    private AudioPolicy uidAudioPolicy;

    @GuardedBy("lock")
    private AudioMix uidAudioMix;

    @GuardedBy("lock")
    private StreamingThread streamingThread;

    @GuardedBy("lock")
    private AudioDeviceInfo remoteSubmixDevice;

    @GuardedBy("lock")
    private AudioRecord ghostRecord;

    private ImmutableSet<Integer> reroutedUids = ImmutableSet.of();

    private final AudioPlaybackCallback audioPlaybackCallback =
            new AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    super.onPlaybackConfigChanged(configs);

                    synchronized (lock) {
                        boolean shouldStream = configs.stream().anyMatch(
                                c -> STREAMING_PLAYER_STATES.contains(c.getPlayerState())
                                        && (reroutedUids.contains(c.getClientUid())
                                        || c.getSessionId() == playbackSessionId));
                        if (audioPolicy == null) {
                            Log.d(
                                    TAG,
                                    "There's no active audio policy, ignoring playback "
                                            + "config callback");
                            return;
                        }

                        if (sessionIdAudioMix != null && shouldStream && streamingThread == null) {
                            remoteIo.sendMessage(
                                    RemoteEvent.newBuilder()
                                            .setStartAudio(StartAudio.newBuilder())
                                            .build());
                            streamingThread =
                                    new StreamingThread(
                                            audioPolicy.createAudioRecordSink(sessionIdAudioMix),
                                            remoteIo);
                            streamingThread.start();
                        } else if (!shouldStream && streamingThread != null) {
                            remoteIo.sendMessage(
                                    RemoteEvent.newBuilder()
                                            .setStopAudio(StopAudio.newBuilder())
                                            .build());
                            streamingThread.stopStreaming();
                            joinUninterruptibly(streamingThread);
                            streamingThread = null;
                        }
                    }
                }
            };

    @Inject
    AudioStreamer(@ApplicationContext Context context, RemoteIo remoteIo) {
        this.context = context;
        this.remoteIo = remoteIo;
        audioManager = context.getSystemService(AudioManager.class);
        playbackSessionId = audioManager.generateAudioSessionId();
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, handler);
    }

    public void start() {
        handler.post(() -> registerAudioPolicy());
    }

    public int getPlaybackSessionId() {
        return playbackSessionId;
    }

    private void registerAudioPolicy() {
        AudioMixingRule mixingRule =
                new AudioMixingRule.Builder()
                        .addMixRule(AudioMixingRule.RULE_MATCH_AUDIO_SESSION_ID, playbackSessionId)
                        .build();
        AudioMix audioMix =
                new AudioMix.Builder(mixingRule)
                        .setRouteFlags(AudioMix.ROUTE_FLAG_LOOP_BACK)
                        .setFormat(AUDIO_FORMAT)
                        .build();

        synchronized (lock) {
            if (audioPolicy != null) {
                Log.w(TAG, "AudioPolicy is already registered");
                return;
            }
            audioPolicy = new AudioPolicy.Builder(context).addMix(audioMix).build();
            int ret = audioManager.registerAudioPolicy(audioPolicy);
            if (ret != AudioManager.SUCCESS) {
                Log.e(TAG, "Failed to register audio policy, error code " + ret);
                audioPolicy = null;
                return;
            }

            // This is a hacky way to determine audio device associated with audio mix.
            // once audio record for the policy is initialized, audio policy manager
            // will create remote submix instance so we can compare devices before and after
            // to determine which device corresponds to this particular mix.
            // The ghost audio record needs to be kept alive, releasing it would cause
            // destruction of remote submix instances and potential problems when updating the
            // UID-based render policy pointing to the remote submix device.
            ImmutableList<AudioDeviceInfo> preexistingRemoteSubmixDevices =
                    getRemoteSubmixDevices();
            ghostRecord = audioPolicy.createAudioRecordSink(audioMix);
            ghostRecord.startRecording();
            remoteSubmixDevice = getNewRemoteSubmixAudioDevice(preexistingRemoteSubmixDevices);
            sessionIdAudioMix = audioMix;
            if (!reroutedUids.isEmpty()) {
                registerUidPolicy(reroutedUids);
            }
        }
    }

    private @Nullable AudioDeviceInfo getNewRemoteSubmixAudioDevice(
            Collection<AudioDeviceInfo> preexistingDevices) {
        ImmutableSet<String> preexistingAddresses =
                preexistingDevices.stream()
                        .map(AudioDeviceInfo::getAddress)
                        .collect(toImmutableSet());

        ImmutableList<AudioDeviceInfo> newDevices =
                getRemoteSubmixDevices().stream()
                        .filter(dev -> !preexistingAddresses.contains(dev.getAddress()))
                        .collect(toImmutableList());

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

    public void updateVdmUids(ImmutableSet<Integer> uids) {
        Log.w(TAG, "Updating mixing rule to reroute uids " + uids);
        handler.post(
                () -> {
                    synchronized (lock) {
                        if (remoteSubmixDevice == null) {
                            Log.e(
                                    TAG,
                                    "Cannot update audio policy - remote submix device not known");
                            return;
                        }

                        if (reroutedUids.equals(uids)) {
                            Log.d(TAG, "Not updating UID audio policy for same set of UIDs");
                            return;
                        }

                        updateAudioPolicies(uids);
                    }
                });
    }

    // TODO(b/293611855) Use finer grained audio policy + mix controls once bugs are addressed
    // This shouldn't unregister all audio policies just to re-register them again.
    // That's inefficient and leads to audio leaks. But this is the most correct way to do this at
    // this time.
    @GuardedBy("lock")
    private void updateAudioPolicies(ImmutableSet<Integer> uids) {
        // TODO(b/293279299) Use Flagged API when available in google3
        // if (com.android.media.audio.flags.Flags.FLAG_AUDIO_POLICY_UPDATE_MIXING_RULES_API &&
        // uidAudioMix != null && (!reroutedUids.isEmpty() && !uids.isEmpty())) {
        //   Pair<AudioMix, AudioMixingRule> update = Pair.create(uidAudioMix,
        // createUidMixingRule(uids));
        //   uidAudioPolicy.updateMixingRules(Collections.singletonList(update));
        //   return;
        // }

        if (audioPolicy != null) {
            audioManager.unregisterAudioPolicy(audioPolicy);
            audioPolicy = null;
        }
        if (uidAudioPolicy != null) {
            audioManager.unregisterAudioPolicy(uidAudioPolicy);
            uidAudioPolicy = null;
        }

        reroutedUids = ImmutableSet.copyOf(uids);
        registerAudioPolicy();
    }

    @GuardedBy("lock")
    private void registerUidPolicy(ImmutableSet<Integer> uids) {
        uidAudioMix =
                new AudioMix.Builder(createUidMixingRule(uids))
                        .setRouteFlags(AudioMix.ROUTE_FLAG_RENDER)
                        .setDevice(remoteSubmixDevice)
                        .setFormat(AUDIO_FORMAT)
                        .build();
        AudioPolicy uidPolicy = new AudioPolicy.Builder(context).addMix(uidAudioMix).build();
        int ret = audioManager.registerAudioPolicy(uidPolicy);
        if (ret != AudioManager.SUCCESS) {
            Log.e(TAG, "Error " + ret + "while trying to register UID policy");
            return;
        }

        uidAudioPolicy = uidPolicy;
        reroutedUids = ImmutableSet.copyOf(uids);
    }

    public void stop() {
        synchronized (lock) {
            if (streamingThread != null) {
                streamingThread.stopStreaming();
                joinUninterruptibly(streamingThread);
                streamingThread = null;
            }
            if (uidAudioPolicy != null) {
                audioManager.unregisterAudioPolicy(uidAudioPolicy);
                uidAudioPolicy = null;
            }
            if (audioPolicy != null) {
                audioManager.unregisterAudioPolicy(audioPolicy);
                audioPolicy = null;
            }
            if (ghostRecord != null) {
                ghostRecord.stop();
                ghostRecord.release();
                ghostRecord = null;
            }
        }
    }

    @Override
    public void close() {
        audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);
        stop();
        handlerThread.quitSafely();
    }

    private AudioMixingRule createUidMixingRule(Collection<Integer> uids) {
        AudioMixingRule.Builder builder = new AudioMixingRule.Builder();
        uids.forEach(uid -> builder.addMixRule(AudioMixingRule.RULE_MATCH_UID, uid));
        return builder.build();
    }

    private ImmutableList<AudioDeviceInfo> getRemoteSubmixDevices() {
        return Arrays.stream(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
                .filter(AudioStreamer::deviceIsRemoteSubmixOut)
                .collect(toImmutableList());
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
        private final RemoteIo remoteIo;
        private final AudioRecord audioRecord;
        private final AtomicBoolean isRunning = new AtomicBoolean(true);

        public StreamingThread(AudioRecord audioRecord, RemoteIo remoteIo) {
            super();
            this.remoteIo = Objects.requireNonNull(remoteIo);
            this.audioRecord = Objects.requireNonNull(audioRecord);
        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "Starting audio streaming");

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Audio record is not initialized");
                return;
            }

            audioRecord.startRecording();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (isRunning.get()) {
                int ret = audioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
                if (ret <= 0) {
                    Log.e(TAG, "AudioRecord.read returned error code " + ret);
                    continue;
                }

                remoteIo.sendMessage(
                        RemoteEvent.newBuilder()
                                .setAudioFrame(
                                        AudioFrame.newBuilder()
                                                .setData(ByteString.copyFrom(buffer, 0, ret)))
                                .build());
            }
            Log.d(TAG, "Stopping audio streaming");
            audioRecord.stop();
            audioRecord.release();
        }

        public void stopStreaming() {
            isRunning.set(false);
        }
    }

    private static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
        return Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf);
    }

    private static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() {
        return Collectors.collectingAndThen(Collectors.toList(), ImmutableSet::copyOf);
    }
}
