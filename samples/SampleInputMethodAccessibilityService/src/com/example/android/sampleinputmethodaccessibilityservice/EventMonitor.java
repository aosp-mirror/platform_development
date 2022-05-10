/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.example.android.sampleinputmethodaccessibilityservice;

import android.os.Parcel;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;

final class EventMonitor {
    @FunctionalInterface
    interface DebugMessageCallback {
        void onMessageChanged(String message);
    }

    private enum State {
        BeforeFirstStartInput,
        InputStarted,
        InputRestarted,
        InputFinished,
    }

    private State mState = State.BeforeFirstStartInput;
    private int mStartInputCount = 0;
    private int mUpdateSelectionCount = 0;
    private int mFinishInputCount = 0;
    private int mSelStart = -1;
    private int mSelEnd = -1;
    private int mCompositionStart = -1;
    private int mCompositionEnd = -1;
    @Nullable
    private EditorInfo mEditorInfo;

    @Nullable
    private final DebugMessageCallback mDebugMessageCallback;

    void onStartInput(EditorInfo attribute, boolean restarting) {
        ++mStartInputCount;
        mState = restarting ? State.InputRestarted : State.InputStarted;
        mSelStart = attribute.initialSelStart;
        mSelEnd = attribute.initialSelEnd;
        mCompositionStart = -1;
        mCompositionEnd = -1;
        mEditorInfo = cloneEditorInfo(attribute);
        updateMessage();
    }

    void onFinishInput() {
        ++mFinishInputCount;
        mState = State.InputFinished;
        mEditorInfo = null;
        updateMessage();
    }

    void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart,
            int newSelEnd, int candidatesStart, int candidatesEnd) {
        ++mUpdateSelectionCount;
        mSelStart = newSelStart;
        mSelEnd = newSelEnd;
        mCompositionStart = candidatesStart;
        mCompositionEnd = candidatesEnd;
        updateMessage();
    }

    EventMonitor(@Nullable DebugMessageCallback callback) {
        mDebugMessageCallback = callback;
    }

    private void updateMessage() {
        if (mDebugMessageCallback == null) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("state=").append(mState).append("\n")
                .append("startInputCount=").append(mStartInputCount).append("\n")
                .append("finishInputCount=").append(mFinishInputCount).append("\n")
                .append("updateSelectionCount=").append(mUpdateSelectionCount).append("\n");
        if (mSelStart == -1 && mSelEnd == -1) {
            sb.append("selection=none\n");
        } else {
            sb.append("selection=(").append(mSelStart).append(",").append(mSelEnd).append(")\n");
        }
        if (mCompositionStart == -1 && mCompositionEnd == -1) {
            sb.append("composition=none");
        } else {
            sb.append("composition=(")
                    .append(mCompositionStart).append(",").append(mCompositionEnd).append(")");
        }
        if (mEditorInfo != null) {
            sb.append("\n");
            sb.append("packageName=").append(mEditorInfo.packageName).append("\n");
            sb.append("inputType=");
            EditorInfoUtil.dumpInputType(sb, mEditorInfo.inputType);
            sb.append("\n");
            sb.append("imeOptions=");
            EditorInfoUtil.dumpImeOptions(sb, mEditorInfo.imeOptions);
        }

        mDebugMessageCallback.onMessageChanged(sb.toString());
    }

    private static EditorInfo cloneEditorInfo(EditorInfo original) {
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            original.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return EditorInfo.CREATOR.createFromParcel(parcel);
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }
}
