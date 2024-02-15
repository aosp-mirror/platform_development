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

import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.GuardedBy;

import com.example.android.vdmdemo.common.RemoteEventProto.DisplayChangeEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.InputDeviceType;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteHomeEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteInputEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteKeyEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteMotionEvent;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Maintains focused display and handles injection of targeted and untargeted input events. */
@Singleton
final class InputManager {
    private static final String TAG = "InputManager";

    private final RemoteIo mRemoteIo;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mFocusedDisplayId = Display.INVALID_DISPLAY;

    interface FocusListener {
        void onFocusChange(int focusedDisplayId);
    }

    @GuardedBy("mLock")
    private final List<FocusListener> mFocusListeners = new ArrayList<>();

    @GuardedBy("mLock")
    private final Set<Integer> mFocusableDisplays = new HashSet<>();

    @Inject
    InputManager(RemoteIo remoteIo) {
        mRemoteIo = remoteIo;
    }

    void addFocusListener(FocusListener focusListener) {
        synchronized (mLock) {
            mFocusListeners.add(focusListener);
        }
    }

    void removeFocusListener(FocusListener focusListener) {
        synchronized (mLock) {
            mFocusListeners.remove(focusListener);
        }
    }

    void addFocusableDisplay(int displayId) {
        synchronized (mLock) {
            if (mFocusableDisplays.add(displayId)) {
                setFocusedDisplayId(displayId);
            }
        }
    }

    void removeFocusableDisplay(int displayId) {
        synchronized (mLock) {
            mFocusableDisplays.remove(displayId);
            if (displayId == mFocusedDisplayId) {
                setFocusedDisplayId(updateFocusedDisplayId());
            }
        }
    }

    /** Injects {@link InputEvent} for the given {@link InputDeviceType} into the given display. */
    void sendInputEvent(InputDeviceType deviceType, InputEvent inputEvent, int displayId) {
        if (inputEvent instanceof MotionEvent) {
            MotionEvent event = (MotionEvent) inputEvent;
            switch (deviceType) {
                case DEVICE_TYPE_NAVIGATION_TOUCHPAD:
                case DEVICE_TYPE_TOUCHSCREEN:
                    sendTouchEvent(deviceType, event, displayId);
                    break;
                case DEVICE_TYPE_MOUSE:
                    sendMouseEvent(event, displayId);
                    break;
                default:
                    Log.e(TAG, "sendInputEvent got invalid device type " + deviceType.getNumber());
            }
        } else {
            KeyEvent event = (KeyEvent) inputEvent;
            sendKeyEvent(deviceType, event, displayId);
        }
    }

    /**
     * Injects {@link InputEvent} for the given {@link InputDeviceType} into the focused display.
     *
     * @return whether the event was sent.
     */
    public boolean sendInputEventToFocusedDisplay(
            InputDeviceType deviceType, InputEvent inputEvent) {
        int targetDisplayId;
        synchronized (mLock) {
            if (mFocusedDisplayId == Display.INVALID_DISPLAY) {
                return false;
            }
            targetDisplayId = mFocusedDisplayId;
        }
        sendInputEvent(deviceType, inputEvent, targetDisplayId);
        return true;
    }

    void sendBack(int displayId) {
        setFocusedDisplayId(displayId);
        for (int action : new int[] {KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP}) {
            sendInputEvent(
                    RemoteInputEvent.newBuilder()
                            .setDeviceType(InputDeviceType.DEVICE_TYPE_DPAD)
                            .setKeyEvent(
                                    RemoteKeyEvent.newBuilder()
                                            .setAction(action)
                                            .setKeyCode(KeyEvent.KEYCODE_BACK))
                            .build(),
                    displayId);
        }
    }

    void sendHome(int displayId) {
        setFocusedDisplayId(displayId);
        mRemoteIo.sendMessage(
                RemoteEvent.newBuilder()
                        .setDisplayId(displayId)
                        .setHomeEvent(RemoteHomeEvent.newBuilder())
                        .build());
    }

    private void sendTouchEvent(InputDeviceType deviceType, MotionEvent event, int displayId) {
        setFocusedDisplayId(displayId);
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
            sendInputEvent(
                    RemoteInputEvent.newBuilder()
                            .setDeviceType(deviceType)
                            .setTimestampMs(event.getEventTime())
                            .setTouchEvent(
                                    RemoteMotionEvent.newBuilder()
                                            .setPointerId(event.getPointerId(pointerIndex))
                                            .setAction(event.getActionMasked())
                                            .setX(event.getX(pointerIndex))
                                            .setY(event.getY(pointerIndex))
                                            .setPressure(event.getPressure(pointerIndex)))
                            .build(),
                    displayId);
        }
    }

    private void sendKeyEvent(InputDeviceType deviceType, KeyEvent event, int displayId) {
        sendInputEvent(
                RemoteInputEvent.newBuilder()
                        .setDeviceType(deviceType)
                        .setTimestampMs(event.getEventTime())
                        .setKeyEvent(
                                RemoteKeyEvent.newBuilder()
                                        .setAction(event.getAction())
                                        .setKeyCode(event.getKeyCode())
                                        .build())
                        .build(),
                displayId);
    }

    private void sendMouseEvent(MotionEvent event, int displayId) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_BUTTON_PRESS:
            case MotionEvent.ACTION_BUTTON_RELEASE:
                RemoteInputEvent buttonEvent =
                        RemoteInputEvent.newBuilder()
                                .setTimestampMs(System.currentTimeMillis())
                                .setDeviceType(InputDeviceType.DEVICE_TYPE_MOUSE)
                                .setMouseButtonEvent(
                                        RemoteKeyEvent.newBuilder()
                                                .setAction(event.getAction())
                                                .setKeyCode(event.getActionButton())
                                                .build())
                                .build();
                sendInputEvent(buttonEvent, displayId);
                break;
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_HOVER_MOVE:
                setFocusedDisplayId(displayId);
                RemoteInputEvent relativeEvent =
                        RemoteInputEvent.newBuilder()
                                .setTimestampMs(System.currentTimeMillis())
                                .setDeviceType(InputDeviceType.DEVICE_TYPE_MOUSE)
                                .setMouseRelativeEvent(
                                        RemoteMotionEvent.newBuilder()
                                                .setX(event.getX())
                                                .setY(event.getY())
                                                .build())
                                .build();
                sendInputEvent(relativeEvent, displayId);
                break;
            case MotionEvent.ACTION_SCROLL:
                float scrollX = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                float scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                RemoteInputEvent scrollEvent =
                        RemoteInputEvent.newBuilder()
                                .setTimestampMs(System.currentTimeMillis())
                                .setDeviceType(InputDeviceType.DEVICE_TYPE_MOUSE)
                                .setMouseScrollEvent(
                                        RemoteMotionEvent.newBuilder()
                                                .setX(clampMouseScroll(scrollX))
                                                .setY(clampMouseScroll(scrollY))
                                                .build())
                                .build();
                sendInputEvent(scrollEvent, displayId);
                break;
        }
    }

    private void sendInputEvent(RemoteInputEvent inputEvent, int displayId) {
        mRemoteIo.sendMessage(
                RemoteEvent.newBuilder().setDisplayId(displayId).setInputEvent(inputEvent).build());
    }

    private static float clampMouseScroll(float val) {
        return Math.max(Math.min(val, 1f), -1f);
    }

    private int updateFocusedDisplayId() {
        synchronized (mLock) {
            if (mFocusableDisplays.contains(mFocusedDisplayId)) {
                return mFocusedDisplayId;
            }
            return Iterables.getFirst(mFocusableDisplays, Display.INVALID_DISPLAY);
        }
    }

    void setFocusedDisplayId(int displayId) {
        List<FocusListener> listenersToNotify = Collections.emptyList();
        boolean focusedDisplayChanged = false;
        synchronized (mLock) {
            if (displayId != mFocusedDisplayId) {
                mFocusedDisplayId = displayId;
                listenersToNotify = new ArrayList<>(mFocusListeners);
                focusedDisplayChanged = true;
            }
        }
        if (focusedDisplayChanged) {
            mRemoteIo.sendMessage(RemoteEvent.newBuilder()
                    .setDisplayId(displayId)
                    .setDisplayChangeEvent(DisplayChangeEvent.newBuilder().setFocused(true))
                    .build());
        }
        for (FocusListener focusListener : listenersToNotify) {
            focusListener.onFocusChange(displayId);
        }
    }
}
