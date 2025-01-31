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

@file:OptIn(ExperimentalAnimatableApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.android.mechanics.demo.presentation

import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.mechanics.debug.DebugMotionValueVisualization
import com.android.mechanics.debug.debugMotionValueGraph
import com.android.mechanics.demo.staging.defaultSpatialSpring
import com.android.mechanics.demo.staging.rememberDistanceGestureContext
import com.android.mechanics.demo.staging.rememberMotionValue
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.spec.Breakpoint
import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder
import com.android.mechanics.spring.SpringParameters

object GuaranteeFadeDemo : Demo<GuaranteeFadeDemo.Config> {
    object Keys {
        val Start = BreakpointKey("Start")
        val Detach = BreakpointKey("Detach")
        val End = Breakpoint.maxLimit.key
    }

    data class Config(val defaultSpring: SpringParameters)

    var inputRange by mutableStateOf(0f..200f)

    @Composable
    override fun DemoUi(config: Config, modifier: Modifier) {
        val colors = MaterialTheme.colorScheme

        val density = LocalDensity.current

        val guaranteeDistance = remember { mutableFloatStateOf(80f) }

        // Also using GestureContext.dragOffset as input.
        val gestureContext = rememberDistanceGestureContext()
        val spec = rememberSpec(inputOutputRange = inputRange, config, { 0f })
        val guaranteeSpec =
            rememberSpec(inputOutputRange = inputRange, config, guaranteeDistance::floatValue)

        val withoutGuarantee =
            rememberMotionValue(gestureContext::dragOffset, { spec }, gestureContext)

        val withGuarantee =
            rememberMotionValue(gestureContext::dragOffset, { guaranteeSpec }, gestureContext)

        val defaultValueColor = MaterialTheme.colorScheme.primary
        val guaranteeValueColor = MaterialTheme.colorScheme.secondary

        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 48.dp),
        ) {
            Text("Guarantee Distance")

            Slider(
                value = guaranteeDistance.value,
                valueRange = inputRange,
                onValueChange = { guaranteeDistance.value = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .graphicsLayer { this.alpha = withoutGuarantee.floatValue }
                            .background(defaultValueColor)
                )
                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .graphicsLayer { this.alpha = withGuarantee.floatValue }
                            .background(guaranteeValueColor)
                )
            }

            // MotionValue visualization
            DebugMotionValueVisualization(
                withGuarantee,
                inputRange,
                modifier =
                    Modifier.fillMaxWidth()
                        .height(64.dp)
                        .debugMotionValueGraph(
                            withoutGuarantee,
                            defaultValueColor,
                            inputRange,
                            0f..1f,
                        ),
            )

            // Input visualization
            Slider(
                value = gestureContext.dragOffset,
                valueRange = inputRange,
                onValueChange = { gestureContext.dragOffset = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    fun rememberSpec(
        inputOutputRange: ClosedFloatingPointRange<Float>,
        config: Config,
        guaranteeDistance: () -> Float,
    ): MotionSpec {
        val distance = guaranteeDistance()
        val guarantee = if (distance > 0) Guarantee.InputDelta(distance) else Guarantee.None

        return remember(guarantee, config, inputOutputRange) {
            MotionSpec.builder(config.defaultSpring, initialMapping = Mapping.Zero)
                .toBreakpoint((inputOutputRange.start + inputOutputRange.endInclusive) / 2f)
                .completeWith(Mapping.One, guarantee = guarantee)
        }
    }

    @Composable
    override fun rememberDefaultConfig(): Config {
        val defaultSpring = defaultSpatialSpring()
        return remember(defaultSpring) { Config(defaultSpring) }
    }

    override val visualizationInputRange: ClosedFloatingPointRange<Float>
        get() = inputRange

    @Composable
    override fun ColumnScope.ConfigUi(config: Config, onConfigChanged: (Config) -> Unit) {}

    override val identifier: String = "GuaranteeFadeDemo"
}
