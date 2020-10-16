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
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import android.view.OnReceiveContentCallback;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Set;

/**
 * Sample implementation that accepts only images. All other content is deferred to default platform
 * behavior.
 */
public class OnReceiveContentCallbackImages implements OnReceiveContentCallback<TextView> {
    private static final Set<String> SUPPORTED_MIME_TYPES = Collections.singleton("image/*");

    @Override
    public Set<String> getSupportedMimeTypes(TextView view) {
        return SUPPORTED_MIME_TYPES;
    }

    @Override
    public boolean onReceiveContent(TextView view, Payload payload) {
        ClipData clip = payload.getClip();
        ContentResolver contentResolver = view.getContext().getContentResolver();
        for (int i = 0; i < clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            Uri uri = item.getUri();
            String mimeType = contentResolver.getType(uri);
            receive(view, uri, mimeType);
        }
        return true;
    }

    private void receive(TextView view, Uri contentUri, String mimeType) {
        String msg = "Received " + mimeType + ": " + contentUri;
        Log.i(LOG_TAG, msg);
        Toast.makeText(view.getContext(), msg, Toast.LENGTH_LONG).show();
    }
}
