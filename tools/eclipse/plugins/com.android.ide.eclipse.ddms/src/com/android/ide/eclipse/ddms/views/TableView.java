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

package com.android.ide.eclipse.ddms.views;

import com.android.ddmuilib.ITableFocusListener;
import com.android.ddmuilib.TablePanel;
import com.android.ddmuilib.ITableFocusListener.IFocusedTableActivator;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Base class for view containing Table that needs to support copy, and select all.
 */
public abstract class TableView extends SelectionDependentViewPart {

    /** Activator for the current Table that has the focus */
    IFocusedTableActivator mActivator = null;

    private Clipboard mClipboard;

    private Action mCopyAction;
    private Action mSelectAllAction;

    /**
     * Setup the listener for the Table objects of <code>Panel</code>, and setup
     * the copy and select all actions.
     * @param panel The panel to setup
     * @param parent The parent composite of the Panel's content.
     */
    void setupTableFocusListener(TablePanel panel, Composite parent) {
        panel.setTableFocusListener(new ITableFocusListener() {
            public void focusGained(IFocusedTableActivator activator) {
                mActivator = activator;
                mCopyAction.setEnabled(true);
                mSelectAllAction.setEnabled(true);
            }

            public void focusLost(IFocusedTableActivator activator) {
                if (activator == mActivator) {
                    mActivator = null;
                    mCopyAction.setEnabled(false);
                    mSelectAllAction.setEnabled(false);
                }
            }
        });

        // setup the copy action
        mClipboard = new Clipboard(parent.getDisplay());
        IActionBars actionBars = getViewSite().getActionBars();
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
                mCopyAction = new Action("Copy") {
            @Override
            public void run() {
                if (mActivator != null) {
                    mActivator.copy(mClipboard);
                }
            }
        });

        // setup the select all action
        actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
                mSelectAllAction = new Action("Select All") {
            @Override
            public void run() {
                if (mActivator != null) {
                    mActivator.selectAll();
                }
            }
        });

    }

    @Override
    public void dispose() {
        super.dispose();
        mClipboard.dispose();
    }
}
