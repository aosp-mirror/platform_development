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
package com.example.android.samplesync.activities;

import com.example.android.samplesync.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Activity to handle the invite-intent. In a real app, this would look up the user on the network
 * and either connect ("add as friend", "follow") or invite them to the network
 */
public class InviteContactActivity extends Activity {
    private static final String TAG = "InviteContactActivity";

    private TextView mUriTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invite_contact_activity);

        mUriTextView = (TextView) findViewById(R.id.invite_contact_uri);
        mUriTextView.setText(getIntent().getDataString());
    }
}
