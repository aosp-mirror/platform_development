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

package com.android.mechanics.demo.tuneable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

typealias ConfigurationContent<T> =
    @Composable ColumnScope.(value: T, onValueChanged: (T) -> Unit) -> Unit

@Composable
fun SectionDescription(shortDescription: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(shortDescription, style = MaterialTheme.typography.bodySmall)
    }
}

data class SectionData(
    val keyPrefix: String,
    val expansionStateFactory: (String) -> MutableState<Boolean>,
)

val LocalSectionData = staticCompositionLocalOf<SectionData> { throw AssertionError() }

@Composable
fun <T> Section(
    label: String,
    summary: (T) -> String,
    value: T,
    onValueChanged: (T) -> Unit,
    sectionKey: String,
    modifier: Modifier = Modifier,
    showSummaryWhenExpanded: Boolean = false,
    subsections: ConfigurationContent<T>? = null,
    content: ConfigurationContent<T>,
) {
    val sectionData = LocalSectionData.current
    val isExpanded = sectionData.expansionStateFactory(sectionKey)

    val borderColor by
        animateColorAsState(
            if (isExpanded.value) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
            label = "Border",
        )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth().clickable { isExpanded.value = !isExpanded.value },
            ) {
                val iconRotation by
                    animateFloatAsState(
                        if (isExpanded.value) 0f else 180f,
                        label = "Expansion icon rotation",
                    )
                Icon(
                    Icons.Outlined.ExpandLess,
                    contentDescription = "Expand / collapse section",
                    modifier = Modifier.padding(4.dp).rotate(iconRotation),
                )

                Text(label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                AnimatedVisibility(
                    showSummaryWhenExpanded || !isExpanded.value,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        summary(value),
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            AnimatedVisibility(isExpanded.value) {
                Column {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                        // Use an alignment line instead of a padding
                        modifier =
                            Modifier.padding(start = 32.dp, top = 4.dp, end = 8.dp, bottom = 8.dp),
                    ) {
                        content(value, onValueChanged)
                    }

                    if (subsections != null) {
                        val subSectionData =
                            remember(sectionData, sectionKey) {
                                SectionData(
                                    keyPrefix = "${sectionData.keyPrefix}-$sectionKey",
                                    sectionData.expansionStateFactory,
                                )
                            }
                        CompositionLocalProvider(LocalSectionData provides subSectionData) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                subsections(value, onValueChanged)
                            }
                        }
                    }
                }
            }
        }
    }
}
