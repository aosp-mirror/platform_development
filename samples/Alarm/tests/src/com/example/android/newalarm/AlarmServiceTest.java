/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.newalarm;

import android.content.Intent;
import android.test.ServiceTestCase;
import com.example.android.newalarm.AlarmService;

/**
 * Test class for the Alarm sample test package. This test class tests the AlarmService
 * service component.
 */
public class AlarmServiceTest extends ServiceTestCase<AlarmService> {
    // Contains an Intent used to start the service
    Intent mStartServiceIntent;

    // Contains a handle to the system alarm service
    AlarmService mService;

    /**
     * Constructor for the test class. Test classes that are run by InstrumentationTestRunner
     * must provide a constructor with no arguments that calls the base class constructor as its
     * first statement.
     */
    public AlarmServiceTest() {
        super(AlarmService.class);
    }

    /*
     * Sets up the test fixture. This method is called before each test
     */
    @Override
    protected void setUp() throws Exception {

        super.setUp();

        // Sets up an intent to start the service under test
        mStartServiceIntent = new Intent(this.getSystemContext(),AlarmService.class);
    }

    /**
     * Cleans up the test fixture
     * Called after each test method. If you override the method, call super.tearDown() as the
     * last statement in your override.
     */
    @Override
    protected void tearDown() throws Exception {
        // Always call the super constructor when overriding tearDown()
        super.tearDown();
    }

    /**
     * Tests the service's onCreate() method. Starts the service using startService(Intent)
     */
    public void testServiceCreate() {
        // Starts the service under test
        this.startService(mStartServiceIntent);

        // Gets a handle to the service under test.
        mService = this.getService();

        // Asserts that the Notification Manager was created in the service under test.
        assertNotNull(mService.mNotificationManager);

        // Asserts that the PendingIntent for the expanded status window was created
        assertNotNull(mService.mContentIntent);

        // Asserts that the notification was created
        assertNotNull(mService.mNotification);
    }

}
