/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.example.android.samplesync.notifier;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Service to handle view notifications. This allows the sample sync adapter to update the
 * information when the contact is being looked at
 */
public class NotifierService extends IntentService {
    private static final String TAG = "NotifierService";

    public NotifierService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // In reality, we would write some data (e.g. a high-res picture) to the contact.
        // for this demo, we just write a line to the log
        Log.i(TAG, "Contact opened: " + intent.getData());
    }
}
