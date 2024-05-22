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

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.AudioTrack;
import android.media.audiopolicy.AudioMix;
import android.media.audiopolicy.AudioMixingRule;
import android.media.audiopolicy.AudioPolicy;
import android.util.Log;

import androidx.annotation.GuardedBy;

import com.example.android.vdmdemo.common.RemoteEventProto;
import com.example.android.vdmdemo.common.RemoteEventProto.AudioFrame;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.common.collect.ImmutableSet;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final List<AudioTrack> mAudioTracks = new ArrayList<>();
    private final RemoteIo mRemoteIo;
    private int mRecordingSessionId;
    private boolean mIsPlaying;
    @GuardedBy("mLock")
    private ImmutableSet<Integer> mReroutedUids = ImmutableSet.of();
    private AudioPolicy mAudioPolicy;
    private final Context mApplicationContext;
    private Context mDeviceContext;
    private AudioManager mAudioManager;

    private final AudioManager.AudioRecordingCallback mAudioRecordingCallback =
            new AudioManager.AudioRecordingCallback() {
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
        mDeviceContext = mApplicationContext.createDeviceContext(deviceId)
                .createDeviceContext(deviceId);
        mRecordingSessionId = audioSessionId;
        mAudioManager = mDeviceContext.getSystemService(AudioManager.class);

        mRemoteIo.addMessageConsumer(this);
        mAudioManager.registerAudioRecordingCallback(mAudioRecordingCallback, null);
        synchronized (mLock) {
            updateAudioPolicies(mReroutedUids);
        }
    }

    /**
     * Stop the AudioInjector
     */
    public void stop() {
        synchronized (mLock) {
            if (mIsPlaying) {
                mIsPlaying = false;
                mRemoteIo.sendMessage(RemoteEvent.newBuilder().setStopAudioInput(
                        RemoteEventProto.StopAudioInput.newBuilder().build()).build());
            }
            for (AudioTrack audioTrack : mAudioTracks) {
                audioTrack.stop();
                audioTrack.release();
            }
            mAudioTracks.clear();

            if (mAudioManager != null) {
                mRemoteIo.removeMessageConsumer(this);
                mAudioManager.unregisterAudioRecordingCallback(mAudioRecordingCallback);

                if (mAudioPolicy != null) {
                    mAudioManager.unregisterAudioPolicy(mAudioPolicy);
                }
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
    public void updateVdmUids(Set<Integer> updatedUids) {
        synchronized (mLock) {
            if (mAudioPolicy == null) {
                Log.e(
                        TAG,
                        "Not updating AudioPolicy - no audio policy configured");
                return;
            }

            if (mReroutedUids.equals(updatedUids)) {
                Log.d(TAG, "Not updating UID audio policy for same set of UIDs");
                return;
            }

            updateAudioPolicies(updatedUids);
        }
    }

    private void updateAudioPolicies(Set<Integer> updatedUids) {
        synchronized (mLock) {
            for (AudioTrack audioTrack : mAudioTracks) {
                audioTrack.stop();
                audioTrack.release();
            }
            mAudioTracks.clear();

            if (mAudioPolicy != null) {
                mAudioManager.unregisterAudioPolicy(mAudioPolicy);
            }

            mReroutedUids = ImmutableSet.copyOf(updatedUids);
            registerAudioPolicy();
        }
    }

    private void registerAudioPolicy() {
        synchronized (mLock) {
            AudioMix sessionIdAudioMix = getSessionIdAudioMix(mRecordingSessionId);
            AudioMix uidAudioMix = getUidAudioMix(mReroutedUids);

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
            addAudioTrack(mAudioPolicy.createAudioTrackSource(sessionIdAudioMix));
            if (uidAudioMix != null) {
                addAudioTrack(mAudioPolicy.createAudioTrackSource(uidAudioMix));
            }
        }
    }

    private void addAudioTrack(AudioTrack audioTrack) {
        synchronized (mLock) {
            if (audioTrack.getState() != STATE_INITIALIZED) {
                throw new IllegalStateException("set an uninitialized AudioTrack.");
            }

            if (mIsPlaying) {
                audioTrack.play();
            }
            mAudioTracks.add(audioTrack);
        }
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

        AudioMixingRule.Builder uidMixingRule = new AudioMixingRule.Builder()
                .setTargetMixRole(AudioMixingRule.MIX_ROLE_INJECTOR);
        for (Integer reroutedUid : reroutedUids) {
            uidMixingRule.addMixRule(AudioMixingRule.RULE_MATCH_UID, reroutedUid);
        }

        return new AudioMix.Builder(uidMixingRule.build())
                .setRouteFlags(AudioMix.ROUTE_FLAG_LOOP_BACK)
                .setFormat(AUDIO_FORMAT_IN)
                .build();
    }
}
