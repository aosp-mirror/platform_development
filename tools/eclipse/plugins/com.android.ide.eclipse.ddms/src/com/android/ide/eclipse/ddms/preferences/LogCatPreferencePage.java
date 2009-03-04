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
import com.android.ide.eclipse.ddms.views.LogCatView;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference Pane for LogCat.
 */
public class LogCatPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    public LogCatPreferencePage() {
        super(GRID);
        setPreferenceStore(DdmsPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        FontFieldEditor ffe = new FontFieldEditor(PreferenceInitializer.ATTR_LOGCAT_FONT,
                "Display Font:", getFieldEditorParent());
        addField(ffe);

        Preferences prefs = DdmsPlugin.getDefault().getPluginPreferences();
        prefs.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                // get the name of the property that changed.
                String property = event.getProperty();

                if (PreferenceInitializer.ATTR_LOGCAT_FONT.equals(property)) {
                    try {
                        FontData fdat = new FontData((String)event.getNewValue());
                        LogCatView.setFont(new Font(getFieldEditorParent().getDisplay(), fdat));
                    } catch (IllegalArgumentException e) {
                        // Looks like the data from the store is not valid.
                        // We do nothing (default font will be used).
                    } catch (SWTError e2) {
                        // Looks like the Font() constructor failed.
                        // We do nothing in this case, the logcat view will use the default font.
                    }
                }
            }
        });
    }

    public void init(IWorkbench workbench) {
    }
}
