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
import android.widget.MediaController;
import android.widget.VideoView;

public class SamplePlugin implements NativePlugin {

    static {
        //needed for jni calls
        System.loadLibrary("sampleplugin");
    }

    private int npp;
    private Context context;

    public void initializePlugin(int npp, Context context) {
        this.npp = npp;
        this.context = context;
    }

    public SurfaceDrawingModel getEmbeddedSurface() {
        return new EmbeddedSurface();
    }

    public SurfaceDrawingModel getFullScreenSurface() {
        return new FullScreenSurface();
    }

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
                    nativeSurfaceChanged(npp, format, width, height);
                }

                public void surfaceCreated(SurfaceHolder holder) {
                    nativeSurfaceCreated(npp, view);
                }

                public void surfaceDestroyed(SurfaceHolder holder) {
                    nativeSurfaceDestroyed(npp);
                }
            });

            // TODO provide way for native plugin code to reset the size
            if (nativeIsFixedSurface(npp)) {
                int width = nativeGetSurfaceWidth(npp);
                int height = nativeGetSurfaceHeight(npp);
                view.getHolder().setFixedSize(width, height);
            }

            return view;
        }
    }

    private class FullScreenSurface implements SurfaceDrawingModel {

        public View getSurface() {
            /* TODO make this aware of the plugin instance and get the video file
             * from the plugin.
             */

            FrameLayout layout = new FrameLayout(context);
            LayoutParams fp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            layout.setLayoutParams(fp);

            VideoView video = new VideoView(context);
            LayoutParams vp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            layout.setLayoutParams(vp);

            GLSurfaceView gl = new GLSurfaceView(context);
            LayoutParams gp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            layout.setLayoutParams(gp);

            layout.addView(video);
            layout.addView(gl);

            // We want an 8888 pixel format because that's required for a translucent 
            // window. And we want a depth buffer.
            gl.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            // Tell the cube renderer that we want to render a translucent version
            // of the cube:
            gl.setRenderer(new CubeRenderer(true));
            // Use a surface format with an Alpha channel:
            gl.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            gl.setWindowType(WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA_OVERLAY);

            video.setVideoPath("/sdcard/test_video.3gp");
            video.setMediaController(new MediaController(context));
            video.requestFocus();

            return layout;
        }
    }
}
