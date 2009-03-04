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

import com.android.ddmlib.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Imports a textual event log.  Gets tags from build path.
 */
public class EventLogImporter {

    private String[] mTags;
    private String[] mLog;

    public EventLogImporter(String filePath) throws FileNotFoundException {
        String top = System.getenv("ANDROID_BUILD_TOP");
        if (top == null) {
            throw new FileNotFoundException();
        }
        final String tagFile = top + "/system/core/logcat/event-log-tags";
        BufferedReader tagReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(tagFile)));
        BufferedReader eventReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath)));
        try {
            readTags(tagReader);
            readLog(eventReader);
        } catch (IOException e) {
        }
    }

    public String[] getTags() {
        return mTags;
    }

    public String[] getLog() {
        return mLog;
    }

    private void readTags(BufferedReader reader) throws IOException {
        String line;

        ArrayList<String> content = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            content.add(line);
        }
        mTags = content.toArray(new String[content.size()]);
    }

    private void readLog(BufferedReader reader) throws IOException {
        String line;

        ArrayList<String> content = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            content.add(line);
        }

        mLog = content.toArray(new String[content.size()]);
    }

}
