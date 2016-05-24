/**
**
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
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Message;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.ServiceManager;
import android.os.ServiceManagerNative;
import android.os.SystemClock;
import android.provider.Settings;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.android.internal.telephony.Phone;
import libcore.io.IoUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import static android.net.NetworkCapabilities.*;

public class Connectivity extends Activity {
    private static final String TAG = "DevToolsConnectivity";
    private static final String GET_SCAN_RES = "Get Results";
    private static final String START_SCAN = "Start Scan";
    private static final String PROGRESS_SCAN = "In Progress";

    private static final long SCAN_CYCLES = 15;

    private static final int EVENT_TOGGLE_WIFI = 1;
    private static final int EVENT_TOGGLE_SCREEN = 2;

    private EditText mDCOnDurationEdit;
    private EditText mDCOffDurationEdit;
    private TextView mDCCycleCountView;
    private long mDCOnDuration = 120000;
    private long mDCOffDuration = 120000;
    private int mDCCycleCount = 0;

    private EditText mSCOnDurationEdit;
    private EditText mSCOffDurationEdit;
    private TextView mSCCycleCountView;
    private long mSCOnDuration = 120000;
    private long mSCOffDuration = 12000;
    private int mSCCycleCount = 0;

    private boolean mDelayedCycleStarted = false;

    private Button mScanButton;
    private TextView mScanResults;
    private EditText mScanCyclesEdit;
    private CheckBox mScanDisconnect;
    private long mScanCycles = SCAN_CYCLES;
    private long mScanCur = -1;
    private long mStartTime = -1;
    private long mStopTime;
    private long mTotalScanTime = 0;
    private long mTotalScanCount = 0;

    private TextView mLinkStatsResults;
    private TextView mHttpRequestResults;

    private String mTdlsAddr = null;

    private WifiManager mWm;
    private WifiManager.MulticastLock mWml;
    private PowerManager mPm;
    private ConnectivityManager mCm;
    private INetworkManagementService mNetd;

    private WifiScanReceiver mScanRecv;
    IntentFilter mIntentFilter;

    private WakeLock mWakeLock = null;
    private WakeLock mScreenonWakeLock = null;

    private boolean mScreenOffToggleRunning = false;
    private boolean mScreenOff = false;

    private static final String CONNECTIVITY_TEST_ALARM =
            "com.android.development.CONNECTIVITY_TEST_ALARM";
    private static final String TEST_ALARM_EXTRA = "CONNECTIVITY_TEST_EXTRA";
    private static final String TEST_ALARM_ON_EXTRA = "CONNECTIVITY_TEST_ON_EXTRA";
    private static final String TEST_ALARM_OFF_EXTRA = "CONNECTIVITY_TEST_OFF_EXTRA";
    private static final String TEST_ALARM_CYCLE_EXTRA = "CONNECTIVITY_TEST_CYCLE_EXTRA";
    private static final String SCREEN_ON = "SCREEN_ON";
    private static final String SCREEN_OFF = "SCREEN_OFF";
    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CONNECTIVITY_TEST_ALARM)) {
                String extra = (String)intent.getExtra(TEST_ALARM_EXTRA);
                PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                Long on = new Long(120000);
                Long off = new Long(120000);
                int cycle = 0;
                try {
                    on = Long.parseLong((String)intent.getExtra(TEST_ALARM_ON_EXTRA));
                    off = Long.parseLong((String)intent.getExtra(TEST_ALARM_OFF_EXTRA));
                    cycle = Integer.parseInt((String)intent.getExtra(TEST_ALARM_CYCLE_EXTRA));
                } catch (Exception e) {}

                if (extra.equals(SCREEN_ON)) {
                    mScreenonWakeLock = mPm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "ConnectivityTest");
                    mScreenonWakeLock.acquire();

                    mSCCycleCount = cycle+1;
                    mSCOnDuration = on;
                    mSCOffDuration = off;
                    mSCCycleCountView.setText(Integer.toString(mSCCycleCount));

                    scheduleAlarm(mSCOnDuration, SCREEN_OFF);
                } else if (extra.equals(SCREEN_OFF)) {

                    mSCCycleCount = cycle;
                    mSCOnDuration = on;
                    mSCOffDuration = off;

                    mScreenonWakeLock.release();
                    mScreenonWakeLock = null;
                    scheduleAlarm(mSCOffDuration, SCREEN_ON);
                    pm.goToSleep(SystemClock.uptimeMillis());
                }
            }
        }
    };

    public Handler mHandler2 = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_TOGGLE_WIFI:
                    Log.e(TAG, "EVENT_TOGGLE_WIFI");
                    if (mDelayedCycleStarted && mWm != null) {
                        long delay;
                        switch (mWm.getWifiState()) {
                            case WifiManager.WIFI_STATE_ENABLED:
                            case WifiManager.WIFI_STATE_ENABLING:
                                mWm.setWifiEnabled(false);
                                delay = mDCOffDuration;
                                break;
                            default:
                                mWm.setWifiEnabled(true);
                                delay = mDCOnDuration;
                                mDCCycleCount++;
                                mDCCycleCountView.setText(Integer.toString(mDCCycleCount));
                        }
                        sendMessageDelayed(obtainMessage(EVENT_TOGGLE_WIFI),
                                delay);
                    }
                    break;
            }
        }
    };

   /**
     * Wifi Scan Listener
     */
    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mStopTime = SystemClock.elapsedRealtime();
                if (mStartTime != -1) {
                    mTotalScanTime += (mStopTime - mStartTime);
                    mStartTime = -1;
                }
                Log.d(TAG, "Scan: READY " + mScanCur);
                mScanResults.setVisibility(View.INVISIBLE);

                List<ScanResult> wifiScanResults = mWm.getScanResults();
                if (wifiScanResults != null) {
                    mTotalScanCount += wifiScanResults.size();
                    mScanResults.setText("Current scan = " + Long.toString(wifiScanResults.size()));
                    mScanResults.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Scan: Results = " + wifiScanResults.size());
                }

                mScanCur--;
                mScanCyclesEdit.setText(Long.toString(mScanCur));
                if (mScanCur == 0) {
                    unregisterReceiver(mScanRecv);
                    mScanButton.setText(GET_SCAN_RES);
                    mScanResults.setVisibility(View.INVISIBLE);
                } else {
                    Log.d(TAG, "Scan: START " + mScanCur);
                    mStartTime = SystemClock.elapsedRealtime();
                    mWm.startScan();
                }
            }
        }
    }

    private static class DevToolsNetworkCallback extends NetworkCallback {
        private static final String TAG = "DevToolsNetworkCallback";

        public void onPreCheck(Network network) {
            Log.d(TAG, "onPreCheck: " + network.netId);
        }

        public void onAvailable(Network network) {
            Log.d(TAG, "onAvailable: " + network.netId);
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) {
            Log.d(TAG, "onCapabilitiesChanged: " + network.netId + " " + nc.toString());
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties lp) {
            Log.d(TAG, "onLinkPropertiesChanged: " + network.netId + " " + lp.toString());
        }

        public void onLosing(Network network, int maxMsToLive) {
            Log.d(TAG, "onLosing: " + network.netId + " " + maxMsToLive);
        }

        public void onLost(Network network) {
            Log.d(TAG, "onLost: " + network.netId);
        }
    }
    private DevToolsNetworkCallback mCallback;

    private class RequestableNetwork {
        private final NetworkRequest mRequest;
        private final int mRequestButton, mReleaseButton, mProgressBar;
        private NetworkCallback mCallback;
        private Network mNetwork;

        public RequestableNetwork(NetworkRequest request, int requestButton, int releaseButton,
                int progressBar) {
            mRequest = request;
            mRequestButton = requestButton;
            mReleaseButton = releaseButton;
            mProgressBar = progressBar;
        }

        public RequestableNetwork(int capability, int requestButton, int releaseButton,
                int progressBar) {
            this(new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addCapability(capability)
                    .build(),
                    requestButton, releaseButton, progressBar);
        }

        public void addOnClickListener() {
            findViewById(mRequestButton).setOnClickListener(
                    new View.OnClickListener() { public void onClick(View v) { request(); }});
            findViewById(mReleaseButton).setOnClickListener(
                    new View.OnClickListener() { public void onClick(View v) { release(); }});
        }

        public void setRequested(boolean requested) {
            findViewById(mRequestButton).setEnabled(!requested);
            findViewById(mReleaseButton).setEnabled(requested);
            findViewById(mProgressBar).setVisibility(
                    requested ? View.VISIBLE : View.GONE);
        }

        public void request() {
            if (mCallback == null) {
                mCallback = new NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        mNetwork = network;
                        onHttpRequestResults(null);
                        runOnUiThread(() -> findViewById(mProgressBar).setVisibility(View.GONE));
                    }
                    @Override
                    public void onLost(Network network) {
                        mNetwork = null;
                        onHttpRequestResults(null);
                    }
                };
                mCm.requestNetwork(mRequest, mCallback);
                setRequested(true);
            }
        }

        public void release() {
            if (mCallback != null) {
                mNetwork = null;
                onHttpRequestResults(null);
                mCm.unregisterNetworkCallback(mCallback);
                mCallback = null;
                setRequested(false);
            }
        }

        public Network getNetwork() {
            return mNetwork;
        }
    }

    private final ArrayList<RequestableNetwork> mRequestableNetworks = new ArrayList<>();
    private final RequestableNetwork mBoundTestNetwork;
    private boolean mRequestRunning;

    private void addRequestableNetwork(RequestableNetwork network) {
        mRequestableNetworks.add(network);
    }

    private void addRequestableNetwork(int capability, int requestButton, int releaseButton,
            int progressBar) {
        mRequestableNetworks.add(new RequestableNetwork(capability, requestButton, releaseButton,
                progressBar));
    }

    public Connectivity() {
        super();
        addRequestableNetwork(NET_CAPABILITY_MMS, R.id.request_mms, R.id.release_mms,
                R.id.mms_progress);
        addRequestableNetwork(NET_CAPABILITY_SUPL, R.id.request_supl, R.id.release_supl,
                R.id.supl_progress);
        addRequestableNetwork(NET_CAPABILITY_INTERNET, R.id.request_cell, R.id.release_cell,
                R.id.cell_progress);

        // Make bound requests use cell data.
        mBoundTestNetwork = mRequestableNetworks.get(mRequestableNetworks.size() - 1);

        NetworkRequest wifiRequest = new NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build();
        addRequestableNetwork(new RequestableNetwork(wifiRequest,
                R.id.request_wifi, R.id.release_wifi, R.id.wifi_progress));
    }

    final NetworkRequest mEmptyRequest = new NetworkRequest.Builder().clearCapabilities().build();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.connectivity);

        mWm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mWml = mWm.createMulticastLock(TAG);
        mPm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNetd = INetworkManagementService.Stub.asInterface(b);

        findViewById(R.id.enableWifi).setOnClickListener(mClickListener);
        findViewById(R.id.disableWifi).setOnClickListener(mClickListener);
        findViewById(R.id.acquireWifiMulticastLock).setOnClickListener(mClickListener);
        findViewById(R.id.releaseWifiMulticastLock).setOnClickListener(mClickListener);
        findViewById(R.id.releaseWifiMulticastLock).setEnabled(false);

        findViewById(R.id.startDelayedCycle).setOnClickListener(mClickListener);
        findViewById(R.id.stopDelayedCycle).setOnClickListener(mClickListener);
        mDCOnDurationEdit = (EditText)findViewById(R.id.dc_wifi_on_duration);
        mDCOnDurationEdit.setText(Long.toString(mDCOnDuration));
        mDCOffDurationEdit = (EditText)findViewById(R.id.dc_wifi_off_duration);
        mDCOffDurationEdit.setText(Long.toString(mDCOffDuration));
        mDCCycleCountView = (TextView)findViewById(R.id.dc_wifi_cycles_done);
        mDCCycleCountView.setText(Integer.toString(mDCCycleCount));

        findViewById(R.id.startScreenCycle).setOnClickListener(mClickListener);
        findViewById(R.id.stopScreenCycle).setOnClickListener(mClickListener);
        mSCOnDurationEdit = (EditText)findViewById(R.id.sc_wifi_on_duration);
        mSCOnDurationEdit.setText(Long.toString(mSCOnDuration));
        mSCOffDurationEdit = (EditText)findViewById(R.id.sc_wifi_off_duration);
        mSCOffDurationEdit.setText(Long.toString(mSCOffDuration));
        mSCCycleCountView = (TextView)findViewById(R.id.sc_wifi_cycles_done);
        mSCCycleCountView.setText(Integer.toString(mSCCycleCount));

        mScanButton = (Button)findViewById(R.id.startScan);
        mScanButton.setOnClickListener(mClickListener);
        mScanCyclesEdit = (EditText)findViewById(R.id.sc_scan_cycles);
        mScanCyclesEdit.setText(Long.toString(mScanCycles));
        mScanDisconnect = (CheckBox)findViewById(R.id.scanDisconnect);
        mScanDisconnect.setChecked(true);
        mScanResults = (TextView)findViewById(R.id.sc_scan_results);
        mScanResults.setVisibility(View.INVISIBLE);

        mScanRecv = new WifiScanReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        findViewById(R.id.startTdls).setOnClickListener(mClickListener);
        findViewById(R.id.stopTdls).setOnClickListener(mClickListener);

        findViewById(R.id.report_all_bad).setOnClickListener(mClickListener);

        findViewById(R.id.default_request).setOnClickListener(mClickListener);
        findViewById(R.id.bound_http_request).setOnClickListener(mClickListener);
        findViewById(R.id.bound_socket_request).setOnClickListener(mClickListener);

        findViewById(R.id.link_stats).setOnClickListener(mClickListener);

        for (RequestableNetwork network : mRequestableNetworks) {
            network.setRequested(false);
            network.addOnClickListener();
        }
        onHttpRequestResults(null);

        registerReceiver(mReceiver, new IntentFilter(CONNECTIVITY_TEST_ALARM));

        mLinkStatsResults = (TextView)findViewById(R.id.stats);
        mLinkStatsResults.setVisibility(View.VISIBLE);

        mHttpRequestResults = (TextView)findViewById(R.id.http_response);
        mHttpRequestResults.setVisibility(View.VISIBLE);

        mCallback = new DevToolsNetworkCallback();
        mCm.registerNetworkCallback(mEmptyRequest, mCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (RequestableNetwork network : mRequestableNetworks) {
            network.release();
        }
        mCm.unregisterNetworkCallback(mCallback);
        mCallback = null;
        unregisterReceiver(mReceiver);
        if (mWml.isHeld()) {
            mWml.release();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        findViewById(R.id.connectivity_layout).requestFocus();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.enableWifi:
                    mWm.setWifiEnabled(true);
                    break;
                case R.id.disableWifi:
                    mWm.setWifiEnabled(false);
                    break;
                case R.id.acquireWifiMulticastLock:
                case R.id.releaseWifiMulticastLock:
                    onWifiMulticastLock(v.getId() == R.id.acquireWifiMulticastLock);
                    break;
                case R.id.startDelayedCycle:
                    onStartDelayedCycle();
                    break;
                case R.id.stopDelayedCycle:
                    onStopDelayedCycle();
                    break;
                case R.id.startScreenCycle:
                    onStartScreenCycle();
                    break;
                case R.id.stopScreenCycle:
                    onStopScreenCycle();
                    break;
                case R.id.startScan:
                    onStartScanCycle();
                    break;
                case R.id.startTdls:
                    onStartTdls();
                    break;
                case R.id.stopTdls:
                    onStopTdls();
                    break;
                case R.id.default_request:
                    onHttpRequest(DEFAULT);
                    break;
                case R.id.bound_http_request:
                    onHttpRequest(HTTPS);
                    break;
                case R.id.bound_socket_request:
                    onHttpRequest(SOCKET);
                    break;
                case R.id.report_all_bad:
                    onReportAllBad();
                    break;
                case R.id.link_stats:
                    onLinkStats();
                    break;
            }
        }
    };


    private void onStartDelayedCycle() {
        if (!mDelayedCycleStarted) {
            mDelayedCycleStarted = true;
            try {
                mDCOnDuration = Long.parseLong(mDCOnDurationEdit.getText().toString());
                mDCOffDuration = Long.parseLong(mDCOffDurationEdit.getText().toString());
            } catch (Exception e) { };
            mDCCycleCount = 0;

            mWakeLock = mPm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ConnectivityTest");
            mWakeLock.acquire();
            mHandler2.sendMessage(mHandler2.obtainMessage(EVENT_TOGGLE_WIFI));
        }
    }

    private void onStopDelayedCycle() {
        if (mDelayedCycleStarted) {
            mDelayedCycleStarted = false;
            mWakeLock.release();
            mWakeLock = null;
            if(mHandler2.hasMessages(EVENT_TOGGLE_WIFI)) {
                mHandler2.removeMessages(EVENT_TOGGLE_WIFI);
            }
        }
    }

    private void onStartScreenCycle() {
        try {
            mSCOnDuration = Long.parseLong(mSCOnDurationEdit.getText().toString());
            mSCOffDuration = Long.parseLong(mSCOffDurationEdit.getText().toString());
        } catch (Exception e) { };
        mSCCycleCount = 0;

        mScreenonWakeLock = mPm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
                "ConnectivityTest");
        mScreenonWakeLock.acquire();

        scheduleAlarm(10, SCREEN_OFF);
    }

    private void scheduleAlarm(long delayMs, String eventType) {
        AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(CONNECTIVITY_TEST_ALARM);

        i.putExtra(TEST_ALARM_EXTRA, eventType);
        i.putExtra(TEST_ALARM_ON_EXTRA, Long.toString(mSCOnDuration));
        i.putExtra(TEST_ALARM_OFF_EXTRA, Long.toString(mSCOffDuration));
        i.putExtra(TEST_ALARM_CYCLE_EXTRA, Integer.toString(mSCCycleCount));

        PendingIntent p = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMs, p);
    }

    private void onStopScreenCycle() {
    }

    private void onReportAllBad() {
        Network[] networks = mCm.getAllNetworks();
        for (Network network : networks) {
            mCm.reportBadNetwork(network);
        }
    }

    private void onStartScanCycle() {
        if (mScanCur == -1) {
            try {
                mScanCur = Long.parseLong(mScanCyclesEdit.getText().toString());
                mScanCycles = mScanCur;
            } catch (Exception e) { };
            if (mScanCur <= 0) {
                mScanCur = -1;
                mScanCycles = SCAN_CYCLES;
                return;
            }
        }
        if (mScanCur > 0) {
            registerReceiver(mScanRecv, mIntentFilter);
            mScanButton.setText(PROGRESS_SCAN);
            mScanResults.setVisibility(View.INVISIBLE);
            if (mScanDisconnect.isChecked())
                mWm.disconnect();
            mTotalScanTime = 0;
            mTotalScanCount = 0;
            Log.d(TAG, "Scan: START " + mScanCur);
            mStartTime = SystemClock.elapsedRealtime();
            mWm.startScan();
        } else {
            // Show results
            mScanResults.setText("Average Scan Time = " +
                Long.toString(mTotalScanTime / mScanCycles) + " ms ; Average Scan Amount = " +
                Long.toString(mTotalScanCount / mScanCycles));
            mScanResults.setVisibility(View.VISIBLE);
            mScanButton.setText(START_SCAN);
            mScanCur = -1;
            mScanCyclesEdit.setText(Long.toString(mScanCycles));
            if (mScanDisconnect.isChecked())
                mWm.reassociate();
        }
    }

    private void onStartTdls() {
        mTdlsAddr = ((EditText)findViewById(R.id.sc_ip_mac)).getText().toString();
        Log.d(TAG, "TDLS: START " + mTdlsAddr);
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(mTdlsAddr);
            mWm.setTdlsEnabled(inetAddress, true);
        } catch (Exception e) {
            mWm.setTdlsEnabledWithMacAddress(mTdlsAddr, true);
        }
    }

    private void onStopTdls() {
        if (mTdlsAddr == null) return;
        Log.d(TAG, "TDLS: STOP " + mTdlsAddr);
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(mTdlsAddr);
            mWm.setTdlsEnabled(inetAddress, false);
        } catch (Exception e) {
            mWm.setTdlsEnabledWithMacAddress(mTdlsAddr, false);
        }
    }

    private void onLinkStats() {
        Log.e(TAG, "LINK STATS:  ");
        try {
            WifiActivityEnergyInfo info =
                    mWm.getControllerActivityEnergyInfo(0);
            if (info != null) {
                mLinkStatsResults.setText(" power " + info.toString());
            } else {
                mLinkStatsResults.setText(" null! ");
            }
        } catch (Exception e) {
            mLinkStatsResults.setText(" failed! " + e.toString());
        }
    }


    private final static int DEFAULT = 0;
    private final static int SOCKET = 1;
    private final static int HTTPS  = 2;

    private void onHttpRequestResults(final String results) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean enabled = !mRequestRunning;
                findViewById(R.id.default_request).setEnabled(enabled);

                enabled = !mRequestRunning && mBoundTestNetwork.getNetwork() != null;
                findViewById(R.id.bound_http_request).setEnabled(enabled);
                findViewById(R.id.bound_socket_request).setEnabled(enabled);

                if (!TextUtils.isEmpty(results) || !mRequestRunning) {
                    ((TextView) findViewById(R.id.http_response)).setText(results);
                }
            }
        });
    }

    private String doSocketRequest(Network network, String host, String path) throws IOException {
        Socket sock = network.getSocketFactory().createSocket(host, 80);
        try {
          sock.setSoTimeout(5000);
          OutputStreamWriter writer = new OutputStreamWriter(sock.getOutputStream());
          String request = String.format(
                  "GET %s HTTP/1.1\nHost: %s\nConnection: close\n\n", path, host);
          writer.write(request);
          writer.flush();
          BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
          String line = reader.readLine();

          if (line == null || !line.startsWith("HTTP/1.1 200")) {
              // Error.
              return "Error: " + line;
          }

          do {
              // Consume headers.
              line = reader.readLine();
          } while (!TextUtils.isEmpty(line));

          // Return first line of body.
          return reader.readLine();
        } finally {
            if (sock != null) {
                IoUtils.closeQuietly(sock);
            }
        }
    }

    private void onHttpRequest(final int type) {
        mRequestRunning = true;
        onHttpRequestResults(null);

        Thread requestThread = new Thread() {
            public void run() {
                final String path = "/ip.js?fmt=text";
                final String randomHost =
                        "h" + Integer.toString(new Random().nextInt()) + ".ds.ipv6test.google.com";
                final String fixedHost = "google-ipv6test.appspot.com";

                Network network = mBoundTestNetwork.getNetwork();
                HttpURLConnection conn = null;
                InputStreamReader in = null;

                try {
                    final URL httpsUrl = new URL("https", fixedHost, path);
                    BufferedReader reader;

                    switch (type) {
                        case DEFAULT:
                            conn = (HttpURLConnection) httpsUrl.openConnection(Proxy.NO_PROXY);
                            in = new InputStreamReader(conn.getInputStream());
                            reader = new BufferedReader(in);
                            onHttpRequestResults(reader.readLine());
                            break;
                        case SOCKET:
                            String response = doSocketRequest(network, randomHost, path);
                            onHttpRequestResults(response);
                            break;
                        case HTTPS:
                            conn = (HttpURLConnection) network.openConnection(httpsUrl,
                                    Proxy.NO_PROXY);
                            in = new InputStreamReader(conn.getInputStream());
                            reader = new BufferedReader(in);
                            onHttpRequestResults(reader.readLine());
                            break;
                        default:
                            throw new IllegalArgumentException("Cannot happen");
                    }
                } catch(IOException e) {
                    onHttpRequestResults("Error! ");
                } finally {
                    mRequestRunning = false;
                    if (in != null) IoUtils.closeQuietly(in);
                    if (conn != null) conn.disconnect();
                }
            }
        };
        requestThread.start();
    }

    private void onWifiMulticastLock(boolean enable) {
        Log.d(TAG, (enable ? "Acquiring" : "Releasing") + " wifi multicast lock");
        if (enable) {
            mWml.acquire();
        } else {
            mWml.release();
        }
        findViewById(R.id.acquireWifiMulticastLock).setEnabled(!enable);
        findViewById(R.id.releaseWifiMulticastLock).setEnabled(enable);
    }
}
