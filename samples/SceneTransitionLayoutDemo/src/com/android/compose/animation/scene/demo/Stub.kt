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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneScope
import com.android.compose.animation.scene.Swipe

object Stub {
    fun leftUserActions(lockscreenScene: SceneKey) =
        mapOf(
            Back to lockscreenScene,
            Swipe.Left to lockscreenScene,
        )

    fun rightUserActions(lockscreenScene: SceneKey) =
        mapOf(
            Back to lockscreenScene,
            Swipe.Right to lockscreenScene,
        )

    object Elements {
        val SceneLeft = ElementKey("StubSceneLeft")
        val SceneRight = ElementKey("StubSceneRight")
        val TextLeft = ElementKey("StubTextLeft")
        val TextRight = ElementKey("StubTextRight")
    }
}

@Composable
fun SceneScope.Stub(
    rootKey: ElementKey,
    textKey: ElementKey,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.element(rootKey).fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Stub scene",
            modifier = modifier.element(textKey),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
