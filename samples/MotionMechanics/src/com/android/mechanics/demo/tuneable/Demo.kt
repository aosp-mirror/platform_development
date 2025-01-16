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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

interface Demo<T> {
    val identifier: String

    @Composable fun rememberDefaultConfig(): T

    @Composable fun ColumnScope.ConfigUi(config: T, onConfigChanged: (T) -> Unit)

    @Composable fun DemoUi(config: T, modifier: Modifier)
}

@Composable
fun <T> Demo<T>.ConfigurableDemo(modifier: Modifier = Modifier) {
    val defaultConfig = rememberDefaultConfig()
    var config by remember(defaultConfig) { mutableStateOf(defaultConfig) }

    var showConfigurationDialog by remember { mutableStateOf(false) }

    if (showConfigurationDialog) {
        ConfigDialog(
            config,
            onConfigurationChange = { config = it },
            onDismissRequest = { showConfigurationDialog = false },
            defaultConfig = defaultConfig,
        ) { value, onValueChanged ->
            ConfigUi(value, onValueChanged)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        DemoUi(config, Modifier.matchParentSize())

        FloatingActionButton(
            onClick = { showConfigurationDialog = true },
            modifier = Modifier.padding(32.dp).align(Alignment.BottomEnd),
        ) {
            Icon(Icons.Filled.Settings, "Config")
        }
    }
}
