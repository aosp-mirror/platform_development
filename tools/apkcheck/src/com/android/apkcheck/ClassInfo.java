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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Container representing a class or interface with fields and methods.
 */
public class ClassInfo {
    private String mName;
    // methods are hashed on name:descriptor
    private HashMap<String,MethodInfo> mMethodList;
    // fields are hashed on name:type
    private HashMap<String,FieldInfo> mFieldList;

    private String mSuperclassName;

    // is this a static inner class?
    private String mIsStatic;

    // holds the name of the superclass and all declared interfaces
    private ArrayList<String> mSuperNames;

    // is this an enumerated type?
    private boolean mIsEnum;
    // is this an annotation type?
    private boolean mIsAnnotation;

    private boolean mFlattening = false;
    private boolean mFlattened = false;

    /**
     * Constructs a new ClassInfo with the provided class name.
     *
     * @param className Binary class name without the package name,
     *      e.g. "AlertDialog$Builder".
     * @param superclassName Fully-qualified binary or non-binary superclass
     *      name (e.g. "java.lang.Enum").
     * @param isStatic Class static attribute, may be "true", "false", or null.
     */
    public ClassInfo(String className, String superclassName, String isStatic) {
        mName = className;
        mMethodList = new HashMap<String,MethodInfo>();
        mFieldList = new HashMap<String,FieldInfo>();
        mSuperNames = new ArrayList<String>();
        mIsStatic = isStatic;

        /*
         * Record the superclass name, and add it to the interface list
         * since we'll need to do the same "flattening" work on it.
         *
         * Interfaces and java.lang.Object have a null value.
         */
        if (superclassName != null) {
            mSuperclassName = superclassName;
            mSuperNames.add(superclassName);
        }
    }

    /**
     * Returns the name of the class.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the name of the superclass.
     */
    public String getSuperclassName() {
        return mSuperclassName;
    }

    /**
     * Returns the "static" attribute.
     *
     * This is actually tri-state:
     *   "true" means it is static
     *   "false" means it's not static
     *   null means it's unknown
     *
     * The "unknown" state is associated with the APK input, while the
     * known states are from the public API definition.
     *
     * This relates to the handling of the "secret" first parameter to
     * constructors of non-static inner classes.
     */
    public String getStatic() {
        return mIsStatic;
    }

    /**
     * Returns whether or not this class is an enumerated type.
     */
    public boolean isEnum() {
        assert mFlattened;
        return mIsEnum;
    }

    /**
     * Returns whether or not this class is an annotation type.
     */
    public boolean isAnnotation() {
        assert mFlattened;
        return mIsAnnotation;
    }

    /**
     * Adds a field to the list.
     */
    public void addField(FieldInfo fieldInfo) {
        mFieldList.put(fieldInfo.getNameAndType(), fieldInfo);
    }

    /**
     * Retrives a field from the list.
     *
     * @param nameAndType fieldName:type
     */
    public FieldInfo getField(String nameAndType) {
        return mFieldList.get(nameAndType);
    }

    /**
     * Returns an iterator over all known fields.
     */
    public Iterator<FieldInfo> getFieldIterator() {
        return mFieldList.values().iterator();
    }

    /**
     * Adds a method to the list.
     */
    public void addMethod(MethodInfo methInfo) {
        mMethodList.put(methInfo.getNameAndDescriptor(), methInfo);
    }

    /**
     * Returns an iterator over all known methods.
     */
    public Iterator<MethodInfo> getMethodIterator() {
        return mMethodList.values().iterator();
    }

    /**
     * Retrieves a method from the list.
     *
     * @param nameAndDescr methodName:descriptor
     */
    public MethodInfo getMethod(String nameAndDescr) {
        return mMethodList.get(nameAndDescr);
    }

    /**
     * Retrieves a method from the list, matching on the part of the key
     * before the return type.
     *
     * The API file doesn't include an entry for a method that overrides
     * a method in the superclass.  Ordinarily this is a good thing, but
     * if the override uses a covariant return type then the reference
     * to it in the APK won't match.
     *
     * @param nameAndDescr methodName:descriptor
     */
    public MethodInfo getMethodIgnoringReturn(String nameAndDescr) {
        String shortKey = nameAndDescr.substring(0, nameAndDescr.indexOf(')')+1);

        Iterator<MethodInfo> iter = getMethodIterator();
        while (iter.hasNext()) {
            MethodInfo methInfo = iter.next();
            String nad = methInfo.getNameAndDescriptor();
            if (nad.startsWith(shortKey))
                return methInfo;
        }

        return null;
    }

    /**
     * Returns true if the method and field lists are empty.
     */
    public boolean hasNoFieldMethod() {
        return mMethodList.size() == 0 && mFieldList.size() == 0;
    }

    /**
     * Adds an interface to the list of classes implemented by this class.
     */
    public void addInterface(String interfaceName) {
        mSuperNames.add(interfaceName);
    }

    /**
     * Flattens a class.  This involves copying all methods and fields
     * declared by the superclass and interfaces (and, recursively, their
     * superclasses and interfaces) into the local structure.
     *
     * The public API file must be fully parsed before calling here.
     *
     * This also detects if we're an Enum or Annotation.
     */
    public void flattenClass(ApiList apiList) {
        if (mFlattened)
            return;

        /*
         * Recursive class definitions aren't allowed in Java code, but
         * there could be one in the API definition file.
         */
        if (mFlattening) {
            throw new RuntimeException("Recursive invoke; current class is "
                + mName);
        }
        mFlattening = true;

        /*
         * Normalize the ambiguous types.  This requires regenerating the
         * field and method lists, because the signature is used as the
         * hash table key.
         */
        normalizeTypes(apiList);

        /*
         * Figure out if this class is an enumerated type.
         */
        mIsEnum = "java.lang.Enum".equals(mSuperclassName);

        /*
         * Figure out if this class is an annotation type.  We expect it
         * to extend Object, implement java.lang.annotation.Annotation,
         * and declare no fields or methods.  (If the API XML file is
         * fixed, it will declare methods; but at that point having special
         * handling for annotations will be unnecessary.)
         */
        if ("java.lang.Object".equals(mSuperclassName) &&
            mSuperNames.contains("java.lang.annotation.Annotation") &&
            hasNoFieldMethod())
        {
            mIsAnnotation = true;
        }

        /*
         * Flatten our superclass and interfaces.
         */
        for (int i = 0; i < mSuperNames.size(); i++) {
            /*
             * The contents of mSuperNames are in an ambiguous form.
             * Normalize it to binary form before working with it.
             */
            String interfaceName = TypeUtils.ambiguousToBinaryName(mSuperNames.get(i),
                    apiList);
            ClassInfo classInfo = lookupClass(interfaceName, apiList);
            if (classInfo == null) {
                ApkCheck.apkWarning("Class " + interfaceName +
                    " not found (super of " + mName + ")");
                continue;
            }

            /* flatten it */
            classInfo.flattenClass(apiList);

            /* copy everything from it in here */
            mergeFrom(classInfo);
        }

        mFlattened = true;
    }

    /**
     * Normalizes the type names used in field and method descriptors.
     *
     * We call the field/method normalization function, which updates how
     * it thinks of itself (and may be called multiple times from different
     * classes).  We then have to re-add it to the hash map because the
     * key may have changed.  (We're using an iterator, so we create a
     * new hashmap and replace the old.)
     */
    private void normalizeTypes(ApiList apiList) {
        Iterator<String> keyIter;

        HashMap<String,FieldInfo> tmpFieldList = new HashMap<String,FieldInfo>();
        keyIter = mFieldList.keySet().iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            FieldInfo fieldInfo = mFieldList.get(key);
            fieldInfo.normalizeType(apiList);
            tmpFieldList.put(fieldInfo.getNameAndType(), fieldInfo);
        }
        mFieldList = tmpFieldList;

        HashMap<String,MethodInfo> tmpMethodList = new HashMap<String,MethodInfo>();
        keyIter = mMethodList.keySet().iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            MethodInfo methodInfo = mMethodList.get(key);
            methodInfo.normalizeTypes(apiList);
            tmpMethodList.put(methodInfo.getNameAndDescriptor(), methodInfo);
        }
        mMethodList = tmpMethodList;
    }

    /**
     * Merges the fields and methods from "otherClass" into this class.
     *
     * Redundant entries will be merged.  We don't specify who the winner
     * will be.
     */
    private void mergeFrom(ClassInfo otherClass) {
        /*System.out.println("merging into " + getName() + ": fields=" +
            mFieldList.size() + "/" + otherClass.mFieldList.size() +
            ", methods=" +
            mMethodList.size() + "/" + otherClass.mMethodList.size());*/

        mFieldList.putAll(otherClass.mFieldList);
        mMethodList.putAll(otherClass.mMethodList);

        /*System.out.println("  now fields=" + mFieldList.size() +
            ", methods=" + mMethodList.size());*/
    }


    /**
     * Finds the named class in the ApiList.
     *
     * @param className Fully-qualified dot notation (e.g. "java.lang.String")
     * @param apiList The hierarchy to search in.
     * @return The class or null if not found.
     */
    private static ClassInfo lookupClass(String fullname, ApiList apiList) {
        String packageName = TypeUtils.packageNameOnly(fullname);
        String className = TypeUtils.classNameOnly(fullname);

        PackageInfo pkg = apiList.getPackage(packageName);
        if (pkg == null)
            return null;
        return pkg.getClass(className);
    }
}

