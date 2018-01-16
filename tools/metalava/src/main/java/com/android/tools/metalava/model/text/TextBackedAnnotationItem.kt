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

import com.android.tools.metalava.model.AnnotationAttribute
import com.android.tools.metalava.model.AnnotationItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.DefaultAnnotationAttribute

class TextBackedAnnotationItem(
    override val codebase: Codebase,
    source: String,
    mapName: Boolean = true
) : AnnotationItem {
    private val qualifiedName: String?
    private val full: String
    private val attributes: List<AnnotationAttribute>

    init {
        val index = source.indexOf("(")
        val annotationClass = if (index == -1)
            source.substring(1) // Strip @
        else
            source.substring(1, index)

        qualifiedName = if (mapName) AnnotationItem.mapName(codebase, annotationClass) else annotationClass
        full = when {
            qualifiedName == null -> ""
            index == -1 -> "@" + qualifiedName
            else -> "@" + qualifiedName + source.substring(index)
        }

        attributes = if (index == -1) {
            emptyList()
        } else {
            DefaultAnnotationAttribute.createList(
                source.substring(index + 1, source.lastIndexOf(')'))
            )
        }
    }

    override fun qualifiedName(): String? = qualifiedName
    override fun attributes(): List<AnnotationAttribute> = attributes
    override fun toSource(): String = full
}
