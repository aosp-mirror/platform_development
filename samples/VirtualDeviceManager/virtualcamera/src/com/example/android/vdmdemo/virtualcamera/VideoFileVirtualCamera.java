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

package com.example.android.vdmdemo.virtualcamera;

import android.companion.virtual.camera.VirtualCameraCallback;
import android.content.Context;
import android.media.MediaPlayer;
import android.view.Surface;

import androidx.annotation.NonNull;

import dagger.hilt.android.qualifiers.ApplicationContext;

import java.util.Objects;

import javax.inject.Inject;

/** Implementation of virtual camera callback playing video on camera's Surface. */
public class VideoFileVirtualCamera implements VirtualCameraCallback {

    private final Context mContext;
    private MediaPlayer mMediaPlayer = null;

    @Inject
    VideoFileVirtualCamera(@ApplicationContext Context context) {
        this.mContext = context;
    }

    @Override
    public void onStreamConfigured(int streamId, @NonNull Surface surface, int width, int height,
            int format) {
        mMediaPlayer = Objects.requireNonNull(MediaPlayer.create(mContext,
                                    R.raw.testvideo));
        mMediaPlayer.setLooping(true);
        mMediaPlayer.setSurface(surface);
        mMediaPlayer.start();
    }

    @Override
    public void onStreamClosed(int streamId) {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
    }
}
