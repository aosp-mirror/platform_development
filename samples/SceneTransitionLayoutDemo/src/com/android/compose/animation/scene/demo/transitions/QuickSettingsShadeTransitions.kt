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
import com.android.compose.animation.scene.demo.Clock
import com.android.compose.animation.scene.demo.MediaPlayer
import com.android.compose.animation.scene.demo.NotificationShade
import com.android.compose.animation.scene.demo.Overlays
import com.android.compose.animation.scene.demo.PartialShade
import com.android.compose.animation.scene.demo.QuickSettingsShade
import com.android.compose.animation.scene.demo.notification.NotificationList

fun SceneTransitionsBuilder.quickSettingsShadeTransitions() {
    to(Overlays.QuickSettings) {
        spec = tween(500)

        translate(QuickSettingsShade.Elements.Root, Edge.Top)

        // Let STL know that the size of the shared background is not expected to change during this
        // transition. This allows better handling of the size during interruptions. See
        // b/290930950#comment22 for details.
        scaleSize(PartialShade.Elements.Background, width = 1f, height = 1f)
    }

    from(Overlays.QuickSettings, to = Overlays.Notifications) {
        spec = tween(500)

        anchoredTranslate(NotificationShade.Elements.Content, PartialShade.Elements.Background)
        anchoredTranslate(QuickSettingsShade.Elements.Content, PartialShade.Elements.Background)

        fractionRange(end = 0.5f) {
            fade(MediaPlayer.Elements.MediaPlayer)
            fade(Clock.Elements.Clock)
        }

        fractionRange(start = 0.5f) { fade(NotificationList.Elements.Notifications) }
    }

    overscroll(Overlays.QuickSettings, Orientation.Vertical) {
        translate(QuickSettingsShade.Elements.Root, y = { absoluteDistance })
    }

    overscroll(Overlays.QuickSettings, Orientation.Horizontal) {
        translate(QuickSettingsShade.Elements.Root, x = { absoluteDistance })
    }
}
