/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner;

import com.android.monkeyrunner.adb.AdbBackend;

import java.io.File;
import java.util.Enumeration;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 *  MonkeyRunner is a host side application to control a monkey instance on a
 *  device. MonkeyRunner provides some useful helper functions to control the
 *  device as well as various other methods to help script tests.  This class bootstraps
 *  MonkeyRunner.
 */
public class MonkeyRunnerStarter {
    private static final Logger LOG = Logger.getLogger(MonkeyRunnerStarter.class.getName());

    private final MonkeyRunnerBackend backend;
    private final File scriptFile;

    /**
     * Creates a specific backend by name.
     *
     * @param backendName the name of the backend to create
     * @return the new backend, or null if none were found.
     */
    public MonkeyRunnerBackend createBackendByName(String backendName) {
        if ("adb".equals(backendName)) {
            return new AdbBackend();
        } else {
            return null;
        }
    }

    public MonkeyRunnerStarter(String backendName,
            File scriptFile) {
        this.backend = createBackendByName(backendName);
        if (this.backend == null) {
            throw new RuntimeException("Unknown backend");
        }
        this.scriptFile = scriptFile;
    }

    private void run() {
        MonkeyRunner.setBackend(backend);
        ScriptRunner.run(scriptFile.getAbsolutePath());
        backend.shutdown();
        MonkeyRunner.setBackend(null);
    }

    private static final void replaceAllLogFormatters(Formatter form) {
        LogManager mgr = LogManager.getLogManager();
        Enumeration<String> loggerNames = mgr.getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String loggerName = loggerNames.nextElement();
            Logger logger = mgr.getLogger(loggerName);
            for (Handler handler : logger.getHandlers()) {
                handler.setFormatter(form);
                handler.setLevel(Level.INFO);
            }
        }
    }

    public static void main(String[] args) {
        MonkeyRunningOptions options = MonkeyRunningOptions.processOptions(args);

        // logging property files are difficult
        replaceAllLogFormatters(MonkeyFormatter.DEFAULT_INSTANCE);

        if (options == null) {
            return;
        }

        MonkeyRunnerStarter runner = new MonkeyRunnerStarter(options.getBackendName(),
                options.getScriptFile());
        runner.run();

        // This will kill any background threads as well.
        System.exit(0);
    }
}
