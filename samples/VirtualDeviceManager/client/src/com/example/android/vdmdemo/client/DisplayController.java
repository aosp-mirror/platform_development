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

  private final int displayId;
  private final RemoteIo remoteIo;

  private VideoManager videoManager = null;

  private int dpi = DPI;
  private RemoteEvent displayCapabilities;

  DisplayController(int displayId, RemoteIo remoteIo) {
    this.displayId = displayId;
    this.remoteIo = remoteIo;
  }

  void setDpi(int dpi) {
    this.dpi = dpi;
  }

  int getDpi() {
    return dpi;
  }

  void close() {
    remoteIo.sendMessage(
        RemoteEvent.newBuilder()
            .setDisplayId(displayId)
            .setStopStreaming(StopStreaming.newBuilder())
            .build());

    if (videoManager != null) {
      videoManager.stop();
    }
  }

  void pause() {
    if (videoManager == null) {
      return;
    }
    videoManager.stop();
    videoManager = null;

    remoteIo.sendMessage(
        RemoteEvent.newBuilder()
            .setDisplayId(displayId)
            .setStopStreaming(StopStreaming.newBuilder().setPause(true))
            .build());
  }

  void sendDisplayCapabilities() {
    remoteIo.sendMessage(displayCapabilities);
  }

  void setSurface(Surface surface, int width, int height) {
    if (videoManager != null) {
      videoManager.stop();
    }
    videoManager = VideoManager.createDecoder(displayId, remoteIo);
    videoManager.startDecoding(surface, width, height);
    displayCapabilities =
        RemoteEvent.newBuilder()
            .setDisplayId(displayId)
            .setDisplayCapabilities(
                DisplayCapabilities.newBuilder()
                    .setViewportWidth(width)
                    .setViewportHeight(height)
                    .setDensityDpi(dpi))
            .build();
    sendDisplayCapabilities();
  }
}
