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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneScope
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserActionResult

object NotificationShade {
    object Elements {
        val Content = ElementKey("NotificationShadeContent")
    }

    val UserActions =
        mapOf(
            Back to UserActionResult.HideOverlay(Overlays.Notifications),
            Swipe.Up to UserActionResult.HideOverlay(Overlays.Notifications),
            Swipe.Down(fromSource = SceneContainerEdge.TopEnd) to
                UserActionResult.ReplaceByOverlay(Overlays.QuickSettings),
        )
}

@Composable
fun SceneScope.NotificationShade(
    mediaPlayer: (@Composable SceneScope.() -> Unit)?,
    notificationList: @Composable SceneScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    PartialShade(modifier) {
        Column(Modifier.element(NotificationShade.Elements.Content)) {
            if (mediaPlayer != null) {
                Box(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) { mediaPlayer() }
            }

            // Don't resize the notifications during the reveal.
            Box(Modifier.noResizeDuringTransitions()) { notificationList() }
        }
    }
}
