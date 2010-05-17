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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;

import com.android.monkeyrunner.doc.MonkeyRunnerExported;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collection of useful utilities function for interacting with the Jython interpreter.
 */
public final class JythonUtils {
    private static final Logger LOG = Logger.getLogger(JythonUtils.class.getCanonicalName());
    private JythonUtils() { }

    /**
     * Mapping of PyObject classes to the java class we want to convert them to.
     */
    private static final Map<Class<? extends PyObject>, Class<?>> PYOBJECT_TO_JAVA_OBJECT_MAP;
    static {
        Builder<Class<? extends PyObject>, Class<?>> builder = ImmutableMap.builder();

        builder.put(PyString.class, String.class);
        // What python calls float, most people call double
        builder.put(PyFloat.class, Double.class);
        builder.put(PyInteger.class, Integer.class);

        PYOBJECT_TO_JAVA_OBJECT_MAP = builder.build();
    }

    /**
     * Utility method to be called from Jython bindings to give proper handling of keyword and
     * positional arguments.
     *
     * @param args the PyObject arguments from the binding
     * @param kws the keyword arguments from the binding
     * @return an ArgParser for this binding, or null on error
     */
    public static ArgParser createArgParser(PyObject[] args, String[] kws) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Up 2 levels in the current stack to give us the calling function
        StackTraceElement element = stackTrace[2];

        String methodName = element.getMethodName();
        String className = element.getClassName();

        Class<?> clz;
        try {
            clz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Got exception: ", e);
            return null;
        }

        Method m;

        try {
            m = clz.getMethod(methodName, PyObject[].class, String[].class);
        } catch (SecurityException e) {
            LOG.log(Level.SEVERE, "Got exception: ", e);
            return null;
        } catch (NoSuchMethodException e) {
            LOG.log(Level.SEVERE, "Got exception: ", e);
            return null;
        }

        MonkeyRunnerExported annotation = m.getAnnotation(MonkeyRunnerExported.class);
        return new ArgParser(methodName, args, kws,
                annotation.args());
    }

    /**
     * Get a python floating point value from an ArgParser.
     *
     * @param ap the ArgParser to get the value from.
     * @param position the position in the parser
     * @return the double value
     */
    public static double getFloat(ArgParser ap, int position) {
        PyObject arg = ap.getPyObject(position);

        if (Py.isInstance(arg, PyFloat.TYPE)) {
            return ((PyFloat) arg).asDouble();
        }
        if (Py.isInstance(arg, PyInteger.TYPE)) {
            return ((PyInteger) arg).asDouble();
        }
        throw Py.TypeError("Unable to parse argument: " + position);
    }

    /**
     * Get a list of arguments from an ArgParser.
     *
     * @param ap the ArgParser
     * @param position the position in the parser to get the argument from
     * @return a list of those items
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(ArgParser ap, int position) {
        List<Object> ret = Lists.newArrayList();
        // cast is safe as getPyObjectbyType ensures it
        PyList array = (PyList) ap.getPyObjectByType(position, PyList.TYPE);
        for (int x = 0; x < array.__len__(); x++) {
            PyObject item = array.__getitem__(x);

            Class<?> javaClass = PYOBJECT_TO_JAVA_OBJECT_MAP.get(item.getClass());
            if (javaClass != null) {
                ret.add(item.__tojava__(javaClass));
            }
        }
        return ret;
    }

    /**
     * Get a dictionary from an ArgParser.  For ease of use, key types are always coerced to
     * strings.  If key type cannot be coeraced to string, an exception is raised.
     *
     * @param ap the ArgParser to work with
     * @param position the position in the parser to get.
     * @return a Map mapping the String key to the value
     */
    public static Map<String, Object> getMap(ArgParser ap, int position) {
        Map<String, Object> ret = Maps.newHashMap();
        // cast is safe as getPyObjectbyType ensures it
        PyDictionary dict = (PyDictionary) ap.getPyObjectByType(position, PyDictionary.TYPE);
        PyList items = dict.items();
        for (int x = 0; x < items.__len__(); x++) {
            // It's a list of tuples
            PyTuple item = (PyTuple) items.__getitem__(x);
            // We call str(key) on the key to get the string and then convert it to the java string.
            String key = (String) item.__getitem__(0).__str__().__tojava__(String.class);
            PyObject value = item.__getitem__(1);

            // Look up the conversion type and convert the value
            Class<?> javaClass = PYOBJECT_TO_JAVA_OBJECT_MAP.get(value.getClass());
            if (javaClass != null) {
                ret.put(key, value.__tojava__(javaClass));
            }
        }
        return ret;
    }
}
