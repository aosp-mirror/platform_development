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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;

import com.android.monkeyrunner.adb.AdbBackend;
import com.android.monkeyrunner.stub.StubBackend;

import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
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
    private static final String MONKEY_RUNNER_MAIN_MANIFEST_NAME = "MonkeyRunnerStartupRunner";

    private final MonkeyRunnerBackend backend;
    private final MonkeyRunnerOptions options;

    public MonkeyRunnerStarter(MonkeyRunnerOptions options) {
        this.options = options;
        this.backend = MonkeyRunnerStarter.createBackendByName(options.getBackendName());
        if (this.backend == null) {
           throw new RuntimeException("Unknown backend");
        }
    }


    /**
     * Creates a specific backend by name.
     *
     * @param backendName the name of the backend to create
     * @return the new backend, or null if none were found.
     */
    public static MonkeyRunnerBackend createBackendByName(String backendName) {
        if ("adb".equals(backendName)) {
            return new AdbBackend();
        } else if ("stub".equals(backendName)) {
            return new StubBackend();
        } else {
            return null;
        }
    }

    private int run() {
        MonkeyRunner.setBackend(backend);
        Map<String, Predicate<PythonInterpreter>> plugins = handlePlugins();
        int error = ScriptRunner.run(options.getScriptFile().getAbsolutePath(),
            options.getArguments(), plugins);
        backend.shutdown();
        MonkeyRunner.setBackend(null);
        return error;
    }

    private Predicate<PythonInterpreter> handlePlugin(File f) {
        JarFile jarFile;
        try {
            jarFile = new JarFile(f);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to open plugin file.  Is it a jar file? " +
                    f.getAbsolutePath(), e);
            return Predicates.alwaysFalse();
        }
        Manifest manifest;
        try {
            manifest = jarFile.getManifest();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to get manifest file from jar: " +
                    f.getAbsolutePath(), e);
            return Predicates.alwaysFalse();
        }
        Attributes mainAttributes = manifest.getMainAttributes();
        String pluginClass = mainAttributes.getValue(MONKEY_RUNNER_MAIN_MANIFEST_NAME);
        if (pluginClass == null) {
            // No main in this plugin, so it always succeeds.
            return Predicates.alwaysTrue();
        }
        URL url;
        try {
            url =  f.toURI().toURL();
        } catch (MalformedURLException e) {
            LOG.log(Level.SEVERE, "Unable to convert file to url " + f.getAbsolutePath(),
                    e);
            return Predicates.alwaysFalse();
        }
        URLClassLoader classLoader = new URLClassLoader(new URL[] { url },
                ClassLoader.getSystemClassLoader());
        Class<?> clz;
        try {
            clz = Class.forName(pluginClass, true, classLoader);
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Unable to load the specified plugin: " + pluginClass, e);
            return Predicates.alwaysFalse();
        }
        Object loadedObject;
        try {
            loadedObject = clz.newInstance();
        } catch (InstantiationException e) {
            LOG.log(Level.SEVERE, "Unable to load the specified plugin: " + pluginClass, e);
            return Predicates.alwaysFalse();
        } catch (IllegalAccessException e) {
            LOG.log(Level.SEVERE, "Unable to load the specified plugin " +
                    "(did you make it public?): " + pluginClass, e);
            return Predicates.alwaysFalse();
        }
        // Cast it to the right type
        if (loadedObject instanceof Runnable) {
            final Runnable run = (Runnable) loadedObject;
            return new Predicate<PythonInterpreter>() {
                public boolean apply(PythonInterpreter i) {
                    run.run();
                    return true;
                }
            };
        } else if (loadedObject instanceof Predicate<?>) {
            return (Predicate<PythonInterpreter>) loadedObject;
        } else {
            LOG.severe("Unable to coerce object into correct type: " + pluginClass);
            return Predicates.alwaysFalse();
        }
    }

    private Map<String, Predicate<PythonInterpreter>> handlePlugins() {
        ImmutableMap.Builder<String, Predicate<PythonInterpreter>> builder = ImmutableMap.builder();
        for (File f : options.getPlugins()) {
            builder.put(f.getAbsolutePath(), handlePlugin(f));
        }
        return builder.build();
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
        MonkeyRunnerOptions options = MonkeyRunnerOptions.processOptions(args);

        // logging property files are difficult
        replaceAllLogFormatters(MonkeyFormatter.DEFAULT_INSTANCE);

        if (options == null) {
            return;
        }

        MonkeyRunnerStarter runner = new MonkeyRunnerStarter(options);
        int error = runner.run();

        // This will kill any background threads as well.
        System.exit(error);
    }
}
