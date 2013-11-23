/*
 * Copyright (C) 20013The Android Open Source Project
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

package com.example.android.apis.hardware;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.hardware.ConsumerIrManager;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import com.example.android.apis.R;

/**
 * App that transmit an IR code
 *
 * <p>This demonstrates the {@link android.hardware.ConsumerIrManager android.hardware.ConsumerIrManager} class.
 *
 * <h4>Demo</h4>
 * Hardware / Consumer IR
 *
 * <h4>Source files</h4>
 * <table class="LinkTable">
 *         <tr>
 *             <td>src/com.example.android.apis/hardware/ConsumerIr.java</td>
 *             <td>Consumer IR demo</td>
 *         </tr>
 *         <tr>
 *             <td>res/any/layout/consumer_ir.xml</td>
 *             <td>Defines contents of the screen</td>
 *         </tr>
 * </table>
 */
public class ConsumerIr extends Activity {
    private static final String TAG = "ConsumerIrTest";
    TextView mFreqsText;
    ConsumerIrManager mCIR;

    /**
     * Initialization of the Activity after it is first created.  Must at least
     * call {@link android.app.Activity#setContentView setContentView()} to
     * describe what is to be displayed in the screen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Get a reference to the ConsumerIrManager
        mCIR = (ConsumerIrManager)getSystemService(Context.CONSUMER_IR_SERVICE);

        // See assets/res/any/layout/consumer_ir.xml for this
        // view layout definition, which is being set here as
        // the content of our screen.
        setContentView(R.layout.consumer_ir);

        // Set the OnClickListener for the button so we see when it's pressed.
        findViewById(R.id.send_button).setOnClickListener(mSendClickListener);
        findViewById(R.id.get_freqs_button).setOnClickListener(mGetFreqsClickListener);
        mFreqsText = (TextView) findViewById(R.id.freqs_text);
    }

    View.OnClickListener mSendClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (!mCIR.hasIrEmitter()) {
                Log.e(TAG, "No IR Emitter found\n");
                return;
            }

            // A pattern of alternating series of carrier on and off periods measured in
            // microseconds.
            int[] pattern = {1901, 4453, 625, 1614, 625, 1588, 625, 1614, 625, 442, 625, 442, 625,
                468, 625, 442, 625, 494, 572, 1614, 625, 1588, 625, 1614, 625, 494, 572, 442, 651,
                442, 625, 442, 625, 442, 625, 1614, 625, 1588, 651, 1588, 625, 442, 625, 494, 598,
                442, 625, 442, 625, 520, 572, 442, 625, 442, 625, 442, 651, 1588, 625, 1614, 625,
                1588, 625, 1614, 625, 1588, 625, 48958};

            // transmit the pattern at 38.4KHz
            mCIR.transmit(38400, pattern);
        }
    };

    View.OnClickListener mGetFreqsClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            StringBuilder b = new StringBuilder();

            if (!mCIR.hasIrEmitter()) {
                mFreqsText.setText("No IR Emitter found!");
                Log.e(TAG, "No IR Emitter found!\n");
                return;
            }

            // Get the available carrier frequency ranges
            ConsumerIrManager.CarrierFrequencyRange[] freqs = mCIR.getCarrierFrequencies();
            b.append("IR Carrier Frequencies:\n");
            for (ConsumerIrManager.CarrierFrequencyRange range : freqs) {
                b.append(String.format("    %d - %d\n", range.getMinFrequency(),
                            range.getMaxFrequency()));
            }
            mFreqsText.setText(b.toString());
        }
    };
}
