/*
 ** Copyright 2011, The Android Open Source Project
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

package com.android.glesv2debugger;

import com.google.protobuf.ByteString;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MessageProcessor {
    static void showError(final String message) {
        // need to call SWT from UI thread
        MessageDialog.openError(null, "MessageProcessor", message);
    }

    /**
     * data layout: uint32 total decompressed length, (chunks: uint32 chunk
     * decompressed size, uint32 chunk compressed size, chunk data)+. 0 chunk
     * compressed size means chunk is not compressed
     */
    public static byte[] lzfDecompressChunks(final ByteString data) {
        ByteBuffer in = data.asReadOnlyByteBuffer();
        in.order(SampleView.targetByteOrder);
        ByteBuffer out = ByteBuffer.allocate(in.getInt());
        byte[] inChunk = new byte[0];
        byte[] outChunk = new byte[0];
        while (in.remaining() > 0) {
            int decompressed = in.getInt();
            int compressed = in.getInt();
            if (decompressed > outChunk.length)
                outChunk = new byte[decompressed];
            if (compressed == 0) {
                in.get(outChunk, 0, decompressed);
                out.put(outChunk, 0, decompressed);
            } else {
                if (compressed > inChunk.length)
                    inChunk = new byte[compressed];
                in.get(inChunk, 0, compressed);
                int size = org.liblzf.CLZF
                        .lzf_decompress(inChunk, compressed, outChunk, outChunk.length);
                assert size == decompressed;
                out.put(outChunk, 0, size);
            }
        }
        assert !out.hasRemaining();
        return out.array();
    }

    /** same data layout as LZFDecompressChunks */
    public static byte[] lzfCompressChunks(final byte[] in, final int inSize) {
        byte[] chunk = new byte[256 * 1024]; // chunk size is arbitrary
        final ByteBuffer out = ByteBuffer.allocate(4 + (inSize + chunk.length - 1)
                / chunk.length * (chunk.length + 4 * 2));
        out.order(SampleView.targetByteOrder);
        out.putInt(inSize);
        for (int i = 0; i < inSize; i += chunk.length) {
            int chunkIn = chunk.length;
            if (i + chunkIn > inSize)
                chunkIn = inSize - i;
            final byte[] inChunk = java.util.Arrays.copyOfRange(in, i, i + chunkIn);
            final int chunkOut = org.liblzf.CLZF
                    .lzf_compress(inChunk, chunkIn, chunk, chunk.length);
            out.putInt(chunkIn);
            out.putInt(chunkOut);
            if (chunkOut == 0) // compressed bigger than chunk (uncompressed)
                out.put(inChunk);
            else
                out.put(chunk, 0, chunkOut);
        }
        return Arrays.copyOf(out.array(), out.position());
    }

    /**
     * returns new ref, which is also the decoded image; ref could be bigger
     * than pixels, in which case the first pixels.length bytes form the image
     */
    public static byte[] decodeReferencedImage(byte[] ref, byte[] pixels) {
        if (ref.length < pixels.length)
            ref = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++)
            ref[i] ^= pixels[i];
        for (int i = pixels.length; i < ref.length; i++)
            ref[i] = 0; // clear unused ref to maintain consistency
        return ref;
    }

    public static ImageData receiveImage(int width, int height, int format,
            int type, final ByteString data) {
        assert width > 0 && height > 0;
        int bpp = 0;
        int redMask = 0, blueMask = 0, greenMask = 0;
        switch (GLEnum.valueOf(type)) {
            case GL_UNSIGNED_SHORT_5_6_5:
            case GL_UNSIGNED_SHORT_4_4_4_4:
            case GL_UNSIGNED_SHORT_5_5_5_1:
                format = type;
                break;
            case GL_UNSIGNED_BYTE:
                break;
            default:
                showError("unsupported texture type " + type);
                return null;
        }

        switch (GLEnum.valueOf(format)) {
            case GL_ALPHA:
            case GL_LUMINANCE:
                redMask = blueMask = greenMask = 0xff;
                bpp = 8;
                break;
            case GL_LUMINANCE_ALPHA:
                blueMask = 0xff;
                redMask = 0xff00;
                bpp = 16;
                break;
            case GL_RGB:
                blueMask = 0xff;
                greenMask = 0xff00;
                redMask = 0xff0000;
                bpp = 24;
                break;
            case GL_RGBA:
                blueMask = 0xff00;
                greenMask = 0xff0000;
                redMask = 0xff000000;
                bpp = 32;
                break;
            case GL_UNSIGNED_SHORT_5_6_5:
                blueMask = ((1 << 5) - 1) << 0;
                greenMask = ((1 << 6) - 1) << 5;
                redMask = ((1 << 5) - 1) << 11;
                bpp = 16;
                break;
            case GL_UNSIGNED_SHORT_4_4_4_4:
                blueMask = ((1 << 4) - 1) << 4;
                greenMask = ((1 << 4) - 1) << 8;
                redMask = ((1 << 4) - 1) << 12;
                bpp = 16;
                break;
            case GL_UNSIGNED_SHORT_5_5_5_1:
                blueMask = ((1 << 5) - 1) << 1;
                greenMask = ((1 << 5) - 1) << 6;
                redMask = ((1 << 5) - 1) << 11;
                bpp = 16;
                break;
            default:
                showError("unsupported texture format: " + format);
                return null;
        }
        byte[] pixels = lzfDecompressChunks(data);
        assert pixels.length == width * height * (bpp / 8);
        PaletteData palette = new PaletteData(redMask, greenMask, blueMask);
        return new ImageData(width, height, bpp, palette, 1, pixels);
    }
}
