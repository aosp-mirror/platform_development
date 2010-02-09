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
import java.util.Iterator;

/**
 * Container representing a package of classes and interfaces.
 */
public class PackageInfo {
    private String mName;
    private HashMap<String,ClassInfo> mClassList;

    public PackageInfo(String name) {
        mName = name;
        mClassList = new HashMap<String,ClassInfo>();
    }

    public String getName() {
        return mName;
    }

    /**
     * Retrieves the named class.
     *
     * @return the package, or null if no match was found
     */
    public ClassInfo getClass(String name) {
        return mClassList.get(name);
    }

    /**
     * Retrieves the named class, creating it if it doesn't already
     * exist.
     *
     * @param className Binary or non-binary class name without the
     *      package name, e.g. "AlertDialog.Builder".
     * @param superclassName Fully-qualified binary or non-binary superclass
     *      name (e.g. "java.lang.Enum").
     * @param isStatic Class static attribute, may be "true", "false", or null.
     */
    public ClassInfo getOrCreateClass(String className, String superclassName,
            String isStatic) {
        String fixedName = TypeUtils.simpleClassNameToBinary(className);
        ClassInfo classInfo = mClassList.get(fixedName);
        if (classInfo == null) {
            //System.out.println("--- creating entry for class " + fixedName +
            //    " (super=" + superclassName + ")");
            classInfo = new ClassInfo(fixedName, superclassName, isStatic);
            mClassList.put(fixedName, classInfo);
        } else {
            //System.out.println("--- returning existing class " + name);
        }
        return classInfo;
    }

    /**
     * Returns an iterator for the set of classes in this package.
     */
    public Iterator<ClassInfo> getClassIterator() {
        return mClassList.values().iterator();
    }
}

