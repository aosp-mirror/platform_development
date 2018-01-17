/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tools.metalava.model.visitors

import com.android.tools.metalava.model.Item

// TODO: Use ApiVisitor instead!
open class VisibleItemVisitor(
    /**
     * Whether constructors should be visited as part of a [#visitMethod] call
     * instead of just a [#visitConstructor] call. Helps simplify visitors that
     * don't care to distinguish between the two cases. Defaults to true.
     */
    visitConstructorsAsMethods: Boolean = true,
    /**
     * Whether inner classes should be visited "inside" a class; when this property
     * is true, inner classes are visited before the [#afterVisitClass] method is
     * called; when false, it's done afterwards. Defaults to false.
     */
    nestInnerClasses: Boolean = false
) : ItemVisitor(visitConstructorsAsMethods, nestInnerClasses) {
    override fun skip(item: Item): Boolean {
        return !item.included
    }
}