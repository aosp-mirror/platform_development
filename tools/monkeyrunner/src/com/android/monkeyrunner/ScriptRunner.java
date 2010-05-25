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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.util.InteractiveConsole;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Runs Jython based scripts.
 */
public class ScriptRunner {
    private static final Logger LOG = Logger.getLogger(MonkeyRunnerOptions.class.getName());

    /** The "this" scope object for scripts. */
    private final Object scope;
    private final String variable;

    /** Private constructor. */
    private ScriptRunner(Object scope, String variable) {
        this.scope = scope;
        this.variable = variable;
    }

    /** Creates a new instance for the given scope object. */
    public static ScriptRunner newInstance(Object scope, String variable) {
        return new ScriptRunner(scope, variable);
    }

    /**
     * Runs the specified Jython script. First runs the initialization script to
     * preload the appropriate client library version.
     *
     * @param scriptfilename the name of the file to run.
     * @param args the arguments passed in (excluding the filename).
     * @param plugins a list of plugins to load.
     * @return the error code from running the script.
     */
    public static int run(String scriptfilename, Collection<String> args,
            Map<String, Predicate<PythonInterpreter>> plugins) {
        // Add the current directory of the script to the python.path search path.
        File f = new File(scriptfilename);

        // Adjust the classpath so jython can access the classes in the specified classpath.
        Collection<String> classpath = Lists.newArrayList(f.getParent());
        classpath.addAll(plugins.keySet());

        String[] argv = new String[args.size() + 1];
        argv[0] = f.getAbsolutePath();
        int x = 1;
        for (String arg : args) {
            argv[x++] = arg;
        }

        initPython(classpath, argv);

        PythonInterpreter python = new PythonInterpreter();

        // Now let the mains run.
        for (Map.Entry<String, Predicate<PythonInterpreter>> entry : plugins.entrySet()) {
            boolean success;
            try {
                success = entry.getValue().apply(python);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Plugin Main through an exception.", e);
                continue;
            }
            if (!success) {
                LOG.severe("Plugin Main returned error for: " + entry.getKey());
            }
        }

        // Bind __name__ to __main__ so mains will run
        python.set("__name__", "__main__");

        try {
          python.execfile(scriptfilename);
        } catch (PyException e) {
          if (Py.SystemExit.equals(e.type)) {
            // Then recover the error code so we can pass it on
            return (Integer) e.value.__tojava__(Integer.class);
          }
          // Then some other kind of exception was thrown.  Log it and return error;
          LOG.log(Level.SEVERE, "Script terminated due to an exception", e);
          return 1;
        }
        return 0;
    }

    public static void runString(String script) {
        initPython();
        PythonInterpreter python = new PythonInterpreter();
        python.exec(script);
    }

    public static Map<String, PyObject> runStringAndGet(String script, String... names) {
        return runStringAndGet(script, Arrays.asList(names));
    }

    public static Map<String, PyObject> runStringAndGet(String script, Collection<String> names) {
        initPython();
        final PythonInterpreter python = new PythonInterpreter();
        python.exec(script);

        Builder<String, PyObject> builder = ImmutableMap.builder();
        for (String name : names) {
            builder.put(name, python.get(name));
        }
        return builder.build();
    }

    private static void initPython() {
        List<String> arg = Collections.emptyList();
        initPython(arg, new String[] {""});
    }

    private static void initPython(Collection<String> pythonPath,
            String[] argv) {
        Properties props = new Properties();

        // Build up the python.path
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("java.class.path"));
        for (String p : pythonPath) {
            sb.append(":").append(p);
        }
        props.setProperty("python.path", sb.toString());

        /** Initialize the python interpreter. */
        // Default is 'message' which displays sys-package-mgr bloat
        // Choose one of error,warning,message,comment,debug
        props.setProperty("python.verbose", "error");

        PythonInterpreter.initialize(System.getProperties(), props, argv);
    }

    /**
     * Create and run a console using a new python interpreter for the test
     * associated with this instance.
     */
    public void console() throws IOException {
        initPython();
        InteractiveConsole python = new InteractiveConsole();
        initInterpreter(python, scope, variable);
        python.interact();
    }

    /**
     * Start an interactive python interpreter using the specified set of local
     * variables. Use this to interrupt a running test script with a prompt:
     *
     * @param locals
     */
    public static void console(PyObject locals) {
        initPython();
        InteractiveConsole python = new InteractiveConsole(locals);
        python.interact();
    }

    /**
     * Initialize a python interpreter.
     *
     * @param python
     * @param scope
     * @throws IOException
     */
    public static void initInterpreter(PythonInterpreter python, Object scope, String variable)
    throws IOException {
        // Store the current test case as the this variable
        python.set(variable, scope);
    }
}
