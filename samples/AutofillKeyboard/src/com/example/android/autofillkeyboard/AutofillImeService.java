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

import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
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
    private Decoder mDecoder;

    private ViewGroup mSuggestionStrip;
    private ViewGroup mPinnedSuggestionsStart;
    private ViewGroup mPinnedSuggestionsEnd;
    private InlineContentClipView mScrollableSuggestionsClip;
    private ViewGroup mScrollableSuggestions;

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

    @Override
    public View onCreateInputView() {
        mInputView = (InputView) LayoutInflater.from(this).inflate(R.layout.input_view, null);
        return mInputView;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        mDecoder = new Decoder(getCurrentInputConnection());
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);

        mInputView.removeAllViews();
        Keyboard keyboard = Keyboard.qwerty(this);
        mInputView.addView(keyboard.inflateKeyboardView(LayoutInflater.from(this), mInputView));

        mSuggestionStrip = mInputView.findViewById(R.id.suggestion_strip);
        mPinnedSuggestionsStart = mInputView.findViewById(R.id.pinned_suggestions_start);
        mPinnedSuggestionsEnd = mInputView.findViewById(R.id.pinned_suggestions_end);
        mScrollableSuggestionsClip = mInputView.findViewById(R.id.scrollable_suggestions_clip);
        mScrollableSuggestions = mInputView.findViewById(R.id.scrollable_suggestions);

        updateInlineSuggestionStrip(Collections.emptyList());
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
        final ArrayList<InlinePresentationSpec> presentationSpecs = new ArrayList<>();
        presentationSpecs.add(new InlinePresentationSpec.Builder(new Size(100, 100),
                new Size(400, 100)).build());
        presentationSpecs.add(new InlinePresentationSpec.Builder(new Size(100, 100),
                new Size(400, 100)).build());

        return new InlineSuggestionsRequest.Builder(presentationSpecs)
                .setMaxSuggestionCount(6)
                .build();
    }

    @Override
    public boolean onInlineSuggestionsResponse(InlineSuggestionsResponse response) {
        Log.d(TAG, "onInlineSuggestionsResponse() called");
        onInlineSuggestionsResponseInternal(response);
        return true;
    }

    private void updateInlineSuggestionStrip(List<SuggestionItem> suggestionItems) {
        mPinnedSuggestionsStart.removeAllViews();
        mScrollableSuggestions.removeAllViews();
        mPinnedSuggestionsEnd.removeAllViews();

        final int size = suggestionItems.size();
        if (size <= 0) {
            mSuggestionStrip.setVisibility(View.GONE);
            return;
        }

        // TODO: refactor me
        mScrollableSuggestionsClip.setBackgroundColor(
                getColor(R.color.suggestion_strip_background));
        mSuggestionStrip.setVisibility(View.VISIBLE);

        for (int i = 0; i < size; i++) {
            final SuggestionItem suggestionItem = suggestionItems.get(i);
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

    private void onInlineSuggestionsResponseInternal(InlineSuggestionsResponse response) {
        Log.d(TAG, "onInlineSuggestionsResponseInternal() called. Suggestion="
                + response.getInlineSuggestions().size());

        final List<InlineSuggestion> inlineSuggestions = response.getInlineSuggestions();

        final int totalSuggestionsCount = inlineSuggestions.size();
        if (totalSuggestionsCount <= 0) {
            updateInlineSuggestionStrip(Collections.emptyList());
            return;
        }

        final Map<Integer, SuggestionItem> suggestionMap = Collections.synchronizedMap((
                new TreeMap<>()));
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        for (int i = 0; i < totalSuggestionsCount; i++) {
            final int index = i;
            final InlineSuggestion inlineSuggestion = inlineSuggestions.get(i);
            final Size size = inlineSuggestion.getInfo().getInlinePresentationSpec().getMaxSize();

            inlineSuggestion.inflate(this, size, executor, suggestionView -> {
                Log.d(TAG, "new inline suggestion view ready");
                if(suggestionView != null) {
                    suggestionView.setLayoutParams(new ViewGroup.LayoutParams(
                            size.getWidth(), size.getHeight()));
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
}
