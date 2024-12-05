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

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneScope
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.transitions

object ParentSTL {
    object Scenes {
        val Left = SceneKey("Left")
        val Right = SceneKey("Right")
    }
}

object ChildSTL {
    object Scenes {
        val Top = SceneKey("Red")
        val Bottom = SceneKey("Red")
    }
}

object Elements {
    val Shared = ElementKey("Shared")
}

@Composable
fun NestedSharedElementDemo(modifier: Modifier = Modifier) {
    Column(modifier) {
        val state = remember {
            MutableSceneTransitionLayoutState(
                ParentSTL.Scenes.Left,
                transitions {
                    from(ParentSTL.Scenes.Left, to = ParentSTL.Scenes.Right) { spec = tween(500) }
                },
            )
        }
        val childState = remember {
            MutableSceneTransitionLayoutState(
                ChildSTL.Scenes.Top,
                transitions {
                    from(ChildSTL.Scenes.Top, to = ChildSTL.Scenes.Bottom) { spec = tween(500) }
                },
            )
        }
        val scope = rememberCoroutineScope()
        SceneTransitionLayout(
            state,
            Modifier.padding(16.dp)
                .border(3.dp, Color.Blue)
                .clickable {
                    val targetScene =
                        when (state.currentScene) {
                            ParentSTL.Scenes.Left -> ParentSTL.Scenes.Right
                            else -> ParentSTL.Scenes.Left
                        }
                    state.setTargetScene(targetScene, scope)
                }
                .padding(16.dp),
        ) {
            scene(ParentSTL.Scenes.Right) {
                Box(Modifier.fillMaxSize()) {
                    SharedElement(Modifier.size(30.dp).align(Alignment.TopEnd))
                }
            }

            scene(ParentSTL.Scenes.Left) {
                Box(Modifier.fillMaxSize()) {
                    ChildSTL(
                        childState,
                        Modifier.align(Alignment.Center).fillMaxSize(fraction = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentScope.ChildSTL(
    state: MutableSceneTransitionLayoutState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    NestedSceneTransitionLayout(
        state,
        modifier.border(3.dp, Color.Red).clickable {
            val targetScene =
                when (state.currentScene) {
                    ChildSTL.Scenes.Top -> ChildSTL.Scenes.Bottom
                    else -> ChildSTL.Scenes.Top
                }
            state.setTargetScene(targetScene, scope)
        },
    ) {
        scene(ChildSTL.Scenes.Top) {
            Box(Modifier.fillMaxSize()) { SharedElement(Modifier.size(100.dp)) }
        }
        scene(ChildSTL.Scenes.Bottom) {
            Box(Modifier.fillMaxSize()) {
                SharedElement(Modifier.align(Alignment.BottomStart).size(60.dp))
            }
        }
    }
}

@Composable
private fun SceneScope.SharedElement(modifier: Modifier = Modifier) {
    Box(modifier.element(Elements.Shared).background(Color.Green, CircleShape))
}
