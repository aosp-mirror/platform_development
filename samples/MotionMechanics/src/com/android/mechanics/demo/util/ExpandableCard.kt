/*
 * Copyright (C) 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.mechanics.demo.util

import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.UserActionDistance
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateElementIntAsState
import com.android.compose.animation.scene.transitions

object Scenes {
    val Collapsed = SceneKey(debugName = "Collapsed")
    val Expanded = SceneKey(debugName = "Expanded")
}

object Elements {
    val Card = ElementKey("Card")
    val Chevron = ElementKey("Chevron")
}

object Values {
    val ChevronRotation = ValueKey("Rotation")
}

@Composable
fun ExpandableCard(
    modifier: Modifier = Modifier,
    header: @Composable ContentScope.(isExpanded: Boolean) -> Unit = {},
    content: @Composable ContentScope.(isExpanded: Boolean) -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme

    val state = remember {
        MutableSceneTransitionLayoutState(
            Scenes.Collapsed,
            transitions =
                transitions {
                    from(Scenes.Expanded, Scenes.Collapsed) {
                        spec = tween(500)
                        distance = UserActionDistance { fromContent, toContent, orientation ->
                            val expandedSize =
                                Scenes.Expanded.targetSize() ?: return@UserActionDistance 0f
                            val collapsedSize =
                                Scenes.Collapsed.targetSize() ?: return@UserActionDistance 0f

                            (expandedSize.height - collapsedSize.height).toFloat()
                        }
                    }
                },
            motionScheme = motionScheme,
        )
    }
    val coroutineScope = rememberCoroutineScope()

    SceneTransitionLayout(state = state, modifier = modifier) {
        scene(Scenes.Collapsed, mapOf(Swipe.Down to Scenes.Expanded)) {
            ExpansionCard(
                false,
                onToggleExpanded = { state.setTargetScene(Scenes.Expanded, coroutineScope) },
                header = header,
                content = content,
            )
        }
        scene(Scenes.Expanded, mapOf(Swipe.Up to Scenes.Collapsed)) {
            ExpansionCard(
                true,
                onToggleExpanded = { state.setTargetScene(Scenes.Collapsed, coroutineScope) },
                header = header,
                content = content,
            )
        }
    }
}

@Composable
private fun ContentScope.ExpansionCard(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    header: @Composable ContentScope.(isExpanded: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ContentScope.(isExpanded: Boolean) -> Unit,
) {
    Card(modifier = modifier.padding(16.dp).element(Elements.Card)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        ) {
            header(isExpanded)
            Chevron(isExpanded)
        }

        content(isExpanded)
    }
}

@Composable
private fun ContentScope.Chevron(rotate: Boolean, modifier: Modifier = Modifier) {
    val key = Elements.Chevron
    Element(key, modifier) {
        val rotation by animateElementIntAsState(if (rotate) 180 else 0, Values.ChevronRotation)

        content {
            Icon(
                Icons.Default.ExpandMore,
                null,
                Modifier.size(24.dp).drawWithContent {
                    rotate(rotation.toFloat()) { this@drawWithContent.drawContent() }
                },
            )
        }
    }
}
