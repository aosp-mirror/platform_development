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

@file:OptIn(ExperimentalAnimatableApi::class)

package com.android.mechanics.demo.presentation

import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ElementKey
import com.android.compose.modifiers.thenIf
import com.android.mechanics.demo.staging.behavior.reveal.fadeReveal
import com.android.mechanics.demo.staging.behavior.reveal.revealContainer
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.demo.tuneable.LabelledCheckbox
import com.android.mechanics.demo.util.ExpandableCard

object PredefinedMotionDemo : Demo<PredefinedMotionDemo.Config> {
    object Elements {
        val ExpandableContent = ElementKey("ExpandableContent")
    }

    data class Config(val showItemBackground: Boolean)

    @Composable
    override fun DemoUi(config: Config, modifier: Modifier) {
        val colors = MaterialTheme.colorScheme
        ExpandableCard(
            modifier = modifier.fillMaxWidth(),
            header = { Text(text = "Contents", style = typography.titleMedium) },
        ) { isExpanded ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier.fillMaxWidth()
                        .element(Elements.ExpandableContent)
                        .revealContainer(this@ExpandableCard)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(24.dp))
                    repeat(10) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier =
                                Modifier.noResizeDuringTransitions()
                                    .fadeReveal(debug = true)
                                    .fillMaxWidth()
                                    .thenIf(config.showItemBackground) {
                                        Modifier.background(colors.primary)
                                    },
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides
                                    if (config.showItemBackground) colors.onPrimary
                                    else colors.onSurface
                            ) {
                                Icon(Icons.Default.AllInclusive, null)
                                Text(text = "Item ${it + 1}", modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    override fun rememberDefaultConfig(): Config {
        return remember { Config(showItemBackground = false) }
    }

    override var visualizationInputRange by mutableStateOf(0f..1000f)
    override val collapsedGraphHeight: Dp = 20.dp

    @Composable
    override fun ColumnScope.ConfigUi(config: Config, onConfigChanged: (Config) -> Unit) {
        LabelledCheckbox(
            "Show item background",
            config.showItemBackground,
            onCheckedChange = { onConfigChanged(config.copy(showItemBackground = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    override val identifier: String = "predefined_motion"
}
