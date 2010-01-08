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

import com.android.sampleplugin.graphics.CubeRenderer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.ViewGroup.LayoutParams;
import android.webkit.plugin.NativePlugin;
import android.webkit.plugin.SurfaceDrawingModel;
import android.widget.FrameLayout;

public class SamplePlugin implements NativePlugin {

    static {
        //needed for jni calls
        System.loadLibrary("sampleplugin");
    }

    private int npp;
    private Context context;
    
    private SurfaceDrawingModel embeddedSurface;
    private SurfaceDrawingModel fullScreenSurface;
    
    private boolean validNPP = false;
    private Object nppLock = new Object();

    public void initializePlugin(int npp, Context context) {
        this.npp = npp;
        this.context = context;
        this.validNPP = nativeJavaInit(npp);
    }

    public SurfaceDrawingModel getEmbeddedSurface() {
        if (embeddedSurface == null) {
            embeddedSurface = new EmbeddedSurface();
        }
        return embeddedSurface;
    }

    public SurfaceDrawingModel getFullScreenSurface() {
        if (fullScreenSurface == null) {
            fullScreenSurface = new FullScreenSurface();
        }
        return fullScreenSurface;
    }

    // called by JNI
    private void invalidateNPP() {
        synchronized (nppLock) {
            validNPP = false;
        }
    }
    
    private native boolean nativeJavaInit(int npp);
    private native void nativeSurfaceCreated(int npp, View surfaceView);
    private native void nativeSurfaceChanged(int npp, int format, int width, int height);
    private native void nativeSurfaceDestroyed(int npp);
    private native int nativeGetSurfaceWidth(int npp);
    private native int nativeGetSurfaceHeight(int npp);
    private native boolean nativeIsFixedSurface(int npp);

    private class EmbeddedSurface implements SurfaceDrawingModel {

        public View getSurface() {
            final SurfaceView view = new SurfaceView(context);

            /* You can do all sorts of interesting operations on the surface view
             * here. We illustrate a few of the important operations below.
             */

            //TODO get pixel format from the subplugin (via jni)
            view.getHolder().setFormat(PixelFormat.RGBA_8888);
            view.getHolder().addCallback(new Callback() {

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
                            nativeSurfaceCreated(npp, view);
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

            // TODO provide way for native plugin code to reset the size
            if (nativeIsFixedSurface(npp)) {
                int width = nativeGetSurfaceWidth(npp);
                int height = nativeGetSurfaceHeight(npp);
                view.getHolder().setFixedSize(width, height);
            }
            
            // ensure that the view system is aware that we will be drawing
            view.setWillNotDraw(false);
            
            return view;
        }
    }

    private class FullScreenSurface implements SurfaceDrawingModel {

        public View getSurface() {
            /* TODO make this aware of the plugin instance and get the video file
             * from the plugin.
             */

            FrameLayout layout = new FrameLayout(context);
            LayoutParams fp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            layout.setLayoutParams(fp);

//            VideoView video = new VideoView(context);
//            LayoutParams vp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
//            layout.setLayoutParams(vp);

            GLSurfaceView gl = new GLSurfaceView(context);
            LayoutParams gp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            layout.setLayoutParams(gp);

            layout.addView(gl);
//            layout.addView(video);

            // Tell the cube renderer that we want to render a translucent version
            // of the cube:
            gl.setRenderer(new CubeRenderer(false));
            gl.setWindowType(WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA_OVERLAY);

//            video.setVideoPath("/sdcard/test_video.3gp");
//            video.setMediaController(new MediaController(context));
//            video.requestFocus();

            // ensure that the view system is aware that we will be drawing
            layout.setWillNotDraw(false);
            
            return layout;
        }
    }
}
