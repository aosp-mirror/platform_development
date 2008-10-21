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

package com.android.ddmuilib.location;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;

/**
 * Label Provider for {@link Table} objects displaying {@link WayPoint} objects.
 */
public class WayPointLabelProvider implements ITableLabelProvider {

    public Image getColumnImage(Object element, int columnIndex) {
        return null;
    }

    public String getColumnText(Object element, int columnIndex) {
        if (element instanceof WayPoint) {
            WayPoint wayPoint = (WayPoint)element;
            switch (columnIndex) {
                case 0:
                    return wayPoint.getName();
                case 1:
                    return String.format("%.6f", wayPoint.getLongitude());
                case 2:
                    return String.format("%.6f", wayPoint.getLatitude());
                case 3:
                    if (wayPoint.hasElevation()) {
                        return String.format("%.1f", wayPoint.getElevation());
                    } else {
                        return "-";
                    }
                case 4:
                    return wayPoint.getDescription();
            }
        }

        return null;
    }

    public void addListener(ILabelProviderListener listener) {
        // pass
    }

    public void dispose() {
        // pass
    }

    public boolean isLabelProperty(Object element, String property) {
        // pass
        return false;
    }

    public void removeListener(ILabelProviderListener listener) {
        // pass
    }
}
