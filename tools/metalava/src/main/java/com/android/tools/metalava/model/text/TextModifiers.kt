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
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.ModifierList
import com.android.tools.metalava.model.MutableModifierList
import java.io.StringWriter

class TextModifiers(
    override val codebase: Codebase,
    annotationStrings: List<String>? = null,
    private var public: Boolean = false,
    private var protected: Boolean = false,
    private var private: Boolean = false,
    private var static: Boolean = false,
    private var abstract: Boolean = false,
    private var final: Boolean = false,
    private var native: Boolean = false,
    private var synchronized: Boolean = false,
    private var strictfp: Boolean = false,
    private var transient: Boolean = false,
    private var volatile: Boolean = false,
    private var default: Boolean = false
) : MutableModifierList {
    private var annotations: MutableList<AnnotationItem> = mutableListOf()

    init {
        annotationStrings?.forEach { source ->
            val index = source.indexOf('(')
            val qualifiedName = AnnotationItem.mapName(
                codebase,
                if (index == -1) source.substring(1) else source.substring(1, index)
            )

            val attributes =
                if (index == -1) {
                    emptyList()
                } else {
                    DefaultAnnotationAttribute.createList(source.substring(index + 1, source.lastIndexOf(')')))
                }
            val codebase = codebase
            val item = object : AnnotationItem {
                override val codebase = codebase
                override fun attributes(): List<AnnotationAttribute> = attributes
                override fun qualifiedName(): String? = qualifiedName
                override fun toSource(): String = source
            }
            annotations.add(item)
        }
    }

    override fun isPublic(): Boolean = public
    override fun isProtected(): Boolean = protected
    override fun isPrivate(): Boolean = private
    override fun isStatic(): Boolean = static
    override fun isAbstract(): Boolean = abstract
    override fun isFinal(): Boolean = final
    override fun isNative(): Boolean = native
    override fun isSynchronized(): Boolean = synchronized
    override fun isStrictFp(): Boolean = strictfp
    override fun isTransient(): Boolean = transient
    override fun isVolatile(): Boolean = volatile
    override fun isDefault(): Boolean = default

    override fun setPublic(public: Boolean) {
        this.public = public
    }

    override fun setProtected(protected: Boolean) {
        this.protected = protected
    }

    override fun setPrivate(private: Boolean) {
        this.private = private
    }

    override fun setStatic(static: Boolean) {
        this.static = static
    }

    override fun setAbstract(abstract: Boolean) {
        this.abstract = abstract
    }

    override fun setFinal(final: Boolean) {
        this.final = final
    }

    override fun setNative(native: Boolean) {
        this.native = native
    }

    override fun setSynchronized(synchronized: Boolean) {
        this.synchronized = synchronized
    }

    override fun setStrictFp(strictfp: Boolean) {
        this.strictfp = strictfp
    }

    override fun setTransient(transient: Boolean) {
        this.transient = transient
    }

    override fun setVolatile(volatile: Boolean) {
        this.volatile = volatile
    }

    override fun setDefault(default: Boolean) {
        this.default = default
    }

    var owner: Item? = null

    override fun owner(): Item = owner!! // Must be set after construction
    override fun isEmpty(): Boolean {
        return !(public || protected || private || static || abstract || final || native || synchronized
                || strictfp || transient || volatile || default)
    }

    override fun annotations(): List<AnnotationItem> {
        return annotations
    }

    override fun addAnnotation(annotation: AnnotationItem) {
        val qualifiedName = annotation.qualifiedName()
        if (annotations.any { it.qualifiedName() == qualifiedName }) {
            return
        }
        // TODO: Worry about repeatable annotations?
        annotations.add(annotation)
    }

    override fun removeAnnotation(annotation: AnnotationItem) {
        annotations.remove(annotation)
    }

    override fun clearAnnotations(annotation: AnnotationItem) {
        annotations.clear()
    }

    override fun toString(): String {
        val item = owner ?: return super.toString()
        val writer = StringWriter()
        ModifierList.write(writer, this, item)
        return writer.toString()
    }
}