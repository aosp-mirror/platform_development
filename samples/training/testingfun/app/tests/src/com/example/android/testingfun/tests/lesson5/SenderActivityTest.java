/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.example.android.testingfun.tests.lesson5;

import com.example.android.testingfun.R;
import com.example.android.testingfun.lesson5.ReceiverActivity;
import com.example.android.testingfun.lesson5.SenderActivity;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Functional test across multiple Activities. Tests SenderActivity and ReceiverActivity. Introduces
 * advanced Instrumentation testing practices as sending key events and interaction monitoring
 * between Activities and the system.
 */
public class SenderActivityTest extends ActivityInstrumentationTestCase2<SenderActivity> {

    private static final int TIMEOUT_IN_MS = 5000;
    private static final String TEST_MESSAGE = "Hello Receiver";
    private SenderActivity mSenderActivity;

    public SenderActivityTest() {
        super(SenderActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(true);
        mSenderActivity = getActivity();
    }

    /**
     * Tests the preconditions of this test fixture.
     */
    @MediumTest
    public void testPreconditions() {
        assertNotNull("mSenderActivity is null", mSenderActivity);
    }

    @MediumTest
    public void testSendMessageToReceiverActivity() {

        //Because this functional test tests interaction across multiple components these views
        //are part of the actual test method and not of the test fixture
        final Button sendToReceiverButton = (Button) mSenderActivity
                .findViewById(R.id.send_message_button);
        final EditText senderMessageEditText = (EditText) mSenderActivity
                .findViewById(R.id.message_input_edit_text);

        //Create and add an ActivityMonitor to monitor interaction between the system and the
        //ReceiverActivity
        Instrumentation.ActivityMonitor receiverActivityMonitor = getInstrumentation()
                .addMonitor(ReceiverActivity.class.getName(), null, false);

        //Request focus on the EditText field. This must be done on the UiThread because
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                senderMessageEditText.requestFocus();
            }
        });
        //Wait until all events from the MainHandler's queue are processed
        getInstrumentation().waitForIdleSync();

        //Send the text message
        getInstrumentation().sendStringSync(TEST_MESSAGE);
        getInstrumentation().waitForIdleSync();

        //Click on the sendToReceiverButton to send the message to ReceiverActivity
        TouchUtils.clickView(this, sendToReceiverButton);

        //Wait until ReceiverActivity was launched and get a reference to it.
        ReceiverActivity receiverActivity = (ReceiverActivity) receiverActivityMonitor
                .waitForActivityWithTimeout(TIMEOUT_IN_MS);
        //Verify that ReceiverActivity was started
        assertNotNull("ReceiverActivity is null", receiverActivity);
        assertEquals("Monitor for ReceiverActivity has not been called", 1,
                receiverActivityMonitor.getHits());
        assertEquals("Activity is of wrong type", ReceiverActivity.class,
                receiverActivity.getClass());

        //Read the message received by ReceiverActivity
        final TextView receivedMessage = (TextView) receiverActivity
                .findViewById(R.id.received_message_text_view);
        //Verify that received message is correct
        assertNotNull(receivedMessage);
        assertEquals("Wrong received message", TEST_MESSAGE, receivedMessage.getText().toString());

        //Unregister monitor for ReceiverActivity
        getInstrumentation().removeMonitor(receiverActivityMonitor);
    }
}