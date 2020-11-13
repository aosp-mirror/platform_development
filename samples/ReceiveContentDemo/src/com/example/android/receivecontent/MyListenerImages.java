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

import static com.example.android.receivecontent.Utils.matchesAny;
import static com.example.android.receivecontent.Utils.showMessage;

import android.content.ClipData;
import android.content.ContentResolver;
import android.net.Uri;
import android.view.OnReceiveContentListener;
import android.view.View;

import java.util.Map;

/**
 * Sample implementation that accepts images, rejects other URIs, and delegates handling for all
 * non-URI content to the platform.
 */
public class MyListenerImages implements OnReceiveContentListener {
    static final String[] SUPPORTED_MIME_TYPES = new String[]{"image/*"};

    @Override
    public Payload onReceiveContent(View view, Payload payload) {
        Map<Boolean, Payload> split = payload.partition(item -> item.getUri() != null);
        if (split.get(true) != null) {
            ClipData clip = payload.getClip();
            for (int i = 0; i < clip.getItemCount(); i++) {
                receive(view, clip.getItemAt(i).getUri());
            }
        }
        return split.get(false);
    }

    private static void receive(View view, Uri contentUri) {
        MyExecutors.getBg().submit(() -> {
            ContentResolver contentResolver = view.getContext().getContentResolver();
            String mimeType = contentResolver.getType(contentUri);
            if (!matchesAny(mimeType, SUPPORTED_MIME_TYPES)) {
                showMessage(view, "Content of type " + mimeType + "  is not supported");
                return;
            }
            showMessage(view, "Received " + mimeType + ": " + contentUri);
        });
    }
}
