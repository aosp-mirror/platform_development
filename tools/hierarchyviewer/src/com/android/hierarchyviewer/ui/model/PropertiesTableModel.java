/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.hierarchyviewer.scene.ViewNode;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.ArrayList;

public class PropertiesTableModel extends DefaultTableModel {
    private List<ViewNode.Property> properties;
    private List<ViewNode.Property> privateProperties = new ArrayList<ViewNode.Property>();

    public PropertiesTableModel(ViewNode node) {
        properties = node.properties;
        loadPrivateProperties(node);
    }

    private void loadPrivateProperties(ViewNode node) {
        int x = node.left;
        int y = node.top;
        ViewNode p = node.parent;
        while (p != null) {
            x += p.left - p.scrollX;
            y += p.top - p.scrollY;
            p = p.parent;
        }

        ViewNode.Property property = new ViewNode.Property();
        property.name = "absolute_x";
        property.value = String.valueOf(x);
        privateProperties.add(property);

        property = new ViewNode.Property();
        property.name = "absolute_y";
        property.value = String.valueOf(y);
        privateProperties.add(property);
    }

    @Override
    public int getRowCount() {
        return (privateProperties == null ? 0 : privateProperties.size()) +
                (properties == null ? 0 : properties.size());
    }

    @Override
    public Object getValueAt(int row, int column) {
        ViewNode.Property property;

        if (row < privateProperties.size()) {
            property = privateProperties.get(row);
        } else {
            property = properties.get(row - privateProperties.size());
        }

        return column == 0 ? property.name : property.value;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "Property" : "Value";
    }

    @Override
    public boolean isCellEditable(int arg0, int arg1) {
        return false;
    }

    @Override
    public void setValueAt(Object arg0, int arg1, int arg2) {
    }
}
