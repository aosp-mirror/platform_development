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
import android.util.DisplayMetrics
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.android.compose.animation.scene.demo.calculateWindowSizeClass
import com.android.compose.animation.scene.demo.shouldUseSplitScenes
import kotlin.math.roundToInt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * This file contains utilities to perform benchmark tests for the demo app of SceneTransitionLayout
 * given a [SceneTransitionLayoutBenchmarkScope].
 *
 * These abstractions are necessary to share test code between AndroidX tests written with the
 * MacrobenchmarkRule and Platform tests written with Platform helpers.
 */
interface SceneTransitionLayoutBenchmarkScope {
    /** Start an activity using [intent]. */
    fun startActivity(intent: Intent)
}

fun SceneTransitionLayoutBenchmarkScope.startDemoActivity(initialScene: String) {
    val intent =
        (context().packageManager.getLaunchIntentForPackage(StlDemoConstants.PACKAGE)
                ?: error("Unable to acquire intent for package ${StlDemoConstants.PACKAGE}"))
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(StlDemoConstants.INITIAL_SCENE_EXTRA, initialScene)
                putExtra(StlDemoConstants.FULLSCREEN_EXTRA, true)
                putExtra(StlDemoConstants.DISABLE_RIPPLE_EXTRA, true)
            }

    val device = device()
    device.pressHome()
    startActivity(intent)
    device.waitForObject(sceneSelector(initialScene))
}

fun SceneTransitionLayoutBenchmarkScope.setupSwipeFromScene(fromScene: String, toScene: String) {
    startDemoActivity(initialScene = fromScene)

    // Wait for the root SceneTransitionLayout to be there. Note that startDemoActivity already
    // waited for fromScene, so we know it's there.
    val device = device()
    device.waitForObject(StlDemoConstants.ROOT_STL_SELECTOR)

    // Verify that toScene is not there yet.
    device.waitUntilGone(sceneSelector(toScene))
}

fun swipeFromScene(fromScene: String, toScene: String, direction: Direction) {
    // Swipe in the given direction.
    val densityDpi = context().resources.configuration.densityDpi
    val density = densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
    val swipeSpeed = 1_500 // in dp/s
    val device = device()
    device
        .findObject(StlDemoConstants.ROOT_STL_SELECTOR)
        .swipe(direction, /* percent= */ 0.9f, /* speed= */ (swipeSpeed * density).roundToInt())

    // Wait for fromScene to disappear.
    device.waitUntilGone(sceneSelector(fromScene))

    // Check that we are at toScene.
    device.waitForObject(sceneSelector(toScene))
}

/**
 * Navigate back to [previousScene] assuming that we are currently on [currentScene] and that going
 * back will land us at [previousScene].
 */
fun navigateBackToPreviousScene(previousScene: String, currentScene: String) {
    val device = device()
    device.waitUntilGone(sceneSelector(previousScene))
    device.waitForObject(sceneSelector(currentScene))

    device.pressBack()
    device.waitUntilGone(sceneSelector(currentScene))
    device.waitForObject(sceneSelector(previousScene))
}

private fun instrumentation() = InstrumentationRegistry.getInstrumentation()

private fun context() = instrumentation().targetContext

private fun device() = UiDevice.getInstance(instrumentation())

private fun UiDevice.waitForObject(selector: BySelector, timeout: Long = 5_000) {
    if (!wait(Until.hasObject(selector), timeout)) {
        error("Did not find $selector within $timeout ms")
    }
}

private fun UiDevice.waitUntilGone(selector: BySelector, timeout: Long = 5_000) {
    if (!wait(Until.gone(selector), timeout)) {
        error("$selector is still there after waiting $timeout ms")
    }
}

private fun sceneSelector(scene: String) = By.res("scene:$scene")

object StlDemoConstants {
    const val PACKAGE = "com.android.compose.animation.scene.demo.app"
    val LOCKSCREEN_SCENE by AdaptiveScene("Lockscreen", "SplitLockscreen")
    val SHADE_SCENE by AdaptiveScene("Shade", "SplitShade")
    const val QUICK_SETTINGS_SCENE = "QuickSettings"

    internal const val INITIAL_SCENE_EXTRA = "initial_scene"
    internal const val FULLSCREEN_EXTRA = "fullscreen"
    internal const val DISABLE_RIPPLE_EXTRA = "disable_ripple"
    internal val ROOT_STL_SELECTOR = By.res("SystemUiSceneTransitionLayout")
}

/** A scene whose key depends on whether we are using split scenes or not. */
private class AdaptiveScene(private val normalScene: String, private val splitScene: String) :
    ReadOnlyProperty<Any, String> {
    override fun getValue(thisRef: Any, property: KProperty<*>): String {
        val context = context()
        val density = Density(context)
        return if (shouldUseSplitScenes(calculateWindowSizeClass(context, density))) {
            splitScene
        } else {
            normalScene
        }
    }
}
