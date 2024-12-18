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

import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawn
import androidx.activity.compose.setContent
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.android.compose.modifiers.thenIf

class DemoActivity : ComponentActivity() {
    private companion object {
        const val INITIAL_SCENE_EXTRA = "initial_scene"
        const val FULLSCREEN_EXTRA = "fullscreen"
        const val DISABLE_RIPPLE_EXTRA = "disable_ripple"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)

        val initialScene =
            intent.extras?.getString(INITIAL_SCENE_EXTRA)?.let { scene ->
                Scenes.AllScenes[scene] ?: error("Scene $scene does not exist")
            }

        val isFullscreen = intent.extras?.getBoolean(FULLSCREEN_EXTRA) ?: false
        val disableRipple = intent.extras?.getBoolean(DISABLE_RIPPLE_EXTRA) ?: false

        setContent {
            DemoTheme {
                var configuration by
                    rememberSaveable(stateSaver = DemoConfiguration.Saver) {
                        mutableStateOf(DemoConfiguration(isFullscreen = isFullscreen))
                    }

                SideEffect {
                    if (configuration.isFullscreen) {
                        window.insetsController?.hide(WindowInsets.Type.statusBars())
                    } else {
                        window.insetsController?.show(WindowInsets.Type.statusBars())
                    }
                }

                val indication = if (disableRipple) NoIndication else LocalIndication.current
                CompositionLocalProvider(LocalIndication provides indication) {
                    SystemUi(
                        configuration,
                        { configuration = it },
                        Modifier.thenIf(!configuration.isFullscreen) {
                            Modifier.safeDrawingPadding()
                        },
                        initialScene = initialScene,
                    )
                }

                ReportDrawn()
            }
        }
    }
}

@Composable
private fun DemoTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme =
        if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    val typography = MaterialTheme.typography
    val typographyWithGoogleSans = remember(typography) { typography.withGoogleSans() }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyWithGoogleSans,
        content = content,
    )
}

private fun Typography.withGoogleSans(): Typography {
    val brandFont = DeviceFontFamilyName("google-sans-text")
    val plainFont = DeviceFontFamilyName("google-sans")

    val brand =
        FontFamily(
            Font(brandFont, weight = FontWeight.Medium),
            Font(brandFont, weight = FontWeight.Normal),
        )

    val plain =
        FontFamily(
            Font(plainFont, weight = FontWeight.Medium),
            Font(plainFont, weight = FontWeight.Normal),
        )

    return this.copy(
        displayLarge = this.displayLarge.copy(fontFamily = brand),
        displayMedium = this.displayMedium.copy(fontFamily = brand),
        displaySmall = this.displaySmall.copy(fontFamily = brand),
        headlineLarge = this.headlineLarge.copy(fontFamily = brand),
        headlineMedium = this.headlineMedium.copy(fontFamily = brand),
        headlineSmall = this.headlineSmall.copy(fontFamily = brand),
        titleLarge = this.titleLarge.copy(fontFamily = brand),
        titleMedium = this.titleMedium.copy(fontFamily = plain),
        titleSmall = this.titleSmall.copy(fontFamily = plain),
        bodyLarge = this.bodyLarge.copy(fontFamily = plain),
        bodyMedium = this.bodyMedium.copy(fontFamily = plain),
        bodySmall = this.bodySmall.copy(fontFamily = plain),
        labelLarge = this.labelLarge.copy(fontFamily = plain),
        labelMedium = this.labelMedium.copy(fontFamily = plain),
        labelSmall = this.labelSmall.copy(fontFamily = plain),
    )
}

private data object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = NoIndicationNode()
}

private class NoIndicationNode : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
    }
}
