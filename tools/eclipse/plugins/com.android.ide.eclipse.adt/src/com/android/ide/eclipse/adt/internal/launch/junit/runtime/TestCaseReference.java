/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.launch.junit.runtime;

import com.android.ddmlib.testrunner.TestIdentifier;

import org.eclipse.jdt.internal.junit.runner.IVisitsTestTrees;
import org.eclipse.jdt.internal.junit.runner.MessageIds;

import java.text.MessageFormat;

/**
 * Reference for a single Android test method.
 */
@SuppressWarnings("restriction")
class TestCaseReference extends AndroidTestReference {

    private final String mClassName;
    private final String mTestName;
    
    /**
     * Creates a TestCaseReference from a class and method name
     */
    TestCaseReference(String className, String testName) {
        mClassName = className;
        mTestName = testName;
    }

    /**
     * Creates a TestCaseReference from a {@link TestIdentifier}
     * @param test
     */
    TestCaseReference(TestIdentifier test) {
        mClassName = test.getClassName();
        mTestName = test.getTestName();
    }

    /**
     * Returns a count of the number of test cases referenced. Is always one for this class.
     */
    public int countTestCases() {
        return 1;
    }

    /**
     * Sends test identifier and test count information for this test
     * 
     * @param notified the {@link IVisitsTestTrees} to send test info to
     */
    public void sendTree(IVisitsTestTrees notified) {
        notified.visitTreeEntry(getIdentifier(), false, countTestCases());
    }

    /**
     * Returns the identifier of this test, in a format expected by JDT JUnit
     */
    public String getName() {
        return MessageFormat.format(MessageIds.TEST_IDENTIFIER_MESSAGE_FORMAT, 
                new Object[] { mTestName, mClassName});
    }
}
