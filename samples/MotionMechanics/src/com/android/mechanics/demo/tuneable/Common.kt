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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.mechanics.demo.tuneable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun SliderWithPreview(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    render: (Float) -> String,
    normalize: (Float) -> Float = { it },
    steps: Int = 0,
    modifier: Modifier = Modifier,
) {
    var sliderPosition by remember { mutableStateOf(value) }
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    Slider(
        value = sliderPosition,
        onValueChangeFinished = { onValueChange(sliderPosition) },
        onValueChange = { sliderPosition = normalize(it) },
        valueRange = valueRange.start..valueRange.endInclusive,
        interactionSource = interactionSource,
        steps = steps,
        thumb = {
            Label(
                label = {
                    PlainTooltip(modifier = Modifier.wrapContentWidth(unbounded = true)) {
                        Text(render(sliderPosition), textAlign = TextAlign.Center)
                    }
                },
                interactionSource = interactionSource,
            ) {
                SliderDefaults.Thumb(interactionSource = interactionSource)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun LogarithmicSliderWithPreview(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    render: (Float) -> String,
    normalize: (Float) -> Float = { it },
    steps: Int = 0,
    modifier: Modifier = Modifier,
) {

    SliderWithPreview(
        value = log10(value),
        onValueChange = { onValueChange(10f.pow(it)) },
        valueRange = log10(valueRange.start)..log10(valueRange.endInclusive),
        render = { render(10f.pow(it)) },
        normalize = { log10(normalize(10f.pow(it))) },
        steps = steps,
        modifier = modifier,
    )
}

@Composable
fun <T> Dropdown(
    value: T,
    options: List<T>,
    render: (T) -> String,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        ) {
            Text(render(value), style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(render(option)) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun LabelledCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = { onCheckedChange(!checked) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Checkbox(checked, onCheckedChange)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
fun LabelledRadioButton(
    label: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = isSelected, onClick = { onSelected() }, role = Role.RadioButton)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null, // null recommended for accessibility with screenreaders
        )
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}
