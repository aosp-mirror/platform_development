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
import com.android.compose.animation.scene.BaseTransitionBuilder
import com.android.compose.animation.scene.SceneTransitionsBuilder
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.demo.Clock
import com.android.compose.animation.scene.demo.MediaPlayer
import com.android.compose.animation.scene.demo.Overlays
import com.android.compose.animation.scene.demo.PartialShade
import com.android.compose.animation.scene.demo.notification.NotificationList
import com.android.compose.animation.scene.reveal.ContainerRevealHaptics
import com.android.compose.animation.scene.reveal.verticalContainerReveal

fun SceneTransitionsBuilder.notificationShadeTransitions(revealHaptics: ContainerRevealHaptics) {
    to(Overlays.Notifications) {
        spec = tween(500)
        toNotificationShade(revealHaptics)
        sharedElement(Clock.Elements.Clock, elevateInContent = Overlays.Notifications)
        sharedElement(MediaPlayer.Elements.MediaPlayer, elevateInContent = Overlays.Notifications)
        sharedElement(
            NotificationList.Elements.Notifications,
            elevateInContent = Overlays.Notifications,
        )
    }

    from(Overlays.Notifications) {
        spec = tween(500)
        reversed { toNotificationShade(revealHaptics) }
        sharedElement(Clock.Elements.Clock, enabled = false)
        sharedElement(MediaPlayer.Elements.MediaPlayer, enabled = false)
        sharedElement(NotificationList.Elements.Notifications, enabled = false)
    }
}

val ToNotificationShadeStartFadeProgress = 0.5f

private fun TransitionBuilder.toNotificationShade(revealHaptics: ContainerRevealHaptics) {
    verticalContainerReveal(PartialShade.Elements.Root, revealHaptics)
}

fun BaseTransitionBuilder.notifyStlThatShadeDoesNotResizeDuringThisTransition() {
    // Let STL know that the shade and its background are not expected to change during this
    // transition. This allows better handling of the size during interruptions. See
    // b/290930950#comment22 for details.
    scaleSize(PartialShade.Elements.Root, width = 1f, height = 1f)
    scaleSize(PartialShade.Elements.Background, width = 1f, height = 1f)
}
