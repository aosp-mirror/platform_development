/* //device/tools/ddms/src/com/android/ddms/DropdownSelectionListener.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.ddms;

import com.android.ddmlib.Log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Helper class for drop-down menus in toolbars.
 */
public class DropdownSelectionListener extends SelectionAdapter {
    private Menu mMenu;
    private ToolItem mDropdown;

    /**
     * Basic constructor.  Creates an empty Menu to hold items.
     */
    public DropdownSelectionListener(ToolItem item) {
        mDropdown = item;
        mMenu = new Menu(item.getParent().getShell(), SWT.POP_UP);
    }

    /**
     * Add an item to the dropdown menu.
     */
    public void add(String label) {
        MenuItem item = new MenuItem(mMenu, SWT.NONE);
        item.setText(label);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // update the dropdown's text to match the selection
                MenuItem sel = (MenuItem) e.widget;
                mDropdown.setText(sel.getText());
            }
        });
    }

    /**
     * Invoked when dropdown or neighboring arrow is clicked.
     */
    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.ARROW) {
            // arrow clicked, show menu
            ToolItem item = (ToolItem) e.widget;
            Rectangle rect = item.getBounds();
            Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
            mMenu.setLocation(pt.x, pt.y + rect.height);
            mMenu.setVisible(true);
        } else {
            // button clicked
            Log.d("ddms", mDropdown.getText() + " Pressed");
        }
    }
}

