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

package com.example.android.vdmdemo.client;

import android.view.Surface;

import com.example.android.vdmdemo.common.RemoteEventProto.DisplayCapabilities;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.StopStreaming;
import com.example.android.vdmdemo.common.RemoteIo;
import com.example.android.vdmdemo.common.VideoManager;

final class DisplayController {
    private static final int DPI = 300;

    private final int mDisplayId;
    private final RemoteIo mRemoteIo;

    private VideoManager mVideoManager = null;

    private int mDpi = DPI;
    private RemoteEvent mDisplayCapabilities;

    DisplayController(int displayId, RemoteIo remoteIo) {
        mDisplayId = displayId;
        mRemoteIo = remoteIo;
    }

    void setDpi(int dpi) {
        mDpi = dpi;
    }

    void close() {
        mRemoteIo.sendMessage(
                RemoteEvent.newBuilder()
                        .setDisplayId(mDisplayId)
                        .setStopStreaming(StopStreaming.newBuilder())
                        .build());

        if (mVideoManager != null) {
            mVideoManager.stop();
        }
    }

    void pause() {
        if (mVideoManager == null) {
            return;
        }
        mVideoManager.stop();
        mVideoManager = null;

        mRemoteIo.sendMessage(
                RemoteEvent.newBuilder()
                        .setDisplayId(mDisplayId)
                        .setStopStreaming(StopStreaming.newBuilder().setPause(true))
                        .build());
    }

    void sendDisplayCapabilities() {
        mRemoteIo.sendMessage(mDisplayCapabilities);
    }

    void setSurface(Surface surface, int width, int height) {
        if (mVideoManager != null) {
            mVideoManager.stop();
        }
        mVideoManager = VideoManager.createDisplayDecoder(mDisplayId, mRemoteIo);
        mVideoManager.startDecoding(surface, width, height);
        mDisplayCapabilities =
                RemoteEvent.newBuilder()
                        .setDisplayId(mDisplayId)
                        .setDisplayCapabilities(
                                DisplayCapabilities.newBuilder()
                                        .setViewportWidth(width)
                                        .setViewportHeight(height)
                                        .setDensityDpi(mDpi))
                        .build();
        sendDisplayCapabilities();
    }
}
