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
import com.android.ide.eclipse.adt.sdk.Sdk;
import com.android.ide.eclipse.adt.sdk.Sdk.ITargetChangeListener;
import com.android.sdklib.IAndroidTarget;
import com.android.sdkuilib.SdkTargetSelector;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.io.File;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class AndroidPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    private SdkDirectoryFieldEditor mDirectoryField;

    public AndroidPreferencePage() {
        super(GRID);
        setPreferenceStore(AdtPlugin.getDefault().getPreferenceStore());
        setDescription(Messages.AndroidPreferencePage_Title);
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {

        mDirectoryField = new SdkDirectoryFieldEditor(AdtPlugin.PREFS_SDK_DIR,
                Messages.AndroidPreferencePage_SDK_Location_, getFieldEditorParent());
        
        addField(mDirectoryField);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }
    
    @Override
    public void dispose() {
        super.dispose();
        
        if (mDirectoryField != null) {
            mDirectoryField.dispose();
            mDirectoryField = null;
        }
    }

    /**
     * Custom version of DirectoryFieldEditor which validates that the directory really
     * contains an SDK.
     *
     * There's a known issue here, which is really a rare edge-case: if the pref dialog is open
     * which a given sdk directory and the *content* of the directory changes such that the sdk
     * state changed (i.e. from valid to invalid or vice versa), the pref panel will display or
     * hide the error as appropriate but the pref panel will fail to validate the apply/ok buttons
     * appropriately. The easy workaround is to cancel the pref panel and enter it again.
     */
    private static class SdkDirectoryFieldEditor extends DirectoryFieldEditor {

        private SdkTargetSelector mTargetSelector;
        private TargetChangedListener mTargetChangeListener;

        public SdkDirectoryFieldEditor(String name, String labelText, Composite parent) {
            super(name, labelText, parent);
            setEmptyStringAllowed(false);
        }

        /**
         * Method declared on StringFieldEditor and overridden in DirectoryFieldEditor.
         * Checks whether the text input field contains a valid directory.
         *
         * @return True if the apply/ok button should be enabled in the pref panel
         */
        @Override
        protected boolean doCheckState() {
            String fileName = getTextControl().getText();
            fileName = fileName.trim();
            
            if (fileName.indexOf(',') >= 0 || fileName.indexOf(';') >= 0) {
                setErrorMessage(Messages.AndroidPreferencePage_ERROR_Reserved_Char);
                return false;  // Apply/OK must be disabled
            }
            
            File file = new File(fileName);
            if (!file.isDirectory()) {
                setErrorMessage(JFaceResources.getString(
                    "DirectoryFieldEditor.errorMessage")); //$NON-NLS-1$
                return false;
            }

            boolean ok = AdtPlugin.getDefault().checkSdkLocationAndId(fileName,
                    new AdtPlugin.CheckSdkErrorHandler() {
                @Override
                public boolean handleError(String message) {
                    setErrorMessage(message.replaceAll("\n", " ")); //$NON-NLS-1$ //$NON-NLS-2$
                    return false;  // Apply/OK must be disabled
                }

                @Override
                public boolean handleWarning(String message) {
                    showMessage(message.replaceAll("\n", " ")); //$NON-NLS-1$ //$NON-NLS-2$
                    return true;  // Apply/OK must be enabled
                }
            });
            if (ok) clearMessage();
            return ok;
        }

        @Override
        public Text getTextControl(Composite parent) {
            setValidateStrategy(VALIDATE_ON_KEY_STROKE);
            return super.getTextControl(parent);
        }

        /* (non-Javadoc)
         * Method declared on StringFieldEditor (and FieldEditor).
         */
        @Override
        protected void doFillIntoGrid(Composite parent, int numColumns) {
            super.doFillIntoGrid(parent, numColumns);

            GridData gd;
            Label l = new Label(parent, SWT.NONE);
            l.setText("Note: The list of SDK Targets below is only reloaded once you hit 'Apply' or 'OK'.");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = numColumns;
            l.setLayoutData(gd);
            
            try {
                // We may not have an sdk if the sdk path pref is empty or not valid.
                Sdk sdk = Sdk.getCurrent();
                IAndroidTarget[] targets = sdk != null ? sdk.getTargets() : null;
                
                mTargetSelector = new SdkTargetSelector(parent,
                        targets,
                        false /*allowSelection*/);
                gd = (GridData) mTargetSelector.getLayoutData();
                gd.horizontalSpan = numColumns;
                
                if (mTargetChangeListener == null) {
                    mTargetChangeListener = new TargetChangedListener();
                    AdtPlugin.getDefault().addTargetListener(mTargetChangeListener);
                }
            } catch (Exception e) {
                // We need to catch *any* exception that arises here, otherwise it disables
                // the whole pref panel. We can live without the Sdk target selector but
                // not being able to actually set an sdk path.
                AdtPlugin.log(e, "SdkTargetSelector failed");
            }
        }
        
        @Override
        public void dispose() {
            super.dispose();
            if (mTargetChangeListener != null) {
                AdtPlugin.getDefault().removeTargetListener(mTargetChangeListener);
                mTargetChangeListener = null;
            }
        }
        
        private class TargetChangedListener implements ITargetChangeListener {
            public void onProjectTargetChange(IProject changedProject) {
                // do nothing.
            }

            public void onTargetsLoaded() {
                if (mTargetSelector != null) {
                    // We may not have an sdk if the sdk path pref is empty or not valid.
                    Sdk sdk = Sdk.getCurrent();
                    IAndroidTarget[] targets = sdk != null ? sdk.getTargets() : null;

                    mTargetSelector.setTargets(targets);
                }
            }
        }
    }
}
