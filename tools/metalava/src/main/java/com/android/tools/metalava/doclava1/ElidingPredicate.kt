package com.android.tools.metalava.doclava1

import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem

import java.util.function.Predicate

// Ported from doclava1

/**
 * Filter that will elide exact duplicate methods that are already included
 * in another superclass/interfaces.
 */
class ElidingPredicate(private val wrapped: Predicate<Item>) : Predicate<Item> {

    override fun test(member: Item): Boolean {
        // This member should be included, but if it's an exact duplicate
        // override then we can elide it.
        return if (member is MethodItem) {
            val different = member.findPredicateSuperMethod(Predicate { test ->
                // We're looking for included and perfect signature
                wrapped.test(test) &&
                        test is MethodItem &&
                        MethodItem.sameSignature(member, test, false)
            })
            different == null
        } else {
            true
        }
    }
}
