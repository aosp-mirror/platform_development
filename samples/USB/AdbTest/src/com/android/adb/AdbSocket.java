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

/* This class represents an adb socket.  adb supports multiple independent
 * socket connections to a single device.  Typically a socket is created
 * for each adb command that is executed.
 */
public class AdbSocket {

    private final AdbDevice mDevice;
    private final int mId;
    private int mPeerId;

    public AdbSocket(AdbDevice device, int id) {
        mDevice = device;
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public boolean open(String destination) {
        AdbMessage message = new AdbMessage();
        message.set(AdbMessage.A_OPEN, mId, 0, destination);
        if (! message.write(mDevice)) {
            return false;
        }

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    public void handleMessage(AdbMessage message) {
        switch (message.getCommand()) {
            case AdbMessage.A_OKAY:
                mPeerId = message.getArg0();
                synchronized (this) {
                    notify();
                }
                break;
            case AdbMessage.A_WRTE:
                mDevice.log(message.getDataString());
                sendReady();
                break;
        }
    }

    private void sendReady() {
        AdbMessage message = new AdbMessage();
        message.set(AdbMessage.A_OKAY, mId, mPeerId);
        message.write(mDevice);
    }
}
