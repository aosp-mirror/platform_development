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
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import com.android.monkeyrunner.doc.MonkeyRunnerExported;

import org.clearsilver.CS;
import org.clearsilver.CSFileLoader;
import org.clearsilver.HDF;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Utility class for generating inline help documentation
 */
public final class MonkeyRunnerHelp {
    private MonkeyRunnerHelp() { }

    private static final String HELP = "help";
    private static final String NAME = "name";
    private static final String DOC = "doc";
    private static final String ARGUMENT = "argument";
    private static final String RETURNS = "returns";
    private static final String TYPE = "type";

    // Enum used to describe documented types.
    private enum Type {
        ENUM, FIELD, METHOD
    }

    private static void getAllExportedClasses(Set<Field> fields,
            Set<Method> methods,
            Set<Constructor<?>> constructors,
            Set<Class<?>> enums) {
        final Set<Class<?>> classesVisited = Sets.newHashSet();
        Set<Class<?>> classesToVisit = Sets.newHashSet();
        classesToVisit.add(MonkeyRunner.class);

        Predicate<Class<?>> haventSeen = new Predicate<Class<?>>() {
            public boolean apply(Class<?> clz) {
                return !classesVisited.contains(clz);
            }
        };

        while (!classesToVisit.isEmpty()) {
            classesVisited.addAll(classesToVisit);

            List<Class<?>> newClasses = Lists.newArrayList();
            for (Class<?> clz : classesToVisit) {
                // See if the class itself is annotated and is an enum
                if (clz.isEnum() && clz.isAnnotationPresent(MonkeyRunnerExported.class)) {
                    enums.add(clz);
                }

                // Constructors
                for (Constructor<?> c : clz.getConstructors()) {
                    newClasses.addAll(Collections2.filter(Arrays.asList(c.getParameterTypes()),
                            haventSeen));
                    if (c.isAnnotationPresent(MonkeyRunnerExported.class)) {
                        constructors.add(c);
                    }
                }

                // Fields
                for (Field f : clz.getFields()) {
                    if (haventSeen.apply(f.getClass())) {
                        newClasses.add(f.getClass());
                    }
                    if (f.isAnnotationPresent(MonkeyRunnerExported.class)) {
                        fields.add(f);
                    }
                }

                // Methods
                for (Method m : clz.getMethods()) {
                    newClasses.addAll(Collections2.filter(Arrays.asList(m.getParameterTypes()),
                            haventSeen));
                    if (haventSeen.apply(m.getReturnType())) {
                        newClasses.add(m.getReturnType());
                    }

                    if (m.isAnnotationPresent(MonkeyRunnerExported.class)) {
                        methods.add(m);
                    }
                }

                // Containing classes
                for (Class<?> toAdd : clz.getClasses()) {
                    if (haventSeen.apply(toAdd)) {
                        newClasses.add(toAdd);
                    }
                }
            }

            classesToVisit.clear();
            classesToVisit.addAll(newClasses);
        }
    }

    private static Comparator<Member> MEMBER_SORTER = new Comparator<Member>() {
        public int compare(Member o1, Member o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    private static Comparator<Class<?>> CLASS_SORTER = new Comparator<Class<?>>() {
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    public static String helpString(String format) {
        // Quick check for support formats
        if ("html".equals(format) || "text".equals(format)) {
            HDF hdf = buildHelpHdf();
            CS clearsilver = new CS(hdf);
            // Set a custom file loader to load requested files from resources relative to this class.
            clearsilver.setFileLoader(new CSFileLoader() {
                public String load(HDF hdf, String filename) throws IOException {
                    return Resources.toString(Resources.getResource(MonkeyRunnerHelp.class, filename),
                            Charset.defaultCharset());
                }
            });

            // Load up the CS template file
            clearsilver.parseFile(format.toLowerCase() + ".cs");
            // And render the output
            return clearsilver.render();
        } else if ("hdf".equals(format)) {
            HDF hdf = buildHelpHdf();
            return hdf.writeString();
        }
        return "";
    }

    private static HDF buildHelpHdf() {
        HDF hdf = new HDF();

        int outputItemCount = 0;

        Set<Field> fields = Sets.newTreeSet(MEMBER_SORTER);
        Set<Method> methods = Sets.newTreeSet(MEMBER_SORTER);
        Set<Constructor<?>> constructors = Sets.newTreeSet(MEMBER_SORTER);
        Set<Class<?>> classes = Sets.newTreeSet(CLASS_SORTER);
        getAllExportedClasses(fields, methods, constructors, classes);

        for (Class<?> clz : classes) {
            String prefix = HELP + "." + outputItemCount + ".";

            hdf.setValue(prefix + NAME, clz.getCanonicalName());
            MonkeyRunnerExported annotation = clz.getAnnotation(MonkeyRunnerExported.class);
            hdf.setValue(prefix + DOC, annotation.doc());
            hdf.setValue(prefix + TYPE, Type.ENUM.name());

            // Now go through the enumeration constants
            Object[] constants = clz.getEnumConstants();
            String[] argDocs = annotation.argDocs();
            if (constants.length > 0) {
                for (int x = 0; x < constants.length; x++) {
                    String argPrefix = prefix + ARGUMENT + "." + x + ".";
                    hdf.setValue(argPrefix + NAME, constants[x].toString());
                    if (argDocs.length > x) {
                        hdf.setValue(argPrefix + DOC, argDocs[x]);
                    }
                }
            }
            outputItemCount++;
        }

        for (Method m : methods) {
            String prefix = HELP + "." + outputItemCount + ".";

            MonkeyRunnerExported annotation = m.getAnnotation(MonkeyRunnerExported.class);
            String className = m.getDeclaringClass().getCanonicalName();
            String methodName = className + "." + m.getName();
            hdf.setValue(prefix + NAME, methodName);
            hdf.setValue(prefix + DOC, annotation.doc());
            if (annotation.args().length > 0) {
                String[] argDocs = annotation.argDocs();
                String[] aargs = annotation.args();
                for (int x = 0; x < aargs.length; x++) {
                    String argPrefix = prefix + ARGUMENT + "." + x + ".";

                    hdf.setValue(argPrefix + NAME, aargs[x]);
                    if (argDocs.length > x) {
                        hdf.setValue(argPrefix + DOC, argDocs[x]);
                    }
                }
            }
            if (!"".equals(annotation.returns())) {
                hdf.setValue(prefix + RETURNS, annotation.returns());
            }
            outputItemCount++;
        }

        return hdf;
    }
}
