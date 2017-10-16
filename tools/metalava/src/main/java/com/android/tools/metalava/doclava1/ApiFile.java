/*
 * Copyright (C) 2011 Google Inc.
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

package com.android.tools.metalava.doclava1;

import com.android.tools.lint.annotations.Extractor;
import com.android.tools.lint.checks.infrastructure.ClassNameKt;
import com.android.tools.metalava.model.AnnotationItem;
import com.android.tools.metalava.model.text.TextClassItem;
import com.android.tools.metalava.model.text.TextConstructorItem;
import com.android.tools.metalava.model.text.TextFieldItem;
import com.android.tools.metalava.model.text.TextMethodItem;
import com.android.tools.metalava.model.text.TextPackageItem;
import com.android.tools.metalava.model.text.TextParameterItem;
import com.android.tools.metalava.model.text.TextTypeItem;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import kotlin.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.android.tools.metalava.model.FieldItemKt.javaUnescapeString;

//
// Copied from doclava1, but adapted to metalava's code model (plus tweaks to handle
// metalava's richer files, e.g. annotations)
//
public class ApiFile {
    public static ApiInfo parseApi(File file,
                                   boolean kotlinStyleNulls,
                                   boolean supportsStagedNullability) throws ApiParseException {
        try {
            String apiText = Files.asCharSource(file, Charsets.UTF_8).read();
            return parseApi(file.getPath(), apiText, kotlinStyleNulls, supportsStagedNullability);
        } catch (IOException ex) {
            throw new ApiParseException("Error reading API file", ex);
        }
    }

    public static ApiInfo parseApi(String filename, String apiText,
                                   boolean kotlinStyleNulls,
                                   boolean supportsStagedNullability) throws ApiParseException {
        if (apiText.contains("/*")) {
            apiText = ClassNameKt.stripComments(apiText, false); // line comments are used to stash field constants
        }

        final Tokenizer tokenizer = new Tokenizer(filename, apiText.toCharArray());
        final ApiInfo api = new ApiInfo();
        api.setSupportsStagedNullability(supportsStagedNullability);
        api.setKotlinStyleNulls(kotlinStyleNulls);

        while (true) {
            String token = tokenizer.getToken();
            if (token == null) {
                break;
            }
            if ("package".equals(token)) {
                parsePackage(api, tokenizer);
            } else {
                throw new ApiParseException("expected package got " + token, tokenizer.getLine());
            }
        }

        api.postProcess();

        return api;
    }

    private static void parsePackage(ApiInfo api, Tokenizer tokenizer)
            throws ApiParseException {
        String token;
        String name;
        TextPackageItem pkg;

        token = tokenizer.requireToken();
        assertIdent(tokenizer, token);
        name = token;
        pkg = new TextPackageItem(api, name, tokenizer.pos());
        token = tokenizer.requireToken();
        if (!"{".equals(token)) {
            throw new ApiParseException("expected '{' got " + token, tokenizer.getLine());
        }
        while (true) {
            token = tokenizer.requireToken();
            if ("}".equals(token)) {
                break;
            } else {
                parseClass(api, pkg, tokenizer, token);
            }
        }
        api.addPackage(pkg);
    }

    private static void parseClass(ApiInfo api, TextPackageItem pkg, Tokenizer tokenizer, String token)
            throws ApiParseException {
        boolean pub = false;
        boolean prot = false;
        boolean stat = false;
        boolean fin = false;
        boolean abs = false;
        boolean dep = false;
        boolean isInterface = false;
        boolean isAnnotation = false;
        boolean isEnum = false;
        String name;
        String qname;
        String ext = null;
        TextClassItem cl;

        // Metalava: including annotations in file now
        List<String> annotations = null;
        Pair<String, List<String>> result = getAnnotations(tokenizer, token);
        if (result != null) {
            token = result.component1();
            annotations = result.component2();
        }

        if ("public".equals(token)) {
            pub = true;
            token = tokenizer.requireToken();
        } else if ("protected".equals(token)) {
            prot = true;
            token = tokenizer.requireToken();
        }
        if ("static".equals(token)) {
            stat = true;
            token = tokenizer.requireToken();
        }
        if ("final".equals(token)) {
            fin = true;
            token = tokenizer.requireToken();
        }
        if ("abstract".equals(token)) {
            abs = true;
            token = tokenizer.requireToken();
        }
        if ("deprecated".equals(token)) {
            dep = true;
            token = tokenizer.requireToken();
        }
        if ("class".equals(token)) {
            token = tokenizer.requireToken();
        } else if ("interface".equals(token)) {
            isInterface = true;
            token = tokenizer.requireToken();
        } else if ("@interface".equals(token)) {
            // Annotation
            abs = true;
            isAnnotation = true;
            token = tokenizer.requireToken();
        } else if ("enum".equals(token)) {
            isEnum = true;
            fin = true;
            ext = "java.lang.Enum";
            token = tokenizer.requireToken();
        } else {
            throw new ApiParseException("missing class or interface. got: " + token, tokenizer.getLine());
        }
        assertIdent(tokenizer, token);
        name = token;
        qname = qualifiedName(pkg.name(), name);
        final TextTypeItem typeInfo = api.obtainTypeFromString(qname);
        // Simple type info excludes the package name (but includes enclosing class names)
        final TextTypeItem simpleTypeInfo = api.obtainTypeFromString(name);
        token = tokenizer.requireToken();

        cl = new TextClassItem(api, tokenizer.pos(), pub, prot,
                false/*isPrivate*/, stat, isInterface, abs, isEnum, isAnnotation,
                fin, typeInfo.toErasedTypeString(), typeInfo.qualifiedTypeName(),
                simpleTypeInfo.toErasedTypeString(), annotations);
        cl.setContainingPackage(pkg);
        cl.setTypeInfo(typeInfo);
        cl.setDeprecated(dep);
        if ("extends".equals(token)) {
            token = tokenizer.requireToken();
            assertIdent(tokenizer, token);
            ext = token;
            token = tokenizer.requireToken();
        }
        // Resolve superclass after done parsing
        api.mapClassToSuper(cl, ext);
        if ("implements".equals(token)) {
            while (true) {
                token = tokenizer.requireToken();
                if ("{".equals(token)) {
                    break;
                } else {
                    /// TODO
                    if (!",".equals(token)) {
                        api.mapClassToInterface(cl, token);
                    }
                }
            }
        }
        if ("java.lang.Enum".equals(ext)) {
            cl.setIsEnum(true);
        } else if (isAnnotation) {
            api.mapClassToInterface(cl, "java.lang.annotation.Annotation");
        } else if (api.implementsInterface(cl, "java.lang.annotation.Annotation")) {
            cl.setIsAnnotationType(true);
        }
        if (!"{".equals(token)) {
            throw new ApiParseException("expected {", tokenizer.getLine());
        }
        token = tokenizer.requireToken();
        while (true) {
            if ("}".equals(token)) {
                break;
            } else if ("ctor".equals(token)) {
                token = tokenizer.requireToken();
                parseConstructor(api, tokenizer, cl, token);
            } else if ("method".equals(token)) {
                token = tokenizer.requireToken();
                parseMethod(api, tokenizer, cl, token);
            } else if ("field".equals(token)) {
                token = tokenizer.requireToken();
                parseField(api, tokenizer, cl, token, false);
            } else if ("enum_constant".equals(token)) {
                token = tokenizer.requireToken();
                parseField(api, tokenizer, cl, token, true);
            } else {
                throw new ApiParseException("expected ctor, enum_constant, field or method", tokenizer.getLine());
            }
            token = tokenizer.requireToken();
        }
        pkg.addClass(cl);
    }

    private static Pair<String, List<String>> processKotlinTypeSuffix(ApiInfo api, String token, List<String> annotations) throws ApiParseException {
        if (api.getKotlinStyleNulls()) {
            if (token.endsWith("?")) {
                token = token.substring(0, token.length() - 1);
                annotations = mergeAnnotations(annotations, Extractor.SUPPORT_NULLABLE);
            } else if (token.endsWith("!")) {
                token = token.substring(0, token.length() - 1);
            } else if (!token.endsWith("!")) {
                if (!TextTypeItem.Companion.isPrimitive(token)) { // Don't add nullness on primitive types like void
                    annotations = mergeAnnotations(annotations, Extractor.SUPPORT_NOTNULL);
                }
            }
        } else if (token.endsWith("?") || token.endsWith("!")) {
            throw new ApiParseException("Did you forget to supply --input-kotlin-nulls? Found Kotlin-style null type suffix when parser was not configured " +
                    "to interpret signature file that way: " + token);
        }
        //noinspection unchecked
        return new Pair<>(token, annotations);
    }

    private static Pair<String, List<String>> getAnnotations(Tokenizer tokenizer, String token) throws ApiParseException {
        List<String> annotations = null;

        while (true) {
            if (token.startsWith("@")) {
                // Annotation
                String annotation = token;
                if (annotation.indexOf('.') == -1) {
                    // Restore annotations that were shortened on export
                    annotation = AnnotationItem.Companion.unshortenAnnotation(annotation);
                }
                token = tokenizer.requireToken();
                if (token.equals("(")) {
                    // Annotation arguments
                    int start = tokenizer.offset() - 1;
                    while (!token.equals(")")) {
                        token = tokenizer.requireToken();
                    }
                    annotation += tokenizer.getStringFromOffset(start);
                    token = tokenizer.requireToken();
                }
                if (annotations == null) {
                    annotations = new ArrayList<>();
                }
                annotations.add(annotation);
            } else {
                break;
            }
        }

        if (annotations != null) {
            //noinspection unchecked
            return new Pair<>(token, annotations);
        } else {
            return null;
        }
    }

    private static void parseConstructor(ApiInfo api, Tokenizer tokenizer, TextClassItem cl, String token)
            throws ApiParseException {
        boolean pub = false;
        boolean prot = false;
        boolean dep = false;
        String name;
        TextConstructorItem method;

        // Metalava: including annotations in file now
        List<String> annotations = null;
        Pair<String, List<String>> result = getAnnotations(tokenizer, token);
        if (result != null) {
            token = result.component1();
            annotations = result.component2();
        }

        if ("public".equals(token)) {
            pub = true;
            token = tokenizer.requireToken();
        } else if ("protected".equals(token)) {
            prot = true;
            token = tokenizer.requireToken();
        }
        if ("deprecated".equals(token)) {
            dep = true;
            token = tokenizer.requireToken();
        }
        assertIdent(tokenizer, token);
        name = token.substring(token.lastIndexOf('.') + 1); // For inner classes, strip outer classes from name
        token = tokenizer.requireToken();
        if (!"(".equals(token)) {
            throw new ApiParseException("expected (", tokenizer.getLine());
        }
        method = new TextConstructorItem(api, /*typeParameters*/
                name, /*signature*/ cl, pub, prot, false/*isPrivate*/, false/*isFinal*/,
                false/*isStatic*/, /*isSynthetic*/ false/*isAbstract*/, false/*isSynthetic*/,
                false/*isNative*/, false/* isDefault */,
                /*isAnnotationElement*/  /*flatSignature*/
                /*overriddenMethod*/ cl.asTypeInfo(),
                /*thrownExceptions*/ tokenizer.pos(), annotations);
        method.setDeprecated(dep);
        token = tokenizer.requireToken();
        parseParameterList(api, tokenizer, method, /*new HashSet<String>(),*/ token);
        token = tokenizer.requireToken();
        if ("throws".equals(token)) {
            token = parseThrows(tokenizer, method);
        }
        if (!";".equals(token)) {
            throw new ApiParseException("expected ; found " + token, tokenizer.getLine());
        }
        cl.addConstructor(method);
    }

    private static void parseMethod(ApiInfo api, Tokenizer tokenizer, TextClassItem cl, String token)
            throws ApiParseException {
        boolean pub = false;
        boolean prot = false;
        boolean stat = false;
        boolean fin = false;
        boolean abs = false;
        boolean dep = false;
        boolean syn = false;
        boolean def = false;
        TextTypeItem returnType;
        String name;
        TextMethodItem method;
        String typeParameterList = null;

        // Metalava: including annotations in file now
        List<String> annotations = null;
        Pair<String, List<String>> result = getAnnotations(tokenizer, token);
        if (result != null) {
            token = result.component1();
            annotations = result.component2();
        }

        if ("public".equals(token)) {
            pub = true;
            token = tokenizer.requireToken();
        } else if ("protected".equals(token)) {
            prot = true;
            token = tokenizer.requireToken();
        }
        if ("default".equals(token)) {
            def = true;
            token = tokenizer.requireToken();
        }
        if ("static".equals(token)) {
            stat = true;
            token = tokenizer.requireToken();
        }
        if ("final".equals(token)) {
            fin = true;
            token = tokenizer.requireToken();
        }
        if ("abstract".equals(token)) {
            abs = true;
            token = tokenizer.requireToken();
        }
        if ("deprecated".equals(token)) {
            dep = true;
            token = tokenizer.requireToken();
        }
        if ("synchronized".equals(token)) {
            syn = true;
            token = tokenizer.requireToken();
        }
        if ("<".equals(token)) {
            typeParameterList = parseTypeParameterList(tokenizer);
            token = tokenizer.requireToken();
        }
        assertIdent(tokenizer, token);

        Pair<String, List<String>> kotlinTypeSuffix = processKotlinTypeSuffix(api, token, annotations);
        token = kotlinTypeSuffix.getFirst();
        annotations = kotlinTypeSuffix.getSecond();
        returnType = api.obtainTypeFromString(token);

        token = tokenizer.requireToken();
        assertIdent(tokenizer, token);
        name = token;
        method = new TextMethodItem(
                api, name, /*signature*/ cl,
                pub, prot, false/*isPrivate*/, fin, stat, abs/*isAbstract*/,
                syn, false/*isNative*/, def/*isDefault*/,
                returnType, tokenizer.pos(), annotations);
        method.setDeprecated(dep);
        method.setTypeParameterList(typeParameterList);
        token = tokenizer.requireToken();
        if (!"(".equals(token)) {
            throw new ApiParseException("expected (", tokenizer.getLine());
        }
        token = tokenizer.requireToken();
        parseParameterList(api, tokenizer, method, /*typeVariableNames,*/ token);
        token = tokenizer.requireToken();
        if ("throws".equals(token)) {
            token = parseThrows(tokenizer, method);
        }
        if (!";".equals(token)) {
            throw new ApiParseException("expected ; found " + token, tokenizer.getLine());
        }
        cl.addMethod(method);
    }

    private static List<String> mergeAnnotations(List<String> annotations, String annotation) {
        if (annotations == null) {
            annotations = new ArrayList<>();
        }
        annotations.add("@" + annotation);
        return annotations;
    }

    private static void parseField(ApiInfo api, Tokenizer tokenizer, TextClassItem cl, String token, boolean isEnum)
            throws ApiParseException {
        boolean pub = false;
        boolean prot = false;
        boolean stat = false;
        boolean fin = false;
        boolean dep = false;
        boolean trans = false;
        boolean vol = false;
        String type;
        String name;
        String val = null;
        Object v;
        TextFieldItem field;

        // Metalava: including annotations in file now
        List<String> annotations = null;
        Pair<String, List<String>> result = getAnnotations(tokenizer, token);
        if (result != null) {
            token = result.component1();
            annotations = result.component2();
        }

        if ("public".equals(token)) {
            pub = true;
            token = tokenizer.requireToken();
        } else if ("protected".equals(token)) {
            prot = true;
            token = tokenizer.requireToken();
        }
        if ("static".equals(token)) {
            stat = true;
            token = tokenizer.requireToken();
        }
        if ("final".equals(token)) {
            fin = true;
            token = tokenizer.requireToken();
        }
        if ("deprecated".equals(token)) {
            dep = true;
            token = tokenizer.requireToken();
        }
        if ("transient".equals(token)) {
            trans = true;
            token = tokenizer.requireToken();
        }
        if ("volatile".equals(token)) {
            vol = true;
            token = tokenizer.requireToken();
        }
        assertIdent(tokenizer, token);

        Pair<String, List<String>> kotlinTypeSuffix = processKotlinTypeSuffix(api, token, annotations);
        token = kotlinTypeSuffix.getFirst();
        annotations = kotlinTypeSuffix.getSecond();
        type = token;
        TextTypeItem typeInfo = api.obtainTypeFromString(type);

        token = tokenizer.requireToken();
        assertIdent(tokenizer, token);
        name = token;
        token = tokenizer.requireToken();
        if ("=".equals(token)) {
            token = tokenizer.requireToken(false);
            val = token;
            token = tokenizer.requireToken();
        }
        if (!";".equals(token)) {
            throw new ApiParseException("expected ; found " + token, tokenizer.getLine());
        }
        try {
            v = parseValue(type, val);
        } catch (ApiParseException ex) {
            ex.line = tokenizer.getLine();
            throw ex;
        }

        field = new TextFieldItem(api, name, cl, pub, prot, false/*isPrivate*/, fin, stat,
                trans, vol, typeInfo, v, tokenizer.pos(),
                annotations);
        field.setDeprecated(dep);
        if (isEnum) {
            cl.addEnumConstant(field);
        } else {
            cl.addField(field);
        }
    }

    public static Object parseValue(String type, String val) throws ApiParseException {
        if (val != null) {
            if ("boolean".equals(type)) {
                return "true".equals(val) ? Boolean.TRUE : Boolean.FALSE;
            } else if ("byte".equals(type)) {
                return Integer.valueOf(val);
            } else if ("short".equals(type)) {
                return Integer.valueOf(val);
            } else if ("int".equals(type)) {
                return Integer.valueOf(val);
            } else if ("long".equals(type)) {
                return Long.valueOf(val.substring(0, val.length() - 1));
            } else if ("float".equals(type)) {
                if ("(1.0f/0.0f)".equals(val) || "(1.0f / 0.0f)".equals(val)) {
                    return Float.POSITIVE_INFINITY;
                } else if ("(-1.0f/0.0f)".equals(val) || "(-1.0f / 0.0f)".equals(val)) {
                    return Float.NEGATIVE_INFINITY;
                } else if ("(0.0f/0.0f)".equals(val) || "(0.0f / 0.0f)".equals(val)) {
                    return Float.NaN;
                } else {
                    return Float.valueOf(val);
                }
            } else if ("double".equals(type)) {
                if ("(1.0/0.0)".equals(val) || "(1.0 / 0.0)".equals(val)) {
                    return Double.POSITIVE_INFINITY;
                } else if ("(-1.0/0.0)".equals(val) || "(-1.0 / 0.0)".equals(val)) {
                    return Double.NEGATIVE_INFINITY;
                } else if ("(0.0/0.0)".equals(val) || "(0.0 / 0.0)".equals(val)) {
                    return Double.NaN;
                } else {
                    return Double.valueOf(val);
                }
            } else if ("char".equals(type)) {
                return (char) Integer.parseInt(val);
            } else if ("java.lang.String".equals(type)) {
                if ("null".equals(val)) {
                    return null;
                } else {
                    return javaUnescapeString(val.substring(1, val.length() - 1));
                }
            }
        }
        if ("null".equals(val)) {
            return null;
        } else {
            return val;
        }
    }

    private static String parseTypeParameterList(Tokenizer tokenizer) throws ApiParseException {
        String token;

        int start = tokenizer.offset() - 1;
        int balance = 1;
        while (balance > 0) {
            token = tokenizer.requireToken();
            if (token.equals("<")) {
                balance++;
            } else if (token.equals(">")) {
                balance--;
            }
        }

        return tokenizer.getStringFromOffset(start);
    }

    private static void parseParameterList(ApiInfo api, Tokenizer tokenizer, TextMethodItem method,
                                           String token) throws ApiParseException {
        int index = 0;
        while (true) {
            if (")".equals(token)) {
                return;
            }

            // Metalava: including annotations in file now
            List<String> annotations = null;
            Pair<String, List<String>> result = getAnnotations(tokenizer, token);
            if (result != null) {
                token = result.component1();
                annotations = result.component2();
            }

            Pair<String, List<String>> kotlinTypeSuffix = processKotlinTypeSuffix(api, token, annotations);
            token = kotlinTypeSuffix.getFirst();
            annotations = kotlinTypeSuffix.getSecond();
            String type = token;
            TextTypeItem typeInfo = api.obtainTypeFromString(token);

            String name = null;
            token = tokenizer.requireToken();
            if (isIdent(token)) {
                name = token;
                token = tokenizer.requireToken();
            } else {
                name = "arg" + (++index);
            }
            if (",".equals(token)) {
                token = tokenizer.requireToken();
            } else if (")".equals(token)) {
            } else {
                throw new ApiParseException("expected , found " + token, tokenizer.getLine());
            }

            method.addParameter(new TextParameterItem(api, method, name, type,
                    typeInfo,
                    type.endsWith("..."),
                    tokenizer.pos(),
                    annotations));
            if (type.endsWith("...")) {
                method.setVarargs(true);
            }
        }
    }

    private static String parseThrows(Tokenizer tokenizer, TextMethodItem method)
            throws ApiParseException {
        String token = tokenizer.requireToken();
        boolean comma = true;
        while (true) {
            if (";".equals(token)) {
                return token;
            } else if (",".equals(token)) {
                if (comma) {
                    throw new ApiParseException("Expected exception, got ','", tokenizer.getLine());
                }
                comma = true;
            } else {
                if (!comma) {
                    throw new ApiParseException("Expected ',' or ';' got " + token, tokenizer.getLine());
                }
                comma = false;
                method.addException(token);
            }
            token = tokenizer.requireToken();
        }
    }

    private static String qualifiedName(String pkg, String className) {
        return pkg + "." + className;
    }

    private static boolean isIdent(String token) {
        return isident(token.charAt(0));
    }

    private static void assertIdent(Tokenizer tokenizer, String token) throws ApiParseException {
        if (!isident(token.charAt(0))) {
            throw new ApiParseException("Expected identifier: " + token, tokenizer.getLine());
        }
    }

    static class Tokenizer {
        char[] mBuf;
        String mFilename;
        int mPos;
        int mLine = 1;

        Tokenizer(String filename, char[] buf) {
            mFilename = filename;
            mBuf = buf;
        }

        public SourcePositionInfo pos() {
            return new SourcePositionInfo(mFilename, mLine, 0);
        }

        public int getLine() {
            return mLine;
        }

        boolean eatWhitespace() {
            boolean ate = false;
            while (mPos < mBuf.length && isspace(mBuf[mPos])) {
                if (mBuf[mPos] == '\n') {
                    mLine++;
                }
                mPos++;
                ate = true;
            }
            return ate;
        }

        boolean eatComment() {
            if (mPos + 1 < mBuf.length) {
                if (mBuf[mPos] == '/' && mBuf[mPos + 1] == '/') {
                    mPos += 2;
                    while (mPos < mBuf.length && !isnewline(mBuf[mPos])) {
                        mPos++;
                    }
                    return true;
                }
            }
            return false;
        }

        void eatWhitespaceAndComments() {
            while (eatWhitespace() || eatComment()) {
            }
        }

        public String requireToken() throws ApiParseException {
            return requireToken(true);
        }

        public String requireToken(boolean parenIsSep) throws ApiParseException {
            final String token = getToken(parenIsSep);
            if (token != null) {
                return token;
            } else {
                throw new ApiParseException("Unexpected end of file", mLine);
            }
        }

        public String getToken() throws ApiParseException {
            return getToken(true);
        }

        public int offset() {
            return mPos;
        }

        public String getStringFromOffset(int offset) {
            return new String(mBuf, offset, mPos - offset);
        }

        public String getToken(boolean parenIsSep) throws ApiParseException {
            eatWhitespaceAndComments();
            if (mPos >= mBuf.length) {
                return null;
            }
            final int line = mLine;
            final char c = mBuf[mPos];
            final int start = mPos;
            mPos++;
            if (c == '"') {
                final int STATE_BEGIN = 0;
                final int STATE_ESCAPE = 1;
                int state = STATE_BEGIN;
                while (true) {
                    if (mPos >= mBuf.length) {
                        throw new ApiParseException("Unexpected end of file for \" starting at " + line, mLine);
                    }
                    final char k = mBuf[mPos];
                    if (k == '\n' || k == '\r') {
                        throw new ApiParseException("Unexpected newline for \" starting at " + line, mLine);
                    }
                    mPos++;
                    switch (state) {
                        case STATE_BEGIN:
                            switch (k) {
                                case '\\':
                                    state = STATE_ESCAPE;
                                    mPos++;
                                    break;
                                case '"':
                                    return new String(mBuf, start, mPos - start);
                            }
                        case STATE_ESCAPE:
                            state = STATE_BEGIN;
                            break;
                    }
                }
            } else if (issep(c, parenIsSep)) {
                return "" + c;
            } else {
                int genericDepth = 0;
                do {
                    while (mPos < mBuf.length && !isspace(mBuf[mPos]) && !issep(mBuf[mPos], parenIsSep)) {
                        mPos++;
                    }
                    if (mPos < mBuf.length) {
                        if (mBuf[mPos] == '<') {
                            genericDepth++;
                            mPos++;
                        } else if (genericDepth != 0) {
                            if (mBuf[mPos] == '>') {
                                genericDepth--;
                            }
                            mPos++;
                        }
                    }
                } while (mPos < mBuf.length
                        && ((!isspace(mBuf[mPos]) && !issep(mBuf[mPos], parenIsSep)) || genericDepth != 0));
                if (mPos >= mBuf.length) {
                    throw new ApiParseException("Unexpected end of file for \" starting at " + line, mLine);
                }
                return new String(mBuf, start, mPos - start);
            }
        }
    }

    static boolean isspace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    static boolean isnewline(char c) {
        return c == '\n' || c == '\r';
    }

    static boolean issep(char c, boolean parenIsSep) {
        if (parenIsSep) {
            if (c == '(' || c == ')') {
                return true;
            }
        }
        return c == '{' || c == '}' || c == ',' || c == ';' || c == '<' || c == '>';
    }

    private static boolean isident(char c) {
        if (c == '"' || issep(c, true)) {
            return false;
        }
        return true;
    }
}
