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

import android.util.Log;
import android.view.Display;
import android.view.InputEvent;

import androidx.annotation.GuardedBy;

import com.example.android.vdmdemo.common.RemoteEventProto.InputDeviceType;

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
            if (mFocusedDisplay == null) {
                mFocusedDisplay = mDisplayRepository.getDisplayByRemoteId(mFocusedRemoteDisplayId)
                        .orElse(null);
                if (mFocusedDisplay == null) {
                    Log.e(TAG, "Failed to inject input event, no focused display "
                            + mFocusedRemoteDisplayId);
                    return;
                }
            }
            mFocusedDisplay.processInputEvent(deviceType, inputEvent);
        }
    }
}
