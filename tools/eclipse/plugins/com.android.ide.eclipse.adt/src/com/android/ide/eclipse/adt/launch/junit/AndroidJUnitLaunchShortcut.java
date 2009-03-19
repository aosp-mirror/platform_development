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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;

/**
 * Launch shortcut to launch debug/run Android JUnit configuration directly.
 */
public class AndroidJUnitLaunchShortcut extends JUnitLaunchShortcut {

    @Override
    protected String getLaunchConfigurationTypeId() {
        return "com.android.ide.eclipse.adt.junit.launchConfigurationType"; //$NON-NLS-1$
    }

    /**
     * Creates a default Android JUnit launch configuration. Sets the instrumentation runner to the
     * first instrumentation found in the AndroidManifest.  
     */
    @Override
    protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element)
            throws CoreException {
        ILaunchConfigurationWorkingCopy config = super.createLaunchConfiguration(element);
        IProject project = element.getResource().getProject();
        String[] instrumentations = 
            AndroidJUnitLaunchConfigDelegate.getInstrumentationsForProject(project);
        if (instrumentations != null && instrumentations.length > 0) {
            // just pick the first runner
            config.setAttribute(AndroidJUnitLaunchConfigDelegate.ATTR_INSTR_NAME, 
                    instrumentations[0]);
        }
        AndroidJUnitLaunchConfigDelegate.setJUnitDefaults(config);

        return config;
    }
}
