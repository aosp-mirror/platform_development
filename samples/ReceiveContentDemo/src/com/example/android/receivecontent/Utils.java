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

import android.content.ClipDescription;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
}
