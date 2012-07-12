/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.development;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class CacheAbuser extends Activity {
    Button mStartInternalAbuse;
    Button mStartSlowInternalAbuse;
    Button mStartExternalAbuse;
    Button mStartSlowExternalAbuse;
    Button mStopAbuse;

    AsyncTask<Void, Void, Void> mInternalAbuseTask;
    AsyncTask<Void, Void, Void> mExternalAbuseTask;

    static class AbuseTask extends AsyncTask<Void, Void, Void> {
        final File mBaseDir;
        final boolean mQuick;
        final byte[] mBuffer;

        AbuseTask(File cacheDir, boolean quick) {
            File dir = new File(cacheDir, quick ? "quick" : "slow");
            mBaseDir = new File(dir, Long.toString(System.currentTimeMillis()));
            mQuick = quick;
            mBuffer = quick ? new byte[1024*1024] : new byte[1024];
        }

        @Override
        protected Void doInBackground(Void... params) {
            long num = 0;
            while (!isCancelled()) {
                long dir1num = num/100;
                long dir2num = num%100;
                File dir = new File(mBaseDir, Long.toString(dir1num));
                File file = new File(dir, Long.toString(dir2num));
                FileOutputStream fos = null;
                try {
                    dir.mkdirs();
                    fos = new FileOutputStream(file, false);
                    fos.write(mBuffer);
                } catch (IOException e) {
                    Log.w("CacheAbuser", "Write failed to " + file + ": " + e);
                    try {
                        wait(5*1000);
                    } catch (InterruptedException e1) {
                    }
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                    }
                }
                num++;
            }
            return null;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.cache_abuser);

        mStartInternalAbuse = (Button) findViewById(R.id.start_internal_abuse);
        mStartInternalAbuse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mInternalAbuseTask == null) {
                    mInternalAbuseTask = new AbuseTask(getCacheDir(), true);
                    mInternalAbuseTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    updateButtonState();
                }
            }
        });

        mStartSlowInternalAbuse = (Button) findViewById(R.id.start_slow_internal_abuse);
        mStartSlowInternalAbuse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mInternalAbuseTask == null) {
                    mInternalAbuseTask = new AbuseTask(getCacheDir(), false);
                    mInternalAbuseTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    updateButtonState();
                }
            }
        });

        mStartExternalAbuse = (Button) findViewById(R.id.start_external_abuse);
        mStartExternalAbuse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mExternalAbuseTask == null) {
                    mExternalAbuseTask = new AbuseTask(getExternalCacheDir(), true);
                    mExternalAbuseTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    updateButtonState();
                }
            }
        });

        mStartSlowExternalAbuse = (Button) findViewById(R.id.start_slow_external_abuse);
        mStartSlowExternalAbuse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mExternalAbuseTask == null) {
                    mExternalAbuseTask = new AbuseTask(getExternalCacheDir(), false);
                    mExternalAbuseTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    updateButtonState();
                }
            }
        });

        mStopAbuse = (Button) findViewById(R.id.stop_abuse);
        mStopAbuse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopAbuse();
            }
        });

        updateButtonState();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateButtonState();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopAbuse();
    }

    void stopAbuse() {
        if (mInternalAbuseTask != null) {
            mInternalAbuseTask.cancel(false);
            mInternalAbuseTask = null;
        }
        if (mExternalAbuseTask != null) {
            mExternalAbuseTask.cancel(false);
            mExternalAbuseTask = null;
        }
        updateButtonState();
    }

    void updateButtonState() {
        mStartInternalAbuse.setEnabled(mInternalAbuseTask == null);
        mStartSlowInternalAbuse.setEnabled(mInternalAbuseTask == null);
        mStartExternalAbuse.setEnabled(mExternalAbuseTask == null);
        mStartSlowExternalAbuse.setEnabled(mExternalAbuseTask == null);
        mStopAbuse.setEnabled(mInternalAbuseTask != null
                || mExternalAbuseTask != null);
    }
}
