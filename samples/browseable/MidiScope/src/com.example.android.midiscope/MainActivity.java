/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.midiscope;

import android.app.ActionBar;
import android.app.Activity;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.example.android.common.midi.MidiFramer;
import com.example.android.common.midi.MidiOutputPortSelector;
import com.example.android.common.midi.MidiPortWrapper;

import java.util.LinkedList;

/**
 * App that provides a MIDI echo service.
 */
public class MainActivity extends Activity implements ScopeLogger {

    private static final int MAX_LINES = 100;

    private final LinkedList<String> mLogLines = new LinkedList<>();
    private TextView mLog;
    private ScrollView mScroller;
    private MidiOutputPortSelector mLogSenderSelector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        mLog = (TextView) findViewById(R.id.log);
        mScroller = (ScrollView) findViewById(R.id.scroll);

        // Setup MIDI
        MidiManager midiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        // Receiver that prints the messages.
        MidiReceiver loggingReceiver = new LoggingReceiver(this);

        // Receiver that parses raw data into complete messages.
        MidiFramer connectFramer = new MidiFramer(loggingReceiver);

        // Setup a menu to select an input source.
        mLogSenderSelector = new MidiOutputPortSelector(midiManager, this, R.id.spinner_senders) {
            @Override
            public void onPortSelected(final MidiPortWrapper wrapper) {
                super.onPortSelected(wrapper);
                if (wrapper != null) {
                    mLogLines.clear();
                    MidiDeviceInfo deviceInfo = wrapper.getDeviceInfo();
                    if (deviceInfo == null) {
                        log(getString(R.string.header_text));
                    } else {
                        log(MidiPrinter.formatDeviceInfo(deviceInfo));
                    }
                }
            }
        };
        mLogSenderSelector.getSender().connect(connectFramer);

        // Tell the virtual device to log its messages here..
        MidiScope.setScopeLogger(this);
    }

    @Override
    public void onDestroy() {
        mLogSenderSelector.onClose();
        // The scope will live on as a service so we need to tell it to stop
        // writing log messages to this Activity.
        MidiScope.setScopeLogger(null);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        setKeepScreenOn(menu.findItem(R.id.action_keep_screen_on).isChecked());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_all:
                mLogLines.clear();
                logOnUiThread("");
                break;
            case R.id.action_keep_screen_on:
                boolean checked = !item.isChecked();
                setKeepScreenOn(checked);
                item.setChecked(checked);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void log(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logOnUiThread(string);
            }
        });
    }

    /**
     * Logs a message to our TextView. This needs to be called from the UI thread.
     */
    private void logOnUiThread(String s) {
        mLogLines.add(s);
        if (mLogLines.size() > MAX_LINES) {
            mLogLines.removeFirst();
        }
        // Render line buffer to one String.
        StringBuilder sb = new StringBuilder();
        for (String line : mLogLines) {
            sb.append(line).append('\n');
        }
        mLog.setText(sb.toString());
        mScroller.fullScroll(View.FOCUS_DOWN);
    }
}
