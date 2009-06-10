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

package com.android.sdkmanager;

import com.android.sdklib.ISdkLog;

import junit.framework.TestCase;


public class CommandLineProcessorTest extends TestCase {

    private MockStdLogger mLog;
    
    /**
     * A mock version of the {@link CommandLineProcessor} class that does not
     * exits and captures its stdout/stderr output.
     */
    public static class MockCommandLineProcessor extends CommandLineProcessor {
        private boolean mExitCalled;
        private boolean mHelpCalled;
        private String mStdOut = "";
        private String mStdErr = "";
        
        public MockCommandLineProcessor(ISdkLog logger) {
            super(logger,
                  new String[][] {
                    { "verb1", "action1", "Some action" },
                    { "verb1", "action2", "Another action" },
            });
            define(Mode.STRING, false /*mandatory*/,
                    "verb1", "action1", "1", "first", "non-mandatory flag", null);
            define(Mode.STRING, true /*mandatory*/,
                    "verb1", "action1", "2", "second", "mandatory flag", null);
        }
        
        @Override
        public void printHelpAndExitForAction(String verb, String directObject,
                String errorFormat, Object... args) {
            mHelpCalled = true;
            super.printHelpAndExitForAction(verb, directObject, errorFormat, args);
        }
        
        @Override
        protected void exit() {
            mExitCalled = true;
        }
        
        @Override
        protected void stdout(String format, Object... args) {
            String s = String.format(format, args);
            mStdOut += s + "\n";
            // don't call super to avoid printing stuff
        }
        
        @Override
        protected void stderr(String format, Object... args) {
            String s = String.format(format, args);
            mStdErr += s + "\n";
            // don't call super to avoid printing stuff
        }
        
        public boolean wasHelpCalled() {
            return mHelpCalled;
        }
        
        public boolean wasExitCalled() {
            return mExitCalled;
        }
        
        public String getStdOut() {
            return mStdOut;
        }
        
        public String getStdErr() {
            return mStdErr;
        }
    }

    @Override
    protected void setUp() throws Exception {
        mLog = new MockStdLogger();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testPrintHelpAndExit() {
        MockCommandLineProcessor c = new MockCommandLineProcessor(mLog);        
        assertFalse(c.wasExitCalled());
        assertFalse(c.wasHelpCalled());
        assertTrue(c.getStdOut().equals(""));
        assertTrue(c.getStdErr().equals(""));
        c.printHelpAndExit(null);
        assertTrue(c.getStdOut().indexOf("-v") != -1);
        assertTrue(c.getStdOut().indexOf("--verbose") != -1);
        assertTrue(c.getStdErr().equals(""));
        assertTrue(c.wasExitCalled());

        c = new MockCommandLineProcessor(mLog);        
        assertFalse(c.wasExitCalled());
        assertTrue(c.getStdOut().equals(""));
        assertTrue(c.getStdErr().indexOf("Missing parameter") == -1);

        c.printHelpAndExit("Missing %s", "parameter");
        assertTrue(c.wasExitCalled());
        assertFalse(c.getStdOut().equals(""));
        assertTrue(c.getStdErr().indexOf("Missing parameter") != -1);
    }
    
    public final void testVerbose() {
        MockCommandLineProcessor c = new MockCommandLineProcessor(mLog);        

        assertFalse(c.isVerbose());
        c.parseArgs(new String[] { "-v" });
        assertTrue(c.isVerbose());
        assertTrue(c.wasExitCalled());
        assertTrue(c.wasHelpCalled());
        assertTrue(c.getStdErr().indexOf("Missing verb name.") != -1);

        c = new MockCommandLineProcessor(mLog);        
        c.parseArgs(new String[] { "--verbose" });
        assertTrue(c.isVerbose());
        assertTrue(c.wasExitCalled());
        assertTrue(c.wasHelpCalled());
        assertTrue(c.getStdErr().indexOf("Missing verb name.") != -1);
    }
    
    public final void testHelp() {
        MockCommandLineProcessor c = new MockCommandLineProcessor(mLog);        

        c.parseArgs(new String[] { "-h" });
        assertTrue(c.wasExitCalled());
        assertTrue(c.wasHelpCalled());
        assertTrue(c.getStdErr().indexOf("Missing verb name.") == -1);

        c = new MockCommandLineProcessor(mLog);        
        c.parseArgs(new String[] { "--help" });
        assertTrue(c.wasExitCalled());
        assertTrue(c.wasHelpCalled());
        assertTrue(c.getStdErr().indexOf("Missing verb name.") == -1);
    }

    public final void testMandatory() {
        MockCommandLineProcessor c = new MockCommandLineProcessor(mLog);        

        c.parseArgs(new String[] { "verb1", "action1", "-1", "value1", "-2", "value2" });
        assertFalse(c.wasExitCalled());
        assertFalse(c.wasHelpCalled());
        assertEquals("", c.getStdErr());
        assertEquals("value1", c.getValue("verb1", "action1", "first"));
        assertEquals("value2", c.getValue("verb1", "action1", "second"));

        c = new MockCommandLineProcessor(mLog);        
        c.parseArgs(new String[] { "verb1", "action1", "-2", "value2" });
        assertFalse(c.wasExitCalled());
        assertFalse(c.wasHelpCalled());
        assertEquals("", c.getStdErr());
        assertEquals(null, c.getValue("verb1", "action1", "first"));
        assertEquals("value2", c.getValue("verb1", "action1", "second"));

        c = new MockCommandLineProcessor(mLog);        
        c.parseArgs(new String[] { "verb1", "action1" });
        assertTrue(c.wasExitCalled());
        assertTrue(c.wasHelpCalled());
        assertTrue(c.getStdErr().indexOf("must be defined") != -1);
        assertEquals(null, c.getValue("verb1", "action1", "first"));
        assertEquals(null, c.getValue("verb1", "action1", "second"));
    }
}
