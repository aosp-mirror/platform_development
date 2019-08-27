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

import android.annotation.NonNull;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.inputmethodservice.MultiClientInputMethodServiceDelegate;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;

/**
 * A {@link Service} that implements multi-client IME protocol.
 */
public final class MultiClientInputMethod extends Service implements DisplayListener {
    private static final String TAG = "MultiClientInputMethod";
    private static final boolean DEBUG = false;

    // last client that had active InputConnection for a given displayId.
    final SparseIntArray mDisplayToLastClientId = new SparseIntArray();
    // Mapping table from the display where IME is attached to the display where IME window will be
    // shown.  Assumes that missing display will use the same display for the IME window.
    SparseIntArray mInputDisplayToImeDisplay;
    SoftInputWindowManager mSoftInputWindowManager;
    MultiClientInputMethodServiceDelegate mDelegate;

    private DisplayManager mDisplayManager;

    @Override
    public void onCreate() {
        if (DEBUG) {
            Log.v(TAG, "onCreate");
        }
        mInputDisplayToImeDisplay = buildInputDisplayToImeDisplay();
        mDelegate = MultiClientInputMethodServiceDelegate.create(this,
                new MultiClientInputMethodServiceDelegate.ServiceCallback() {
                    @Override
                    public void initialized() {
                        if (DEBUG) {
                            Log.i(TAG, "initialized");
                        }
                    }

                    @Override
                    public void addClient(int clientId, int uid, int pid,
                            int selfReportedDisplayId) {
                        int imeDisplayId = mInputDisplayToImeDisplay.get(selfReportedDisplayId,
                                selfReportedDisplayId);
                        final ClientCallbackImpl callback = new ClientCallbackImpl(
                                MultiClientInputMethod.this, mDelegate,
                                mSoftInputWindowManager, clientId, uid, pid, imeDisplayId);
                        if (DEBUG) {
                            Log.v(TAG, "addClient clientId=" + clientId + " uid=" + uid
                                    + " pid=" + pid + " displayId=" + selfReportedDisplayId
                                    + " imeDisplayId=" + imeDisplayId);
                        }

                        mDelegate.acceptClient(clientId, callback, callback.getDispatcherState(),
                                callback.getLooper());
                    }

                    @Override
                    public void removeClient(int clientId) {
                        if (DEBUG) {
                            Log.v(TAG, "removeClient clientId=" + clientId);
                        }
                    }
                });
        mSoftInputWindowManager = new SoftInputWindowManager(this, mDelegate);
    }

    @Override
    public void onDisplayAdded(int displayId) {
        mInputDisplayToImeDisplay = buildInputDisplayToImeDisplay();
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        mDisplayToLastClientId.delete(displayId);
    }

    @Override
    public void onDisplayChanged(int displayId) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) {
            Log.v(TAG, "onBind intent=" + intent);
        }
        mDisplayManager = getApplicationContext().getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(this, getMainThreadHandler());
        return mDelegate.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (DEBUG) {
            Log.v(TAG, "onUnbind intent=" + intent);
        }
        if (mDisplayManager != null) {
            mDisplayManager.unregisterDisplayListener(this);
        }
        return mDelegate.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.v(TAG, "onDestroy");
        }
        mDelegate.onDestroy();
    }

    @NonNull
    private SparseIntArray buildInputDisplayToImeDisplay() {
        Context context = getApplicationContext();
        String config[] = context.getResources().getStringArray(
                R.array.config_inputDisplayToImeDisplay);

        SparseIntArray inputDisplayToImeDisplay = new SparseIntArray();
        Display[] displays = context.getSystemService(DisplayManager.class).getDisplays();
        for (String item: config) {
            String[] pair = item.split("/");
            if (pair.length != 2) {
                Log.w(TAG, "Skip illegal config: " + item);
                continue;
            }
            int inputDisplay = findDisplayId(displays, pair[0]);
            int imeDisplay = findDisplayId(displays, pair[1]);
            if (inputDisplay != Display.INVALID_DISPLAY && imeDisplay != Display.INVALID_DISPLAY) {
                inputDisplayToImeDisplay.put(inputDisplay, imeDisplay);
            }
        }
        return inputDisplayToImeDisplay;
    }

    private static int findDisplayId(Display displays[], String regexp) {
        for (Display display: displays) {
            if (display.getUniqueId().matches(regexp)) {
                int displayId = display.getDisplayId();
                if (DEBUG) {
                    Log.v(TAG, regexp + " matches displayId=" + displayId);
                }
                return displayId;
            }
        }
        Log.w(TAG, "Can't find the display of " + regexp);
        return Display.INVALID_DISPLAY;
    }
}
