/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.apis.telephony;

import android.app.Activity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.widget.TextView;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import com.example.android.apis.R;

/**
 * Activity that uses {@link android.telephony.TelephonyManager} to obtain
 * telephony parameters like network state, phone type and SIM state.
 */
public class NetworkDetector extends Activity {

    /*
     * SIM state constants
     */
    public static final String SIM_ABSENT = "Absent";
    public static final String SIM_READY = "Ready";
    public static final String SIM_PIN_REQUIRED = "PIN required";
    public static final String SIM_PUK_REQUIRED = "PUK required";
    public static final String SIM_NETWORK_LOCKED = "Network locked";
    public static final String SIM_UNKNOWN = "Unknown";

    /*
     * Network type constants
     */
    public static final String NETWORK_CDMA = "CDMA: Either IS95A or IS95B (2G)";
    public static final String NETWORK_EDGE = "EDGE (2.75G)";
    public static final String NETWORK_GPRS = "GPRS (2.5G)";
    public static final String NETWORK_UMTS = "UMTS (3G)";
    public static final String NETWORK_EVDO_0 = "EVDO revision 0 (3G)";
    public static final String NETWORK_EVDO_A = "EVDO revision A (3G - Transitional)";
    public static final String NETWORK_EVDO_B = "EVDO revision B (3G - Transitional)";
    public static final String NETWORK_1X_RTT = "1xRTT  (2G - Transitional)";
    public static final String NETWORK_HSDPA = "HSDPA (3G - Transitional)";
    public static final String NETWORK_HSUPA = "HSUPA (3G - Transitional)";
    public static final String NETWORK_HSPA = "HSPA (3G - Transitional)";
    public static final String NETWORK_IDEN = "iDen (2G)";
    public static final String NETWORK_LTE = "LTE (4G)";
    public static final String NETWORK_EHRPD = "EHRPD (3G)";
    public static final String NETWORK_HSPAP = "HSPAP (3G)";
    public static final String NETWORK_UNKOWN = "Unknown";

    /*
     * Phone type constants
     */
    public static final String PHONE_CDMA = "CDMA";
    public static final String PHONE_GSM = "GSM";
    public static final String PHONE_SIP = "SIP";
    public static final String PHONE_NONE = "No radio";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.network_detector);

        // Get the telephony system service to find out network details
        final TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        // Update text views with readable values.
        updateViews(tm);

        // Since these attributes can change, we will register a
        // {@code PhoneStateListener} to listen for these changes and
        // update the view.
        tm.listen(new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                // Update our TextViews
                updateViews(tm);
            }

            @Override
            public void onDataConnectionStateChanged(int state) {
                // A change in data connection state may be due to availability
                // of a different network type
                updateViews(tm);
            }

        }, PhoneStateListener.LISTEN_SERVICE_STATE
                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        // Update text views with readable values.
        updateViews(tm);
    }

    /**
     * Update text views with telephony attributes.
     */
    private final void updateViews(TelephonyManager tm) {

        // The telephony system service returns integer constants for various
        // telephony attributes.
        TextView view = null;

        view = (TextView) findViewById(R.id.sim_state);
        view.setText("SIM State: " + mapSimStateToName(tm.getSimState()));

        view = (TextView) findViewById(R.id.network_type);
        view.setText("Network Type: " + mapNetworkTypeToName(tm.getNetworkType()));

        view = (TextView) findViewById(R.id.phone_type);
        view.setText("Phone Type: " + mapDeviceTypeToName(tm.getPhoneType()));

        view = (TextView) findViewById(R.id.network_name);
        view.setText("Network Operator: " + tm.getNetworkOperatorName());
    }

    /**
     * Returns a string describing the current SIM state.
     */
    private static String mapSimStateToName(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return SIM_ABSENT;
            case TelephonyManager.SIM_STATE_READY:
                return SIM_READY;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return SIM_PIN_REQUIRED;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return SIM_PUK_REQUIRED;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return SIM_NETWORK_LOCKED;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return SIM_UNKNOWN;
            default:
                // shouldn't happen.
                return null;
        }
    }

    /**
     * Returns a string indicating the phone radio type.
     */
    private static String mapDeviceTypeToName(int device) {

        switch (device) {
            case TelephonyManager.PHONE_TYPE_CDMA:
                return PHONE_CDMA;
            case TelephonyManager.PHONE_TYPE_GSM:
                return PHONE_GSM;
            case TelephonyManager.PHONE_TYPE_SIP:
               return PHONE_SIP;
            case TelephonyManager.PHONE_TYPE_NONE:
                return PHONE_NONE;
            default:
                // shouldn't happen.
                return null;
        }
    }

    /**
     * Returns a string describing the network type.
     */
    public static String mapNetworkTypeToName(int networkType) {

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return NETWORK_CDMA;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return NETWORK_EDGE;
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return NETWORK_EDGE;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return NETWORK_UMTS;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return NETWORK_EVDO_0;
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return NETWORK_EVDO_A;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return NETWORK_EVDO_B;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return NETWORK_1X_RTT;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return NETWORK_HSDPA;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return NETWORK_HSPA;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return NETWORK_HSUPA;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORK_IDEN;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NETWORK_LTE;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return NETWORK_EHRPD;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORK_HSPAP;
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                return NETWORK_UNKOWN;
        }
    }
}
