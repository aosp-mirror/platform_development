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

import com.android.SdkConstants.AMP_ENTITY
import com.android.SdkConstants.APOS_ENTITY
import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.DOT_CLASS
import com.android.SdkConstants.DOT_JAR
import com.android.SdkConstants.DOT_XML
import com.android.SdkConstants.DOT_ZIP
import com.android.SdkConstants.GT_ENTITY
import com.android.SdkConstants.INT_DEF_ANNOTATION
import com.android.SdkConstants.LT_ENTITY
import com.android.SdkConstants.QUOT_ENTITY
import com.android.SdkConstants.STRING_DEF_ANNOTATION
import com.android.SdkConstants.TYPE_DEF_FLAG_ATTRIBUTE
import com.android.SdkConstants.TYPE_DEF_VALUE_ATTRIBUTE
import com.android.SdkConstants.VALUE_TRUE
import com.android.annotations.NonNull
import com.android.tools.lint.annotations.ApiDatabase
import com.android.tools.lint.annotations.Extractor.ANDROID_INT_DEF
import com.android.tools.lint.annotations.Extractor.ANDROID_NOTNULL
import com.android.tools.lint.annotations.Extractor.ANDROID_NULLABLE
import com.android.tools.lint.annotations.Extractor.ANDROID_STRING_DEF
import com.android.tools.lint.annotations.Extractor.ATTR_PURE
import com.android.tools.lint.annotations.Extractor.ATTR_VAL
import com.android.tools.lint.annotations.Extractor.IDEA_CONTRACT
import com.android.tools.lint.annotations.Extractor.IDEA_MAGIC
import com.android.tools.lint.annotations.Extractor.IDEA_NOTNULL
import com.android.tools.lint.annotations.Extractor.IDEA_NULLABLE
import com.android.tools.lint.annotations.Extractor.SUPPORT_NOTNULL
import com.android.tools.lint.annotations.Extractor.SUPPORT_NULLABLE
import com.android.tools.lint.detector.api.LintUtils.getChildren
import com.android.tools.metalava.model.AnnotationAttribute
import com.android.tools.metalava.model.AnnotationAttributeValue
import com.android.tools.metalava.model.AnnotationItem
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.DefaultAnnotationValue
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.psi.PsiAnnotationItem
import com.android.utils.XmlUtils
import com.google.common.base.Charsets
import com.google.common.base.Splitter
import com.google.common.collect.ImmutableSet
import com.google.common.io.ByteStreams
import com.google.common.io.Closeables
import com.google.common.io.Files
import com.google.common.xml.XmlEscapers
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.SAXParseException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.reflect.Field
import java.util.*
import java.util.jar.JarInputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import kotlin.Comparator

/** Merges annotations into classes already registered in the given [Codebase] */
class AnnotationsMerger(
    private val codebase: Codebase,
    private val apiFilter: ApiDatabase?,
    private val listIgnored: Boolean = true
) {
    fun merge(mergeAnnotations: List<File>) {
        mergeAnnotations.forEach { mergeExisting(it) }
    }

    private fun mergeExisting(@NonNull file: File) {
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (child in files) {
                    mergeExisting(child)
                }
            }
        } else if (file.isFile) {
            if (file.path.endsWith(DOT_JAR) || file.path.endsWith(DOT_ZIP)) {
                mergeFromJar(file)
            } else if (file.path.endsWith(DOT_XML)) {
                try {
                    val xml = Files.asCharSource(file, Charsets.UTF_8).read()
                    mergeAnnotationsXml(file.path, xml)
                } catch (e: IOException) {
                    error("Aborting: I/O problem during transform: " + e.toString())
                }

            }
        }
    }

    private fun mergeFromJar(@NonNull jar: File) {
        // Reads in an existing annotations jar and merges in entries found there
        // with the annotations analyzed from source.
        var zis: JarInputStream? = null
        try {
            val fis = FileInputStream(jar)
            zis = JarInputStream(fis)
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".xml")) {
                    val bytes = ByteStreams.toByteArray(zis)
                    val xml = String(bytes, Charsets.UTF_8)
                    mergeAnnotationsXml(jar.path + ": " + entry, xml)
                }
                entry = zis.nextEntry
            }
        } catch (e: IOException) {
            error("Aborting: I/O problem during transform: " + e.toString())
        } finally {
            try {
                Closeables.close(zis, true /* swallowIOException */)
            } catch (e: IOException) {
                // cannot happen
            }
        }
    }

    private fun mergeAnnotationsXml(@NonNull path: String, @NonNull xml: String) {
        try {
            val document = XmlUtils.parseDocument(xml, false)
            mergeDocument(document)
        } catch (e: Exception) {
            var message = "Failed to merge " + path + ": " + e.toString()
            if (e is SAXParseException) {
                message = "Line " + e.lineNumber + ":" + e.columnNumber + ": " + message
            }
            error(message)
            if (e !is IOException) {
                e.printStackTrace()
            }
        }
    }

    internal fun error(message: String) {
        // TODO: Integrate with metalava error facility
        options.stderr.println("Error: " + message)
    }

    internal fun warning(message: String) {
        options.stdout.println("Warning: " + message)
    }

    @Suppress("PrivatePropertyName")
    private val XML_SIGNATURE: Pattern = Pattern.compile(
        // Class (FieldName | Type? Name(ArgList) Argnum?)
        //"(\\S+) (\\S+|(.*)\\s+(\\S+)\\((.*)\\)( \\d+)?)");
        "(\\S+) (\\S+|((.*)\\s+)?(\\S+)\\((.*)\\)( \\d+)?)"
    )

    private fun mergeDocument(@NonNull document: Document) {

        val root = document.documentElement
        val rootTag = root.tagName
        assert(rootTag == "root") { rootTag }

        for (item in getChildren(root)) {
            var signature: String? = item.getAttribute(ATTR_NAME)
            if (signature == null || signature == "null") {
                continue // malformed item
            }

            signature = unescapeXml(signature)
            if (signature == "java.util.Calendar int get(int)") {
                // https://youtrack.jetbrains.com/issue/IDEA-137385
                continue
            } else if (signature == "java.util.Calendar void set(int, int, int) 1"
                || signature == "java.util.Calendar void set(int, int, int, int, int) 1"
                || signature == "java.util.Calendar void set(int, int, int, int, int, int) 1") {
                // http://b.android.com/76090
                continue
            }

            val matcher = XML_SIGNATURE.matcher(signature)
            if (matcher.matches()) {
                val containingClass = matcher.group(1)
                if (containingClass == null) {
                    warning("Could not find class for " + signature)
                    continue
                }

                if (apiFilter != null &&
                    !hasHistoricData(item) &&
                    !apiFilter.hasClass(containingClass)) {
                    if (listIgnored) {
                        warning("Skipping imported element because it is not part of the API file: $containingClass")
                    }
                    continue
                }

                val classItem = codebase.findClass(containingClass)
                if (classItem == null) {
                    warning("Could not find class $containingClass; omitting annotations merge")
                    continue
                }

                val methodName = matcher.group(5)
                if (methodName != null) {
                    val parameters = matcher.group(6)
                    val parameterIndex =
                        if (matcher.group(7) != null) {
                            Integer.parseInt(matcher.group(7).trim())
                        } else {
                            -1
                        }
                    mergeMethodOrParameter(item, containingClass, classItem, methodName, parameterIndex, parameters)
                } else {
                    val fieldName = matcher.group(2)
                    mergeField(item, containingClass, classItem, fieldName)
                }
            } else if (signature.indexOf(' ') == -1 && signature.indexOf('.') != -1) {
                // Must be just a class
                val containingClass = signature
                if (apiFilter != null &&
                    !hasHistoricData(item) &&
                    !apiFilter.hasClass(containingClass)) {
                    if (listIgnored) {
                        warning("Skipping imported element because it is not part of the API file: $containingClass")
                    }
                    continue
                }

                val classItem = codebase.findClass(containingClass)
                if (classItem == null) {
                    warning("Could not find class $containingClass; omitting annotations merge")
                    continue
                }

                mergeAnnotations(item, classItem)
            } else {
                warning("No merge match for signature " + signature)
            }
        }
    }

    // The parameter declaration used in XML files should not have duplicated spaces,
    // and there should be no space after commas (we can't however strip out all spaces,
    // since for example the spaces around the "extends" keyword needs to be there in
    // types like Map<String,? extends Number>
    private fun fixParameterString(parameters: String): String {
        return parameters.replace("  ", " ").replace(", ", ",").replace("?super", "? super ").replace("?extends", "? extends ")
    }

    private fun mergeMethodOrParameter(
        item: Element, containingClass: String, classItem: ClassItem,
        methodName: String, parameterIndex: Int,
        parameters: String
    ) {
        @Suppress("NAME_SHADOWING")
        val parameters = fixParameterString(parameters)

        if (apiFilter != null &&
            !hasHistoricData(item) &&
            !apiFilter.hasMethod(containingClass, methodName, parameters)) {
            if (listIgnored) {
                warning(
                    "Skipping imported element because it is not part of the API file: "
                            + containingClass + "#" + methodName + "(" + parameters + ")"
                )
            }
            return
        }

        val methodItem: MethodItem? = classItem.findMethod(methodName, parameters)
        if (methodItem == null) {
            warning("Could not find class $methodName($parameters) in $containingClass; omitting annotations merge")
            return
        }

        if (parameterIndex != -1) {
            val parameterItem = methodItem.parameters()[parameterIndex]

            if ("java.util.Calendar" == containingClass && "set" == methodName
                && parameterIndex > 0) {
                // Skip the metadata for Calendar.set(int, int, int+); see
                // https://code.google.com/p/android/issues/detail?id=73982
                return
            }

            mergeAnnotations(item, parameterItem)
        } else {
            // Annotation on the method itself
            mergeAnnotations(item, methodItem)
        }
    }

    private fun mergeField(item: Element, containingClass: String, classItem: ClassItem, fieldName: String) {
        if (apiFilter != null &&
            !hasHistoricData(item) &&
            !apiFilter.hasField(containingClass, fieldName)) {
            if (listIgnored) {
                warning(
                    "Skipping imported element because it is not part of the API file: "
                            + containingClass + "#" + fieldName
                )
            }
        } else {
            val fieldItem = classItem.findField(fieldName)
            if (fieldItem == null) {
                warning("Could not find field $fieldName in $containingClass; omitting annotations merge")
                return
            }

            mergeAnnotations(item, fieldItem)
        }
    }

    private fun getAnnotationName(element: Element): String {
        val tagName = element.tagName
        assert(tagName == "annotation") { tagName }

        val qualifiedName = element.getAttribute(ATTR_NAME)
        assert(qualifiedName != null && !qualifiedName.isEmpty())
        return qualifiedName
    }

    private fun mergeAnnotations(xmlElement: Element, item: Item): Int {
        var count = 0

        loop@ for (annotationElement in getChildren(xmlElement)) {
            val qualifiedName = getAnnotationName(annotationElement)
            if (!AnnotationItem.isSignificantAnnotation(qualifiedName)) {
                continue
            }
            var haveNullable = false
            var haveNotNull = false
            for (existing in item.modifiers.annotations()) {
                val name = existing.qualifiedName() ?: continue
                if (isNonNull(name)) {
                    haveNotNull = true
                }
                if (isNullable(name)) {
                    haveNullable = true
                }
                if (name == qualifiedName) {
                    continue@loop
                }
            }

            // Make sure we don't have a conflict between nullable and not nullable
            if (isNonNull(qualifiedName) && haveNullable || isNullable(qualifiedName) && haveNotNull) {
                warning("Found both @Nullable and @NonNull after import for " + item)
                continue
            }

            val annotationItem = createAnnotation(annotationElement) ?: continue
            item.mutableModifiers().addAnnotation(annotationItem)
            count++
        }

        return count
    }

    /** Reads in annotation data from an XML item (using IntelliJ IDE's external annotations XML format) and
     * creates a corresponding [AnnotationItem], performing some "translations" in the process (e.g. mapping
     * from IntelliJ annotations like `org.jetbrains.annotations.Nullable` to `android.support.annotation.Nullable`,
     * as well as dropping constants from typedefs that aren't included according to the [apiFilter]. */
    private fun createAnnotation(annotationElement: Element): AnnotationItem? {
        val tagName = annotationElement.tagName
        assert(tagName == "annotation") { tagName }
        val name = annotationElement.getAttribute(ATTR_NAME)
        assert(name != null && !name.isEmpty())
        when {
            name == IDEA_MAGIC -> {
                val children = getChildren(annotationElement)
                assert(children.size == 1) { children.size }
                val valueElement = children[0]
                val valName = valueElement.getAttribute(ATTR_NAME)
                var value = valueElement.getAttribute(ATTR_VAL)
                val flagsFromClass = valName == "flagsFromClass"
                val flag = valName == "flags" || flagsFromClass
                if (valName == "valuesFromClass" || flagsFromClass) {
                    // Not supported
                    var found = false
                    if (value.endsWith(DOT_CLASS)) {
                        val clsName = value.substring(0, value.length - DOT_CLASS.length)
                        val sb = StringBuilder()
                        sb.append('{')

                        var reflectionFields: Array<Field>? = null
                        try {
                            val cls = Class.forName(clsName)
                            reflectionFields = cls.declaredFields
                        } catch (ignore: Exception) {
                            // Class not available: not a problem. We'll rely on API filter.
                            // It's mainly used for sorting anyway.
                        }

                        if (apiFilter != null) {
                            // Search in API database
                            var fields: Set<String>? = apiFilter.getDeclaredIntFields(clsName)
                            if ("java.util.zip.ZipEntry" == clsName) {
                                // The metadata says valuesFromClass ZipEntry, and unfortunately
                                // that class implements ZipConstants and therefore imports a large
                                // number of irrelevant constants that aren't valid here. Instead,
                                // only allow these two:
                                fields = ImmutableSet.of("STORED", "DEFLATED")
                            }

                            if (fields != null) {
                                val sorted = ArrayList(fields)
                                Collections.sort(sorted)
                                if (reflectionFields != null) {
                                    val rank = HashMap<String, Int>()
                                    run {
                                        var i = 0
                                        val n = sorted.size
                                        while (i < n) {
                                            rank.put(sorted[i], reflectionFields.size + i)
                                            i++

                                        }
                                    }
                                    var i = 0
                                    val n = reflectionFields.size
                                    while (i < n) {
                                        rank.put(reflectionFields[i].name, i)
                                        i++
                                    }
                                    sorted.sortWith(Comparator { o1, o2 ->
                                        val rank1 = rank[o1]
                                        val rank2 = rank[o2]
                                        val delta = rank1!! - rank2!!
                                        if (delta != 0) {
                                            return@Comparator delta

                                        }
                                        o1.compareTo(o2)
                                    })
                                }
                                var first = true
                                for (field in sorted) {
                                    if (first) {
                                        first = false
                                    } else {
                                        sb.append(',').append(' ')
                                    }
                                    sb.append(clsName).append('.').append(field)
                                }
                                found = true
                            }
                        }
                        // Attempt to sort in reflection order
                        if (!found && reflectionFields != null && (apiFilter == null || apiFilter.hasClass(clsName))) {
                            // Attempt with reflection
                            var first = true
                            for (field in reflectionFields) {
                                if (field.type == Integer.TYPE || field.type == Int::class.javaPrimitiveType) {
                                    if (first) {
                                        first = false
                                    } else {
                                        sb.append(',').append(' ')
                                    }
                                    sb.append(clsName).append('.').append(field.name)
                                }
                            }
                        }
                        sb.append('}')
                        value = sb.toString()
                        if (sb.length > 2) { // 2: { }
                            found = true
                        }
                    }

                    if (!found) {
                        return null
                    }
                }

                if (apiFilter != null) {
                    value = removeFiltered(value)
                    while (value.contains(", ,")) {
                        value = value.replace(", ,", ",")
                    }
                    if (value.startsWith(", ")) {
                        value = value.substring(2)
                    }
                }

                val attributes = mutableListOf<XmlBackedAnnotationAttribute>()
                attributes.add(XmlBackedAnnotationAttribute(TYPE_DEF_VALUE_ATTRIBUTE, value))
                if (flag) {
                    attributes.add(XmlBackedAnnotationAttribute(TYPE_DEF_FLAG_ATTRIBUTE, VALUE_TRUE))
                }
                return PsiAnnotationItem.create(
                    codebase, XmlBackedAnnotationItem(
                        codebase,
                        if (valName == "stringValues") STRING_DEF_ANNOTATION else INT_DEF_ANNOTATION, attributes
                    )
                )
            }

            name == STRING_DEF_ANNOTATION ||
                    name == ANDROID_STRING_DEF ||
                    name == INT_DEF_ANNOTATION ||
                    name == ANDROID_INT_DEF -> {
                val children = getChildren(annotationElement)
                var valueElement = children[0]
                val valName = valueElement.getAttribute(ATTR_NAME)
                assert(TYPE_DEF_VALUE_ATTRIBUTE == valName)
                val value = valueElement.getAttribute(ATTR_VAL)
                var flag = false
                if (children.size == 2) {
                    valueElement = children[1]
                    assert(TYPE_DEF_FLAG_ATTRIBUTE == valueElement.getAttribute(ATTR_NAME))
                    flag = VALUE_TRUE == valueElement.getAttribute(ATTR_VAL)
                }
                val intDef = INT_DEF_ANNOTATION == name || ANDROID_INT_DEF == name

                val attributes = mutableListOf<XmlBackedAnnotationAttribute>()
                attributes.add(XmlBackedAnnotationAttribute(TYPE_DEF_VALUE_ATTRIBUTE, value))
                if (flag) {
                    attributes.add(XmlBackedAnnotationAttribute(TYPE_DEF_FLAG_ATTRIBUTE, VALUE_TRUE))
                }
                return PsiAnnotationItem.create(
                    codebase, XmlBackedAnnotationItem(
                        codebase,
                        if (intDef) INT_DEF_ANNOTATION else STRING_DEF_ANNOTATION, attributes
                    )
                )
            }

            name == IDEA_CONTRACT -> {
                val children = getChildren(annotationElement)
                val valueElement = children[0]
                val value = valueElement.getAttribute(ATTR_VAL)
                val pure = valueElement.getAttribute(ATTR_PURE)
                return if (pure != null && !pure.isEmpty()) {
                    PsiAnnotationItem.create(
                        codebase, XmlBackedAnnotationItem(
                            codebase, name,
                            listOf(
                                XmlBackedAnnotationAttribute(TYPE_DEF_VALUE_ATTRIBUTE, value),
                                XmlBackedAnnotationAttribute(ATTR_PURE, pure)
                            )
                        )
                    )
                } else {
                    PsiAnnotationItem.create(
                        codebase, XmlBackedAnnotationItem(
                            codebase, name,
                            listOf(XmlBackedAnnotationAttribute(TYPE_DEF_VALUE_ATTRIBUTE, value))
                        )
                    )
                }
            }

            isNonNull(name) -> return codebase.createAnnotation("@$SUPPORT_NOTNULL")

            isNullable(name) -> return codebase.createAnnotation("@$SUPPORT_NULLABLE")

            else -> {
                val children = getChildren(annotationElement)
                if (children.isEmpty()) {
                    return codebase.createAnnotation("@$name")
                }
                val attributes = mutableListOf<XmlBackedAnnotationAttribute>()
                for (valueElement in children) {
                    attributes.add(
                        XmlBackedAnnotationAttribute(
                            valueElement.getAttribute(ATTR_NAME) ?: continue,
                            valueElement.getAttribute(ATTR_VAL) ?: continue
                        )
                    )
                }
                return PsiAnnotationItem.create(codebase, XmlBackedAnnotationItem(codebase, name, attributes))
            }
        }
    }

    private fun removeFiltered(originalValue: String): String {
        var value = originalValue
        assert(apiFilter != null)
        if (value.startsWith("{")) {
            value = value.substring(1)
        }
        if (value.endsWith("}")) {
            value = value.substring(0, value.length - 1)
        }
        value = value.trim { it <= ' ' }
        val sb = StringBuilder(value.length)
        sb.append('{')
        for (escaped in Splitter.on(',').omitEmptyStrings().trimResults().split(value)) {
            val fqn = unescapeXml(escaped)
            if (fqn.startsWith("\"")) {
                continue
            }
            val index = fqn.lastIndexOf('.')
            val cls = fqn.substring(0, index)
            val field = fqn.substring(index + 1)
            if (apiFilter?.hasField(cls, field) != false) {
                if (sb.length > 1) { // 0: '{'
                    sb.append(", ")
                }
                sb.append(fqn)
            } else if (listIgnored) {
                warning("Skipping constant from typedef because it is not part of the SDK: " + fqn)
            }
        }
        sb.append('}')
        return escapeXml(sb.toString())
    }

    private fun isNonNull(name: String): Boolean {
        return name == IDEA_NOTNULL
                || name == ANDROID_NOTNULL
                || name == SUPPORT_NOTNULL
    }

    private fun isNullable(name: String): Boolean {
        return name == IDEA_NULLABLE
                || name == ANDROID_NULLABLE
                || name == SUPPORT_NULLABLE
    }

    /**
     * Returns true if this XML entry contains historic metadata, e.g. has
     * an api attribute which designates that this API may no longer be in the SDK,
     * but the annotations should be preserved for older API levels
     */
    private fun hasHistoricData(@NonNull item: Element): Boolean {
        var curr: Node? = item.firstChild
        while (curr != null) {
            // Example:
            // <item name="android.provider.Browser BOOKMARKS_URI">
            //   <annotation name="android.support.annotation.RequiresPermission.Read">
            //     <val name="value" val="&quot;com.android.browser.permission.READ_HISTORY_BOOKMARKS&quot;" />
            //     <val name="apis" val="&quot;..22&quot;" />
            //   </annotation>
            //   ..
            if (curr.nodeType == Node.ELEMENT_NODE && "annotation" == curr.nodeName) {
                var inner: Node? = curr.firstChild
                while (inner != null) {
                    if (inner.nodeType == Node.ELEMENT_NODE &&
                        "val" == inner.nodeName &&
                        "apis" == (inner as Element).getAttribute("name")) {
                        return true
                    }
                    inner = inner.nextSibling
                }
            }
            curr = curr.nextSibling
        }

        return false
    }

    @NonNull
    private fun escapeXml(@NonNull unescaped: String): String {
        return XmlEscapers.xmlAttributeEscaper().escape(unescaped)
    }

    @NonNull
    private fun unescapeXml(@NonNull escaped: String): String {
        var workingString = escaped.replace(QUOT_ENTITY, "\"")
        workingString = workingString.replace(LT_ENTITY, "<")
        workingString = workingString.replace(GT_ENTITY, ">")
        workingString = workingString.replace(APOS_ENTITY, "'")
        workingString = workingString.replace(AMP_ENTITY, "&")

        return workingString
    }
}

// TODO: Replace with usage of DefaultAnnotationValue?
data class XmlBackedAnnotationAttribute(
    override val name: String,
    private val valueLiteral: String
) : AnnotationAttribute {
    override val value: AnnotationAttributeValue = DefaultAnnotationValue.create(valueLiteral)

    override fun toString(): String {
        return "$name=$valueLiteral"
    }
}

// TODO: Replace with usage of DefaultAnnotationAttribute?
class XmlBackedAnnotationItem(
    override var codebase: Codebase,
    private val qualifiedName: String,
    private val attributes: List<XmlBackedAnnotationAttribute> = emptyList()
) : AnnotationItem {
    override fun qualifiedName(): String? = AnnotationItem.mapName(codebase, qualifiedName)

    override fun attributes() = attributes

    override fun toSource(): String {
        val qualifiedName = qualifiedName() ?: return ""

        if (attributes.isEmpty()) {
            return "@" + qualifiedName
        }

        val sb = StringBuilder(30)
        sb.append("@")
        sb.append(qualifiedName)
        sb.append("(")
        attributes.joinTo(sb)
        sb.append(")")

        return sb.toString()
    }
}
