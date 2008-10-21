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

package com.android.ddmlib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A receiver able to parse the result of the execution of 
 * {@link #GETPROP_COMMAND} on a device.
 */
final class GetPropReceiver extends MultiLineReceiver {
    final static String GETPROP_COMMAND = "getprop"; //$NON-NLS-1$
    
    private final static Pattern GETPROP_PATTERN = Pattern.compile("^\\[([^]]+)\\]\\:\\s*\\[(.*)\\]$"); //$NON-NLS-1$

    /** indicates if we need to read the first */
    private Device mDevice = null;

    /**
     * Creates the receiver with the device the receiver will modify.
     * @param device The device to modify
     */
    public GetPropReceiver(Device device) {
        mDevice = device;
    }

    @Override
    public void processNewLines(String[] lines) {
        // We receive an array of lines. We're expecting
        // to have the build info in the first line, and the build
        // date in the 2nd line. There seems to be an empty line
        // after all that.

        for (String line : lines) {
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            
            Matcher m = GETPROP_PATTERN.matcher(line);
            if (m.matches()) {
                String label = m.group(1);
                String value = m.group(2);
                
                if (label.length() > 0) {
                    mDevice.addProperty(label, value);
                }
            }
        }
    }
    
    public boolean isCancelled() {
        return false;
    }
    
    @Override
    public void done() {
        mDevice.update(Device.CHANGE_BUILD_INFO);
    }
}
