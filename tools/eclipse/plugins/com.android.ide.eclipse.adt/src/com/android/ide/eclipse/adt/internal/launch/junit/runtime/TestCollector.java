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

import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;

import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.IVisitsTestTrees;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects info about tests to be executed by listening to the results of an Android test run.
 */
@SuppressWarnings("restriction")
class TestCollector implements ITestRunListener {

    private int mTotalTestCount;
    /** test name to test suite reference map. */
    private Map<String, TestSuiteReference> mTestTree;
    private String mErrorMessage = null;

    TestCollector() {
        mTotalTestCount = 0; 
        mTestTree = new HashMap<String, TestSuiteReference>();
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testEnded(com.android.ddmlib.testrunner.TestIdentifier)
     */
    public void testEnded(TestIdentifier test) {
        // ignore
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testFailed(com.android.ddmlib.testrunner.ITestRunListener.TestFailure, com.android.ddmlib.testrunner.TestIdentifier, java.lang.String)
     */
    public void testFailed(TestFailure status, TestIdentifier test, String trace) {
        // ignore - should be impossible since this is only collecting test information
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testRunEnded(long)
     */
    public void testRunEnded(long elapsedTime) {
        // ignore
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testRunFailed(java.lang.String)
     */
    public void testRunFailed(String errorMessage) {
        mErrorMessage = errorMessage;
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testRunStarted(int)
     */
    public void testRunStarted(int testCount) {
        mTotalTestCount = testCount;
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testRunStopped(long)
     */
    public void testRunStopped(long elapsedTime) {
        // ignore
    }

    /* (non-Javadoc)
     * @see com.android.ddmlib.testrunner.ITestRunListener#testStarted(com.android.ddmlib.testrunner.TestIdentifier)
     */
    public void testStarted(TestIdentifier test) {
        TestSuiteReference suiteRef = mTestTree.get(test.getClassName());
        if (suiteRef == null) {
            // this test suite has not been seen before, create it
            suiteRef = new TestSuiteReference(test.getClassName());
            mTestTree.put(test.getClassName(), suiteRef);
        }
        suiteRef.addTest(new TestCaseReference(test));
    }

    /**
     * Returns the total test count in the test run.
     */
    public int getTestCaseCount() {
        return mTotalTestCount;
    }

    /**
     * Sends info about the test tree to be executed (ie the suites and their enclosed tests) 
     * 
     * @param notified the {@link IVisitsTestTrees} to send test data to
     */
    public void sendTrees(IVisitsTestTrees notified) {
        for (ITestReference ref : mTestTree.values()) {
            ref.sendTree(notified);
        }
    }

    /**
     * Returns the error message that was reported when collecting test info. 
     * Returns <code>null</code> if no error occurred.
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }
}
