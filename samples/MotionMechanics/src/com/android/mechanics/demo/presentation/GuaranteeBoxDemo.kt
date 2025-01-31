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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.compose.modifiers.height
import com.android.compose.modifiers.width
import com.android.mechanics.debug.DebugMotionValueVisualization
import com.android.mechanics.demo.staging.defaultSpatialSpring
import com.android.mechanics.demo.staging.rememberDistanceGestureContext
import com.android.mechanics.demo.staging.rememberMotionValue
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.demo.tuneable.DpSlider
import com.android.mechanics.demo.tuneable.Dropdown
import com.android.mechanics.demo.tuneable.SpringParameterSection
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder
import com.android.mechanics.spring.SpringParameters
import kotlin.math.min

object GuaranteeBoxDemo : Demo<GuaranteeBoxDemo.Config> {
    enum class Scenario(val label: String) {
        Mapped("Mapped"),
        Triggered("With Triggers"),
        Guaranteed("With Gurarantee"),
    }

    data class Config(
        val defaultSpring: SpringParameters,
        val boxWidth: Dp,
        val minVisibleWidth: Dp,
        val guaranteeDistance: Dp,
    )

    var inputRange by mutableStateOf(0f..0f)

    @Composable
    override fun DemoUi(config: Config, modifier: Modifier) {
        val colors = MaterialTheme.colorScheme
        var activeScenario by remember { mutableStateOf(Scenario.Guaranteed) }

        var placedBoxWidth by remember { mutableFloatStateOf(0f) }
        var placedBoxX by remember { mutableFloatStateOf(0f) }

        // Also using GestureContext.dragOffset as input.
        val gestureContext = rememberDistanceGestureContext()
        val spec =
            rememberSpec(
                activeScenario,
                { placedBoxX },
                { placedBoxWidth },
                inputOutputRange = inputRange,
                config,
            )
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
            val density = LocalDensity.current
            var minShapeSize by remember { mutableFloatStateOf(with(density) { 16.dp.toPx() }) }

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier.fillMaxWidth().padding(horizontal = 48.dp).onPlaced {
                        inputRange = 0f..it.size.width.toFloat()
                    },
            ) {
                val colors = MaterialTheme.colorScheme
                val stroke =
                    remember(this) {
                        with(density) {
                            val intervals = floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                            Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(intervals))
                        }
                    }

                // To illustrate the effect, use an outer Box to visually hint the spec dimension.
                // In real code, the dimensions for the spec are derived from Lookahead instead
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        Modifier.size(config.boxWidth, 64.dp)
                            .onPlaced {
                                placedBoxWidth = it.size.width.toFloat()
                                placedBoxX = it.positionInParent().x
                            }
                            .drawBehind {
                                drawRoundRect(
                                    colors.primary,
                                    cornerRadius = CornerRadius(24.dp.toPx()),
                                    alpha = 0.1f,
                                    style = stroke,
                                )
                            },
                ) {
                    Box(
                        modifier =
                            Modifier.width { motionValue.floatValue.toInt() }
                                .height {
                                    // Make it look prettier by offsetting the height from
                                    val targetHeight = 64.dp.toPx()
                                    val cornerRadius = 24.dp.toPx() * 2

                                    (targetHeight -
                                            (cornerRadius - motionValue.floatValue).coerceAtLeast(
                                                0f
                                            ))
                                        .toInt()
                                }
                                .clip(remember { RoundedCornerShape(24.dp) })
                                .graphicsLayer {
                                    alpha =
                                        (motionValue.floatValue / (minShapeSize / 2)).coerceIn(
                                            0f,
                                            1f,
                                        )
                                }
                                .background(MaterialTheme.colorScheme.primary)
                    )
                }
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
        x: () -> Float,
        width: () -> Float,
        inputOutputRange: ClosedFloatingPointRange<Float>,
        config: Config,
    ): MotionSpec {

        val density = LocalDensity.current
        val left = x()
        val widthVal = width()
        val right = left + widthVal

        return remember(scenario, inputOutputRange, config, left, widthVal, density) {
            with(density) {
                val guarantee = Guarantee.InputDelta(config.guaranteeDistance.toPx())
                val minSize = config.minVisibleWidth.toPx()
                when (scenario) {
                    Scenario.Mapped ->
                        MotionSpec.builder(config.defaultSpring, initialMapping = Mapping.Zero)
                            .toBreakpoint(left)
                            .jumpTo(0f)
                            .continueWithTargetValue(widthVal)
                            .toBreakpoint(right)
                            .completeWith(Mapping.Fixed(widthVal))

                    Scenario.Triggered ->
                        MotionSpec.builder(config.defaultSpring, initialMapping = Mapping.Zero)
                            .toBreakpoint(min(left + minSize, right))
                            .jumpTo(minSize)
                            .continueWithTargetValue(widthVal - minSize)
                            .toBreakpoint(right)
                            .completeWith(Mapping.Fixed(widthVal))

                    Scenario.Guaranteed ->
                        MotionSpec.builder(config.defaultSpring, initialMapping = Mapping.Zero)
                            .toBreakpoint(min(left + minSize, right))
                            .jumpTo(minSize, guarantee = guarantee)
                            .continueWithTargetValue(widthVal - minSize)
                            .toBreakpoint(right)
                            .completeWith(Mapping.Fixed(widthVal), guarantee = guarantee)
                }
            }
        }
    }

    @Composable
    override fun rememberDefaultConfig(): Config {
        val defaultSpring = defaultSpatialSpring()
        return remember(defaultSpring) {
            Config(
                defaultSpring,
                boxWidth = 192.dp,
                minVisibleWidth = 24.dp,
                guaranteeDistance = 24.dp,
            )
        }
    }

    override val visualizationInputRange: ClosedFloatingPointRange<Float>
        get() = inputRange

    @Composable
    override fun ColumnScope.ConfigUi(config: Config, onConfigChanged: (Config) -> Unit) {

        SpringParameterSection(
            "spring",
            config.defaultSpring,
            { onConfigChanged(config.copy(defaultSpring = it)) },
            "spring",
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Box Width")
        DpSlider(
            config.boxWidth,
            { onConfigChanged(config.copy(boxWidth = it)) },
            valueRange = 48.dp..200.dp,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Min visible width")
        DpSlider(
            config.minVisibleWidth,
            { onConfigChanged(config.copy(minVisibleWidth = it)) },
            valueRange = 0.dp..48.dp,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Guarantee Distance")
        DpSlider(
            config.guaranteeDistance,
            { onConfigChanged(config.copy(guaranteeDistance = it)) },
            valueRange = 0.dp..200.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    override val identifier: String = "GuaranteeBoxDemo"
}
