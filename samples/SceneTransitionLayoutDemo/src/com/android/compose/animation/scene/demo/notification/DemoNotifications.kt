/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.demo.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextMeasurer
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MovableElementKey
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.SceneScope
import com.android.compose.animation.scene.SceneTransitions
import com.android.compose.animation.scene.StaticElementContentPicker
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.demo.SpringConfiguration
import com.android.compose.animation.scene.demo.transitions.ToShadeScrimFadeEndFraction
import com.android.compose.animation.scene.demo.transitions.ToSplitShadeOpaqueBackgroundProgress

fun notifications(
    isInteractive: Boolean,
    n: Int,
    springConfiguration: SpringConfiguration,
    textMeasurer: TextMeasurer,
): List<NotificationViewModel> {
    val transitions = NotificationContent.transitions(springConfiguration)
    return List(n) { notification(isInteractive, it + 1, transitions, textMeasurer) }
}

private fun notification(
    isInteractive: Boolean,
    i: Int,
    transitions: SceneTransitions,
    textMeasurer: TextMeasurer,
    key: MovableElementKey =
        MovableElementKey(
            "Notification:$i",
            identity = NotificationIdentity(),
            contentPicker = NotificationContentPicker,
        ),
): NotificationViewModel {
    return object : NotificationViewModel {
        override val key: MovableElementKey = key
        override val state: MutableSceneTransitionLayoutState =
            MutableSceneTransitionLayoutState(Notification.Scenes.Collapsed, transitions)
        override val isInteractive: Boolean = isInteractive

        override val collapsedContent: @Composable SceneScope.() -> Unit = {
            CollapsedNotificationContent(i, textMeasurer)
        }

        override val expandedContent: @Composable SceneScope.() -> Unit = {
            ExpandedNotificationContent(i, textMeasurer)
        }
    }
}

private object NotificationContentPicker : StaticElementContentPicker {
    override val contents =
        setOf(Scenes.Lockscreen, Scenes.Shade, Scenes.SplitLockscreen, Scenes.SplitShade)

    override fun contentDuringTransition(
        element: ElementKey,
        transition: TransitionState.Transition,
        fromContentZIndex: Float,
        toContentZIndex: Float,
    ): ContentKey {
        return when {
            transition.isTransitioning(from = Scenes.Lockscreen, to = Scenes.Shade) -> Scenes.Shade
            transition.isTransitioning(from = Scenes.Shade, to = Scenes.Lockscreen) -> {
                // From Shade to Lockscreen the shared element animation is disabled, so we first
                // show the notification in the shade/scrim as long as the scrim is opaque, then we
                // show them in the lockscreen once the scrim fades out.
                if (transition.progress < 1f - ToShadeScrimFadeEndFraction) {
                    Scenes.Shade
                } else {
                    Scenes.Lockscreen
                }
            }
            transition.isTransitioning(from = Scenes.SplitLockscreen, to = Scenes.SplitShade) ->
                Scenes.SplitShade
            transition.isTransitioning(from = Scenes.SplitShade, to = Scenes.SplitLockscreen) -> {
                if (transition.progress < 1f - ToSplitShadeOpaqueBackgroundProgress) {
                    Scenes.SplitShade
                } else {
                    Scenes.SplitLockscreen
                }
            }
            else -> pickSingleContentIn(contents, transition, element)
        }
    }
}
