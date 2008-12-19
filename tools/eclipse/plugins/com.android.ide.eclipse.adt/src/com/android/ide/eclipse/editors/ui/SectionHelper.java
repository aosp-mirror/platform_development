/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.editors.ui;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.editors.AndroidEditor;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import java.lang.reflect.Method;

/**
 * Helper for the AndroidManifest form editor.
 * 
 * Helps create a new SectionPart with sensible default parameters,
 * create default layout or add typical widgets.
 * 
 * IMPORTANT: This is NOT a generic class. It makes a lot of assumptions on the
 * UI as used by the form editor for the AndroidManifest.
 * 
 * TODO: Consider moving to a common package.
 */
public final class SectionHelper {

    /**
     * Utility class that derives from SectionPart, constructs the Section with
     * sensible defaults (with a title and a description) and provide some shorthand
     * methods for creating typically UI (label and text, form text.)
     */
    static public class ManifestSectionPart extends SectionPart {

        /**
         * Construct a SectionPart that uses a title bar and a description.
         * It's up to the caller to call setText() and setDescription().
         * <p/>
         * The section style includes a description and a title bar by default.
         * 
         * @param body The parent (e.g. FormPage body)
         * @param toolkit Form Toolkit
         */
        public ManifestSectionPart(Composite body, FormToolkit toolkit) {
            this(body, toolkit, 0, false);
        }

        /**
         * Construct a SectionPart that uses a title bar and a description.
         * It's up to the caller to call setText() and setDescription().
         * <p/>
         * The section style includes a description and a title bar by default.
         * You can add extra styles, like Section.TWISTIE.
         * 
         * @param body The parent (e.g. FormPage body).
         * @param toolkit Form Toolkit.
         * @param extra_style Extra styles (on top of description and title bar).
         * @param use_description True if the Section.DESCRIPTION style should be added.
         */
        public ManifestSectionPart(Composite body, FormToolkit toolkit,
                int extra_style, boolean use_description) {
            super(body, toolkit, extra_style |
                    Section.TITLE_BAR |
                    (use_description ? Section.DESCRIPTION : 0));
        }

        // Create non-static methods of helpers just for convenience
        
        /**
         * Creates a new composite with a TableWrapLayout set with a given number of columns.
         * 
         * If the parent composite is a Section, the new composite is set as a client.
         * 
         * @param toolkit Form Toolkit
         * @param numColumns Desired number of columns.
         * @return The new composite.
         */
        public Composite createTableLayout(FormToolkit toolkit, int numColumns) {
            return SectionHelper.createTableLayout(getSection(), toolkit, numColumns);
        }

        /**
         * Creates a label widget.
         * If the parent layout if a TableWrapLayout, maximize it to span over all columns.
         * 
         * @param parent The parent (e.g. composite from CreateTableLayout())
         * @param toolkit Form Toolkit
         * @param label The string for the label.
         * @param tooltip An optional tooltip for the label and text. Can be null.
         * @return The new created label 
         */
        public Label createLabel(Composite parent, FormToolkit toolkit, String label,
                String tooltip) {
            return SectionHelper.createLabel(parent, toolkit, label, tooltip);
        }

        /**
         * Creates two widgets: a label and a text field.
         * 
         * This expects the parent composite to have a TableWrapLayout with 2 columns.
         * 
         * @param parent The parent (e.g. composite from CreateTableLayout())
         * @param toolkit Form Toolkit
         * @param label The string for the label.
         * @param value The initial value of the text field. Can be null.
         * @param tooltip An optional tooltip for the label and text. Can be null.
         * @return The new created Text field (the label is not returned) 
         */
        public Text createLabelAndText(Composite parent, FormToolkit toolkit, String label,
                String value, String tooltip) {
            return SectionHelper.createLabelAndText(parent, toolkit, label, value, tooltip);
        }

        /**
         * Creates a FormText widget.
         * 
         * This expects the parent composite to have a TableWrapLayout with 2 columns.
         * 
         * @param parent The parent (e.g. composite from CreateTableLayout())
         * @param toolkit Form Toolkit
         * @param isHtml True if the form text will contain HTML that must be interpreted as
         *               rich text (i.e. parse tags & expand URLs).
         * @param label The string for the label.
         * @param setupLayoutData indicates whether the created form text receives a TableWrapData
         * through the setLayoutData method. In some case, creating it will make the table parent
         * huge, which we don't want.
         * @return The new created FormText.
         */
        public FormText createFormText(Composite parent, FormToolkit toolkit, boolean isHtml,
                String label, boolean setupLayoutData) {
            return SectionHelper.createFormText(parent, toolkit, isHtml, label, setupLayoutData);
        }

        /**
         * Forces the section to recompute its layout and redraw.
         * This is needed after the content of the section has been changed at runtime.
         */
        public void layoutChanged() {
            Section section = getSection();

            // Calls getSection().reflow(), which is the same that Section calls
            // when the expandable state is changed and the height changes.
            // Since this is protected, some reflection is needed to invoke it.
            try {
                Method reflow;
                reflow = section.getClass().getDeclaredMethod("reflow", (Class<?>[])null);
                reflow.setAccessible(true);
                reflow.invoke(section);
            } catch (Exception e) {
                AdtPlugin.log(e, "Error when invoking Section.reflow");
            }
            
            section.layout(true /* changed */, true /* all */);
        }
    }
    
    /**
     * Creates a new composite with a TableWrapLayout set with a given number of columns.
     * 
     * If the parent composite is a Section, the new composite is set as a client.
     * 
     * @param composite The parent (e.g. a Section or SectionPart)
     * @param toolkit Form Toolkit
     * @param numColumns Desired number of columns.
     * @return The new composite.
     */
    static public Composite createTableLayout(Composite composite, FormToolkit toolkit,
            int numColumns) {
        Composite table = toolkit.createComposite(composite);
        TableWrapLayout layout = new TableWrapLayout();
        layout.numColumns = numColumns;
        table.setLayout(layout);
        toolkit.paintBordersFor(table);
        if (composite instanceof Section) {
            ((Section) composite).setClient(table);
        }
        return table;
    }

    /**
     * Creates a new composite with a GridLayout set with a given number of columns.
     * 
     * If the parent composite is a Section, the new composite is set as a client.
     * 
     * @param composite The parent (e.g. a Section or SectionPart)
     * @param toolkit Form Toolkit
     * @param numColumns Desired number of columns.
     * @return The new composite.
     */
    static public Composite createGridLayout(Composite composite, FormToolkit toolkit,
            int numColumns) {
        Composite grid = toolkit.createComposite(composite);
        GridLayout layout = new GridLayout();
        layout.numColumns = numColumns;
        grid.setLayout(layout);
        toolkit.paintBordersFor(grid);
        if (composite instanceof Section) {
            ((Section) composite).setClient(grid);
        }
        return grid;
    }

    /**
     * Creates two widgets: a label and a text field.
     * 
     * This expects the parent composite to have a TableWrapLayout with 2 columns.
     * 
     * @param parent The parent (e.g. composite from CreateTableLayout())
     * @param toolkit Form Toolkit
     * @param label_text The string for the label.
     * @param value The initial value of the text field. Can be null.
     * @param tooltip An optional tooltip for the label and text. Can be null.
     * @return The new created Text field (the label is not returned) 
     */
    static public Text createLabelAndText(Composite parent, FormToolkit toolkit, String label_text,
            String value, String tooltip) {
        Label label = toolkit.createLabel(parent, label_text);
        label.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));
        Text text = toolkit.createText(parent, value);
        text.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.MIDDLE));

        addControlTooltip(label, tooltip);
        return text;
    }

    /**
     * Creates a label widget.
     * If the parent layout if a TableWrapLayout, maximize it to span over all columns.
     * 
     * @param parent The parent (e.g. composite from CreateTableLayout())
     * @param toolkit Form Toolkit
     * @param label_text The string for the label.
     * @param tooltip An optional tooltip for the label and text. Can be null.
     * @return The new created label 
     */
    static public Label createLabel(Composite parent, FormToolkit toolkit, String label_text,
            String tooltip) {
        Label label = toolkit.createLabel(parent, label_text);

        TableWrapData twd = new TableWrapData(TableWrapData.FILL_GRAB);
        if (parent.getLayout() instanceof TableWrapLayout) {
            twd.colspan = ((TableWrapLayout) parent.getLayout()).numColumns;
        }
        label.setLayoutData(twd);

        addControlTooltip(label, tooltip);
        return label;
    }

    /**
     * Associates a tooltip with a control.
     * 
     * This mirrors the behavior from org.eclipse.pde.internal.ui.editor.text.PDETextHover
     * 
     * @param control The control to which associate the tooltip.
     * @param tooltip The tooltip string. Can use \n for multi-lines. Will not display if null.
     */
    static public void addControlTooltip(final Control control, String tooltip) {
        if (control == null || tooltip == null || tooltip.length() == 0) {
            return;
        }
        
        // Some kinds of controls already properly implement tooltip display. 
        if (control instanceof Button) {
            control.setToolTipText(tooltip);
            return;
        }

        control.setToolTipText(null);

        final DefaultInformationControl ic = new DefaultInformationControl(control.getShell());
        ic.setInformation(tooltip);
        Point sz = ic.computeSizeHint();
        ic.setSize(sz.x, sz.y);
        ic.setVisible(false); // initially hidden
        
        control.addMouseTrackListener(new MouseTrackListener() {
            public void mouseEnter(MouseEvent e) {
            }

            public void mouseExit(MouseEvent e) {
                ic.setVisible(false);
            }

            public void mouseHover(MouseEvent e) {
                ic.setLocation(control.toDisplay(10, 25));  // same offset as in PDETextHover
                ic.setVisible(true);
            }
        });
    }

    /**
     * Creates a FormText widget.
     * 
     * This expects the parent composite to have a TableWrapLayout with 2 columns.
     * 
     * @param parent The parent (e.g. composite from CreateTableLayout())
     * @param toolkit Form Toolkit
     * @param isHtml True if the form text will contain HTML that must be interpreted as
     *               rich text (i.e. parse tags & expand URLs).
     * @param label The string for the label.
     * @param setupLayoutData indicates whether the created form text receives a TableWrapData
     * through the setLayoutData method. In some case, creating it will make the table parent
     * huge, which we don't want.
     * @return The new created FormText.
     */
    static public FormText createFormText(Composite parent, FormToolkit toolkit,
            boolean isHtml, String label, boolean setupLayoutData) {
        FormText text = toolkit.createFormText(parent, true /* track focus */);
        if (setupLayoutData) {
            TableWrapData twd = new TableWrapData(TableWrapData.FILL_GRAB);
            twd.maxWidth = AndroidEditor.TEXT_WIDTH_HINT;
            if (parent.getLayout() instanceof TableWrapLayout) {
                twd.colspan = ((TableWrapLayout) parent.getLayout()).numColumns;
            }
            text.setLayoutData(twd);
        }
        text.setWhitespaceNormalized(true);
        text.setText(label, isHtml /* parseTags */, isHtml /* expandURLs */);
        return text;
    }
}
