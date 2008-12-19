/*
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.development;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Checkin;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.telephony.ServiceState;
import android.text.format.DateFormat;
import static com.android.internal.util.CharSequences.forAsciiBytes;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.collect.Maps;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * Report radio issues to the StatisticsService.
 */
public class RadioIssueReport extends Activity
{
    private static final String TAG = "RadioIssue";
    private static final int HEADER_SIZE = 24;
    private static final String RADIO_BUFFER_OPTIONS = "-b radio\n-d\n";

    /** List of system properties to snapshot. */
    private static String[] SYSTEM_PROPERTIES = {
        "net.gsm.radio-reset",
        "net.gsm.attempt-gprs",
        "net.gsm.succeed-gprs",
        "net.gsm.disconnect",
        "net.ppp.sent",
        "net.ppp.received",
        "gsm.version.baseband",
        "gsm.version.ril-impl",
    };

    private Button          mSubmitButton;
    private EditText        mReportText;
    private ServiceState mServiceState;
    private Phone.State     mPhoneState;
    private int             mSignalStrength;
    private Phone.DataState mDataState;
    private String          mRadioLog;

    /** Snapshot of interesting variables relevant to the radio. */
    private Map<String, String> mRadioState;

    @Override
    public void
    onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.radio_issue);

        initSubmitButton();
        initReportText();

        mRadioState = snapState();
    }

    /**
     * @return a snapshot of phone state variables to report.
     */
    private static Map<String, String> snapState() {
        Map<String, String> state = Maps.newHashMap();

        // Capture a bunch of system properties
        for (String property: SYSTEM_PROPERTIES) {
            String value = SystemProperties.get(property);
            state.put(property, SystemProperties.get(property));
        }

        Phone phone = PhoneFactory.getDefaultPhone();
        state.put("phone-data", phone.getDataConnectionState().toString());
        state.put("phone-service", phone.getServiceState().toString());
        state.put("phone-signal", String.valueOf(phone.getSignalStrengthASU()));
        state.put("phone-state", phone.getState().toString());

        try {
            state.put("radio-log", getRadioLog());
        } catch (IOException e) {
            Log.e(TAG, "Error reading radio log", e);
        }

        return state;
    }

    private void initSubmitButton() {
        mSubmitButton = (Button) findViewById(R.id.submit);
        mSubmitButton.setOnClickListener(mSubmitButtonHandler);
    }

    private void initReportText() {
        mReportText = (EditText) findViewById(R.id.report_text);
        mReportText.requestFocus();
    }

    OnClickListener mSubmitButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            // Include the user-supplied report text.
            mRadioState.put("user-report", mReportText.getText().toString());

            // Dump the state variables directly into the report.
            Checkin.logEvent(getContentResolver(),
                    Checkin.Events.Tag.RADIO_BUG_REPORT,
                    mRadioState.toString());

            finish();
        }
    };

    // Largely stolen from LogViewer.java
    private static String getRadioLog() throws IOException {
        Socket sock = new Socket("127.0.0.1", 5040);
        DataInputStream in = new DataInputStream(sock.getInputStream());
        StringBuilder log = new StringBuilder();

        // Set options
        sock.getOutputStream().write(RADIO_BUFFER_OPTIONS.getBytes());
        sock.getOutputStream().write('\n');
        sock.getOutputStream().write('\n');

        // Read in the log
        try {
            Calendar cal = new GregorianCalendar();

            while (true) {
                int length = in.readInt();
                long when = (long)in.readInt();
                byte[] bytes = new byte[length-4];
                in.readFully(bytes);

                int tagEnd = next0(bytes, HEADER_SIZE-4);
                int fileEnd = next0(bytes, tagEnd + 1);
                int messageEnd = next0(bytes, fileEnd + 1);

                CharSequence tag
                        = forAsciiBytes(bytes, HEADER_SIZE-4, tagEnd);
                CharSequence message
                        = forAsciiBytes(bytes, fileEnd + 1, messageEnd);

                cal.setTimeInMillis(when*1000);
                log.append(DateFormat.format("MM-dd kk:mm:ss ", cal));
                log.append(tag)
                   .append(": ")
                   .append(message)
                   .append("\n");
            }
        } catch (EOFException e) {
            Log.d(TAG, "reached end of stream");
        }

        return log.toString();
    }

    private static int next0(byte[] bytes, int start) {
        for (int current = start; current < bytes.length; current++) {
            if (bytes[current] == 0)
                return current;
        }
        return bytes.length;
    }
}
