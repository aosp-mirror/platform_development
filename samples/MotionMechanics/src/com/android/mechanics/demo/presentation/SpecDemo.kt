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

@file:OptIn(ExperimentalAnimatableApi::class)

package com.android.mechanics.demo.presentation

import android.util.Log
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.compose.animation.Easings
import com.android.mechanics.debug.DebugMotionValueVisualization
import com.android.mechanics.debug.debugMotionValue
import com.android.mechanics.demo.staging.defaultSpatialSpring
import com.android.mechanics.demo.staging.rememberDistanceGestureContext
import com.android.mechanics.demo.staging.rememberMotionValue
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.demo.tuneable.Dropdown
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder
import com.android.mechanics.spring.SpringParameters

object SpecDemo : Demo<SpecDemo.Config> {
    enum class Scenario(val label: String) {
        Empty("Simple"),
        Toggle("Toggle"),
        Steps("Discrete Steps"),
        TrackNSnap("Track and Snap"),
        EasingComparison("Easing Comparison"),
    }

    data class Config(val defaultSpring: SpringParameters)

    var inputRange by mutableStateOf(0f..0f)

    @Composable
    override fun DemoUi(config: Config, modifier: Modifier) {
        val colors = MaterialTheme.colorScheme
        var activeScenario by remember { mutableStateOf(Scenario.Empty) }

        // Also using GestureContext.dragOffset as input.
        val gestureContext = rememberDistanceGestureContext()
        val spec = rememberSpec(activeScenario, inputOutputRange = inputRange, config)
        val motionValue = rememberMotionValue(gestureContext::dragOffset, { spec }, gestureContext)

        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 48.dp),
        ) {

            // Scenario selector
            Row {
                Text("Example Scenario: ")

                Dropdown(
                    activeScenario,
                    Scenario.entries,
                    { it.label },
                    { activeScenario = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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
        scenario: Scenario,
        inputOutputRange: ClosedFloatingPointRange<Float>,
        config: Config,
    ): MotionSpec {

        return remember(scenario, inputOutputRange, config) {
            when (scenario) {
                Scenario.Empty -> MotionSpec.Empty
                Scenario.Toggle -> {
                    MotionSpec.builder(
                            config.defaultSpring,
                            initialMapping = Mapping.Fixed(inputOutputRange.start),
                        )
                        .toBreakpoint((inputOutputRange.start + inputOutputRange.endInclusive) / 2f)
                        .completeWith(Mapping.Fixed(inputOutputRange.endInclusive))
                }

                Scenario.Steps -> {
                    val steps = 8
                    val stepSize = (inputOutputRange.start + inputOutputRange.endInclusive) / steps

                    var underConstruction =
                        MotionSpec.builder(
                            config.defaultSpring,
                            initialMapping = Mapping.Fixed(inputOutputRange.start),
                        )
                    repeat(steps - 1) { step ->
                        underConstruction =
                            underConstruction
                                .toBreakpoint((step + 1) * stepSize)
                                .continueWith(Mapping.Fixed((step + 1) * stepSize))
                    }
                    underConstruction.complete()
                }

                Scenario.TrackNSnap -> {
                    val third = (inputOutputRange.start + inputOutputRange.endInclusive) / 3

                    MotionSpec.builder(
                            config.defaultSpring,
                            initialMapping = Mapping.Fixed(inputOutputRange.start),
                        )
                        .toBreakpoint(third)
                        .jumpTo(third)
                        .continueWithTargetValue(2 * third)
                        .toBreakpoint(2 * third)
                        .completeWith(Mapping.Fixed(inputOutputRange.endInclusive))
                }

                Scenario.EasingComparison -> {
                    val dOut = inputOutputRange.start + inputOutputRange.endInclusive

                    val segmentSizes = buildList {
                        val fourth = (dOut) / 4
                        val sixth = (dOut) / 6
                        repeat(2) {
                            add(
                                (inputOutputRange.start + (it * fourth))..(inputOutputRange.start +
                                        (it + 1) * fourth)
                            )
                        }

                        repeat(3) {
                            add(
                                (inputOutputRange.start +
                                    (it * sixth) +
                                    dOut / 2)..(inputOutputRange.start +
                                        (it + 1) * sixth +
                                        dOut / 2)
                            )
                        }
                    }

                    Log.d("MIKES", "rememberSpec() called $segmentSizes")

                    fun easingMapping(easing: Easing, range: ClosedFloatingPointRange<Float>) =
                        Mapping {
                            val d = range.endInclusive - range.start
                            easing.transform((it - range.start) / d) * (dOut) +
                                inputOutputRange.start
                        }

                    MotionSpec.builder(config.defaultSpring, initialMapping = Mapping.Zero)
                        .toBreakpoint(segmentSizes[0].start)
                        .continueWith(easingMapping(Easings.Emphasized, segmentSizes[0]))
                        .toBreakpoint(segmentSizes[1].start)
                        .continueWith(
                            easingMapping({ Easings.Emphasized.transform(1 - it) }, segmentSizes[1])
                        )
                        .toBreakpoint(segmentSizes[2].start)
                        .continueWith(Mapping.Fixed(inputOutputRange.start))
                        .toBreakpoint(segmentSizes[3].start)
                        .continueWith(Mapping.Fixed(inputOutputRange.endInclusive))
                        .toBreakpoint(segmentSizes[4].start)
                        .completeWith(Mapping.Fixed(inputOutputRange.start))
                }
            }
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

    override val identifier: String = "SpecDemo"
}
