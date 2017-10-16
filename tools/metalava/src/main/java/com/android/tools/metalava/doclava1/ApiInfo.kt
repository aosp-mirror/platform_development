/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.metalava.doclava1

import com.android.annotations.NonNull
import com.android.tools.metalava.CodebaseComparator
import com.android.tools.metalava.ComparisonVisitor
import com.android.tools.metalava.model.AnnotationItem
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.DefaultCodebase
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.PackageList
import com.android.tools.metalava.model.text.TextBackedAnnotationItem
import com.android.tools.metalava.model.text.TextClassItem
import com.android.tools.metalava.model.text.TextMethodItem
import com.android.tools.metalava.model.text.TextPackageItem
import com.android.tools.metalava.model.text.TextTypeItem
import com.android.tools.metalava.model.visitors.ItemVisitor
import com.android.tools.metalava.model.visitors.TypeVisitor
import java.util.*
import java.util.function.Predicate

// Copy of ApiInfo in doclava1 (converted to Kotlin + some cleanup to make it work with metalava's data structures.
// (Converted to Kotlin such that I can inherit behavior via interfaces, in particular Codebase.)
class ApiInfo : DefaultCodebase() {
    /**
     * Whether types should be interpreted to be in Kotlin format (e.g. ? suffix means nullable,
     * ! suffix means unknown, and absence of a suffix means not nullable.
     */
    var kotlinStyleNulls = false

    private val mPackages = HashMap<String, TextPackageItem>(300)
    private val mAllClasses = HashMap<String, TextClassItem>(30000)
    private val mClassToSuper = HashMap<TextClassItem, String>(30000)
    private val mClassToInterface = HashMap<TextClassItem, ArrayList<String>>(10000)

    override var description = "Codebase"

    override fun trustedApi(): Boolean = true

    override fun getPackages(): PackageList {
        val list = ArrayList<PackageItem>(mPackages.values)
        list.sortWith(PackageItem.comparator)
        return PackageList(list)
    }

    override fun size(): Int {
        return mPackages.size
    }

    override fun findClass(@NonNull className: String): TextClassItem? {
        return mAllClasses[className]
    }

    private fun resolveInterfaces() {
        for (cl in mAllClasses.values) {
            val ifaces = mClassToInterface[cl] ?: continue
            for (iface in ifaces) {
                var ci: TextClassItem? = mAllClasses[iface]
                if (ci == null) {
                    // Interface not provided by this codebase. Inject a stub.
                    ci = TextClassItem.createInterfaceStub(this, iface)
                }
                cl.addInterface(ci)
            }
        }
    }

    override fun supportsDocumentation(): Boolean = false

    fun mapClassToSuper(classInfo: TextClassItem, superclass: String?) {
        superclass?.let { mClassToSuper.put(classInfo, superclass) }
    }

    fun mapClassToInterface(classInfo: TextClassItem, iface: String) {
        if (!mClassToInterface.containsKey(classInfo)) {
            mClassToInterface.put(classInfo, ArrayList())
        }
        mClassToInterface[classInfo]?.add(iface)
    }

    fun implementsInterface(classInfo: TextClassItem, iface: String): Boolean {
        return mClassToInterface[classInfo]?.contains(iface) ?: false
    }

    fun addPackage(pInfo: TextPackageItem) {
        // track the set of organized packages in the API
        mPackages.put(pInfo.name(), pInfo)

        // accumulate a direct map of all the classes in the API
        for (cl in pInfo.allClasses()) {
            mAllClasses.put(cl.qualifiedName(), cl as TextClassItem)
        }
    }

    private fun resolveSuperclasses() {
        for (cl in mAllClasses.values) {
            // java.lang.Object has no superclass
            if (cl.isJavaLangObject()) {
                continue
            }
            var scName: String? = mClassToSuper[cl]
            if (scName == null) {
                scName = "java.lang.Object"
            }
            var superclass: TextClassItem? = mAllClasses[scName]
            if (superclass == null) {
                // Superclass not provided by this codebase. Inject a stub.
                superclass = TextClassItem.createClassStub(this, scName)
            }
            cl.setSuperClass(superclass)
        }
    }

    private fun resolveThrowsClasses() {
        for (cl in mAllClasses.values) {
            for (methodItem in cl.methods()) {
                val methodInfo = methodItem as TextMethodItem
                val names = methodInfo.throwsTypeNames()
                if (!names.isEmpty()) {
                    val result = ArrayList<TextClassItem>()
                    for (exception in names) {
                        var exceptionClass: TextClassItem? = mAllClasses[exception]
                        if (exceptionClass == null) {
                            // Exception not provided by this codebase. Inject a stub.
                            exceptionClass = TextClassItem.createClassStub(
                                this, exception
                            )
                        }
                        result.add(exceptionClass)
                    }
                    methodInfo.setThrowsList(result)
                }
            }

            // java.lang.Object has no superclass
            var scName: String? = mClassToSuper[cl]
            if (scName == null) {
                scName = "java.lang.Object"
            }
            var superclass: TextClassItem? = mAllClasses[scName]
            if (superclass == null) {
                // Superclass not provided by this codebase. Inject a stub.
                superclass = TextClassItem.createClassStub(this, scName)
            }
            cl.setSuperClass(superclass)
        }
    }

    private fun resolveInnerClasses() {
        mPackages.values
            .asSequence()
            .map { it.classList().listIterator() as MutableListIterator<ClassItem> }
            .forEach {
                while (it.hasNext()) {
                    val cl = it.next() as TextClassItem
                    val name = cl.name
                    var index = name.lastIndexOf('.')
                    if (index != -1) {
                        cl.name = name.substring(index + 1)
                        val qualifiedName = cl.qualifiedName
                        index = qualifiedName.lastIndexOf('.')
                        assert(index != -1) { qualifiedName }
                        val outerClassName = qualifiedName.substring(0, index)
                        val outerClass = mAllClasses[outerClassName]!!
                        cl.containingClass = outerClass
                        outerClass.addInnerClass(cl)

                        // Should no longer be listed as top level
                        it.remove()
                    }
                }
            }
    }

    fun postProcess() {
        resolveSuperclasses()
        resolveInterfaces()
        resolveThrowsClasses()
        resolveInnerClasses()
    }

    override fun findPackage(pkgName: String): PackageItem? {
        return mPackages.values.firstOrNull { pkgName == it.qualifiedName() }
    }

    override fun accept(visitor: ItemVisitor) {
        getPackages().accept(visitor)
    }

    override fun acceptTypes(visitor: TypeVisitor) {
        getPackages().acceptTypes(visitor)
    }

    override fun compareWith(visitor: ComparisonVisitor, other: Codebase) {
        CodebaseComparator().compare(visitor, this, other)
    }

    override fun createAnnotation(source: String, context: Item?, mapName: Boolean): AnnotationItem {
        return TextBackedAnnotationItem(this, source, mapName)
    }

    override fun toString(): String {
        return description
    }

    override fun unsupported(desc: String?): Nothing {
        error(desc ?: "Not supported for a signature-file based codebase")
    }

    override fun filter(filterEmit: Predicate<Item>, filterReference: Predicate<Item>): Codebase {
        unsupported()
    }

    // Copied from Converter:

    fun obtainTypeFromString(type: String): TextTypeItem {
        return mTypesFromString.obtain(type) as TextTypeItem
    }

    private val mTypesFromString = object : Cache(this) {
        override fun make(o: Any): Any {
            val name = o as String

            return TextTypeItem(codebase, name)
        }
    }

    private abstract class Cache(val codebase: ApiInfo) {

        protected var mCache = HashMap<Any, Any>()

        internal fun obtain(o: Any?): Any? {
            if (o == null) {
                return null
            }
            var r: Any? = mCache[o]
            if (r == null) {
                r = make(o)
                mCache.put(o, r)
            }
            return r
        }

        protected abstract fun make(o: Any): Any
    }
}
