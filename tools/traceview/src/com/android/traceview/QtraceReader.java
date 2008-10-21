/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import java.util.ArrayList;
import java.util.HashMap;

public class QtraceReader extends TraceReader {
    QtraceReader(String traceName) {
    }

    @Override
    public MethodData[] getMethods() {
        return null;
    }

    @Override
    public HashMap<Integer, String> getThreadLabels() {
        return null;
    }

    @Override
    public ArrayList<TimeLineView.Record> getThreadTimeRecords() {
        return null;
    }

    @Override
    public ProfileProvider getProfileProvider() {
        return null;
    }
}
