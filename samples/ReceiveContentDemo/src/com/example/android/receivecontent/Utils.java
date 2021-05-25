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

import android.content.ClipData;
import android.content.ClipDescription;
import android.net.Uri;
import android.util.Pair;
import android.view.ContentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class Utils {
    private Utils() {}

    /**
     * If you use Jetpack, use {@code androidx.core.view.ContentInfoCompat.partition()}.
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

    public static List<Uri> collectUris(ClipData clip) {
        List<Uri> uris = new ArrayList<>(clip.getItemCount());
        for (int i = 0; i < clip.getItemCount(); i++) {
            Uri uri = clip.getItemAt(i).getUri();
            if (uri != null) {
                uris.add(uri);
            }
        }
        return uris;
    }
}
