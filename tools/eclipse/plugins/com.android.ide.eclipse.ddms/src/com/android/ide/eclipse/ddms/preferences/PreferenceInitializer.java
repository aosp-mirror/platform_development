/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.ddms.preferences;

import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.ide.eclipse.ddms.views.DeviceView.HProfHandler;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmuilib.DdmUiPreferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public final static String ATTR_LOG_LEVEL =
        DdmsPlugin.PLUGIN_ID + ".logLevel"; //$NON-NLS-1$

    public final static String ATTR_DEBUG_PORT_BASE =
        DdmsPlugin.PLUGIN_ID + ".adbDebugBasePort"; //$NON-NLS-1$

    public final static String ATTR_SELECTED_DEBUG_PORT =
        DdmsPlugin.PLUGIN_ID + ".debugSelectedPort"; //$NON-NLS-1$

    public final static String ATTR_DEFAULT_THREAD_UPDATE =
        DdmsPlugin.PLUGIN_ID + ".defaultThreadUpdateEnabled"; //$NON-NLS-1$

    public final static String ATTR_DEFAULT_HEAP_UPDATE =
        DdmsPlugin.PLUGIN_ID + ".defaultHeapUpdateEnabled"; //$NON-NLS-1$

    public final static String ATTR_THREAD_INTERVAL =
        DdmsPlugin.PLUGIN_ID + ".threadStatusInterval"; //$NON-NLS-1$

    public final static String ATTR_IMAGE_SAVE_DIR =
        DdmsPlugin.PLUGIN_ID + ".imageSaveDir"; //$NON-NLS-1$

    public final static String ATTR_LAST_IMAGE_SAVE_DIR =
        DdmsPlugin.PLUGIN_ID + ".lastImageSaveDir"; //$NON-NLS-1$

    public final static String ATTR_LOGCAT_FONT =
        DdmsPlugin.PLUGIN_ID + ".logcatFont"; //$NON-NLS-1$

    public final static String ATTR_HPROF_ACTION =
        DdmsPlugin.PLUGIN_ID + ".hprofAction"; //$NON-NLS-1$

    public final static String ATTR_TIME_OUT =
        DdmsPlugin.PLUGIN_ID + ".timeOut"; //$NON-NLS-1$

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
     * #initializeDefaultPreferences()
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = DdmsPlugin.getDefault().getPreferenceStore();

        store.setDefault(ATTR_DEBUG_PORT_BASE, DdmPreferences.DEFAULT_DEBUG_PORT_BASE);

        store.setDefault(ATTR_SELECTED_DEBUG_PORT, DdmPreferences.DEFAULT_SELECTED_DEBUG_PORT);

        store.setDefault(ATTR_DEFAULT_THREAD_UPDATE, DdmPreferences.DEFAULT_INITIAL_THREAD_UPDATE);
        store.setDefault(ATTR_DEFAULT_HEAP_UPDATE,
                DdmPreferences.DEFAULT_INITIAL_HEAP_UPDATE);

        store.setDefault(ATTR_THREAD_INTERVAL, DdmUiPreferences.DEFAULT_THREAD_REFRESH_INTERVAL);

        String homeDir = System.getProperty("user.home"); //$NON-NLS-1$
        store.setDefault(ATTR_IMAGE_SAVE_DIR, homeDir);

        store.setDefault(ATTR_LOG_LEVEL, DdmPreferences.DEFAULT_LOG_LEVEL.getStringValue());

        store.setDefault(ATTR_LOGCAT_FONT,
                new FontData("Courier", 10, SWT.NORMAL).toString()); //$NON-NLS-1$

        store.setDefault(ATTR_HPROF_ACTION, HProfHandler.ACTION_OPEN);

        store.setDefault(ATTR_TIME_OUT, DdmPreferences.DEFAULT_TIMEOUT);
    }

    /**
     * Initializes the preferences of ddmlib and ddmuilib with values from the eclipse store.
     */
    public synchronized static void setupPreferences() {
        IPreferenceStore store = DdmsPlugin.getDefault().getPreferenceStore();

        DdmPreferences.setDebugPortBase(store.getInt(ATTR_DEBUG_PORT_BASE));
        DdmPreferences.setSelectedDebugPort(store.getInt(ATTR_SELECTED_DEBUG_PORT));
        DdmPreferences.setLogLevel(store.getString(ATTR_LOG_LEVEL));
        DdmPreferences.setInitialThreadUpdate(store.getBoolean(ATTR_DEFAULT_THREAD_UPDATE));
        DdmPreferences.setInitialHeapUpdate(store.getBoolean(ATTR_DEFAULT_HEAP_UPDATE));
        DdmUiPreferences.setThreadRefreshInterval(store.getInt(ATTR_THREAD_INTERVAL));
        DdmPreferences.setTimeOut(store.getInt(ATTR_TIME_OUT));
    }
}
