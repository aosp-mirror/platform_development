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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.android.mechanics.debug.debugMotionValue
import com.android.mechanics.demo.presentation.MagneticDetachWithOverdragDemo.TargetValue
import com.android.mechanics.demo.presentation.MagneticDetachWithOverdragDemo.inputRange
import com.android.mechanics.demo.staging.rememberDistanceGestureContext
import com.android.mechanics.demo.staging.rememberMotionValue
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.effects.MagneticDetach
import com.android.mechanics.effects.Overdrag
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.SemanticKey
import com.android.mechanics.spec.builder.MotionBuilderContext
import com.android.mechanics.spec.builder.fixedSpatialValueSpec
import com.android.mechanics.spec.builder.rememberMotionBuilderContext
import com.android.mechanics.spec.builder.spatialMotionSpec

object MagneticDetachWithOverdragDemo : Demo<Unit> {

    var inputRange by mutableStateOf(0f..0f)

    @Composable
    override fun DemoUi(config: Unit, modifier: Modifier) {
        val colors = MaterialTheme.colorScheme
        val gestureContext = rememberDistanceGestureContext()
        val motionBuilderContext = rememberMotionBuilderContext()
        var spec by remember() { mutableStateOf(motionBuilderContext.fixedSpatialValueSpec(0f)) }

        val motionValue = rememberMotionValue(gestureContext::dragOffset, { spec }, gestureContext)

        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 48.dp),
        ) {

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
                        Modifier.size(48.dp)
                            .offset {
                                val halfSize = 48.dp.toPx() / 2f
                                val xOffset = (-halfSize + motionValue.output).toInt()
                                IntOffset(x = xOffset, y = 0)
                            }
                            .draggable(
                                rememberDraggableState { gestureContext.dragOffset += it },
                                Orientation.Horizontal,
                                onDragStarted = {
                                    gestureContext.reset(motionValue.output, InputDirection.Max)
                                    spec = motionBuilderContext.createDragSpec()
                                },
                                onDragStopped = {
                                    val targetValue = motionValue[TargetValue] ?: motionValue.output
                                    spec = motionBuilderContext.fixedSpatialValueSpec(targetValue)
                                },
                            )
                            .debugMotionValue(motionValue)
                            .clip(remember { RoundedCornerShape(16.dp) })
                            .background(colors.primary)
                )
            }
        }
    }

    @Composable override fun rememberDefaultConfig() {}

    override val visualizationInputRange: ClosedFloatingPointRange<Float>
        get() = inputRange

    @Composable override fun ColumnScope.ConfigUi(config: Unit, onConfigChanged: (Unit) -> Unit) {}

    override val identifier: String = "MagneticDetachOverdrag"

    val TargetValue = SemanticKey<Float?>()
}

private fun MotionBuilderContext.createDragSpec() = spatialMotionSpec {
    before(inputRange.start, Overdrag(overdragLimit = TargetValue))
    after(inputRange.start, MagneticDetach(semanticAttachedValue = TargetValue))

    before(inputRange.endInclusive, MagneticDetach(semanticAttachedValue = TargetValue))
    after(inputRange.endInclusive, Overdrag(overdragLimit = TargetValue))
}
