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

package com.android.missilelauncher;

import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MissileLauncherActivity extends Activity
        implements View.OnClickListener, Runnable {

    private static final String TAG = "MissileLauncherActivity";

    private Button mFire;
    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint mEndpointIntr;
    private SensorManager mSensorManager;
    private Sensor mGravitySensor;

    // USB control commands
    private static final int COMMAND_UP = 1;
    private static final int COMMAND_DOWN = 2;
    private static final int COMMAND_RIGHT = 4;
    private static final int COMMAND_LEFT = 8;
    private static final int COMMAND_FIRE = 16;
    private static final int COMMAND_STOP = 32;
    private static final int COMMAND_STATUS = 64;

    // constants for accelerometer orientation
    private static final int TILT_LEFT = 1;
    private static final int TILT_RIGHT = 2;
    private static final int TILT_UP = 4;
    private static final int TILT_DOWN = 8;
    private static final double THRESHOLD = 5.0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.launcher);
        mFire = (Button)findViewById(R.id.fire);
        mFire.setOnClickListener(this);

        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mGravityListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(mGravityListener, mGravitySensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        Intent intent = getIntent();
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();

        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            setDevice(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (mDevice != null && mDevice.equals(device)) {
                setDevice(null);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setDevice(UsbDevice device) {
        Log.d(TAG, "setDevice " + device);
        if (device.getInterfaceCount() != 1) {
            Log.e(TAG, "could not find interface");
            return;
        }
        UsbInterface intf = device.getInterface(0);
        // device should have one endpoint
        if (intf.getEndpointCount() != 1) {
            Log.e(TAG, "could not find endpoint");
            return;
        }
        // endpoint should be of type interrupt
        UsbEndpoint ep = intf.getEndpoint(0);
        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_INT) {
            Log.e(TAG, "endpoint is not interrupt type");
            return;
        }
        mDevice = device;
        mEndpointIntr = ep;
        if (device != null) {
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null && connection.claimInterface(intf, true)) {
                Log.d(TAG, "open SUCCESS");
                mConnection = connection;
                Thread thread = new Thread(this);
                thread.start();

            } else {
                Log.d(TAG, "open FAIL");
                mConnection = null;
            }
         }
    }

    private void sendCommand(int control) {
        synchronized (this) {
            if (control != COMMAND_STATUS) {
                Log.d(TAG, "sendMove " + control);
            }
            if (mConnection != null) {
                byte[] message = new byte[1];
                message[0] = (byte)control;
                // Send command via a control request on endpoint zero
                mConnection.controlTransfer(0x21, 0x9, 0x200, 0, message, message.length, 0);
            }
        }
    }

    public void onClick(View v) {
        if (v == mFire) {
            sendCommand(COMMAND_FIRE);
        }
    }

    private int mLastValue = 0;

    SensorEventListener mGravityListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {

            // compute current tilt
            int value = 0;
            if (event.values[0] < -THRESHOLD) {
                value += TILT_LEFT;
            } else if (event.values[0] > THRESHOLD) {
                value += TILT_RIGHT;
            }
            if (event.values[1] < -THRESHOLD) {
                value += TILT_UP;
            } else if (event.values[1] > THRESHOLD) {
                value += TILT_DOWN;
            }

            if (value != mLastValue) {
                mLastValue = value;
                // send motion command if the tilt changed
                switch (value) {
                    case TILT_LEFT:
                        sendCommand(COMMAND_LEFT);
                        break;
                    case TILT_RIGHT:
                       sendCommand(COMMAND_RIGHT);
                        break;
                    case TILT_UP:
                        sendCommand(COMMAND_UP);
                        break;
                    case TILT_DOWN:
                        sendCommand(COMMAND_DOWN);
                        break;
                    default:
                        sendCommand(COMMAND_STOP);
                        break;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // ignore
        }
    };

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        UsbRequest request = new UsbRequest();
        request.initialize(mConnection, mEndpointIntr);
        byte status = -1;
        while (true) {
            // queue a request on the interrupt endpoint
            request.queue(buffer, 1);
            // send poll status command
            sendCommand(COMMAND_STATUS);
            // wait for status event
            if (mConnection.requestWait() == request) {
                byte newStatus = buffer.get(0);
                if (newStatus != status) {
                    Log.d(TAG, "got status " + newStatus);
                    status = newStatus;
                    if ((status & COMMAND_FIRE) != 0) {
                        // stop firing
                        sendCommand(COMMAND_STOP);
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            } else {
                Log.e(TAG, "requestWait failed, exiting");
                break;
            }
        }
    }
}


