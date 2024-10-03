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

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

class UiState {
    var initialScroll = 0

    private val expansionStateByKey = mutableMapOf<String, MutableState<Boolean>>()

    fun isExpanded(key: String): MutableState<Boolean> {
        return expansionStateByKey.computeIfAbsent(key) { mutableStateOf(false) }
    }
}

@Composable
fun <T> ConfigDialog(
    config: T,
    onConfigurationChange: (T) -> Unit,
    onDismissRequest: () -> Unit,
    defaultConfig: T,
    uiState: UiState = remember { UiState() },
    content: ConfigurationContent<T>,
) {

    var tabIndex by rememberSaveable { mutableStateOf(0) }
    val scrollState = rememberScrollState(initial = uiState.initialScroll)

    val rootSectionData =
        remember(uiState) {
            SectionData(keyPrefix = "mechanics", expansionStateFactory = uiState::isExpanded)
        }
    CompositionLocalProvider(LocalSectionData provides rootSectionData) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            text = {
                Column(
                    verticalArrangement = spacedBy(8.dp, Alignment.Top),
                    modifier =
                        Modifier.fillMaxWidth()
                            .verticalScroll(scrollState)
                            .clip(MaterialTheme.shapes.medium),
                ) {
                    content(config, onConfigurationChange)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        uiState.initialScroll = scrollState.value
                        onDismissRequest()
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                Button(onClick = { onConfigurationChange(defaultConfig) }) { Text("Reset") }
            },
        )
    }
}
