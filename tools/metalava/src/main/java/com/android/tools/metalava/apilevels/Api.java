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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the whole Android API.
 */
public class Api extends ApiElement {
    private final Map<String, ApiClass> mClasses = new HashMap<String, ApiClass>();

    public Api() {
        // Pretend that API started from version 0 to make sure that classes existed in the first version
        // are printed with since="1".
        super("Android API");
    }

    /**
     * Prints the whole API definition to a stream.
     *
     * @param stream the stream to print the XML elements to
     */
    public void print(PrintStream stream) {
        stream.println("<api version=\"2\">");
        print(mClasses.values(), "class", "\t", stream);
        printClosingTag("api", "", stream);
    }

    /**
     * Adds or updates a class.
     *
     * @param name       the name of the class
     * @param version    an API version in which the class existed
     * @param deprecated whether the class was deprecated in the API version
     * @return the newly created or a previously existed class
     */
    public ApiClass addClass(String name, int version, boolean deprecated) {
        ApiClass classElement = mClasses.get(name);
        if (classElement == null) {
            classElement = new ApiClass(name, version, deprecated);
            mClasses.put(name, classElement);
        } else {
            classElement.update(version, deprecated);
        }
        return classElement;
    }

    /**
     * The bytecode visitor registers interfaces listed for a class. However,
     * a class will <b>also</b> implement interfaces implemented by the super classes.
     * This isn't available in the class file, so after all classes have been read in,
     * we iterate through all classes, and for those that have interfaces, we check up
     * the inheritance chain to see if it has already been introduced in a super class
     * at an earlier API level.
     */
    public void removeImplicitInterfaces() {
        for (ApiClass classElement : mClasses.values()) {
            classElement.removeImplicitInterfaces(mClasses);
        }
    }

    /**
     * @see ApiClass#removeOverridingMethods
     */
    public void removeOverridingMethods() {
        for (ApiClass classElement : mClasses.values()) {
            classElement.removeOverridingMethods(mClasses);
        }
    }
}
