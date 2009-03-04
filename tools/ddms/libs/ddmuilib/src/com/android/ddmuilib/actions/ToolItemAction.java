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

package com.android.ddmuilib.actions;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Wrapper around {@link ToolItem} to implement {@link ICommonAction}
 */
public class ToolItemAction implements ICommonAction {
    public ToolItem item;

    public ToolItemAction(ToolBar parent, int style) {
        item = new ToolItem(parent, style);
    }

    /**
     * Sets the enabled state of this action.
     * @param enabled <code>true</code> to enable, and
     *   <code>false</code> to disable
     * @see ICommonAction#setChecked(boolean)
     */
    public void setChecked(boolean checked) {
        item.setSelection(checked);
    }

    /**
     * Sets the enabled state of this action.
     * @param enabled <code>true</code> to enable, and
     *   <code>false</code> to disable
     * @see ICommonAction#setEnabled(boolean)
     */
    public void setEnabled(boolean enabled) {
        item.setEnabled(enabled);
    }

    /**
     * Sets the {@link Runnable} that will be executed when the action is triggered (through
     * {@link SelectionListener#widgetSelected(SelectionEvent)} on the wrapped {@link ToolItem}).
     * @see ICommonAction#setRunnable(Runnable)
     */
    public void setRunnable(final Runnable runnable) {
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                runnable.run();
            }
        });
    }
}
