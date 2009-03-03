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

import java.util.Hashtable;
import java.util.Map;

/**
 * Parses the 'raw output mode' results of an instrument test run from shell, and informs a 
 * ITestRunListener of the results
 * 
 * Expects the following output:
 * 
 * If fatal error occurred when attempted to run the tests:
 * <i> INSTRUMENTATION_FAILED: </i>  
 * 
 * Otherwise, expect a series of test results, each one containing a set of status key/value
 * pairs, delimited by a start(1)/pass(0)/fail(-2)/error(-1) status code result. At end of test 
 * run, expects that the elapsed test time in seconds will be displayed  
 * 
 * i.e.
 * <i>
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
 * </i>
 * 
 * Note that the "value" portion of the key-value pair may wrap over several text lines
 */
public class InstrumentationResultParser extends MultiLineReceiver {
    
    // relevant test status keys
    private static final String CODE_KEY = "code";
    private static final String TEST_KEY = "test";
    private static final String CLASS_KEY = "class";
    private static final String STACK_KEY = "stack";
    private static final String NUMTESTS_KEY = "numtests";
    
    // test result status codes
    private static final int FAILURE_STATUS_CODE = -2;
    private static final int START_STATUS_CODE = 1;
    private static final int ERROR_STATUS_CODE = -1;
    private static final int OK_STATUS_CODE = 0;

    // recognized output patterns
    private static final String STATUS_PREFIX = "INSTRUMENTATION_STATUS: ";
    private static final String STATUS_PREFIX_CODE = "INSTRUMENTATION_STATUS_CODE: ";
    private static final String STATUS_FAILED = "INSTRUMENTATION_FAILED: ";
    private static final String TIME_REPORT = "Time: ";
    
    private final ITestRunListener mTestListener;
    /** key-value map for current test */
    private Map<String, String> mStatusValues;
    /** stores the current "key" portion of the status key-value being parsed */
    private String mCurrentKey;
    /** stores the current "value" portion of the status key-value being parsed */
    private StringBuilder mCurrentValue;
    /** true if start of test has already been reported to listener */
    private boolean mTestStartReported;
    /** the elapsed time of the test run, in ms */
    private long mTestTime;
    /** true if current test run has been canceled by user */
    private boolean mIsCancelled;
    
    private static final String LOG_TAG = "InstrumentationResultParser";
    
    /**
     * Creates the InstrumentationResultParser
     * @param listener - listener to report results to. will be informed of test results as the 
     * tests are executing
     */
    public InstrumentationResultParser(ITestRunListener listener) {
        mStatusValues = new Hashtable<String, String>();
        mCurrentKey = null;
        setTrimLine(false);
        mTestListener = listener;
        mTestStartReported = false;
        mTestTime = 0;
        mIsCancelled = false;
    }
    
    /**
     * Processes the instrumentation test output from shell
     * @see MultiLineReceiver#processNewLines
     */
    @Override
    public void processNewLines(String[] lines) {
        for (String line : lines) {
            parse(line);
        }
    }
    
    /**
     * Parse an individual output line. Expects a line that either is:
     * a) the start of a new status line (ie. starts with STATUS_PREFIX or STATUS_PREFIX_CODE), 
     *    and thus there is a new key=value pair to parse, and the previous key-value pair is 
     *    finished
     * b) a continuation of the previous status (ie the "value" portion of the key has wrapped
     *    to the next line. 
     * c) a line reporting a fatal error in the test run (STATUS_FAILED)
     * d) a line reporting the total elapsed time of the test run.  
     *    
     * @param line - text output line
     */
    private void parse(String line) {
        if (line.startsWith(STATUS_PREFIX_CODE)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            parseStatusCode(line);
        } else if (line.startsWith(STATUS_PREFIX)) {
            // Previous status key-value has been collected. Store it.
            submitCurrentKeyValue();
            parseKey(line, STATUS_PREFIX.length());
        } else if (line.startsWith(STATUS_FAILED)) {
            Log.e(LOG_TAG, "test run failed " + line);
            mTestListener.testRunFailed(line);
        } else if (line.startsWith(TIME_REPORT)) {
            parseTime(line, TIME_REPORT.length());
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
     * Stores the currently parsed key-value pair in the status map
     */
    private void submitCurrentKeyValue() {
        if (mCurrentKey != null && mCurrentValue != null) {
            mStatusValues.put(mCurrentKey, mCurrentValue.toString());
            mCurrentKey = null;
            mCurrentValue = null;
        }
    }
    
    /**
     * Parses the key from the current line
     * Expects format of "key=value",  
     * @param line - full line of text to parse 
     * @param keyStartPos - the starting position of the key in the given line
     */
    private void parseKey(String line, int keyStartPos) {
        int endKeyPos = line.indexOf('=', keyStartPos);
        if (endKeyPos != -1) {
            mCurrentKey = line.substring(keyStartPos, endKeyPos).trim();
            parseValue(line, endKeyPos+1);
        }
    }
    
    /**
     * Parses the start of a key=value pair. 
     * @param line - full line of text to parse 
     * @param valueStartPos - the starting position of the value in the given line
     */
    private void parseValue(String line, int valueStartPos) {
        mCurrentValue = new StringBuilder();
        mCurrentValue.append(line.substring(valueStartPos));
    }
    
    /**
     * Parses out a status code result. For consistency, stores the result as a CODE entry in 
     * key-value status map 
     */
    private void parseStatusCode(String line) {
        String value = line.substring(STATUS_PREFIX_CODE.length()).trim();
        mStatusValues.put(CODE_KEY, value);
        
        // this means we're done with current test result bundle
        reportResult(mStatusValues);
        mStatusValues.clear();
    }
    
    /**
     * Returns true if test run canceled
     * 
     * @see IShellOutputReceiver#isCancelled()
     */
    public boolean isCancelled() {
        return mIsCancelled;
    }
    
    /**
     * Requests cancellation of test result parsing
     */
    public void cancel() {
        mIsCancelled = true;
    }
    
    /**
     * Reports a test result to the test run listener. Must be called when a individual test
     * result has been fully parsed. 
     * @param statusMap - key-value status pairs of test result
     */
    private void reportResult(Map<String, String> statusMap) {
        String className = statusMap.get(CLASS_KEY);
        String testName = statusMap.get(TEST_KEY);
        String statusCodeString = statusMap.get(CODE_KEY);
        
        if (className == null || testName == null || statusCodeString == null) {
            Log.e(LOG_TAG, "invalid instrumentation status bundle " + statusMap.toString());
            return;
        }
        className = className.trim();
        testName = testName.trim();

        reportTestStarted(statusMap);
        
        try {
           int statusCode = Integer.parseInt(statusCodeString);
           
           switch (statusCode) {
               case START_STATUS_CODE:
                   mTestListener.testStarted(className, testName);
                   break;
               case FAILURE_STATUS_CODE:
                   mTestListener.testFailed(ITestRunListener.STATUS_FAILURE, className, testName, 
                           getTrace(statusMap));
                   mTestListener.testEnded(className, testName);
                   break;
               case ERROR_STATUS_CODE:
                   mTestListener.testFailed(ITestRunListener.STATUS_ERROR, className, testName, 
                           getTrace(statusMap));
                   mTestListener.testEnded(className, testName);
                   break;
               case OK_STATUS_CODE:
                   mTestListener.testEnded(className, testName);
                   break;
               default:
                   Log.e(LOG_TAG, "Expected status code, received: " + statusCodeString);
                   mTestListener.testEnded(className, testName);
                   break;
           }
        }
        catch (NumberFormatException e) {
           Log.e(LOG_TAG, "Expected integer status code, received: " + statusCodeString);    
        }
    }
    
    /**
     * Reports the start of a test run, and the total test count, if it has not been previously 
     * reported
     * @param statusMap - key-value status pairs
     */
    private void reportTestStarted(Map<String, String> statusMap) {
        // if start test run not reported yet
        if (!mTestStartReported) {
            String numTestsString = statusMap.get(NUMTESTS_KEY);
            if (numTestsString != null) {
                try {
                    int numTests = Integer.parseInt(numTestsString);
                    mTestListener.testRunStarted(numTests);
                    mTestStartReported = true;
                }
                catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "Unexpected numTests format " + numTestsString);
                }
            }
        }
    }
    
    /**
     * Returns the stack trace of the current failed test, from the provided key-value status map
     */
    private String getTrace(Map<String, String> statusMap) {
        String stackTrace = statusMap.get(STACK_KEY);
        if (stackTrace != null) {        
            return stackTrace;    
        }
        else {
            Log.e(LOG_TAG, "Could not find stack trace for failed test ");
            return new Throwable("Unknown failure").toString();
        }
    }
    
    /**
     * Parses out and store the elapsed time
     */
    private void parseTime(String line, int startPos) {
        String timeString = line.substring(startPos);
        try {
            float timeSeconds = Float.parseFloat(timeString);
            mTestTime = (long)(timeSeconds * 1000); 
        }
        catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Unexpected time format " + timeString);
        }
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
