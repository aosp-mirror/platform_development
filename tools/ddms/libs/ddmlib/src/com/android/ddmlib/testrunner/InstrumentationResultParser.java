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

import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.Log;
import com.android.ddmlib.MultiLineReceiver;

/**
 * Parses the 'raw output mode' results of an instrumentation test run from shell and informs a 
 * ITestRunListener of the results.
 * 
 * <p>Expects the following output:
 * 
 * <p>If fatal error occurred when attempted to run the tests:
 * <pre>
 * INSTRUMENTATION_STATUS: Error=error Message
 * INSTRUMENTATION_FAILED: 
 * </pre>
 * <p>or
 * <pre>
 * INSTRUMENTATION_RESULT: shortMsg=error Message
 * </pre>
 * 
 * <p>Otherwise, expect a series of test results, each one containing a set of status key/value
 * pairs, delimited by a start(1)/pass(0)/fail(-2)/error(-1) status code result. At end of test 
 * run, expects that the elapsed test time in seconds will be displayed  
 * 
 * <p>For example:
 * <pre>
 * INSTRUMENTATION_STATUS_CODE: 1
 * INSTRUMENTATION_STATUS: class=com.foo.FooTest
 * INSTRUMENTATION_STATUS: test=testFoo
 * INSTRUMENTATION_STATUS: numtests=2
 * INSTRUMENTATION_STATUS: stack=com.foo.FooTest#testFoo:312
 *    com.foo.X
 * INSTRUMENTATION_STATUS_CODE: -2   
 * ... 
 * 
 * Time: X
 * </pre>
 * <p>Note that the "value" portion of the key-value pair may wrap over several text lines
 */
public class InstrumentationResultParser extends MultiLineReceiver {
    
    /** Relevant test status keys. */
    private static class StatusKeys {
        private static final String TEST = "test";
        private static final String CLASS = "class";
        private static final String STACK = "stack";
        private static final String NUMTESTS = "numtests";
        private static final String ERROR = "Error";
        private static final String SHORTMSG = "shortMsg";
    }
    
    /** Test result status codes. */
    private static class StatusCodes {
        private static final int FAILURE = -2;
        private static final int START = 1;
        private static final int ERROR = -1;
        private static final int OK = 0;
    }

    /** Prefixes used to identify output. */
    private static class Prefixes {
        private static final String STATUS = "INSTRUMENTATION_STATUS: ";
        private static final String STATUS_CODE = "INSTRUMENTATION_STATUS_CODE: ";
        private static final String STATUS_FAILED = "INSTRUMENTATION_FAILED: ";
        private static final String CODE = "INSTRUMENTATION_CODE: ";
        private static final String RESULT = "INSTRUMENTATION_RESULT: ";
        private static final String TIME_REPORT = "Time: ";
    }
    
    private final ITestRunListener mTestListener;

    /** 
     * Test result data
     */
    private static class TestResult {
        private Integer mCode = null;
        private String mTestName = null;
        private String mTestClass = null;
        private String mStackTrace = null;
        private Integer mNumTests = null;
        
        /** Returns true if all expected values have been parsed */
        boolean isComplete() {
            return mCode != null && mTestName != null && mTestClass != null;
        }
        
        /** Provides a more user readable string for TestResult, if possible */
        @Override
        public String toString() {
            StringBuilder output = new StringBuilder();
            if (mTestClass != null ) {
                output.append(mTestClass);
                output.append('#');
            }    
            if (mTestName != null) {
                output.append(mTestName);
            }
            if (output.length() > 0) {
                return output.toString();
            }    
            return "unknown result";
        }
    }
    
    /** Stores the status values for the test result currently being parsed */
    private TestResult mCurrentTestResult = null;
    
    /** Stores the current "key" portion of the status key-value being parsed. */
    private String mCurrentKey = null;
    
    /** Stores the current "value" portion of the status key-value being parsed. */
    private StringBuilder mCurrentValue = null;
    
    /** True if start of test has already been reported to listener. */
    private boolean mTestStartReported = false;
    
    /** The elapsed time of the test run, in milliseconds. */
    private long mTestTime = 0;
    
    /** True if current test run has been canceled by user. */
    private boolean mIsCancelled = false;
    
    private static final String LOG_TAG = "InstrumentationResultParser";
    
    /**
     * Creates the InstrumentationResultParser.
     * 
     * @param listener informed of test results as the tests are executing
     */
    public InstrumentationResultParser(ITestRunListener listener) {
        mTestListener = listener;
    }
    
    /**
     * Processes the instrumentation test output from shell.
     * 
     * @see MultiLineReceiver#processNewLines
     */
    @Override
    public void processNewLines(String[] lines) {
        for (String line : lines) {
            parse(line);
            // in verbose mode, dump all adb output to log
            Log.v(LOG_TAG, line);
        }
    }
    
    /**
     * Parse an individual output line. Expects a line that is one of:
     * <ul>
     * <li> 
     * The start of a new status line (starts with Prefixes.STATUS or Prefixes.STATUS_CODE), 
     * and thus there is a new key=value pair to parse, and the previous key-value pair is 
     * finished. 
     * </li>
     * <li>
     * A continuation of the previous status (the "value" portion of the key has wrapped
     * to the next line).
     * </li>  
     * <li> A line reporting a fatal error in the test run (Prefixes.STATUS_FAILED) </li>
     * <li> A line reporting the total elapsed time of the test run. (Prefixes.TIME_REPORT) </li>  
     * </ul>
     *    
     * @param line  Text output line
     */
    private void parse(String line) {
        if (line.startsWith(Prefixes.STATUS_CODE)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            parseStatusCode(line);
        } else if (line.startsWith(Prefixes.STATUS)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            parseKey(line, Prefixes.STATUS.length());
        } else if (line.startsWith(Prefixes.RESULT)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            parseKey(line, Prefixes.RESULT.length());  
        } else if (line.startsWith(Prefixes.STATUS_FAILED) || 
                   line.startsWith(Prefixes.CODE)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            // just ignore the remaining data on this line            
        } else if (line.startsWith(Prefixes.TIME_REPORT)) {
            parseTime(line, Prefixes.TIME_REPORT.length());
        } else {
            if (mCurrentValue != null) {
                // this is a value that has wrapped to next line. 
                mCurrentValue.append("\r\n");
                mCurrentValue.append(line);
            } else {
                Log.w(LOG_TAG, "unrecognized line " + line);
            }
        }
    }
    
    /**
     * Stores the currently parsed key-value pair into mCurrentTestInfo.
     */
    private void submitCurrentKeyValue() {
        if (mCurrentKey != null && mCurrentValue != null) {
            TestResult testInfo = getCurrentTestInfo();
            String statusValue = mCurrentValue.toString();

            if (mCurrentKey.equals(StatusKeys.CLASS)) {
                testInfo.mTestClass = statusValue.trim();
            } else if (mCurrentKey.equals(StatusKeys.TEST)) {
                testInfo.mTestName = statusValue.trim();
            } else if (mCurrentKey.equals(StatusKeys.NUMTESTS)) {
                try {
                    testInfo.mNumTests = Integer.parseInt(statusValue);
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "Unexpected integer number of tests, received " + statusValue);
                }
            } else if (mCurrentKey.equals(StatusKeys.ERROR) || 
                    mCurrentKey.equals(StatusKeys.SHORTMSG)) {
                // test run must have failed
                handleTestRunFailed(statusValue); 
            } else if (mCurrentKey.equals(StatusKeys.STACK)) {
                testInfo.mStackTrace = statusValue;
            }

            mCurrentKey = null;
            mCurrentValue = null;
        }
    }
    
    private TestResult getCurrentTestInfo() {
        if (mCurrentTestResult == null) {
            mCurrentTestResult = new TestResult();
        }
        return mCurrentTestResult;
    }
    
    private void clearCurrentTestInfo() {
        mCurrentTestResult = null;
    }
    
    /**
     * Parses the key from the current line.
     * Expects format of "key=value".
     *  
     * @param line full line of text to parse 
     * @param keyStartPos the starting position of the key in the given line
     */
    private void parseKey(String line, int keyStartPos) {
        int endKeyPos = line.indexOf('=', keyStartPos);
        if (endKeyPos != -1) {
            mCurrentKey = line.substring(keyStartPos, endKeyPos).trim();
            parseValue(line, endKeyPos + 1);
        }
    }
    
    /**
     * Parses the start of a key=value pair.
     *  
     * @param line - full line of text to parse 
     * @param valueStartPos - the starting position of the value in the given line
     */
    private void parseValue(String line, int valueStartPos) {
        mCurrentValue = new StringBuilder();
        mCurrentValue.append(line.substring(valueStartPos));
    }
    
    /**
     * Parses out a status code result. 
     */
    private void parseStatusCode(String line) {
        String value = line.substring(Prefixes.STATUS_CODE.length()).trim();
        TestResult testInfo = getCurrentTestInfo();
        try {
            testInfo.mCode = Integer.parseInt(value);    
        } catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Expected integer status code, received: " + value);
        }
        
        // this means we're done with current test result bundle
        reportResult(testInfo);
        clearCurrentTestInfo();
    }
    
    /**
     * Returns true if test run canceled.
     * 
     * @see IShellOutputReceiver#isCancelled()
     */
    public boolean isCancelled() {
        return mIsCancelled;
    }
    
    /**
     * Requests cancellation of test run.
     */
    public void cancel() {
        mIsCancelled = true;
    }
    
    /**
     * Reports a test result to the test run listener. Must be called when a individual test
     * result has been fully parsed. 
     * 
     * @param statusMap key-value status pairs of test result
     */
    private void reportResult(TestResult testInfo) {
        if (!testInfo.isComplete()) {
            Log.w(LOG_TAG, "invalid instrumentation status bundle " + testInfo.toString());
            return;
        }
        reportTestRunStarted(testInfo);
        TestIdentifier testId = new TestIdentifier(testInfo.mTestClass, testInfo.mTestName);

        switch (testInfo.mCode) {
            case StatusCodes.START:
                mTestListener.testStarted(testId);
                break;
            case StatusCodes.FAILURE:
                mTestListener.testFailed(ITestRunListener.TestFailure.FAILURE, testId, 
                        getTrace(testInfo));
                mTestListener.testEnded(testId);
                break;
            case StatusCodes.ERROR:
                mTestListener.testFailed(ITestRunListener.TestFailure.ERROR, testId, 
                        getTrace(testInfo));
                mTestListener.testEnded(testId);
                break;
            case StatusCodes.OK:
                mTestListener.testEnded(testId);
                break;
            default:
                Log.e(LOG_TAG, "Unknown status code received: " + testInfo.mCode);
                mTestListener.testEnded(testId);
            break;
        }

    }
    
    /**
     * Reports the start of a test run, and the total test count, if it has not been previously 
     * reported.
     * 
     * @param testInfo current test status values
     */
    private void reportTestRunStarted(TestResult testInfo) {
        // if start test run not reported yet
        if (!mTestStartReported && testInfo.mNumTests != null) {
            mTestListener.testRunStarted(testInfo.mNumTests);
            mTestStartReported = true;
        }
    }
    
    /**
     * Returns the stack trace of the current failed test, from the provided testInfo.
     */
    private String getTrace(TestResult testInfo) {
        if (testInfo.mStackTrace != null) {
            return testInfo.mStackTrace;    
        } else {
            Log.e(LOG_TAG, "Could not find stack trace for failed test ");
            return new Throwable("Unknown failure").toString();
        }
    }
    
    /**
     * Parses out and store the elapsed time.
     */
    private void parseTime(String line, int startPos) {
        String timeString = line.substring(startPos);
        try {
            float timeSeconds = Float.parseFloat(timeString);
            mTestTime = (long) (timeSeconds * 1000); 
        } catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Unexpected time format " + timeString);
        }
    }
    
    /**
     * Process a instrumentation run failure
     */
    private void handleTestRunFailed(String errorMsg) {
        mTestListener.testRunFailed(errorMsg == null ? "Unknown error" : errorMsg);
    }
    
    /**
     * Called by parent when adb session is complete. 
     */
    @Override
    public void done() {
        super.done();
        mTestListener.testRunEnded(mTestTime);
    }
}
