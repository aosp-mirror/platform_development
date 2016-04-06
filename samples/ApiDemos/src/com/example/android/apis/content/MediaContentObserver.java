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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.android.apis.R;

public class MediaContentObserver extends Activity {
    public static final int REQ_PHOTOS_PERM = 1;

    ContentObserver mContentObserver;
    View mScheduleMediaJob;
    View mCancelMediaJob;
    View mSchedulePhotosJob;
    View mCancelPhotosJob;
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

        mScheduleMediaJob = findViewById(R.id.schedule_media_job);
        mCancelMediaJob = findViewById(R.id.cancel_media_job);
        mSchedulePhotosJob = findViewById(R.id.schedule_photos_job);
        mCancelPhotosJob = findViewById(R.id.cancel_photos_job);
        mDataText = (TextView)findViewById(R.id.changes_text);

        mScheduleMediaJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaContentJob.scheduleJob(MediaContentObserver.this);
                updateButtons();
            }
        });
        mCancelMediaJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaContentJob.cancelJob(MediaContentObserver.this);
                updateButtons();
            }
        });
        mSchedulePhotosJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                            REQ_PHOTOS_PERM);
                } else {
                    PhotosContentJob.scheduleJob(MediaContentObserver.this);
                    updateButtons();
                }
            }
        });
        mCancelPhotosJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotosContentJob.cancelJob(MediaContentObserver.this);
                updateButtons();
            }
        });
        updateButtons();

        getContentResolver().registerContentObserver(MediaContentJob.MEDIA_URI, true,
                mContentObserver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == REQ_PHOTOS_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PhotosContentJob.scheduleJob(MediaContentObserver.this);
                updateButtons();
            }
        }
    }

    void updateButtons() {
        if (MediaContentJob.isScheduled(this)) {
            mScheduleMediaJob.setEnabled(false);
            mCancelMediaJob.setEnabled(true);
        } else {
            mScheduleMediaJob.setEnabled(true);
            mCancelMediaJob.setEnabled(false);
        }
        if (PhotosContentJob.isScheduled(this)) {
            mSchedulePhotosJob.setEnabled(false);
            mCancelPhotosJob.setEnabled(true);
        } else {
            mSchedulePhotosJob.setEnabled(true);
            mCancelPhotosJob.setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mContentObserver);
    }

}
