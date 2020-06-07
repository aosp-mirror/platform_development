/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.example.android.inlinefillservice;

import static com.example.android.inlinefillservice.InlineFillService.TAG;

import android.content.Context;
import android.content.IntentSender;
import android.service.autofill.Dataset;
import android.service.autofill.InlinePresentation;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.Optional;

class ResponseHelper {

    static Dataset newUnlockedDataset(@NonNull Context context,
            @NonNull Map<String, AutofillId> fields, @NonNull String packageName, int index,
            @NonNull Optional<InlineSuggestionsRequest> inlineRequest) {

        Dataset.Builder dataset = new Dataset.Builder();
        for (Map.Entry<String, AutofillId> field : fields.entrySet()) {
            final String hint = field.getKey();
            final AutofillId id = field.getValue();
            final String value = hint + (index + 1);

            // We're simple - our dataset values are hardcoded as "hintN" (for example,
            // "username1", "username2") and they're displayed as such, except if they're a
            // password
            Log.d(TAG, "hint: " + hint);
            final String displayValue = hint.contains("password") ? "password for #" + (index + 1)
                    : value;
            final RemoteViews presentation = newDatasetPresentation(packageName, displayValue);

            // Add Inline Suggestion required info.
            if (inlineRequest.isPresent()) {
                Log.d(TAG, "Found InlineSuggestionsRequest in FillRequest: " + inlineRequest);
                final InlinePresentation inlinePresentation =
                        InlineRequestHelper.createInlineDataset(context, inlineRequest.get(),
                                displayValue, index);
                dataset.setValue(id, AutofillValue.forText(value), presentation,
                        inlinePresentation);
            } else {
                dataset.setValue(id, AutofillValue.forText(value), presentation);
            }
        }

        return dataset.build();
    }

    static Dataset newLockedDataset(@NonNull Context context,
            @NonNull Map<String, AutofillId> fields, @NonNull String packageName, int index,
            @NonNull Optional<InlineSuggestionsRequest> inlineRequest) {
        Dataset unlockedDataset = ResponseHelper.newUnlockedDataset(context, fields,
                packageName, index, inlineRequest);

        Dataset.Builder lockedDataset = new Dataset.Builder();
        for (Map.Entry<String, AutofillId> field : fields.entrySet()) {
            String hint = field.getKey();
            AutofillId id = field.getValue();
            String value = (index + 1) + "-" + hint;
            String displayValue = "Tap to auth " + value;
            IntentSender authentication =
                    AuthActivity.newIntentSenderForDataset(context, unlockedDataset);
            RemoteViews presentation = newDatasetPresentation(packageName, displayValue);
            if (inlineRequest.isPresent()) {
                final InlinePresentation inlinePresentation =
                        InlineRequestHelper.createInlineDataset(context, inlineRequest.get(),
                        displayValue, index);
                lockedDataset.setValue(id, null, presentation, inlinePresentation)
                        .setAuthentication(authentication);
            } else {
                lockedDataset.setValue(id, null, presentation)
                        .setAuthentication(authentication);
            }
        }
        return lockedDataset.build();
    }

    /**
     * Helper method to create a dataset presentation with the givean text.
     */
    @NonNull
    static RemoteViews newDatasetPresentation(@NonNull String packageName,
            @NonNull CharSequence text) {
        RemoteViews presentation =
                new RemoteViews(packageName, R.layout.list_item);
        presentation.setTextViewText(R.id.text, text);
        return presentation;
    }
}
