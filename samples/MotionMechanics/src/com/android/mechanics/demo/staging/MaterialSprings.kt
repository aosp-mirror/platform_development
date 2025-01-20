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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.mechanics.demo.staging

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.android.mechanics.spring.SpringParameters

// The [MaterialTheme.motionScheme] contains the material motion tokes, but they are not accessible
// yet for MM Springs.

/** Converts a [SpringSpec] into its [SpringParameters] equivalent. */
fun SpringSpec<*>.asMechanics() = SpringParameters(stiffness, dampingRatio)

/** Converts a [SpringSpec] into its [SpringParameters] equivalent. */
fun FiniteAnimationSpec<*>.asMechanics() =
    with(this as SpringSpec<*>) { SpringParameters(stiffness, dampingRatio) }

private fun MechanicsSpringSpec(factory: () -> FiniteAnimationSpec<Any>) =
    (factory() as SpringSpec<Any>).asMechanics()

@Composable
fun defaultSpatialSpring(): SpringParameters {
    return (MaterialTheme.motionScheme.defaultSpatialSpec<Any>() as SpringSpec<Any>).asMechanics()
}

@Composable
fun defaultEffectSpring(): SpringParameters {
    return (MaterialTheme.motionScheme.defaultSpatialSpec<Any>() as SpringSpec<Any>).asMechanics()
}
