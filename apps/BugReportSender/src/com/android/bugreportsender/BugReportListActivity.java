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

package com.android.bugreportsender;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * Shows a list of bug reports currently in /sdcard/bugreports
 */
public class BugReportListActivity extends ListActivity {
    private static final String TAG = "BugReportListActivity";
    private static final File REPORT_DIR = new File("/sdcard/bugreports");
    private static final int SYSTEM_LOG_ID = 1;
    private static final int MEMORY_ID = 2;
    private static final int CPU_ID = 3;
    private static final int PROCRANK_ID = 4;
    private static final HashMap<Integer, String> ID_MAP = new HashMap<Integer, String>();

    static {
        ID_MAP.put(SYSTEM_LOG_ID, "SYSTEM LOG");
        ID_MAP.put(MEMORY_ID, "MEMORY INFO");
        ID_MAP.put(CPU_ID, "CPU INFO");
        ID_MAP.put(PROCRANK_ID, "PROCRANK");
    }

    private ArrayAdapter<String> mAdapter = null;
    private ArrayList<File> mFiles = null;
    private Handler mHandler = null;
    private FileObserver mObserver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mFiles = new ArrayList<File>();
        mHandler = new Handler();

        int flags = FileObserver.CREATE | FileObserver.MOVED_TO;
        mObserver = new FileObserver(REPORT_DIR.getPath(), flags) {
            public void onEvent(int event, String path) {
                mHandler.post(new Runnable() { public void run() { scanDirectory(); } });
            }
        };

        setListAdapter(mAdapter);
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, SYSTEM_LOG_ID, 0, "System Log");
        menu.add(0, CPU_ID, 0, "CPU Info");
        menu.add(0, MEMORY_ID, 0, "Memory Info");
        menu.add(0, PROCRANK_ID, 0, "Procrank");
    }

    @Override
    public void onStart() {
        super.onStart();
        mObserver.startWatching();
        scanDirectory();
    }

    @Override
    public void onStop() {
        super.onStop();
        mObserver.stopWatching();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (position < mFiles.size()) {
            File file = mFiles.get(position);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra("subject", file.getName());
            intent.putExtra("body", "Build: " + Build.DISPLAY + "\n(Sent by BugReportSender)");
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            if (file.getName().endsWith(".gz")) {
                intent.setType("application/x-gzip");
            } else if (file.getName().endsWith(".txt")) {
                intent.setType("text/plain");
            } else {
                intent.setType("application/octet-stream");
            }
            startActivity(intent);
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
      AdapterView.AdapterContextMenuInfo info =
              (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      if (info.position >= mFiles.size()) {
        return true;
      }
      int id = item.getItemId();
      switch (id) {
          case SYSTEM_LOG_ID: // drop down
          case MEMORY_ID:     // drop down
          case CPU_ID:        // drop down
          case PROCRANK_ID:
          File file = mFiles.get(info.position);
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setDataAndType(Uri.fromFile(file), "vnd.android/bugreport");
          intent.putExtra("section", ID_MAP.get(id));
          startActivity(intent);
          return true;
      default:
        return super.onContextItemSelected(item);
      }
    }

    private void scanDirectory() {
        mAdapter.clear();
        mFiles.clear();

        File[] files = REPORT_DIR.listFiles();
        if (files == null) return;

        // Sort in reverse order: newest bug reports first
        Arrays.sort(files, Collections.reverseOrder());
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            if (name.endsWith(".gz")) name = name.substring(0, name.length() - 3);
            if (!name.startsWith("bugreport-") || !name.endsWith(".txt")) {
                Log.w(TAG, "Ignoring non-bugreport: " + files[i]);
                continue;
            }

            // Make sure to keep the parallel arrays in sync
            mAdapter.add(name.substring(10, name.length() - 4));
            mFiles.add(files[i]);
        }
    }
}
