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

package com.android.mechanics.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.android.mechanics.demo.demos.MagneticOverviewDismiss
import com.android.mechanics.demo.demos.MaterialFadeThrough

object DemoScreens {
    val Home =
        ParentScreen(
            "home",
            mapOf(
                "Magnetic Card Dismiss" to DemoScreen(MagneticOverviewDismiss),
                "Material Fade Through" to DemoScreen(MaterialFadeThrough),
            ),
        )
}

@Composable
fun MechanicsDemo() {
    val rootScreen = DemoScreens.Home

    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = rootScreen.identifier) {
            screen(rootScreen, navController)
        }
    }
}
