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

package com.android.ddmuilib.log.event;

import com.android.ddmlib.log.EventContainer;
import com.android.ddmlib.log.EventLogParser;

import java.util.ArrayList;

public class DisplayFilteredLog extends DisplayLog {

    public DisplayFilteredLog(String name) {
        super(name);
    }

    /**
     * Adds event to the display.
     */
    @Override
    void newEvent(EventContainer event, EventLogParser logParser) {
        ArrayList<ValueDisplayDescriptor> valueDescriptors =
                new ArrayList<ValueDisplayDescriptor>();

        ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors =
                new ArrayList<OccurrenceDisplayDescriptor>();

        if (filterEvent(event, valueDescriptors, occurrenceDescriptors)) {
            addToLog(event, logParser, valueDescriptors, occurrenceDescriptors);
        }
    }

    /**
     * Gets display type
     *
     * @return display type as an integer
     */
    @Override
    int getDisplayType() {
        return DISPLAY_TYPE_FILTERED_LOG;
    }
}
