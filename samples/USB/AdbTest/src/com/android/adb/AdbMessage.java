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

import android.hardware.usb.UsbRequest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* This class encapsulates and adb command packet */
public class AdbMessage {

    // command names
    public static final int A_SYNC = 0x434e5953;
    public static final int A_CNXN = 0x4e584e43;
    public static final int A_OPEN = 0x4e45504f;
    public static final int A_OKAY = 0x59414b4f;
    public static final int A_CLSE = 0x45534c43;
    public static final int A_WRTE = 0x45545257;

    // ADB protocol version
    public static final int A_VERSION = 0x01000000;

    public static final int MAX_PAYLOAD = 4096;

    private final ByteBuffer mMessageBuffer;
    private final ByteBuffer mDataBuffer;

    public AdbMessage() {
        mMessageBuffer = ByteBuffer.allocate(24);
        mDataBuffer = ByteBuffer.allocate(MAX_PAYLOAD);
        mMessageBuffer.order(ByteOrder.LITTLE_ENDIAN);
        mDataBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    // sets the fields in the command header
    public void set(int command, int arg0, int arg1, byte[] data) {
        mMessageBuffer.putInt(0, command);
        mMessageBuffer.putInt(4, arg0);
        mMessageBuffer.putInt(8, arg1);
        mMessageBuffer.putInt(12, (data == null ? 0 : data.length));
        mMessageBuffer.putInt(16, (data == null ? 0 : checksum(data)));
        mMessageBuffer.putInt(20, command ^ 0xFFFFFFFF);
        if (data != null) {
            mDataBuffer.put(data, 0, data.length);
        }
    }

    public void set(int command, int arg0, int arg1) {
        set(command, arg0, arg1, (byte[])null);
    }
    public void set(int command, int arg0, int arg1, String data) {
        // add trailing zero
        data += "\0";
        set(command, arg0, arg1, data.getBytes());
    }

    // returns the command's message ID
    public int getCommand() {
        return mMessageBuffer.getInt(0);
    }

    // returns command's first argument
    public int getArg0() {
        return mMessageBuffer.getInt(4);
    }

    // returns command's second argument
    public int getArg1() {
        return mMessageBuffer.getInt(8);
    }

    // returns command's data buffer
    public ByteBuffer getData() {
        return mDataBuffer;
    }

    // returns command's data length
    public int getDataLength() {
        return mMessageBuffer.getInt(12);
    }

    // returns command's data as a string
    public String getDataString() {
        int length = getDataLength();
        if (length == 0) return null;
        // trim trailing zero
        return new String(mDataBuffer.array(), 0, length - 1);
    }


    public boolean write(AdbDevice device) {
        synchronized (device) {
            UsbRequest request = device.getOutRequest();
            request.setClientData(this);
            if (request.queue(mMessageBuffer, 24)) {
                int length = getDataLength();
                if (length > 0) {
                    request = device.getOutRequest();
                    request.setClientData(this);
                    if (request.queue(mDataBuffer, length)) {
                        return true;
                    } else {
                        device.releaseOutRequest(request);
                        return false;
                    }
                }
                return true;
            } else {
                device.releaseOutRequest(request);
                return false;
            }
        }
    }

    public boolean readCommand(UsbRequest request) {
        request.setClientData(this);
        return request.queue(mMessageBuffer, 24);
    }

    public boolean readData(UsbRequest request, int length) {
        request.setClientData(this);
        return request.queue(mDataBuffer, length);
    }

    private static String extractString(ByteBuffer buffer, int offset, int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = buffer.get(offset++);
        }
        return new String(bytes);
    }

    @Override
    public String toString() {
        String commandName = extractString(mMessageBuffer, 0, 4);
        int dataLength = getDataLength();
        String result = "Adb Message: " + commandName + " arg0: " + getArg0() +
             " arg1: " + getArg1() + " dataLength: " + dataLength;
        if (dataLength > 0) {
            result += (" data: \"" + getDataString() + "\"");
        }
        return result;
    }

    private static int checksum(byte[] data) {
        int result = 0;
        for (int i = 0; i < data.length; i++) {
            int x = data[i];
            // dang, no unsigned ints in java
            if (x < 0) x += 256;
            result += x;
        }
        return result;
    }
}