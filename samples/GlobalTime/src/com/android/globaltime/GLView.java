/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.globaltime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.KeyEvent;

class Message {

    private String mText;
    private long mExpirationTime;

    public Message(String text, long expirationTime) {
        this.mText = text;
        this.mExpirationTime = expirationTime;
    }

    public String getText() {
        return mText;
    }

    public long getExpirationTime() {
        return mExpirationTime;
    }
}

/**
 * A helper class to simplify writing an Activity that renders using
 * OpenGL ES.
 *
 * <p> A GLView object stores common elements of GL state and allows
 * them to be modified interactively.  This is particularly useful for
 * determining the proper settings of parameters such as the view
 * frustum and light intensities during application development.
 *
 * <p> A GLView is not an actual View; instead, it is meant to be
 * called from within a View to perform event processing on behalf of the
 * actual View.
 *
 * <p> By passing key events to the GLView object from the View,
 * the application can automatically allow certain parameters to
 * be user-controlled from the keyboard.  Key events may be passed as
 * shown below:
 *
 * <pre>
 * GLView mGlView = new GLView();
 *
 * public boolean onKeyDown(int keyCode, KeyEvent event) {
 *     // Hand the key to the GLView object first
 *     if (mGlView.processKey(keyCode)) {
 *         return;
 *     }
 *  
 *     switch (keyCode) {
 *     case KeyEvent.KEY_CODE_X:
 *         // perform app processing
 *         break;
 *     
 *     default:
 *         super.onKeyDown(keyCode, event);
 *         break;
 *     }
 * }
 * </pre>
 *
 * <p> During drawing of a frame, the GLView object should be given the
 * opportunity to manage GL parameters as shown below:
 * 
 * OpenGLContext mGLContext; // initialization not shown
 * int mNumTrianglesDrawn = 0;
 * 
 * protected void onDraw(Canvas canvas) {
 *     int w = getWidth();
 *     int h = getHeight();
 *         
 *     float ratio = (float) w / h;
 *     mGLView.setAspectRatio(ratio);
 *
 *     GL10 gl = (GL10) mGLContext.getGL();
 *     mGLContext.waitNative(canvas, this);
 *     
 *     // Enable a light for the GLView to manipulate
 *     gl.glEnable(GL10.GL_LIGHTING);
 *     gl.glEnable(GL10.GL_LIGHT0);
 *         
 *     // Allow the GLView to set GL parameters
 *     mGLView.setTextureParameters(gl);
 *     mGLView.setProjection(gl);
 *     mGLView.setView(gl);
 *     mGLView.setLights(gl, GL10.GL_LIGHT0);
 *         
 *     // Draw some stuff (not shown)
 *     mNumTrianglesDrawn += <num triangles just drawn>;
 *     
 *     // Wait for GL drawing to complete
 *     mGLContext.waitGL();
 *     
 *     // Inform the GLView of what was drawn, and ask it to display statistics
 *     mGLView.setNumTriangles(mNumTrianglesDrawn);
 *     mGLView.showMessages(canvas);
 *     mGLView.showStatistics(canvas, w);
 * }      
 * </pre>
 *
 * <p> At the end of each frame, following the call to
 * GLContext.waitGL, the showStatistics and showMessages methods
 * will cause additional information to be displayed.
 *
 * <p> To enter the interactive command mode, the 'tab' key must be
 * pressed twice in succession.  A subsequent press of the 'tab' key
 * exits the interactive command mode.  Entering a multi-letter code
 * sets the parameter to be modified. The 'newline' key erases the
 * current code, and the 'del' key deletes the last letter of
 * the code. The parameter value may be modified by pressing the
 * keypad left or up to decrement the value and right or down to
 * increment the value.  The current value will be displayed as an
 * overlay above the GL rendered content.
 * 
 * <p> The supported keyboard commands are as follows:
 *
 * <ul>
 * <li>     h - display a list of commands
 * <li>    fn - near frustum 
 * <li>    ff - far frustum 
 * <li>    tx - translate x
 * <li>    ty - translate y
 * <li>    tz - translate z
 * <li>     z - zoom (frustum size)
 * <li>    la - ambient light (all RGB channels)
 * <li>   lar - ambient light red channel
 * <li>   lag - ambient light green channel
 * <li>   lab - ambient light blue channel
 * <li>    ld - diffuse light (all RGB channels)
 * <li>   ldr - diffuse light red channel
 * <li>   ldg - diffuse light green channel
 * <li>   ldb - diffuse light blue channel
 * <li>    ls - specular light (all RGB channels)
 * <li>   lsr - specular light red channel
 * <li>   lsg - specular light green channel
 * <li>   lsb - specular light blue channel
 * <li>   lma - light model ambient (all RGB channels)
 * <li>  lmar - light model ambient light red channel
 * <li>  lmag - light model ambient green channel
 * <li>  lmab - light model ambient blue channel
 * <li>  tmin - texture min filter
 * <li>  tmag - texture mag filter
 * <li>  tper - texture perspective correction
 * </ul>
 * 
 * {@hide}
 */
public class GLView {

    private static final int DEFAULT_DURATION_MILLIS = 1000;
    private static final int STATE_KEY = KeyEvent.KEYCODE_TAB;
    private static final int HAVE_NONE = 0;
    private static final int HAVE_ONE = 1;
    private static final int HAVE_TWO = 2;

    private static final float MESSAGE_Y_SPACING = 12.0f;

    private int mState = HAVE_NONE;

    private static final int NEAR_FRUSTUM  = 0;
    private static final int FAR_FRUSTUM   = 1;
    private static final int TRANSLATE_X   = 2;
    private static final int TRANSLATE_Y   = 3;
    private static final int TRANSLATE_Z   = 4;
    private static final int ZOOM_EXPONENT = 5;

    private static final int AMBIENT_INTENSITY = 6;
    private static final int AMBIENT_RED = 7;
    private static final int AMBIENT_GREEN = 8;
    private static final int AMBIENT_BLUE = 9;

    private static final int DIFFUSE_INTENSITY = 10;
    private static final int DIFFUSE_RED = 11;
    private static final int DIFFUSE_GREEN = 12;
    private static final int DIFFUSE_BLUE = 13;

    private static final int SPECULAR_INTENSITY = 14;
    private static final int SPECULAR_RED = 15;
    private static final int SPECULAR_GREEN = 16;
    private static final int SPECULAR_BLUE = 17;

    private static final int LIGHT_MODEL_AMBIENT_INTENSITY = 18;
    private static final int LIGHT_MODEL_AMBIENT_RED = 19;
    private static final int LIGHT_MODEL_AMBIENT_GREEN = 20;
    private static final int LIGHT_MODEL_AMBIENT_BLUE = 21;

    private static final int TEXTURE_MIN_FILTER = 22;
    private static final int TEXTURE_MAG_FILTER = 23;
    private static final int TEXTURE_PERSPECTIVE_CORRECTION = 24;

    private static final String[] commands = { 
        "fn",
        "ff",
        "tx",
        "ty",
        "tz",
        "z",
        "la", "lar", "lag", "lab",
        "ld", "ldr", "ldg", "ldb",
        "ls", "lsr", "lsg", "lsb",
        "lma", "lmar", "lmag", "lmab",
        "tmin", "tmag", "tper"
   };

    private static final String[] labels = {
        "Near Frustum",
        "Far Frustum",
        "Translate X",
        "Translate Y",
        "Translate Z",
        "Zoom",
        "Ambient Intensity",
        "Ambient Red",
        "Ambient Green",
        "Ambient Blue",
        "Diffuse Intensity",
        "Diffuse Red",
        "Diffuse Green",
        "Diffuse Blue",
        "Specular Intenstity",
        "Specular Red",
        "Specular Green",
        "Specular Blue",
        "Light Model Ambient Intensity",
        "Light Model Ambient Red",
        "Light Model Ambient Green",
        "Light Model Ambient Blue",
        "Texture Min Filter",
        "Texture Mag Filter",
        "Texture Perspective Correction",
    };

    private static final float[] defaults = {
        5.0f, 100.0f,
        0.0f, 0.0f, -50.0f,
        0,
        0.125f,	1.0f, 1.0f, 1.0f,
        0.125f,	1.0f, 1.0f, 1.0f,
        0.125f,	1.0f, 1.0f, 1.0f,
        0.125f,	1.0f, 1.0f, 1.0f,
        GL10.GL_NEAREST, GL10.GL_NEAREST,
        GL10.GL_FASTEST
    };

    private static final float[] increments = {
        0.01f, 0.5f,
        0.125f, 0.125f, 0.125f,
        1.0f,
        0.03125f, 0.1f, 0.1f, 0.1f,
        0.03125f, 0.1f, 0.1f, 0.1f,
        0.03125f, 0.1f, 0.1f, 0.1f,
        0.03125f, 0.1f, 0.1f, 0.1f,
        0, 0, 0
    };

    private float[] params = new float[commands.length];

    private static final float mZoomScale = 0.109f;
    private static final float mZoomBase  = 1.01f;

    private int             mParam = -1;
    private float           mIncr = 0;

    private Paint           mPaint = new Paint();

    private float           mAspectRatio = 1.0f;

    private float           mZoom;

    // private boolean         mPerspectiveCorrection = false;
    // private int             mTextureMinFilter = GL10.GL_NEAREST;
    // private int             mTextureMagFilter = GL10.GL_NEAREST;

    // Counters for FPS calculation
    private boolean         mDisplayFPS = false;
    private boolean         mDisplayCounts = false;
    private int             mFramesFPS = 10;
    private long[]          mTimes = new long[mFramesFPS];
    private int             mTimesIdx = 0;

    private Map<String,Message> mMessages = new HashMap<String,Message>();

    /**
     * Constructs a new GLView.
     */
    public GLView() {
        mPaint.setColor(0xffffffff);
        reset();
    }

    /**
     * Sets the aspect ratio (width/height) of the screen.
     *
     * @param aspectRatio the screen width divided by the screen height
     */
    public void setAspectRatio(float aspectRatio) {
        this.mAspectRatio = aspectRatio;
    }
    
    /**
     * Sets the overall ambient light intensity.  This intensity will
     * be used to modify the ambient light value for each of the red,
     * green, and blue channels passed to glLightfv(...GL_AMBIENT...).
     * The default value is 0.125f.
     *
     * @param intensity a floating-point value controlling the overall
     * ambient light intensity.
     */
    public void setAmbientIntensity(float intensity) {
        params[AMBIENT_INTENSITY] = intensity;
    }

    /**
     * Sets the light model ambient intensity.  This intensity will be
     * used to modify the ambient light value for each of the red,
     * green, and blue channels passed to
     * glLightModelfv(GL_LIGHT_MODEL_AMBIENT...).  The default value
     * is 0.125f.
     *
     * @param intensity a floating-point value controlling the overall
     * light model ambient intensity.
     */
    public void setLightModelAmbientIntensity(float intensity) {
        params[LIGHT_MODEL_AMBIENT_INTENSITY] = intensity;
    }

    /**
     * Sets the ambient color for the red, green, and blue channels
     * that will be multiplied by the value of setAmbientIntensity and
     * passed to glLightfv(...GL_AMBIENT...).  The default values are
     * {1, 1, 1}.
     *
     * @param ambient an arry of three floats containing ambient
     * red, green, and blue intensity values.
     */
    public void setAmbientColor(float[] ambient) {
        params[AMBIENT_RED]   = ambient[0];
        params[AMBIENT_GREEN] = ambient[1];
        params[AMBIENT_BLUE]  = ambient[2];
    }

    /**
     * Sets the overall diffuse light intensity.  This intensity will
     * be used to modify the diffuse light value for each of the red,
     * green, and blue channels passed to glLightfv(...GL_DIFFUSE...).
     * The default value is 0.125f.
     *
     * @param intensity a floating-point value controlling the overall
     * ambient light intensity.
     */
    public void setDiffuseIntensity(float intensity) {
        params[DIFFUSE_INTENSITY] = intensity;
    }

    /**
     * Sets the diffuse color for the red, green, and blue channels
     * that will be multiplied by the value of setDiffuseIntensity and
     * passed to glLightfv(...GL_DIFFUSE...).  The default values are
     * {1, 1, 1}.
     *
     * @param diffuse an array of three floats containing diffuse
     * red, green, and blue intensity values.
     */
    public void setDiffuseColor(float[] diffuse) {
        params[DIFFUSE_RED]   = diffuse[0];
        params[DIFFUSE_GREEN] = diffuse[1];
        params[DIFFUSE_BLUE]  = diffuse[2];
    }

    /**
     * Sets the overall specular light intensity.  This intensity will
     * be used to modify the diffuse light value for each of the red,
     * green, and blue channels passed to glLightfv(...GL_SPECULAR...).
     * The default value is 0.125f.
     *
     * @param intensity a floating-point value controlling the overall
     * ambient light intensity.
     */
    public void setSpecularIntensity(float intensity) {
        params[SPECULAR_INTENSITY] = intensity;
    }

    /**
     * Sets the specular color for the red, green, and blue channels
     * that will be multiplied by the value of setSpecularIntensity and
     * passed to glLightfv(...GL_SPECULAR...).  The default values are
     * {1, 1, 1}.
     *
     * @param specular an array of three floats containing specular
     * red, green, and blue intensity values.
     */
    public void setSpecularColor(float[] specular) {
        params[SPECULAR_RED]   = specular[0];
        params[SPECULAR_GREEN] = specular[1];
        params[SPECULAR_BLUE]  = specular[2];
    }

    /**
     * Returns the current X translation of the modelview
     * transformation as passed to glTranslatef.  The default value is
     * 0.0f.
     *
     * @return the X modelview translation as a float.
     */
    public float getTranslateX() {
        return params[TRANSLATE_X];
    }

    /**
     * Returns the current Y translation of the modelview
     * transformation as passed to glTranslatef.  The default value is
     * 0.0f.
     *
     * @return the Y modelview translation as a float.
     */
    public float getTranslateY() {
        return params[TRANSLATE_Y];
    }

    /**
     * Returns the current Z translation of the modelview
     * transformation as passed to glTranslatef.  The default value is
     * -50.0f.
     *
     * @return the Z modelview translation as a float.
     */
    public float getTranslateZ() {
        return params[TRANSLATE_Z];
    }

    /**
     * Sets the position of the near frustum clipping plane as passed
     * to glFrustumf.  The default value is 5.0f;
     *
     * @param nearFrustum the near frustum clipping plane distance as
     * a float.
     */
    public void setNearFrustum(float nearFrustum) {
        params[NEAR_FRUSTUM] = nearFrustum;
    }

    /**
     * Sets the position of the far frustum clipping plane as passed
     * to glFrustumf.  The default value is 100.0f;
     *
     * @param farFrustum the far frustum clipping plane distance as a
     * float.
     */
    public void setFarFrustum(float farFrustum) {
        params[FAR_FRUSTUM] = farFrustum;
    }

    private void computeZoom() {
        mZoom = mZoomScale*(float)Math.pow(mZoomBase, -params[ZOOM_EXPONENT]);
    }

    /**
     * Resets all parameters to their default values.
     */
    public void reset() {
        for (int i = 0; i < params.length; i++) {
            params[i] = defaults[i];
        }
        computeZoom();
    }

    private void removeExpiredMessages() {
        long now = System.currentTimeMillis();

        List<String> toBeRemoved = new ArrayList<String>();

        Iterator<String> keyIter = mMessages.keySet().iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            Message msg = mMessages.get(key);
            if (msg.getExpirationTime() < now) {
                toBeRemoved.add(key);
            }
        }

        Iterator<String> tbrIter = toBeRemoved.iterator();
        while (tbrIter.hasNext()) {
            String key = tbrIter.next();
            mMessages.remove(key);
        }
    }
    
    /**
     * Displays the message overlay on the given Canvas.  The
     * GLContext.waitGL method should be called prior to calling this
     * method.  The interactive command display is drawn by this
     * method.
     *
     * @param canvas the Canvas on which messages are to appear.
     */
    public void showMessages(Canvas canvas) {
        removeExpiredMessages();

        float y = 10.0f;

        List<String> l = new ArrayList<String>();
        l.addAll(mMessages.keySet());
        Collections.sort(l);

        Iterator<String> iter = l.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            String text = mMessages.get(key).getText();
            canvas.drawText(text, 10.0f, y, mPaint);
            y += MESSAGE_Y_SPACING;
        }
    }

    private int mTriangles;

    /**
     * Sets the number of triangles drawn in the previous frame for
     * display by the showStatistics method.  The number of triangles
     * is not computed by GLView but must be supplied by the
     * calling Activity.
     *
     * @param triangles an Activity-supplied estimate of the number of 
     * triangles drawn in the previous frame.
     */
    public void setNumTriangles(int triangles) {
        this.mTriangles = triangles;
    }

    /**
     * Displays statistics on frames and triangles per second. The
     * GLContext.waitGL method should be called prior to calling this
     * method.
     *
     * @param canvas the Canvas on which statistics are to appear.
     * @param width the width of the Canvas.
     */
    public void showStatistics(Canvas canvas, int width) {	
        long endTime = mTimes[mTimesIdx] = System.currentTimeMillis();
        mTimesIdx = (mTimesIdx + 1) % mFramesFPS;

        float th = mPaint.getTextSize();

        if (mDisplayFPS) {
            // Use end time from mFramesFPS frames ago
            long startTime = mTimes[mTimesIdx];
            String fps = "" + (1000.0f*mFramesFPS/(endTime - startTime));

            // Normalize fps to XX.XX format
            if (fps.indexOf(".") == 1) {
                fps = " " + fps;
            }
            int len = fps.length();
            if (len == 2) {
                fps += ".00";
            } else if (len == 4) {
                fps += "0";
            } else if (len > 5) {
                fps = fps.substring(0, 5);
            }

            canvas.drawText(fps + " fps", width - 60.0f, 10.0f, mPaint);
        }

        if (mDisplayCounts) {
            canvas.drawText(mTriangles + " triangles",
                            width - 100.0f, 10.0f + th + 5, mPaint);
        }
    }

    private void addMessage(String key, String text, int durationMillis) {
        long expirationTime = System.currentTimeMillis() + durationMillis;

        mMessages.put(key, new Message(text, expirationTime));
    }

    private void addMessage(String key, String text) {
        addMessage(key, text, DEFAULT_DURATION_MILLIS);
    }

    private void addMessage(String text) {
        addMessage(text, text, DEFAULT_DURATION_MILLIS);
    }

    private void clearMessages() {
        mMessages.clear();
    }

    String command = "";

    private void toggleFilter() {
        if (params[mParam] == GL10.GL_NEAREST) {
            params[mParam] = GL10.GL_LINEAR;
        } else {
            params[mParam] = GL10.GL_NEAREST;
        }
        addMessage(commands[mParam],
                   "Texture " + 
                   (mParam == TEXTURE_MIN_FILTER ? "min" : "mag") +
                   " filter = " +
                   (params[mParam] == GL10.GL_NEAREST ?
                    "nearest" : "linear"));
    }

    private void togglePerspectiveCorrection() {
        if (params[mParam] == GL10.GL_NICEST) {
            params[mParam] = GL10.GL_FASTEST;
        } else {
            params[mParam] = GL10.GL_NICEST;
        }
        addMessage(commands[mParam],
                   "Texture perspective correction = " +
                   (params[mParam] == GL10.GL_FASTEST ?
                    "fastest" : "nicest"));
    }

    private String valueString() {
        if (mParam == TEXTURE_MIN_FILTER ||
            mParam == TEXTURE_MAG_FILTER) {
            if (params[mParam] == GL10.GL_NEAREST) {
                return "nearest";
            }
            if (params[mParam] == GL10.GL_LINEAR) {
                return "linear";
            }
        }
        if (mParam == TEXTURE_PERSPECTIVE_CORRECTION) {
            if (params[mParam] == GL10.GL_FASTEST) {
                return "fastest";
            }
            if (params[mParam] == GL10.GL_NICEST) {
                return "nicest";
            }
        }
        return "" + params[mParam];
    }

    /**
     * 
     * @return true if the view 
     */
    public boolean hasMessages() {
        return mState == HAVE_TWO || mDisplayFPS || mDisplayCounts;
    }
    
    /**
     * Process a key stroke.  The calling Activity should pass all
     * keys from its onKeyDown method to this method.  If the key is
     * part of a GLView command, true is returned and the calling
     * Activity should ignore the key event.  Otherwise, false is
     * returned and the calling Activity may process the key event
     * normally.
     *
     * @param keyCode the key code as passed to Activity.onKeyDown.
     *
     * @return true if the key is part of a GLView command sequence,
     * false otherwise.
     */
    public boolean processKey(int keyCode) {
        // Pressing the state key twice enters the UI
        // Pressing it again exits the UI
        if ((keyCode == STATE_KEY) || 
            (keyCode == KeyEvent.KEYCODE_SLASH) || 
            (keyCode == KeyEvent.KEYCODE_PERIOD))
        {
            mState = (mState + 1) % 3;
            if (mState == HAVE_NONE) {
                clearMessages();
            }
            if (mState == HAVE_TWO) {
                clearMessages();
                addMessage("aaaa", "GL", Integer.MAX_VALUE);
                addMessage("aaab", "", Integer.MAX_VALUE);
                command = "";
            }
            return true;
        } else {
            if (mState == HAVE_ONE) {
                mState = HAVE_NONE;
                return false;
            }
        }

        // If we're not in the UI, exit without handling the key
        if (mState != HAVE_TWO) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            command = "";
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (command.length() > 0) {
                command = command.substring(0, command.length() - 1);
            }

        } else if (keyCode >= KeyEvent.KEYCODE_A &&
                   keyCode <= KeyEvent.KEYCODE_Z) {
            command += "" + (char)(keyCode - KeyEvent.KEYCODE_A + 'a');
        }

        addMessage("aaaa", "GL " + command, Integer.MAX_VALUE);

        if (command.equals("h")) {
            addMessage("aaaa", "GL", Integer.MAX_VALUE);
            addMessage("h - help");
            addMessage("fn/ff - frustum near/far clip Z");
            addMessage("la/lar/lag/lab - abmient intensity/r/g/b");
            addMessage("ld/ldr/ldg/ldb - diffuse intensity/r/g/b");
            addMessage("ls/lsr/lsg/lsb - specular intensity/r/g/b");
            addMessage("s - toggle statistics display");
            addMessage("tmin/tmag - texture min/mag filter");
            addMessage("tpersp - texture perspective correction");
            addMessage("tx/ty/tz - view translate x/y/z");
            addMessage("z - zoom");
            command = "";
            return true;
        } else if (command.equals("s")) {
            mDisplayCounts = !mDisplayCounts;
            mDisplayFPS = !mDisplayFPS;
            command = "";
            return true;
        }

        mParam = -1;
        for (int i = 0; i < commands.length; i++) {
            if (command.equals(commands[i])) {
                mParam = i;
                mIncr = increments[i];
            }
        }
        if (mParam == -1) {
            return true;
        }

        boolean addMessage = true;

        // Increment or decrement
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (mParam == ZOOM_EXPONENT) {
                params[mParam] += mIncr;
                computeZoom();
            } else if ((mParam == TEXTURE_MIN_FILTER) ||
                       (mParam == TEXTURE_MAG_FILTER)) {
                toggleFilter();
            } else if (mParam == TEXTURE_PERSPECTIVE_CORRECTION) {
                togglePerspectiveCorrection();
            } else {
                params[mParam] += mIncr;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                   keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (mParam == ZOOM_EXPONENT) {
                params[mParam] -= mIncr;
                computeZoom();
            } else if ((mParam == TEXTURE_MIN_FILTER) ||
                       (mParam == TEXTURE_MAG_FILTER)) {
                toggleFilter();
            } else if (mParam == TEXTURE_PERSPECTIVE_CORRECTION) {
                togglePerspectiveCorrection();
            } else {
                params[mParam] -= mIncr;
            }
        }
	
        if (addMessage) {
            addMessage(commands[mParam],
                       labels[mParam] + ": " + valueString());
        }

        return true;
    }

    /**
     * Zoom in by a given number of steps.  A negative value of steps
     * zooms out.  Each step zooms in by 1%.
     *
     * @param steps the number of steps to zoom by.
     */
    public void zoom(int steps) {
        params[ZOOM_EXPONENT] += steps;
        computeZoom();
    }
		
    /**
     * Set the projection matrix using glFrustumf.  The left and right
     * clipping planes are set at -+(aspectRatio*zoom), the bottom and
     * top clipping planes are set at -+zoom, and the near and far
     * clipping planes are set to the values set by setNearFrustum and
     * setFarFrustum or interactively.
     *
     * <p> GL side effects:
     * <ul>
     *    <li>overwrites the matrix mode</li>
     *    <li>overwrites the projection matrix</li>
     * </ul>
     *
     * @param gl a GL10 instance whose projection matrix is to be modified.
     */
    public void setProjection(GL10 gl) {
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        if (mAspectRatio >= 1.0f) {
            gl.glFrustumf(-mAspectRatio*mZoom, mAspectRatio*mZoom,
                          -mZoom, mZoom,
                          params[NEAR_FRUSTUM], params[FAR_FRUSTUM]);
        } else {
            gl.glFrustumf(-mZoom, mZoom,
                          -mZoom / mAspectRatio, mZoom / mAspectRatio,
                          params[NEAR_FRUSTUM], params[FAR_FRUSTUM]);
        }
    }

    /**
     * Set the modelview matrix using glLoadIdentity and glTranslatef.
     * The translation values are set interactively.
     *
     * <p> GL side effects:
     * <ul>
     * <li>overwrites the matrix mode</li>
     * <li>overwrites the modelview matrix</li>
     * </ul>
     *
     * @param gl a GL10 instance whose modelview matrix is to be modified.
     */
    public void setView(GL10 gl) {
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Move the viewpoint backwards
        gl.glTranslatef(params[TRANSLATE_X],
                        params[TRANSLATE_Y],
                        params[TRANSLATE_Z]);
    }

    /**
     * Sets texture parameters.
     *
     * <p> GL side effects:
     * <ul>
     * <li>sets the GL_PERSPECTIVE_CORRECTION_HINT</li>
     * <li>sets the GL_TEXTURE_MIN_FILTER texture parameter</li>
     * <li>sets the GL_TEXTURE_MAX_FILTER texture parameter</li>
     * </ul>
     *
     * @param gl a GL10 instance whose texture parameters are to be modified.
     */
    public void setTextureParameters(GL10 gl) {
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                  (int)params[TEXTURE_PERSPECTIVE_CORRECTION]);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                           GL10.GL_TEXTURE_MIN_FILTER,
                           params[TEXTURE_MIN_FILTER]);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                           GL10.GL_TEXTURE_MAG_FILTER,
                           params[TEXTURE_MAG_FILTER]);
    }

    /**
     * Sets the lighting parameters for the given light.
     *
     * <p> GL side effects:
     * <ul>
     * <li>sets the GL_LIGHT_MODEL_AMBIENT intensities
     * <li>sets the GL_AMBIENT intensities for the given light</li>
     * <li>sets the GL_DIFFUSE intensities for the given light</li>
     * <li>sets the GL_SPECULAR intensities for the given light</li>
     * </ul>
     *
     * @param gl a GL10 instance whose texture parameters are to be modified.
     */
    public void setLights(GL10 gl, int lightNum) {
        float[] light = new float[4];
        light[3] = 1.0f;

        float lmi = params[LIGHT_MODEL_AMBIENT_INTENSITY];
        light[0] = params[LIGHT_MODEL_AMBIENT_RED]*lmi;
        light[1] = params[LIGHT_MODEL_AMBIENT_GREEN]*lmi;
        light[2] = params[LIGHT_MODEL_AMBIENT_BLUE]*lmi;
        gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, light, 0);
	
        float ai = params[AMBIENT_INTENSITY];
        light[0] = params[AMBIENT_RED]*ai;
        light[1] = params[AMBIENT_GREEN]*ai;
        light[2] = params[AMBIENT_BLUE]*ai;
        gl.glLightfv(lightNum, GL10.GL_AMBIENT, light, 0);

        float di = params[DIFFUSE_INTENSITY];
        light[0] = params[DIFFUSE_RED]*di;
        light[1] = params[DIFFUSE_GREEN]*di;
        light[2] = params[DIFFUSE_BLUE]*di;
        gl.glLightfv(lightNum, GL10.GL_DIFFUSE, light, 0);

        float si = params[SPECULAR_INTENSITY];
        light[0] = params[SPECULAR_RED]*si;
        light[1] = params[SPECULAR_GREEN]*si;
        light[2] = params[SPECULAR_BLUE]*si;
        gl.glLightfv(lightNum, GL10.GL_SPECULAR, light, 0);
    }
}
