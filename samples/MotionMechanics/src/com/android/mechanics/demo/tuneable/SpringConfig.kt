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

package com.android.mechanics.demo.tuneable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.android.mechanics.demo.util.asLabel
import com.android.mechanics.spring.SpringParameters
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun SpringParameterSection(
    label: String,
    value: SpringParameters,
    onValueChanged: (SpringParameters) -> Unit,
    sectionKey: String,
    modifier: Modifier = Modifier,
    description: (@Composable () -> Unit)? = null,
) {
    Section(
        label = label,
        summary = { it.asLabel() },
        value = value,
        sectionKey = sectionKey,
        onValueChanged = onValueChanged,
        modifier = modifier,
    ) { springSpec, onSpringSpecChanged ->
        description?.invoke()

        var damping by remember { mutableFloatStateOf(springSpec.dampingRatio) }
        var stiffness by remember { mutableFloatStateOf(springSpec.stiffness) }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Damping")
                SliderWithPreview(
                    value = damping,
                    onValueChange = {
                        damping = it
                        onSpringSpecChanged(SpringParameters(stiffness, damping))
                    },
                    valueRange = 0.1f..2f,
                    normalize = { round(it * 100) / 100f },
                    render = { "%.2f".format(it) },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Stiffness")
                LogarithmicSliderWithPreview(
                    value = stiffness,
                    onValueChange = {
                        stiffness = round(it)
                        onSpringSpecChanged(SpringParameters(stiffness, damping))
                    },
                    valueRange = 50f..50000f,
                    normalize = {
                        val digits = floor(log10(it))
                        val factor = 10f.pow(digits - 1)

                        round(round(it / factor) * factor)
                    },
                    render = { "${it.roundToInt()}" },
                )
            }
        }
    }
}
