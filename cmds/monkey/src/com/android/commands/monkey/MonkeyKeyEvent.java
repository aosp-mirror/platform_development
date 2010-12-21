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
import android.os.RemoteException;
import android.view.IWindowManager;
import android.view.KeyEvent;
/**
 * monkey key event
 */
public class MonkeyKeyEvent extends MonkeyEvent {
    private long mDownTime = -1;
    private int mMetaState = -1;
    private int mAction = -1;
    private int mKeyCode = -1;
    private int mScancode = -1;
    private int mRepeatCount = -1;
    private int mDeviceId = -1;
    private long mEventTime = -1;

    private KeyEvent keyEvent = null;

    public MonkeyKeyEvent(int action, int keycode) {
        super(EVENT_TYPE_KEY);
        mAction = action;
        mKeyCode = keycode;
    }

    public MonkeyKeyEvent(KeyEvent e) {
        super(EVENT_TYPE_KEY);
        keyEvent = e;
    }

    public MonkeyKeyEvent(long downTime, long eventTime, int action,
            int code, int repeat, int metaState,
            int device, int scancode) {
        super(EVENT_TYPE_KEY);

        mAction = action;
        mKeyCode = code;
        mMetaState = metaState;
        mScancode = scancode;
        mRepeatCount = repeat;
        mDeviceId = device;
        mDownTime = downTime;
        mEventTime = eventTime;
    }

    public int getKeyCode() {
        return mKeyCode;
    }

    public int getAction() {
        return mAction;
    }

    public long getDownTime() {
        return mDownTime;
    }

    public long getEventTime() {
        return mEventTime;
    }

    public void setDownTime(long downTime) {
        mDownTime = downTime;
    }

    public void setEventTime(long eventTime) {
        mEventTime = eventTime;
    }

    /**
     * @return the key event
     */
    private KeyEvent getEvent() {
        if (keyEvent == null) {
            if (mDeviceId < 0) {
                keyEvent = new KeyEvent(mAction, mKeyCode);
            } else {
                // for scripts
                keyEvent = new KeyEvent(mDownTime, mEventTime, mAction,
                                        mKeyCode, mRepeatCount, mMetaState, mDeviceId, mScancode);
            }
        }
        return keyEvent;
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

        // inject key event
        try {
            if (!iwm.injectKeyEvent(getEvent(), false)) {
                return MonkeyEvent.INJECT_FAIL;
            }
        } catch (RemoteException ex) {
            return MonkeyEvent.INJECT_ERROR_REMOTE_EXCEPTION;
        }

        return MonkeyEvent.INJECT_SUCCESS;
    }
}
