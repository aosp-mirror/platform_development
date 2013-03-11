/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.interactivechart;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
    private InteractiveLineGraphView mGraphView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGraphView = (InteractiveLineGraphView) findViewById(R.id.chart);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_zoom_in:
                mGraphView.zoomIn();
                return true;

            case R.id.action_zoom_out:
                mGraphView.zoomOut();
                return true;

            case R.id.action_pan_left:
                mGraphView.panLeft();
                return true;

            case R.id.action_pan_right:
                mGraphView.panRight();
                return true;

            case R.id.action_pan_up:
                mGraphView.panUp();
                return true;

            case R.id.action_pan_down:
                mGraphView.panDown();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
