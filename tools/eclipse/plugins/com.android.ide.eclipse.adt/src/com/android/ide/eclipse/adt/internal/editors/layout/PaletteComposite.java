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

package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import java.util.List;

/**
 * A palette composite for the {@link GraphicalEditorPart}.
 * <p/>
 * The palette contains several groups, each with a UI name (e.g. layouts and views) and each
 * with a list of element descriptors.
 * <p/>
 *
 * @since GLE2
 *
 * TODO list:
 * - *Mandatory* for a first release:
 *   - Currently this displays elements as buttons. Eventually this needs to either be replaced
 *     by custom drawing right in here or we need to use a custom control.
 *   - Needs to be able to originate drag'n'drop from these controls onto the GEP.
 *   - Scroll the list.
 * - For later releases:
 *   - Ability to collapse palettes or dockable palettes.
 *   - Different view strategies: big icon, small icons, text vs no text, compact grid.
 *     - This would only be useful with meaningful icons. Out current 1-letter icons are not enough
 *       to get rid of text labels.
 *   - Would be nice to have context-sensitive tools items, e.g. selection arrow tool,
 *     group selection tool, alignment, etc.
 */
public class PaletteComposite extends Composite {

    /**
     * Create the composite.
     * @param parent The parent composite.
     */
    public PaletteComposite(Composite parent) {
        super(parent, SWT.BORDER | SWT.V_SCROLL);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    /**
     * Load or reload the palette elements by using the layour and view descriptors from the
     * given target data.
     *
     * @param targetData The target data that contains the descriptors. If null or empty,
     *   no groups will be created.
     */
    public void reloadPalette(AndroidTargetData targetData) {

        for (Control c : getChildren()) {
            c.dispose();
        }

        if (targetData != null) {
            GridLayout gl = new GridLayout(1, false);
            gl.horizontalSpacing = 0;
            gl.verticalSpacing = 0;
            gl.marginHeight = 2;
            gl.marginBottom = 2;
            gl.marginLeft = 2;
            gl.marginRight = 2;
            gl.marginTop = 2;
            gl.marginBottom = 2;
            setLayout(gl);

            /* TODO: All this is TEMPORARY. */
            Label l = new Label(this, SWT.NONE);
            l.setText("*** PLACEHOLDER ***");  //$NON-NLS-1$
            l.setToolTipText("Temporary mock for the palette. Needs to scroll, needs no buttons, needs to drag'n'drop."); //$NON-NLS-1$

            addGroup("Layouts", targetData.getLayoutDescriptors().getLayoutDescriptors());
            addGroup("Views", targetData.getLayoutDescriptors().getViewDescriptors());
        }

        layout(true);
    }

    private void addGroup(String uiName, List<ElementDescriptor> descriptors) {
        Label label = new Label(this, SWT.NONE);
        label.setText(uiName);

        for (ElementDescriptor desc : descriptors) {
            Button b = new Button(this, SWT.PUSH);
            b.setText(desc.getUiName());
            b.setImage(desc.getIcon());
            b.setToolTipText(desc.getTooltip());
            b.setData(desc);
        }
    }
}
