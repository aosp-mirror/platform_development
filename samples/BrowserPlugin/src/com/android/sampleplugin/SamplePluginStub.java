/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sampleplugin;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.ViewGroup.LayoutParams;
import android.webkit.PluginStub;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.VideoView;
import com.android.sampleplugin.graphics.CubeRenderer;

public class SamplePluginStub implements PluginStub {

    static {
        //needed for jni calls
        System.loadLibrary("sampleplugin");
    }
    
    public View getEmbeddedView(final int npp, Context context) {
        
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
        
        if (nativeIsFixedSurface(npp)) {
            //TODO get the fixed dimensions from the plugin 
            //view.getHolder().setFixedSize(width, height);
        }
        
        return view;
    }
    
    public View getFullScreenView(int npp, Context context) {
        
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
    
    private native void nativeSurfaceCreated(int npp, View surfaceView);
    private native void nativeSurfaceChanged(int npp, int format, int width, int height);
    private native void nativeSurfaceDestroyed(int npp);
    private native boolean nativeIsFixedSurface(int npp);
}
