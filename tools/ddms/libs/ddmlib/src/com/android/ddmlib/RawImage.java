/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmlib;

import java.nio.ByteBuffer;

/**
 * Data representing an image taken from a device frame buffer.
 */
public final class RawImage {
    public int version;
    public int bpp;
    public int size;
    public int width;
    public int height;
    public int red_offset;
    public int red_length;
    public int blue_offset;
    public int blue_length;
    public int green_offset;
    public int green_length;
    public int alpha_offset;
    public int alpha_length;

    public byte[] data;

    /**
     * Reads the header of a RawImage from a {@link ByteBuffer}.
     * <p/>The way the data is sent over adb is defined in system/core/adb/framebuffer_service.c
     * @param version the version of the protocol.
     * @param buf the buffer to read from.
     * @return true if success
     */
    public boolean readHeader(int version, ByteBuffer buf) {
        this.version = version;

        if (version == 16) {
            // compatibility mode with original protocol
            this.bpp = 16;

            // read actual values.
            this.size = buf.getInt();
            this.width = buf.getInt();
            this.height = buf.getInt();

            // create default values for the rest. Format is 565
            this.red_offset = 11;
            this.red_length = 5;
            this.green_offset = 5;
            this.green_length = 6;
            this.blue_offset = 0;
            this.blue_length = 5;
            this.alpha_offset = 0;
            this.alpha_length = 0;
        } else if (version == 1) {
            this.bpp = buf.getInt();
            this.size = buf.getInt();
            this.width = buf.getInt();
            this.height = buf.getInt();
            this.red_offset = buf.getInt();
            this.red_length = buf.getInt();
            this.blue_offset = buf.getInt();
            this.blue_length = buf.getInt();
            this.green_offset = buf.getInt();
            this.green_length = buf.getInt();
            this.alpha_offset = buf.getInt();
            this.alpha_length = buf.getInt();
        } else {
            // unsupported protocol!
            return false;
        }

        return true;
    }

    /**
     * Returns the mask value for the red color.
     * <p/>This value is compatible with org.eclipse.swt.graphics.PaletteData
     */
    public int getRedMask() {
        return getMask(red_length, red_offset);
    }

    /**
     * Returns the mask value for the green color.
     * <p/>This value is compatible with org.eclipse.swt.graphics.PaletteData
     */
    public int getGreenMask() {
        return getMask(green_length, green_offset);
    }

    /**
     * Returns the mask value for the blue color.
     * <p/>This value is compatible with org.eclipse.swt.graphics.PaletteData
     */
    public int getBlueMask() {
        return getMask(blue_length, blue_offset);
    }

    /**
     * Returns the size of the header for a specific version of the framebuffer adb protocol.
     * @param version the version of the protocol
     * @return the number of int that makes up the header.
     */
    public static int getHeaderSize(int version) {
        switch (version) {
            case 16: // compatibility mode
                return 3; // size, width, height
            case 1:
                return 12; // bpp, size, width, height, 4*(length, offset)
        }

        return 0;
    }

    /**
     * creates a mask value based on a length and offset.
     * <p/>This value is compatible with org.eclipse.swt.graphics.PaletteData
     */
    private int getMask(int length, int offset) {
        int res = 0;
        for (int i = 0 ; i < length ; i++) {
            res = (res << 1) + 1;
        }

        res = res << offset;

        // if the bpp is 32 bits then we need to invert it because the buffer is in little endian
        if (bpp == 32) {
            return Integer.reverseBytes(res);
        }

        return res;
    }
}
