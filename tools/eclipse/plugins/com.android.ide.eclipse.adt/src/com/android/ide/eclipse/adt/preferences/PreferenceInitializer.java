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

package com.android.ide.eclipse.adt.preferences;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.jarutils.DebugKeyProvider;
import com.android.jarutils.DebugKeyProvider.KeytoolException;
import com.android.prefs.AndroidLocation.AndroidLocationException;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
     * #initializeDefaultPreferences()
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();

        store.setDefault(AdtPlugin.PREFS_RES_AUTO_REFRESH, true);

        store.setDefault(AdtPlugin.PREFS_BUILD_VERBOSITY, BuildPreferencePage.BUILD_STR_NORMAL);
        
        store.setDefault(AdtPlugin.PREFS_HOME_PACKAGE, "android.process.acore"); //$NON-NLS-1$
        
        try {
            store.setDefault(AdtPlugin.PREFS_DEFAULT_DEBUG_KEYSTORE,
                    DebugKeyProvider.getDefaultKeyStoreOsPath());
        } catch (KeytoolException e) {
            AdtPlugin.log(e, "Get default debug keystore path failed"); //$NON-NLS-1$
        } catch (AndroidLocationException e) {
            AdtPlugin.log(e, "Get default debug keystore path failed"); //$NON-NLS-1$
        }
    }
}
