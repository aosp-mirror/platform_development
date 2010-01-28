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

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class BadBehaviorActivity extends Activity {
    private static final String TAG = "BadBehaviorActivity";

    private static class BadBehaviorException extends RuntimeException {
        BadBehaviorException() {
            super("Whatcha gonna do, whatcha gonna do",
                    new IllegalStateException("When they come for you"));
        }
    }

    public static class BadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "in BadReceiver.onReceive() -- about to hang");
            try { Thread.sleep(20000); } catch (InterruptedException e) { Log.wtf(TAG, e); }
        }
    };

    public static class BadService extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int id) {
            Log.i(TAG, "in BadService.onStartCommand() -- about to hang");
            try { Thread.sleep(30000); } catch (InterruptedException e) { Log.wtf(TAG, e); }
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (getIntent().getBooleanExtra("anr", false)) {
            Log.i(TAG, "in ANR activity -- about to hang");
            try { Thread.sleep(20000); } catch (InterruptedException e) { Log.wtf(TAG, e); }
            finish();
            return;
        }

        setContentView(R.layout.bad_behavior);

        Button crash_system = (Button) findViewById(R.id.bad_behavior_crash_system);
        crash_system.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    IBinder b = ServiceManager.getService(POWER_SERVICE);
                    IPowerManager pm = IPowerManager.Stub.asInterface(b);
                    pm.crash("Crashed by BadBehaviorActivity");
                } catch (RemoteException e) {
                    Log.e(TAG, "Can't call IPowerManager.crash()", e);
                }
            }
        });

        Button crash_main = (Button) findViewById(R.id.bad_behavior_crash_main);
        crash_main.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { throw new BadBehaviorException(); }
        });

        Button crash_thread = (Button) findViewById(R.id.bad_behavior_crash_thread);
        crash_thread.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread() {
                    @Override
                    public void run() { throw new BadBehaviorException(); }
                }.start();
            }
        });

        Button crash_native = (Button) findViewById(R.id.bad_behavior_crash_native);
        crash_native.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // For some reason, the JVM needs two of these to get the hint
                Log.i(TAG, "Native crash pressed -- about to kill -11 self");
                Process.sendSignal(Process.myPid(), 11);
                Process.sendSignal(Process.myPid(), 11);
                Log.i(TAG, "Finished kill -11, should be dead or dying");
            }
        });

        Button wtf = (Button) findViewById(R.id.bad_behavior_wtf);
        wtf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Log.wtf(TAG, "Apps Behaving Badly"); }
        });

        Button anr = (Button) findViewById(R.id.bad_behavior_anr);
        anr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "ANR pressed -- about to hang");
                try { Thread.sleep(20000); } catch (InterruptedException e) { Log.wtf(TAG, e); }
            }
        });

        Button anr_activity = (Button) findViewById(R.id.bad_behavior_anr_activity);
        anr_activity.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(BadBehaviorActivity.this, BadBehaviorActivity.class);
                Log.i(TAG, "ANR activity pressed -- about to launch");
                startActivity(intent.putExtra("anr", true));
            }
        });

        Button anr_broadcast = (Button) findViewById(R.id.bad_behavior_anr_broadcast);
        anr_broadcast.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "ANR broadcast pressed -- about to send");
                sendOrderedBroadcast(new Intent("com.android.development.BAD_BEHAVIOR"), null);
            }
        });

        Button anr_service = (Button) findViewById(R.id.bad_behavior_anr_service);
        anr_service.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "ANR service pressed -- about to start");
                startService(new Intent(BadBehaviorActivity.this, BadService.class));
            }
        });
    }
}
