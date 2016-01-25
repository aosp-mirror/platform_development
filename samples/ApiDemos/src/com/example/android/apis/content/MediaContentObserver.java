/**
 * Copyright (c) 2016, The Android Open Source Project
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

package com.example.android.apis.content;

import android.app.Activity;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.android.apis.R;

public class MediaContentObserver extends Activity {
    ContentObserver mContentObserver;
    View mScheduleJob;
    View mCancelJob;
    TextView mDataText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContentObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange, Uri uri) {
                mDataText.append(uri.toString());
                mDataText.append("\n");
            }
        };

        Log.d("foo", "Observing: " + MediaContentJob.MEDIA_URI);

        // See res/any/layout/resources.xml for this view layout definition.
        setContentView(R.layout.media_content_observer);

        mScheduleJob = findViewById(R.id.schedule_job);
        mCancelJob = findViewById(R.id.cancel_job);
        mDataText = (TextView)findViewById(R.id.changes_text);

        mScheduleJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaContentJob.scheduleJob(MediaContentObserver.this);
                updateButtons();
            }
        });
        mCancelJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaContentJob.cancelJob(MediaContentObserver.this);
                updateButtons();
            }
        });
        updateButtons();

        getContentResolver().registerContentObserver(MediaContentJob.MEDIA_URI, true,
                mContentObserver);
    }

    void updateButtons() {
        if (MediaContentJob.isScheduled(this)) {
            mScheduleJob.setEnabled(false);
            mCancelJob.setEnabled(true);
        } else {
            mScheduleJob.setEnabled(true);
            mCancelJob.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mContentObserver);
    }

}
