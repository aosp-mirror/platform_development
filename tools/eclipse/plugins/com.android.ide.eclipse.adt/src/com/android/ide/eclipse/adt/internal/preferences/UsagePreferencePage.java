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

package com.android.ide.eclipse.adt.internal.preferences;

import com.android.sdkstats.SdkStatsService;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.io.IOException;

public class UsagePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private BooleanFieldEditor mOptInCheckBox;

    public UsagePreferencePage() {
    }

    public void init(IWorkbench workbench) {
        // pass
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayout(new GridLayout(1, false));
        top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Link text = new Link(top, SWT.WRAP);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 200;
        text.setLayoutData(gd);
        text.setText(SdkStatsService.BODY_TEXT);

        text.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                SdkStatsService.openUrl(event.text);
            }
        });

        mOptInCheckBox = new BooleanFieldEditor(SdkStatsService.PING_OPT_IN,
                SdkStatsService.CHECKBOX_TEXT, top);
        mOptInCheckBox.setPage(this);
        mOptInCheckBox.setPreferenceStore(SdkStatsService.getPreferenceStore());
        mOptInCheckBox.load();
        
        return top;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performCancel()
     */
    @Override
    public boolean performCancel() {
        mOptInCheckBox.load();
        return super.performCancel();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        mOptInCheckBox.loadDefault();
        super.performDefaults();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        save();
        return super.performOk();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    @Override
    protected void performApply() {
        save();
        super.performApply();
    }
    
    private void save() {
        try {
            PreferenceStore store = SdkStatsService.getPreferenceStore();
            if (store !=  null) {
                store.setValue(SdkStatsService.PING_OPT_IN, mOptInCheckBox.getBooleanValue());
                store.save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
