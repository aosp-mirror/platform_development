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
package com.android.tools.metalava.apilevels;

import com.google.common.collect.Iterables;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a class or an interface and its methods/fields.
 * This is used to write the simplified XML file containing all the public API.
 */
public class ApiClass extends ApiElement {
    private final List<ApiElement> mSuperClasses = new ArrayList<ApiElement>();
    private final List<ApiElement> mInterfaces = new ArrayList<ApiElement>();

    private final Map<String, ApiElement> mFields = new HashMap<String, ApiElement>();
    private final Map<String, ApiElement> mMethods = new HashMap<String, ApiElement>();

    public ApiClass(String name, int version, boolean deprecated) {
        super(name, version, deprecated);
    }

    public void addField(String name, int version, boolean deprecated) {
        addToMap(mFields, name, version, deprecated);
    }

    public void addMethod(String name, int version, boolean deprecated) {
        addToMap(mMethods, name, version, deprecated);
    }

    public void addSuperClass(String superClass, int since) {
        addToArray(mSuperClasses, superClass, since);
    }

    public void addInterface(String interfaceClass, int since) {
        addToArray(mInterfaces, interfaceClass, since);
    }

    private void addToMap(Map<String, ApiElement> elements, String name, int version, boolean deprecated) {
        ApiElement element = elements.get(name);
        if (element == null) {
            element = new ApiElement(name, version, deprecated);
            elements.put(name, element);
        } else {
            element.update(version, deprecated);
        }
    }

    private void addToArray(Collection<ApiElement> elements, String name, int version) {
        ApiElement element = findByName(elements, name);
        if (element == null) {
            element = new ApiElement(name, version);
            elements.add(element);
        } else {
            element.update(version);
        }
    }

    private ApiElement findByName(Collection<ApiElement> collection, String name) {
        for (ApiElement element : collection) {
            if (element.getName().equals(name)) {
                return element;
            }
        }
        return null;
    }

    @Override
    public void print(String tag, ApiElement parentElement, String indent, PrintStream stream) {
        super.print(tag, false, parentElement, indent, stream);
        String innerIndent = indent + '\t';
        print(mSuperClasses, "extends", innerIndent, stream);
        print(mInterfaces, "implements", innerIndent, stream);
        print(mMethods.values(), "method", innerIndent, stream);
        print(mFields.values(), "field", innerIndent, stream);
        printClosingTag(tag, indent, stream);
    }

    /**
     * Removes all interfaces that are also implemented by superclasses or extended by interfaces
     * this class implements.
     *
     * @param allClasses all classes keyed by their names.
     */
    public void removeImplicitInterfaces(Map<String, ApiClass> allClasses) {
        if (mInterfaces.isEmpty() || mSuperClasses.isEmpty()) {
            return;
        }

        for (Iterator<ApiElement> iterator = mInterfaces.iterator(); iterator.hasNext(); ) {
            ApiElement interfaceElement = iterator.next();

            for (ApiElement superClass : mSuperClasses) {
                if (superClass.introducedNotLaterThan(interfaceElement)) {
                    ApiClass cls = allClasses.get(superClass.getName());
                    if (cls != null && cls.implementsInterface(interfaceElement, allClasses)) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    private boolean implementsInterface(ApiElement interfaceElement, Map<String, ApiClass> allClasses) {
        for (ApiElement localInterface : mInterfaces) {
            if (localInterface.introducedNotLaterThan(interfaceElement)) {
                if (interfaceElement.getName().equals(localInterface.getName())) {
                    return true;
                }
                // Check parent interface.
                ApiClass cls = allClasses.get(localInterface.getName());
                if (cls != null && cls.implementsInterface(interfaceElement, allClasses)) {
                    return true;
                }
            }
        }

        for (ApiElement superClass : mSuperClasses) {
            if (superClass.introducedNotLaterThan(interfaceElement)) {
                ApiClass cls = allClasses.get(superClass.getName());
                if (cls != null && cls.implementsInterface(interfaceElement, allClasses)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes all methods that override method declared by superclasses and interfaces of this class.
     *
     * @param allClasses all classes keyed by their names.
     */
    public void removeOverridingMethods(Map<String, ApiClass> allClasses) {
        for (Iterator<Map.Entry<String, ApiElement>> iter = mMethods.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, ApiElement> entry = iter.next();
            ApiElement method = entry.getValue();
            if (!method.getName().startsWith("<init>(") && isOverrideOfInherited(method, allClasses)) {
                iter.remove();
            }
        }
    }

    /**
     * Checks if the given method overrides one of the methods defined by this class or
     * its superclasses or interfaces.
     *
     * @param method     the method to check
     * @param allClasses the map containing all API classes
     * @return true if the method is an override
     */
    private boolean isOverride(ApiElement method, Map<String, ApiClass> allClasses) {
        ApiElement localMethod = mMethods.get(method.getName());
        if (localMethod != null && localMethod.introducedNotLaterThan(method)) {
            // This class has the method and it was introduced in at the same api level
            // as the child method, or before.
            return true;
        }
        return isOverrideOfInherited(method, allClasses);
    }

    /**
     * Checks if the given method overrides one of the methods declared by ancestors of this class.
     */
    private boolean isOverrideOfInherited(ApiElement method, Map<String, ApiClass> allClasses) {
        // Check this class' parents.
        for (ApiElement parent : Iterables.concat(mSuperClasses, mInterfaces)) {
            // Only check the parent if it was a parent class at the introduction of the method.
            if (parent.introducedNotLaterThan(method)) {
                ApiClass cls = allClasses.get(parent.getName());
                if (cls != null && cls.isOverride(method, allClasses)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return getName();
    }
}
