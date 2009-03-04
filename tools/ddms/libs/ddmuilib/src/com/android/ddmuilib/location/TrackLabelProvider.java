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

import com.android.ddmuilib.location.GpxParser.Track;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;

import java.util.Date;

/**
 * Label Provider for {@link Table} objects displaying {@link Track} objects.
 */
public class TrackLabelProvider implements ITableLabelProvider {

    public Image getColumnImage(Object element, int columnIndex) {
        return null;
    }

    public String getColumnText(Object element, int columnIndex) {
        if (element instanceof Track) {
            Track track = (Track)element;
            switch (columnIndex) {
                case 0:
                    return track.getName();
                case 1:
                    return Integer.toString(track.getPointCount());
                case 2:
                    long time = track.getFirstPointTime();
                    if (time != -1) {
                        return new Date(time).toString();
                    }
                    break;
                case 3:
                    time = track.getLastPointTime();
                    if (time != -1) {
                        return new Date(time).toString();
                    }
                    break;
                case 4:
                    return track.getComment();
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
