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

package com.example.android.wearable.elizachat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";

    public static final String EXTRA_MESSAGE = "message";

    public static final String ACTION_NOTIFY = "com.example.android.wearable.elizachat.NOTIFY";

    public static final String ACTION_GET_CONVERSATION
            = "com.example.android.wearable.elizachat.CONVERSATION";

    private BroadcastReceiver mReceiver;

    private TextView mHistoryView;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.main);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                processMessage(intent);
            }
        };
        mHistoryView = (TextView) findViewById(R.id.history);
        startResponderService(ResponderService.ACTION_INCOMING);
    }

    private void startResponderService(String action) {
        Intent serviceIntent = new Intent(this, ResponderService.class);
        serviceIntent.setAction(action);
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(ACTION_NOTIFY));
        mHistoryView.setText("");
        startResponderService(ACTION_GET_CONVERSATION);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onPause();
    }

    private void processMessage(Intent intent) {
        String text = intent.getStringExtra(EXTRA_MESSAGE);
        if (!TextUtils.isEmpty(text)) {
            mHistoryView.append("\n" + text);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_stop_service:
                stopService(new Intent(this, ResponderService.class));
                finish();
                break;
        }
        return true;
    }
}
