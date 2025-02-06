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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.android.compose.animation.scene.ContentScope
import com.android.mechanics.GestureContext
import com.android.mechanics.ProvidedGestureContext
import com.android.mechanics.spec.InputDirection

fun Modifier.revealContainer(contentScope: ContentScope): Modifier =
    this.then(RevealContainerElement(contentScope))

internal class RevealContainerNode(var contentScope: ContentScope) :
    Modifier.Node(),
    TraversableNode,
    ApproachLayoutModifierNode,
    ObserverModifierNode,
    GestureContext {

    var containerHeight by mutableFloatStateOf(0f)
        private set

    override val traverseKey = TRAVERSAL_NODE_KEY

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        return contentScope.layoutState.currentTransition != null
    }

    private var lastGestureContext by
        mutableStateOf<GestureContext>(ProvidedGestureContext(dragOffset = 0f, InputDirection.Max))

    override fun onAttach() {
        updateGestureContext()
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val animatedContainerHeight = constraints.maxHeight

        containerHeight = animatedContainerHeight.toFloat()

        val placeable = measurable.measure(constraints.copy(minHeight = animatedContainerHeight))
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    companion object {
        const val TRAVERSAL_NODE_KEY = "com.android.mechanics.demo.util.REVEAL_CONTAINER_NODE_KEY"
    }

    override fun onObservedReadsChanged() {
        updateGestureContext()
    }

    private fun updateGestureContext() {
        observeReads {
            val gestureContext = contentScope.layoutState.currentTransition?.gestureContext
            if (gestureContext != null) {
                this.lastGestureContext = gestureContext
            }
        }
    }

    override val direction: InputDirection
        get() = lastGestureContext.direction

    override val dragOffset: Float
        get() = lastGestureContext.dragOffset
}

private data class RevealContainerElement(val contentScope: ContentScope) :
    ModifierNodeElement<RevealContainerNode>() {
    override fun create(): RevealContainerNode = RevealContainerNode(contentScope)

    override fun update(node: RevealContainerNode) {
        check(node.contentScope === contentScope)
    }
}
