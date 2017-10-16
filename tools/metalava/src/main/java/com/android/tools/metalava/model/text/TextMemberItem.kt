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
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.MemberItem
import com.android.tools.metalava.model.ModifierList

abstract class TextMemberItem(
    codebase: Codebase,
    private val name: String,
    private val containingClass: TextClassItem,
    position: SourcePositionInfo,
    override var modifiers: ModifierList
) : TextItem(codebase, position = position, modifiers = modifiers), MemberItem {

    override fun name(): String = name
    override fun containingClass(): ClassItem = containingClass
}