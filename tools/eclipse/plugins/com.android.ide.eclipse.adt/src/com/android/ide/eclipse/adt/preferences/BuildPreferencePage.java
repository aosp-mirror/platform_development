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

import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for build options.
 *
 */
public class BuildPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    final static String BUILD_STR_SILENT = "silent"; //$NON-NLS-1$
    final static String BUILD_STR_NORMAL = "normal"; //$NON-NLS-1$
    final static String BUILD_STR_VERBOSE = "verbose"; //$NON-NLS-1$

    public BuildPreferencePage() {
        super(GRID);
        setPreferenceStore(AdtPlugin.getDefault().getPreferenceStore());
        setDescription(Messages.BuildPreferencePage_Title);
    }

    public static int getBuildLevel(String buildPrefValue) {
        if (BUILD_STR_SILENT.equals(buildPrefValue)) {
            return AdtConstants.BUILD_ALWAYS;
        } else if (BUILD_STR_VERBOSE.equals(buildPrefValue)) {
            return AdtConstants.BUILD_VERBOSE;
        }

        return AdtConstants.BUILD_NORMAL;
    }

    @Override
    protected void createFieldEditors() {
        addField(new BooleanFieldEditor(AdtPlugin.PREFS_RES_AUTO_REFRESH,
                Messages.BuildPreferencePage_Auto_Refresh_Resources_on_Build,
                getFieldEditorParent()));

        RadioGroupFieldEditor rgfe = new RadioGroupFieldEditor(
                AdtPlugin.PREFS_BUILD_VERBOSITY,
                Messages.BuildPreferencePage_Build_Output, 1, new String[][] {
                    { Messages.BuildPreferencePage_Silent, BUILD_STR_SILENT },
                    { Messages.BuildPreferencePage_Normal, BUILD_STR_NORMAL },
                    { Messages.BuildPreferencePage_Verbose, BUILD_STR_VERBOSE }
                    },
                getFieldEditorParent(), true);
        addField(rgfe);

        addField(new ReadOnlyFieldEditor(AdtPlugin.PREFS_DEFAULT_DEBUG_KEYSTORE,
                Messages.BuildPreferencePage_Default_KeyStore, getFieldEditorParent()));

        addField(new FileFieldEditor(AdtPlugin.PREFS_CUSTOM_DEBUG_KEYSTORE,
                Messages.BuildPreferencePage_Custom_Keystore, getFieldEditorParent()));

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    /**
     * A read-only string field editor.
     */
    private static class ReadOnlyFieldEditor extends StringFieldEditor {

        public ReadOnlyFieldEditor(String name, String labelText, Composite parent) {
            super(name, labelText, parent);
        }

        @Override
        protected void createControl(Composite parent) {
            super.createControl(parent);
            
            Text control = getTextControl();
            control.setEditable(false);
        }
    }
}
