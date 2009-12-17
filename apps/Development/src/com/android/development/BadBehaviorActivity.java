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
import android.os.Bundle;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class BadBehaviorActivity extends Activity {
    static class BadBehaviorException extends RuntimeException {
        BadBehaviorException() {
            super("Whatcha gonna do, whatcha gonna do",
                    new IllegalStateException("When they come for you"));
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.bad_behavior);

        Button crash_system = (Button) findViewById(R.id.bad_behavior_crash_system);
        crash_system.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    IBinder b = ServiceManager.getService(POWER_SERVICE);
                    IPowerManager pm = IPowerManager.Stub.asInterface(b);
                    pm.crash("Crashed by BadBehaviorActivity");
                } catch (RemoteException e) {
                    Log.e("BadBehavior", "Can't call IPowerManager.crash()", e);
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

        Button wtf = (Button) findViewById(R.id.bad_behavior_wtf);
        wtf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Log.wtf("BadBehavior", "Apps Behaving Badly"); }
        });

        Button anr = (Button) findViewById(R.id.bad_behavior_anr);
        anr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}
