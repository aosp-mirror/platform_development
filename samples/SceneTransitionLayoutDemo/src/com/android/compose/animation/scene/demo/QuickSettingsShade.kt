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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserActionResult

object QuickSettingsShade {
    object Elements {
        val Content = ElementKey("QuickSettingsShadeContent")
    }

    val UserActions =
        mapOf(
            Back to UserActionResult.HideOverlay(Overlays.QuickSettings),
            Swipe.Up to UserActionResult.HideOverlay(Overlays.QuickSettings),
            Swipe.Down(fromSource = SceneContainerEdge.TopStart) to
                UserActionResult.ReplaceByOverlay(Overlays.Notifications),
        )
}

@Composable
fun ContentScope.QuickSettingsShade(
    qsPager: @Composable ContentScope.() -> Unit,
    mediaPlayer: @Composable (ContentScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    PartialShade(modifier) {
        Column(Modifier.element(QuickSettingsShade.Elements.Content)) {
            val horizontalPaddingModifier = Modifier.padding(horizontal = 16.dp)

            Clock(MaterialTheme.colorScheme.onSurfaceVariant, horizontalPaddingModifier)

            if (mediaPlayer != null) {
                // Ensure that the media player is above the QS tiles when they fade in.
                Box(horizontalPaddingModifier.zIndex(1f)) { mediaPlayer() }
            }

            Spacer(Modifier.padding(top = 16.dp))
            qsPager()
        }
    }
}
