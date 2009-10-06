/* //device/apps/Settings/src/com/android/settings/Keyguard.java
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
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Message;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.ServiceManager;
import android.os.ServiceManagerNative;
import android.os.SystemClock;
import android.provider.Settings;
import android.os.Bundle;
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;

public class Connectivity extends Activity {
    private static final String TAG = "Connectivity";

    private static final int EVENT_TOGGLE_WIFI = 1;
    private static final int EVENT_TOGGLE_SCREEN = 2;

    private Button mEnableWifiButton;
    private Button mDisableWifiButton;

    private Button mStartDelayedCycleButton;
    private Button mStopDelayedCycleButton;
    private EditText mDCOnDurationEdit;
    private EditText mDCOffDurationEdit;
    private TextView mDCCycleCountView;
    private long mDCOnDuration = 120000;
    private long mDCOffDuration = 120000;
    private int mDCCycleCount = 0;

    private Button mStartScreenCycleButton;
    private Button mStopScreenCycleButton;
    private EditText mSCOnDurationEdit;
    private EditText mSCOffDurationEdit;
    private TextView mSCCycleCountView;
    private long mSCOnDuration = 120000;
    private long mSCOffDuration = 12000;
    private int mSCCycleCount = 0;

    private Button mStartMmsButton;
    private Button mStopMmsButton;
    private Button mStartHiPriButton;
    private Button mStopHiPriButton;
    private Button mCrashButton;

    private boolean mDelayedCycleStarted = false;

    private WifiManager mWm;
    private PowerManager mPm;
    private ConnectivityManager mCm;

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

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.connectivity);

        mWm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mPm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mEnableWifiButton = (Button)findViewById(R.id.enableWifi);
        mEnableWifiButton.setOnClickListener(mEnableWifiClicked);
        mDisableWifiButton = (Button)findViewById(R.id.disableWifi);
        mDisableWifiButton.setOnClickListener(mDisableWifiClicked);

        mStartDelayedCycleButton = (Button)findViewById(R.id.startDelayedCycle);
        mStartDelayedCycleButton.setOnClickListener(mStartDelayedCycleClicked);
        mStopDelayedCycleButton = (Button)findViewById(R.id.stopDelayedCycle);
        mStopDelayedCycleButton.setOnClickListener(mStopDelayedCycleClicked);
        mDCOnDurationEdit = (EditText)findViewById(R.id.dc_wifi_on_duration);
        mDCOnDurationEdit.setText(Long.toString(mDCOnDuration));
        mDCOffDurationEdit = (EditText)findViewById(R.id.dc_wifi_off_duration);
        mDCOffDurationEdit.setText(Long.toString(mDCOffDuration));
        mDCCycleCountView = (TextView)findViewById(R.id.dc_wifi_cycles_done);
        mDCCycleCountView.setText(Integer.toString(mDCCycleCount));

        mStartScreenCycleButton = (Button)findViewById(R.id.startScreenCycle);
        mStartScreenCycleButton.setOnClickListener(mStartScreenCycleClicked);
        mStopScreenCycleButton = (Button)findViewById(R.id.stopScreenCycle);
        mStopScreenCycleButton.setOnClickListener(mStopScreenCycleClicked);
        mSCOnDurationEdit = (EditText)findViewById(R.id.sc_wifi_on_duration);
        mSCOnDurationEdit.setText(Long.toString(mSCOnDuration));
        mSCOffDurationEdit = (EditText)findViewById(R.id.sc_wifi_off_duration);
        mSCOffDurationEdit.setText(Long.toString(mSCOffDuration));
        mSCCycleCountView = (TextView)findViewById(R.id.sc_wifi_cycles_done);
        mSCCycleCountView.setText(Integer.toString(mSCCycleCount));

        mStartMmsButton = (Button)findViewById(R.id.start_mms);
        mStartMmsButton.setOnClickListener(mStartMmsClicked);
        mStopMmsButton = (Button)findViewById(R.id.stop_mms);
        mStopMmsButton.setOnClickListener(mStopMmsClicked);
        mStartHiPriButton = (Button)findViewById(R.id.start_hipri);
        mStartHiPriButton.setOnClickListener(mStartHiPriClicked);
        mStopHiPriButton = (Button)findViewById(R.id.stop_hipri);
        mStopHiPriButton.setOnClickListener(mStopHiPriClicked);
        mCrashButton = (Button)findViewById(R.id.crash);
        mCrashButton.setOnClickListener(mCrashClicked);

        registerReceiver(mReceiver, new IntentFilter(CONNECTIVITY_TEST_ALARM));
    }



    @Override
    public void onResume() {
        super.onResume();
    }

    private View.OnClickListener mStartDelayedCycleClicked = new View.OnClickListener() {
        public void onClick(View v) {
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
    };
    private View.OnClickListener mStopDelayedCycleClicked = new View.OnClickListener() {
        public void onClick(View v) {
            if (mDelayedCycleStarted) {
                mDelayedCycleStarted = false;
                mWakeLock.release();
                mWakeLock = null;
                if(mHandler2.hasMessages(EVENT_TOGGLE_WIFI)) {
                    mHandler2.removeMessages(EVENT_TOGGLE_WIFI);
                }
            }
        }
    };

    private View.OnClickListener mEnableWifiClicked = new View.OnClickListener() {
        public void onClick(View v) {
            mWm.setWifiEnabled(true);
        }
    };
    private View.OnClickListener mDisableWifiClicked = new View.OnClickListener() {
        public void onClick(View v) {
            mWm.setWifiEnabled(false);
        }
    };

    private View.OnClickListener mStartScreenCycleClicked = new View.OnClickListener() {
        public void onClick(View v) {

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
    };

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

    private View.OnClickListener mStopScreenCycleClicked = new View.OnClickListener() {
        public void onClick(View v) {
        }
    };

    private View.OnClickListener mStartMmsClicked = new View.OnClickListener() {
        public void onClick(View v) {
            mCm.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, Phone.FEATURE_ENABLE_MMS);
        }
    };

    private View.OnClickListener mStopMmsClicked = new View.OnClickListener() {
        public void onClick(View v) {
            mCm.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, Phone.FEATURE_ENABLE_MMS);
        }
    };

    private View.OnClickListener mStartHiPriClicked = new View.OnClickListener() {
        public void onClick(View v) {
            mCm.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                    Phone.FEATURE_ENABLE_HIPRI);
        }
    };

    private View.OnClickListener mStopHiPriClicked = new View.OnClickListener() {
        public void onClick(View v) {
            mCm.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                    Phone.FEATURE_ENABLE_HIPRI);
        }
    };

    private View.OnClickListener mCrashClicked = new View.OnClickListener() {
        public void onClick(View v) {
            ConnectivityManager foo = null;
            foo.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                    Phone.FEATURE_ENABLE_MMS);
        }
    };
}
