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

import android.graphics.PointF;
import android.hardware.input.VirtualMouseButtonEvent;
import android.hardware.input.VirtualMouseRelativeEvent;
import android.hardware.input.VirtualMouseScrollEvent;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.MotionEvent;

import androidx.annotation.GuardedBy;

import com.example.android.vdmdemo.common.RemoteEventProto.InputDeviceType;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
final class InputController {

    private static final String TAG = "InputController";

    @Inject DisplayRepository mDisplayRepository;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mFocusedRemoteDisplayId = Display.INVALID_DISPLAY;

    @GuardedBy("mLock")
    private RemoteDisplay mFocusedDisplay;

    @Inject
    InputController() {}

    void setFocusedRemoteDisplayId(int remoteDisplayId) {
        synchronized (mLock) {
            mFocusedRemoteDisplayId = remoteDisplayId;
            mFocusedDisplay =
                    mDisplayRepository.getDisplayByRemoteId(remoteDisplayId).orElse(null);
        }
    }

    void sendEventToFocusedDisplay(InputDeviceType deviceType, InputEvent inputEvent) {
        synchronized (mLock) {
            getFocusedDisplayLocked().ifPresent(d -> d.processInputEvent(deviceType, inputEvent));
        }
    }

    void sendHomeToFocusedDisplay() {
        synchronized (mLock) {
            getFocusedDisplayLocked().ifPresent(d -> d.goHome());
        }
    }

    void sendMouseButtonEvent(int button) {
        for (int action : new int[]{
                MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE}) {
            sendMouseEventToFocusedDisplay(
                    new VirtualMouseButtonEvent.Builder()
                            .setButtonCode(button)
                            .setAction(action)
                            .build());
        }
    }

    void sendMouseRelativeEvent(float x, float y) {
        sendMouseEventToFocusedDisplay(
                new VirtualMouseRelativeEvent.Builder()
                        .setRelativeX(x)
                        .setRelativeY(y)
                        .build());
    }

    void sendMouseScrollEvent(float x, float y) {
        sendMouseEventToFocusedDisplay(
                new VirtualMouseScrollEvent.Builder()
                        .setXAxisMovement(clampMouseScroll(x))
                        .setYAxisMovement(clampMouseScroll(y))
                        .build());
    }

    private void sendMouseEventToFocusedDisplay(Object mouseEvent) {
        synchronized (mLock) {
            getFocusedDisplayLocked().ifPresent(d -> d.processVirtualMouseEvent(mouseEvent));
        }
    }

    void sendStylusEventToFocusedDisplay(Object stylusEvent) {
        synchronized (mLock) {
            getFocusedDisplayLocked().ifPresent(d -> d.processVirtualStylusEvent(stylusEvent));
        }
    }

    Optional<PointF> getFocusedDisplaySize() {
        synchronized (mLock) {
            Optional<RemoteDisplay> display = getFocusedDisplayLocked();
            return display.isPresent()
                    ? Optional.of(display.get().getDisplaySize())
                    : Optional.empty();
        }
    }

    @GuardedBy("mLock")
    private Optional<RemoteDisplay> getFocusedDisplayLocked() {
        synchronized (mLock) {
            if (mFocusedDisplay == null) {
                mFocusedDisplay = mDisplayRepository.getDisplayByRemoteId(mFocusedRemoteDisplayId)
                        .orElse(null);
                if (mFocusedDisplay == null) {
                    Log.e(TAG, "Failed to inject input event, no focused display "
                            + mFocusedRemoteDisplayId);
                    return Optional.empty();
                }
            }
            return Optional.of(mFocusedDisplay);
        }
    }

    private static float clampMouseScroll(float val) {
        return Math.max(Math.min(val, 1f), -1f);
    }
}
