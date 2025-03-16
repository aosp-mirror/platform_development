/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.compose.animation.scene.benchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SceneTransitionLayoutDemoBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    private fun MacrobenchmarkScope.benchmarkScope(): SceneTransitionLayoutBenchmarkScope {
        return object : SceneTransitionLayoutBenchmarkScope {
            override fun startActivity(intent: Intent) = startActivityAndWait(intent)
        }
    }

    @Test
    fun shadeStartup() {
        benchmarkRule.measureRepeated(
            packageName = StlDemoConstants.PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.COLD,
            compilationMode = CompilationMode.Full(),
        ) {
            // Start the demo in the shade. This is our more busy screen because it has a bunch of
            // elements for the quick settings but also nested SceneTransitionLayouts for each
            // notification.
            benchmarkScope().startDemoActivity(StlDemoConstants.SHADE_SCENE)
        }
    }

    @Test
    fun lockscreenToShade() {
        benchmarkSwipeFromScene(
            fromScene = StlDemoConstants.LOCKSCREEN_SCENE,
            toContent = StlDemoConstants.SHADE_SCENE,
            direction = Direction.DOWN,
        )
    }

    @Test
    fun shadeToQuickSettings() {
        benchmarkSwipeFromScene(
            fromScene = StlDemoConstants.SHADE_SCENE,
            toContent = StlDemoConstants.QUICK_SETTINGS_SCENE,
            direction = Direction.DOWN,
        )
    }

    @Test
    fun lockscreenToNotificationsOverlay() {
        benchmarkSwipeFromScene(
            fromScene = StlDemoConstants.LOCKSCREEN_SCENE,
            toContent = StlDemoConstants.NOTIFICATIONS_OVERLAY,
            direction = Direction.DOWN,
            toContentIsOverlay = true,
            swipeOn = StlDemoConstants.STL_START_HALF_SELECTOR,
        )
    }

    @Test
    fun lockscreenToQuickSettingsOverlay() {
        benchmarkSwipeFromScene(
            fromScene = StlDemoConstants.LOCKSCREEN_SCENE,
            toContent = StlDemoConstants.QUICK_SETTINGS_OVERLAY,
            direction = Direction.DOWN,
            toContentIsOverlay = true,
            swipeOn = StlDemoConstants.STL_END_HALF_SELECTOR,
        )
    }

    private fun benchmarkSwipeFromScene(
        fromScene: String,
        toContent: String,
        direction: Direction,
        toContentIsOverlay: Boolean = false,
        swipeOn: BySelector? = null,
    ) {
        benchmarkRule.measureRepeated(
            packageName = StlDemoConstants.PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.WARM,
            compilationMode = CompilationMode.Full(),
            setupBlock = {
                benchmarkScope().setupSwipeFromScene(fromScene, toContent, toContentIsOverlay)
            },
        ) {
            swipeFromScene(fromScene, toContent, direction, toContentIsOverlay, swipeOn)
        }
    }

    private companion object {
        const val ITERATIONS = 25
    }
}
