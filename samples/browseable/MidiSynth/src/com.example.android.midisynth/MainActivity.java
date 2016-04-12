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

package com.example.android.midisynth;

import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice.MidiConnection;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.Toolbar;

import com.example.android.common.midi.MidiOutputPortConnectionSelector;
import com.example.android.common.midi.MidiPortConnector;
import com.example.android.common.midi.MidiTools;

/**
 * Simple synthesizer as a MIDI Device.
 */
public class MainActivity extends Activity {
    static final String TAG = "MidiSynthExample";

    private MidiManager mMidiManager;
    private MidiOutputPortConnectionSelector mPortSelector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        }
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

    private void setupMidi() {
        // Setup MIDI
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        MidiDeviceInfo synthInfo = MidiTools.findDevice(mMidiManager, "AndroidTest",
                "SynthExample");
        int portIndex = 0;
        mPortSelector = new MidiOutputPortConnectionSelector(mMidiManager, this,
                R.id.spinner_synth_sender, synthInfo, portIndex);
        mPortSelector.setConnectedListener(new MyPortsConnectedListener());
    }

    private void closeSynthResources() {
        if (mPortSelector != null) {
            mPortSelector.close();
        }
    }

    // TODO A better way would be to listen to the synth server
    // for open/close events and then disable/enable the spinner.
    private class MyPortsConnectedListener
            implements MidiPortConnector.OnPortsConnectedListener {
        @Override
        public void onPortsConnected(final MidiConnection connection) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (connection == null) {
                        Toast.makeText(MainActivity.this,
                                R.string.error_port_busy, Toast.LENGTH_SHORT)
                                .show();
                        mPortSelector.clearSelection();
                    } else {
                        Toast.makeText(MainActivity.this,
                                R.string.port_open_ok, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        closeSynthResources();
        super.onDestroy();
    }

}
