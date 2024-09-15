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

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.service.chooser.ChooserAction
import android.service.chooser.ChooserTarget
import android.text.TextUtils
import androidx.core.os.bundleOf
import kotlin.math.roundToLong

const val REFINEMENT_ACTION = "com.android.sharetest.REFINEMENT"
private const val EXTRA_IS_INITIAL = "isInitial"

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

fun Intent.setModifyShareAction(context: Context) {
    val modifyShareAction = createModifyShareAction(context, true, 0)
    putExtra(Intent.EXTRA_CHOOSER_MODIFY_SHARE_ACTION, modifyShareAction)
}

fun Bundle.setModifyShareAction(context: Context, count: Int) {
    val modifyShareAction = createModifyShareAction(context, false, count)
    putParcelable(Intent.EXTRA_CHOOSER_MODIFY_SHARE_ACTION, modifyShareAction)
}

// Provide some gaussian noise around the preferred average latency
fun getLatencyMs(avg: Long): Long {
    // Using avg/4 as the standard deviation.
    val noise = avg / 4 * random.nextGaussian()
    return (avg + noise).roundToLong().coerceAtLeast(0)
}

fun getRandomFailure(failureRate: Float): Boolean = random.nextFloat() < failureRate

private val random by lazy { java.util.Random() }

private fun createModifyShareAction(
    context: Context,
    isInitial: Boolean,
    count: Int,
): ChooserAction {
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        Intent(CustomActionFactory.BROADCAST_ACTION)
            .apply {
                this.isInitial = isInitial
            },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
    )
    return ChooserAction.Builder(
        Icon.createWithResource(context, R.drawable.testicon),
        buildString {
            append("Modify Share")
            if (!isInitial) {
                append(" (items: $count)")
            }
        },
        pendingIntent
    ).build()
}

val Intent.extraStream: List<Uri>
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

var Intent.isInitial: Boolean
    set(value) {
        putExtra(EXTRA_IS_INITIAL, value)
    }
    get() = getBooleanExtra(EXTRA_IS_INITIAL, true)

fun createCallerTarget(context: Context, text: String) =
    ChooserTarget(
        "Caller Target",
        Icon.createWithResource(context, R.drawable.launcher_icon),
        1f,
        ComponentName(context, CallerDirectTargetActivity::class.java),
        bundleOf(Intent.EXTRA_TEXT to text)
    )

fun createRefinementIntentSender(context: Context, isInitial: Boolean) =
    PendingIntent.getBroadcast(
        context,
        1,
        Intent(REFINEMENT_ACTION).apply {
            setPackage(context.packageName)
            this.isInitial = isInitial
        },
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT or
            PendingIntent.FLAG_CANCEL_CURRENT

    ).intentSender
