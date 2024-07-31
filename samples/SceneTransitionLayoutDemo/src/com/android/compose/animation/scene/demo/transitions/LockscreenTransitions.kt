/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Edge
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneTransitionsBuilder
import com.android.compose.animation.scene.demo.Bouncer
import com.android.compose.animation.scene.demo.Camera
import com.android.compose.animation.scene.demo.Clock
import com.android.compose.animation.scene.demo.DemoConfiguration
import com.android.compose.animation.scene.demo.Launcher
import com.android.compose.animation.scene.demo.Lockscreen
import com.android.compose.animation.scene.demo.MediaPlayer
import com.android.compose.animation.scene.demo.QuickSettings
import com.android.compose.animation.scene.demo.Scenes
import com.android.compose.animation.scene.demo.Shade
import com.android.compose.animation.scene.demo.SmartSpace
import com.android.compose.animation.scene.demo.Stub
import com.android.compose.animation.scene.demo.notification.NotificationList

fun SceneTransitionsBuilder.lockscreenTransitions(configuration: DemoConfiguration) {
    // The transitions between lockscreen/split lockscreen <=> bouncer/launcher/camera/stub are
    // the same.
    commonLockscreenTransitions(Scenes.Lockscreen)
    commonLockscreenTransitions(Scenes.SplitLockscreen)

    if (configuration.useOverscrollSpec) {
        overscroll(Scenes.Lockscreen, Orientation.Vertical) {}

        overscroll(Scenes.StubStart, Orientation.Horizontal) {
            progressConverter = configuration.overscrollProgress::convert
            translate(Stub.Elements.TextStart, x = { absoluteDistance })
        }

        overscroll(Scenes.StubEnd, Orientation.Horizontal) {
            progressConverter = configuration.overscrollProgress::convert
            translate(Stub.Elements.TextEnd, x = { absoluteDistance })
        }
    }
}

val BouncerBackgroundEndProgress = 0.5f

fun SceneTransitionsBuilder.commonLockscreenTransitions(lockscreenScene: SceneKey) {
    from(lockscreenScene, to = Scenes.Bouncer) {
        spec = tween(durationMillis = 500)

        translate(Bouncer.Elements.Content, y = 300.dp)
        fractionRange(end = BouncerBackgroundEndProgress) { fade(Bouncer.Elements.Background) }
        fractionRange(start = BouncerBackgroundEndProgress) { fade(Bouncer.Elements.Content) }
    }

    from(lockscreenScene, to = Scenes.Launcher) {
        spec = tween(durationMillis = 500)

        fractionRange(end = 0.5f) {
            fade(Clock.Elements.Clock)
            fade(SmartSpace.Elements.SmartSpace)
            fade(Lockscreen.Elements.LockButton)
            fade(Camera.Elements.Button)
            fade(NotificationList.Elements.Notifications)
            fade(MediaPlayer.Elements.MediaPlayer)
            fade(Shade.Elements.BatteryPercentage)
            fade(QuickSettings.Elements.Operator)
        }

        fractionRange(start = 0.5f) {
            fade(Launcher.Elements.Scene)
            scaleDraw(Launcher.Elements.Scene, 0.5f, 0.5f)
        }
    }

    from(lockscreenScene, to = Scenes.StubStart) {
        spec = tween(durationMillis = 500)

        translate(Stub.Elements.SceneStart, Edge.Start)
        translate(Lockscreen.Elements.Scene, Edge.End)
    }

    from(lockscreenScene, to = Scenes.StubEnd) {
        spec = tween(durationMillis = 500)

        translate(Stub.Elements.SceneEnd, Edge.End)
        translate(Lockscreen.Elements.Scene, Edge.Start)
    }

    from(lockscreenScene, to = Scenes.Camera) {
        spec = tween(durationMillis = 350)

        fade(Camera.Elements.Background)
    }
}
