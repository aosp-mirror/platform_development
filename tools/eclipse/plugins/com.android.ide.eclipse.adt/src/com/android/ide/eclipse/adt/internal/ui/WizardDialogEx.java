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

package com.android.ide.eclipse.adt.internal.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

/**
 * A {@link WizardDialog} that gives access to some inner controls.
 */
public final class WizardDialogEx extends WizardDialog {

    /**
     * @see WizardDialog#WizardDialog(Shell, IWizard)
     */
    public WizardDialogEx(Shell parentShell, IWizard newWizard) {
        super(parentShell, newWizard);
    }

    /**
     * Returns the cancel button.
     * <p/>
     * Note: there is already a protected, deprecated method that does the same thing.
     * To avoid overriding a deprecated method, the name as be changed to ...Ex.
     */
    public Button getCancelButtonEx() {
        return getButton(IDialogConstants.CANCEL_ID);
    }
}
