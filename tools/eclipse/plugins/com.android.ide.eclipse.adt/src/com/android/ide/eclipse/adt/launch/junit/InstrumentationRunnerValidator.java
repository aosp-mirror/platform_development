/*
 * Copyright (C) 2009 The Android Open Source Project
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
package com.android.ide.eclipse.adt.launch.junit;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.AndroidManifestParser;
import com.android.ide.eclipse.common.project.AndroidManifestParser.Instrumentation;
import com.android.ide.eclipse.common.project.BaseProjectHelper;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Provides validation for Android instrumentation test runner 
 */
class InstrumentationRunnerValidator {
    private final IJavaProject mJavaProject;
    private String[] mInstrumentationNames = null;
    private boolean mHasRunnerLibrary = false;
    
    static final String INSTRUMENTATION_OK = null;

    /**
     * Initializes the InstrumentationRunnerValidator.
     * 
     * @param javaProject the {@link IJavaProject} for the Android project to validate
     */
    InstrumentationRunnerValidator(IJavaProject javaProject) {
        mJavaProject = javaProject;
        try {
            AndroidManifestParser manifestParser = AndroidManifestParser.parse(javaProject, 
                    null /* errorListener */, true /* gatherData */, false /* markErrors */);
            init(manifestParser);
        } catch (CoreException e) {
            AdtPlugin.printErrorToConsole(javaProject.getProject(), "ERROR: Failed to parse %1$s",
                    AndroidConstants.FN_ANDROID_MANIFEST);
        }
    }

    /**
     * Initializes the InstrumentationRunnerValidator.
     * 
     * @param project the {@link IProject} for the Android project to validate
     * @throws CoreException if a fatal error occurred in initialization
     */
    InstrumentationRunnerValidator(IProject project) throws CoreException {
        this(BaseProjectHelper.getJavaProject(project));
    }

    /**
     * Initializes the InstrumentationRunnerValidator with an existing {@link AndroidManifestParser}
     * 
     * @param javaProject the {@link IJavaProject} for the Android project to validate
     * @param manifestParser the {@link AndroidManifestParser} for the Android project
     */
    InstrumentationRunnerValidator(IJavaProject javaProject, AndroidManifestParser manifestParser) {
        mJavaProject = javaProject;
        init(manifestParser);
    }
    
    private void init(AndroidManifestParser manifestParser) {
        Instrumentation[] instrumentations = manifestParser.getInstrumentations();
        mInstrumentationNames = new String[instrumentations.length];
        for (int i = 0; i < instrumentations.length; i++) {
            mInstrumentationNames[i] = instrumentations[i].getName();
        }
        mHasRunnerLibrary = hasTestRunnerLibrary(manifestParser);
    }
    
    /**
     * Helper method to determine if given manifest has a <code>AndroidConstants.LIBRARY_TEST_RUNNER
     * </code> library reference
     *
     * @param manifestParser the {@link AndroidManifestParser} to search
     * @return true if test runner library found, false otherwise
     */
    private boolean hasTestRunnerLibrary(AndroidManifestParser manifestParser) {
       for (String lib : manifestParser.getUsesLibraries()) {
           if (lib.equals(AndroidConstants.LIBRARY_TEST_RUNNER)) {
               return true;
           }
       }
       return false;
    }

    /**
     * Return the set of instrumentation names for the Android project.
     * 
     * @return <code>null</code if error occurred parsing instrumentations, otherwise returns array
     * of instrumentation class names
     */
    String[] getInstrumentationNames() {
        return mInstrumentationNames;
    }

    /**
     * Helper method to get the first instrumentation that can be used as a test runner.
     * 
     * @return fully qualified instrumentation class name. <code>null</code> if no valid
     * instrumentation can be found.
     */
    String getValidInstrumentationTestRunner() {
        for (String instrumentation : getInstrumentationNames()) {
            if (validateInstrumentationRunner(instrumentation) == INSTRUMENTATION_OK) {
                return instrumentation;
            }
        }
        return null;
    }

    /**
     * Helper method to determine if specified instrumentation can be used as a test runner
     * 
     * @param instrumentation the instrumentation class name to validate. Assumes this
     *   instrumentation is one of {@link #getInstrumentationNames()}
     * @return <code>INSTRUMENTATION_OK</code> if valid, otherwise returns error message
     */
    String validateInstrumentationRunner(String instrumentation) {
        if (!mHasRunnerLibrary) {
            return String.format("The application does not declare uses-library %1$s", 
                    AndroidConstants.LIBRARY_TEST_RUNNER);
        }
        // check if this instrumentation is the standard test runner
        if (!instrumentation.equals(AndroidConstants.CLASS_INSTRUMENTATION_RUNNER)) {
            // check if it extends the standard test runner
            String result = BaseProjectHelper.testClassForManifest(mJavaProject,
                    instrumentation, AndroidConstants.CLASS_INSTRUMENTATION_RUNNER, true);
             if (result != BaseProjectHelper.TEST_CLASS_OK) {
                return String.format("The instrumentation runner must be of type %s", 
                        AndroidConstants.CLASS_INSTRUMENTATION_RUNNER);
             }
        }
        return INSTRUMENTATION_OK;
    }
}
