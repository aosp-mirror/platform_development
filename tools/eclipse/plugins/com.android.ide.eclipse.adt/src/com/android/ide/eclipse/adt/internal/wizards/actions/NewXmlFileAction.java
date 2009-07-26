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

package com.android.ide.eclipse.adt.internal.wizards.actions;

import com.android.ide.eclipse.adt.internal.wizards.newxmlfile.NewXmlFileWizard;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * Delegate for the toolbar action "Android Project" or for the
 * project > Android Project context menu.
 * 
 * It displays the Android New XML file wizard.
 */
public class NewXmlFileAction extends OpenWizardAction {

    @Override
    protected IWorkbenchWizard instanciateWizard(IAction action) {
        return new NewXmlFileWizard();
    }
}
