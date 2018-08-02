/**
 * Copyright (c) 2018 The Android Open Source Project
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

package com.example.android.multidisplay.launcher;

import static android.widget.Toast.LENGTH_LONG;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.multidisplay.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LauncherActivity extends Activity implements DisplayManager.DisplayListener,
        LoaderManager.LoaderCallbacks<List<AppEntry>>{

    private Spinner mDisplaySpinner;
    private List<Display> mDisplayList;
    private int mSelectedDisplayId;
    private AppListAdapter mAppListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDisplaySpinner = findViewById(R.id.spinner);
        mDisplaySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedDisplayId = mDisplayList.get(i).getDisplayId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mSelectedDisplayId = -1;
            }
        });

        final GridView appGridView = findViewById(R.id.app_grid);
        mAppListAdapter = new AppListAdapter(this);
        appGridView.setAdapter(mAppListAdapter);
        final OnItemClickListener itemClickListener = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final AppEntry entry = mAppListAdapter.getItem(position);
                launch(entry.getLaunchIntent());
            }
        };
        appGridView.setOnItemClickListener(itemClickListener);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDisplayList(null);
    }

    void launch(Intent launchIntent) {
        if (mSelectedDisplayId == -1) {
            Toast.makeText(this, R.string.select_display, LENGTH_LONG).show();
            return;
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        final ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(mSelectedDisplayId);
        try {
            startActivity(launchIntent, options.toBundle());
        } catch (Exception e) {
            final AlertDialog.Builder builder =
                    new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            builder.setTitle(R.string.couldnt_launch)
                    .setMessage(e.getLocalizedMessage())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    /**
     * Read the list of currently connected displays and pick one.
     * When the list changes it'll try to keep the previously selected display. If that one won't be
     * available, it'll pick the display with biggest id (last connected).
     */
    public void updateDisplayList(View view) {
        final DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        mDisplayList = Arrays.asList(dm.getDisplays());
        final List<String> spinnerItems = new ArrayList<>();
        int preferredDisplayPosition = -1;
        int biggestId = -1, biggestPos = -1;
        for (int i = 0; i < mDisplayList.size(); i++) {
            final Display display = mDisplayList.get(i);
            final int id = display.getDisplayId();
            final boolean isDisplayPrivate = (display.getFlags() & Display.FLAG_PRIVATE) != 0;
            spinnerItems.add("" + id + ": " + display.getName()
                    + (isDisplayPrivate ? " (private)" : ""));
            if (id == mSelectedDisplayId) {
                preferredDisplayPosition = i;
            }
            if (display.getDisplayId() > biggestId) {
                biggestId = display.getDisplayId();
                biggestPos = i;
            }
        }
        if (preferredDisplayPosition == -1) {
            preferredDisplayPosition = biggestPos;
        }
        mSelectedDisplayId = mDisplayList.get(preferredDisplayPosition).getDisplayId();

        final ArrayAdapter<String> displayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        displayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDisplaySpinner.setAdapter(displayAdapter);
        mDisplaySpinner.setSelection(preferredDisplayPosition);
    }

    @Override
    public void onDisplayAdded(int displayId) {
        updateDisplayList(null);
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        updateDisplayList(null);
    }

    @Override
    public void onDisplayChanged(int displayId) {
        updateDisplayList(null);
    }

    @Override
    public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
        return new AppListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<AppEntry>> loader, List<AppEntry> data) {
        mAppListAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<AppEntry>> loader) {
        mAppListAdapter.setData(null);
    }
}
