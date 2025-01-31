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

package com.android.mechanics.demo.staging.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.compose.modifiers.height
import com.android.mechanics.debug.DebugMotionValueVisualization
import com.android.mechanics.debug.MotionValueDebuggerState
import com.android.mechanics.debug.motionValueDebugger

/**  */
@Composable
fun DebugUi(
    visualizationInputRange: ClosedFloatingPointRange<Float>,
    expandedGraphHeight: Dp,
    collapsedGraphHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    val debuggerState = remember { MotionValueDebuggerState() }

    Box(modifier = modifier.fillMaxHeight()) {
        Box(
            modifier =
                Modifier.fillMaxWidth().motionValueDebugger(debuggerState).align(Alignment.TopStart)
        ) {
            content(Modifier.align(Alignment.TopStart))
        }

        var isExpanded by rememberSaveable(key = "debugUiExpanded") { mutableStateOf(false) }
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.BottomStart)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                ) {
                    Text(text = "Motion Value Visualization", style = typography.titleMedium)
                    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)

                    Icon(
                        Icons.Default.ExpandMore,
                        null,
                        Modifier.size(24.dp).drawWithContent {
                            rotate(rotation) { this@drawWithContent.drawContent() }
                        },
                    )
                }
                val scrollState = rememberScrollState()

                AnimatedVisibility(isExpanded) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.verticalScroll(scrollState),
                    ) {
                        debuggerState.observedMotionValues.forEach {
                            key(it) {
                                var rowExpanded by remember { mutableStateOf(false) }
                                val height by
                                    animateDpAsState(
                                        if (rowExpanded) expandedGraphHeight
                                        else collapsedGraphHeight
                                    )

                                DebugMotionValueVisualization(
                                    it,
                                    visualizationInputRange,
                                    modifier =
                                        Modifier.height { height.toPx().toInt() }
                                            .fillMaxWidth()
                                            .clickable { rowExpanded = !rowExpanded },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
