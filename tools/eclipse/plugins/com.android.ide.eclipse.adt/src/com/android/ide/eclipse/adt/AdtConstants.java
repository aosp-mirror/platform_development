/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt;

import com.android.ide.eclipse.adt.project.internal.AndroidClasspathContainerInitializer;


/**
 * Constant definition class.<br>
 * <br>
 * Most constants have a prefix defining the content.
 * <ul>
 * <li><code>WS_</code> Workspace path constant. Those are absolute paths,
 * from the project root.</li>
 * <li><code>OS_</code> OS path constant. These paths are different depending on the platform.</li>
 * <li><code>FN_</code> File name constant.</li>
 * <li><code>FD_</code> Folder name constant.</li>
 * <li><code>MARKER_</code> Resource Marker Ids constant.</li>
 * <li><code>EXT_</code> File extension constant. This does NOT include a dot.</li>
 * <li><code>DOT_</code> File extension constant. This start with a dot.</li>
 * <li><code>RE_</code> Regexp constant.</li>
 * <li><code>BUILD_</code> Build verbosity level constant. To use with
 * <code>AdtPlugin.printBuildToConsole()</code></li>
 * </ul>
 */
public class AdtConstants {
    /** Generic marker for ADT errors. */
    public final static String MARKER_ADT = AdtPlugin.PLUGIN_ID + ".adtProblem"; //$NON-NLS-1$

    /** Marker for Android Target errors.
     * This is not cleared on each like other markers. Instead, it's cleared
     * when an {@link AndroidClasspathContainerInitializer} has succeeded in creating an
     * AndroidClasspathContainer */
    public final static String MARKER_TARGET = AdtPlugin.PLUGIN_ID + ".targetProblem"; //$NON-NLS-1$

    /** Build verbosity "Always". Those messages are always displayed. */
    public final static int BUILD_ALWAYS = 0;

    /** Build verbosity level "Normal" */
    public final static int BUILD_NORMAL = 1;

    /** Build verbosity level "Verbose". Those messages are only displayed in verbose mode */
    public final static int BUILD_VERBOSE = 2;

}
