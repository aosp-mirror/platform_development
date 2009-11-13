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
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.PortFieldEditor;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    public PreferencePage() {
        super(GRID);
        setPreferenceStore(DdmsPlugin.getDefault().getPreferenceStore());
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        IntegerFieldEditor ife;

        ife = new PortFieldEditor(PreferenceInitializer.ATTR_DEBUG_PORT_BASE,
            "Base local debugger port:", getFieldEditorParent());
        addField(ife);

        BooleanFieldEditor bfe;

        bfe = new BooleanFieldEditor(PreferenceInitializer.ATTR_DEFAULT_THREAD_UPDATE,
            "Thread updates enabled by default", getFieldEditorParent());
        addField(bfe);

        bfe = new BooleanFieldEditor(PreferenceInitializer.ATTR_DEFAULT_HEAP_UPDATE,
            "Heap updates enabled by default", getFieldEditorParent());
        addField(bfe);

        ife = new IntegerFieldEditor(PreferenceInitializer.ATTR_THREAD_INTERVAL,
            "Thread status refresh interval (seconds):", getFieldEditorParent());
        ife.setValidRange(1, 60);
        addField(ife);

        ComboFieldEditor cfe = new ComboFieldEditor(PreferenceInitializer.ATTR_HPROF_ACTION,
                "HPROF Action:", new String[][] {
                    { "Save to disk", HProfHandler.ACTION_SAVE },
                    { "Open in Eclipse", HProfHandler.ACTION_OPEN },
                }, getFieldEditorParent());
        addField(cfe);

        ife = new IntegerFieldEditor(PreferenceInitializer.ATTR_TIME_OUT,
                "ADB connection time out (ms):", getFieldEditorParent());
            addField(ife);

        RadioGroupFieldEditor rgfe = new RadioGroupFieldEditor(PreferenceInitializer.ATTR_LOG_LEVEL,
                "Logging Level", 1, new String[][] {
                    { "Verbose", LogLevel.VERBOSE.getStringValue() },
                    { "Debug", LogLevel.DEBUG.getStringValue() },
                    { "Info", LogLevel.INFO.getStringValue() },
                    { "Warning", LogLevel.WARN.getStringValue() },
                    { "Error", LogLevel.ERROR.getStringValue() },
                    { "Assert", LogLevel.ASSERT.getStringValue() }
                    },
                getFieldEditorParent(), true);
        addField(rgfe);
    }

    public void init(IWorkbench workbench) {
    }
}
