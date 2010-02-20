/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apkcheck;

import java.util.HashMap;

public class TypeUtils {
    private void TypeUtils() {}

    /*
     * Conversions for the primitive types, as well as a few things
     * that show up a lot so we can avoid the string manipulation.
     */
    private static final HashMap<String,String> sQuickConvert;
    static {
        sQuickConvert = new HashMap<String,String>();

        sQuickConvert.put("boolean", "Z");
        sQuickConvert.put("byte", "B");
        sQuickConvert.put("char", "C");
        sQuickConvert.put("short", "S");
        sQuickConvert.put("int", "I");
        sQuickConvert.put("float", "F");
        sQuickConvert.put("long", "J");
        sQuickConvert.put("double", "D");
        sQuickConvert.put("void", "V");
        sQuickConvert.put("java.lang.Object", "Ljava/lang/Object;");
        sQuickConvert.put("java.lang.String", "Ljava/lang/String;");
        sQuickConvert.put("java.util.ArrayList", "Ljava/util/ArrayList;");
        sQuickConvert.put("java.util.HashMap", "Ljava/util/HashMap;");
    };

    /*
     * Convert a human-centric type into something suitable for a method
     * signature.  Examples:
     *
     *   int --> I
     *   float[] --> [F
     *   java.lang.String --> Ljava/lang/String;
     */
    public static String typeToDescriptor(String type) {
        String quick = sQuickConvert.get(type);
        if (quick != null)
            return quick;

        int arrayDepth = 0;
        int firstPosn = -1;
        int posn = -1;
        while ((posn = type.indexOf('[', posn+1)) != -1) {
            if (firstPosn == -1)
                firstPosn = posn;
            arrayDepth++;
        }

        /* if we found an array, strip the brackets off */
        if (firstPosn != -1)
            type = type.substring(0, firstPosn);

        StringBuilder builder = new StringBuilder();
        while (arrayDepth-- > 0)
            builder.append("[");

        /* retry quick convert */
        quick = sQuickConvert.get(type);
        if (quick != null) {
            builder.append(quick);
        } else {
            builder.append("L");
            builder.append(type.replace('.', '/'));
            builder.append(";");
        }

        return builder.toString();
    }

    /**
     * Converts a "simple" class name into a "binary" class name.  For
     * example:
     *
     *   SharedPreferences.Editor --&gt; SharedPreferences$Editor
     *
     * Do not use this on fully-qualified class names.
     */
    public static String simpleClassNameToBinary(String className) {
        return className.replace('.', '$');
    }

    /**
     * Returns the class name portion of a fully-qualified binary class name.
     */
    static String classNameOnly(String typeName) {
        int start = typeName.lastIndexOf(".");
        if (start < 0) {
            return typeName;
        } else {
            return typeName.substring(start+1);
        }
    }

    /**
     * Returns the package portion of a fully-qualified binary class name.
     */
    static String packageNameOnly(String typeName) {
        int end = typeName.lastIndexOf(".");
        if (end < 0) {
            /* lives in default package */
            return "";
        } else {
            return typeName.substring(0, end);
        }
    }


    /**
     * Normalizes a full class name to binary form.
     *
     * For example, "android.view.View.OnClickListener" could be in
     * the "android.view" package or the "android.view.View" package.
     * Checking capitalization is unreliable.  We do have a full list
     * of package names from the file though, so there's an excellent
     * chance that we can identify the package that way.  (Of course, we
     * can only do this after we have finished parsing the file.)
     *
     * If the name has two or more dots, we need to compare successively
     * shorter strings until we find a match in the package list.
     *
     * Do not call this on previously-returned output, as that may
     * confuse the code that deals with generic signatures.
     */
    public static String ambiguousToBinaryName(String typeName, ApiList apiList) {
        /*
         * In some cases this can be a generic signature:
         *   <parameter name="collection" type="java.util.Collection&lt;? extends E&gt;">
         *   <parameter name="interfaces" type="java.lang.Class&lt;?&gt;[]">
         *   <parameter name="object" type="E">
         *
         * If we see a '<', strip out everything from it to the '>'.  That
         * does pretty much the right thing, though we have to deal with
         * nested stuff like "<? extends Map<String>>".
         *
         * Handling the third item is ugly.  If the string is a single
         * character, change it to java.lang.Object.  This is generally
         * insufficient and also ambiguous with respect to classes in the
         * default package, but we don't have much choice here, and it gets
         * us through the standard collection classes.  Note this is risky
         * if somebody tries to normalize a string twice, since we could be
         * "promoting" a primitive type.
         */
        typeName = stripAngleBrackets(typeName);
        if (typeName.length() == 1) {
            //System.out.println("converting X to Object: " + typeName);
            typeName = "java.lang.Object";
        } else if (typeName.length() == 3 &&
                   typeName.substring(1, 3).equals("[]")) {
            //System.out.println("converting X[] to Object[]: " + typeName);
            typeName = "java.lang.Object[]";
        } else if (typeName.length() == 4 &&
                   typeName.substring(1, 4).equals("...")) {
            //System.out.println("converting X... to Object[]: " + typeName);
            typeName = "java.lang.Object[]";
        }

        /*
         * Catch-all for varargs, which come in different varieties:
         *  java.lang.Object...
         *  java.lang.Class...
         *  java.lang.CharSequence...
         *  int...
         *  Progress...
         *
         * The latter is a generic type that we didn't catch above because
         * it's not using a single-character descriptor.
         *
         * The method reference for "java.lang.Class..." will be looking
         * for java.lang.Class[], not java.lang.Object[], so we don't want
         * to do a blanket conversion.  Similarly, "int..." turns into int[].
         *
         * There's not much we can do with "Progress...", unless we want
         * to write off the default package and filter out primitive types.
         * Probably easier to fix it up elsewhere.
         */
        int ellipsisIndex = typeName.indexOf("...");
        if (ellipsisIndex >= 0) {
            String newTypeName = typeName.substring(0, ellipsisIndex) + "[]";
            //System.out.println("vararg " + typeName + " --> " + newTypeName);
            typeName = newTypeName;
        }

        /*
         * It's possible the code that generates API definition files
         * has been fixed.  If we see a '$', just return the original.
         */
        if (typeName.indexOf('$') >= 0)
            return typeName;

        int lastDot = typeName.lastIndexOf('.');
        if (lastDot < 0)
            return typeName;

        /*
         * What we have looks like some variation of these:
         *   package.Class
         *   Class.InnerClass
         *   long.package.name.Class
         *   long.package.name.Class.InnerClass
         *
         * We cut it off at the last '.' and test to see if it's a known
         * package name.  If not, keep moving left until we run out of dots.
         */
        int nextDot = lastDot;
        while (nextDot >= 0) {
            String testName = typeName.substring(0, nextDot);
            if (apiList.getPackage(testName) != null) {
                break;
            }

            nextDot = typeName.lastIndexOf('.', nextDot-1);
        }

        if (nextDot < 0) {
            /* no package name found, convert all dots */
            System.out.println("+++ no pkg name found on " + typeName + typeName.length());
            typeName = typeName.replace('.', '$');
        } else if (nextDot == lastDot) {
            /* class name is last element; original string is fine */
        } else {
            /* in the middle; zap the dots in the inner class name */
            String oldClassName = typeName;
            typeName = typeName.substring(0, nextDot+1) +
                typeName.substring(nextDot+1).replace('.', '$');
            //System.out.println("+++ " + oldClassName + " --> " + typeName);
        }

        return typeName;
    }

    /**
     * Strips out everything between '<' and '>'.  This will handle
     * nested brackets, but we're not expecting to see multiple instances
     * in series (i.e. "&lt;foo&lt;bar&gt;&gt;" is expected, but
     * "&lt;foo&gt;STUFF&lt;bar&gt; is not).
     *
     * @return the stripped string
     */
    private static String stripAngleBrackets(String str) {
        /*
         * Since we only expect to see one "run", we can just find the
         * first '<' and the last '>'.  Ideally we'd verify that they're
         * not mismatched, but we're assuming the input file is generally
         * correct.
         */
        int ltIndex = str.indexOf('<');
        if (ltIndex < 0)
            return str;

        int gtIndex = str.lastIndexOf('>');
        if (gtIndex < 0) {
            System.err.println("ERROR: found '<' without '>': " + str);
            return str;     // not much we can do
        }

        return str.substring(0, ltIndex) + str.substring(gtIndex+1);
    }
}

