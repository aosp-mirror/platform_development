/*
 * Copyright (C) 2008 The Android Open Source Project
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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Settings page for launch related preferences.
 */
public class LaunchPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {
    
    public LaunchPreferencePage() {
        super(GRID);
        setPreferenceStore(AdtPlugin.getDefault().getPreferenceStore());
        setDescription(Messages.LaunchPreferencePage_Title);
    }

    @Override
    protected void createFieldEditors() {
        addField(new StringFieldEditor(AdtPlugin.PREFS_EMU_OPTIONS,
                Messages.LaunchPreferencePage_Default_Emu_Options, getFieldEditorParent()));

        addField(new StringFieldEditor(AdtPlugin.PREFS_HOME_PACKAGE,
                Messages.LaunchPreferencePage_Default_HOME_Package, getFieldEditorParent()));
    }

    public void init(IWorkbench workbench) {
        // pass
    }

}
