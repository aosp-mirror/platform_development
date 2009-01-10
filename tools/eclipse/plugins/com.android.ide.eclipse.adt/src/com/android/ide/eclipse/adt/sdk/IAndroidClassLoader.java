/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.sdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.management.InvalidAttributeValueException;

/**
 * Classes which implements this interface provide methods to access framework resource
 * data loaded from the SDK.
 */
public interface IAndroidClassLoader {
    
    /**
     * Classes which implement this interface provide methods to describe a class.
     */
    public interface IClassDescriptor {

        String getCanonicalName();

        IClassDescriptor getSuperclass();

        String getSimpleName();

        IClassDescriptor getEnclosingClass();

        IClassDescriptor[] getDeclaredClasses();
        
        boolean isInstantiable();
    }

    /**
     * Finds and loads all classes that derive from a given set of super classes.
     * 
     * @param rootPackage Root package of classes to find. Use an empty string to find everyting.
     * @param superClasses The super classes of all the classes to find. 
     * @return An hash map which keys are the super classes looked for and which values are
     *         ArrayList of the classes found. The array lists are always created for all the
     *         valid keys, they are simply empty if no deriving class is found for a given
     *         super class. 
     * @throws IOException
     * @throws InvalidAttributeValueException
     * @throws ClassFormatError
     */
    public HashMap<String, ArrayList<IClassDescriptor>> findClassesDerivingFrom(
            String rootPackage, String[] superClasses)
        throws IOException, InvalidAttributeValueException, ClassFormatError;

    /**
     * Returns a {@link IClassDescriptor} by its fully-qualified name.
     * @param className the fully-qualified name of the class to return.
     * @throws ClassNotFoundException
     */
    public IClassDescriptor getClass(String className) throws ClassNotFoundException;

    /**
     * Returns a string indicating the source of the classes, typically for debugging
     * or in error messages. This would typically be a JAR file name or some kind of
     * identifier that would mean something to the user when looking at error messages.
     * 
     * @return An informal string representing the source of the classes.
     */
    public String getSource();
}
