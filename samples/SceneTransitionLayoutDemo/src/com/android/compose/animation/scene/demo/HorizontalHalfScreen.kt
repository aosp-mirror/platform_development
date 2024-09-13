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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.android.compose.animation.scene.SwipeSource
import com.android.compose.animation.scene.SwipeSourceDetector

/** A [SwipeSource] for the left or right half of the device. */
enum class HorizontalHalfScreen(private val onResolve: (LayoutDirection) -> Resolved) :
    SwipeSource {
    Left(onResolve = { Resolved.Left }),
    Right(onResolve = { Resolved.Right }),
    Start(onResolve = { if (it == LayoutDirection.Ltr) Resolved.Left else Resolved.Right }),
    End(onResolve = { if (it == LayoutDirection.Ltr) Resolved.Right else Resolved.Left });

    override fun resolve(layoutDirection: LayoutDirection): SwipeSource.Resolved {
        return onResolve(layoutDirection)
    }

    enum class Resolved : SwipeSource.Resolved {
        Left,
        Right,
    }
}

object HorizontalHalfScreenDetector : SwipeSourceDetector {
    override fun source(
        layoutSize: IntSize,
        position: IntOffset,
        density: Density,
        orientation: Orientation,
    ): SwipeSource.Resolved {
        return if (position.x < layoutSize.width / 2) {
            HorizontalHalfScreen.Resolved.Left
        } else {
            HorizontalHalfScreen.Resolved.Right
        }
    }
}
