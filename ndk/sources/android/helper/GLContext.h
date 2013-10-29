/*
 * Copyright 2013 The Android Open Source Project
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

//--------------------------------------------------------------------------------
// glContext.h
//--------------------------------------------------------------------------------

#ifndef GLCONTEXT_H_
#define GLCONTEXT_H_

#include <android/sensor.h>
#include <android/log.h>
#include <android_native_app_glue.h>
#include <android/native_window_jni.h>
#include "JNIHelper.h"

//--------------------------------------------------------------------------------
// Constants
//--------------------------------------------------------------------------------

//--------------------------------------------------------------------------------
// Class
//--------------------------------------------------------------------------------

/******************************************************************
 * OpenGL context handler
 * The class handles OpenGL and EGL context based on Android activity life cycle
 * The caller needs to call corresponding methods for each activity life cycle events as it's done in sample codes.
 *
 * Also the class initializes OpenGL ES3 when the compatible driver is installed in the device.
 * getGLVersion() returns 3.0~ when the device supports OpenGLES3.0
 */
class GLContext
{
private:
    //ELG configurations
    ANativeWindow* _window;
    EGLDisplay _display;
    EGLSurface _surface;
    EGLContext _context;
    EGLConfig _config;

    //Screen parameters
    int32_t _iWidth;
    int32_t _iHeight;
    int32_t _iColorSize;
    int32_t _iDepthSize;

    //Flags
    bool _bGLESInitialized;
    bool _bEGLContextInitialized;
    bool _bES3Support;
    float _fGLVersion;

    void initGLES();
    bool _bContextValid;
    void terminate();
    bool initEGLSurface();
    bool initEGLContext();
public:
    static GLContext* getInstance()
    {
        //Singleton
        static GLContext instance;

        return &instance;
    }

    GLContext( GLContext const& );
    void operator=( GLContext const& );

    GLContext();
    virtual ~GLContext();

    bool init( ANativeWindow* window );
    EGLint swap();
    bool invalidate();

    void suspend();
    EGLint resume(ANativeWindow* window);

    int32_t getScreenWidth() { return _iWidth; }
    int32_t getScreenHeight() { return _iHeight; }

    int32_t getBufferColorSize() { return _iColorSize; }
    int32_t getBufferDepthSize() { return _iDepthSize; }
    float getGLVersion() { return _fGLVersion; }
    bool checkExtension( const char* extension );
};

#endif /* GLCONTEXT_H_ */
