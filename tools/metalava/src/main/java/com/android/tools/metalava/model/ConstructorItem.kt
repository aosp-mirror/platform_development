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

package com.android.tools.metalava.model

interface ConstructorItem : MethodItem {
    override fun isConstructor(): Boolean = true

    /**
     * The constructor that this method delegates to initially (e.g. super- or this- or default/implicit null
     * constructor). Note that it may not be in a super class, as in the case of a this-call.
     */
    var superConstructor: ConstructorItem?
}