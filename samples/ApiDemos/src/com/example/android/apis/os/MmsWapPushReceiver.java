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

package com.example.android.apis.os;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.util.Log;

import com.example.android.mmslib.ContentType;
import com.example.android.mmslib.pdu.GenericPdu;
import com.example.android.mmslib.pdu.NotificationInd;
import com.example.android.mmslib.pdu.PduHeaders;
import com.example.android.mmslib.pdu.PduParser;

/**
 * Receiver for MMS WAP push
 */
public class MmsWapPushReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsMessagingDemo";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction())
                && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            final byte[] data = intent.getByteArrayExtra("data");
            final PduParser parser = new PduParser(
                    data, PduParserUtil.shouldParseContentDisposition());
            GenericPdu pdu = null;
            try {
                pdu = parser.parse();
            } catch (final RuntimeException e) {
                Log.e(TAG, "Invalid MMS WAP push", e);
            }
            if (pdu == null) {
                Log.e(TAG, "Invalid WAP push data");
                return;
            }
            switch (pdu.getMessageType()) {
                case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND: {
                    final NotificationInd nInd = (NotificationInd) pdu;
                    final String location = new String(nInd.getContentLocation());
                    Log.v(TAG, "Received MMS notification: " + location);
                    final Intent di = new Intent();
                    di.setClass(context, MmsMessagingDemo.class);
                    di.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    di.putExtra(MmsMessagingDemo.EXTRA_NOTIFICATION_URL, location);
                    context.startActivity(di);
                    break;
                }
                // FLAG (ywen): impl. handling of the following push
                case PduHeaders.MESSAGE_TYPE_DELIVERY_IND: {
                    Log.v(TAG, "Received delivery report");
                    break;
                }
                case PduHeaders.MESSAGE_TYPE_READ_ORIG_IND: {
                    Log.v(TAG, "Received read report");
                    break;
                }
            }
        }
    }
}
