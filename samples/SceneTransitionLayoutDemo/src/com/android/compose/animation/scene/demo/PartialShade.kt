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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.LowestZIndexContentPicker
import com.android.compose.animation.scene.SceneScope

object PartialShade {
    object Elements {
        val Background =
            ElementKey("PartialShadeBackground", contentPicker = LowestZIndexContentPicker)
    }

    object Colors {
        val Background
            @Composable get() = Shade.Colors.Scrim
    }

    object Shapes {
        val Background = RoundedCornerShape(Shade.Dimensions.ScrimCornerSize)
    }
}

@Composable
fun SceneScope.PartialShade(
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(16.dp),
    innerPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth(0.5f).fillMaxHeight().padding(outerPadding)) {
        Box(
            Modifier.element(PartialShade.Elements.Background)
                .matchParentSize()
                .background(PartialShade.Colors.Background, PartialShade.Shapes.Background)
        )

        Box(Modifier.padding(innerPadding), content = content)
    }
}
