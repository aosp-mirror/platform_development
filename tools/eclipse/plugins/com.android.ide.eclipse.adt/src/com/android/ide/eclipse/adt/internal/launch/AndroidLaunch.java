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

package com.android.ide.eclipse.adt.internal.launch;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ISourceLocator;

/**
 * Custom implementation of Launch to allow access to the LaunchManager
 *
 */
public class AndroidLaunch extends Launch {

    /**
     * Basic constructor does nothing special
     * @param launchConfiguration
     * @param mode
     * @param locator
     */
    public AndroidLaunch(ILaunchConfiguration launchConfiguration, String mode,
            ISourceLocator locator) {
        super(launchConfiguration, mode, locator);
    }

    /** Stops the launch, and removes it from the launch manager */
    public void stopLaunch() {
        ILaunchManager mgr = getLaunchManager();

        if (canTerminate()) {
            try {
                terminate();
            } catch (DebugException e) {
                // well looks like we couldn't stop it. nothing else to be
                // done really
            }
        }
        // remove the launch
        mgr.removeLaunch(this);
    }
}
