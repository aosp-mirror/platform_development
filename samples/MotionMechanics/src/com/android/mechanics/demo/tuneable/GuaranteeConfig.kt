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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.mechanics.demo.util.GuaranteeType
import com.android.mechanics.demo.util.asLabel
import com.android.mechanics.demo.util.type
import com.android.mechanics.spec.Guarantee

@Composable
fun GuaranteeSection(
    label: String,
    value: Guarantee,
    onValueChanged: (Guarantee) -> Unit,
    sectionKey: String,
    modifier: Modifier = Modifier,
    description: (@Composable () -> Unit)? = null,
) {

    val density = LocalDensity.current
    Section(
        label = label,
        summary = { it.asLabel(density) },
        value = value,
        sectionKey = sectionKey,
        onValueChanged = onValueChanged,
        modifier = modifier,
    ) { guarantee, onGuaranteeChanged ->
        description?.invoke()

        var lastDelta by remember {
            mutableFloatStateOf(
                when (guarantee) {
                    is Guarantee.None -> with(density) { 4.dp.toPx() }
                    is Guarantee.InputDelta -> guarantee.delta
                    is Guarantee.GestureDragDelta -> guarantee.delta
                }
            )
        }

        Dropdown(
            value.type,
            GuaranteeType.entries,
            { it.label },
            onChange = {
                onGuaranteeChanged(
                    when (it) {
                        GuaranteeType.None -> Guarantee.None
                        GuaranteeType.Input -> Guarantee.InputDelta(lastDelta)
                        GuaranteeType.Drag -> Guarantee.GestureDragDelta(lastDelta)
                    }
                )
            },
        )

        AnimatedVisibility(value.type != GuaranteeType.None) {
            Text(text = "Delta", modifier = Modifier.padding(start = 8.dp))
            DpSlider(
                with(density) { lastDelta.toDp() },
                {
                    with(density) { lastDelta = it.toPx() }
                    onGuaranteeChanged(
                        when (guarantee) {
                            Guarantee.None -> Guarantee.None
                            is Guarantee.InputDelta -> Guarantee.InputDelta(lastDelta)
                            is Guarantee.GestureDragDelta -> Guarantee.GestureDragDelta(lastDelta)
                        }
                    )
                },
                0.dp..48.dp,
            )
        }
    }
}
