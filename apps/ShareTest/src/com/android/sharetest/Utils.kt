/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.sharetest

import android.content.Intent
import android.net.Uri
import android.text.TextUtils

fun createAlternateIntent(intent: Intent): Intent {
    val text = buildString {
        append("Shared URIs:")
        intent.extraStream.forEach {
            append("\n * $it")
        }
    }
    return Intent(Intent.ACTION_SEND).apply {
        setText(text)
    }
}

fun Intent.setText(text: CharSequence) {
    if (TextUtils.isEmpty(type)) {
        type = "text/plain"
    }
    putExtra(Intent.EXTRA_TEXT, text)
}

private val Intent.extraStream: List<Uri>
    get() = buildList {
        when (action) {
            Intent.ACTION_SEND -> getParcelableExtra(
                Intent.EXTRA_STREAM,
                Uri::class.java
            )?.let { add(it) }

            Intent.ACTION_SEND_MULTIPLE -> getParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                Uri::class.java
            )?.let { uris ->
                for (uri in uris) {
                    if (uri != null) {
                        add(uri)
                    }
                }
            }
        }
    }
