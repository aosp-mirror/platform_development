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

import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.MultiClientInputMethodServiceDelegate;
import android.os.IBinder;
import android.util.Log;

/**
 * A {@link Service} that implements multi-client IME protocol.
 */
public final class MultiClientInputMethod extends Service {
    private static final String TAG = "MultiClientInputMethod";
    private static final boolean DEBUG = false;

    // last client that had active InputConnection.
    int mLastClientId = MultiClientInputMethodServiceDelegate.INVALID_CLIENT_ID;
    SoftInputWindowManager mSoftInputWindowManager;
    MultiClientInputMethodServiceDelegate mDelegate;

    @Override
    public void onCreate() {
        if (DEBUG) {
            Log.v(TAG, "onCreate");
        }
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
                        final ClientCallbackImpl callback = new ClientCallbackImpl(
                                MultiClientInputMethod.this, mDelegate,
                                mSoftInputWindowManager, clientId, uid, pid, selfReportedDisplayId);
                        if (DEBUG) {
                            Log.v(TAG, "addClient clientId=" + clientId + " uid=" + uid
                                    + " pid=" + pid + " displayId=" + selfReportedDisplayId);
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
    public IBinder onBind(Intent intent) {
        if (DEBUG) {
            Log.v(TAG, "onBind intent=" + intent);
        }
        return mDelegate.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (DEBUG) {
            Log.v(TAG, "onUnbind intent=" + intent);
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
}
