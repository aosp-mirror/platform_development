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

package com.android.globaltime;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.*;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.opengl.Object3D;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * The main View of the GlobalTime Activity.
 */
class GTView extends SurfaceView implements SurfaceHolder.Callback {

    /**
     * A TimeZone object used to compute the current UTC time.
     */
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("utc");

    /**
     * The Sun's color is close to that of a 5780K blackbody.
     */
    private static final float[] SUNLIGHT_COLOR = {
        1.0f, 0.9375f, 0.91015625f, 1.0f
    };

    /**
     * The inclination of the earth relative to the plane of the ecliptic
     * is 23.45 degrees.
     */
    private static final float EARTH_INCLINATION = 23.45f * Shape.PI / 180.0f;

    /** Seconds in a day */
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    /** Flag for the depth test */
    private static final boolean PERFORM_DEPTH_TEST= false;

    /** Use raw time zone offsets, disregarding "summer time."  If false,
     * current offsets will be used, which requires a much longer startup time
     * in order to sort the city database.
     */
    private static final boolean USE_RAW_OFFSETS = true;

    /**
     * The earth's atmosphere.
     */
    private static final Annulus ATMOSPHERE =
        new Annulus(0.0f, 0.0f, 1.75f, 0.9f, 1.08f, 0.4f, 0.4f, 0.8f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 50);

    /**
     * The tesselation of the earth by latitude.
     */
    private static final int SPHERE_LATITUDES = 25;

    /**
     * The tesselation of the earth by longitude.
     */
    private static int SPHERE_LONGITUDES = 25;

    /**
     * A flattened version of the earth.  The normals are computed identically
     * to those of the round earth, allowing the day/night lighting to be
     * applied to the flattened surface.
     */
    private static Sphere worldFlat = new LatLongSphere(0.0f, 0.0f, 0.0f, 1.0f,
        SPHERE_LATITUDES, SPHERE_LONGITUDES,
        0.0f, 360.0f, true, true, false, true);

    /**
     * The earth.
     */
    private Object3D mWorld;

    /**
     * Geometry of the city lights
     */
    private PointCloud mLights;

    /**
     * True if the activiy has been initialized.
     */
    boolean mInitialized = false;

    /**
     * True if we're in alphabetic entry mode.
     */
    private boolean mAlphaKeySet = false;

    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;
    private EGLDisplay mEGLDisplay;
    private EGLConfig  mEGLConfig;
    GLView  mGLView;

    // Rotation and tilt of the Earth
    private float mRotAngle = 0.0f;
    private float mTiltAngle = 0.0f;

    // Rotational velocity of the orbiting viewer
    private float mRotVelocity = 1.0f;

    // Rotation of the flat view
    private float mWrapX =  0.0f;
    private float  mWrapVelocity =  0.0f;
    private float mWrapVelocityFactor =  0.01f;

    // Toggle switches
    private boolean mDisplayAtmosphere = true;
    private boolean mDisplayClock = false;
    private boolean mClockShowing = false;
    private boolean mDisplayLights = false;
    private boolean mDisplayWorld = true;
    private boolean mDisplayWorldFlat = false;
    private boolean mSmoothShading = true;

    // City search string
    private String mCityName = "";

    // List of all cities
    private List<City> mClockCities;

    // List of cities matching a user-supplied prefix
    private List<City> mCityNameMatches = new ArrayList<City>();

    private List<City> mCities;

    // Start time for clock fade animation
    private long mClockFadeTime;

    // Interpolator for clock fade animation
    private Interpolator mClockSizeInterpolator =
        new DecelerateInterpolator(1.0f);

    // Index of current clock
    private int mCityIndex;

    // Current clock
    private Clock mClock;

    // City-to-city flight animation parameters
    private boolean mFlyToCity = false;
    private long mCityFlyStartTime;
    private float mCityFlightTime;
    private float mRotAngleStart, mRotAngleDest;
    private float mTiltAngleStart, mTiltAngleDest;

    // Interpolator for flight motion animation
    private Interpolator mFlyToCityInterpolator =
        new AccelerateDecelerateInterpolator();

    private static int sNumLights;
    private static int[] sLightCoords;

    //     static Map<Float,int[]> cityCoords = new HashMap<Float,int[]>();

    // Arrays for GL calls
    private float[] mClipPlaneEquation = new float[4];
    private float[] mLightDir = new float[4];

    // Calendar for computing the Sun's position
    Calendar mSunCal = Calendar.getInstance(UTC_TIME_ZONE);

    // Triangles drawn per frame
    private int mNumTriangles;

    private long startTime;

    private static final int MOTION_NONE = 0;
    private static final int MOTION_X = 1;
    private static final int MOTION_Y = 2;

    private static final int MIN_MANHATTAN_DISTANCE = 20;
    private static final float ROTATION_FACTOR = 1.0f / 30.0f;
    private static final float TILT_FACTOR = 0.35f;

    // Touchscreen support
    private float mMotionStartX;
    private float mMotionStartY;
    private float mMotionStartRotVelocity;
    private float mMotionStartTiltAngle;
    private int mMotionDirection;
    
    private boolean mPaused = true;
    private boolean mHaveSurface = false;
    private boolean mStartAnimating = false;
    
    public void surfaceCreated(SurfaceHolder holder) {
        mHaveSurface = true;
        startEGL();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mHaveSurface = false;
        stopEGL();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // nothing to do
    }

    /**
     * Set up the view.
     *
     * @param context the Context
     * @param am an AssetManager to retrieve the city database from
     */
    public GTView(Context context) {
        super(context);

        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);

        startTime = System.currentTimeMillis();

        mClock = new Clock();

        startEGL();
        
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    /**
     * Creates an egl context. If the state of the activity is right, also
     * creates the egl surface. Otherwise the surface will be created in a
     * future call to createEGLSurface().
     */
    private void startEGL() {
        EGL10 egl = (EGL10)EGLContext.getEGL();

        if (mEGLContext == null) {
            EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            egl.eglInitialize(dpy, version);
            int[] configSpec = {
                    EGL10.EGL_DEPTH_SIZE,   16,
                    EGL10.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] num_config = new int[1];
            egl.eglChooseConfig(dpy, configSpec, configs, 1, num_config);
            mEGLConfig = configs[0];

            mEGLContext = egl.eglCreateContext(dpy, mEGLConfig, 
                    EGL10.EGL_NO_CONTEXT, null);
            mEGLDisplay = dpy;
            
            AssetManager am = mContext.getAssets();
            try {
                loadAssets(am);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new RuntimeException(ioe);
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                aioobe.printStackTrace();
                throw new RuntimeException(aioobe);
            }
        }
        
        if (mEGLSurface == null && !mPaused && mHaveSurface) {
            mEGLSurface = egl.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, 
                    this, null);
            egl.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, 
                    mEGLContext);
            mInitialized = false;
            if (mStartAnimating) {
                startAnimating();
                mStartAnimating = false;
            }
        }
    }
    
    /**
     * Destroys the egl context. If an egl surface has been created, it is
     * destroyed as well.
     */
    private void stopEGL() {
        EGL10 egl = (EGL10)EGLContext.getEGL();
        if (mEGLSurface != null) {
            egl.eglMakeCurrent(mEGLDisplay, 
                    egl.EGL_NO_SURFACE, egl.EGL_NO_SURFACE, egl.EGL_NO_CONTEXT);
            egl.eglDestroySurface(mEGLDisplay, mEGLSurface);
            mEGLSurface = null;
        }

        if (mEGLContext != null) {
            egl.eglDestroyContext(mEGLDisplay, mEGLContext);
            egl.eglTerminate(mEGLDisplay);
            mEGLContext = null;
            mEGLDisplay = null;
            mEGLConfig = null;
        }
    }
    
    public void onPause() {
        mPaused = true;
        stopAnimating();
        stopEGL();
    }
    
    public void onResume() {
        mPaused = false;
        startEGL();
    }
    
    public void destroy() {
        stopAnimating();
        stopEGL();
    }

    /**
     * Begin animation.
     */
    public void startAnimating() {
        if (mEGLSurface == null) {
            mStartAnimating = true; // will start when egl surface is created
        } else {
            mHandler.sendEmptyMessage(INVALIDATE);
        }
    }

    /**
     * Quit animation.
     */
    public void stopAnimating() {
        mHandler.removeMessages(INVALIDATE);
    }

    /**
     * Read a two-byte integer from the input stream.
     */
    private int readInt16(InputStream is) throws IOException {
        int lo = is.read();
        int hi = is.read();
        return (hi << 8) | lo;
    }

    /**
     * Returns the offset from UTC for the given city.  If USE_RAW_OFFSETS
     * is true, summer/daylight savings is ignored.
     */
    private static float getOffset(City c) {
        return USE_RAW_OFFSETS ? c.getRawOffset() : c.getOffset();
    }

    private InputStream cache(InputStream is) throws IOException {
        int nbytes = is.available();
        byte[] data = new byte[nbytes];
        int nread = 0;
        while (nread < nbytes) {
            nread += is.read(data, nread, nbytes - nread);
        }
        return new ByteArrayInputStream(data);
    }

    /**
     * Load the city and lights databases.
     *
     * @param am the AssetManager to load from.
     */
    private void loadAssets(final AssetManager am) throws IOException {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        String country = locale.getCountry();

        InputStream cis = null;
        try {
            // Look for (e.g.) cities_fr_FR.dat or cities_fr_CA.dat
            cis = am.open("cities_" + language + "_" + country + ".dat");
        } catch (FileNotFoundException e1) {
            try {
                // Look for (e.g.) cities_fr.dat or cities_fr.dat
                cis = am.open("cities_" + language + ".dat");
            } catch (FileNotFoundException e2) {
                try {
                    // Use English city names by default
                    cis = am.open("cities_en.dat");
                } catch (FileNotFoundException e3) {
                    throw e3;
                }
            }
        }

        cis = cache(cis);
        City.loadCities(cis);
        City[] cities;
        if (USE_RAW_OFFSETS) {
            cities = City.getCitiesByRawOffset();
        } else {
            cities = City.getCitiesByOffset();
        }

        mClockCities = new ArrayList<City>(cities.length);
        for (int i = 0; i < cities.length; i++) {
            mClockCities.add(cities[i]);
        }
        mCities = mClockCities;
        mCityIndex = 0;

        this.mWorld = new Object3D() {
                @Override
                public InputStream readFile(String filename)
                    throws IOException {
                    return cache(am.open(filename));
                }
            };

        mWorld.load("world.gles");

        // lights.dat has the following format.  All integers
        // are 16 bits, low byte first.
        //
        // width
        // height
        // N [# of lights]
        // light 0 X [in the range 0 to (width - 1)]
        // light 0 Y ]in the range 0 to (height - 1)]
        // light 1 X [in the range 0 to (width - 1)]
        // light 1 Y ]in the range 0 to (height - 1)]
        // ...
        // light (N - 1) X [in the range 0 to (width - 1)]
        // light (N - 1) Y ]in the range 0 to (height - 1)]
        //
        // For a larger number of lights, it could make more
        // sense to store the light positions in a bitmap
        // and extract them manually
        InputStream lis = am.open("lights.dat");
        lis = cache(lis);

        int lightWidth = readInt16(lis);
        int lightHeight = readInt16(lis);
        sNumLights = readInt16(lis);
        sLightCoords = new int[3 * sNumLights];

        int lidx = 0;
        float lightRadius = 1.009f;
        float lightScale = 65536.0f * lightRadius;

        float[] cosTheta = new float[lightWidth];
        float[] sinTheta = new float[lightWidth];
        float twoPi = (float) (2.0 * Math.PI);
        float scaleW = twoPi / lightWidth;
        for (int i = 0; i < lightWidth; i++) {
            float theta = twoPi - i * scaleW;
            cosTheta[i] = (float)Math.cos(theta);
            sinTheta[i] = (float)Math.sin(theta);
        }

        float[] cosPhi = new float[lightHeight];
        float[] sinPhi = new float[lightHeight];
        float scaleH = (float) (Math.PI / lightHeight);
        for (int j = 0; j < lightHeight; j++) {
            float phi = j * scaleH;
            cosPhi[j] = (float)Math.cos(phi);
            sinPhi[j] = (float)Math.sin(phi);
        }

        int nbytes = 4 * sNumLights;
        byte[] ilights = new byte[nbytes];
        int nread = 0;
        while (nread < nbytes) {
            nread += lis.read(ilights, nread, nbytes - nread);
        }

        int idx = 0;
        for (int i = 0; i < sNumLights; i++) {
            int lx = (((ilights[idx + 1] & 0xff) << 8) |
                       (ilights[idx    ] & 0xff));
            int ly = (((ilights[idx + 3] & 0xff) << 8) |
                       (ilights[idx + 2] & 0xff));
            idx += 4;

            float sin = sinPhi[ly];
            float x = cosTheta[lx]*sin;
            float y = cosPhi[ly];
            float z = sinTheta[lx]*sin;

            sLightCoords[lidx++] = (int) (x * lightScale);
            sLightCoords[lidx++] = (int) (y * lightScale);
            sLightCoords[lidx++] = (int) (z * lightScale);
        }
        mLights = new PointCloud(sLightCoords);
    }

    /**
     * Returns true if two time zone offsets are equal.  We assume distinct
     * time zone offsets will differ by at least a few minutes.
     */
    private boolean tzEqual(float o1, float o2) {
        return Math.abs(o1 - o2) < 0.001;
    }

    /**
     * Move to a different time zone.
     *
     * @param incr The increment between the current and future time zones.
     */
    private void shiftTimeZone(int incr) {
        // If only 1 city in the current set, there's nowhere to go
        if (mCities.size() <= 1) {
            return;
        }

        float offset = getOffset(mCities.get(mCityIndex));
        do {
            mCityIndex = (mCityIndex + mCities.size() + incr) % mCities.size();
        } while (tzEqual(getOffset(mCities.get(mCityIndex)), offset));

        offset = getOffset(mCities.get(mCityIndex));
        locateCity(true, offset);
        goToCity();
    }

    /**
     * Returns true if there is another city within the current time zone
     * that is the given increment away from the current city.
     *
     * @param incr the increment, +1 or -1
     * @return
     */
    private boolean atEndOfTimeZone(int incr) {
        if (mCities.size() <= 1) {
            return true;
        }

        float offset = getOffset(mCities.get(mCityIndex));
        int nindex = (mCityIndex + mCities.size() + incr) % mCities.size();
        if (tzEqual(getOffset(mCities.get(nindex)), offset)) {
            return false;
        }
        return true;
    }

    /**
     * Shifts cities within the current time zone.
     *
     * @param incr the increment, +1 or -1
     */
    private void shiftWithinTimeZone(int incr) {
        float offset = getOffset(mCities.get(mCityIndex));
        int nindex = (mCityIndex + mCities.size() + incr) % mCities.size();
        if (tzEqual(getOffset(mCities.get(nindex)), offset)) {
            mCityIndex = nindex;
            goToCity();
        }
    }

    /**
     * Returns true if the city name matches the given prefix, ignoring spaces.
     */
    private boolean nameMatches(City city, String prefix) {
        String cityName = city.getName().replaceAll("[ ]", "");
        return prefix.regionMatches(true, 0,
                                    cityName, 0,
                                    prefix.length());
    }

    /**
     * Returns true if there are cities matching the given name prefix.
     */
    private boolean hasMatches(String prefix) {
        for (int i = 0; i < mClockCities.size(); i++) {
            City city = mClockCities.get(i);
            if (nameMatches(city, prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Shifts to the nearest city that matches the new prefix.
     */
    private void shiftByName() {
        // Attempt to keep current city if it matches
        City finalCity = null;
        City currCity = mCities.get(mCityIndex);
        if (nameMatches(currCity, mCityName)) {
            finalCity = currCity;
        }

        mCityNameMatches.clear();
        for (int i = 0; i < mClockCities.size(); i++) {
            City city = mClockCities.get(i);
            if (nameMatches(city, mCityName)) {
                mCityNameMatches.add(city);
            }
        }

        mCities = mCityNameMatches;

        if (finalCity != null) {
            for (int i = 0; i < mCityNameMatches.size(); i++) {
                if (mCityNameMatches.get(i) == finalCity) {
                    mCityIndex = i;
                    break;
                }
            }
        } else {
            // Find the closest matching city
            locateCity(false, 0.0f);
        }
        goToCity();
    }

    /**
     * Increases or decreases the rotational speed of the earth.
     */
    private void incrementRotationalVelocity(float incr) {
        if (mDisplayWorldFlat) {
            mWrapVelocity -= incr;
        } else {
            mRotVelocity -= incr;
        }
    }

    /**
     * Clears the current matching prefix, while keeping the focus on
     * the current city.
     */
    private void clearCityMatches() {
        // Determine the global city index that matches the current city
        if (mCityNameMatches.size() > 0) {
            City city = mCityNameMatches.get(mCityIndex);
            for (int i = 0; i < mClockCities.size(); i++) {
                City ncity = mClockCities.get(i);
                if (city.equals(ncity)) {
                    mCityIndex = i;
                    break;
                }
            }
        }

        mCityName = "";
        mCityNameMatches.clear();
        mCities = mClockCities;
        goToCity();
    }

    /**
     * Fade the clock in or out.
     */
    private void enableClock(boolean enabled) {
        mClockFadeTime = System.currentTimeMillis();
        mDisplayClock = enabled;
        mClockShowing = true;
        mAlphaKeySet = enabled;
        if (enabled) {
            // Find the closest matching city
            locateCity(false, 0.0f);
        }
        clearCityMatches();
    }

    /**
     * Use the touchscreen to alter the rotational velocity or the
     * tilt of the earth.
     */
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mMotionStartX = event.getX();
                mMotionStartY = event.getY();
                mMotionStartRotVelocity = mDisplayWorldFlat ?
                    mWrapVelocity : mRotVelocity;
                mMotionStartTiltAngle = mTiltAngle;

                // Stop the rotation
                if (mDisplayWorldFlat) {
                    mWrapVelocity = 0.0f;
                } else {
                    mRotVelocity = 0.0f;
                }
                mMotionDirection = MOTION_NONE;
                break;

            case MotionEvent.ACTION_MOVE:
                // Disregard motion events when the clock is displayed
                float dx = event.getX() - mMotionStartX;
                float dy = event.getY() - mMotionStartY;
                float delx = Math.abs(dx);
                float dely = Math.abs(dy);

                // Determine the direction of motion (major axis)
                // Once if has been determined, it's locked in until
                // we receive ACTION_UP or ACTION_CANCEL
                if ((mMotionDirection == MOTION_NONE) &&
                    (delx + dely > MIN_MANHATTAN_DISTANCE)) {
                    if (delx > dely) {
                        mMotionDirection = MOTION_X;
                    } else {
                        mMotionDirection = MOTION_Y;
                    }
                }

                // If the clock is displayed, don't actually rotate or tilt;
                // just use mMotionDirection to record whether motion occurred
                if (!mDisplayClock) {
                    if (mMotionDirection == MOTION_X) {
                        if (mDisplayWorldFlat) {
                            mWrapVelocity = mMotionStartRotVelocity +
                                dx * ROTATION_FACTOR;
                        } else {
                            mRotVelocity = mMotionStartRotVelocity +
                                dx * ROTATION_FACTOR;
                        }
                        mClock.setCity(null);
                    } else if (mMotionDirection == MOTION_Y &&
                        !mDisplayWorldFlat) {
                        mTiltAngle = mMotionStartTiltAngle + dy * TILT_FACTOR;
                        if (mTiltAngle < -90.0f) {
                            mTiltAngle = -90.0f;
                        }
                        if (mTiltAngle > 90.0f) {
                            mTiltAngle = 90.0f;
                        }
                        mClock.setCity(null);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mMotionDirection = MOTION_NONE;
                break;

            case MotionEvent.ACTION_CANCEL:
                mTiltAngle = mMotionStartTiltAngle;
                if (mDisplayWorldFlat) {
                    mWrapVelocity = mMotionStartRotVelocity;
                } else {
                    mRotVelocity = mMotionStartRotVelocity;
                }
                mMotionDirection = MOTION_NONE;
                break;
        }
        return true;
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mInitialized && mGLView.processKey(keyCode)) {
            boolean drawing = (mClockShowing || mGLView.hasMessages());
            this.setWillNotDraw(!drawing);
            return true;
        }

        boolean handled = false;

        // If we're not in alphabetical entry mode, convert letters
        // to their digit equivalents
        if (!mAlphaKeySet) {
            char numChar = event.getNumber();
            if (numChar >= '0' && numChar <= '9') {
                keyCode = KeyEvent.KEYCODE_0 + (numChar - '0');
            }
        }

        switch (keyCode) {
        // The 'space' key toggles the clock
        case KeyEvent.KEYCODE_SPACE:
            mAlphaKeySet = !mAlphaKeySet;
            enableClock(mAlphaKeySet);
            handled = true;
            break;

        // The 'left' and 'right' buttons shift time zones if the clock is
        // displayed, otherwise they alters the rotational speed of the earthh
        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (mDisplayClock) {
                shiftTimeZone(-1);
            } else {
                mClock.setCity(null);
                incrementRotationalVelocity(1.0f);
            }
            handled = true;
            break;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (mDisplayClock) {
                shiftTimeZone(1);
            } else {
                mClock.setCity(null);
                incrementRotationalVelocity(-1.0f);
            }
            handled = true;
            break;

        // The 'up' and 'down' buttons shift cities within a time zone if the
        // clock is displayed, otherwise they tilt the earth
        case KeyEvent.KEYCODE_DPAD_UP:
            if (mDisplayClock) {
                shiftWithinTimeZone(-1);
            } else {
                mClock.setCity(null);
                if (!mDisplayWorldFlat) {
                    mTiltAngle += 360.0f / 48.0f;
                }
            }
            handled = true;
            break;

        case KeyEvent.KEYCODE_DPAD_DOWN:
            if (mDisplayClock) {
                shiftWithinTimeZone(1);
            } else {
                mClock.setCity(null);
                if (!mDisplayWorldFlat) {
                    mTiltAngle -= 360.0f / 48.0f;
                }
            }
            handled = true;
            break;

        // The center key stops the earth's rotation, then toggles between the
        // round and flat views of the earth
        case KeyEvent.KEYCODE_DPAD_CENTER:
            if ((!mDisplayWorldFlat && mRotVelocity == 0.0f) ||
                (mDisplayWorldFlat && mWrapVelocity == 0.0f)) {
                mDisplayWorldFlat = !mDisplayWorldFlat;
            } else {
                if (mDisplayWorldFlat) {
                    mWrapVelocity = 0.0f;
                } else {
                    mRotVelocity = 0.0f;
                }
            }
            handled = true;
            break;

        // The 'L' key toggles the city lights
        case KeyEvent.KEYCODE_L:
            if (!mAlphaKeySet && !mDisplayWorldFlat) {
                mDisplayLights = !mDisplayLights;
                handled = true;
            }
            break;


        // The 'W' key toggles the earth (just for fun)
        case KeyEvent.KEYCODE_W:
            if (!mAlphaKeySet && !mDisplayWorldFlat) {
                mDisplayWorld = !mDisplayWorld;
                handled = true;
            }
            break;

        // The 'A' key toggles the atmosphere
        case KeyEvent.KEYCODE_A:
            if (!mAlphaKeySet && !mDisplayWorldFlat) {
                mDisplayAtmosphere = !mDisplayAtmosphere;
                handled = true;
            }
            break;

        // The '2' key zooms out
        case KeyEvent.KEYCODE_2:
            if (!mAlphaKeySet && !mDisplayWorldFlat && mInitialized) {
                mGLView.zoom(-2);
                handled = true;
            }
            break;

        // The '8' key zooms in
        case KeyEvent.KEYCODE_8:
            if (!mAlphaKeySet && !mDisplayWorldFlat && mInitialized) {
                mGLView.zoom(2);
                handled = true;
            }
            break;
        }

        // Handle letters in city names
        if (!handled && mAlphaKeySet) {
            switch (keyCode) {
            // Add a letter to the city name prefix
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_B:
            case KeyEvent.KEYCODE_C:
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_E:
            case KeyEvent.KEYCODE_F:
            case KeyEvent.KEYCODE_G:
            case KeyEvent.KEYCODE_H:
            case KeyEvent.KEYCODE_I:
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_K:
            case KeyEvent.KEYCODE_L:
            case KeyEvent.KEYCODE_M:
            case KeyEvent.KEYCODE_N:
            case KeyEvent.KEYCODE_O:
            case KeyEvent.KEYCODE_P:
            case KeyEvent.KEYCODE_Q:
            case KeyEvent.KEYCODE_R:
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_T:
            case KeyEvent.KEYCODE_U:
            case KeyEvent.KEYCODE_V:
            case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_X:
            case KeyEvent.KEYCODE_Y:
            case KeyEvent.KEYCODE_Z:
                char c = (char)(keyCode - KeyEvent.KEYCODE_A + 'A');
                if (hasMatches(mCityName + c)) {
                    mCityName += c;
                    shiftByName();
                }
                handled = true;
                break;

            // Remove a letter from the city name prefix
            case KeyEvent.KEYCODE_DEL:
                if (mCityName.length() > 0) {
                    mCityName = mCityName.substring(0, mCityName.length() - 1);
                    shiftByName();
                } else {
                    clearCityMatches();
                }
                handled = true;
                break;

            // Clear the city name prefix
            case KeyEvent.KEYCODE_ENTER:
                clearCityMatches();
                handled = true;
                break;
            }
        }

        boolean drawing = (mClockShowing ||
            ((mGLView != null) && (mGLView.hasMessages())));
        this.setWillNotDraw(!drawing);

        // Let the system handle other keypresses
        if (!handled) {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    /**
     * Initialize OpenGL ES drawing.
     */
    private synchronized void init(GL10 gl) {
        mGLView = new GLView();
        mGLView.setNearFrustum(5.0f);
        mGLView.setFarFrustum(50.0f);
        mGLView.setLightModelAmbientIntensity(0.225f);
        mGLView.setAmbientIntensity(0.0f);
        mGLView.setDiffuseIntensity(1.5f);
        mGLView.setDiffuseColor(SUNLIGHT_COLOR);
        mGLView.setSpecularIntensity(0.0f);
        mGLView.setSpecularColor(SUNLIGHT_COLOR);

        if (PERFORM_DEPTH_TEST) {
            gl.glEnable(GL10.GL_DEPTH_TEST);
        }
        gl.glDisable(GL10.GL_SCISSOR_TEST);
        gl.glClearColor(0, 0, 0, 1);
        gl.glHint(GL10.GL_POINT_SMOOTH_HINT, GL10.GL_NICEST);

        mInitialized = true;
    }

    /**
     * Computes the vector from the center of the earth to the sun for a
     * particular moment in time.
     */
    private void computeSunDirection() {
        mSunCal.setTimeInMillis(System.currentTimeMillis());
        int day = mSunCal.get(Calendar.DAY_OF_YEAR);
        int seconds = 3600 * mSunCal.get(Calendar.HOUR_OF_DAY) +
            60 * mSunCal.get(Calendar.MINUTE) + mSunCal.get(Calendar.SECOND);
        day += (float) seconds / SECONDS_PER_DAY;

        // Approximate declination of the sun, changes sinusoidally
        // during the year.  The winter solstice occurs 10 days before
        // the start of the year.
        float decl = (float) (EARTH_INCLINATION *
            Math.cos(Shape.TWO_PI * (day + 10) / 365.0));

        // Subsolar latitude, convert from (-PI/2, PI/2) -> (0, PI) form
        float phi = decl + Shape.PI_OVER_TWO;
        // Subsolar longitude
        float theta = Shape.TWO_PI * seconds / SECONDS_PER_DAY;

        float sinPhi = (float) Math.sin(phi);
        float cosPhi = (float) Math.cos(phi);
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);

        // Convert from polar to rectangular coordinates
        float x = cosTheta * sinPhi;
        float y = cosPhi;
        float z = sinTheta * sinPhi;

        // Directional light -> w == 0
        mLightDir[0] = x;
        mLightDir[1] = y;
        mLightDir[2] = z;
        mLightDir[3] = 0.0f;
    }

    /**
     * Computes the approximate spherical distance between two
     * (latitude, longitude) coordinates.
     */
    private float distance(float lat1, float lon1,
                           float lat2, float lon2) {
        lat1 *= Shape.DEGREES_TO_RADIANS;
        lat2 *= Shape.DEGREES_TO_RADIANS;
        lon1 *= Shape.DEGREES_TO_RADIANS;
        lon2 *= Shape.DEGREES_TO_RADIANS;

        float r = 6371.0f; // Earth's radius in km
        float dlat = lat2 - lat1;
        float dlon = lon2 - lon1;
        double sinlat2 = Math.sin(dlat / 2.0f);
        sinlat2 *= sinlat2;
        double sinlon2 = Math.sin(dlon / 2.0f);
        sinlon2 *= sinlon2;

        double a = sinlat2 + Math.cos(lat1) * Math.cos(lat2) * sinlon2;
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (r * c);
    }

    /**
     * Locates the closest city to the currently displayed center point,
     * optionally restricting the search to cities within a given time zone.
     */
    private void locateCity(boolean useOffset, float offset) {
        float mindist = Float.MAX_VALUE;
        int minidx = -1;
        for (int i = 0; i < mCities.size(); i++) {
            City city = mCities.get(i);
            if (useOffset && !tzEqual(getOffset(city), offset)) {
                continue;
            }
            float dist = distance(city.getLatitude(), city.getLongitude(),
                mTiltAngle, mRotAngle - 90.0f);
            if (dist < mindist) {
                mindist = dist;
                minidx = i;
            }
        }

        mCityIndex = minidx;
    }

    /**
     * Animates the earth to be centered at the current city.
     */
    private void goToCity() {
        City city = mCities.get(mCityIndex);
        float dist = distance(city.getLatitude(), city.getLongitude(),
            mTiltAngle, mRotAngle - 90.0f);

        mFlyToCity = true;
        mCityFlyStartTime = System.currentTimeMillis();
        mCityFlightTime = dist / 5.0f; // 5000 km/sec
        mRotAngleStart = mRotAngle;
        mRotAngleDest = city.getLongitude() + 90;

        if (mRotAngleDest - mRotAngleStart > 180.0f) {
            mRotAngleDest -= 360.0f;
        } else if (mRotAngleStart - mRotAngleDest > 180.0f) {
            mRotAngleDest += 360.0f;
        }

        mTiltAngleStart = mTiltAngle;
        mTiltAngleDest = city.getLatitude();
        mRotVelocity = 0.0f;
    }

    /**
     * Returns a linearly interpolated value between two values.
     */
    private float lerp(float a, float b, float lerp) {
        return a + (b - a)*lerp;
    }

    /**
     * Draws the city lights, using a clip plane to restrict the lights
     * to the night side of the earth.
     */
    private void drawCityLights(GL10 gl, float brightness) {
        gl.glEnable(GL10.GL_POINT_SMOOTH);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_LIGHTING);
        gl.glDisable(GL10.GL_DITHER);
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glPointSize(1.0f);

        float ls = lerp(0.8f, 0.3f, brightness);
        gl.glColor4f(ls * 1.0f, ls * 1.0f, ls * 0.8f, 1.0f);

        if (mDisplayWorld) {
            mClipPlaneEquation[0] = -mLightDir[0];
            mClipPlaneEquation[1] = -mLightDir[1];
            mClipPlaneEquation[2] = -mLightDir[2];
            mClipPlaneEquation[3] = 0.0f;
            // Assume we have glClipPlanef() from OpenGL ES 1.1
            ((GL11) gl).glClipPlanef(GL11.GL_CLIP_PLANE0,
                mClipPlaneEquation, 0);
            gl.glEnable(GL11.GL_CLIP_PLANE0);
        }
        mLights.draw(gl);
        if (mDisplayWorld) {
            gl.glDisable(GL11.GL_CLIP_PLANE0);
        }

        mNumTriangles += mLights.getNumTriangles()*2;
    }

    /**
     * Draws the atmosphere.
     */
    private void drawAtmosphere(GL10 gl) {
        gl.glDisable(GL10.GL_LIGHTING);
        gl.glDisable(GL10.GL_CULL_FACE);
        gl.glDisable(GL10.GL_DITHER);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glShadeModel(mSmoothShading ? GL10.GL_SMOOTH : GL10.GL_FLAT);

        // Draw the atmospheric layer
        float tx = mGLView.getTranslateX();
        float ty = mGLView.getTranslateY();
        float tz = mGLView.getTranslateZ();

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(tx, ty, tz);

        // Blend in the atmosphere a bit
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        ATMOSPHERE.draw(gl);

        mNumTriangles += ATMOSPHERE.getNumTriangles();
    }

    /**
     * Draws the world in a 2D map view.
     */
    private void drawWorldFlat(GL10 gl) {
        gl.glDisable(GL10.GL_BLEND);
        gl.glEnable(GL10.GL_DITHER);
        gl.glShadeModel(mSmoothShading ? GL10.GL_SMOOTH : GL10.GL_FLAT);

        gl.glTranslatef(mWrapX - 2, 0.0f, 0.0f);
        worldFlat.draw(gl);
        gl.glTranslatef(2.0f, 0.0f, 0.0f);
        worldFlat.draw(gl);
        mNumTriangles += worldFlat.getNumTriangles() * 2;

        mWrapX += mWrapVelocity * mWrapVelocityFactor;
        while (mWrapX < 0.0f) {
            mWrapX += 2.0f;
        }
        while (mWrapX > 2.0f) {
            mWrapX -= 2.0f;
        }
    }

    /**
     * Draws the world in a 2D round view.
     */
    private void drawWorldRound(GL10 gl) {
        gl.glDisable(GL10.GL_BLEND);
        gl.glEnable(GL10.GL_DITHER);
        gl.glShadeModel(mSmoothShading ? GL10.GL_SMOOTH : GL10.GL_FLAT);

        mWorld.draw(gl);
        mNumTriangles += mWorld.getNumTriangles();
    }

    /**
     * Draws the clock.
     *
     * @param canvas the Canvas to draw to
     * @param now the current time
     * @param w the width of the screen
     * @param h the height of the screen
     * @param lerp controls the animation, between 0.0 and 1.0
     */
    private void drawClock(Canvas canvas,
                           long now,
                           int w, int h,
                           float lerp) {
        float clockAlpha = lerp(0.0f, 0.8f, lerp);
        mClockShowing = clockAlpha > 0.0f;
        if (clockAlpha > 0.0f) {
            City city = mCities.get(mCityIndex);
            mClock.setCity(city);
            mClock.setTime(now);

            float cx = w / 2.0f;
            float cy = h / 2.0f;
            float smallRadius = 18.0f;
            float bigRadius = 0.75f * 0.5f * Math.min(w, h);
            float radius = lerp(smallRadius, bigRadius, lerp);

            // Only display left/right arrows if we are in a name search
            boolean scrollingByName =
                (mCityName.length() > 0) && (mCities.size() > 1);
            mClock.drawClock(canvas, cx, cy, radius,
                             clockAlpha,
                             1.0f,
                             lerp == 1.0f, lerp == 1.0f,
                             !atEndOfTimeZone(-1),
                             !atEndOfTimeZone(1),
                             scrollingByName,
                             mCityName.length());
        }
    }

    /**
     * Draws the 2D layer.
     */
    @Override protected void onDraw(Canvas canvas) {
        long now = System.currentTimeMillis();
        if (startTime != -1) {
            startTime = -1;
        }

        int w = getWidth();
        int h = getHeight();

        // Interpolator for clock size, clock alpha, night lights intensity
        float lerp = Math.min((now - mClockFadeTime)/1000.0f, 1.0f);
        if (!mDisplayClock) {
            // Clock is receding
            lerp = 1.0f - lerp;
        }
        lerp = mClockSizeInterpolator.getInterpolation(lerp);

        // we don't need to make sure OpenGL rendering is done because
        // we're drawing in to a different surface

        drawClock(canvas, now, w, h, lerp);

        mGLView.showMessages(canvas);
        mGLView.showStatistics(canvas, w);
    }

    /**
     * Draws the 3D layer.
     */
    protected void drawOpenGLScene() {
        long now = System.currentTimeMillis();
        mNumTriangles = 0;

        EGL10 egl = (EGL10)EGLContext.getEGL();
        GL10 gl = (GL10)mEGLContext.getGL();

        if (!mInitialized) {
            init(gl);
        }

        int w = getWidth();
        int h = getHeight();
        gl.glViewport(0, 0, w, h);

        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glFrontFace(GL10.GL_CCW);

        float ratio = (float) w / h;
        mGLView.setAspectRatio(ratio);

        mGLView.setTextureParameters(gl);

        if (PERFORM_DEPTH_TEST) {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        } else {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        }

        if (mDisplayWorldFlat) {
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-1.0f, 1.0f, -1.0f / ratio, 1.0f / ratio, 1.0f, 2.0f);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0.0f, 0.0f, -1.0f);
        } else {
            mGLView.setProjection(gl);
            mGLView.setView(gl);
        }

        if (!mDisplayWorldFlat) {
            if (mFlyToCity) {
                float lerp = (now - mCityFlyStartTime)/mCityFlightTime;
                if (lerp >= 1.0f) {
                    mFlyToCity = false;
                }
                lerp = Math.min(lerp, 1.0f);
                lerp = mFlyToCityInterpolator.getInterpolation(lerp);
                mRotAngle = lerp(mRotAngleStart, mRotAngleDest, lerp);
                mTiltAngle = lerp(mTiltAngleStart, mTiltAngleDest, lerp);
            }

            // Rotate the viewpoint around the earth
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glRotatef(mTiltAngle, 1, 0, 0);
            gl.glRotatef(mRotAngle, 0, 1, 0);

            // Increment the rotation angle
            mRotAngle += mRotVelocity;
            if (mRotAngle < 0.0f) {
                mRotAngle += 360.0f;
            }
            if (mRotAngle > 360.0f) {
                mRotAngle -= 360.0f;
            }
        }

        // Draw the world with lighting
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, mLightDir, 0);
        mGLView.setLights(gl, GL10.GL_LIGHT0);

        if (mDisplayWorldFlat) {
            drawWorldFlat(gl);
        } else if (mDisplayWorld) {
            drawWorldRound(gl);
        }

        if (mDisplayLights && !mDisplayWorldFlat) {
            // Interpolator for clock size, clock alpha, night lights intensity
            float lerp = Math.min((now - mClockFadeTime)/1000.0f, 1.0f);
            if (!mDisplayClock) {
                // Clock is receding
                lerp = 1.0f - lerp;
            }
            lerp = mClockSizeInterpolator.getInterpolation(lerp);
            drawCityLights(gl, lerp);
        }

        if (mDisplayAtmosphere && !mDisplayWorldFlat) {
            drawAtmosphere(gl);
        }
        mGLView.setNumTriangles(mNumTriangles);
        egl.eglSwapBuffers(mEGLDisplay, mEGLSurface);

        if (egl.eglGetError() == EGL11.EGL_CONTEXT_LOST) {
            // we lost the gpu, quit immediately
            Context c = getContext();
            if (c instanceof Activity) {
                ((Activity)c).finish();
            }
        }
    }


    private static final int INVALIDATE = 1;
    private static final int ONE_MINUTE = 60000;

    /**
     * Controls the animation using the message queue.  Every time we receive
     * an INVALIDATE message, we redraw and place another message in the queue.
     */
    private final Handler mHandler = new Handler() {
        private long mLastSunPositionTime = 0;

        @Override public void handleMessage(Message msg) {
            if (msg.what == INVALIDATE) {

                // Use the message's time, it's good enough and
                // allows us to avoid a system call.
                if ((msg.getWhen() - mLastSunPositionTime) >= ONE_MINUTE) {
                    // Recompute the sun's position once per minute
                    // Place the light at the Sun's direction
                    computeSunDirection();
                    mLastSunPositionTime = msg.getWhen();
                }

                // Draw the GL scene
                drawOpenGLScene();

                // Send an update for the 2D overlay if needed
                if (mInitialized &&
                                (mClockShowing || mGLView.hasMessages())) {
                    invalidate();
                }

                // Just send another message immediately. This works because
                // drawOpenGLScene() does the timing for us -- it will
                // block until the last frame has been processed.
                // The invalidate message we're posting here will be
                // interleaved properly with motion/key events which
                // guarantee a prompt reaction to the user input.
                sendEmptyMessage(INVALIDATE);
            }
        }
    };
}

/**
 * The main activity class for GlobalTime.
 */
public class GlobalTime extends Activity {

    GTView gtView = null;

    @Override protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        gtView = new GTView(this);
        setContentView(gtView);
    }

    @Override protected void onResume() {
        super.onResume();
        gtView.onResume();
        Looper.myQueue().addIdleHandler(new Idler());
    }

    @Override protected void onPause() {
        super.onPause();
        gtView.onPause();
    }

    @Override protected void onStop() {
        super.onStop();
        gtView.destroy();
        gtView = null;
    }

    // Allow the activity to go idle before its animation starts
    class Idler implements MessageQueue.IdleHandler {
        public Idler() {
            super();
        }

        public final boolean queueIdle() {
            if (gtView != null) {
                gtView.startAnimating();
            }
            return false;
        }
    }
}
