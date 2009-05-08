/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Eclipse Public License, Version 1.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.eclipse.org/org/documents/epl-v10.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.ide.eclipse.tests;

import org.eclipse.core.runtime.Plugin;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Class for collecting all test cases in an eclipse plugin
 * 
 */
public class EclipseTestCollector {

    /**
     * Constructor
     */
    public EclipseTestCollector() {
        
    }

    /**
     * Searches through given plugin, adding all TestCase classes to given suite
     * @param suite - TestSuite to add to
     * @param plugin - Plugin to search for tests
     * @param expectedPackage - expected package for tests. Only test classes 
     *  that start with this package name will be added to suite
     */
    public void addTestCases(TestSuite suite, Plugin plugin, String expectedPackage) {
        if (plugin != null) {
            Enumeration<?> entries = plugin.getBundle().findEntries("/", "*.class", true);
    
            while (entries.hasMoreElements()) {
                URL entry = (URL)entries.nextElement();
                String filePath = entry.getPath().replace(".class", "");
                try {
                  Class<?> testClass = getClass(filePath, expectedPackage);
                  if (isTestClass(testClass)) {
                      suite.addTestSuite(testClass);
                  }
                } 
                catch (ClassNotFoundException e) {
                  // ignore, this is not the class we're looking for
                  //sLogger.log(Level.INFO, "Could not load class " + filePath);
              }
            }
        }
    }
    
    /**
     * Returns true if given class should be added to suite
     */
    protected boolean isTestClass(Class<?> testClass) {
        return TestCase.class.isAssignableFrom(testClass) &&
          Modifier.isPublic(testClass.getModifiers()) &&
          hasPublicConstructor(testClass);
    }
    
    /**
     * Returns true if given class has a public constructor
     */
    protected boolean hasPublicConstructor(Class<?> testClass) {
        try {
            TestSuite.getTestConstructor(testClass);
        } catch(NoSuchMethodException e) {
            return false;
        }
        return true;
    }
    
    /**
     * Load the class given by the plugin aka bundle file path
     * @param filePath - path of class in bundle
     * @param expectedPackage - expected package of class
     * @throws ClassNotFoundException
     */
    protected Class<?> getClass(String filePath, String expectedPackage) throws ClassNotFoundException {
        String dotPath = filePath.replace('/', '.');
        // remove the output folders, by finding where package name starts
        int index = dotPath.indexOf(expectedPackage);
        if (index == -1) {
            throw new ClassNotFoundException();
        }
        String packagePath = dotPath.substring(index);
        return Class.forName(packagePath);   
    }
}
