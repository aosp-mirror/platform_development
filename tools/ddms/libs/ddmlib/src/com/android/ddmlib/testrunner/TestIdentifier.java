/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ddmlib.testrunner;

/**
 * Identifies a parsed instrumentation test 
 */
public class TestIdentifier {

    private final String mClassName;
    private final String mTestName;
    
    /**
     * Creates a test identifier
     * 
     * @param className fully qualified class name of the test. Cannot be null.
     * @param testName name of the test. Cannot be null.
     */
    public TestIdentifier(String className, String testName) {
        if (className == null || testName == null) {
            throw new IllegalArgumentException("className and testName must " + 
                    "be non-null");
        }
        mClassName = className;
        mTestName = testName;
    }
    
    /**
     * Returns the fully qualified class name of the test
     */
    public String getClassName() {
        return mClassName;
    }

    /**
     * Returns the name of the test
     */
    public String getTestName() {
        return mTestName;
    }
    
    /**
     * Tests equality by comparing class and method name
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TestIdentifier)) {
            return false;
        }
        TestIdentifier otherTest = (TestIdentifier)other;
        return getClassName().equals(otherTest.getClassName())  && 
                getTestName().equals(otherTest.getTestName());
    }
    
    /**
     * Generates hashCode based on class and method name.
     */
    @Override
    public int hashCode() {
        return getClassName().hashCode() * 31 + getTestName().hashCode();
    }
}
