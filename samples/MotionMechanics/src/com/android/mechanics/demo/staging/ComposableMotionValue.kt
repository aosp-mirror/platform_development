/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.demo.staging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import com.android.mechanics.DistanceGestureContext
import com.android.mechanics.GestureContext
import com.android.mechanics.MotionValue
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.MotionSpec

@Composable
fun rememberMotionValue(
    input: () -> Float,
    spec: () -> MotionSpec,
    gestureContext: GestureContext,
    stableThreshold: Float = 0.01f,
    label: String? = null,
): MotionValue {
    val motionValue =
        remember(input) {
            MotionValue(
                input,
                gestureContext,
                initialSpec = spec(),
                label = label,
                stableThreshold = stableThreshold,
            )
        }

    val currentSpec = spec()
    SideEffect {
        // New spec is intentionally only applied after recomposition.
        motionValue.spec = currentSpec
    }

    LaunchedEffect(motionValue) { motionValue.keepRunning() }
    return motionValue
}

@Composable
fun rememberDerivedMotionValue(
    input: MotionValue,
    spec: () -> MotionSpec,
    stableThreshold: Float = 0.01f,
    label: String? = null,
): MotionValue {
    val motionValue =
        remember(input) {
            MotionValue.createDerived(
                input,
                initialSpec = spec(),
                label = label,
                stableThreshold = stableThreshold,
            )
        }

    val currentSpec = spec()
    SideEffect {
        // New spec is intentionally only applied after recomposition.
        motionValue.spec = currentSpec
    }

    LaunchedEffect(motionValue) { motionValue.keepRunning() }
    return motionValue
}

@Composable
fun rememberDistanceGestureContext(
    initDistance: Float = 0f,
    initialDirection: InputDirection = InputDirection.Max,
): DistanceGestureContext {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val gestureContext = remember {
        DistanceGestureContext(initDistance, initialDirection, touchSlop)
    }

    return gestureContext
}
