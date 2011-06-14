/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include "HostConnection.h"
#include "ThreadInfo.h"
#include "eglDisplay.h"
#include "egl_ftable.h"
#include <cutils/log.h>

#include <private/ui/android_natives_priv.h>

template<typename T>
static T setError(GLint error, T returnValue) {
    getEGLThreadInfo()->eglError = error;
    return returnValue;
}

#define RETURN_ERROR(ret,err)           \
    getEGLThreadInfo()->eglError = err; \
    return ret;

#define VALIDATE_CONFIG(cfg,ret) \
    if(((int)cfg<0)||((int)cfg>s_display.getNumConfigs())) { \
        RETURN_ERROR(ret,EGL_BAD_CONFIG); \
    }

#define VALIDATE_DISPLAY(dpy,ret) \
    if ((dpy) != (EGLDisplay)&s_display) { \
        getEGLThreadInfo()->eglError = EGL_BAD_DISPLAY; \
        return ret; \
    }

#define VALIDATE_DISPLAY_INIT(dpy,ret) \
    VALIDATE_DISPLAY(dpy, ret) \
    if (!s_display.initialized()) { \
        getEGLThreadInfo()->eglError = EGL_NOT_INITIALIZED; \
        return ret; \
    }

#define DEFINE_HOST_CONNECTION \
    HostConnection *hostCon = HostConnection::get(); \
    renderControl_encoder_context_t *rcEnc = (hostCon ? hostCon->rcEncoder() : NULL)

#define DEFINE_AND_VALIDATE_HOST_CONNECTION(ret) \
    HostConnection *hostCon = HostConnection::get(); \
    if (!hostCon) { \
        LOGE("egl: Failed to get host connection\n"); \
        return ret; \
    } \
    renderControl_encoder_context_t *rcEnc = hostCon->rcEncoder(); \
    if (!rcEnc) { \
        LOGE("egl: Failed to get renderControl encoder context\n"); \
        return ret; \
    }


// ----------------------------------------------------------------------------
//egl_surface_t

//we don't need to handle depth since it's handled when window created on the host

struct egl_surface_t {

    EGLDisplay          dpy;
    EGLConfig           config;
    EGLContext          ctx;

    egl_surface_t(EGLDisplay dpy, EGLConfig config);
    virtual     ~egl_surface_t();
    virtual     EGLBoolean         createRc() = 0;
    virtual     EGLBoolean         destroyRc() = 0;
    void         setRcSurface(uint32_t handle){ rcSurface = handle; }
    uint32_t     getRcSurface(){ return rcSurface; }

    virtual     EGLBoolean    isValid(){ return valid; }
    virtual     EGLint      getWidth() const = 0;
    virtual     EGLint      getHeight() const = 0;

protected:
    EGLBoolean            valid;
    uint32_t             rcSurface; //handle to surface created via remote control
};

egl_surface_t::egl_surface_t(EGLDisplay dpy, EGLConfig config)
    : dpy(dpy), config(config), ctx(0), valid(EGL_FALSE), rcSurface(0)
{
}

egl_surface_t::~egl_surface_t()
{
}

// ----------------------------------------------------------------------------
// egl_window_surface_t

struct egl_window_surface_t : public egl_surface_t {

    ANativeWindow*     nativeWindow;
    int width;
    int height;

    virtual     EGLint      getWidth() const    { return width;  }
    virtual     EGLint      getHeight() const   { return height; }

    egl_window_surface_t(
            EGLDisplay dpy, EGLConfig config,
            ANativeWindow* window);

    ~egl_window_surface_t();
    virtual     EGLBoolean     createRc();
    virtual     EGLBoolean     destroyRc();

};


egl_window_surface_t::egl_window_surface_t (
            EGLDisplay dpy, EGLConfig config,
            ANativeWindow* window)
    : egl_surface_t(dpy, config),
    nativeWindow(window)
{
    // keep a reference on the window
    nativeWindow->common.incRef(&nativeWindow->common);
    nativeWindow->query(nativeWindow, NATIVE_WINDOW_WIDTH, &width);
    nativeWindow->query(nativeWindow, NATIVE_WINDOW_HEIGHT, &height);

}

egl_window_surface_t::~egl_window_surface_t() {
    nativeWindow->common.decRef(&nativeWindow->common);
}

EGLBoolean egl_window_surface_t::createRc()
{
    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    uint32_t rcSurface = rcEnc->rcCreateWindowSurface(rcEnc, (uint32_t)config, getWidth(), getHeight());
    if (!rcSurface) {
        LOGE("rcCreateWindowSurface returned 0");
        return EGL_FALSE;
    }
    valid = EGL_TRUE;
    return EGL_TRUE;
}

EGLBoolean egl_window_surface_t::destroyRc()
{
    if (!rcSurface) {
        LOGE("destroyRc called on invalid rcSurface");
        return EGL_FALSE;
    }

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    rcEnc->rcDestroyWindowSurface(rcEnc, rcSurface);
    rcSurface = 0;

    return EGL_TRUE;
}

// ----------------------------------------------------------------------------
//egl_pbuffer_surface_t

struct egl_pbuffer_surface_t : public egl_surface_t {

    int width;
    int height;
    GLenum    format;

    virtual     EGLint      getWidth() const    { return width;  }
    virtual     EGLint      getHeight() const   { return height; }

    egl_pbuffer_surface_t(
            EGLDisplay dpy, EGLConfig config,
            int32_t w, int32_t h, GLenum format);

    virtual ~egl_pbuffer_surface_t();
    virtual     EGLBoolean     createRc();
    virtual     EGLBoolean     destroyRc();

    uint32_t    getRcColorBuffer(){ return rcColorBuffer; }
    void         setRcColorBuffer(uint32_t colorBuffer){ rcColorBuffer = colorBuffer; }
private:
    uint32_t rcColorBuffer;
};

egl_pbuffer_surface_t::egl_pbuffer_surface_t(
        EGLDisplay dpy, EGLConfig config,
        int32_t w, int32_t h, GLenum pixelFormat)
    : egl_surface_t(dpy, config),
    width(w), height(h), format(pixelFormat)
{
}

egl_pbuffer_surface_t::~egl_pbuffer_surface_t()
{
    rcColorBuffer = 0;
}

EGLBoolean egl_pbuffer_surface_t::createRc()
{
    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    rcSurface = rcEnc->rcCreateWindowSurface(rcEnc, (uint32_t)config, getWidth(), getHeight());
    if (!rcSurface) {
        LOGE("rcCreateWindowSurface returned 0");
        return EGL_FALSE;
    }
    rcColorBuffer = rcEnc->rcCreateColorBuffer(rcEnc, getWidth(), getHeight(), format);
    if (!rcColorBuffer) {
        LOGE("rcCreateColorBuffer returned 0");
        return EGL_FALSE;
    }

    valid = EGL_TRUE;
    return EGL_TRUE;
}

EGLBoolean egl_pbuffer_surface_t::destroyRc()
{
    if ((!rcSurface)||(!rcColorBuffer)) {
        LOGE("destroyRc called on invalid rcSurface");
        return EGL_FALSE;
    }

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    rcEnc->rcDestroyWindowSurface(rcEnc, rcSurface);
    rcEnc->rcDestroyColorBuffer(rcEnc, rcColorBuffer);
    rcSurface = 0;

    return EGL_TRUE;
}


// ----------------------------------------------------------------------------

// The one and only supported display object.
static eglDisplay s_display;

static EGLClient_eglInterface s_eglIface = {
    getThreadInfo: getEGLThreadInfo
};
EGLDisplay eglGetDisplay(EGLNativeDisplayType display_id)
{
    //
    // we support only EGL_DEFAULT_DISPLAY.
    //
    if (display_id != EGL_DEFAULT_DISPLAY) {
        return EGL_NO_DISPLAY;
    }

    return (EGLDisplay)&s_display;
}

EGLBoolean eglInitialize(EGLDisplay dpy, EGLint *major, EGLint *minor)
{
    VALIDATE_DISPLAY(dpy,EGL_FALSE);

    if (!s_display.initialize(&s_eglIface)) {
        return EGL_FALSE;
    }

    *major = s_display.getVersionMajor();
    *minor = s_display.getVersionMinor();
    return EGL_TRUE;
}

EGLBoolean eglTerminate(EGLDisplay dpy)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);

    s_display.terminate();
    return EGL_TRUE;
}

EGLint eglGetError()
{
    return getEGLThreadInfo()->eglError;
}

__eglMustCastToProperFunctionPointerType eglGetProcAddress(const char *procname)
{
    // search in EGL function table
    for (int i=0; i<egl_num_funcs; i++) {
        if (!strcmp(egl_funcs_by_name[i].name, procname)) {
            return (__eglMustCastToProperFunctionPointerType)egl_funcs_by_name[i].proc;
        }
    }

    //
    // Make sure display is initialized before searching in client APIs
    //
    if (!s_display.initialized()) {
        if (!s_display.initialize(&s_eglIface)) {
            return NULL;
        }
    }

    // look in gles
    void *proc = s_display.gles_iface()->getProcAddress( procname );
    if (proc != NULL) {
        return (__eglMustCastToProperFunctionPointerType)proc;
    }

    // look in gles2
    if (s_display.gles2_iface() != NULL) {
        proc = s_display.gles2_iface()->getProcAddress( procname );
        if (proc != NULL) {
            return (__eglMustCastToProperFunctionPointerType)proc;
        }
    }

    // Fail - function not found.
    return NULL;
}

const char* eglQueryString(EGLDisplay dpy, EGLint name)
{
    VALIDATE_DISPLAY_INIT(dpy, NULL);

    return s_display.queryString(name);
}

EGLBoolean eglGetConfigs(EGLDisplay dpy, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
    VALIDATE_DISPLAY_INIT(dpy, NULL);

    if(!num_config) {
        RETURN_ERROR(EGL_FALSE,EGL_BAD_PARAMETER);
    }

    GLint numConfigs = s_display.getNumConfigs();
    if (!configs) {
        *num_config = numConfigs;
        return EGL_TRUE;
    }

    int i=0;
    for (i=0 ; i<numConfigs && i<config_size ; i++) {
        *configs++ = (EGLConfig)i;
    }
    *num_config = i;
    return EGL_TRUE;
}

EGLBoolean eglChooseConfig(EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
    VALIDATE_DISPLAY_INIT(dpy, NULL);

    //TODO
    return 0;
}

EGLBoolean eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value)
{
    VALIDATE_DISPLAY_INIT(dpy, NULL);
    VALIDATE_CONFIG(config, EGL_FALSE);

    if (s_display.getConfigAttrib(config, attribute, value))
    {
        return EGL_TRUE;
    }
    else
    {
        RETURN_ERROR(EGL_FALSE, EGL_BAD_ATTRIBUTE);
    }
}

EGLSurface eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *attrib_list)
{
    VALIDATE_DISPLAY_INIT(dpy, NULL);
    VALIDATE_CONFIG(config, EGL_FALSE);
    if (win == 0)
    {
        return setError(EGL_BAD_MATCH, EGL_NO_SURFACE);
    }

    EGLint surfaceType;
    if (s_display.getConfigAttrib(config, EGL_SURFACE_TYPE, &surfaceType) == EGL_FALSE)    return EGL_FALSE;

    if (!(surfaceType & EGL_WINDOW_BIT)) {
        return setError(EGL_BAD_MATCH, EGL_NO_SURFACE);
    }


    if (static_cast<ANativeWindow*>(win)->common.magic != ANDROID_NATIVE_WINDOW_MAGIC) {
        return setError(EGL_BAD_NATIVE_WINDOW, EGL_NO_SURFACE);
    }

    egl_surface_t* surface;
    surface = new egl_window_surface_t(&s_display, config, static_cast<ANativeWindow*>(win));
    if (!surface) setError(EGL_BAD_ALLOC, EGL_NO_SURFACE);
    if (!surface->createRc()) setError(EGL_BAD_ALLOC, EGL_NO_SURFACE);

    return surface;
}

EGLSurface eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list)
{
    VALIDATE_DISPLAY_INIT(dpy, NULL);
    VALIDATE_CONFIG(config, EGL_FALSE);

    EGLint surfaceType;
    if (s_display.getConfigAttrib(config, EGL_SURFACE_TYPE, &surfaceType) == EGL_FALSE)    return EGL_FALSE;

    if (!(surfaceType & EGL_PBUFFER_BIT)) {
        return setError(EGL_BAD_MATCH, EGL_NO_SURFACE);
    }

    int32_t w = 0;
    int32_t h = 0;
    while (attrib_list[0]) {
        if (attrib_list[0] == EGL_WIDTH)  w = attrib_list[1];
        if (attrib_list[0] == EGL_HEIGHT) h = attrib_list[1];
        attrib_list+=2;
    }

    GLenum pixelFormat;
    if (s_display.getConfigPixelFormat(config, &pixelFormat) == EGL_FALSE)
        return setError(EGL_BAD_MATCH, EGL_NO_SURFACE);

    egl_surface_t* surface = new egl_pbuffer_surface_t(dpy, config, w, h, pixelFormat);
    if (!surface) setError(EGL_BAD_ALLOC, EGL_NO_SURFACE);
    if (!surface->createRc()) setError(EGL_BAD_ALLOC, EGL_NO_SURFACE);

    return surface;
}

EGLSurface eglCreatePixmapSurface(EGLDisplay dpy, EGLConfig config, EGLNativePixmapType pixmap, const EGLint *attrib_list)
{
    //XXX: Pixmap not supported. The host cannot render to a pixmap resource
    //     located on host. In order to support Pixmaps we should either punt
    //     to s/w rendering -or- let the host render to a buffer that will be
    //     copied back to guest at some sync point. None of those methods not
    //     implemented and pixmaps are not used with OpenGL anyway ...
    return EGL_NO_SURFACE;
}

EGLBoolean eglDestroySurface(EGLDisplay dpy, EGLSurface eglSurface)
{
    VALIDATE_DISPLAY_INIT(dpy, NULL);

    if (eglSurface != EGL_NO_SURFACE)
    {
        egl_surface_t* surface( static_cast<egl_surface_t*>(eglSurface) );
        if (!surface->isValid())
            return setError(EGL_BAD_SURFACE, EGL_FALSE);
        if (surface->dpy != dpy)
            return setError(EGL_BAD_DISPLAY, EGL_FALSE);

        surface->destroyRc();
        delete surface;
    }
    return EGL_TRUE;
}

EGLBoolean eglQuerySurface(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint *value)
{
    //TODO
    return 0;
}

EGLBoolean eglBindAPI(EGLenum api)
{
    //TODO
    return 0;
}

EGLenum eglQueryAPI()
{
    //TODO
    return 0;
}

EGLBoolean eglWaitClient()
{   //TODO
    return 0;

}

EGLBoolean eglReleaseThread()
{
    //TODO
    return 0;
}

EGLSurface eglCreatePbufferFromClientBuffer(EGLDisplay dpy, EGLenum buftype, EGLClientBuffer buffer, EGLConfig config, const EGLint *attrib_list)
{
    //TODO
    return 0;
}

EGLBoolean eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint value)
{
    //TODO
    return 0;
}

EGLBoolean eglBindTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    //TODO
    return 0;
}

EGLBoolean eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    //TODO
    return 0;
}

EGLBoolean eglSwapInterval(EGLDisplay dpy, EGLint interval)
{
    //TODO
    return 0;
}

EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig config, EGLContext share_context, const EGLint *attrib_list)
{
    //TODO
    return 0;
}

EGLBoolean eglDestroyContext(EGLDisplay dpy, EGLContext ctx)
{
    //TODO
    return 0;
}

EGLBoolean eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx)
{
    //TODO
    return 0;
}

EGLContext eglGetCurrentContext()
{
    //TODO
    return 0;
}

EGLSurface eglGetCurrentSurface(EGLint readdraw)
{
    //TODO
    return 0;
}

EGLDisplay eglGetCurrentDisplay()
{
    //TODO
    return 0;
}

EGLBoolean eglQueryContext(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value)
{
    //TODO
    return 0;
}

EGLBoolean eglWaitGL()
{
    //TODO
    return 0;
}

EGLBoolean eglWaitNative(EGLint engine)
{
    //TODO
    return 0;
}

EGLBoolean eglSwapBuffers(EGLDisplay dpy, EGLSurface surface)
{
    //TODO
    return 0;
}

EGLBoolean eglCopyBuffers(EGLDisplay dpy, EGLSurface surface, EGLNativePixmapType target)
{
    //TODO
    return 0;
}

EGLBoolean eglLockSurfaceKHR(EGLDisplay display, EGLSurface surface, const EGLint *attrib_list)
{
    //TODO
    return 0;
}

EGLBoolean eglUnlockSurfaceKHR(EGLDisplay display, EGLSurface surface)
{
    //TODO
    return 0;
}

EGLImageKHR eglCreateImageKHR(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLint *attrib_list)
{
    //TODO
    return 0;
}

EGLBoolean eglDestroyImageKHR(EGLDisplay dpy, EGLImageKHR image)
{
    //TODO
    return 0;
}

EGLSyncKHR eglCreateSyncKHR(EGLDisplay dpy, EGLenum type, const EGLint *attrib_list)
{
    //TODO
    return 0;
}

EGLBoolean eglDestroySyncKHR(EGLDisplay dpy, EGLSyncKHR sync)
{
    //TODO
    return 0;
}

EGLint eglClientWaitSyncKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLint flags, EGLTimeKHR timeout)
{
    //TODO
    return 0;
}

EGLBoolean eglSignalSyncKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLenum mode)
{
    //TODO
    return 0;
}

EGLBoolean eglGetSyncAttribKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLint attribute, EGLint *value)
{
    //TODO
    return 0;
}

EGLBoolean eglSetSwapRectangleANDROID(EGLDisplay dpy, EGLSurface draw, EGLint left, EGLint top, EGLint width, EGLint height)
{
    //TODO
    return 0;
}
