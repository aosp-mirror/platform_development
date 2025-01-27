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

package com.android.mechanics.demo.demos

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
import com.android.mechanics.demo.staging.behavior.reveal.FadeContentRevealSpec
import com.android.mechanics.demo.staging.behavior.reveal.fadeReveal
import com.android.mechanics.demo.staging.behavior.reveal.rememberFadeContentRevealSpec
import com.android.mechanics.demo.staging.behavior.reveal.revealContainer
import com.android.mechanics.demo.tuneable.Demo
import com.android.mechanics.demo.tuneable.DpSlider
import com.android.mechanics.demo.tuneable.GuaranteeSection
import com.android.mechanics.demo.tuneable.LabelledCheckbox
import com.android.mechanics.demo.tuneable.Section
import com.android.mechanics.demo.tuneable.SpringParameterSection
import com.android.mechanics.demo.util.ExpandableCard

object Elements {
    val ExpandableContent = ElementKey("ExpandableContent")
}

object MaterialFadeThrough : Demo<MaterialFadeThrough.Config> {

    data class Config(val fadeSpec: FadeContentRevealSpec, val showItemBackground: Boolean)

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
                                    .fadeReveal(spec = config.fadeSpec, debug = true)
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
        val fadeRevealSpec = rememberFadeContentRevealSpec(showDelta = 8.dp, hideDelta = 16.dp)
        return remember(fadeRevealSpec) { Config(fadeRevealSpec, showItemBackground = false) }
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

        Section(
            "Show",
            summary = { "" },
            value = config.fadeSpec,
            onValueChanged = { onConfigChanged(config.copy(fadeSpec = it)) },
            sectionKey = "show spec",
            modifier = Modifier.fillMaxWidth(),
        ) { spec, onSpecChanged ->
            SpringParameterSection(
                " Spring",
                spec.showSpring,
                onValueChanged = { onSpecChanged(spec.copy(showSpring = it)) },
                "showspring",
                modifier = Modifier.fillMaxWidth(),
            )
            GuaranteeSection(
                " Guarantee",
                spec.showGuarantee,
                { onSpecChanged(spec.copy(showGuarantee = it)) },
                "showguarantee",
                modifier = Modifier.fillMaxWidth(),
            )

            Text(text = "Delta", modifier = Modifier.padding(start = 8.dp))
            DpSlider(spec.showDelta, { onSpecChanged(spec.copy(showDelta = it)) }, 0.dp..24.dp)
        }

        Section(
            "Hide",
            summary = { "" },
            value = config.fadeSpec,
            onValueChanged = { onConfigChanged(config.copy(fadeSpec = it)) },
            sectionKey = "show spec",
            modifier = Modifier.fillMaxWidth(),
        ) { spec, onSpecChanged ->
            SpringParameterSection(
                "Spring",
                spec.hideSpring,
                onValueChanged = { onSpecChanged(spec.copy(hideSpring = it)) },
                "hidespring",
                modifier = Modifier.fillMaxWidth(),
            )
            GuaranteeSection(
                "Guarantee",
                spec.hideGuarantee,
                { onSpecChanged(spec.copy(hideGuarantee = it)) },
                "hideguarantee",
                modifier = Modifier.fillMaxWidth(),
            )

            Text(text = "Delta", modifier = Modifier.padding(start = 8.dp))
            DpSlider(spec.hideDelta, { onSpecChanged(spec.copy(hideDelta = it)) }, 0.dp..24.dp)
        }
    }

    override val identifier: String = "material_fade_through"
}
