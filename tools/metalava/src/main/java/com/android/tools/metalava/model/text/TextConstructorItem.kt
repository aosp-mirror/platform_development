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

package com.android.tools.metalava.model.text

import com.android.tools.metalava.doclava1.SourcePositionInfo
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.ConstructorItem

class TextConstructorItem(
    codebase: Codebase,
    name: String,
    containingClass: TextClassItem,
    isPublic: Boolean,
    isProtected: Boolean,
    isPrivate: Boolean,
    isFinal: Boolean,
    isStatic: Boolean,
    isAbstract: Boolean,
    isSynchronized: Boolean,
    isNative: Boolean,
    isDefault: Boolean,
    returnType: TextTypeItem?,
    position: SourcePositionInfo,
    annotations: List<String>?
) : TextMethodItem(
    codebase, name, containingClass, isPublic, isProtected, isPrivate,
    isFinal, isStatic, isAbstract, isSynchronized, isNative, isDefault, returnType, position, annotations
),
    ConstructorItem {
    override var superConstructor: ConstructorItem? = null

    override fun isConstructor(): Boolean = true
}


