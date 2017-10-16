package com.android.tools.metalava.doclava1

import com.android.tools.metalava.Options
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MemberItem
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.options
import java.util.function.Predicate

// Ported from doclava1

/**
 * Predicate that decides if the given member should be considered part of an
 * API surface area. To make the most accurate decision, it searches for
 * signals on the member, all containing classes, and all containing packages.
 */
class ApiPredicate(
    val codebase: Codebase,
    /**
     * Set if the value of [MemberItem.hasShowAnnotation] should be
     * ignored. That is, this predicate will assume that all encountered members
     * match the "shown" requirement.
     *
     * This is typically useful when generating "current.txt", when no
     * [Options.showAnnotations] have been defined.
     */
    private val ignoreShown: Boolean = false,
    /**
     * Set if the value of [MemberItem.removed] should be ignored.
     * That is, this predicate will assume that all encountered members match
     * the "removed" requirement.
     *
     * This is typically useful when generating "removed.txt", when it's okay to
     * reference both current and removed APIs.
     */
    private val ignoreRemoved: Boolean = false,
    /**
     * Set what the value of [MemberItem.removed] must be equal to in
     * order for a member to match.
     *
     * This is typically useful when generating "removed.txt", when you only
     * want to match members that have actually been removed.
     */
    private val matchRemoved: Boolean = false,
    /** Whether we allow matching items loaded from jar files instead of sources */
    private val allowFromJar: Boolean = true
) : Predicate<Item> {

    override fun test(member: Item): Boolean {
        if (!allowFromJar && member.isFromClassPath()) {
            return false
        }

        var visible = member.isPublic || member.isProtected
        var hidden = member.hidden
        if (!visible || hidden) {
            return false
        }

        var hasShowAnnotation = ignoreShown || member.hasShowAnnotation()
        var docOnly = member.docOnly
        var removed = member.removed

        var clazz: ClassItem? = when (member) {
            is MemberItem -> member.containingClass()
            is ClassItem -> member
            else -> null
        }

        if (clazz != null) {
            var pkg: PackageItem? = clazz.containingPackage()
            while (pkg != null) {
                hidden = hidden or pkg.hidden
                docOnly = docOnly or pkg.docOnly
                removed = removed or pkg.removed
                pkg = pkg.containingPackage()
            }
        }
        while (clazz != null) {
            visible = visible and (clazz.isPublic || clazz.isProtected)
            hasShowAnnotation = hasShowAnnotation or (ignoreShown || clazz.hasShowAnnotation())
            hidden = hidden or clazz.hidden
            docOnly = docOnly or clazz.docOnly
            removed = removed or clazz.removed
            clazz = clazz.containingClass()
        }

        if (ignoreRemoved) {
            removed = matchRemoved
        }

        if (docOnly && options.includeDocOnly) {
            docOnly = false
        }

        return visible && hasShowAnnotation && !hidden && !docOnly && removed == matchRemoved
    }
}
