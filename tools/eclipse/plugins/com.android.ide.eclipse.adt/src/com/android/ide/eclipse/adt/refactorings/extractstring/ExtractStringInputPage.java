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

package com.android.ide.eclipse.adt.refactorings.extractstring;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @see ExtractStringRefactoring
 */
class ExtractStringInputPage extends UserInputWizardPage implements IWizardPage {

    public ExtractStringInputPage() {
        super("ExtractStringInputPage");  //$NON-NLS-1$
    }

    private Label mStringLabel;
    private Text mNewIdTextField;
    private Label mFileLabel;

    /**
     * Create the UI for the refactoring wizard.
     * <p/>
     * Note that at that point the initial conditions have been checked in
     * {@link ExtractStringRefactoring}.
     */
    public void createControl(Composite parent) {
        
        final ExtractStringRefactoring ref = getOurRefactoring();
        
        Composite content = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        content.setLayout(layout);

        // line 1: String found in selection
        
        Label label = new Label(content, SWT.NONE);
        label.setText("String:");

        String selectedString = ref.getTokenString();
        
        mStringLabel = new Label(content, SWT.NONE);
        mStringLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStringLabel.setText(selectedString != null ? selectedString : "");
        
        // TODO provide an option to replace all occurences of this string instead of
        // just the one.

        // line 2 : Textfield for new ID
        
        label = new Label(content, SWT.NONE);
        label.setText("Replace by R.string.");

        mNewIdTextField = new Text(content, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        mNewIdTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mNewIdTextField.setText(guessId(selectedString));

        ref.setReplacementStringId(mNewIdTextField.getText().trim());

        mNewIdTextField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (validatePage(ref)) {
                    ref.setReplacementStringId(mNewIdTextField.getText().trim());
                }
            }
        });

        // line 3: selection of the output file
        // TODO add a file field/chooser combo to let the user select the file to edit.

        label = new Label(content, SWT.NONE);
        label.setText("Resource file:");

        mFileLabel = new Label(content, SWT.NONE);
        mFileLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mFileLabel.setText("/res/values/strings.xml");
        ref.setTargetFile(mFileLabel.getText());

        // line 4: selection of the res config
        // TODO add the Configuration Selector to decide with strings.xml to change

        label = new Label(content, SWT.NONE);
        label.setText("Configuration:");

        label = new Label(content, SWT.NONE);
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        label.setText("default");
        
        validatePage(ref);
        setControl(content);
    }

    private String guessId(String text) {
        // make lower case
        text = text.toLowerCase();
        
        // everything not alphanumeric becomes an underscore
        text = text.replaceAll("[^a-zA-Z0-9]+", "_");  //$NON-NLS-1$ //$NON-NLS-2$

        // the id must be a proper Java identifier, so it can't start with a number
        if (text.length() > 0 && !Character.isJavaIdentifierStart(text.charAt(0))) {
            text = "_" + text;  //$NON-NLS-1$
        }
        return text;
    }

    private ExtractStringRefactoring getOurRefactoring() {
        return (ExtractStringRefactoring) getRefactoring();
    }

    private boolean validatePage(ExtractStringRefactoring ref) {
        String text = mNewIdTextField.getText().trim();
        boolean success = true;

        // Analyze fatal errors.
        
        if (text == null || text.length() < 1) {
            setErrorMessage("Please provide a resource ID to replace with.");
            success = false;
        } else {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                boolean ok = i == 0 ?
                        Character.isJavaIdentifierStart(c) :
                        Character.isJavaIdentifierPart(c);
                if (!ok) {
                    setErrorMessage(String.format(
                            "The resource ID must be a valid Java identifier. The character %1$c at position %2$d is not acceptable.",
                            c, i+1));
                    success = false;
                    break;
                }
            }
        }

        // Analyze info & warnings.
        
        if (success) {
            if (ref.isResIdDuplicate(mFileLabel.getText(), text)) {
                setErrorMessage(null);
                setMessage(
                    String.format("Warning: There's already a string item called '%1$s' in %2$s.",
                        text, mFileLabel.getText()));
            } else {
                setMessage(null);
                setErrorMessage(null);
            }
        }

        setPageComplete(success);
        return success;
    }
}
