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

import android.app.Dialog;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.MultiClientInputMethodServiceDelegate;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;

import java.util.Arrays;

final class SoftInputWindow extends Dialog {
    private static final String TAG = "SoftInputWindow";
    private static final boolean DEBUG = false;

    private final KeyboardView mKeyboardView;

    private final Keyboard mQwertygKeyboard;
    private final Keyboard mSymbolKeyboard;
    private final Keyboard mSymbolShiftKeyboard;

    private int mClientId = MultiClientInputMethodServiceDelegate.INVALID_CLIENT_ID;
    private int mTargetWindowHandle = MultiClientInputMethodServiceDelegate.INVALID_WINDOW_HANDLE;

    private static final KeyboardView.OnKeyboardActionListener sNoopListener =
            new NoopKeyboardActionListener();

    SoftInputWindow(Context context, IBinder token) {
        super(context, android.R.style.Theme_DeviceDefault_InputMethod);

        final LayoutParams lp = getWindow().getAttributes();
        lp.type = LayoutParams.TYPE_INPUT_METHOD;
        lp.setTitle("InputMethod");
        lp.gravity = Gravity.BOTTOM;
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.WRAP_CONTENT;
        lp.token = token;
        getWindow().setAttributes(lp);

        final int windowSetFlags = LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | LayoutParams.FLAG_NOT_FOCUSABLE
                | LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        final int windowModFlags = LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | LayoutParams.FLAG_NOT_FOCUSABLE
                | LayoutParams.FLAG_DIM_BEHIND
                | LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        getWindow().setFlags(windowSetFlags, windowModFlags);

        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        mKeyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.input, null);
        mQwertygKeyboard = new Keyboard(context, R.xml.qwerty);
        mSymbolKeyboard = new Keyboard(context, R.xml.symbols);
        mSymbolShiftKeyboard = new Keyboard(context, R.xml.symbols_shift);
        mKeyboardView.setKeyboard(mQwertygKeyboard);
        mKeyboardView.setOnKeyboardActionListener(sNoopListener);
        layout.addView(mKeyboardView);

        setContentView(layout, new ViewGroup.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // TODO: Check why we need to call this.
        getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    int getClientId() {
        return mClientId;
    }

    int getTargetWindowHandle() {
        return mTargetWindowHandle;
    }

    boolean isQwertyKeyboard() {
        return mKeyboardView.getKeyboard() == mQwertygKeyboard;
    }

    boolean isSymbolKeyboard() {
        Keyboard keyboard = mKeyboardView.getKeyboard();
        return keyboard == mSymbolKeyboard || keyboard == mSymbolShiftKeyboard;
    }

    void onFinishClient() {
        mKeyboardView.setOnKeyboardActionListener(sNoopListener);
        mClientId = MultiClientInputMethodServiceDelegate.INVALID_CLIENT_ID;
        mTargetWindowHandle = MultiClientInputMethodServiceDelegate.INVALID_WINDOW_HANDLE;
    }

    void onDummyStartInput(int clientId, int targetWindowHandle) {
        if (DEBUG) {
            Log.v(TAG, "onDummyStartInput clientId=" + clientId
                    + " targetWindowHandle=" + targetWindowHandle);
        }
        mKeyboardView.setOnKeyboardActionListener(sNoopListener);
        mClientId = clientId;
        mTargetWindowHandle = targetWindowHandle;
    }

    void onStartInput(int clientId, int targetWindowHandle, InputConnection inputConnection) {
        if (DEBUG) {
            Log.v(TAG, "onStartInput clientId=" + clientId
                    + " targetWindowHandle=" + targetWindowHandle);
        }
        mClientId = clientId;
        mTargetWindowHandle = targetWindowHandle;
        mKeyboardView.setOnKeyboardActionListener(new NoopKeyboardActionListener() {
            @Override
            public void onKey(int primaryCode, int[] keyCodes) {
                if (DEBUG) {
                    Log.v(TAG, "onKey clientId=" + clientId + " primaryCode=" + primaryCode
                            + " keyCodes=" + Arrays.toString(keyCodes));
                }
                boolean isShifted = isShifted();  // Store the current state before resetting it.
                resetShift();
                switch (primaryCode) {
                    case Keyboard.KEYCODE_CANCEL:
                        hide();
                        break;
                    case Keyboard.KEYCODE_DELETE:
                        inputConnection.sendKeyEvent(
                                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        inputConnection.sendKeyEvent(
                                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
                        break;
                    case Keyboard.KEYCODE_MODE_CHANGE:
                        handleSwitchKeyboard();
                        break;
                    case Keyboard.KEYCODE_SHIFT:
                        handleShift(isShifted);
                        break;
                    default:
                        handleCharacter(inputConnection, primaryCode, isShifted);
                        break;
                }
            }

            @Override
            public void onText(CharSequence text) {
                if (DEBUG) {
                    Log.v(TAG, "onText clientId=" + clientId + " text=" + text);
                }
                if (inputConnection == null) {
                    return;
                }
                inputConnection.commitText(text, 0);
            }
        });
    }

    void handleSwitchKeyboard() {
        if (isQwertyKeyboard()) {
            mKeyboardView.setKeyboard(mSymbolKeyboard);
        } else {
            mKeyboardView.setKeyboard(mQwertygKeyboard);
        }

    }

    boolean isShifted() {
        return mKeyboardView.isShifted();
    }

    void resetShift() {
        if (isSymbolKeyboard() && isShifted()) {
            mKeyboardView.setKeyboard(mSymbolKeyboard);
        }
        mKeyboardView.setShifted(false);
    }

    void handleShift(boolean isShifted) {
        if (isSymbolKeyboard()) {
            mKeyboardView.setKeyboard(isShifted ? mSymbolKeyboard : mSymbolShiftKeyboard);
        }
        mKeyboardView.setShifted(!isShifted);
    }

    void handleCharacter(InputConnection inputConnection, int primaryCode, boolean isShifted) {
        if (isQwertyKeyboard() && isShifted) {
            primaryCode = Character.toUpperCase(primaryCode);
        }
        inputConnection.commitText(String.valueOf((char) primaryCode), 1);
    }
}
