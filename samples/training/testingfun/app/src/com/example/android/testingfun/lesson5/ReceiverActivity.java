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
package com.example.android.testingfun.lesson5;

import com.example.android.testingfun.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

/**
 * Receives a message from SenderActivity and displays the message.
 */
public class ReceiverActivity extends Activity {

    /**
     * The extra key that is used to identify the message in the Intents data bundle
     */
    private static final String EXTRA_SENDER_MESSAGE_TEXT
            = "com.example.android.testingfun.lesson5.extra.sender.message.text";

    /**
     * Factory method to create an launch intent for this activity.
     *
     * @param context the context to this intent should be bound to
     * @param message the message data that should be added to this intent
     * @return a configured intent to launch this activity with a given message
     */
    public static Intent makeIntent(Context context, CharSequence message) {
        return new Intent(context, ReceiverActivity.class)
                .putExtra(EXTRA_SENDER_MESSAGE_TEXT, message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        final CharSequence senderMessage = getIntent()
                .getCharSequenceExtra(EXTRA_SENDER_MESSAGE_TEXT);
        final TextView receiverTextView = (TextView) findViewById(R.id.received_message_text_view);
        if (!TextUtils.isEmpty(senderMessage)) {
            receiverTextView.setText(senderMessage);
        }
    }
}
