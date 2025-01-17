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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.mechanics.demo.staging.debug.DebugUi

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

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { showConfigurationDialog = true }) {
                Icon(Icons.Default.Settings, null)
                Spacer(Modifier.width(8.dp))
                Text("Config")
            }
        }

        DebugUi(modifier = modifier.fillMaxWidth().weight(1f, fill = true)) { contentModifier ->
            DemoUi(config, contentModifier)
        }
    }
}
