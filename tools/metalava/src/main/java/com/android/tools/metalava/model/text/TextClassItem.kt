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

import com.android.tools.metalava.doclava1.ApiInfo
import com.android.tools.metalava.doclava1.SourcePositionInfo
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.ConstructorItem
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.TypeItem
import java.util.function.Predicate

class TextClassItem(
    override val codebase: ApiInfo,
    position: SourcePositionInfo = SourcePositionInfo.UNKNOWN,
    isPublic: Boolean = false,
    isProtected: Boolean = false,
    isPrivate: Boolean = false,
    isStatic: Boolean = false,
    private var isInterface: Boolean = false,
    isAbstract: Boolean = false,
    private var isEnum: Boolean = false,
    private var isAnnotation: Boolean = false,
    isFinal: Boolean = false,
    val qualifiedName: String = "",
    private val qualifiedTypeName: String = qualifiedName,
    var name: String = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1),
    val annotations: List<String>? = null
) : TextItem(
    codebase = codebase,
    position = position,
    modifiers = TextModifiers(
        codebase = codebase,
        annotationStrings = annotations,
        public = isPublic, protected = isProtected, private = isPrivate,
        static = isStatic, abstract = isAbstract, final = isFinal
    )
), ClassItem {

    init {
        (modifiers as TextModifiers).owner = this
    }

    override val isTypeParameter: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassItem) return false

        return qualifiedName == other.qualifiedName()
    }

    override fun hashCode(): Int {
        return qualifiedName.hashCode()
    }

    override fun interfaceTypes(): List<TypeItem> = interfaceTypes
    override fun allInterfaces(): Sequence<ClassItem> {
        return interfaceTypes.asSequence().map { it.asClass() }.filterNotNull()
    }

    private var innerClasses: List<ClassItem> = mutableListOf()

    override var defaultConstructor: ConstructorItem? = null

    override var hasPrivateConstructor: Boolean = false

    override fun innerClasses(): List<ClassItem> = innerClasses

    override fun hasImplicitDefaultConstructor(): Boolean {
        return false
    }

    override fun isInterface(): Boolean = isInterface
    override fun isAnnotationType(): Boolean = isAnnotation
    override fun isEnum(): Boolean = isEnum

    var containingClass: TextClassItem? = null
    override fun containingClass(): ClassItem? = containingClass

    private var containingPackage: PackageItem? = null

    fun setContainingPackage(containingPackage: TextPackageItem) {
        this.containingPackage = containingPackage
    }

    fun setIsAnnotationType(isAnnotation: Boolean) {
        this.isAnnotation = isAnnotation
    }

    fun setIsEnum(isEnum: Boolean) {
        this.isEnum = isEnum
    }

    override fun containingPackage(): PackageItem = containingPackage ?: error(this)

    override fun toType(): TypeItem = codebase.obtainTypeFromString(
// TODO: No, handle List<String>[]
        if (typeParameterList() != null)
            qualifiedName() + "<" + typeParameterList() + ">"
        else
            qualifiedName()
    )

    override fun hasTypeVariables(): Boolean {
        return typeInfo?.hasTypeArguments() ?: false
    }

    override fun typeParameterList(): String? {
// TODO: No, handle List<String>[]
        val s = typeInfo.toString()
        val index = s.indexOf('<')
        if (index != -1) {
            return s.substring(index)
        }
        return null
    }

    override fun typeParameterNames(): List<String> = codebase.unsupported()

    private var superClass: ClassItem? = null
    private var superClassType: TypeItem? = null

    override fun superClass(): ClassItem? = superClass
    override fun superClassType(): TypeItem? = superClassType

    override fun setSuperClass(superClass: ClassItem?, superClassType: TypeItem?) {
        this.superClass = superClass
        this.superClassType = superClassType
    }

    override fun setInterfaceTypes(interfaceTypes: List<TypeItem>) {
        this.interfaceTypes = interfaceTypes.toMutableList()
    }

    override fun findMethod(methodName: String, parameters: String): MethodItem? = codebase.unsupported()

    override fun findField(fieldName: String): FieldItem? = codebase.unsupported()

    private var typeInfo: TextTypeItem? = null
    fun setTypeInfo(typeInfo: TextTypeItem) {
        this.typeInfo = typeInfo
    }

    fun asTypeInfo(): TextTypeItem {
        if (typeInfo == null) {
            typeInfo = codebase.obtainTypeFromString(qualifiedTypeName)
        }
        return typeInfo!!
    }

    private var interfaceTypes = mutableListOf<TypeItem>()
    private val constructors = mutableListOf<ConstructorItem>()
    private val methods = mutableListOf<MethodItem>()
    private val fields = mutableListOf<FieldItem>()

    override fun constructors(): List<ConstructorItem> = constructors
    override fun methods(): List<MethodItem> = methods
    override fun fields(): List<FieldItem> = fields

    fun addInterface(intf: TypeItem) {
        interfaceTypes.add(intf)
    }

    fun addInterface(intf: TextClassItem) {
        interfaceTypes.add(intf.toType())
    }

    fun addConstructor(constructor: TextConstructorItem) {
        constructors += constructor
    }

    fun addMethod(method: TextMethodItem) {
        methods += method
    }

    fun addField(field: TextFieldItem) {
        fields += field
    }

    fun addEnumConstant(field: TextFieldItem) {
        field.setEnumConstant(true)
        fields += field
    }

    fun addInnerClass(cls: TextClassItem) {
        innerClasses += cls
    }

    override fun filteredSuperClassType(predicate: Predicate<Item>): TypeItem? {
        // No filtering in signature files: we assume signature APIs
        // have already been filtered and all items should match.
        // This lets us load signature files and rewrite them using updated
        // output formats etc.
        return superClassType
    }

    private var fullName: String = name
    override fun simpleName(): String = name.substring(name.lastIndexOf('.') + 1)
    override fun fullName(): String = fullName
    override fun qualifiedName(): String = qualifiedName
    override fun toString(): String = qualifiedName()

    companion object {
        fun createClassStub(codebase: ApiInfo, name: String): TextClassItem =
            TextClassItem(codebase = codebase, qualifiedName = name, isPublic = true).also { addStubPackage(name, codebase, it) }

        private fun addStubPackage(
            name: String, codebase: Codebase,
            textClassItem: TextClassItem
        ) {
            val pkgPath = name.substring(0, name.lastIndexOf('.'))
            val pkg = codebase.findPackage(pkgPath) as? TextPackageItem ?: TextPackageItem(codebase, pkgPath, SourcePositionInfo.UNKNOWN)
            textClassItem.setContainingPackage(pkg)
        }

        fun createInterfaceStub(codebase: ApiInfo, name: String): TextClassItem =
            TextClassItem(isInterface = true, codebase = codebase, qualifiedName = name, isPublic = true).also {
                addStubPackage(
                    name,
                    codebase,
                    it
                )
            }
    }
}
