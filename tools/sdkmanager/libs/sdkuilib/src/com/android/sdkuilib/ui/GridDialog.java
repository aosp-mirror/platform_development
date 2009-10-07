/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * jface-based dialog that properly sets up a {@link GridLayout} top composite with the proper
 * margin.
 *
 * Implementing dialog must create the content of the dialog in
 * {@link #createDialogContent(Composite)}.
 *
 */
public abstract class GridDialog extends Dialog {

    private final int mNumColumns;
    private final boolean mMakeColumnsEqualWidth;

    /**
     * Creates the dialog
     * @param parentShell the parent {@link Shell}.
     * @param numColumns the number of columns in the grid
     * @param makeColumnsEqualWidth whether or not the columns will have equal width
     */
    public GridDialog(Shell parentShell, int numColumns, boolean makeColumnsEqualWidth) {
        super(parentShell);
        mNumColumns = numColumns;
        mMakeColumnsEqualWidth = makeColumnsEqualWidth;
    }

    /**
     * Creates the content of the dialog. The <var>parent</var> composite is a {@link GridLayout}
     * created with the <var>numColumn</var> and <var>makeColumnsEqualWidth</var> parameters
     * passed to {@link #GridDialog(Shell, int, boolean)}.
     * @param parent the parent composite.
     */
    public abstract void createDialogContent(Composite parent);

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(mNumColumns, mMakeColumnsEqualWidth);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(
                IDialogConstants.HORIZONTAL_SPACING);
        top.setLayout(layout);
        top.setLayoutData(new GridData(GridData.FILL_BOTH));

        createDialogContent(top);

        applyDialogFont(top);
        return top;
    }
}
