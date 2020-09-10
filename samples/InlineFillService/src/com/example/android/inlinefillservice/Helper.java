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

import android.app.assist.AssistStructure;
import android.content.Context;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

final class Helper {

    /**
     * Displays a toast with the given message.
     */
    static void showMessage(@NonNull Context context, @NonNull CharSequence message) {
        Log.i(TAG, message.toString());
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Extracts the autofillable fields from the request through assist structure.
     */
    static ArrayMap<String, AutofillId> getAutofillableFields(@NonNull FillRequest request) {
        AssistStructure structure = getLatestAssistStructure(request);
        return getAutofillableFields(structure);
    }

    /**
     * Helper method to get the {@link AssistStructure} associated with the latest request
     * in an autofill context.
     */
    @NonNull
    private static AssistStructure getLatestAssistStructure(@NonNull FillRequest request) {
        List<FillContext> fillContexts = request.getFillContexts();
        return fillContexts.get(fillContexts.size() - 1).getStructure();
    }

    /**
     * Parses the {@link AssistStructure} representing the activity being autofilled, and returns a
     * map of autofillable fields (represented by their autofill ids) mapped by the hint associate
     * with them.
     *
     * <p>An autofillable field is a {@link AssistStructure.ViewNode} whose getHint(ViewNode)
     * method.
     */
    @NonNull
    private static ArrayMap<String, AutofillId> getAutofillableFields(
            @NonNull AssistStructure structure) {
        ArrayMap<String, AutofillId> fields = new ArrayMap<>();
        int nodes = structure.getWindowNodeCount();
        for (int i = 0; i < nodes; i++) {
            AssistStructure.ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            addAutofillableFields(fields, node);
        }
        ArrayMap<String, AutofillId> result = new ArrayMap<>();
        int filedCount = fields.size();
        for (int i = 0; i < filedCount; i++) {
            String key = fields.keyAt(i);
            AutofillId value = fields.valueAt(i);
            // For fields with no hint we just use Field
            if (key.equals(value.toString())) {
                result.put("Field:" + i + "-", fields.valueAt(i));
            } else {
                result.put(key, fields.valueAt(i));
            }
        }
        return result;
    }

    /**
     * Adds any autofillable view from the {@link AssistStructure.ViewNode} and its descendants to
     * the map.
     */
    private static void addAutofillableFields(@NonNull Map<String, AutofillId> fields,
            @NonNull AssistStructure.ViewNode node) {
        if (node.getAutofillType() == View.AUTOFILL_TYPE_TEXT) {
            if (!fields.containsValue(node.getAutofillId())) {
                final String key;
                if (node.getHint() != null) {
                    key = node.getHint().toLowerCase();
                } else if (node.getAutofillHints() != null) {
                    key = node.getAutofillHints()[0].toLowerCase();
                } else {
                    key = node.getAutofillId().toString();
                }
                fields.put(key, node.getAutofillId());
            }
        }
        int childrenSize = node.getChildCount();
        for (int i = 0; i < childrenSize; i++) {
            addAutofillableFields(fields, node.getChildAt(i));
        }
    }
}
