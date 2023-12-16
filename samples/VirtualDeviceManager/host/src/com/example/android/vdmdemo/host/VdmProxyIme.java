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

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import com.example.android.vdmdemo.common.RemoteEventProto.KeyboardVisibilityEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteIo;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/** A custom IME component that forwards soft keyboard visibility events to the client. */
@AndroidEntryPoint(InputMethodService.class)
public final class VdmProxyIme extends Hilt_VdmProxyIme {

    public static final String TAG = "VdmProxyIme";

    @Inject RemoteIo mRemoteIo;
    @Inject DisplayRepository mDisplayRepository;

    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);
        sendVisibilityEvent(true);
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        sendVisibilityEvent(true);
        return true;
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        sendVisibilityEvent(false);
    }

    private void sendVisibilityEvent(boolean visible) {
        Log.d(TAG, "Sending client IME visibility event, visible=" + visible);
        mRemoteIo.sendMessage(
                RemoteEvent.newBuilder()
                        .setDisplayId(getRemoteDisplayId())
                        .setKeyboardVisibilityEvent(
                                KeyboardVisibilityEvent.newBuilder().setVisible(visible))
                        .build());
    }

    private int getRemoteDisplayId() {
        return mDisplayRepository.getRemoteDisplayId(
                getWindow().getWindow().getDecorView().getDisplay().getDisplayId());
    }
}
