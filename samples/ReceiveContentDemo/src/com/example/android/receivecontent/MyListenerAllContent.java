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
import static com.example.android.receivecontent.Utils.matchesAny;
import static com.example.android.receivecontent.Utils.showMessage;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import android.view.ContentInfo;
import android.view.OnReceiveContentListener;
import android.view.View;

import java.util.ArrayList;

/**
 * Sample implementation that:
 * <ul>
 *     <li>Accepts images and mp4 videos.
 *     <li>Rejects other content URIs.
 *     <li>Coerces all other content to lower-case, plain text and delegates its insertion to the
 *     platform.
 * </ul>
 */
public class MyListenerAllContent implements OnReceiveContentListener {
    static final String[] SUPPORTED_MIME_TYPES = new String[]{"image/*", "video/mp4"};

    @Override
    public ContentInfo onReceiveContent(View view, ContentInfo payload) {
        ClipData clip = payload.getClip();
        ClipDescription description = clip.getDescription();
        ArrayList<ClipData.Item> remainingItems = new ArrayList<>();
        for (int i = 0; i < clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            Uri uri = item.getUri();
            if (uri != null) {
                receive(view, uri);
                continue;
            }
            CharSequence text = item.coerceToText(view.getContext());
            text = text.toString().toLowerCase();
            remainingItems.add(new ClipData.Item(
                    text, item.getHtmlText(), item.getIntent(), item.getUri()));
        }

        if (!remainingItems.isEmpty()) {
            Log.i(LOG_TAG, "Delegating " + remainingItems.size() + " item(s) to platform");
            ClipData newClip = new ClipData(description, remainingItems.get(0));
            for (int i = 1; i < remainingItems.size(); i++) {
                newClip.addItem(remainingItems.get(i));
            }
            return new ContentInfo.Builder(payload).setClip(newClip).build();
        }

        return null;
    }

    private static void receive(View view, Uri contentUri) {
        final String viewClassName = view.getClass().getSimpleName();
        MyExecutors.getBg().submit(() -> {
            ContentResolver contentResolver = view.getContext().getContentResolver();
            String mimeType = contentResolver.getType(contentUri);
            if (!matchesAny(mimeType, SUPPORTED_MIME_TYPES)) {
                showMessage(view, "Content of type " + mimeType + "  is not supported");
                return;
            }
            showMessage(view, viewClassName + ": Received " + mimeType + ": " + contentUri);
        });
    }
}
