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

package com.android.ddmuilib;

import com.android.ddmlib.Log;

import org.eclipse.swt.graphics.ImageData;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Compensate for SWT issues by writing our own PNGs from ImageData.
 */
public class WritePng {
    private WritePng() {}

    private static final byte[] PNG_MAGIC =
        new byte[] { -119, 80, 78, 71, 13, 10, 26, 10 };

    public static void savePng(String fileName, ImageData imageData)
        throws IOException {

        try {
            FileOutputStream out = new FileOutputStream(fileName);

            Log.d("ddms", "Saving to PNG, width=" + imageData.width
                + ", height=" + imageData.height
                + ", depth=" + imageData.depth
                + ", bpl=" + imageData.bytesPerLine);

            savePng(out, imageData);

            // need to do that on, or the file is empty on windows!
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e("writepng", e);
        }
    }

    /*
     * Supply functionality missing from our version of SWT.
     */
    private static void savePng(OutputStream out, ImageData imageData)
        throws IOException {

        int width = imageData.width;
        int height = imageData.height;
        byte[] out24;

        Log.i("ddms-png", "Convert to 24bit from " + imageData.depth);

        if (imageData.depth == 24 || imageData.depth == 32) {
            out24 = convertTo24ForPng(imageData.data, width, height,
                imageData.depth, imageData.bytesPerLine);
        } else if (imageData.depth == 16) {
            out24 = convert16to24(imageData);
        } else {
            return;
        }

        // Create the compressed form.  I'm taking the low road here and
        // just creating a large buffer, which should always be enough to
        // hold the compressed output.
        byte[] compPixels = new byte[out24.length + 16384];
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);
        compressor.setInput(out24);
        compressor.finish();
        int compLen;
        do {        // must do this in a loop to satisfy java.util.Zip
            compLen = compressor.deflate(compPixels);
            assert compLen != 0 || !compressor.needsInput();
        } while (compLen == 0);
        Log.d("ddms", "Compressed image data from " + out24.length
            + " to " + compLen);

        // Write the PNG magic
        out.write(PNG_MAGIC);

        ByteBuffer buf;
        CRC32 crc;

        // Write the IHDR chunk (13 bytes)
        byte[] header = new byte[8 + 13 + 4];
        buf = ByteBuffer.wrap(header);
        buf.order(ByteOrder.BIG_ENDIAN);

        putChunkHeader(buf, 13, "IHDR");
        buf.putInt(width);
        buf.putInt(height);
        buf.put((byte) 8);       // 8pp
        buf.put((byte) 2);       // direct color used
        buf.put((byte) 0);       // compression method == deflate
        buf.put((byte) 0);       // filter method (none)
        buf.put((byte) 0);       // interlace method (none)

        crc = new CRC32();
        crc.update(header, 4, 4+13);
        buf.putInt((int)crc.getValue());

        out.write(header);

        // Write the IDAT chunk
        byte[] datHdr = new byte[8 + 0 + 4];
        buf = ByteBuffer.wrap(datHdr);
        buf.order(ByteOrder.BIG_ENDIAN);

        putChunkHeader(buf, compLen, "IDAT");
        crc = new CRC32();
        crc.update(datHdr, 4, 4+0);
        crc.update(compPixels, 0, compLen);
        buf.putInt((int) crc.getValue());

        out.write(datHdr, 0, 8);
        out.write(compPixels, 0, compLen);
        out.write(datHdr, 8, 4);

        // Write the IEND chunk (0 bytes)
        byte[] trailer = new byte[8 + 0 + 4];

        buf = ByteBuffer.wrap(trailer);
        buf.order(ByteOrder.BIG_ENDIAN);
        putChunkHeader(buf, 0, "IEND");

        crc = new CRC32();
        crc.update(trailer, 4, 4+0);
        buf.putInt((int)crc.getValue());

        out.write(trailer);
    }

    /*
     * Output a chunk header.
     */
    private static void putChunkHeader(ByteBuffer buf, int length,
        String typeStr) {

        int type = 0;

        if (typeStr.length() != 4)
            throw new RuntimeException();

        for (int i = 0; i < 4; i++) {
            type <<= 8;
            type |= (byte) typeStr.charAt(i);
        }

        buf.putInt(length);
        buf.putInt(type);
    }

    /*
     * Convert raw pixels to 24-bit RGB with a "filter" byte at the start
     * of each row.
     */
    private static byte[] convertTo24ForPng(byte[] in, int width, int height,
        int depth, int stride) {

        assert depth == 24 || depth == 32;
        assert stride == width * (depth/8);

        // 24 bit pixels plus one byte per line for "filter"
        byte[] out24 = new byte[width * height * 3 + height];
        int y;

        int inOff = 0;
        int outOff = 0;
        for (y = 0; y < height; y++) {
            out24[outOff++] = 0;           // filter flag

            if (depth == 24) {
                System.arraycopy(in, inOff, out24, outOff, width * 3);
                outOff += width * 3;
            } else if (depth == 32) {
                int tmpOff = inOff;
                for (int x = 0; x < width; x++) {
                    tmpOff++;       // ignore alpha
                    out24[outOff++] = in[tmpOff++];
                    out24[outOff++] = in[tmpOff++];
                    out24[outOff++] = in[tmpOff++];
                }
            }

            inOff += stride;
        }

        assert outOff == out24.length;

        return out24;
    }

    private static byte[] convert16to24(ImageData imageData) {
        int width = imageData.width;
        int height = imageData.height;

        int redShift = imageData.palette.redShift;
        int greenShift = imageData.palette.greenShift;
        int blueShift = imageData.palette.blueShift;

        int redMask = imageData.palette.redMask;
        int greenMask = imageData.palette.greenMask;
        int blueMask = imageData.palette.blueMask;

        // 24 bit pixels plus one byte per line for "filter"
        byte[] out24 = new byte[width * height * 3 + height];
        int outOff = 0;


        int[] line = new int[width];
        for (int y = 0; y < height; y++) {
            imageData.getPixels(0, y, width, line, 0);

            out24[outOff++] = 0; // filter flag
            for (int x = 0; x < width; x++) {
                int pixelValue = line[x];
                out24[outOff++] = byteChannelValue(pixelValue, redMask, redShift);
                out24[outOff++] = byteChannelValue(pixelValue, greenMask, greenShift);
                out24[outOff++] = byteChannelValue(pixelValue, blueMask, blueShift);
            }
        }

        return out24;
    }

    private static byte byteChannelValue(int value, int mask, int shift) {
        int bValue = value & mask;
        if (shift < 0) {
            bValue = bValue >>> -shift;
        } else {
            bValue = bValue << shift;
        }

        return (byte)bValue;

    }

}
