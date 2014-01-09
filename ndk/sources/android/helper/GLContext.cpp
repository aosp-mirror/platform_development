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
// GLContext.cpp
//--------------------------------------------------------------------------------

//--------------------------------------------------------------------------------
// includes
//--------------------------------------------------------------------------------
#include <unistd.h>
#include "GLContext.h"
#include "gl3stub.h"

//--------------------------------------------------------------------------------
// eGLContext
//--------------------------------------------------------------------------------

//--------------------------------------------------------------------------------
// Ctor
//--------------------------------------------------------------------------------
GLContext::GLContext() : _display(EGL_NO_DISPLAY),
    _surface(EGL_NO_SURFACE),
    _context(EGL_NO_CONTEXT),
    _iWidth( 0 ),
    _iHeight( 0 ),
    _bES3Support( false ),
    _bEGLContextInitialized( false ),
    _bGLESInitialized( false )
{
}

void GLContext::initGLES()
{
    if( _bGLESInitialized )
        return;
    //
    //Initialize OpenGL ES 3 if available
    //
    const char* versionStr = (const char*)glGetString(GL_VERSION);
    if (strstr(versionStr, "OpenGL ES 3.")
            && gl3stubInit())
    {
        _bES3Support = true;
        _fGLVersion = 3.0f;
    }
    else
    {
        _fGLVersion = 2.0f;
    }

    _bGLESInitialized = true;
}

//--------------------------------------------------------------------------------
// Dtor
//--------------------------------------------------------------------------------
GLContext::~GLContext()
{
    terminate();
}

bool GLContext::init( ANativeWindow* window )
{
    if( _bEGLContextInitialized )
        return true;

    //
    //Initialize EGL
    //
    _window = window;
    initEGLSurface();
    initEGLContext();
    initGLES();

    _bEGLContextInitialized = true;

    return true;
}

bool GLContext::initEGLSurface()
{
    _display = eglGetDisplay( EGL_DEFAULT_DISPLAY );
    eglInitialize( _display, 0, 0 );

    /*
     * Here specify the attributes of the desired configuration.
     * Below, we select an EGLConfig with at least 8 bits per color
     * component compatible with on-screen windows
     */
    const EGLint attribs[] = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,    //Request opengl ES2.0
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_DEPTH_SIZE, 24,
            EGL_NONE
    };
    _iColorSize = 8;
    _iDepthSize = 24;

    EGLint numConfigs;
    eglChooseConfig( _display, attribs, &_config, 1, &numConfigs );

    if( !numConfigs )
    {
        //Fall back to 16bit depth buffer
        const EGLint attribs[] = {
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,    //Request opengl ES2.0
                EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
                EGL_BLUE_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_RED_SIZE, 8,
                EGL_DEPTH_SIZE, 16,
                EGL_NONE
        };
        eglChooseConfig( _display, attribs, &_config, 1, &numConfigs );
        _iDepthSize = 16;
    }

    if ( !numConfigs )
    {
        LOGW("Unable to retrieve EGL config");
        return false;
    }

    _surface = eglCreateWindowSurface( _display, _config, _window, NULL );
    eglQuerySurface(_display, _surface, EGL_WIDTH, &_iWidth);
    eglQuerySurface(_display, _surface, EGL_HEIGHT, &_iHeight);

    /* EGL_NATIVE_VISUAL_ID is an attribute of the EGLConfig that is
     * guaranteed to be accepted by ANativeWindow_setBuffersGeometry().
     * As soon as we picked a EGLConfig, we can safely reconfigure the
     * ANativeWindow buffers to match, using EGL_NATIVE_VISUAL_ID. */
    EGLint format;
    eglGetConfigAttrib(_display, _config, EGL_NATIVE_VISUAL_ID, &format);
    ANativeWindow_setBuffersGeometry( _window, 0, 0, format);

    return true;
}

bool GLContext::initEGLContext()
{
    const EGLint contextAttribs[] = {
            EGL_CONTEXT_CLIENT_VERSION, 2,  //Request opengl ES2.0
            EGL_NONE
    };
    _context = eglCreateContext( _display, _config, NULL, contextAttribs );

    if( eglMakeCurrent(_display, _surface, _surface, _context) == EGL_FALSE )
    {
        LOGW("Unable to eglMakeCurrent");
        return false;
    }

    _bContextValid = true;
    return true;
}

EGLint GLContext::swap()
{
    bool b = eglSwapBuffers( _display, _surface);
    if( !b )
    {
        EGLint err = eglGetError();
        if( err == EGL_BAD_SURFACE )
        {
            //Recreate surface
            initEGLSurface();
        }
        else if( err == EGL_CONTEXT_LOST || err == EGL_BAD_CONTEXT )
        {
            //Context has been lost!!
            _bContextValid = false;
            terminate();
            initEGLContext();
        }
        return err;
    }
    return EGL_SUCCESS;
}

void GLContext::terminate()
{
    if( _display != EGL_NO_DISPLAY )
    {
        eglMakeCurrent( _display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT );
        if ( _context != EGL_NO_CONTEXT )
        {
            eglDestroyContext( _display, _context );
        }

        if( _surface != EGL_NO_SURFACE )
        {
            eglDestroySurface( _display, _surface );
        }
        eglTerminate( _display );
    }

    _display = EGL_NO_DISPLAY;
    _context = EGL_NO_CONTEXT;
    _surface = EGL_NO_SURFACE;
    _bContextValid = false;

}

EGLint GLContext::resume(ANativeWindow* window)
{
    if( _bEGLContextInitialized == false )
    {
        init( window );
        return EGL_SUCCESS;
    }

    int32_t iOriginalWidth = _iWidth;
    int32_t iOriginalHeight = _iHeight;

    //Create surface
    _window = window;
    _surface = eglCreateWindowSurface( _display, _config, _window, NULL );
    eglQuerySurface(_display, _surface, EGL_WIDTH, &_iWidth);
    eglQuerySurface(_display, _surface, EGL_HEIGHT, &_iHeight);

    if( _iWidth != iOriginalWidth || _iHeight != iOriginalHeight )
    {
        //Screen resized
        LOGI("Screen resized");
    }

    if( eglMakeCurrent(_display, _surface, _surface, _context) == EGL_TRUE )
        return EGL_SUCCESS;

    EGLint err = eglGetError();
    LOGW("Unable to eglMakeCurrent %d", err);

    if( err == EGL_CONTEXT_LOST )
    {
        //Recreate context
        LOGI("Re-creating egl context");
        initEGLContext();
    }
    else
    {
        //Recreate surface
        terminate();
        initEGLSurface();
        initEGLContext();
    }

    return err;

}

void GLContext::suspend()
{
    if( _surface != EGL_NO_SURFACE )
    {
        eglDestroySurface( _display, _surface );
        _surface = EGL_NO_SURFACE;
    }
}

bool GLContext::invalidate()
{
    terminate();

    _bEGLContextInitialized = false;
    return true;
}


bool GLContext::checkExtension( const char* extension )
{
    if( extension == NULL )
        return false;

    std::string extensions = std::string( (char*)glGetString(GL_EXTENSIONS) );
    std::string str = std::string( extension );
    str.append( " " );

    size_t pos = 0;
    if( extensions.find( extension, pos ) != std::string::npos )
    {
        return true;
    }

    return false;
}
