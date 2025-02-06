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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.mechanics.debug.DebugMotionValueVisualization
import com.android.mechanics.debug.debugMotionValue
import com.android.mechanics.demo.staging.asMechanics
import com.android.mechanics.demo.staging.defaultSpatialSpring
import com.android.mechanics.demo.staging.rememberDistanceGestureContext
import com.android.mechanics.demo.staging.rememberMotionValue
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.spec.Breakpoint
import com.android.mechanics.spec.BreakpointKey
import com.android.mechanics.spec.DirectionalMotionSpec
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.OnChangeSegmentHandler
import com.android.mechanics.spec.SegmentData
import com.android.mechanics.spec.SegmentKey
import com.android.mechanics.spec.builder
import com.android.mechanics.spec.reverseBuilder
import com.android.mechanics.spring.SpringParameters

object DirectionSpecDemo : Demo<DirectionSpecDemo.Config> {
    object Keys {
        val Start = BreakpointKey("Start")
        val Detach = BreakpointKey("Detach")
        val End = Breakpoint.maxLimit.key
    }

    data class Config(val defaultSpring: SpringParameters)

    var inputRange by mutableStateOf(0f..0f)

    @Composable
    override fun DemoUi(config: Config, modifier: Modifier) {
        val colors = MaterialTheme.colorScheme

        // Also using GestureContext.dragOffset as input.
        val gestureContext = rememberDistanceGestureContext()
        val spec = rememberSpec(inputOutputRange = inputRange, config)
        val motionValue = rememberMotionValue(gestureContext::dragOffset, { spec }, gestureContext)

        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 48.dp),
        ) {
            Text("Change Direction Slop")

            val density = LocalDensity.current
            Slider(
                value = gestureContext.directionChangeSlop,
                valueRange = 0.001f..with(density) { 48.dp.toPx() },
                onValueChange = { gestureContext.directionChangeSlop = it },
                modifier = Modifier.fillMaxWidth(),
            )

            // Output visualization
            val lineColor = colors.primary
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier =
                    Modifier.fillMaxWidth()
                        .onPlaced { inputRange = 0f..it.size.width.toFloat() }
                        .drawBehind {
                            drawLine(
                                lineColor,
                                start = Offset(x = 0f, y = center.y),
                                end = Offset(x = size.width, y = center.y),
                                pathEffect =
                                    PathEffect.dashPathEffect(
                                        floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                                    ),
                            )
                        },
            ) {
                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .offset {
                                val halfSize = 24.dp.toPx() / 2f
                                val xOffset = (-halfSize + motionValue.output).toInt()
                                IntOffset(x = xOffset, y = 0)
                            }
                            .debugMotionValue(motionValue)
                            .clip(remember { RoundedCornerShape(24.dp) })
                            .background(colors.primary)
                )
            }

            // MotionValue visualization
            DebugMotionValueVisualization(
                motionValue,
                inputRange,
                modifier = Modifier.fillMaxWidth().height(64.dp),
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
    ): MotionSpec {
        val delta = inputOutputRange.endInclusive - inputOutputRange.start

        val startPosPx = inputOutputRange.start
        val detachPosPx = delta * .4f
        val attachPosPx = delta * .1f

        val fastSpring = MotionScheme.expressive().fastSpatialSpec<Float>().asMechanics()
        val slowSpring = MotionScheme.expressive().slowSpatialSpec<Float>().asMechanics()

        return remember(inputOutputRange, config) {
            val detachSpec =
                DirectionalMotionSpec.builder(config.defaultSpring, initialMapping = Mapping.Zero)
                    .toBreakpoint(startPosPx, Keys.Start)
                    .continueWith(Mapping.Linear(.3f))
                    .toBreakpoint(detachPosPx, Keys.Detach)
                    .completeWith(Mapping.Identity, slowSpring)

            val attachSpec =
                DirectionalMotionSpec.reverseBuilder(config.defaultSpring)
                    .toBreakpoint(attachPosPx, Keys.Detach)
                    .completeWith(mapping = Mapping.Zero, fastSpring)

            val segmentHandlers =
                mapOf<SegmentKey, OnChangeSegmentHandler>(
                    SegmentKey(Keys.Detach, Keys.End, InputDirection.Min) to
                        { currentSegment, _, newDirection ->
                            if (newDirection != currentSegment.direction) currentSegment else null
                        },
                    SegmentKey(Keys.Start, Keys.Detach, InputDirection.Max) to
                        { currentSegment: SegmentData, newInput: Float, newDirection: InputDirection
                            ->
                            if (newDirection != currentSegment.direction && newInput >= 0)
                                currentSegment
                            else null
                        },
                )

            MotionSpec(
                maxDirection = detachSpec,
                minDirection = attachSpec,
                resetSpring = config.defaultSpring,
                segmentHandlers = segmentHandlers,
            )
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

    override val identifier: String = "DirectionSpecDemo"
}
