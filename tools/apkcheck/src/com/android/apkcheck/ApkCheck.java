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

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Checks an APK's dependencies against the published API specification.
 *
 * We need to read two XML files (spec and APK) and perform some operations
 * on the elements.  The file formats are similar but not identical, so
 * we distill it down to common elements.
 *
 * We may also want to read some additional API lists representing
 * libraries that would be included with a "uses-library" directive.
 *
 * For performance we want to allow processing of multiple APKs so
 * we don't have to re-parse the spec file each time.
 */
public class ApkCheck {
    /* keep track of current APK file name, for error messages */
    private static ApiList sCurrentApk;

    /* show warnings? */
    private static boolean sShowWarnings = false;
    /* show errors? */
    private static boolean sShowErrors = true;

    /* names of packages we're allowed to ignore */
    private static HashSet<String> sIgnorablePackages = new HashSet<String>();


    /**
     * Program entry point.
     */
    public static void main(String[] args) {
        ApiList apiDescr = new ApiList("public-api");

        if (args.length < 2) {
            usage();
            return;
        }

        /* process args */
        int idx;
        for (idx = 0; idx < args.length; idx++) {
            if (args[idx].equals("--help")) {
                usage();
                return;
            } else if (args[idx].startsWith("--uses-library=")) {
                String libName = args[idx].substring(args[idx].indexOf('=')+1);
                if ("BUILTIN".equals(libName)) {
                    Reader reader = Builtin.getReader();
                    if (!parseXml(apiDescr, reader, "BUILTIN"))
                        return;
                } else {
                    if (!parseApiDescr(apiDescr, libName))
                        return;
                }
            } else if (args[idx].startsWith("--ignore-package=")) {
                String pkgName = args[idx].substring(args[idx].indexOf('=')+1);
                sIgnorablePackages.add(pkgName);
            } else if (args[idx].equals("--warn")) {
                sShowWarnings = true;
            } else if (args[idx].equals("--no-warn")) {
                sShowWarnings = false;
            } else if (args[idx].equals("--error")) {
                sShowErrors = true;
            } else if (args[idx].equals("--no-error")) {
                sShowErrors = false;

            } else if (args[idx].startsWith("--")) {
                if (args[idx].equals("--")) {
                    // remainder are filenames, even if they start with "--"
                    idx++;
                    break;
                } else {
                    // unknown option specified
                    System.err.println("ERROR: unknown option " +
                        args[idx] + " (use \"--help\" for usage info)");
                    return;
                }
            } else {
                break;
            }
        }
        if (idx > args.length - 2) {
            usage();
            return;
        }

        /* parse base API description */
        if (!parseApiDescr(apiDescr, args[idx++]))
            return;

        /* "flatten" superclasses and interfaces */
        sCurrentApk = apiDescr;
        flattenInherited(apiDescr);

        /* walk through list of libs we want to scan */
        for ( ; idx < args.length; idx++) {
            ApiList apkDescr = new ApiList(args[idx]);
            sCurrentApk = apkDescr;
            boolean success = parseApiDescr(apkDescr, args[idx]);
            if (!success) {
                if (idx < args.length-1)
                    System.err.println("Skipping...");
                continue;
            }

            check(apiDescr, apkDescr);
            System.out.println(args[idx] + ": summary: " +
                apkDescr.getErrorCount() + " errors, " +
                apkDescr.getWarningCount() + " warnings\n");
        }
    }

    /**
     * Prints usage statement.
     */
    static void usage() {
        System.err.println("Android APK checker v1.0");
        System.err.println("Copyright (C) 2010 The Android Open Source Project\n");
        System.err.println("Usage: apkcheck [options] public-api.xml apk1.xml ...\n");
        System.err.println("Options:");
        System.err.println("  --help                  show this message");
        System.err.println("  --uses-library=lib.xml  load additional public API list");
        System.err.println("  --ignore-package=pkg    don't show errors for references to this package");
        System.err.println("  --[no-]warn             enable or disable display of warnings");
        System.err.println("  --[no-]error            enable or disable display of errors");
    }

    /**
     * Opens the file and passes it to parseXml.
     *
     * TODO: allow '-' as an alias for stdin?
     */
    static boolean parseApiDescr(ApiList apiList, String fileName) {
        boolean result = false;

        try {
            FileReader fileReader = new FileReader(fileName);
            result = parseXml(apiList, fileReader, fileName);
            fileReader.close();
        } catch (IOException ioe) {
            System.err.println("Error opening " + fileName);
        }
        return result;
    }

    /**
     * Parses an XML file holding an API description.
     *
     * @param fileReader Data source.
     * @param apiList Container to add stuff to.
     * @param fileName Input file name, only used for debug messages.
     */
    static boolean parseXml(ApiList apiList, Reader reader,
            String fileName) {
        //System.out.println("--- parsing " + fileName);
        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            ApiDescrHandler handler = new ApiDescrHandler(apiList);
            xmlReader.setContentHandler(handler);
            xmlReader.setErrorHandler(handler);
            xmlReader.parse(new InputSource(reader));

            //System.out.println("--- parsing complete");
            //dumpApi(apiList);
            return true;
        } catch (SAXParseException ex) {
            System.err.println("Error parsing " + fileName + " line " +
                ex.getLineNumber() + ": " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("Error while reading " + fileName + ": " +
                ex.getMessage());
            ex.printStackTrace();
        }

        // failed
        return false;
    }

    /**
     * Expands lists of fields and methods to recursively include superclass
     * and interface entries.
     *
     * The API description files have entries for every method a class
     * declares, even if it's present in the superclass (e.g. toString()).
     * Removal of one of these methods doesn't constitute an API change,
     * though, so if we don't find a method in a class we need to hunt
     * through its superclasses.
     *
     * We can walk up the hierarchy while analyzing the target APK,
     * or we can "flatten" the methods declared by the superclasses and
     * interfaces before we begin the analysis.  Expanding up front can be
     * beneficial if we're analyzing lots of APKs in one go, but detrimental
     * to startup time if we just want to look at one small APK.
     *
     * It also means filling the field/method hash tables with lots of
     * entries that never get used, possibly worsening the hash table
     * hit rate.
     *
     * We only need to do this for the public API list.  The dexdeps output
     * doesn't have this sort of information anyway.
     */
    static void flattenInherited(ApiList pubList) {
        Iterator<PackageInfo> pkgIter = pubList.getPackageIterator();
        while (pkgIter.hasNext()) {
            PackageInfo pubPkgInfo = pkgIter.next();

            Iterator<ClassInfo> classIter = pubPkgInfo.getClassIterator();
            while (classIter.hasNext()) {
                ClassInfo pubClassInfo = classIter.next();

                pubClassInfo.flattenClass(pubList);
            }
        }
    }

    /**
     * Checks the APK against the public API.
     *
     * Run through and find the mismatches.
     *
     * @return true if all is well
     */
    static boolean check(ApiList pubList, ApiList apkDescr) {

        Iterator<PackageInfo> pkgIter = apkDescr.getPackageIterator();
        while (pkgIter.hasNext()) {
            PackageInfo apkPkgInfo = pkgIter.next();
            PackageInfo pubPkgInfo = pubList.getPackage(apkPkgInfo.getName());
            boolean badPackage = false;

            if (pubPkgInfo == null) {
                // "illegal package" not a tremendously useful message
                //apkError("Illegal package ref: " + apkPkgInfo.getName());
                badPackage = true;
            }

            Iterator<ClassInfo> classIter = apkPkgInfo.getClassIterator();
            while (classIter.hasNext()) {
                ClassInfo apkClassInfo = classIter.next();

                if (badPackage) {
                    /*
                     * The package is not present in the public API file,
                     * but simply saying "bad package" isn't all that
                     * useful, so we emit the names of each of the classes.
                     */
                    if (isIgnorable(apkPkgInfo)) {
                        apkWarning("Ignoring class ref: " +
                            apkPkgInfo.getName() + "." + apkClassInfo.getName());
                    } else {
                        apkError("Illegal class ref: " +
                            apkPkgInfo.getName() + "." + apkClassInfo.getName());
                    }
                } else {
                    checkClass(pubPkgInfo, apkClassInfo);
                }
            }
        }

        return true;
    }

    /**
     * Checks the class against the public API.  We check the class
     * itself and then any fields and methods.
     */
    static boolean checkClass(PackageInfo pubPkgInfo, ClassInfo classInfo) {

        ClassInfo pubClassInfo = pubPkgInfo.getClass(classInfo.getName());

        if (pubClassInfo == null) {
            if (isIgnorable(pubPkgInfo)) {
                apkWarning("Ignoring class ref: " +
                    pubPkgInfo.getName() + "." + classInfo.getName());
            } else if (classInfo.hasNoFieldMethod()) {
                apkWarning("Hidden class referenced: " +
                    pubPkgInfo.getName() + "." + classInfo.getName());
            } else {
                apkError("Illegal class ref: " +
                    pubPkgInfo.getName() + "." + classInfo.getName());
                // could list specific fields/methods used
            }
            return false;
        }

        /*
         * Check the contents of classInfo against pubClassInfo.
         */
        Iterator<FieldInfo> fieldIter = classInfo.getFieldIterator();
        while (fieldIter.hasNext()) {
            FieldInfo apkFieldInfo = fieldIter.next();
            String nameAndType = apkFieldInfo.getNameAndType();
            FieldInfo pubFieldInfo = pubClassInfo.getField(nameAndType);
            if (pubFieldInfo == null) {
                if (pubClassInfo.isEnum()) {
                    apkWarning("Enum field ref: " + pubPkgInfo.getName() +
                        "." + classInfo.getName() + "." + nameAndType);
                } else {
                    apkError("Illegal field ref: " + pubPkgInfo.getName() +
                        "." + classInfo.getName() + "." + nameAndType);
                }
            }
        }

        Iterator<MethodInfo> methodIter = classInfo.getMethodIterator();
        while (methodIter.hasNext()) {
            MethodInfo apkMethodInfo = methodIter.next();
            String nameAndDescr = apkMethodInfo.getNameAndDescriptor();
            MethodInfo pubMethodInfo = pubClassInfo.getMethod(nameAndDescr);
            if (pubMethodInfo == null) {
                pubMethodInfo = pubClassInfo.getMethodIgnoringReturn(nameAndDescr);
                if (pubMethodInfo == null) {
                    if (pubClassInfo.isAnnotation()) {
                        apkWarning("Annotation method ref: " +
                            pubPkgInfo.getName() + "." + classInfo.getName() +
                            "." + nameAndDescr);
                    } else {
                        apkError("Illegal method ref: " + pubPkgInfo.getName() +
                            "." + classInfo.getName() + "." + nameAndDescr);
                    }
                } else {
                    apkWarning("Possibly covariant method ref: " +
                        pubPkgInfo.getName() + "." + classInfo.getName() +
                        "." + nameAndDescr);
                }
            }
        }


        return true;
    }

    /**
     * Returns true if the package is in the "ignored" list.
     */
    static boolean isIgnorable(PackageInfo pkgInfo) {
        return sIgnorablePackages.contains(pkgInfo.getName());
    }

    /**
     * Prints a warning message about an APK problem.
     */
    public static void apkWarning(String msg) {
        if (sShowWarnings) {
            System.out.println("(warn) " + sCurrentApk.getDebugString() +
                ": " + msg);
        }
        sCurrentApk.incrWarnings();
    }

    /**
     * Prints an error message about an APK problem.
     */
    public static void apkError(String msg) {
        if (sShowErrors) {
            System.out.println(sCurrentApk.getDebugString() + ": " + msg);
        }
        sCurrentApk.incrErrors();
    }

    /**
     * Recursively dumps the contents of the API.  Sort order is not
     * specified.
     */
    private static void dumpApi(ApiList apiList) {
        Iterator<PackageInfo> iter = apiList.getPackageIterator();
        while (iter.hasNext()) {
            PackageInfo pkgInfo = iter.next();
            dumpPackage(pkgInfo);
        }
    }

    private static void dumpPackage(PackageInfo pkgInfo) {
        Iterator<ClassInfo> iter = pkgInfo.getClassIterator();
        System.out.println("PACKAGE " + pkgInfo.getName());
        while (iter.hasNext()) {
            ClassInfo classInfo = iter.next();
            dumpClass(classInfo);
        }
    }

    private static void dumpClass(ClassInfo classInfo) {
        System.out.println(" CLASS " + classInfo.getName());
        Iterator<FieldInfo> fieldIter = classInfo.getFieldIterator();
        while (fieldIter.hasNext()) {
            FieldInfo fieldInfo = fieldIter.next();
            dumpField(fieldInfo);
        }
        Iterator<MethodInfo> methIter = classInfo.getMethodIterator();
        while (methIter.hasNext()) {
            MethodInfo methInfo = methIter.next();
            dumpMethod(methInfo);
        }
    }

    private static void dumpMethod(MethodInfo methInfo) {
        System.out.println("  METHOD " + methInfo.getNameAndDescriptor());
    }

    private static void dumpField(FieldInfo fieldInfo) {
        System.out.println("  FIELD " + fieldInfo.getNameAndType());
    }
}

