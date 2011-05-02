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

package com.android.adb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

/* Main activity for the adb test program */
public class AdbTestActivity extends Activity {

    private static final String TAG = "AdbTestActivity";

    private TextView mLog;
    private UsbManager mManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mDeviceConnection;
    private UsbInterface mInterface;
    private AdbDevice mAdbDevice;

    private static final int MESSAGE_LOG = 1;
    private static final int MESSAGE_DEVICE_ONLINE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.adb);
        mLog = (TextView)findViewById(R.id.log);

        mManager = (UsbManager)getSystemService(Context.USB_SERVICE);

        // check for existing devices
        for (UsbDevice device :  mManager.getDeviceList().values()) {
            UsbInterface intf = findAdbInterface(device);
            if (setAdbInterface(device, intf)) {
                break;
            }
        }

        // listen for new devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        setAdbInterface(null, null);
        super.onDestroy();
    }

    public void log(String s) {
        Message m = Message.obtain(mHandler, MESSAGE_LOG);
        m.obj = s;
        mHandler.sendMessage(m);
    }

    private void appendLog(String text) {
        Rect r = new Rect();
        mLog.getDrawingRect(r);
        int maxLines = r.height() / mLog.getLineHeight() - 1;
        text = mLog.getText() + "\n" + text;

        // see how many lines we have
        int index = text.lastIndexOf('\n');
        int count = 0;
        while (index > 0 && count <= maxLines) {
            count++;
            index = text.lastIndexOf('\n', index - 1);
        }

        // truncate to maxLines
        if (index > 0) {
            text = text.substring(index + 1);
        }
        mLog.setText(text);
    }

    public void deviceOnline(AdbDevice device) {
        Message m = Message.obtain(mHandler, MESSAGE_DEVICE_ONLINE);
        m.obj = device;
        mHandler.sendMessage(m);
    }

    private void handleDeviceOnline(AdbDevice device) {
        log("device online: " + device.getSerial());
        device.openSocket("shell:exec logcat");
    }

    // Sets the current USB device and interface
    private boolean setAdbInterface(UsbDevice device, UsbInterface intf) {
        if (mDeviceConnection != null) {
            if (mInterface != null) {
                mDeviceConnection.releaseInterface(mInterface);
                mInterface = null;
            }
            mDeviceConnection.close();
            mDevice = null;
            mDeviceConnection = null;
        }

        if (device != null && intf != null) {
            UsbDeviceConnection connection = mManager.openDevice(device);
            if (connection != null) {
                log("open succeeded");
                if (connection.claimInterface(intf, false)) {
                    log("claim interface succeeded");
                    mDevice = device;
                    mDeviceConnection = connection;
                    mInterface = intf;
                    mAdbDevice = new AdbDevice(this, mDeviceConnection, intf);
                    log("call start");
                    mAdbDevice.start();
                    return true;
                } else {
                    log("claim interface failed");
                    connection.close();
                }
            } else {
                log("open failed");
            }
        }

        if (mDeviceConnection == null && mAdbDevice != null) {
            mAdbDevice.stop();
            mAdbDevice = null;
        }
        return false;
    }

    // searches for an adb interface on the given USB device
    static private UsbInterface findAdbInterface(UsbDevice device) {
        Log.d(TAG, "findAdbInterface " + device);
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == 255 && intf.getInterfaceSubclass() == 66 &&
                    intf.getInterfaceProtocol() == 1) {
                return intf;
            }
        }
        return null;
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                UsbInterface intf = findAdbInterface(device);
                if (intf != null) {
                    log("Found adb interface " + intf);
                    setAdbInterface(device, intf);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                String deviceName = device.getDeviceName();
                if (mDevice != null && mDevice.equals(deviceName)) {
                    log("adb interface removed");
                    setAdbInterface(null, null);
                }
            }
        }
    };

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_LOG:
                    appendLog((String)msg.obj);
                    break;
                case MESSAGE_DEVICE_ONLINE:
                    handleDeviceOnline((AdbDevice)msg.obj);
                    break;
            }
        }
    };
}


