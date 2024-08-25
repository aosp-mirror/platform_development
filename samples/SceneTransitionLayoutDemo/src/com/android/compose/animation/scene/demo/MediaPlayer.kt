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

package com.android.compose.animation.scene.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MovableElementKey
import com.android.compose.animation.scene.SceneScope
import com.android.compose.animation.scene.StaticElementContentPicker
import com.android.compose.animation.scene.content.state.TransitionState
import kotlin.math.ceil
import kotlin.math.roundToInt

object MediaPlayer {
    object Elements {
        val MediaPlayer = MovableElementKey("MediaPlayer", contentPicker = ContentPicker)
    }

    object Dimensions {
        val Height = 150.dp
    }

    object Shapes {
        val Background = RoundedCornerShape(24.dp)
    }

    object ContentPicker : StaticElementContentPicker {
        override val contents =
            setOf(
                Scenes.Lockscreen,
                Scenes.SplitLockscreen,
                Scenes.Shade,
                Scenes.SplitShade,
                Scenes.QuickSettings,
            )

        override fun contentDuringTransition(
            element: ElementKey,
            transition: TransitionState.Transition,
            fromContentZIndex: Float,
            toContentZIndex: Float
        ): ContentKey {
            return when {
                // During the Lockscreen => Shade transition, the media player is visible in the
                // Lockscreen when progress is in [0; 0.5]. It is then visible in the Shade scene
                // when progress is in [0.8; 1]. We move it half-way through, when progress = 0.65.
                transition.isTransitioning(from = Scenes.Lockscreen, to = Scenes.Shade) -> {
                    if (transition.progress < 0.65f) {
                        Scenes.Lockscreen
                    } else {
                        Scenes.Shade
                    }
                }

                // Same as Lockscreen => Shade, but with reversed progress.
                transition.isTransitioning(from = Scenes.Shade, to = Scenes.Lockscreen) -> {
                    if (transition.progress < 1f - 0.65f) {
                        Scenes.Shade
                    } else {
                        Scenes.Lockscreen
                    }
                }

                // When going from QS <=> Shade we always want to compose the media player in the QS
                // scene, otherwise it will be drawn above the QS footer actions.
                transition.isTransitioningBetween(Scenes.QuickSettings, Scenes.Shade) -> {
                    Scenes.QuickSettings
                }

                // When going from SplitLockscreen to SplitShade, we always compose the media player
                // in the SplitShade, otherwise the shade will fade above it.
                transition.isTransitioningBetween(Scenes.SplitLockscreen, Scenes.SplitShade) ->
                    Scenes.SplitShade
                transition.isTransitioningBetween(Scenes.Lockscreen, Scenes.QuickSettings) ->
                    Scenes.QuickSettings
                else -> pickSingleContentIn(contents, transition, element)
            }
        }
    }
}

@Composable
fun SceneScope.MediaPlayer(
    isPlaying: Boolean,
    onIsPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    MovableElement(MediaPlayer.Elements.MediaPlayer, modifier) {
        content {
            Box(
                Modifier.fillMaxWidth()
                    .height(MediaPlayer.Dimensions.Height)
                    .background(MaterialTheme.colorScheme.tertiary, MediaPlayer.Shapes.Background)
                    .padding(16.dp)
            ) {
                FilledIconButton(
                    onClick = { onIsPlayingChange(!isPlaying) },
                    Modifier.align(Alignment.CenterEnd),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            MaterialTheme.colorScheme.onTertiary
                        ),
                ) {
                    val color = MaterialTheme.colorScheme.tertiary
                    if (isPlaying) {
                        Icon(Icons.Default.Pause, null, tint = color)
                    } else {
                        Icon(Icons.Default.PlayArrow, null, tint = color)
                    }
                }

                Sinusoid(isPlaying = isPlaying)
            }
        }
    }
}

@Composable
private fun BoxScope.Sinusoid(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onTertiary
    val waveWidth = 10.dp
    val maxWaveHeight = 5.dp
    val lineWidth = 3.dp

    val waveHeight by
        animateDpAsState(
            if (isPlaying) maxWaveHeight else 0.dp,
            spring(stiffness = Spring.StiffnessMediumLow)
        )

    val shouldAnimateWave by remember { derivedStateOf { waveHeight > 0.dp } }
    Box(
        modifier.align(Alignment.BottomCenter).fillMaxWidth().height(maxWaveHeight * 2),
        propagateMinConstraints = true,
    ) {
        if (shouldAnimateWave) {
            val translation by
                rememberInfiniteTransition()
                    .animateFloat(
                        0f,
                        1f,
                        infiniteRepeatable(tween(durationMillis = 1_000, easing = LinearEasing)),
                    )

            val path = remember { Path() }
            Canvas(Modifier) {
                val waveWidthPx = waveWidth.toPx()
                val waveHeightPx = waveHeight.toPx()
                val lineWidthPx = lineWidth.toPx()

                clipRect {
                    path.reset()

                    repeat(ceil(size.width / (waveWidthPx * 2)).roundToInt() + 1) { i ->
                        path.moveTo((i - translation) * waveWidthPx * 2, center.y - lineWidthPx / 2)
                        path.relativeQuadraticTo(waveWidthPx / 2, -waveHeightPx, waveWidthPx, 0f)
                        path.relativeQuadraticTo(waveWidthPx / 2, waveHeightPx, waveWidthPx, 0f)
                        path.relativeLineTo(0f, lineWidthPx)
                        path.relativeQuadraticTo(-waveWidthPx / 2, waveHeightPx, -waveWidthPx, 0f)
                        path.relativeQuadraticTo(-waveWidthPx / 2, -waveHeightPx, -waveWidthPx, 0f)
                        path.close()
                    }

                    drawPath(path, color)
                }
            }
        } else {
            Canvas(Modifier) {
                drawLine(
                    color,
                    start = Offset(0f, center.y),
                    end = Offset(size.width, center.y),
                    strokeWidth = lineWidth.toPx(),
                )
            }
        }
    }
}
