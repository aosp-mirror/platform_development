/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddms;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.DebugPortManager;
import com.android.ddmlib.Log;
import com.android.sdkstats.SdkStatsService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;


/**
 * Start the UI and network.
 */
public class Main {

    /** User visible version number. */
    public static final String VERSION = "0.8.1";

    public Main() {
    }

    /*
     * If a thread bails with an uncaught exception, bring the whole
     * thing down.
     */
    private static class UncaughtHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e) {
            Log.e("ddms", "shutting down due to uncaught exception");

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.e("ddms", sw.toString());

            System.exit(1);
        }
    }

    /**
     * Parse args, start threads.
     */
    public static void main(String[] args) {
        // In order to have the AWT/SWT bridge work on Leopard, we do this little hack.
        String os = System.getProperty("os.name"); //$NON-NLS-1$
        if (os.startsWith("Mac OS")) { //$NON-NLS-1$
            RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
            System.setProperty(
                    "JAVA_STARTED_ON_FIRST_THREAD_" + (rt.getName().split("@"))[0], //$NON-NLS-1$
                    "1"); //$NON-NLS-1$
        }
        
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtHandler());

        // load prefs and init the default values
        PrefsDialog.init();

        Log.d("ddms", "Initializing");

        // the "ping" argument means to check in with the server and exit
        // the application name and version number must also be supplied
        if (args.length >= 3 && args[0].equals("ping")) {
            SdkStatsService.ping(args[1], args[2], null);
            return;
        } else if (args.length > 0) {
            Log.e("ddms", "Unknown argument: " + args[0]);
            System.exit(1);
        }

        // ddms itself is wanted: send a ping for ourselves
        SdkStatsService.ping("ddms", VERSION, null);  //$NON-NLS-1$

        DebugPortManager.setProvider(DebugPortProvider.getInstance());

        // create the three main threads
        UIThread ui = UIThread.getInstance();

        try {
            ui.runUI();
        } finally {
            PrefsDialog.save();
    
            AndroidDebugBridge.terminate();
        }

        Log.d("ddms", "Bye");
        
        // this is kinda bad, but on MacOS the shutdown doesn't seem to finish because of
        // a thread called AWT-Shutdown. This will help while I track this down.
        System.exit(0);
    }
}
