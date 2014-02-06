/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.emulator.smoketests.sms;

import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.database.Cursor;
import android.os.Bundle;
import android.os.HandlerThread;
import com.android.test.InjectContext;

import org.junit.Assert;
import static junit.framework.Assert.assertEquals;

import org.junit.Test;
/**
 * Sms Test
 *
 * Test that an SMS message has been received
 */
public class SmsTest {

    /**
     * Prior to running this test an sms must be sent
     * via DDMS
     */
    public final static String NUMBER = "5551212";
    public final static String BODY = "test sms";
    private final static int SMS_POLL_TIME_MS = 10 * 1000;
    private final static int SIXY_SECONDS_OF_LOOPS = 6;
    @InjectContext
    public Context mContext;

    /**
     * Verify that an SMS has been received with the correct number and body
     */
    @Test
    public void testReceivedSms() throws java.lang.InterruptedException {
        Cursor c = getSmsCursor();
        c.moveToFirst();

        String number = c.getString(c.getColumnIndexOrThrow("address"));
        String body = c.getString(c.getColumnIndexOrThrow("body"));

        c.close();

        assertEquals(NUMBER, number);
        assertEquals(BODY, body);
    }

    private Cursor getSmsCursor() throws java.lang.InterruptedException {
        ContentResolver r = mContext.getContentResolver();
        Uri message = Uri.parse("content://sms/");
        Cursor c;

        for(int i = 0; i < SIXY_SECONDS_OF_LOOPS; i++) {
            c = r.query(message,null,null,null,null);

            if(c.getCount() != 0) {
                return c;
            }

            c.close();
            Thread.sleep(SMS_POLL_TIME_MS);
        }
        Assert.fail("Did not find any SMS messages. Giving up");
        // necessary for compilation
        return null;
    }

}
