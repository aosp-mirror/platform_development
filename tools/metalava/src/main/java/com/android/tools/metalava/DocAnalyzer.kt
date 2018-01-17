package com.android.tools.metalava

import com.android.tools.lint.LintCliClient
import com.android.tools.lint.checks.ApiLookup
import com.android.tools.lint.helpers.DefaultJavaEvaluator
import com.android.tools.metalava.doclava1.Errors
import com.android.tools.metalava.model.AnnotationAttributeValue
import com.android.tools.metalava.model.AnnotationItem
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MemberItem
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.ParameterItem
import com.android.tools.metalava.model.visitors.ApiVisitor
import com.android.tools.metalava.model.visitors.VisibleItemVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 * Walk over the API and apply tweaks to the documentation, such as
 *     - Looking for annotations and converting them to auxiliary tags
 *       that will be processed by the documentation tools later.
 *     - Reading lint's API database and inserting metadata into
 *       the documentation like api levels and deprecation levels.
 *     - Transferring docs from hidden super methods.
 *     - Performing tweaks for common documentation mistakes, such as
 *       ending the first sentence with ", e.g. " where javadoc will sadly
 *       see the ". " and think "aha, that's the end of the sentence!"
 *       (It works around this by replacing the space with &nbsp;.)
 *       This will also attempt to fix common typos (Andriod->Android etc).
 */
class DocAnalyzer(
    /** The codebase to analyze */
    private val codebase: Codebase
) {

    /** Computes the visible part of the API from all the available code in the codebase */
    fun enhance() {
        // Apply options for packages that should be hidden
        documentsFromAnnotations()

        tweakGrammar()

        // TODO:
        // addMinSdkVersionMetadata()
        // addDeprecationMetadata()
        // insertMissingDocFromHiddenSuperclasses()
    }

    val mentionsNull: Pattern = Pattern.compile("\\bnull\\b")

    /** Hide packages explicitly listed in [Options.hidePackages] */
    private fun documentsFromAnnotations() {
        // Note: Doclava1 inserts its own javadoc parameters into the documentation,
        // which is then later processed by javadoc to insert actual descriptions.
        // This indirection makes the actual descriptions of the annotations more
        // configurable from a separate file -- but since this tool isn't hooked
        // into javadoc anymore (and is going to be used by for example Dokka too)
        // instead metalava will generate the descriptions directly in-line into the
        // docs.
        //
        // This does mean that you have to update the metalava source code to update
        // the docs -- but on the other hand all the other docs in the documentation
        // set also requires updating framework source code, so this doesn't seem
        // like an unreasonable burden.

        codebase.accept(object : ApiVisitor(codebase) {
            override fun visitItem(item: Item) {
                val annotations = item.modifiers.annotations()
                if (annotations.isEmpty()) {
                    return
                }

                for (annotation in annotations) {
                    handleAnnotation(annotation, item, depth = 0)
                }

                /* Handled via @memberDoc/@classDoc on the annotations themselves right now.
                   That doesn't handle combinations of multiple thread annotations, but those
                   don't occur yet, right?
                // Threading annotations: can't look at them one at a time; need to list them
                // all together
                if (item is ClassItem || item is MethodItem) {
                    val threads = findThreadAnnotations(annotations)
                    threads?.let {
                        val threadList = it.joinToString(separator = " or ") +
                                (if (it.size == 1) " thread" else " threads")
                        val doc = if (item is ClassItem) {
                            "All methods in this class must be invoked on the $threadList, unless otherwise noted"
                        } else {
                            assert(item is MethodItem)
                            "This method must be invoked on the $threadList"
                        }
                        appendDocumentation(doc, item, false)
                    }
                }
                */
                if (findThreadAnnotations(annotations).size > 1) {
                    reporter.warning(
                        item, "Found more than one threading annotation on $item; " +
                                "the auto-doc feature does not handle this correctly",
                        Errors.MULTIPLE_THREAD_ANNOTATIONS
                    )
                }
            }

            private fun findThreadAnnotations(annotations: List<AnnotationItem>): List<String> {
                var result: MutableList<String>? = null
                for (annotation in annotations) {
                    val name = annotation.qualifiedName()
                    if (name != null && name.endsWith("Thread") && name.startsWith("android.support.annotation.")) {
                        if (result == null) {
                            result = mutableListOf()
                        }
                        val threadName = if (name.endsWith("UiThread")) {
                            "UI"
                        } else {
                            name.substring(name.lastIndexOf('.') + 1, name.length - "Thread".length)
                        }
                        result.add(threadName)
                    }
                }
                return result ?: emptyList()
            }

            /** Fallback if field can't be resolved or if an inlined string value is used */
            private fun findPermissionField(codebase: Codebase, value: Any): FieldItem? {
                val perm = value.toString()
                val permClass = codebase.findClass("android.Manifest.permission")
                permClass?.fields()?.filter {
                    it.initialValue(requireConstant = false)?.toString() == perm
                }?.forEach { return it }
                return null
            }

            private fun handleAnnotation(
                annotation: AnnotationItem,
                item: Item, depth: Int
            ) {
                val name = annotation.qualifiedName()
                if (name == null || name.startsWith("java.lang.")) {
                    // Ignore java.lang.Retention etc.
                    return
                }

                // Some annotations include the documentation they want inlined into usage docs.
                // Copy those here:

                if (annotation.isNullable() || annotation.isNonNull()) {
                    // Some docs already specifically talk about null policy; in that case,
                    // don't include the docs (since it may conflict with more specific conditions
                    // outlined in the docs).
                    if (item.documentation.contains("null") &&
                        mentionsNull.matcher(item.documentation).find()) {
                        return
                    }
                }

                when (item) {
                    is FieldItem -> {
                        addDoc(annotation, "memberDoc", item)
                    }
                    is MethodItem -> {
                        addDoc(annotation, "memberDoc", item)
                        addDoc(annotation, "returnDoc", item)
                    }
                    is ParameterItem -> {
                        addDoc(annotation, "paramDoc", item)
                    }
                    is ClassItem -> {
                        addDoc(annotation, "classDoc", item)
                    }
                }

                // Document required permissions
                if (item is MemberItem && name == "android.support.annotation.RequiresPermission") {
                    for (attribute in annotation.attributes()) {
                        var values: List<AnnotationAttributeValue>? = null
                        var any = false
                        when (attribute.name) {
                            "value", "allOf" -> {
                                values = attribute.leafValues()
                            }
                            "anyOf" -> {
                                any = true
                                values = attribute.leafValues()
                            }
                        }

                        if (values == null || values.isEmpty()) {
                            continue
                        }

                        // Look at macros_override.cs for the usage of these
                        // tags. In particular, search for def:dump_permission

                        val sb = StringBuilder(100)
                        sb.append("Requires ")
                        var first = true
                        for (value in values) {
                            when {
                                first -> first = false
                                any -> sb.append(" or ")
                                else -> sb.append(" and ")
                            }

                            val resolved = value.resolve()
                            val field = if (resolved is FieldItem)
                                resolved
                            else {
                                val v: Any = value.value() ?: value.toSource()
                                findPermissionField(codebase, v)
                            }
                            if (field == null) {
                                reporter.report(
                                    Errors.MISSING_PERMISSION, item,
                                    "Cannot find permission field for $value required by $item (may be hidden or removed)"
                                )
                                //return
                                sb.append(value.toSource())

                            } else {
                                if (field.isHiddenOrRemoved()) {
                                    reporter.report(
                                        Errors.MISSING_PERMISSION, item,
                                        "Permission $value required by $item is hidden or removed"
                                    )
                                }
                                sb.append("{@link ${field.containingClass().qualifiedName()}#${field.name()}}")
                            }
                        }

                        appendDocumentation(sb.toString(), item, false)
                    }
                }

                // Document value ranges
                if (name == "android.support.annotation.IntRange" || name == "android.support.annotation.FloatRange") {
                    val from: String? = annotation.findAttribute("from")?.value?.toSource()
                    val to: String? = annotation.findAttribute("to")?.value?.toSource()
                    // TODO: inclusive/exclusive attributes on FloatRange!
                    if (from != null || to != null) {
                        val args = HashMap<String, String>()
                        if (from != null) args.put("from", from)
                        if (from != null) args.put("from", from)
                        if (to != null) args.put("to", to)
                        val doc = if (from != null && to != null) {
                            "Value is between $from and $to inclusive"
                        } else if (from != null) {
                            "Value is $from or greater"
                        } else if (to != null) {
                            "Value is $to or less"
                        } else {
                            null
                        }
                        appendDocumentation(doc, item, true)
                    }
                }

                // Document expected constants
                if (name == "android.support.annotation.IntDef" || name == "android.support.annotation.LongDef") {
                    val values = annotation.findAttribute("value")?.leafValues() ?: return
                    val flag = annotation.findAttribute("flag")?.value?.toSource() == "true"

                    // Look at macros_override.cs for the usage of these
                    // tags. In particular, search for def:dump_int_def

                    val sb = StringBuilder(100)
                    sb.append("Value is ")
                    if (flag) {
                        sb.append("either <code>0</code> or ")
                        if (values.size > 1) {
                            sb.append("a combination of ")
                        }
                    }

                    values.forEachIndexed { index, value ->
                        sb.append(
                            when (index) {
                                0 -> {
                                    ""
                                }
                                values.size - 1 -> {
                                    if (flag) {
                                        ", and "
                                    } else {
                                        ", or "
                                    }
                                }
                                else -> {
                                    ", "
                                }
                            }
                        )

                        val field = value.resolve()
                        if (field is FieldItem)
                            sb.append("{@link ${field.containingClass().qualifiedName()}#${field.name()}}")
                        else {
                            sb.append(value.toSource())
                        }
                    }
                    appendDocumentation(sb.toString(), item, true)
                }

                // Thread annotations are ignored here because they're handled as a group afterwards

                // TODO: Resource type annotations

                // Handle inner annotations
                annotation.resolve()?.modifiers?.annotations()?.forEach { nested ->
                    if (depth == 20) { // Temp debugging
                        throw StackOverflowError(
                            "Unbounded recursion, processing annotation " +
                                    "${annotation.toSource()} in $item in ${item.compilationUnit()} "
                        )
                    }
                    handleAnnotation(nested, item, depth + 1)
                }
            }
        })
    }

    /**
     * Appends the given documentation to the given item.
     * If it's documentation on a parameter, it is redirected to the surrounding method's
     * documentation.
     *
     * If the [returnValue] flag is true, the documentation is added to the description text
     * of the method, otherwise, it is added to the return tag. This lets for example
     * a threading annotation requirement be listed as part of a method description's
     * text, and a range annotation be listed as part of the return value description.
     * */
    private fun appendDocumentation(doc: String?, item: Item, returnValue: Boolean) {
        doc ?: return

        when (item) {
            is ParameterItem -> item.containingMethod().appendDocumentation(doc, item.name())
            is MethodItem ->
                // Document as part of return annotation, not member doc
                item.appendDocumentation(doc, if (returnValue) "@return" else null)
            else -> item.appendDocumentation(doc)
        }
    }

    private fun addDoc(annotation: AnnotationItem, tag: String, item: Item) {
        // TODO: Cache: we shouldn't have to keep looking this up over and over
        // for example for the nullable/non-nullable annotation classes that
        // are used everywhere!
        val cls = annotation.resolve() ?: return

        val documentation = cls.findTagDocumentation(tag)
        if (documentation != null) {
            assert(documentation.startsWith("@$tag"), { documentation })
            // TODO: Insert it in the right place (@return or @param)
            val section = when {
                documentation.startsWith("@returnDoc") -> "@return"
                documentation.startsWith("@paramDoc") -> "@param"
                documentation.startsWith("@memberDoc") -> null
                else -> null
            }
            val insert = stripMetaTags(documentation.substring(tag.length + 2))
            item.appendDocumentation(insert, section) // 2: @ and space after tag
        }
    }

    private fun stripMetaTags(string: String): String {
        // Get rid of @hide and @remove tags etc that are part of documentation snippets
        // we pull in, such that we don't accidentally start applying this to the
        // item that is pulling in the documentation.
        if (string.contains("@hide") || string.contains("@remove")) {
            return string.replace("@hide", "").replace("@remove", "")
        }
        return string
    }

    /** Replacements to perform in documentation */
    val typos = mapOf(
        "Andriod" to "Android",
        "Kitkat" to "KitKat",
        "LemonMeringuePie" to "Lollipop",
        "LMP" to "Lollipop",
        "KeyLimePie" to "KitKat",
        "KLP" to "KitKat"
    )

    private fun tweakGrammar() {
        codebase.accept(object : VisibleItemVisitor() {
            override fun visitItem(item: Item) {
                var doc = item.documentation
                if (doc.isBlank()) {
                    return
                }

                for (typo in typos.keys) {
                    if (doc.contains(typo)) {
                        val replacement = typos[typo] ?: continue
                        reporter.report(Errors.TYPO, item, "Replaced $typo with $replacement in documentation for $item")
                        doc = doc.replace(typo, replacement, false)
                        item.documentation = doc
                    }
                }

                val firstDot = doc.indexOf(".")
                if (firstDot > 0 && doc.regionMatches(firstDot - 1, "e.g. ", 0, 5, false)) {
                    doc = doc.substring(0, firstDot) + ".g.&nbsp;" + doc.substring(firstDot + 4)
                    item.documentation = doc
                }
            }
        })
    }

    fun applyApiLevels(applyApiLevelsXml: File) {
        val client = object : LintCliClient() {
            override fun findResource(relativePath: String): File? {
                if (relativePath == ApiLookup.XML_FILE_PATH) {
                    return applyApiLevelsXml
                }
                return super.findResource(relativePath)
            }

            override fun getCacheDir(name: String?, create: Boolean): File? {
                val dir = File(System.getProperty("java.io.tmpdir"))
                if (create) {
                    dir.mkdirs()
                }
                return dir
            }
        }

        val apiLookup = ApiLookup.get(client)

        //codebase.accept(object : VisibleItemVisitor(visitConstructorsAsMethods = false) {
        codebase.accept(object : ApiVisitor(codebase, visitConstructorsAsMethods = false) {
            override fun visitMethod(method: MethodItem) {
                val psiMethod = method.psi() as PsiMethod
                addApiLevelDocumentation(apiLookup.getMethodVersion(psiMethod), method)
                addDeprecatedDocumentation(apiLookup.getMethodDeprecatedIn(psiMethod), method)
            }

            override fun visitClass(cls: ClassItem) {
                val psiClass = cls.psi() as PsiClass
                addApiLevelDocumentation(apiLookup.getClassVersion(psiClass), cls)
                addDeprecatedDocumentation(apiLookup.getClassDeprecatedIn(psiClass), cls)
            }

            override fun visitField(field: FieldItem) {
                val psiField = field.psi() as PsiField
                addApiLevelDocumentation(apiLookup.getFieldVersion(psiField), field)
                addDeprecatedDocumentation(apiLookup.getFieldDeprecatedIn(psiField), field)
            }

            private fun addApiLevelDocumentation(level: Int, item: Item) {
                if (level > 1) {
                    appendDocumentation("Requires API level $level", item, false)
                }
            }

            private fun addDeprecatedDocumentation(level: Int, item: Item) {
                if (level > 1) {
                    // TODO: *pre*pend instead!
                    //val description = "This class was deprecated in API level $level. "
                    val description = "<p class=\"caution\"><strong>This class was deprecated in API level 21.</strong></p>"
                    item.appendDocumentation(description, "@deprecated", append = false)
                }
            }
        })
    }
}

fun ApiLookup.getClassVersion(cls: PsiClass): Int {
    val owner = cls.qualifiedName ?: return -1
    return getClassVersion(owner)
}

fun ApiLookup.getMethodVersion(method: PsiMethod): Int {
    val containingClass = method.containingClass ?: return -1
    val owner = containingClass.qualifiedName ?: return -1
    val evaluator = DefaultJavaEvaluator(null, null)
    val desc = evaluator.getMethodDescription(method, false, false)
    return getMethodVersion(owner, method.name, desc)
}

fun ApiLookup.getFieldVersion(field: PsiField): Int {
    val containingClass = field.containingClass ?: return -1
    val owner = containingClass.qualifiedName ?: return -1
    return getFieldVersion(owner, field.name)
}

fun ApiLookup.getClassDeprecatedIn(cls: PsiClass): Int {
    val owner = cls.qualifiedName ?: return -1
    return getClassDeprecatedIn(owner)
}

fun ApiLookup.getMethodDeprecatedIn(method: PsiMethod): Int {
    val containingClass = method.containingClass ?: return -1
    val owner = containingClass.qualifiedName ?: return -1
    val evaluator = DefaultJavaEvaluator(null, null)
    val desc = evaluator.getMethodDescription(method, false, false)
    return getMethodDeprecatedIn(owner, method.name, desc)
}

fun ApiLookup.getFieldDeprecatedIn(field: PsiField): Int {
    val containingClass = field.containingClass ?: return -1
    val owner = containingClass.qualifiedName ?: return -1
    return getFieldDeprecatedIn(owner, field.name)
}