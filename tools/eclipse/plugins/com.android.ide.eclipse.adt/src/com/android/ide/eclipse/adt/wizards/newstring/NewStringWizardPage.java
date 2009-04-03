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

package com.android.ide.eclipse.adt.wizards.newstring;

import com.android.ide.eclipse.adt.wizards.newstring.NewStringBaseImpl.INewStringPageCallback;
import com.android.ide.eclipse.adt.wizards.newstring.NewStringBaseImpl.ValidationStatus;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 */
class NewStringWizardPage extends WizardPage implements INewStringPageCallback {

    private NewStringBaseImpl mImpl;

    /** Field displaying the user-selected string to be replaced. */
    private Label mStringValueField;

    private String mNewStringId;

    public NewStringWizardPage(IProject project, String pageName) {
        super(pageName);
        mImpl = new NewStringBaseImpl(project, this);
    }
    
    public String getNewStringValue() {
        return mStringValueField.getText();
    }
    
    public String getNewStringId() {
        return mNewStringId;
    }
    
    public String getResFilePathProjPath() {
        return mImpl.getResFileProjPath();
    }

    /**
     * Create the UI for the new string wizard.
     */
    public void createControl(Composite parent) {
        Composite content = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        content.setLayout(layout);

        mImpl.createControl(content);
        setControl(content);
    }

    /**
     * Creates the top group with the field to replace which string and by what
     * and by which options.
     * 
     * @param content A composite with a 1-column grid layout
     * @return The {@link Text} field for the new String ID name.
     */
    public Text createStringGroup(Composite content) {

        Group group = new Group(content, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("New String");

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        group.setLayout(layout);

        Label label = new Label(group, SWT.NONE);
        label.setText("String:");

        mStringValueField = new Label(group, SWT.NONE);
        mStringValueField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStringValueField.setText("");  //$NON-NLS-1$
        
        // TODO provide an option to refactor all known occurences of this string.

        // line : Textfield for new ID
        
        label = new Label(group, SWT.NONE);
        label.setText("Replace by R.string.");

        final Text stringIdField = new Text(group, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        stringIdField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        stringIdField.setText("");

        mNewStringId = stringIdField.getText().trim();

        stringIdField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (mImpl.validatePage()) {
                    mNewStringId = stringIdField.getText().trim();
                }
            }
        });
        
        return stringIdField;
    }

    public void postValidatePage(ValidationStatus status) {
        // pass
    }
}
