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
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.ParameterItem
import com.android.tools.metalava.model.visitors.ItemVisitor
import com.intellij.util.containers.Stack
import java.util.*

/**
 * Visitor which visits all items in two matching codebases and
 * matches up the items and invokes [compare] on each pair, or
 * [added] or [removed] when items are not matched
 */
open class ComparisonVisitor {
    open fun compare(old: Item, new: Item) {}
    open fun added(item: Item) {}
    open fun removed(item: Item) {}

    open fun compare(old: PackageItem, new: PackageItem) {}
    open fun compare(old: ClassItem, new: ClassItem) {}
    open fun compare(old: MethodItem, new: MethodItem) {}
    open fun compare(old: FieldItem, new: FieldItem) {}
    open fun compare(old: ParameterItem, new: ParameterItem) {}

    open fun added(item: PackageItem) {}
    open fun added(item: ClassItem) {}
    open fun added(item: MethodItem) {}
    open fun added(item: FieldItem) {}
    open fun added(item: ParameterItem) {}

    open fun removed(item: PackageItem) {}
    open fun removed(item: ClassItem) {}
    open fun removed(item: MethodItem) {}
    open fun removed(item: FieldItem) {}
    open fun removed(item: ParameterItem) {}
}

class CodebaseComparator {
    /**
     * Visits this codebase and compares it with another codebase, informing the visitors about
     * the correlations and differences that it finds
     */
    fun compare(visitor: ComparisonVisitor, old: Codebase, new: Codebase) {
        // Algorithm: build up two trees (by nesting level); then visit the
        // two trees
        val oldTree = createTree(old)
        val newTree = createTree(new)
        compare(visitor, oldTree, newTree)
    }

    private fun compare(visitor: ComparisonVisitor, oldList: List<ItemTree>, newList: List<ItemTree>) {
        var index1 = 0
        var index2 = 0
        val length1 = oldList.size
        val length2 = newList.size

        while (true) {
            if (index1 < length1) {
                if (index2 < length2) {
                    // Compare the items
                    val oldTree = oldList[index1]
                    val newTree = newList[index2]
                    val old = oldTree.item()
                    val new = newTree.item()
                    val compare = compare(old, new)
                    when {
                        compare > 0 -> {
                            index2++
                            visitAdded(visitor, new)
                        }
                        compare < 0 -> {
                            index1++
                            visitRemoved(visitor, old)
                        }
                        else -> {
                            visitCompare(visitor, old, new)

                            // Compare the children (recurse)
                            compare(visitor, oldTree.children, newTree.children)

                            index1++
                            index2++
                        }
                    }

                } else {
                    // All the remaining items in oldList have been deleted
                    while (index1 < length1) {
                        visitRemoved(visitor, oldList[index1++].item())
                    }
                }
            } else if (index2 < length2) {
                // All the remaining items in newList have been added
                while (index2 < length2) {
                    visitAdded(visitor, newList[index2++].item())
                }
            } else {
                break
            }
        }
    }

    private fun visitAdded(visitor: ComparisonVisitor, item: Item) {
        visitor.added(item)

        when (item) {
            is PackageItem -> visitor.added(item)
            is ClassItem -> visitor.added(item)
            is MethodItem -> visitor.added(item)
            is FieldItem -> visitor.added(item)
            is ParameterItem -> visitor.added(item)
        }
    }

    private fun visitRemoved(visitor: ComparisonVisitor, item: Item) {
        visitor.added(item)

        when (item) {
            is PackageItem -> visitor.removed(item)
            is ClassItem -> visitor.removed(item)
            is MethodItem -> visitor.removed(item)
            is FieldItem -> visitor.removed(item)
            is ParameterItem -> visitor.removed(item)
        }
    }

    private fun visitCompare(visitor: ComparisonVisitor, old: Item, new: Item) {
        visitor.compare(old, new)

        when (old) {
            is PackageItem -> visitor.compare(old, new as PackageItem)
            is ClassItem -> visitor.compare(old, new as ClassItem)
            is MethodItem -> visitor.compare(old, new as MethodItem)
            is FieldItem -> visitor.compare(old, new as FieldItem)
            is ParameterItem -> visitor.compare(old, new as ParameterItem)
        }
    }

    private fun compare(item1: Item, item2: Item): Int = comparator.compare(item1, item2)

    companion object {
        /** Sorting rank for types */
        private fun typeRank(item: Item): Int {
            return when (item) {
                is PackageItem -> 0
                is MethodItem -> if (item.isConstructor()) 1 else 2
                is FieldItem -> 3
                is ClassItem -> 4
                is ParameterItem -> 5
                is AnnotationItem -> 6
                else -> 7
            }
        }

        val comparator: Comparator<Item> = Comparator { item1, item2 ->
            val typeSort = typeRank(item1) - typeRank(item2)
            when {
                typeSort != 0 -> typeSort
                item1 == item2 -> 0
                else -> when (item1) {
                    is PackageItem -> {
                        item1.qualifiedName().compareTo((item2 as PackageItem).qualifiedName())
                    }
                    is ClassItem -> {
                        item1.qualifiedName().compareTo((item2 as ClassItem).qualifiedName())
                    }
                    is MethodItem -> {
                        val delta = item1.name().compareTo((item2 as MethodItem).name())
                        // TODO: Sort by signatures/parameters
                        delta
                    }
                    is FieldItem -> {
                        item1.name().compareTo((item2 as FieldItem).name())
                    }
                    is ParameterItem -> {
                        item1.name().compareTo((item2 as ParameterItem).name())
                    }
                    is AnnotationItem -> {
                        (item1.qualifiedName() ?: "").compareTo((item2 as AnnotationItem).qualifiedName() ?: "")
                    }
                    else -> {
                        error("Unexpected item type ${item1.javaClass}")
                    }
                }
            }
        }

        val treeComparator: Comparator<ItemTree> = Comparator { item1, item2 ->
            comparator.compare(item1.item, item2.item())
        }
    }

    private fun ensureSorted(items: MutableList<ItemTree>) {
        items.sortWith(treeComparator)
        for (item in items) {
            ensureSorted(item)
        }
    }

    private fun ensureSorted(item: ItemTree) {
        item.children.sortWith(treeComparator)
        for (child in item.children) {
            ensureSorted(child)
        }
    }

    private fun createTree(codebase: Codebase): List<ItemTree> {
        // TODO: Make sure the items are sorted!
        val stack = Stack<ItemTree>()
        val root = ItemTree(null)
        stack.push(root)
        codebase.accept(object : ItemVisitor(nestInnerClasses = true, skipEmptyPackages = true) {
            override fun visitItem(item: Item) {
                val node = ItemTree(item)
                val parent = stack.peek()
                parent.children += node

                stack.push(node)
            }

            override fun afterVisitItem(item: Item) {
                stack.pop()
            }
        })

        ensureSorted(root.children)
        return root.children
    }

    data class ItemTree(val item: Item?) : Comparable<ItemTree> {
        val children: MutableList<ItemTree> = mutableListOf()
        fun item(): Item = item!! // Only the root note can be null, and this method should never be called on it

        override fun compareTo(other: ItemTree): Int {
            return comparator.compare(item(), other.item())
        }

        override fun toString(): String {
            return item.toString()
        }

        fun prettyPrint(): String {
            val sb = StringBuilder(1000)
            prettyPrint(sb, 0)
            return sb.toString()
        }

        private fun prettyPrint(sb: StringBuilder, depth: Int) {
            for (i in 0 until depth) {
                sb.append("    ")
            }
            sb.append(toString())
            sb.append('\n')
            for (child in children) {
                child.prettyPrint(sb, depth + 1)
            }
        }

        companion object {
            fun prettyPrint(list: List<ItemTree>): String {
                val sb = StringBuilder(1000)
                for (child in list) {
                    child.prettyPrint(sb, 0)
                }
                return sb.toString()
            }
        }
    }
}