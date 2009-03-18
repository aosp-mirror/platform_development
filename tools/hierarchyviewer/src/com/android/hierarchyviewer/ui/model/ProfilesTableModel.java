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

package com.android.hierarchyviewer.ui.model;

import javax.swing.table.DefaultTableModel;
import java.text.NumberFormat;

public class ProfilesTableModel extends DefaultTableModel {
    private static final String[] NAMES = { "measure", "layout", "draw" };

    private final double[] profiles;
    private final NumberFormat formatter;

    public ProfilesTableModel(double[] profiles) {
        this.profiles = profiles;
        formatter = NumberFormat.getNumberInstance();
    }

    @Override
    public int getRowCount() {
        return profiles == null ? 0 : profiles.length;
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (profiles == null) return "";

        if (column == 0) {
            return NAMES[row];
        }


        return formatter.format(profiles[row]) + "";
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "Operation" : "Duration (ms)";
    }

    @Override
    public boolean isCellEditable(int arg0, int arg1) {
        return false;
    }

    @Override
    public void setValueAt(Object arg0, int arg1, int arg2) {
    }
}
