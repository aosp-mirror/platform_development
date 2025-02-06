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

package com.android.mechanics.demo.util

import androidx.compose.ui.unit.Density
import com.android.mechanics.spec.Guarantee

enum class GuaranteeType(val label: String) {
    None("None"),
    Input("Input delta"),
    Drag("Drag delta"),
}

val Guarantee.type: GuaranteeType
    get() =
        when (this) {
            is Guarantee.None -> GuaranteeType.None
            is Guarantee.InputDelta -> GuaranteeType.Input
            is Guarantee.GestureDragDelta -> GuaranteeType.Drag
        }

fun Guarantee.asLabel(): String {
    return when (this) {
        is Guarantee.None -> "None"
        is Guarantee.InputDelta -> "${GuaranteeType.Input.label}($delta)"
        is Guarantee.GestureDragDelta -> "${GuaranteeType.Drag.label}($delta)"
    }
}

fun Guarantee.asLabel(density: Density): String {
    return with(density) {
        when (this@asLabel) {
            is Guarantee.None -> "None"
            is Guarantee.InputDelta -> "${GuaranteeType.Input.label}(${delta.toDp()})"
            is Guarantee.GestureDragDelta -> "${GuaranteeType.Drag.label}(${delta.toDp()})"
        }
    }
}
