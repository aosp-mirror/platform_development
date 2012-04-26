/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.commands.monkey;

import android.app.IActivityManager;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
/**
 * monkey key event
 */
public class MonkeyKeyEvent extends MonkeyEvent {
    private int mDeviceId;
    private long mEventTime;
    private long mDownTime;
    private int mAction;
    private int mKeyCode;
    private int mScanCode;
    private int mMetaState;
    private int mRepeatCount;

    private KeyEvent mKeyEvent;

    public MonkeyKeyEvent(int action, int keyCode) {
        this(-1, -1, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0);
    }

    public MonkeyKeyEvent(long downTime, long eventTime, int action,
            int keyCode, int repeatCount, int metaState,
            int device, int scanCode) {
        super(EVENT_TYPE_KEY);
        mDownTime = downTime;
        mEventTime = eventTime;
        mAction = action;
        mKeyCode = keyCode;
        mRepeatCount = repeatCount;
        mMetaState = metaState;
        mDeviceId = device;
        mScanCode = scanCode;
    }

    public MonkeyKeyEvent(KeyEvent e) {
        super(EVENT_TYPE_KEY);
        mKeyEvent = e;
    }

    public int getKeyCode() {
        return mKeyEvent != null ? mKeyEvent.getKeyCode() : mKeyCode;
    }

    public int getAction() {
        return mKeyEvent != null ? mKeyEvent.getAction() : mAction;
    }

    public long getDownTime() {
        return mKeyEvent != null ? mKeyEvent.getDownTime() : mDownTime;
    }

    public long getEventTime() {
        return mKeyEvent != null ? mKeyEvent.getEventTime() : mEventTime;
    }

    public void setDownTime(long downTime) {
        if (mKeyEvent != null) {
            throw new IllegalStateException("Cannot modify down time of this key event.");
        }
        mDownTime = downTime;
    }

    public void setEventTime(long eventTime) {
        if (mKeyEvent != null) {
            throw new IllegalStateException("Cannot modify event time of this key event.");
        }
        mEventTime = eventTime;
    }

    @Override
    public boolean isThrottlable() {
        return (getAction() == KeyEvent.ACTION_UP);
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (verbose > 1) {
            String note;
            if (mAction == KeyEvent.ACTION_UP) {
                note = "ACTION_UP";
            } else {
                note = "ACTION_DOWN";
            }

            try {
                System.out.println(":Sending Key (" + note + "): "
                        + mKeyCode + "    // "
                        + MonkeySourceRandom.getKeyName(mKeyCode));
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println(":Sending Key (" + note + "): "
                        + mKeyCode + "    // Unknown key event");
            }
        }

        KeyEvent keyEvent = mKeyEvent;
        if (keyEvent == null) {
            long eventTime = mEventTime;
            if (eventTime <= 0) {
                eventTime = SystemClock.uptimeMillis();
            }
            long downTime = mDownTime;
            if (downTime <= 0) {
                downTime = eventTime;
            }
            keyEvent = new KeyEvent(downTime, eventTime, mAction, mKeyCode,
                    mRepeatCount, mMetaState, mDeviceId, mScanCode,
                    KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD);
        }
        if (!InputManager.getInstance().injectInputEvent(keyEvent,
                InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT)) {
            return MonkeyEvent.INJECT_FAIL;
        }
        return MonkeyEvent.INJECT_SUCCESS;
    }
}
