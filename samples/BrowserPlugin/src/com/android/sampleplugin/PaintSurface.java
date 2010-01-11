/*
 * Copyright 2009, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.sampleplugin;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

public class PaintSurface extends SurfaceView {

    static {
        //needed for jni calls
        System.loadLibrary("sampleplugin");
    }

    private final int npp;

    private boolean validNPP = true;
    private Object nppLock = new Object();

    public PaintSurface(Context context, int NPP, int width, int height) {
        super(context);

        this.npp = NPP;

        this.getHolder().setFormat(PixelFormat.RGBA_8888);
        this.getHolder().addCallback(new Callback() {

            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                synchronized (nppLock) {
                    if (validNPP) {
                        nativeSurfaceChanged(npp, format, width, height);
                    }
                }
            }

            public void surfaceCreated(SurfaceHolder holder) {
                synchronized (nppLock) {
                    if (validNPP) {
                        nativeSurfaceCreated(npp);
                    }
                }
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                synchronized (nppLock) {
                    if (validNPP) {
                        nativeSurfaceDestroyed(npp);
                    }
                }
            }
        });

        // sets the plugin's surface to a fixed size
        this.getHolder().setFixedSize(width, height);

        // ensure that the view system is aware that we will be drawing
        this.setWillNotDraw(false);
    }

    // called by JNI
    private void invalidateNPP() {
        synchronized (nppLock) {
            validNPP = false;
        }
    }

    private native void nativeSurfaceCreated(int npp);
    private native void nativeSurfaceChanged(int npp, int format, int width, int height);
    private native void nativeSurfaceDestroyed(int npp);
}
