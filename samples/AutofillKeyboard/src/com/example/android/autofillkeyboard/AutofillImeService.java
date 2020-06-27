/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.example.android.autofillkeyboard;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.inline.InlineContentView;
import android.widget.inline.InlinePresentationSpec;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import android.widget.Toast;

import androidx.autofill.inline.UiVersions;
import androidx.autofill.inline.UiVersions.StylesBuilder;
import androidx.autofill.inline.common.ImageViewStyle;
import androidx.autofill.inline.common.TextViewStyle;
import androidx.autofill.inline.common.ViewStyle;
import androidx.autofill.inline.v1.InlineSuggestionUi;
import androidx.autofill.inline.v1.InlineSuggestionUi.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The {@link InputMethodService} implementation for Autofill keyboard. */
public class AutofillImeService extends InputMethodService {
    private static final boolean SHOWCASE_BG_FG_TRANSITION = false;
    // To test this you need to change KeyboardArea style layout_height to 400dp
    private static final boolean SHOWCASE_UP_DOWN_TRANSITION = false;

    private static final long MOVE_SUGGESTIONS_TO_BG_TIMEOUT = 5000;
    private static final long MOVE_SUGGESTIONS_TO_FG_TIMEOUT = 15000;

    private static final long MOVE_SUGGESTIONS_UP_TIMEOUT = 5000;
    private static final long MOVE_SUGGESTIONS_DOWN_TIMEOUT = 10000;

    private InputView mInputView;
    private Keyboard mKeyboard;
    private Decoder mDecoder;

    private ViewGroup mSuggestionStrip;
    private ViewGroup mPinnedSuggestionsStart;
    private ViewGroup mPinnedSuggestionsEnd;
    private InlineContentClipView mScrollableSuggestionsClip;
    private ViewGroup mScrollableSuggestions;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mMoveScrollableSuggestionsToBg = () -> {
        mScrollableSuggestionsClip.setZOrderedOnTop(false);
        Toast.makeText(AutofillImeService.this, "Chips moved to bg - not clickable",
                Toast.LENGTH_SHORT).show();
    };

    private final Runnable mMoveScrollableSuggestionsToFg = () -> {
        mScrollableSuggestionsClip.setZOrderedOnTop(true);
        Toast.makeText(AutofillImeService.this, "Chips moved to fg - clickable",
                Toast.LENGTH_SHORT).show();
    };

    private final Runnable mMoveScrollableSuggestionsUp = () -> {
        mSuggestionStrip.animate().translationY(-50).setDuration(500).start();
        Toast.makeText(AutofillImeService.this, "Animating up",
                Toast.LENGTH_SHORT).show();
    };

    private final Runnable mMoveScrollableSuggestionsDown = () -> {
        mSuggestionStrip.animate().translationY(0).setDuration(500).start();
        Toast.makeText(AutofillImeService.this, "Animating down",
                Toast.LENGTH_SHORT).show();
    };

    private ResponseState mResponseState = ResponseState.RESET;
    private Runnable mDelayedDeletion;
    private Runnable mPendingResponse;

    @Override
    public View onCreateInputView() {
        mInputView = (InputView) LayoutInflater.from(this).inflate(R.layout.input_view, null);
        mKeyboard = Keyboard.qwerty(this);
        mInputView.addView(mKeyboard.inflateKeyboardView(LayoutInflater.from(this), mInputView));
        mSuggestionStrip = mInputView.findViewById(R.id.suggestion_strip);
        mPinnedSuggestionsStart = mInputView.findViewById(R.id.pinned_suggestions_start);
        mPinnedSuggestionsEnd = mInputView.findViewById(R.id.pinned_suggestions_end);
        mScrollableSuggestionsClip = mInputView.findViewById(R.id.scrollable_suggestions_clip);
        mScrollableSuggestions = mInputView.findViewById(R.id.scrollable_suggestions);
        return mInputView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        mDecoder = new Decoder(getCurrentInputConnection());
        if(mKeyboard != null) {
            mKeyboard.reset();
        }
        if (mResponseState == ResponseState.RECEIVE_RESPONSE) {
            mResponseState = ResponseState.START_INPUT;
        } else {
            mResponseState = ResponseState.RESET;
        }
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
    }

    private void cancelPendingResponse() {
        if (mPendingResponse != null) {
            Log.d(TAG, "Canceling pending response");
            mHandler.removeCallbacks(mPendingResponse);
            mPendingResponse = null;
        }
    }

    private void postPendingResponse(InlineSuggestionsResponse response) {
        cancelPendingResponse();
        final List<InlineSuggestion> inlineSuggestions = response.getInlineSuggestions();
        mResponseState = ResponseState.RECEIVE_RESPONSE;
        mPendingResponse = () -> {
            mPendingResponse = null;
            if (mResponseState == ResponseState.START_INPUT && inlineSuggestions.isEmpty()) {
                scheduleDelayedDeletion();
            } else {
                inflateThenShowSuggestions(inlineSuggestions);
            }
            mResponseState = ResponseState.RESET;
        };
        mHandler.post(mPendingResponse);
    }

    private void cancelDelayedDeletion(String msg) {
        if(mDelayedDeletion != null) {
            Log.d(TAG, msg + " canceling delayed deletion");
            mHandler.removeCallbacks(mDelayedDeletion);
            mDelayedDeletion = null;
        }
    }

    private void scheduleDelayedDeletion() {
        if (mInputView != null && mDelayedDeletion == null) {
            // We delay the deletion of the suggestions from previous input connection, to avoid
            // the flicker caused by deleting them and immediately showing new suggestions for
            // the current input connection.
            Log.d(TAG, "Scheduling a delayed deletion of inline suggestions");
            mDelayedDeletion = () -> {
                Log.d(TAG, "Executing scheduled deleting inline suggestions");
                mDelayedDeletion = null;
                clearInlineSuggestionStrip();
            };
            mHandler.postDelayed(mDelayedDeletion, 200);
        }
    }

    private void clearInlineSuggestionStrip() {
        if (mInputView != null) {
            updateInlineSuggestionStrip(Collections.emptyList());
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        if (!finishingInput) {
            // This runs when the IME is hide (but not finished). We need to clear the suggestions.
            // Otherwise, they will stay on the screen for a bit after the IME window disappears.
            // TODO: right now the framework resends the suggestions when onStartInputView is
            // called. If the framework is changed to not resend, then we need to cache the
            // inline suggestion views locally and re-attach them when the IME is shown again by
            // onStartInputView.
            clearInlineSuggestionStrip();
        }
    }

    @Override
    public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (mInputView != null) {
            outInsets.contentTopInsets += mInputView.getTopInsets();
        }
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT;
    }

    /*****************    Inline Suggestions Demo Code   *****************/

    private static final String TAG = "AutofillImeService";

    @Override
    public InlineSuggestionsRequest onCreateInlineSuggestionsRequest(Bundle uiExtras) {
        Log.d(TAG, "onCreateInlineSuggestionsRequest() called");
        StylesBuilder stylesBuilder = UiVersions.newStylesBuilder();
        Style style = InlineSuggestionUi.newStyleBuilder()
                .setSingleIconChipStyle(
                        new ViewStyle.Builder()
                                .setBackground(
                                        Icon.createWithResource(this, R.drawable.chip_background))
                                .setPadding(0, 0, 0, 0)
                                .build())
                .setChipStyle(
                        new ViewStyle.Builder()
                                .setBackground(
                                        Icon.createWithResource(this, R.drawable.chip_background))
                                .setPadding(toPixel(5 + 8), 0, toPixel(5 + 8), 0)
                                .build())
                .setStartIconStyle(new ImageViewStyle.Builder().setLayoutMargin(0, 0, 0, 0).build())
                .setTitleStyle(
                        new TextViewStyle.Builder()
                                .setLayoutMargin(toPixel(4), 0, toPixel(4), 0)
                                .setTextColor(Color.parseColor("#FF202124"))
                                .setTextSize(16)
                                .build())
                .setSubtitleStyle(
                        new TextViewStyle.Builder()
                                .setLayoutMargin(0, 0, toPixel(4), 0)
                                .setTextColor(Color.parseColor("#99202124")) // 60% opacity
                                .setTextSize(14)
                                .build())
                .setEndIconStyle(new ImageViewStyle.Builder().setLayoutMargin(0, 0, 0, 0).build())
                .build();
        stylesBuilder.addStyle(style);
        Bundle stylesBundle = stylesBuilder.build();

        final ArrayList<InlinePresentationSpec> presentationSpecs = new ArrayList<>();
        presentationSpecs.add(new InlinePresentationSpec.Builder(new Size(100, getHeight()),
                new Size(740, getHeight())).setStyle(stylesBundle).build());
        presentationSpecs.add(new InlinePresentationSpec.Builder(new Size(100, getHeight()),
                new Size(740, getHeight())).setStyle(stylesBundle).build());

        return new InlineSuggestionsRequest.Builder(presentationSpecs)
                .setMaxSuggestionCount(6)
                .build();
    }

    private int toPixel(int dp) {
        return (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private int getHeight() {
        return getResources().getDimensionPixelSize(R.dimen.keyboard_header_height);
    }

    @Override
    public boolean onInlineSuggestionsResponse(InlineSuggestionsResponse response) {
        Log.d(TAG,
                "onInlineSuggestionsResponse() called: " + response.getInlineSuggestions().size());
        cancelDelayedDeletion("onInlineSuggestionsResponse");
        postPendingResponse(response);
        return true;
    }

    private void updateInlineSuggestionStrip(List<SuggestionItem> suggestionItems) {
        Log.d(TAG, "Actually updating the suggestion strip: " + suggestionItems.size());
        mPinnedSuggestionsStart.removeAllViews();
        mScrollableSuggestions.removeAllViews();
        mPinnedSuggestionsEnd.removeAllViews();

        if (suggestionItems.isEmpty()) {
            return;
        }

        // TODO: refactor me
        mScrollableSuggestionsClip.setBackgroundColor(
                getColor(R.color.suggestion_strip_background));
        mSuggestionStrip.setVisibility(View.VISIBLE);

        for (SuggestionItem suggestionItem : suggestionItems) {
            if (suggestionItem == null) {
                continue;
            }
            final InlineContentView suggestionView = suggestionItem.mView;
            if (suggestionItem.mIsPinned) {
                if (mPinnedSuggestionsStart.getChildCount() <= 0) {
                    mPinnedSuggestionsStart.addView(suggestionView);
                } else   {
                    mPinnedSuggestionsEnd.addView(suggestionView);
                }
            } else {
                mScrollableSuggestions.addView(suggestionView);
            }
        }

        if (SHOWCASE_BG_FG_TRANSITION) {
            rescheduleShowcaseBgFgTransitions();
        }
        if (SHOWCASE_UP_DOWN_TRANSITION) {
            rescheduleShowcaseUpDownTransitions();
        }
    }

    private void rescheduleShowcaseBgFgTransitions() {
        final Handler handler = mInputView.getHandler();
        handler.removeCallbacks(mMoveScrollableSuggestionsToBg);
        handler.postDelayed(mMoveScrollableSuggestionsToBg, MOVE_SUGGESTIONS_TO_BG_TIMEOUT);
        handler.removeCallbacks(mMoveScrollableSuggestionsToFg);
        handler.postDelayed(mMoveScrollableSuggestionsToFg, MOVE_SUGGESTIONS_TO_FG_TIMEOUT);
    }

    private void rescheduleShowcaseUpDownTransitions() {
        final Handler handler = mInputView.getHandler();
        handler.removeCallbacks(mMoveScrollableSuggestionsUp);
        handler.postDelayed(mMoveScrollableSuggestionsUp, MOVE_SUGGESTIONS_UP_TIMEOUT);
        handler.removeCallbacks(mMoveScrollableSuggestionsDown);
        handler.postDelayed(mMoveScrollableSuggestionsDown, MOVE_SUGGESTIONS_DOWN_TIMEOUT);
    }

    private void inflateThenShowSuggestions( List<InlineSuggestion> inlineSuggestions) {
        final int totalSuggestionsCount = inlineSuggestions.size();
        if (inlineSuggestions.isEmpty()) {
            // clear the suggestions and then return
            getMainExecutor().execute(() -> updateInlineSuggestionStrip(Collections.EMPTY_LIST));
            return;
        }

        final Map<Integer, SuggestionItem> suggestionMap = Collections.synchronizedMap((
                new TreeMap<>()));
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        for (int i = 0; i < totalSuggestionsCount; i++) {
            final int index = i;
            final InlineSuggestion inlineSuggestion = inlineSuggestions.get(i);
            final Size size = new Size(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            inlineSuggestion.inflate(this, size, executor, suggestionView -> {
                Log.d(TAG, "new inline suggestion view ready");
                if(suggestionView != null) {
                    suggestionView.setOnClickListener((v) -> {
                        Log.d(TAG, "Received click on the suggestion");
                    });
                    suggestionView.setOnLongClickListener((v) -> {
                        Log.d(TAG, "Received long click on the suggestion");
                        return true;
                    });
                    final SuggestionItem suggestionItem = new SuggestionItem(
                            suggestionView, /*isAction*/ inlineSuggestion.getInfo().isPinned());
                    suggestionMap.put(index, suggestionItem);
                } else {
                    suggestionMap.put(index, null);
                }

                // Update the UI once the last inflation completed
                if (suggestionMap.size() >= totalSuggestionsCount) {
                    final ArrayList<SuggestionItem> suggestionItems = new ArrayList<>(
                            suggestionMap.values());
                    getMainExecutor().execute(() -> updateInlineSuggestionStrip(suggestionItems));
                }
            });
        }
    }

    void handle(String data) {
        Log.d(TAG, "handle() called: [" + data + "]");
        mDecoder.decodeAndApply(data);
    }

    static class SuggestionItem {
        final InlineContentView mView;
        final boolean mIsPinned;

        SuggestionItem(InlineContentView view, boolean isPinned) {
            mView = view;
            mIsPinned = isPinned;
        }
    }

    enum ResponseState {
        RESET,
        RECEIVE_RESPONSE,
        START_INPUT,
    }
}
