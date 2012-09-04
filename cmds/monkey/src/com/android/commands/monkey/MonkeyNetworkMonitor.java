/**
** Copyright 2007, The Android Open Source Project
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

package com.android.commands.monkey;

import android.app.IActivityManager;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;

/**
 * Class for monitoring network connectivity during monkey runs.
 */
public class MonkeyNetworkMonitor extends IIntentReceiver.Stub {
    private static final boolean LDEBUG = false;
    private final IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    private long mCollectionStartTime; // time we started collecting data
    private long mEventTime; // time of last event (connect, disconnect, etc.)
    private int mLastNetworkType = -1; // unknown
    private long mWifiElapsedTime = 0;  // accumulated time spent on wifi since start()
    private long mMobileElapsedTime = 0; // accumulated time spent on mobile since start()
    private long mElapsedTime = 0; // amount of time spent between start() and stop()
    
    public void performReceive(Intent intent, int resultCode, String data, Bundle extras,
            boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
        NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra(
                ConnectivityManager.EXTRA_NETWORK_INFO);
        if (LDEBUG) System.out.println("Network state changed: " 
                + "type=" + ni.getType() + ", state="  + ni.getState());
        updateNetworkStats();
        if (NetworkInfo.State.CONNECTED == ni.getState()) {
            if (LDEBUG) System.out.println("Network connected");
            mLastNetworkType = ni.getType();
        } else if (NetworkInfo.State.DISCONNECTED == ni.getState()) {
            if (LDEBUG) System.out.println("Network not connected");
            mLastNetworkType = -1; // unknown since we're disconnected
        }
        mEventTime = SystemClock.elapsedRealtime();
    }

    private void updateNetworkStats() {
        long timeNow = SystemClock.elapsedRealtime();
        long delta = timeNow - mEventTime;
        switch (mLastNetworkType) {
            case ConnectivityManager.TYPE_MOBILE:
                if (LDEBUG) System.out.println("Adding to mobile: " + delta);
                mMobileElapsedTime += delta;
                break;
            case ConnectivityManager.TYPE_WIFI:
                if (LDEBUG) System.out.println("Adding to wifi: " + delta);
                mWifiElapsedTime += delta;
                break;
            default:
                if (LDEBUG) System.out.println("Unaccounted for: " + delta);
                break;
        }
        mElapsedTime = timeNow - mCollectionStartTime;
    }

    public void start() {
        mWifiElapsedTime = 0;
        mMobileElapsedTime = 0;
        mElapsedTime = 0;
        mEventTime = mCollectionStartTime = SystemClock.elapsedRealtime();
    }

    public void register(IActivityManager am) throws RemoteException {
        if (LDEBUG) System.out.println("registering Receiver");
        am.registerReceiver(null, null, this, filter, null, UserHandle.USER_ALL); 
    }
    
    public void unregister(IActivityManager am) throws RemoteException {
        if (LDEBUG) System.out.println("unregistering Receiver");
        am.unregisterReceiver(this);
    }
    
    public void stop() {
        updateNetworkStats();
    }
    
    public void dump() {
        System.out.println("## Network stats: elapsed time=" + mElapsedTime + "ms (" 
                + mMobileElapsedTime + "ms mobile, "
                + mWifiElapsedTime + "ms wifi, "
                + (mElapsedTime - mMobileElapsedTime - mWifiElapsedTime) + "ms not connected)");
    }
 }