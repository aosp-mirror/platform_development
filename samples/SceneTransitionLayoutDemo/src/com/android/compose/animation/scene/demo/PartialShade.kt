/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.modifiers.thenIf

object PartialShade {
    object Colors {
        val Background
            @Composable get() = Shade.Colors.Scrim
    }

    object Shapes {
        val Background =
            RoundedCornerShape(
                bottomStart = Shade.Dimensions.ScrimCornerSize,
                bottomEnd = Shade.Dimensions.ScrimCornerSize,
            )
        val SplitBackground = RoundedCornerShape(Shade.Dimensions.ScrimCornerSize)
    }
}

@Composable
fun ContentScope.PartialShade(
    rootElement: ElementKey,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isSplitShade = shouldUseSplitScenes(calculateWindowSizeClass())

    val shape =
        if (isSplitShade) PartialShade.Shapes.SplitBackground else PartialShade.Shapes.Background
    val contentOverscrollEffect = checkNotNull(rememberOverscrollEffect())
    Box(
        modifier
            .fillMaxWidth(if (isSplitShade) 0.5f else 1f)
            .overscroll(verticalOverscrollEffect)
            .overscroll(contentOverscrollEffect)
            .thenIf(isSplitShade) { Modifier.padding(16.dp) }
            .element(rootElement)
            .clip(shape)
            .background(PartialShade.Colors.Background)
            .disableSwipesWhenScrolling()
            .verticalScroll(
                rememberScrollState(),
                overscrollEffect = contentOverscrollEffect.withoutVisualEffect(),
            ),
        content = content,
    )
}

// A modifier that ensures that there is no hole between the shade and the top of the device when
// overscrolling it in non-split mode.
private data class ExtraBackgroundElement(val color: Color) :
    ModifierNodeElement<ExtraBackgroundNode>() {
    override fun create(): ExtraBackgroundNode = ExtraBackgroundNode(color)

    override fun update(node: ExtraBackgroundNode) {
        node.color = color
    }
}

private class ExtraBackgroundNode(var color: Color) :
    Modifier.Node(), LayoutAwareModifierNode, DrawModifierNode {
    private var lastY = 0f

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val y = coordinates.positionInWindow().y
        if (y != lastY) {
            lastY = y
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        if (lastY > 0) {
            drawRect(color, topLeft = Offset(0f, -lastY), size = Size(size.width, lastY))
        }

        drawContent()
    }
}
