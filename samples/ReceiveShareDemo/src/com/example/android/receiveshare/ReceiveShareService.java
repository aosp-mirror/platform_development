/**
 * Copyright (c) 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.receiveshare;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class ReceiveShareService extends IntentService {
    Handler mHandler;

    public ReceiveShareService() {
        super("ReceiveShareService");
    }

    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(getMainLooper());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mHandler.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(ReceiveShareService.this, R.string.preparing_to_process_share,
                        Toast.LENGTH_LONG).show();
            }
        });
        try {
            // Give the activity a chance to finish.
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final CharSequence text = ReceiveShare.buildShareInfo(getContentResolver(), intent);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ReceiveShareService.this, text, Toast.LENGTH_LONG).show();
            }
        });
        Log.i("ReceiveShare", text.toString());
    }
}
