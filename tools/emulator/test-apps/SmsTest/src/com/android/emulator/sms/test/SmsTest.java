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
package com.android.emulator.sms.test;

import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.database.Cursor;
import android.os.Bundle;
import android.os.HandlerThread;

import android.support.test.InjectContext;

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

    @InjectContext
    public Context mContext;

    /**
     * Verify that an SMS has been recieved with the correct number and body
     */
    public void testRecievedSms(){
        ContentResolver r = mContext.getContentResolver();
        Uri message = Uri.parse("content://sms/");
        Cursor c = r.query(message,null,null,null,null);
        c.moveToFirst();
        String number = c.getString(c.getColumnIndexOrThrow("address"));
        String body = c.getString(c.getColumnIndexOrThrow("body"));
        c.close();
        assertEquals(NUMBER, number);
        assertEquals(BODY, body);
    }

}
