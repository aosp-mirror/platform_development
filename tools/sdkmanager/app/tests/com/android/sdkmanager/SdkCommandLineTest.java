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

public class SdkCommandLineTest extends TestCase {

    private MockStdLogger mLog;
    
    /**
     * A mock version of the {@link SdkCommandLine} class that does not
     * exits and discards its stdout/stderr output.
     */
    public static class MockSdkCommandLine extends SdkCommandLine {
        private boolean mExitCalled;
        private boolean mHelpCalled;
        
        public MockSdkCommandLine(ISdkLog logger) {
            super(logger);
        }

        @Override
        public void printHelpAndExitForAction(String actionFilter,
                String errorFormat, Object... args) {
            mHelpCalled = true;
            super.printHelpAndExitForAction(actionFilter, errorFormat, args);
        }

        @Override
        protected void exit() {
            mExitCalled = true;
        }
        
        @Override
        protected void stdout(String format, Object... args) {
            // discard
        }
        
        @Override
        protected void stderr(String format, Object... args) {
            // discard
        }

        public boolean wasExitCalled() {
            return mExitCalled;
        }
        
        public boolean wasHelpCalled() {
            return mHelpCalled;
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

    /** Test list with long name and verbose */
    public final void testList_Long_Verbose() {
        MockSdkCommandLine c = new MockSdkCommandLine(mLog);
        assertEquals("all", c.getListFilter());
        c.parseArgs(new String[] { "-v", "list", "--filter", "vm" });
        assertFalse(c.wasHelpCalled());
        assertFalse(c.wasExitCalled());
        assertEquals("vm", c.getListFilter());
        assertTrue(c.isVerbose());
    }

    /** Test list with short name and no verbose */
    public final void testList_Short() {
        MockSdkCommandLine c = new MockSdkCommandLine(mLog);
        assertEquals("all", c.getListFilter());
        c.parseArgs(new String[] { "list", "-f", "vm" });
        assertFalse(c.wasHelpCalled());
        assertFalse(c.wasExitCalled());
        assertEquals("vm", c.getListFilter());
    }
    
    /** Test list with long name and missing parameter */
    public final void testList_Long_MissingParam() {
        MockSdkCommandLine c = new MockSdkCommandLine(mLog);
        assertEquals("all", c.getListFilter());
        c.parseArgs(new String[] { "list", "--filter" });
        assertTrue(c.wasHelpCalled());
        assertTrue(c.wasExitCalled());
        assertEquals("all", c.getListFilter());
    }
}
