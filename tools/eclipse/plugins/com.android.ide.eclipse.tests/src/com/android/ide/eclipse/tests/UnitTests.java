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


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Container TestSuite for all eclipse unit tests to be run
 * 
 * Uses Eclipse OSGI to find and then run all junit.junit.framework.Tests in 
 * this plugin, excluding tests in the FuncTests.FUNC_TEST_PACKAGE package
 * 
 * Since it uses Eclipse OSGI, it must be run in a Eclipse plugin environment
 * i.e. from Eclipse workbench, this suite must be run using the 
 * "JUnit Plug-in Test" launch configuration as opposed to as a "JUnit Test"  
 * 
 */                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              
public class UnitTests {
    private static final String TEST_PACKAGE = "com.android";
    
    public static Test suite() {
        TestSuite suite = new TestSuite();

        UnitTestCollector collector = new UnitTestCollector();
        collector.addTestCases(suite, AndroidTestPlugin.getDefault(), TEST_PACKAGE);

        return suite;
    }

    /**
     * Specialized test collector which will skip adding functional tests
     */
    private static class UnitTestCollector extends EclipseTestCollector {
        /**
         * Override parent class to exclude functional tests
         */
        @Override
        protected boolean isTestClass(Class<?> testClass) {
            return super.isTestClass(testClass) &&
            !testClass.getPackage().getName().startsWith(FuncTests.FUNC_TEST_PACKAGE);
        }
    }
}
