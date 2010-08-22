/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.samplesync.authenticator;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;

/**
 * This is a series of unit tests for the {@link AuthenticatorActivity} class.
 */
@SmallTest
public class AuthenticatorActivityTests extends
    ActivityInstrumentationTestCase2<AuthenticatorActivity> {

    private static final int ACTIVITY_WAIT = 10000;

    private Instrumentation mInstrumentation;

    private Context mContext;

    public AuthenticatorActivityTests() {

        super(AuthenticatorActivity.class);
    }

    /**
     * Common setup code for all tests. Sets up a default launch intent, which
     * some tests will use (others will override).
     */
    @Override
    protected void setUp() throws Exception {

        super.setUp();
        mInstrumentation = this.getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
    }

    @Override
    protected void tearDown() throws Exception {

        super.tearDown();
    }

    /**
     * Confirm that Login is presented.
     */
    @SmallTest
    public void testLoginOffered() {

        Instrumentation.ActivityMonitor monitor =
            mInstrumentation.addMonitor(AuthenticatorActivity.class.getName(), null, false);
        Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mInstrumentation.startActivitySync(intent);
        Activity activity = mInstrumentation.waitForMonitorWithTimeout(monitor, ACTIVITY_WAIT);
        View loginbutton = activity.findViewById(R.id.ok_button);
        int expected = View.VISIBLE;
        assertEquals(expected, loginbutton.getVisibility());
    }
}
