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

package com.android.ddmuilib;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Utility class to help using Table objects.
 *
 */
public final class TableHelper {
    /**
     * Create a TableColumn with the specified parameters. If a
     * <code>PreferenceStore</code> object and a preference entry name String
     * object are provided then the column will listen to change in its width
     * and update the preference store accordingly.
     *
     * @param parent The Table parent object
     * @param header The header string
     * @param style The column style
     * @param sample_text A sample text to figure out column width if preference
     *            value is missing
     * @param pref_name The preference entry name for column width
     * @param prefs The preference store
     * @return The TableColumn object that was created
     */
    public static TableColumn createTableColumn(Table parent, String header,
            int style, String sample_text, final String pref_name,
            final IPreferenceStore prefs) {

        // create the column
        TableColumn col = new TableColumn(parent, style);

        // if there is no pref store or the entry is missing, we use the sample
        // text and pack the column.
        // Otherwise we just read the width from the prefs and apply it.
        if (prefs == null || prefs.contains(pref_name) == false) {
            col.setText(sample_text);
            col.pack();

            // init the prefs store with the current value
            if (prefs != null) {
                prefs.setValue(pref_name, col.getWidth());
            }
        } else {
            col.setWidth(prefs.getInt(pref_name));
        }

        // set the header
        col.setText(header);

        // if there is a pref store and a pref entry name, then we setup a
        // listener to catch column resize to put store the new width value.
        if (prefs != null && pref_name != null) {
            col.addControlListener(new ControlListener() {
                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    // get the new width
                    int w = ((TableColumn)e.widget).getWidth();

                    // store in pref store
                    prefs.setValue(pref_name, w);
                }
            });
        }

        return col;
    }

    /**
     * Create a TreeColumn with the specified parameters. If a
     * <code>PreferenceStore</code> object and a preference entry name String
     * object are provided then the column will listen to change in its width
     * and update the preference store accordingly.
     *
     * @param parent The Table parent object
     * @param header The header string
     * @param style The column style
     * @param sample_text A sample text to figure out column width if preference
     *            value is missing
     * @param pref_name The preference entry name for column width
     * @param prefs The preference store
     */
    public static void createTreeColumn(Tree parent, String header, int style,
            String sample_text, final String pref_name,
            final IPreferenceStore prefs) {

        // create the column
        TreeColumn col = new TreeColumn(parent, style);

        // if there is no pref store or the entry is missing, we use the sample
        // text and pack the column.
        // Otherwise we just read the width from the prefs and apply it.
        if (prefs == null || prefs.contains(pref_name) == false) {
            col.setText(sample_text);
            col.pack();

            // init the prefs store with the current value
            if (prefs != null) {
                prefs.setValue(pref_name, col.getWidth());
            }
        } else {
            col.setWidth(prefs.getInt(pref_name));
        }

        // set the header
        col.setText(header);

        // if there is a pref store and a pref entry name, then we setup a
        // listener to catch column resize to put store the new width value.
        if (prefs != null && pref_name != null) {
            col.addControlListener(new ControlListener() {
                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    // get the new width
                    int w = ((TreeColumn)e.widget).getWidth();

                    // store in pref store
                    prefs.setValue(pref_name, w);
                }
            });
        }
    }

    /**
     * Create a TreeColumn with the specified parameters. If a
     * <code>PreferenceStore</code> object and a preference entry name String
     * object are provided then the column will listen to change in its width
     * and update the preference store accordingly.
     *
     * @param parent The Table parent object
     * @param header The header string
     * @param style The column style
     * @param width the width of the column if the preference value is missing
     * @param pref_name The preference entry name for column width
     * @param prefs The preference store
     */
    public static void createTreeColumn(Tree parent, String header, int style,
            int width, final String pref_name,
            final IPreferenceStore prefs) {

        // create the column
        TreeColumn col = new TreeColumn(parent, style);

        // if there is no pref store or the entry is missing, we use the sample
        // text and pack the column.
        // Otherwise we just read the width from the prefs and apply it.
        if (prefs == null || prefs.contains(pref_name) == false) {
            col.setWidth(width);

            // init the prefs store with the current value
            if (prefs != null) {
                prefs.setValue(pref_name, width);
            }
        } else {
            col.setWidth(prefs.getInt(pref_name));
        }

        // set the header
        col.setText(header);

        // if there is a pref store and a pref entry name, then we setup a
        // listener to catch column resize to put store the new width value.
        if (prefs != null && pref_name != null) {
            col.addControlListener(new ControlListener() {
                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    // get the new width
                    int w = ((TreeColumn)e.widget).getWidth();

                    // store in pref store
                    prefs.setValue(pref_name, w);
                }
            });
        }
    }
}
