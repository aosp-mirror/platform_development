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

package com.android.mechanics.demo.demos

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.outlinedCardColors
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.mechanics.debug.debugMotionValue
import com.android.mechanics.demo.staging.asMechanics
import com.android.mechanics.demo.staging.defaultSpatialSpring
import com.android.mechanics.demo.staging.rememberDistanceGestureContext
import com.android.mechanics.demo.staging.rememberMotionValue
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.demo.tuneable.Section
import com.android.mechanics.demo.tuneable.SliderWithPreview
import com.android.mechanics.demo.tuneable.SpringParameterSection
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
import kotlin.math.abs
import kotlin.math.roundToInt

object MagneticOverviewDismiss : Demo<MagneticOverviewDismiss.Config> {
    object Keys {
        val Start = BreakpointKey("Start")
        val Detach = BreakpointKey("Detach")
        val Dismiss = BreakpointKey("Dismiss")
    }

    data class Config(
        val defaultSpring: SpringParameters,
        val snapSpring: SpringParameters,
        val dismissPosition: Dp = 400.dp,
        val detachPosition: Dp = 200.dp,
        val attachPosition: Dp = 50.dp,
        val startPosition: Dp = 0.dp,
        val velocityThreshold: Dp = 125.dp,
        val overdragDistance: Dp = 24.dp,
    )

    @Composable
    override fun DemoUi(config: Config, modifier: Modifier) {

        val density = LocalDensity.current
        val spec = remember(config) { with(density) { mutableStateOf(createDetachSpec(config)) } }
        val gestureContext = rememberDistanceGestureContext()
        val yOffset = rememberMotionValue(gestureContext::dragOffset, spec::value, gestureContext)

        Column(modifier = modifier.fillMaxSize().padding(64.dp).debugMotionValue(yOffset)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Card(
                    colors = outlinedCardColors(MaterialTheme.colorScheme.primary),
                    modifier =
                        modifier
                            .align(Alignment.Center)
                            .padding(64.dp)
                            .size(width = 150.dp, height = 300.dp)
                            .offset { IntOffset(0, -yOffset.output.toInt()) }
                            .draggable(
                                orientation = Orientation.Vertical,
                                state =
                                    rememberDraggableState { delta ->
                                        gestureContext.dragOffset -= delta
                                    },
                                onDragStopped = { velocity ->
                                    with(yOffset.spec.maxDirection) {
                                        val currentValue = gestureContext.dragOffset
                                        val detachPosition =
                                            breakpoints[findBreakpointIndex(Keys.Detach)].position
                                        val isDismiss =
                                            if (
                                                abs(velocity) >
                                                    with(density) {
                                                        config.velocityThreshold.toPx()
                                                    }
                                            ) {
                                                velocity < 0
                                            } else {
                                                currentValue > detachPosition
                                            }

                                        val snapTarget =
                                            breakpoints[
                                                    findBreakpointIndex(
                                                        if (isDismiss) Keys.Dismiss else Keys.Start
                                                    )]
                                                .position
                                        Animatable(gestureContext.dragOffset).animateTo(
                                            snapTarget
                                        ) {
                                            gestureContext.dragOffset = value
                                        }
                                    }
                                },
                            ),
                ) {}
            }
        }
    }

    // Stuff below is only demo helpers - configuration, stuff that should go to libraries etc.

    @Composable
    override fun ColumnScope.ConfigUi(config: Config, onConfigChanged: (Config) -> Unit) {

        SpringParameterSection(
            label = "Snap Spring",
            value = config.snapSpring,
            onValueChanged = { onConfigChanged(config.copy(snapSpring = it)) },
            sectionKey = "snap_spring",
        )

        Section(
            label = "Detach Position",
            summary = { "$it" },
            value = config.detachPosition,
            onValueChanged = { onConfigChanged(config.copy(detachPosition = it)) },
            sectionKey = "detach_position",
        ) { value, onValueChanged ->
            SliderWithPreview(
                value = value.value,
                valueRange = config.attachPosition.value..config.dismissPosition.value,
                onValueChange = { onValueChanged(it.dp) },
                render = { "$it" },
                normalize = { it.roundToInt().toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Section(
            label = "Attach Position",
            summary = { "$it" },
            value = config.attachPosition,
            onValueChanged = { onConfigChanged(config.copy(attachPosition = it)) },
            sectionKey = "attach_position",
        ) { value, onValueChanged ->
            SliderWithPreview(
                value = value.value,
                valueRange = config.startPosition.value..config.detachPosition.value,
                onValueChange = { onValueChanged(it.dp) },
                render = { "$it" },
                normalize = { it.roundToInt().toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun rememberDefaultConfig(): Config {
        val defaultSpring = defaultSpatialSpring()
        val snapSpring = MotionScheme.expressive().fastSpatialSpec<Float>().asMechanics()
        return remember(defaultSpring, snapSpring) { Config(defaultSpring, snapSpring) }
    }

    fun Density.createDetachSpec(config: Config): MotionSpec {
        val overdragDistancePx = config.overdragDistance.toPx()
        val startPosPx = config.startPosition.toPx()
        val detachPosPx = config.detachPosition.toPx()
        val attachPosPx = config.attachPosition.toPx()
        val dismissPosPx = config.dismissPosition.toPx()

        val dismissedMapping = Mapping.Fixed(dismissPosPx)
        val dismissSpec =
            DirectionalMotionSpec.builder(
                    config.defaultSpring,
                    initialMapping = Mapping.Tanh(overdragDistancePx, 3f),
                )
                .toBreakpoint(startPosPx, Keys.Start)
                .continueWith(Mapping.Linear(.3f))
                .toBreakpoint(detachPosPx, Keys.Detach)
                .continueWith(Mapping.Identity, config.snapSpring)
                .toBreakpoint(dismissPosPx, Keys.Dismiss)
                .completeWith(dismissedMapping)

        val abortSpec =
            DirectionalMotionSpec.reverseBuilder(
                    config.defaultSpring,
                    initialMapping = dismissedMapping,
                )
                .toBreakpoint(dismissPosPx, Keys.Dismiss)
                .continueWith(Mapping.Identity)
                .toBreakpoint(attachPosPx, Keys.Detach)
                .continueWith(mapping = Mapping.Zero, spring = config.snapSpring)
                .toBreakpoint(startPosPx, Keys.Start)
                .completeWith(Mapping.Tanh(overdragDistancePx, 3f))

        val segmentHandlers =
            mapOf<SegmentKey, OnChangeSegmentHandler>(
                SegmentKey(Keys.Detach, Keys.Dismiss, InputDirection.Min) to
                    { currentSegment, _, newDirection ->
                        if (newDirection != currentSegment.direction) currentSegment else null
                    },
                SegmentKey(Keys.Start, Keys.Detach, InputDirection.Max) to
                    { currentSegment: SegmentData, newInput: Float, newDirection: InputDirection ->
                        if (newDirection != currentSegment.direction && newInput >= 0)
                            currentSegment
                        else null
                    },
            )

        return MotionSpec(
            maxDirection = dismissSpec,
            minDirection = abortSpec,
            resetSpring = config.defaultSpring,
            segmentHandlers = segmentHandlers,
        )
    }

    override val identifier: String = "magnetic_dismiss"
}
