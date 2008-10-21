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

package com.android.objviewer;

import android.app.Activity;
import android.content.AssetManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.OpenGLContext;
import android.graphics.Paint;
import android.graphics.glutils.GLView;
import android.graphics.glutils.Object3D;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

class OBJView extends View {

    // Mathematical constants
    private static final float PI = (float)Math.PI;
    private static final float TWO_PI = (float)(2.0*Math.PI);
    private static final float PI_OVER_TWO = (float)(Math.PI/2.0);

    // Ambient light to apply
    // private float[] lightModelAmbient = { 0.0f, 0.0f, 0.0f, 1.0f };
    private float[] lightModelAmbient = { 0.2f, 0.2f, 0.2f, 1.0f };

    // Paint object for drawing the FPS display
    private Paint paint = new Paint();

    // GLView object to manage drawing    
    private GLView glView = new GLView();

    private boolean         initialized = false;

    private OpenGLContext   mGLContext;

    // Next time to draw
    private long            mNextTime;

    // View transformation controlled by UI
    private float           mRotAngle = 0.0f;
    private float           mRotVelocity = 1.0f;
    private float           mTiltAngle = 0.0f;

    // Object bounds
    private float           mCenterX = 0.0f;
    private float           mCenterY = 0.0f;
    private float           mCenterZ = 0.0f;
    private float           mScale   = 1.0f;

    // Light direction
    private float[] mLightDir = { 0.0f, 0.0f, 1.0f, 0.0f };

    public OBJView(Context context) {
        super(context);

        mGLContext = new OpenGLContext(OpenGLContext.DEPTH_BUFFER);

        Message msg = mHandler.obtainMessage(INVALIDATE);
        mNextTime = SystemClock.uptimeMillis() + 100;
        mHandler.sendMessageAtTime(msg, mNextTime);

        requestFocus();
    }

    public void reset() {
        initialized = false;

        mRotAngle = 0.0f;
        mRotVelocity = 1.0f;
        mTiltAngle = 0.0f;

        mCenterX = 0.0f;
        mCenterY = 0.0f;
        mCenterZ = 0.0f;
        mScale   = 1.0f;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Hand the key to the GLView object first
        if (glView.processKey(keyCode)) {
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mRotVelocity -= 1.0f;
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mRotVelocity += 1.0f;
                break;

            case KeyEvent.KEYCODE_DPAD_CENTER:
                mRotVelocity = 0.0f;
                break;

            case KeyEvent.KEYCODE_DPAD_UP:	
                mTiltAngle -= 360.0f/24.0f;
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                mTiltAngle += 360.0f/24.0f;
                break;

            case KeyEvent.KEYCODE_U:
                OBJViewer.nextObject();
                reset();
                break;

            default:
                return super.onKeyDown(keyCode, event);
        }

        return true;
    }

    private void init(GL10 gl) {
        glView.reset();

        paint.setColor(0xffffffff);

        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glDisable(gl.GL_SCISSOR_TEST);

        // Some quality settings...
        gl.glEnable(gl.GL_DITHER);

        gl.glShadeModel(gl.GL_SMOOTH);

        gl.glEnable(gl.GL_CULL_FACE);
        gl.glFrontFace(gl.GL_CCW);

        gl.glClearColor(0, 0, 0, 1);

        gl.glLightModelf(gl.GL_LIGHT_MODEL_TWO_SIDE, 0);
        gl.glLightModelfv(gl.GL_LIGHT_MODEL_AMBIENT, lightModelAmbient, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        GL10 gl = (GL10)mGLContext.getGL();
        mGLContext.makeCurrent(this);

        if (!initialized) {
            init(gl);
            initialized = true;

            // Load the object
            Object3D obj = OBJViewer.getObject();

            // Compute a scale factor and translation to bring it
            // into view
            mCenterX = (obj.getBoundsMinX() + obj.getBoundsMaxX())/2.0f;
            mCenterY = (obj.getBoundsMinY() + obj.getBoundsMaxY())/2.0f;
            mCenterZ = (obj.getBoundsMinZ() + obj.getBoundsMaxZ())/2.0f;
            float spanX = obj.getBoundsMaxX() - obj.getBoundsMinX();
            float spanY = obj.getBoundsMaxY() - obj.getBoundsMinY();
            float spanZ = obj.getBoundsMaxZ() - obj.getBoundsMinZ();
            float maxSpan = Math.max(spanX, spanY);
            maxSpan = Math.max(maxSpan, spanZ);
            mScale = 2.0f/maxSpan;
        }

        int w = getWidth();
        int h = getHeight();
        gl.glViewport(0, 0, w, h);

        float ratio = (float)w/h;
        glView.setAspectRatio(ratio);

        // Clear buffers
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        // Set up the projection and view
        glView.setProjection(gl);
        glView.setView(gl);

        // Set up lighting
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glEnable(gl.GL_LIGHTING);
        gl.glEnable(gl.GL_LIGHT0);
        gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, mLightDir, 0);
        glView.setLights(gl, gl.GL_LIGHT0);

        // Rotate the viewpoint around the model
        gl.glRotatef(mTiltAngle, 1, 0, 0);
        gl.glRotatef(mRotAngle, 0, 1, 0);

        // Scale object to fit in [-1, 1]
        gl.glScalef(mScale, mScale, mScale);

        // Center the object at the origin
        gl.glTranslatef(-mCenterX, -mCenterY, -mCenterZ);

        // Increment the rotation angle
        mRotAngle += mRotVelocity;
        if (mRotAngle < 0.0f) {
            mRotAngle += 360.0f;
        }
        if (mRotAngle > 360.0f) {
            mRotAngle -= 360.0f;
        }

        // Draw the object
        Object3D object = OBJViewer.getObject();
        object.draw(gl);

        // Allow GL to complete
        mGLContext.post();

        // Draw GLView messages and/or FPS
        glView.showMessages(canvas);
        glView.setNumTriangles(object.getNumTriangles());
        glView.showStatistics(canvas, w);
    }

    private static final int INVALIDATE = 1;

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == INVALIDATE) {
                invalidate();
                msg = obtainMessage(INVALIDATE);
                long current = SystemClock.uptimeMillis();
                if (mNextTime < current) {
                    mNextTime = current + 20;
                }
                sendMessageAtTime(msg, mNextTime);
                mNextTime += 20;
            }
        }
    };
}


public class OBJViewer extends Activity {

    private static Object3D object = null;

    private static List<String> objectFiles = new ArrayList<String>();
    private static int objectIndex = 0;

    static {
        objectFiles.add("world.gles");
        objectFiles.add("al.gles");
        objectFiles.add("apple.gles");
        objectFiles.add("dolphins.gles");
        objectFiles.add("f16.gles");
        objectFiles.add("flowers.gles");
        objectFiles.add("porsche.gles");
        objectFiles.add("rosevase.gles");
        objectFiles.add("shuttle.gles");
        objectFiles.add("soccerball.gles");
    }

    private int readInt16(InputStream is) throws Exception {
        return is.read() | (is.read() << 8);
    }

    public static Object3D getObject() {
        return object;
    }

    public static void nextObject() {
        try {
            objectIndex = (objectIndex + 1) % objectFiles.size();
            object.load(objectFiles.get(objectIndex));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Get rid of the title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Make sure we're not drawing a background
        setTheme(R.style.Theme);
        setContentView(new OBJView((Context)getApplication()));

        if (object == null) {
            try {
                final AssetManager am = getAssets();
                this.object = new Object3D() {
                    public InputStream readFile(String filename)
                    throws IOException {
                        return am.open(filename);

                    }
                };
                object.load(objectFiles.get(0));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override protected void onResume() {
        super.onResume();
    }

    @Override protected void onStop() {
        super.onStop();
    }
}
