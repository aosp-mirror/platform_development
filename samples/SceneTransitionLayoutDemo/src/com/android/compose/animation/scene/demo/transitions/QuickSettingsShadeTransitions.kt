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

package com.android.compose.animation.scene.demo.transitions

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import com.android.compose.animation.scene.Edge
import com.android.compose.animation.scene.SceneTransitionsBuilder
import com.android.compose.animation.scene.demo.Overlays
import com.android.compose.animation.scene.demo.QuickSettings
import com.android.compose.animation.scene.demo.QuickSettingsGrid
import com.android.compose.animation.scene.demo.QuickSettingsShade
import com.android.compose.animation.scene.demo.notification.NotificationList

fun SceneTransitionsBuilder.quickSettingsShadeTransitions() {
    to(Overlays.QuickSettings) {
        spec = tween(500)

        notifyStlThatShadeDoesNotResizeDuringThisTransition()

        translate(QuickSettingsShade.Elements.Root, Edge.Top)
        fractionRange(start = 0.5f) {
            fade(QuickSettingsGrid.Elements.Tiles)
            fade(QuickSettings.Elements.PagerIndicators)
        }
    }

    from(Overlays.QuickSettings, to = Overlays.Notifications) {
        spec = tween(500)
        fractionRange(end = 0.5f) {
            fade(QuickSettingsGrid.Elements.Tiles)
            fade(QuickSettings.Elements.PagerIndicators)
        }
        fractionRange(start = 0.5f) { fade(NotificationList.Elements.Notifications) }
    }

    overscroll(Overlays.QuickSettings, Orientation.Vertical) {
        notifyStlThatShadeDoesNotResizeDuringThisTransition()

        translate(QuickSettingsShade.Elements.Root, y = { absoluteDistance })
    }

    overscroll(Overlays.QuickSettings, Orientation.Horizontal) {
        notifyStlThatShadeDoesNotResizeDuringThisTransition()

        translate(QuickSettingsShade.Elements.Root, x = { absoluteDistance })
    }
}
