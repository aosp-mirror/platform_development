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

package com.android.mechanics.demo.staging.behavior.reveal

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.android.mechanics.MotionValue
import com.android.mechanics.debug.findMotionValueDebugger
import com.android.mechanics.demo.staging.behavior.reveal.RevealContainerNode.Companion.TRAVERSAL_NODE_KEY
import com.android.mechanics.demo.staging.defaultEffectSpring
import com.android.mechanics.spec.DirectionalMotionSpec
import com.android.mechanics.spec.Guarantee
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder
import com.android.mechanics.spec.reverseBuilder
import com.android.mechanics.spring.SpringParameters
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch

@Composable
fun rememberFadeContentRevealSpec(
    showSpring: SpringParameters = defaultEffectSpring(),
    hideSpring: SpringParameters = defaultEffectSpring(),
    showDelta: Dp = 0.dp,
    hideDelta: Dp = 0.dp,
    showGuarantee: Guarantee = Guarantee.None,
    hideGuarantee: Guarantee = Guarantee.None,
): FadeContentRevealSpec {
    return remember(showSpring, hideSpring, showGuarantee, hideGuarantee) {
        FadeContentRevealSpec(
            showSpring,
            hideSpring,
            showDelta,
            hideDelta,
            showGuarantee,
            hideGuarantee,
        )
    }
}

@Composable
fun Modifier.fadeReveal(
    spec: FadeContentRevealSpec = rememberFadeContentRevealSpec(),
    debug: Boolean = false,
    label: String? = null,
): Modifier = this.then(FadeContentRevealElement(spec, debug, label))

data class FadeContentRevealSpec(
    val showSpring: SpringParameters,
    val hideSpring: SpringParameters,
    val showDelta: Dp,
    val hideDelta: Dp,
    val showGuarantee: Guarantee,
    val hideGuarantee: Guarantee,
)

internal class FadeContentRevealNode(
    private var spec: FadeContentRevealSpec,
    private val debug: Boolean,
    private val label: String?,
) : Modifier.Node(), ApproachLayoutModifierNode {

    private class AttachedState(
        val revealContainerNode: RevealContainerNode,
        val alphaValue: MotionValue,
        val debugDisposer: DisposableHandle?,
    )

    private var attachedState: AttachedState? = null

    fun updateSpec(spec: FadeContentRevealSpec) {
        this.spec = spec
        attachedState?.updateSpec(lookaheadTargetBounds)
    }

    private var lookaheadTargetBounds = Rect.Zero

    private fun AttachedState.updateSpec(lookaheadBounds: Rect) {
        with(requireDensity()) {
            val showSpec =
                DirectionalMotionSpec.builder(
                        initialMapping = Mapping.Zero,
                        defaultSpring = spec.showSpring,
                    )
                    .toBreakpoint(atPosition = lookaheadBounds.bottom + spec.showDelta.toPx())
                    .continueWith(Mapping.One, guarantee = spec.showGuarantee)
                    .complete()

            val hideSpec =
                DirectionalMotionSpec.reverseBuilder(
                        initialMapping = Mapping.One,
                        defaultSpring = spec.hideSpring,
                    )
                    .toBreakpoint(atPosition = lookaheadBounds.bottom + spec.hideDelta.toPx())
                    .continueWith(Mapping.Zero, guarantee = spec.hideGuarantee)
                    .complete()

            alphaValue.spec = MotionSpec(maxDirection = showSpec, minDirection = hideSpec)
        }
    }

    override fun onAttach() {
        val revealContainerNode =
            checkNotNull(findNearestAncestor(TRAVERSAL_NODE_KEY)) as RevealContainerNode

        val alphaValue =
            MotionValue(
                currentInput = revealContainerNode::containerHeight,
                gestureContext = revealContainerNode,
                label = "FadeReveal($label)::alpha",
            )

        var debugDisposer: DisposableHandle? = null
        if (debug) {
            val motionValueDebugger = findMotionValueDebugger()
            if (motionValueDebugger != null) {
                debugDisposer = motionValueDebugger.register(alphaValue)
            } else {
                Log.w(TAG, "Debugging requested, but debugger not found.")
            }
        }

        attachedState = AttachedState(revealContainerNode, alphaValue, debugDisposer)
        coroutineScope.launch { alphaValue.keepRunning() }
    }

    override fun onDetach() {
        attachedState?.debugDisposer?.dispose()
        attachedState = null
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        with(checkNotNull(attachedState)) {
            return revealContainerNode.contentScope.layoutState.currentTransition != null ||
                !alphaValue.isStable
        }
    }

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates
    ): Boolean {
        with(checkNotNull(attachedState)) {
            return revealContainerNode.contentScope.layoutState.currentTransition != null ||
                !alphaValue.isStable
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            if (isLookingAhead && coordinates != null) {

                with(checkNotNull(attachedState)) {
                    lookaheadTargetBounds =
                        Rect(
                            with(revealContainerNode.contentScope.lookaheadScope) {
                                revealContainerNode
                                    .requireLayoutCoordinates()
                                    .localLookaheadPositionOf(coordinates!!)
                            },
                            coordinates!!.size.toSize(),
                        )
                    updateSpec(lookaheadTargetBounds)
                }
            }

            placeable.place(0, 0)
        }
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        return measurable.measure(constraints).run {
            layout(width, height) {
                val revealAlpha = checkNotNull(attachedState).alphaValue.output

                if (revealAlpha < 1) {
                    placeWithLayer(IntOffset.Zero) {
                        alpha = revealAlpha.coerceAtLeast(0f)
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                } else {
                    place(IntOffset.Zero)
                }
            }
        }
    }

    companion object {
        const val TAG = "FadeReveal"
    }
}

internal data class FadeContentRevealElement(
    val spec: FadeContentRevealSpec,
    val debug: Boolean,
    val label: String?,
) : ModifierNodeElement<FadeContentRevealNode>() {
    override fun create(): FadeContentRevealNode = FadeContentRevealNode(spec, debug, label)

    override fun update(node: FadeContentRevealNode) {
        node.updateSpec(spec)
    }
}
