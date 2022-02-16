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

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.inputmethodservice.MultiClientInputMethodServiceDelegate;
import android.os.IBinder;
import android.util.SparseArray;
import android.view.Display;

final class SoftInputWindowManager {
    private final Context mContext;
    private final MultiClientInputMethodServiceDelegate mDelegate;
    private final SparseArray<SoftInputWindow> mSoftInputWindows = new SparseArray<>();

    SoftInputWindowManager(Context context, MultiClientInputMethodServiceDelegate delegate) {
        mContext = context;
        mDelegate = delegate;
    }

    SoftInputWindow getOrCreateSoftInputWindow(int displayId) {
        final SoftInputWindow existingWindow = mSoftInputWindows.get(displayId);
        if (existingWindow != null) {
            return existingWindow;
        }

        final Display display =
                mContext.getSystemService(DisplayManager.class).getDisplay(displayId);
        if (display == null) {
            return null;
        }
        final IBinder windowToken = mDelegate.createInputMethodWindowToken(displayId);
        if (windowToken == null) {
            return null;
        }

        final Context displayContext = mContext.createDisplayContext(display);
        final SoftInputWindow newWindow = new SoftInputWindow(displayContext, windowToken);
        mSoftInputWindows.put(displayId, newWindow);
        return newWindow;
    }

    SoftInputWindow getSoftInputWindow(int displayId) {
        return mSoftInputWindows.get(displayId);
    }
}
