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

package com.android.tools.metalava

import com.android.tools.metalava.NullnessMigration.Companion.findNullnessAnnotation
import com.android.tools.metalava.NullnessMigration.Companion.isNullable
import com.android.tools.metalava.doclava1.Errors
import com.android.tools.metalava.doclava1.Errors.Error
import com.android.tools.metalava.model.AnnotationItem
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.ParameterItem

/**
 * Compares the current API with a previous version and makes sure
 * the changes are compatible. For example, you can make a previously
 * nullable parameter non null, but not vice versa.
 *
 * TODO: Only allow nullness changes on final classes!
 */
class CompatibilityCheck : ComparisonVisitor() {
    override fun compare(old: Item, new: Item) {
        // Should not remove nullness information
        // Can't change information incompatibly

        val oldNullnessAnnotation = findNullnessAnnotation(old)
        if (oldNullnessAnnotation != null) {
            val newNullnessAnnotation = findNullnessAnnotation(new)
            if (newNullnessAnnotation == null) {
                val name = AnnotationItem.simpleName(oldNullnessAnnotation)
                reporter.report(
                    Errors.INVALID_NULL_CONVERSION, new,
                    "Attempted to remove $name annotation from ${describe(new)}"
                )
            } else {
                val oldNullable = isNullable(old)
                val newNullable = isNullable(new)
                if (oldNullable != newNullable) {
                    // You can change a parameter from nonnull to nullable
                    // You can change a method from nullable to nonnull
                    // You cannot change a parameter from nullable to nonnull
                    // You cannot change a method from nonnull to nullable
                    if (oldNullable && old is ParameterItem) {
                        reporter.report(
                            Errors.INVALID_NULL_CONVERSION, new,
                            "Attempted to change parameter from @Nullable to @NonNull: " +
                                    "incompatible change for ${describe(new)}"
                        )
                    } else if (!oldNullable && old is MethodItem) {
                        reporter.report(
                            Errors.INVALID_NULL_CONVERSION, new,
                            "Attempted to change method return from @NonNull to @Nullable: " +
                                    "incompatible change for ${describe(new)}"
                        )
                    }
                }
            }
        }
    }

    override fun compare(old: ClassItem, new: ClassItem) {
        if (old.isInterface() != new.isInterface()) {
            reporter.report(
                Errors.CHANGED_CLASS, new, "Class " + new.qualifiedName()
                        + " changed class/interface declaration"
            )
        }
    }

    private fun handleAdded(error: Error, item: Item) {
        // Adding a package isn't technically a compatibility error, but doclava1 flags it as a warning
        // so we will too
        reporter.report(error, item, "Added ${describe(item)}")
    }

    private fun handleRemoved(error: Error, item: Item) {
        reporter.report(error, item, "Removed ${if (item.deprecated) "deprecated " else ""}${describe(item)}")
    }

    override fun added(item: PackageItem) {
        handleAdded(Errors.ADDED_PACKAGE, item)
    }

    override fun removed(item: PackageItem) {
        handleRemoved(Errors.REMOVED_PACKAGE, item)
    }

    override fun removed(item: ClassItem) {
        val error = when {
            item.isInterface() -> Errors.REMOVED_INTERFACE
            item.deprecated -> Errors.REMOVED_DEPRECATED_CLASS
            else -> Errors.REMOVED_CLASS
        }
        handleRemoved(error, item)
    }

    override fun removed(item: MethodItem) {
        handleRemoved(Errors.REMOVED_METHOD, item)
    }

    override fun removed(item: FieldItem) {
        handleRemoved(Errors.REMOVED_FIELD, item)
    }

    private fun describe(item: Item): String {
        return when (item) {
            is PackageItem -> "package ${item.qualifiedName()}"
            is ClassItem -> "class ${item.qualifiedName()}"
            is FieldItem -> "field ${item.containingClass().qualifiedName()}.${item.name()}"
            is MethodItem -> "method ${item.containingClass().qualifiedName()}.${item.name()}"
            is ParameterItem -> "parameter ${item.name()} in " +
                    "${item.containingMethod().containingClass().qualifiedName()}.${item.containingMethod().name()}"
            else -> item.toString()
        }
    }
}