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

import static android.content.ContentResolver.SCHEME_CONTENT;

import static com.example.android.receivecontent.ReceiveContentDemoActivity.LOG_TAG;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.OnReceiveContentCallback;
import android.widget.TextView;
import android.widget.TextViewOnReceiveContentCallback;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

/**
 * Sample implementation that accepts all content. This implementation converts all text to
 * lowercase plain text, handles image and video URIs, and delegates handling for all other content
 * to the platform.
 */
public class OnReceiveContentCallbackAllTypes extends TextViewOnReceiveContentCallback {
    static final String[] SUPPORTED_MIME_TYPES = new String[] {"*/*"};

    private static final Set<String> SUPPORTED_CONTENT_URI_MIME_TYPES =
            new ArraySet<>(new String[] {"image/*", "video/mp4"});

    @Override
    public boolean onReceiveContent(TextView view, Payload payload) {
        ClipData clip = payload.getClip();
        ClipDescription description = clip.getDescription();
        ContentResolver contentResolver = view.getContext().getContentResolver();
        ArrayList<ClipData.Item> remainingItems = new ArrayList<>();
        for (int i = 0; i < clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            Uri uri = item.getUri();
            if (uri != null) {
                String mimeType = getMimeType(uri, contentResolver);
                if (isSupportedUri(uri, mimeType)) {
                    receive(view, uri, mimeType);
                    continue;
                }
            }
            CharSequence text = item.getText();
            if (!TextUtils.isEmpty(text)) {
                Log.i(LOG_TAG, "Converting text item to lowercase plain text");
                text = text.toString().toLowerCase();
                remainingItems.add(new ClipData.Item(
                        text, item.getHtmlText(), item.getIntent(), item.getUri()));
                continue;
            }
            remainingItems.add(item);
        }
        if (!remainingItems.isEmpty()) {
            Log.i(LOG_TAG, "Delegating to super for " + remainingItems.size() + " item(s)");
            ClipData newClip = new ClipData(description, remainingItems.get(0));
            for (int i = 1; i < remainingItems.size(); i++) {
                newClip.addItem(remainingItems.get(i));
            }
            Payload newPayload =
                    new OnReceiveContentCallback.Payload.Builder(newClip, payload.getSource())
                            .setFlags(payload.getFlags())
                            .setLinkUri(payload.getLinkUri())
                            .setExtras(payload.getExtras())
                            .build();
            super.onReceiveContent(view, newPayload);
        }

        return true;
    }

    private void receive(TextView view, Uri contentUri, String mimeType) {
        String msg = "Received " + mimeType + ": " + contentUri;
        Log.i(LOG_TAG, msg);
        Toast.makeText(view.getContext(), msg, Toast.LENGTH_LONG).show();
    }

    private static String getMimeType(Uri uri, ContentResolver contentResolver) {
        if (uri == null || !SCHEME_CONTENT.equals(uri.getScheme())) {
            return null;
        }
        return contentResolver.getType(uri);
    }

    private static boolean isSupportedUri(Uri uri, String mimeType) {
        if (uri == null || mimeType == null || mimeType.contains("*")) {
            return false;
        }
        for (String supportedContentMimeType : SUPPORTED_CONTENT_URI_MIME_TYPES) {
            if (ClipDescription.compareMimeTypes(mimeType, supportedContentMimeType)) {
                return true;
            }
        }
        return false;
    }
}
