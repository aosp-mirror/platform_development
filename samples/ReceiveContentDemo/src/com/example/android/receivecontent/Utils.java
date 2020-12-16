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

package com.example.android.receivecontent;

import static com.example.android.receivecontent.ReceiveContentDemoActivity.LOG_TAG;

import android.content.ClipData;
import android.content.ClipDescription;
import android.util.Log;
import android.util.Pair;
import android.view.ContentInfo;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class Utils {
    private Utils() {}

    public static boolean matchesAny(String mimeType, String[] targetMimeTypes) {
        for (String targetMimeType : targetMimeTypes) {
            if (ClipDescription.compareMimeTypes(mimeType, targetMimeType)) {
                return true;
            }
        }
        return false;
    }

    public static void showMessage(View view, String msg) {
        Log.i(LOG_TAG, msg);
        view.getHandler().post(() ->
                Toast.makeText(view.getContext(), msg, Toast.LENGTH_LONG).show()
        );
    }

    /**
     * If you use the support library, use {@code androidx.core.view.ContentInfoCompat.partition()}.
     */
    public static Pair<ContentInfo, ContentInfo> partition(ContentInfo payload,
            Predicate<ClipData.Item> itemPredicate) {
        ClipData clip = payload.getClip();
        if (clip.getItemCount() == 1) {
            boolean matched = itemPredicate.test(clip.getItemAt(0));
            return Pair.create(matched ? payload : null, matched ? null : payload);
        }
        ArrayList<ClipData.Item> acceptedItems = new ArrayList<>();
        ArrayList<ClipData.Item> remainingItems = new ArrayList<>();
        for (int i = 0; i < clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            if (itemPredicate.test(item)) {
                acceptedItems.add(item);
            } else {
                remainingItems.add(item);
            }
        }
        if (acceptedItems.isEmpty()) {
            return Pair.create(null, payload);
        }
        if (remainingItems.isEmpty()) {
            return Pair.create(payload, null);
        }
        ContentInfo accepted = new ContentInfo.Builder(payload)
                .setClip(buildClipData(new ClipDescription(clip.getDescription()), acceptedItems))
                .build();
        ContentInfo remaining = new ContentInfo.Builder(payload)
                .setClip(buildClipData(new ClipDescription(clip.getDescription()), remainingItems))
                .build();
        return Pair.create(accepted, remaining);
    }

    private static ClipData buildClipData(ClipDescription description,
            List<ClipData.Item> items) {
        ClipData clip = new ClipData(new ClipDescription(description), items.get(0));
        for (int i = 1; i < items.size(); i++) {
            clip.addItem(items.get(i));
        }
        return clip;
    }
}
