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

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.InputMethod;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * A sample {@link AccessibilityService} to demo how to use IME APIs.
 */
public final class SampleInputMethodAccessibilityService extends AccessibilityService {
    private static final String TAG = "SampleImeA11yService";

    private EventMonitor mEventMonitor;

    private final class InputMethodImpl extends InputMethod {
        InputMethodImpl(AccessibilityService service) {
            super(service);
        }

        @Override
        public void onStartInput(EditorInfo attribute, boolean restarting) {
            Log.d(TAG, String.format("onStartInput(%s,%b)", attribute, restarting));
            mEventMonitor.onStartInput(attribute, restarting);
        }

        @Override
        public void onFinishInput() {
            Log.d(TAG, "onFinishInput()");
            mEventMonitor.onFinishInput();
        }

        @Override
        public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart,
                int newSelEnd, int candidatesStart, int candidatesEnd) {
            Log.d(TAG, String.format("onUpdateSelection(%d,%d,%d,%d,%d,%d)", oldSelStart, oldSelEnd,
                    newSelStart, newSelEnd, candidatesStart, candidatesEnd));
            mEventMonitor.onUpdateSelection(oldSelStart, oldSelEnd,
                    newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        }
    }

    private static <T> Pair<CharSequence, T> item(@NonNull CharSequence label, @Nullable T value) {
        return Pair.create(label, value);
    }

    private <T> void addButtons(@NonNull LinearLayout parentView, @NonNull String headerText,
            @NonNull List<Pair<CharSequence, T>> items,
            @NonNull BiConsumer<T, InputMethod.AccessibilityInputConnection> action) {
        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        {
            final TextView headerTextView = new TextView(this, null,
                    android.R.attr.listSeparatorTextViewStyle);
            headerTextView.setAllCaps(false);
            headerTextView.setText(headerText);
            layout.addView(headerTextView);
        }
        {
            final LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (Pair<CharSequence, T> item : items) {
                final Button button = new Button(this, null, android.R.attr.buttonStyleSmall);
                button.setAllCaps(false);
                button.setText(item.first);
                button.setOnClickListener(view -> {
                    final InputMethod ime = getInputMethod();
                    if (ime == null) {
                        return;
                    }
                    final InputMethod.AccessibilityInputConnection ic =
                            ime.getCurrentInputConnection();
                    if (ic == null) {
                        return;
                    }
                    action.accept(item.second, ic);
                });
                itemLayout.addView(button);
            }
            final HorizontalScrollView scrollView = new HorizontalScrollView(this);
            scrollView.addView(itemLayout);
            layout.addView(scrollView);
        }
        parentView.addView(layout);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        final WindowManager windowManager = getSystemService(WindowManager.class);
        final WindowMetrics metrics = windowManager.getCurrentWindowMetrics();

        // Create a monitor window.
        {
            final TextView textView = new TextView(this);
            mEventMonitor = new EventMonitor(textView::setText);

            final LinearLayout monitorWindowContent = new LinearLayout(this);
            monitorWindowContent.setOrientation(LinearLayout.VERTICAL);
            monitorWindowContent.setPadding(10, 10, 10, 10);

            monitorWindowContent.addView(textView);

            OverlayWindowBuilder.from(monitorWindowContent)
                    .setSize((metrics.getBounds().width() * 3) / 4,
                            WindowManager.LayoutParams.WRAP_CONTENT)
                    .setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setBackgroundColor(0xeed2e3fc)
                    .show();
        }

        final LinearLayout contentView = new LinearLayout(this);
        contentView.setOrientation(LinearLayout.VERTICAL);
        {
            final TextView textView = new TextView(this, null, android.R.attr.windowTitleStyle);
            textView.setGravity(Gravity.CENTER);
            textView.setText("A11Y IME");
            contentView.addView(textView);
        }
        {
            final LinearLayout buttonLayout = new LinearLayout(this);
            buttonLayout.setBackgroundColor(0xfffeefc3);
            buttonLayout.setPadding(10, 10, 10, 10);
            buttonLayout.setOrientation(LinearLayout.VERTICAL);

            addButtons(buttonLayout,
                    "commitText", List.of(
                            item("A", "A"),
                            item("Hello World", "Hello World"),
                            item("\uD83D\uDE36\u200D\uD83C\uDF2B\uFE0F",
                                    "\uD83D\uDE36\u200D\uD83C\uDF2B\uFE0F")),
                    (value, ic) -> ic.commitText(value, 1, null));

            addButtons(buttonLayout,
                    "sendKeyEvent", List.of(
                            item("A", KeyEvent.KEYCODE_A),
                            item("DEL", KeyEvent.KEYCODE_DEL),
                            item("DPAD_LEFT", KeyEvent.KEYCODE_DPAD_LEFT),
                            item("DPAD_RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT),
                            item("COPY", KeyEvent.KEYCODE_COPY),
                            item("CUT", KeyEvent.KEYCODE_CUT),
                            item("PASTE", KeyEvent.KEYCODE_PASTE)),
                    (keyCode, ic) -> {
                        final long eventTime = SystemClock.uptimeMillis();
                        ic.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                                KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
                        ic.sendKeyEvent(new KeyEvent(eventTime, SystemClock.uptimeMillis(),
                                KeyEvent.ACTION_UP, keyCode, 0, 0,
                                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                                KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
                    });

            addButtons(buttonLayout,
                    "performEditorAction", List.of(
                            item("UNSPECIFIED", EditorInfo.IME_ACTION_UNSPECIFIED),
                            item("NONE", EditorInfo.IME_ACTION_NONE),
                            item("GO", EditorInfo.IME_ACTION_GO),
                            item("SEARCH", EditorInfo.IME_ACTION_SEARCH),
                            item("SEND", EditorInfo.IME_ACTION_SEND),
                            item("NEXT", EditorInfo.IME_ACTION_NEXT),
                            item("DONE", EditorInfo.IME_ACTION_DONE),
                            item("PREVIOUS", EditorInfo.IME_ACTION_PREVIOUS)),
                    (action, ic) -> ic.performEditorAction(action));

            addButtons(buttonLayout,
                    "performContextMenuAction", List.of(
                            item("selectAll", android.R.id.selectAll),
                            item("startSelectingText", android.R.id.startSelectingText),
                            item("stopSelectingText", android.R.id.stopSelectingText),
                            item("cut", android.R.id.cut),
                            item("copy", android.R.id.copy),
                            item("paste", android.R.id.paste),
                            item("copyUrl", android.R.id.copyUrl),
                            item("switchInputMethod", android.R.id.switchInputMethod)),
                    (action, ic) -> ic.performContextMenuAction(action));

            addButtons(buttonLayout,
                    "setSelection", List.of(
                            item("(0,0)", Pair.create(0, 0)),
                            item("(0,1)", Pair.create(0, 1)),
                            item("(1,1)", Pair.create(1, 1)),
                            item("(0,999)", Pair.create(0, 999))),
                    (pair, ic) -> ic.setSelection(pair.first, pair.second));

            addButtons(buttonLayout,
                    "deleteSurroundingText", List.of(
                            item("(0,0)", Pair.create(0, 0)),
                            item("(0,1)", Pair.create(0, 1)),
                            item("(1,0)", Pair.create(1, 0)),
                            item("(1,1)", Pair.create(1, 1)),
                            item("(999,0)", Pair.create(999, 0)),
                            item("(0,999)", Pair.create(0, 999))),
                    (pair, ic) -> ic.deleteSurroundingText(pair.first, pair.second));

            final ScrollView scrollView = new ScrollView(this);
            scrollView.addView(buttonLayout);
            contentView.addView(scrollView);

            // Set margin
            {
                final LinearLayout.LayoutParams lp =
                        ((LinearLayout.LayoutParams) scrollView.getLayoutParams());
                lp.leftMargin = lp.rightMargin = lp.bottomMargin = 20;
                scrollView.setLayoutParams(lp);
            }
        }

        OverlayWindowBuilder.from(contentView)
                .setSize((metrics.getBounds().width() * 3) / 4,
                        metrics.getBounds().height() / 5)
                .setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                .setRelativePosition(300, 300)
                .setBackgroundColor(0xfffcc934)
                .show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public InputMethod onCreateInputMethod() {
        Log.d(TAG, "onCreateInputMethod");
        return new InputMethodImpl(this);
    }
}
