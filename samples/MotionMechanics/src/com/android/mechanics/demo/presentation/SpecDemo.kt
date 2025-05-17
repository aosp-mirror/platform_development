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
import com.android.mechanics.debug.DebugMotionValueVisualization
import com.android.mechanics.debug.debugMotionValue
import com.android.mechanics.demo.staging.rememberDistanceGestureContext
import com.android.mechanics.demo.staging.rememberMotionValue
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.demo.tuneable.Dropdown
import com.android.mechanics.demo.tuneable.LabelledCheckbox
import com.android.mechanics.spec.DirectionalMotionSpec
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder.rememberMotionBuilderContext
import com.android.mechanics.spec.builder.spatialDirectionalMotionSpec

object SpecDemo : Demo<SpecDemo.Config> {
    enum class Scenario(val label: String) {
        Empty("Simple"),
        Toggle("Toggle"),
        Steps("Discrete Steps"),
        TrackNSnap("Track and Snap"),
    }

    data class Config(val stepGuarantee: Boolean)

    var inputRange by mutableStateOf(0f..0f)

    @Composable
    override fun DemoUi(config: Config, modifier: Modifier) {
        val colors = MaterialTheme.colorScheme
        var activeScenario by remember { mutableStateOf(Scenario.Empty) }

        // Also using GestureContext.dragOffset as input.
        val gestureContext = rememberDistanceGestureContext()
        val spec = rememberSpec(activeScenario, config, inputOutputRange = inputRange)
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
        config: Config,
        inputOutputRange: ClosedFloatingPointRange<Float>,
    ): MotionSpec {

        val builderContext = rememberMotionBuilderContext()

        return remember(scenario, inputOutputRange, config, builderContext) {
            MotionSpec(
                when (scenario) {
                    Scenario.Empty -> DirectionalMotionSpec.Empty
                    Scenario.Toggle ->
                        builderContext.spatialDirectionalMotionSpec(
                            Mapping.Fixed(inputOutputRange.start)
                        ) {
                            fixedValue(
                                breakpoint =
                                    (inputOutputRange.start + inputOutputRange.endInclusive) / 2f,
                                value = inputOutputRange.endInclusive,
                            )
                        }

                    Scenario.Steps ->
                        builderContext.spatialDirectionalMotionSpec(
                            Mapping.Fixed(inputOutputRange.start)
                        ) {
                            val steps = 8
                            val stepSize =
                                (inputOutputRange.start + inputOutputRange.endInclusive) / steps

                            val guarantee =
                                if (config.stepGuarantee) Guarantee.InputDelta(stepSize)
                                else Guarantee.None

                            val outDiff =
                                (inputOutputRange.start + inputOutputRange.endInclusive) /
                                    (steps - 1)
                            repeat(steps - 2) { step ->
                                fixedValue(
                                    breakpoint = (step + 1) * stepSize,
                                    value = (step + 1) * outDiff,
                                    guarantee = guarantee,
                                )
                            }

                            fixedValue(
                                breakpoint = inputOutputRange.endInclusive - stepSize,
                                value = inputOutputRange.endInclusive,
                                guarantee = guarantee,
                            )
                        }

                    Scenario.TrackNSnap ->
                        builderContext.spatialDirectionalMotionSpec(
                            Mapping.Fixed(inputOutputRange.start)
                        ) {
                            val third = (inputOutputRange.start + inputOutputRange.endInclusive) / 3

                            target(third, from = third, to = 2 * third)
                            fixedValue(
                                breakpoint = 2 * third,
                                value = inputOutputRange.endInclusive,
                            )
                        }
                }
            )
        }
    }

    @Composable
    override fun rememberDefaultConfig(): Config {
        return remember { Config(stepGuarantee = false) }
    }

    override val visualizationInputRange: ClosedFloatingPointRange<Float>
        get() = inputRange

    @Composable
    override fun ColumnScope.ConfigUi(config: Config, onConfigChanged: (Config) -> Unit) {
        Text("Steps")
        LabelledCheckbox(
            "Use Guarantee",
            config.stepGuarantee,
            { onConfigChanged(config.copy(stepGuarantee = it)) },
        )
    }

    override val identifier: String = "SpecDemo"
}
