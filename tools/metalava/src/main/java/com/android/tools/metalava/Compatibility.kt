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

const val COMPAT_MODE_BY_DEFAULT = true

/**
 * The old API generator code had a number of quirks. Initially we want to simulate these
 * quirks to produce compatible signature files and APIs, but we want to track what these quirks
 * are and be able to turn them off eventually. This class offers more fine grained control
 * of these compatibility behaviors such that we can enable/disable them selectively
 */
var compatibility: Compatibility = Compatibility()

class Compatibility(
    /** Whether compatibility is generally on */
    val compat: Boolean = COMPAT_MODE_BY_DEFAULT
) {

    /** Whether to inline fields from implemented interfaces into concrete classes */
    var inlineInterfaceFields: Boolean = compat

    /** In signature files, use "implements" instead of "extends" for the super class of
     * an interface */
    var extendsForInterfaceSuperClass: Boolean = compat

    /** In signature files, refer to annotations as an "abstract class" instead of an "@interface"
     * and implementing this interface: java.lang.annotation.Annotation */
    var classForAnnotations: Boolean = compat

    /** Add in explicit `valueOf` and `values` methods into annotation classes  */
    var defaultAnnotationMethods: Boolean = compat

    /** In signature files, refer to enums as "class" instead of "enum" */
    var classForEnums: Boolean = compat

    /** Whether to use a nonstandard, compatibility modifier order instead of the Java canonical order.
     * ("deprecated" isn't a real modifier, so in "standard" mode it's listed first, as if it was the
     * `@Deprecated` annotation before the modifier list */
    var nonstandardModifierOrder: Boolean = compat

    /** In signature files, skip the native modifier from the modifier lists */
    var skipNativeModifier: Boolean = nonstandardModifierOrder

    /** In signature files, skip the strictfp modifier from the modifier lists */
    var skipStrictFpModifier: Boolean = nonstandardModifierOrder

    /** Whether to include instance methods in annotation classes for the annotation properties */
    var skipAnnotationInstanceMethods: Boolean = compat

    /** Include spaces after commas in type strings */
    var spacesAfterCommas: Boolean = compat

    /**
     * In signature files, whether interfaces should also be described as "abstract"
     */
    var abstractInInterfaces: Boolean = compat

    /**
     * In signature files, whether annotation types should also be described as "abstract"
     */
    var abstractInAnnotations: Boolean = compat

    /**
     * In signature files, whether interfaces can be listed as final
     */
    var finalInInterfaces: Boolean = compat

    /**
     * In this signature
     *        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
     *  doclava1 would treat this as "throws Throwable" instead of "throws X". This variable turns on
     *  this compat behavior.
     * */
    var useErasureInThrows: Boolean = compat

    /**
     * Include a single space in front of package private classes with no other modifiers
     * (this doesn't align well, but is supported to make the output 100% identical to the
     * doclava1 format
     */
    var extraSpaceForEmptyModifiers: Boolean = compat

    /** Format `Map<K,V>` as `Map<K, V>` */
    var spaceAfterCommaInTypes: Boolean = compat

    /**
     * Doclava1 sorts classes/interfaces by class name instead of qualified name
     */
    var sortClassesBySimpleName: Boolean = compat

    /**
     * Doclava1 omits type parameters in interfaces (in signature files, not in stubs)
     */
    var omitTypeParametersInInterfaces: Boolean = compat

    /**
     * Doclava1 sorted the methods like this:
     *
     *      public final class RoundingMode extends java.lang.Enum {
     *          method public static java.math.RoundingMode valueOf(java.lang.String);
     *          method public static java.math.RoundingMode valueOf(int);
     *          ...
     *
     * Note how the two valueOf methods are out of order. With this compatibility mode,
     * we try to perform the same sorting.
     */
    var sortEnumValueOfMethodFirst: Boolean = compat

    /**
     * Whether packages should be treated as recursive for documentation. In other words,
     * if a directory has a `packages.html` file containing a `@hide` comment, then
     * all "sub" packages (directories below this one) will also inherit the same comment.
     * Java packages aren't supposed to work that way, but doclava does.
     */
    var inheritPackageDocs: Boolean = compat

    /** Force methods named "values" in enums to be marked final. This was done by
     * doclava1 with this comment:
     *
     *     Explicitly coerce 'final' state of Java6-compiled enum values() method,
     *     to match the Java5-emitted base API description.
     *
     **/
    var forceFinalInEnumValueMethods: Boolean = compat

    /** Whether signature files and stubs should contain annotations */
    var annotationsInSignatures: Boolean = !compat

    /** Emit errors in the old API diff format */
    var oldErrorOutputFormat: Boolean = false

    /**
     * When a public class implementing a public interface inherits the implementation
     * of a method in that interface from a hidden super class, the method must be
     * included in the stubs etc (since otherwise subclasses would believe they need
     * to implement that method and can't just inherit it). However, doclava1 does not
     * list these methods. This flag controls this compatibility behavior.
     */
    var skipInheritedInterfaceMethods: Boolean = compat

    /**
     * Whether to include parameter names in the signature file
     */
    val parameterNames: Boolean = true

    // Other examples: sometimes we sort by qualified name, sometimes by full name
}