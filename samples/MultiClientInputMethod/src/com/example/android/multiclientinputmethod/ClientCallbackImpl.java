/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.android.multiclientinputmethod;

import android.inputmethodservice.MultiClientInputMethodServiceDelegate;
import android.os.Bundle;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

final class ClientCallbackImpl implements MultiClientInputMethodServiceDelegate.ClientCallback {
    private static final String TAG = "ClientCallbackImpl";
    private static final boolean DEBUG = false;

    private final MultiClientInputMethodServiceDelegate mDelegate;
    private final SoftInputWindowManager mSoftInputWindowManager;
    private final int mClientId;
    private final int mUid;
    private final int mPid;
    private final int mSelfReportedDisplayId;
    private final KeyEvent.DispatcherState mDispatcherState;
    private final Looper mLooper;
    private final MultiClientInputMethod mInputMethod;

    ClientCallbackImpl(MultiClientInputMethod inputMethod,
            MultiClientInputMethodServiceDelegate delegate,
            SoftInputWindowManager softInputWindowManager, int clientId, int uid, int pid,
            int selfReportedDisplayId) {
        mInputMethod = inputMethod;
        mDelegate = delegate;
        mSoftInputWindowManager = softInputWindowManager;
        mClientId = clientId;
        mUid = uid;
        mPid = pid;
        mSelfReportedDisplayId = selfReportedDisplayId;
        mDispatcherState = new KeyEvent.DispatcherState();
        // For simplicity, we use the main looper for this sample.
        // To use other looper thread, make sure that the IME Window also runs on the same looper
        // and introduce an appropriate synchronization mechanism instead of directly accessing
        // MultiClientInputMethod#mLastClientId.
        mLooper = Looper.getMainLooper();
    }

    KeyEvent.DispatcherState getDispatcherState() {
        return mDispatcherState;
    }

    Looper getLooper() {
        return mLooper;
    }

    @Override
    public void onAppPrivateCommand(String action, Bundle data) {
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
    }

    @Override
    public void onFinishSession() {
        if (DEBUG) {
            Log.v(TAG, "onFinishSession clientId=" + mClientId);
        }
        final SoftInputWindow window =
                mSoftInputWindowManager.getSoftInputWindow(mSelfReportedDisplayId);
        if (window == null) {
            return;
        }
        // SoftInputWindow also needs to be cleaned up when this IME client is still associated with
        // it.
        if (mClientId == window.getClientId()) {
            window.onFinishClient();
        }
    }

    @Override
    public void onHideSoftInput(int flags, ResultReceiver resultReceiver) {
        if (DEBUG) {
            Log.v(TAG, "onHideSoftInput clientId=" + mClientId + " flags=" + flags);
        }
        final SoftInputWindow window =
                mSoftInputWindowManager.getSoftInputWindow(mSelfReportedDisplayId);
        if (window == null) {
            return;
        }
        // Seems that the Launcher3 has a bug to call onHideSoftInput() too early so we cannot
        // enforce clientId check yet.
        // TODO: Check clientId like we do so for onShowSoftInput().
        window.hide();
    }

    @Override
    public void onShowSoftInput(int flags, ResultReceiver resultReceiver) {
        if (DEBUG) {
            Log.v(TAG, "onShowSoftInput clientId=" + mClientId + " flags=" + flags);
        }
        final SoftInputWindow window =
                mSoftInputWindowManager.getSoftInputWindow(mSelfReportedDisplayId);
        if (window == null) {
            return;
        }
        if (mClientId != window.getClientId()) {
            Log.w(TAG, "onShowSoftInput() from a background client is ignored."
                    + " windowClientId=" + window.getClientId()
                    + " clientId=" + mClientId);
            return;
        }
        window.show();
    }

    @Override
    public void onStartInputOrWindowGainedFocus(InputConnection inputConnection,
            EditorInfo editorInfo, int startInputFlags, int softInputMode, int targetWindowHandle) {
        if (DEBUG) {
            Log.v(TAG, "onStartInputOrWindowGainedFocus clientId=" + mClientId
                    + " editorInfo=" + editorInfo
                    + " startInputFlags="
                    + InputMethodDebug.startInputFlagsToString(startInputFlags)
                    + " softInputMode=" + InputMethodDebug.softInputModeToString(softInputMode)
                    + " targetWindowHandle=" + targetWindowHandle);
        }

        final int state = softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE;
        final boolean forwardNavigation =
                (softInputMode & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION) != 0;

        final SoftInputWindow window =
                mSoftInputWindowManager.getOrCreateSoftInputWindow(mSelfReportedDisplayId);
        if (window == null) {
            return;
        }

        if (window.getTargetWindowHandle() != targetWindowHandle) {
            // Target window has changed.  Report new IME target window to the system.
            mDelegate.reportImeWindowTarget(
                    mClientId, targetWindowHandle, window.getWindow().getAttributes().token);
        }
        if (inputConnection == null || editorInfo == null) {
            // deactivate previous client.
            if (mInputMethod.mLastClientId != mClientId) {
                mDelegate.setActive(mInputMethod.mLastClientId, false /* active */);
            }
            // Dummy InputConnection case.
            if (window.getClientId() == mClientId) {
                // Special hack for temporary focus changes (e.g. notification shade).
                // If we have already established a connection to this client, and if a dummy
                // InputConnection is notified, just ignore this event.
            } else {
                window.onDummyStartInput(mClientId, targetWindowHandle);
            }
        } else {
            if (mInputMethod.mLastClientId != mClientId) {
                mDelegate.setActive(mClientId, true /* active */);
            }
            window.onStartInput(mClientId, targetWindowHandle, inputConnection);
        }

        switch (state) {
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE:
                if (forwardNavigation) {
                    window.show();
                }
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE:
                window.show();
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN:
                if (forwardNavigation) {
                    window.hide();
                }
                break;
            case WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN:
                window.hide();
                break;
        }
        mInputMethod.mLastClientId = mClientId;
    }

    @Override
    public void onToggleSoftInput(int showFlags, int hideFlags) {
        // TODO: Implement
        Log.w(TAG, "onToggleSoftInput is not yet implemented. clientId=" + mClientId
                + " showFlags=" + showFlags + " hideFlags=" + hideFlags);
    }

    @Override
    public void onUpdateCursorAnchorInfo(CursorAnchorInfo info) {
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (DEBUG) {
            Log.v(TAG, "onKeyDown clientId=" + mClientId + " keyCode=" + keyCode
                    + " event=" + event);
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            final SoftInputWindow window =
                    mSoftInputWindowManager.getSoftInputWindow(mSelfReportedDisplayId);
            if (window != null && window.isShowing()) {
                event.startTracking();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (DEBUG) {
            Log.v(TAG, "onKeyUp clientId=" + mClientId + "keyCode=" + keyCode
                    + " event=" + event);
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking() && !event.isCanceled()) {
            final SoftInputWindow window =
                    mSoftInputWindowManager.getSoftInputWindow(mSelfReportedDisplayId);
            if (window != null && window.isShowing()) {
                window.hide();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        return false;
    }
}
