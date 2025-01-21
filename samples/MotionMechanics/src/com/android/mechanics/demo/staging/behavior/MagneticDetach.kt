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

package com.android.mechanics.demo.staging.behavior

import android.util.Log
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.UserActionDistance
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.transformation.CustomPropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformationScope
import com.android.mechanics.MotionValue
import com.android.mechanics.ProvidedGestureContext
import com.android.mechanics.spec.InputDirection
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder
import com.android.mechanics.spring.SpringParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Animate the reveal of [container] by animating its size. */
fun TransitionBuilder.magneticDetach(container: ElementKey) {
    // Make the swipe distance be exactly the target height of the container.
    // TODO(b/376438969): Make sure that this works correctly when the target size of the element
    // is changing during the transition (e.g. a notification was added). At the moment, the user
    // action distance is only called until it returns a value > 0f, which is then cached.
    distance = UserActionDistance { fromContent, toContent, _ ->
        val targetSizeInFromContent = container.targetSize(fromContent)
        val targetSizeInToContent = container.targetSize(toContent)
        if (targetSizeInFromContent != null && targetSizeInToContent != null) {
            error(
                "verticalContainerReveal should not be used with shared elements, but " +
                    "${container.debugName} is in both ${fromContent.debugName} and " +
                    toContent.debugName
            )
        }

        (targetSizeInToContent?.height ?: targetSizeInFromContent?.height)?.toFloat() ?: 0f
    }

    Log.d("MIKES", "magneticDetach() called with: container = $container")

    transformation(container) {
        Log.d("MIKES", "magneticDetach() Created")
        MagneticDetachTransformation()
    }
}

@OptIn(ExperimentalAnimatableApi::class)
private class MagneticDetachTransformation() : CustomPropertyTransformation<IntSize> {
    override val property = PropertyTransformation.Property.Size

    val input = mutableFloatStateOf(0f)

    var heightValue: MotionValue? = null
    var heightValueJob: Job? = null

    override fun PropertyTransformationScope.transform(
        content: ContentKey,
        element: ElementKey,
        transition: TransitionState.Transition,
        transitionScope: CoroutineScope,
    ): IntSize {
        Log.d(
            "MIKES",
            "transform() called with: content = $content, element = $element, transition = $transition, transitionScope = $transitionScope",
        )
        val idleSize = checkNotNull(element.targetSize(content))

        if (
            heightValue?.isStable == true &&
                transition.progress == 1f &&
                !transition.isUserInputOngoing
        ) {
            Log.d("MIKES", "transform() KILL")
            heightValue = null
            heightValueJob?.cancel()
            heightValueJob = null
            return idleSize
        }

        val fromSize = checkNotNull(element.targetSize(transition.fromContent)).toSize()
        val toSize = checkNotNull(element.targetSize(transition.toContent)).toSize()
        val collapsedSize = if (fromSize.height < toSize.height) fromSize else toSize
        val expandedHeight = if (fromSize.height >= toSize.height) fromSize else toSize

        input.floatValue = (expandedHeight.height - collapsedSize.height) * transition.progress
        Log.d("MIKES", "transform()UPD")

        if (heightValue == null) {
            Log.d("MIKES", "transform()START")
            val springParameters = SpringParameters(stiffness = 380f, dampingRatio = 0.9f)

            val detachDistance = 48.dp.toPx()

            val spec =
                MotionSpec.builder(
                        springParameters,
                        initialMapping = Mapping.Fixed(collapsedSize.height),
                    )
                    .toBreakpoint(0f)
                    .jumpBy(0f)
                    .continueWithFractionalInput(.2f)
                    .toBreakpoint(detachDistance)
                    .jumpTo(collapsedSize.height + detachDistance)
                    .continueWithFractionalInput(1f)
                    .complete()

            heightValue =
                MotionValue(
                    input::floatValue,
                    transition.gestureContext ?: ProvidedGestureContext(0f, InputDirection.Max),
                    spec,
                )
            heightValueJob = transitionScope.launch { heightValue?.keepRunning() }
        }

        return IntSize(idleSize.width, height = heightValue!!.output.toInt())
    }
}
