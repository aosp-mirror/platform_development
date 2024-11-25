/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ProgressConverter
import com.android.compose.animation.scene.demo.DemoOverscrollProgress.Tanh
import kotlin.math.roundToInt
import kotlin.math.tanh

data class DemoConfiguration(
    val notificationsInLockscreen: Int = 2,
    val notificationsInShade: Int = 10,
    val interactiveNotifications: Boolean = false,
    val showMediaPlayer: Boolean = true,
    val isFullscreen: Boolean = false,
    val canChangeSceneOrOverlays: Boolean = true,
    val transitionInterceptionThreshold: Float = 0.05f,
    val springConfigurations: DemoSpringConfigurations = DemoSpringConfigurations.presets[0],
    val useOverscrollSpec: Boolean = true,
    val overscrollProgressConverter: DemoOverscrollProgress = Tanh(maxProgress = 0.2f, tilt = 3f),
    val lsToShadeRequiresFullSwipe: ToggleableState = ToggleableState.Indeterminate,
    val enableOverlays: Boolean = false,
    val transitionBorder: Boolean = true,
) {
    companion object {
        val Saver = run {
            val notificationsInLockscreenKey = "notificationsInLockscreen"
            val notificationsInShadeKey = "notificationsInShade"
            val interactiveNotificationsKey = "interactiveNotifications"
            val showMediaPlayerKey = "showMediaPlayer"
            val isFullscreenKey = "isFullscreen"
            val canChangeSceneOrOverlaysKey = "canChangeSceneOrOverlays"
            val transitionInterceptionThresholdKey = "transitionInterceptionThreshold"
            val springConfigurationsKey = "springConfigurations"
            val useOverscrollSpec = "useOverscrollSpec"
            val overscrollProgress = "overscrollProgress"
            val lsToShadeRequiresFullSwipe = "lsToShadeRequiresFullSwipe"
            val enableOverlays = "enableOverlays"
            val transitionBorder = "transitionBorder"

            mapSaver(
                save = {
                    mapOf(
                        notificationsInLockscreenKey to it.notificationsInLockscreen,
                        notificationsInShadeKey to it.notificationsInShade,
                        interactiveNotificationsKey to it.interactiveNotifications,
                        showMediaPlayerKey to it.showMediaPlayer,
                        isFullscreenKey to it.isFullscreen,
                        canChangeSceneOrOverlaysKey to it.canChangeSceneOrOverlays,
                        transitionInterceptionThresholdKey to it.transitionInterceptionThreshold,
                        springConfigurationsKey to it.springConfigurations.save(),
                        useOverscrollSpec to it.useOverscrollSpec,
                        overscrollProgress to it.overscrollProgressConverter.save(),
                        lsToShadeRequiresFullSwipe to it.lsToShadeRequiresFullSwipe,
                        enableOverlays to it.enableOverlays,
                        transitionBorder to it.transitionBorder,
                    )
                },
                restore = {
                    DemoConfiguration(
                        notificationsInLockscreen = it[notificationsInLockscreenKey] as Int,
                        notificationsInShade = it[notificationsInShadeKey] as Int,
                        interactiveNotifications = it[interactiveNotificationsKey] as Boolean,
                        showMediaPlayer = it[showMediaPlayerKey] as Boolean,
                        isFullscreen = it[isFullscreenKey] as Boolean,
                        canChangeSceneOrOverlays = it[canChangeSceneOrOverlaysKey] as Boolean,
                        transitionInterceptionThreshold =
                            it[transitionInterceptionThresholdKey] as Float,
                        springConfigurations =
                            it[springConfigurationsKey].restoreSpringConfigurations(),
                        useOverscrollSpec = it[useOverscrollSpec] as Boolean,
                        overscrollProgressConverter =
                            it[overscrollProgress].restoreOverscrollProgress(),
                        lsToShadeRequiresFullSwipe =
                            it[lsToShadeRequiresFullSwipe] as ToggleableState,
                        enableOverlays = it[enableOverlays] as Boolean,
                        transitionBorder = it[transitionBorder] as Boolean,
                    )
                },
            )
        }
    }
}

data class DemoSpringConfigurations(
    val name: String,
    val systemUiSprings: SpringConfiguration,
    val notificationSprings: SpringConfiguration,
) {
    fun save(): String =
        listOf(
                systemUiSprings.stiffness,
                systemUiSprings.dampingRatio,
                notificationSprings.stiffness,
                notificationSprings.dampingRatio,
            )
            .joinToString(",")

    companion object {
        val presets =
            listOf(
                DemoSpringConfigurations(
                    name = "Default",
                    systemUiSprings =
                        SpringConfiguration(
                            Spring.StiffnessMediumLow,
                            Spring.DampingRatioLowBouncy,
                        ),
                    notificationSprings =
                        SpringConfiguration(Spring.StiffnessMediumLow, Spring.DampingRatioLowBouncy),
                ),
                DemoSpringConfigurations(
                    name = "NotBouncy Fast",
                    systemUiSprings =
                        SpringConfiguration(Spring.StiffnessMedium, Spring.DampingRatioNoBouncy),
                    notificationSprings =
                        SpringConfiguration(Spring.StiffnessMedium, Spring.DampingRatioNoBouncy),
                ),
                DemoSpringConfigurations(
                    name = "NotBouncy Normal",
                    systemUiSprings =
                        SpringConfiguration(Spring.StiffnessMediumLow, Spring.DampingRatioNoBouncy),
                    notificationSprings =
                        SpringConfiguration(Spring.StiffnessMediumLow, Spring.DampingRatioNoBouncy),
                ),
                DemoSpringConfigurations(
                    name = "SlowBouncy",
                    systemUiSprings =
                        SpringConfiguration(
                            stiffness = (Spring.StiffnessLow + Spring.StiffnessVeryLow) / 2f,
                            dampingRatio = 0.85f,
                        ),
                    notificationSprings =
                        SpringConfiguration(Spring.StiffnessMediumLow, Spring.DampingRatioLowBouncy),
                ),
                DemoSpringConfigurations(
                    name = "Less Bouncy",
                    systemUiSprings =
                        SpringConfiguration(
                            stiffness = (Spring.StiffnessMediumLow + Spring.StiffnessLow) / 2f,
                            dampingRatio = 0.8f,
                        ),
                    notificationSprings =
                        SpringConfiguration(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = 0.8f,
                        ),
                ),
                DemoSpringConfigurations(
                    name = "Bouncy",
                    systemUiSprings =
                        SpringConfiguration(
                            stiffness = (Spring.StiffnessMediumLow + Spring.StiffnessLow) / 2f,
                            dampingRatio = Spring.DampingRatioLowBouncy,
                        ),
                    notificationSprings =
                        SpringConfiguration(Spring.StiffnessMediumLow, Spring.DampingRatioLowBouncy),
                ),
                DemoSpringConfigurations(
                    name = "VeryBouncy",
                    systemUiSprings =
                        SpringConfiguration(Spring.StiffnessLow, Spring.DampingRatioMediumBouncy),
                    notificationSprings =
                        SpringConfiguration(
                            Spring.StiffnessMediumLow,
                            Spring.DampingRatioMediumBouncy,
                        ),
                ),
            )

        private fun List<Pair<Float, String>>.addIntermediateValues(): List<Pair<Float, String>> =
            flatMapIndexed { index, curr ->
                if (index == 0) {
                    listOf(curr)
                } else {
                    val prev = get(index - 1)
                    val incr = (curr.first - prev.first) / 4f
                    listOf(
                        prev.first + incr * 1f to "${prev.second}+",
                        prev.first + incr * 2f to "${prev.second}++",
                        prev.first + incr * 3f to "${prev.second}+++",
                        curr,
                    )
                }
            }

        private val stiffnessPairs =
            listOf(
                    Spring.StiffnessVeryLow to "VeryLow",
                    Spring.StiffnessLow to "Low",
                    Spring.StiffnessMediumLow to "MediumLow",
                    Spring.StiffnessMedium to "Medium",
                    Spring.StiffnessHigh to "High",
                )
                .addIntermediateValues()

        val stiffnessNames = stiffnessPairs.toMap()
        val stiffnessValues = stiffnessPairs.map { it.first }

        private val dampingRatioPairs =
            listOf(
                    Spring.DampingRatioNoBouncy to "NoBouncy",
                    0.85f to "VeryLow",
                    Spring.DampingRatioLowBouncy to "Low",
                    Spring.DampingRatioMediumBouncy to "Medium",
                    Spring.DampingRatioHighBouncy to "High",
                )
                .addIntermediateValues()
        val dampingRatioNames = dampingRatioPairs.toMap()
        val dampingRatioValues = dampingRatioPairs.map { it.first }
    }
}

sealed class DemoOverscrollProgress(val name: String, val params: LinkedHashMap<String, Any>) :
    ProgressConverter {
    // Note: the order is guaranteed because we are using an ordered map (LinkedHashMap).
    fun save(): String = "$name:${params.values.joinToString(",")}"

    data object Linear : DemoOverscrollProgress("linear", linkedMapOf()) {
        override fun convert(value: Float) = value
    }

    data class RubberBand(val factor: Float) :
        DemoOverscrollProgress("rubberBand", linkedMapOf("factor" to factor)) {
        override fun convert(value: Float) = value * factor
    }

    data class Tanh(val maxProgress: Float, val tilt: Float) :
        DemoOverscrollProgress("tanh", linkedMapOf("maxValue" to maxProgress, "tilt" to tilt)) {
        override fun convert(value: Float) = maxProgress * tanh(value / (maxProgress * tilt))
    }

    companion object {
        // We are using "by lazy" to avoid a null pointer on "Linear", read more on
        // https://medium.com/livefront/kotlin-a-tale-of-static-cyclical-initialization-3aea530d2053
        val presets by lazy {
            listOf(
                Linear,
                RubberBand(factor = 0.1f),
                RubberBand(factor = 0.15f),
                RubberBand(factor = 0.21f),
                Tanh(maxProgress = 1f, tilt = 1f),
                Tanh(maxProgress = 0.5f, tilt = 1f),
                Tanh(maxProgress = 0.5f, tilt = 1.5f),
                Tanh(maxProgress = 0.7f, tilt = 2f),
                Tanh(maxProgress = 0.2f, tilt = 3f),
            )
        }
    }
}

private fun Any?.restoreSpringConfigurations(): DemoSpringConfigurations {
    val p = (this as String).split(",").map { it.toFloat() }
    return DemoSpringConfigurations(
        name = "Custom",
        systemUiSprings = SpringConfiguration(stiffness = p[0], dampingRatio = p[1]),
        notificationSprings = SpringConfiguration(stiffness = p[2], dampingRatio = p[3]),
    )
}

private fun Any?.restoreOverscrollProgress(): DemoOverscrollProgress {
    val (name, paramsString) = (this as String).split(":")
    val p = paramsString.split(",")
    return when (name) {
        "linear" -> DemoOverscrollProgress.Linear
        "tanh" -> DemoOverscrollProgress.Tanh(maxProgress = p[0].toFloat(), tilt = p[1].toFloat())
        "rubberBand" -> DemoOverscrollProgress.RubberBand(factor = p[0].toFloat())
        else -> error("Unknown OverscrollProgress $name ($paramsString)")
    }
}

data class SpringConfiguration(val stiffness: Float, val dampingRatio: Float)

@Composable
fun DemoConfigurationDialog(
    configuration: DemoConfiguration,
    onConfigurationChange: (DemoConfiguration) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Demo configuration") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(text = "Generic app settings", style = MaterialTheme.typography.titleMedium)

                // Fullscreen.
                Checkbox(
                    label = "Fullscreen",
                    checked = configuration.isFullscreen,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(isFullscreen = !configuration.isFullscreen)
                        )
                    },
                )

                // Can change scene.
                Checkbox(
                    label = "Can change scene or overlays",
                    checked = configuration.canChangeSceneOrOverlays,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(
                                canChangeSceneOrOverlays = !configuration.canChangeSceneOrOverlays
                            )
                        )
                    },
                )

                // Overlays.
                Checkbox(
                    label = "Overlays",
                    checked = configuration.enableOverlays,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(enableOverlays = !configuration.enableOverlays)
                        )
                    },
                )

                // Transition border.
                Checkbox(
                    label = "Transition border",
                    checked = configuration.transitionBorder,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(transitionBorder = !configuration.transitionBorder)
                        )
                    },
                )

                Text(text = "Springs", style = MaterialTheme.typography.titleMedium)

                SpringsPicker(
                    value = configuration.springConfigurations,
                    onValue = {
                        onConfigurationChange(configuration.copy(springConfigurations = it))
                    },
                )

                Text(text = "Scrollable", style = MaterialTheme.typography.titleMedium)

                // Interception threshold.
                val thresholdString =
                    String.format("%.2f", configuration.transitionInterceptionThreshold)
                Text(text = "Interception threshold: $thresholdString")
                Slider(
                    value = configuration.transitionInterceptionThreshold,
                    onValueChange = {
                        onConfigurationChange(
                            configuration.copy(transitionInterceptionThreshold = it)
                        )
                    },
                    valueRange = 0f..0.5f,
                    stepSize = 0.01f,
                )

                Text(text = "Overscroll", style = MaterialTheme.typography.titleMedium)

                Checkbox(
                    label = "Use Overscroll Spec",
                    checked = configuration.useOverscrollSpec,
                    onCheckedChange = {
                        onConfigurationChange(configuration.copy(useOverscrollSpec = it))
                    },
                )

                OverscrollProgressPicker(
                    value = configuration.overscrollProgressConverter,
                    onValue = {
                        onConfigurationChange(configuration.copy(overscrollProgressConverter = it))
                    },
                )

                Text(text = "Media", style = MaterialTheme.typography.titleMedium)

                // Whether we should show the media player.
                Checkbox(
                    label = "Show media player",
                    checked = configuration.showMediaPlayer,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(showMediaPlayer = !configuration.showMediaPlayer)
                        )
                    },
                )

                Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)

                // Whether notifications are interactive
                Checkbox(
                    label = "Interactive notifications",
                    checked = configuration.interactiveNotifications,
                    onCheckedChange = {
                        onConfigurationChange(
                            configuration.copy(
                                interactiveNotifications = !configuration.interactiveNotifications
                            )
                        )
                    },
                )

                // Number of notifications in the Shade scene.
                Counter(
                    "# notifications in Shade",
                    configuration.notificationsInShade,
                    onValueChange = {
                        onConfigurationChange(configuration.copy(notificationsInShade = it))
                    },
                )

                // Number of notifications in the Lockscreen scene.
                Counter(
                    "# notifications in Lockscreen",
                    configuration.notificationsInLockscreen,
                    onValueChange = {
                        onConfigurationChange(configuration.copy(notificationsInLockscreen = it))
                    },
                )

                Text(text = "Lockscreen", style = MaterialTheme.typography.titleMedium)

                // Whether the LS => Shade transition requires a full distance swipe to be
                // committed.
                Checkbox(
                    label = "Require full LS => Shade swipe",
                    state = configuration.lsToShadeRequiresFullSwipe,
                    onStateChange = {
                        onConfigurationChange(configuration.copy(lsToShadeRequiresFullSwipe = it))
                    },
                )
            }
        },
        confirmButton = { Button(onClick = { onDismissRequest() }) { Text("Done") } },
        dismissButton = {
            Button(onClick = { onConfigurationChange(DemoConfiguration()) }) { Text("Reset") }
        },
    )
}

@Composable
private fun Checkbox(
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
        Checkbox(checked, onCheckedChange)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Checkbox(
    label: String,
    state: ToggleableState,
    onStateChange: (ToggleableState) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun onClick() {
        onStateChange(
            when (state) {
                ToggleableState.On -> ToggleableState.Off
                ToggleableState.Off -> ToggleableState.Indeterminate
                ToggleableState.Indeterminate -> ToggleableState.On
            }
        )
    }

    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = ::onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(state, onClick = ::onClick)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Counter(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValueChange((value - 1).coerceAtLeast(0)) }) {
            Icon(Icons.Default.Remove, null)
        }
        Text(value.toString(), Modifier.width(18.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onValueChange((value + 1)) }) { Icon(Icons.Default.Add, null) }
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun <T> Slider(
    value: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    onValueNotFound: () -> Int = { 0 },
) {
    Slider(
        value = (values.indexOf(value).takeIf { it != -1 } ?: onValueNotFound()).toFloat(),
        onValueChange = { onValueChange(values[it.roundToInt()]) },
        valueRange = 0f..values.lastIndex.toFloat(),
        steps = values.lastIndex - 1,
    )
}

@Composable
private fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    stepSize: Float,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = ((valueRange.endInclusive - valueRange.start) / stepSize).toInt() - 1,
    )
}

@Composable
fun SpringsPicker(value: DemoSpringConfigurations, onValue: (DemoSpringConfigurations) -> Unit) {
    Text(text = "Selected: ${value.name}")
    Slider(value = value, onValueChange = onValue, values = DemoSpringConfigurations.presets)

    var isExpanded by remember { mutableStateOf(value.name == "Custom") }
    DisposableEffect(value) { onDispose { isExpanded = true } }
    if (!isExpanded) FilledTonalButton(onClick = { isExpanded = true }) { Text("Show more") }

    AnimatedVisibility(isExpanded, enter = expandVertically()) {
        Column(
            Modifier.animateContentSize()
                .border(1.dp, color = MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(text = "System Ui")

            Text(
                text =
                    buildString {
                        append("stiffness: ")
                        append(String.format("%.2f", value.systemUiSprings.stiffness))
                        append(" (")
                        append(
                            DemoSpringConfigurations.stiffnessNames[value.systemUiSprings.stiffness]
                        )
                        append(")")
                    }
            )

            Slider(
                value = value.systemUiSprings.stiffness,
                onValueChange = {
                    onValue(
                        value.copy(
                            name = "Custom",
                            systemUiSprings = value.systemUiSprings.copy(stiffness = it),
                        )
                    )
                },
                values = DemoSpringConfigurations.stiffnessValues,
            )

            Text(
                text =
                    buildString {
                        append("dampingRatio: ")
                        append(String.format("%.2f", value.systemUiSprings.dampingRatio))
                        append(" (")
                        append(
                            DemoSpringConfigurations.dampingRatioNames[
                                    value.systemUiSprings.dampingRatio]
                        )
                        append(")")
                    }
            )

            Slider(
                value = value.systemUiSprings.dampingRatio,
                onValueChange = {
                    onValue(
                        value.copy(
                            name = "Custom",
                            systemUiSprings = value.systemUiSprings.copy(dampingRatio = it),
                        )
                    )
                },
                DemoSpringConfigurations.dampingRatioValues,
            )

            Text(text = "Notification")

            Text(
                text =
                    buildString {
                        append("stiffness: ")
                        append(String.format("%.2f", value.notificationSprings.stiffness))
                        append(" (")
                        append(
                            DemoSpringConfigurations.stiffnessNames[
                                    value.notificationSprings.stiffness]
                        )
                        append(")")
                    }
            )

            Slider(
                value = value.notificationSprings.stiffness,
                onValueChange = {
                    onValue(
                        value.copy(
                            name = "Custom",
                            notificationSprings = value.notificationSprings.copy(stiffness = it),
                        )
                    )
                },
                DemoSpringConfigurations.stiffnessValues,
            )

            Text(
                text =
                    buildString {
                        append("dampingRatio: ")
                        append(String.format("%.2f", value.notificationSprings.dampingRatio))
                        append(" (")
                        append(
                            DemoSpringConfigurations.dampingRatioNames[
                                    value.notificationSprings.dampingRatio]
                        )
                        append(")")
                    }
            )

            Slider(
                value = value.notificationSprings.dampingRatio,
                onValueChange = {
                    onValue(
                        value.copy(
                            name = "Custom",
                            notificationSprings = value.notificationSprings.copy(dampingRatio = it),
                        )
                    )
                },
                DemoSpringConfigurations.dampingRatioValues,
            )
        }
    }
}

@Composable
fun OverscrollProgressPicker(
    value: DemoOverscrollProgress,
    onValue: (DemoOverscrollProgress) -> Unit,
) {
    Text(text = "Overscroll progress")
    val presets = DemoOverscrollProgress.presets
    Slider(
        value = value,
        onValueChange = onValue,
        values = DemoOverscrollProgress.presets,
        onValueNotFound = { presets.indexOfFirst { it.name == value.name } },
    )

    var isExpanded by remember { mutableStateOf(false) }
    DisposableEffect(value) { onDispose { isExpanded = true } }

    Column(
        Modifier.animateContentSize()
            .border(1.dp, color = MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .padding(4.dp)
            .fillMaxWidth()
    ) {
        Text(text = "Function name: ${value.name}")
        when (value) {
            DemoOverscrollProgress.Linear -> {}
            is DemoOverscrollProgress.Tanh -> {
                Text(text = "Max progress: ${String.format("%.2f", value.maxProgress)}")
                if (isExpanded) {
                    Slider(
                        value = value.maxProgress,
                        onValueChange = { onValue(value.copy(maxProgress = it)) },
                        valueRange = 0.05f..3f,
                        stepSize = 0.05f,
                    )
                }
                Text(text = "Tilt: ${String.format("%.2f", value.tilt)}")
                if (isExpanded) {
                    Slider(
                        value = value.tilt,
                        onValueChange = { onValue(value.copy(tilt = it)) },
                        valueRange = 1f..5f,
                        stepSize = 0.1f,
                    )
                }
            }
            is DemoOverscrollProgress.RubberBand -> {
                Text(text = "Factor: ${String.format("%.2f", value.factor)}")
                if (isExpanded) {
                    Slider(
                        value = value.factor,
                        onValueChange = { onValue(value.copy(factor = it)) },
                        valueRange = 0.01f..1f,
                        stepSize = 0.01f,
                    )
                }
            }
        }
    }

    if (!isExpanded) FilledTonalButton(onClick = { isExpanded = true }) { Text("Show more") }
}
