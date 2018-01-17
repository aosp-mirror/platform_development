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

import com.android.tools.metalava.model.AnnotationItem
import com.android.tools.metalava.model.Item

/**
 * Performs null migration analysis, looking at previous API signature
 * files and new signature files, and replacing new @Nullable and @NonNull
 * annotations with @NewlyNullable and @NewlyNonNull, and similarly
 * moving @NewlyNullable and @NewlyNonNull to @RecentlyNullable and @RecentlyNonNull
 * (and finally once the annotations have been there for another API level,
 * finally moving them to unconditionally nullable/nonnull.)
 *
 * (Newly null is the initial level; user code is marked as warnings if in
 * conflict with the annotation. Recently null is the next level; once an
 * API has had newly-null metadata in one API level, it gets promoted to
 * recently, which generates errors instead of warnings. The reason we have
 * this instead of just making it unconditional is that you can still invoke
 * the compiler with a flag to defeat it, so the Kotlin team suggested we do
 * this.
 */
class NullnessMigration : ComparisonVisitor() {
    override fun compare(old: Item, new: Item) {
        if (hasNullnessInformation(new)) {
            if (!hasNullnessInformation(old)) {
                // Nullness information change: Add migration annotation
                val annotation = if (isNullable(new)) NEWLY_NULLABLE else NEWLY_NONNULL

                val migration = findNullnessAnnotation(new) ?: return
                val modifiers = new.mutableModifiers()
                modifiers.removeAnnotation(migration)

                // Don't map annotation names - this would turn newly non null back into non null
                modifiers.addAnnotation(new.codebase.createAnnotation("@" + annotation, new, mapName = false))
            } else if (hasMigrationAnnotation(old)) {
                // Already marked migration before: Now we can promote it to
                // no longer migrated!
                val nullAnnotation = findNullnessAnnotation(new) ?: return
                val migration = findMigrationAnnotation(old)?.toSource() ?: return
                val modifiers = new.mutableModifiers()
                modifiers.removeAnnotation(nullAnnotation)

                if (isNewlyMigrated(old)) {
                    // Move from newly to recently
                    val source = migration.replace("Newly", "Recently")
                    modifiers.addAnnotation(new.codebase.createAnnotation(source, new, mapName = false))
                } else {
                    // Move from recently to no longer marked as migrated
                    val source = migration.replace("Newly", "").replace("Recently", "")
                    modifiers.addAnnotation(new.codebase.createAnnotation(source, new, mapName = false))
                }
            }
        }
    }

    companion object {
        fun hasNullnessInformation(item: Item): Boolean {
            return isNullable(item) || isNonNull(item)
        }

        fun findNullnessAnnotation(item: Item): AnnotationItem? {
            return item.modifiers.annotations().firstOrNull { it.isNullnessAnnotation() }
        }

        fun findMigrationAnnotation(item: Item): AnnotationItem? {
            return item.modifiers.annotations().firstOrNull {
                val qualifiedName = it.qualifiedName() ?: ""
                isMigrationAnnotation(qualifiedName)
            }
        }

        fun isNullable(item: Item): Boolean {
            return item.modifiers.annotations().any { it.isNullable() }
        }

        fun isNonNull(item: Item): Boolean {
            return item.modifiers.annotations().any { it.isNonNull() }
        }

        fun hasMigrationAnnotation(item: Item): Boolean {
            return item.modifiers.annotations().any { isMigrationAnnotation(it.qualifiedName() ?: "") }
        }

        fun isNewlyMigrated(item: Item): Boolean {
            return item.modifiers.annotations().any { isNewlyMigrated(it.qualifiedName() ?: "") }
        }

        fun isRecentlyMigrated(item: Item): Boolean {
            return item.modifiers.annotations().any { isRecentlyMigrated(it.qualifiedName() ?: "") }
        }

        fun isNewlyMigrated(qualifiedName: String): Boolean {
            return qualifiedName.endsWith(".NewlyNullable") ||
                    qualifiedName.endsWith(".NewlyNonNull")
        }

        fun isRecentlyMigrated(qualifiedName: String): Boolean {
            return qualifiedName.endsWith(".RecentlyNullable") ||
                    qualifiedName.endsWith(".RecentlyNonNull")
        }

        fun isMigrationAnnotation(qualifiedName: String): Boolean {
            return isNewlyMigrated(qualifiedName) || isRecentlyMigrated(qualifiedName)
        }
    }
}

/**
 * @TypeQualifierNickname
 * @NonNull
 * @kotlin.annotations.jvm.UnderMigration(status = kotlin.annotations.jvm.MigrationStatus.WARN)
 * @Retention(RetentionPolicy.CLASS)
 * public @interface NewlyNullable {
 * }
 */
const val NEWLY_NULLABLE = "android.support.annotation.NewlyNullable"

/**
 * @TypeQualifierNickname
 * @NonNull
 * @kotlin.annotations.jvm.UnderMigration(status = kotlin.annotations.jvm.MigrationStatus.WARN)
 * @Retention(RetentionPolicy.CLASS)
 * public @interface NewlyNonNull {
 * }
 */
const val NEWLY_NONNULL = "android.support.annotation.NewlyNonNull"

/**
 * @TypeQualifierNickname
 * @NonNull
 * @kotlin.annotations.jvm.UnderMigration(status = kotlin.annotations.jvm.MigrationStatus.STRICT)
 * @Retention(RetentionPolicy.CLASS)
 * public @interface NewlyNullable {
 * }
 */

const val RECENTLY_NULLABLE = "android.support.annotation.RecentlyNullable"
/**
 * @TypeQualifierNickname
 * @NonNull
 * @kotlin.annotations.jvm.UnderMigration(status = kotlin.annotations.jvm.MigrationStatus.STRICT)
 * @Retention(RetentionPolicy.CLASS)
 * public @interface NewlyNonNull {
 * }
 */
const val RECENTLY_NONNULL = "android.support.annotation.RecentlyNonNull"

