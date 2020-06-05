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

import android.app.PendingIntent;
import android.app.slice.Slice;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.autofill.Dataset;
import android.service.autofill.FillRequest;
import android.service.autofill.InlinePresentation;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.autofill.AutofillId;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.widget.inline.InlinePresentationSpec;

import androidx.autofill.inline.v1.InlineSuggestionUi;
import androidx.autofill.inline.v1.InlineSuggestionUi.Content;

import java.util.Optional;

public class InlineRequestHelper {
    static Optional<InlineSuggestionsRequest> getInlineSuggestionsRequest(FillRequest request) {
        final InlineSuggestionsRequest inlineRequest = request.getInlineSuggestionsRequest();
        if (inlineRequest != null && inlineRequest.getMaxSuggestionCount() > 0
                && !inlineRequest.getInlinePresentationSpecs().isEmpty()) {
            return Optional.of(inlineRequest);
        }
        return Optional.empty();
    }

    static int getMaxSuggestionCount(Optional<InlineSuggestionsRequest> inlineRequest, int max) {
        if (inlineRequest.isPresent()) {
            return Math.min(max, inlineRequest.get().getMaxSuggestionCount());
        }
        return max;
    }

    static InlinePresentation maybeCreateInlineAuthenticationResponse(
            Context context, Optional<InlineSuggestionsRequest> inlineRequest) {
        if (!inlineRequest.isPresent()) {
            return null;
        }
        final PendingIntent attribution = createAttribution(context,
                "Please tap on the chip to authenticate the Autofill response.");
        final Slice slice = createSlice("Tap to auth response", null, null, null, attribution);
        final InlinePresentationSpec spec = inlineRequest.get().getInlinePresentationSpecs().get(0);
        return new InlinePresentation(slice, spec, false);
    }

    static InlinePresentation createInlineDataset(Context context,
            InlineSuggestionsRequest inlineRequest, String value, int index) {
        final PendingIntent attribution = createAttribution(context,
                "Please tap on the chip to autofill the value:" + value);
        final Slice slice = createSlice(value, null, null, null, attribution);
        index = Math.min(inlineRequest.getInlinePresentationSpecs().size() - 1, index);
        final InlinePresentationSpec spec = inlineRequest.getInlinePresentationSpecs().get(index);
        return new InlinePresentation(slice, spec, false);
    }

    static Dataset createInlineActionDataset(Context context,
            ArrayMap<String, AutofillId> fields,
            InlineSuggestionsRequest inlineRequest, int drawable) {
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, new Intent(context, SettingsActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        Dataset.Builder builder =
                new Dataset.Builder()
                        .setInlinePresentation(createInlineAction(context, inlineRequest, drawable))
                        .setAuthentication(pendingIntent.getIntentSender());
        for (AutofillId fieldId : fields.values()) {
            builder.setValue(fieldId, null);
        }
        return builder.build();
    }

    private static InlinePresentation createInlineAction(Context context,
            InlineSuggestionsRequest inlineRequest, int drawable) {
        final PendingIntent attribution = createAttribution(context,
                "Please tap on the chip to launch the action.");
        final Icon icon = Icon.createWithResource(context, drawable);
        final Slice slice = createSlice(null, null, icon, null, attribution);
        // Reuse the first spec's height for the inline action size, as there isn't dedicated
        // value from the request for this.
        final InlinePresentationSpec spec = inlineRequest.getInlinePresentationSpecs().get(0);
        return new InlinePresentation(slice, spec, true);
    }

    private static Slice createSlice(
            String title, String subtitle, Icon startIcon, Icon endIcon,
            PendingIntent attribution) {
        Content.Builder builder = InlineSuggestionUi.newContentBuilder(attribution);
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        if (!TextUtils.isEmpty(subtitle)) {
            builder.setSubtitle(subtitle);
        }
        if (startIcon != null) {
            builder.setStartIcon(startIcon);
        }
        if (endIcon != null) {
            builder.setEndIcon(endIcon);
        }
        return builder.build().getSlice();
    }

    private static PendingIntent createAttribution(Context context, String msg) {
        Intent intent = new Intent(context, AttributionDialogActivity.class);
        intent.putExtra(AttributionDialogActivity.KEY_MSG, msg);
        // Should use different request code to avoid the new intent overriding the old one.
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context, msg.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

}
