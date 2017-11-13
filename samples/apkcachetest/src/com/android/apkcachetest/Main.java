/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.apkcachetest;

import android.app.Activity;
import android.os.Bundle;
import android.os.FileUtils;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class Main extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.hello_activity);
        File preloadsFileCache = getPreloadsFileCache();
        TextView txt = (TextView) findViewById(R.id.text);
        if (!getApplicationInfo().isPrivilegedApp()) {
            txt.append("WARNING: App must be installed in /system/priv-app directory to access "
                    + "preloads cache\n");
        }
        txt.append("PreloadsFileCache app directory: " + preloadsFileCache + '\n');
        if (!preloadsFileCache.exists()) {
            txt.append("   --- Directory does not exist ---\n");
        } else {
            File[] files = preloadsFileCache.listFiles();
            if (files == null || files.length == 0) {
                txt.append("   --- No files found ---\n");
            } else {
                for (File file : files) {
                    try {
                        txt.append("   " + file.getName() + ": [" + readTextFile(file) + "]\n");
                    } catch (IOException e) {
                        txt.append("   " + file.getName() + ": Error " + e + "\n");
                        e.printStackTrace();
                    }
                }
                txt.append(files.length + " files");
            }

        }
    }

    String readTextFile(File file) throws IOException {
        String s = FileUtils.readTextFile(file, 100, "...");
        if (s != null) {
            s = s.replaceAll("[^\\x20-\\x7E]+", " ");
        }
        return s;
    }
}
