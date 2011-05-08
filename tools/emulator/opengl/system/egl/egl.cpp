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
#include "gralloc_cb.h"

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
    VALIDATE_DISPLAY(dpy, ret)    \
    if (!s_display.initialized()) {        \
        getEGLThreadInfo()->eglError = EGL_NOT_INITIALIZED;    \
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

#define VALIDATE_CONTEXT_RETURN(context,ret)        \
    if (!context) {                                    \
        RETURN_ERROR(ret,EGL_BAD_CONTEXT);    \
    }

#define VALIDATE_SURFACE_RETURN(surface, ret)    \
    if (surface != EGL_NO_SURFACE) {    \
        egl_surface_t* s( static_cast<egl_surface_t*>(surface) );    \
        if (!s->isValid())    \
            return setError(EGL_BAD_SURFACE, EGL_FALSE);    \
        if (s->dpy != (EGLDisplay)&s_display)    \
            return setError(EGL_BAD_DISPLAY, EGL_FALSE);    \
    }


// ----------------------------------------------------------------------------
//EGLContext_t

struct EGLContext_t {

    enum {
        IS_CURRENT      =   0x00010000,
        NEVER_CURRENT   =   0x00020000
    };

    EGLContext_t(EGLDisplay dpy, EGLConfig config) : dpy(dpy), config(config),
        read(EGL_NO_SURFACE), draw(EGL_NO_SURFACE),
        rcContext(0) {
            flags = 0;
            version = 1;
        };
    ~EGLContext_t(){};
    uint32_t            flags;
    EGLDisplay          dpy;
    EGLConfig           config;
    EGLSurface          read;
    EGLSurface          draw;
    EGLint                version;
    uint32_t             rcContext;
};

// ----------------------------------------------------------------------------
//egl_surface_t

//we don't need to handle depth since it's handled when window created on the host

struct egl_surface_t {

    EGLDisplay          dpy;
    EGLConfig           config;


    egl_surface_t(EGLDisplay dpy, EGLConfig config);
    virtual     ~egl_surface_t();

    virtual     EGLBoolean         rcCreate() = 0;
    virtual     EGLBoolean         rcDestroy() = 0;

    virtual     EGLBoolean  connect() { return EGL_TRUE; }
    virtual     void        disconnect() {}
    virtual     EGLBoolean     swapBuffers() { return EGL_TRUE; }

    void         setRcSurface(uint32_t handle){ rcSurface = handle; }
    uint32_t     getRcSurface(){ return rcSurface; }

    virtual     EGLBoolean    isValid(){ return valid; }

    void        setWidth(EGLint w){ width = w; }
    EGLint      getWidth(){ return width; }
    void         setHeight(EGLint h){ height = h; }
    EGLint      getHeight(){ return height; }
    void        setTextureFormat(EGLint _texFormat){ texFormat = _texFormat; }
    EGLint        getTextureFormat(){ return texFormat; }
    void         setTextureTarget(EGLint _texTarget){ texTarget = _texTarget; }
    EGLint        getTextureTarget(){ return texTarget; }

private:
    //
    //Surface attributes
    //
    EGLint     width;
    EGLint     height;
    EGLint    texFormat;
    EGLint    texTarget;

protected:
    EGLBoolean    valid;
    uint32_t     rcSurface; //handle to surface created via remote control


};

egl_surface_t::egl_surface_t(EGLDisplay dpy, EGLConfig config)
    : dpy(dpy), config(config), valid(EGL_FALSE), rcSurface(0)
{
    width = 0;
    height = 0;
    texFormat = EGL_NO_TEXTURE;
    texTarget = EGL_NO_TEXTURE;
}

egl_surface_t::~egl_surface_t()
{
}

// ----------------------------------------------------------------------------
// egl_window_surface_t

struct egl_window_surface_t : public egl_surface_t {

    egl_window_surface_t(
            EGLDisplay dpy, EGLConfig config,
            ANativeWindow* window);

    ~egl_window_surface_t();

    virtual     EGLBoolean     rcCreate();
    virtual     EGLBoolean     rcDestroy();

    virtual     EGLBoolean  connect();
    virtual     void        disconnect();
    virtual     EGLBoolean  swapBuffers();

private:
    ANativeWindow*     nativeWindow;
    android_native_buffer_t*   buffer;

};


egl_window_surface_t::egl_window_surface_t (
            EGLDisplay dpy, EGLConfig config,
            ANativeWindow* window)
    : egl_surface_t(dpy, config),
    nativeWindow(window),
    buffer(NULL)
{
    // keep a reference on the window
    nativeWindow->common.incRef(&nativeWindow->common);
    EGLint w,h;
    nativeWindow->query(nativeWindow, NATIVE_WINDOW_WIDTH, &w);
    setWidth(w);
    nativeWindow->query(nativeWindow, NATIVE_WINDOW_HEIGHT, &h);
    setHeight(h);
}

egl_window_surface_t::~egl_window_surface_t() {
    nativeWindow->common.decRef(&nativeWindow->common);
}

EGLBoolean egl_window_surface_t::rcCreate()
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

EGLBoolean egl_window_surface_t::rcDestroy()
{
    if (!rcSurface) {
        LOGE("rcDestroy called on invalid rcSurface");
        return EGL_FALSE;
    }

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    rcEnc->rcDestroyWindowSurface(rcEnc, rcSurface);
    rcSurface = 0;

    return EGL_TRUE;
}

EGLBoolean egl_window_surface_t::connect()
{
    // dequeue a buffer
    if (nativeWindow->dequeueBuffer(nativeWindow, &buffer) != NO_ERROR) {
        return setError(EGL_BAD_ALLOC, EGL_FALSE);
    }
    buffer->common.incRef(&buffer->common);

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    rcEnc->rcSetWindowColorBuffer(rcEnc, rcSurface, ((cb_handle_t *)(buffer->handle))->hostHandle);

    return EGL_TRUE;
}

void egl_window_surface_t::disconnect()
{
    if (buffer) {
        nativeWindow->queueBuffer(nativeWindow, buffer);
        buffer->common.decRef(&buffer->common);
        buffer = 0;
    }
}

EGLBoolean egl_window_surface_t::swapBuffers()
{
    if (!buffer) {
        return setError(EGL_BAD_ACCESS, EGL_FALSE);
    }

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);

    //post the back buffer
    nativeWindow->queueBuffer(nativeWindow, buffer);

    // dequeue a new buffer
    if (nativeWindow->dequeueBuffer(nativeWindow, &buffer)) {
        return setError(EGL_BAD_ALLOC, EGL_FALSE);
    }

    rcEnc->rcSetWindowColorBuffer(rcEnc, rcSurface, ((cb_handle_t *)(buffer->handle))->hostHandle);

    return EGL_TRUE;
}

// ----------------------------------------------------------------------------
//egl_pbuffer_surface_t

struct egl_pbuffer_surface_t : public egl_surface_t {

    GLenum    format;

    egl_pbuffer_surface_t(
            EGLDisplay dpy, EGLConfig config,
            int32_t w, int32_t h, GLenum format);

    virtual ~egl_pbuffer_surface_t();
    virtual     EGLBoolean     rcCreate();
    virtual     EGLBoolean     rcDestroy();

    virtual     EGLBoolean    connect();

    uint32_t    getRcColorBuffer(){ return rcColorBuffer; }
    void         setRcColorBuffer(uint32_t colorBuffer){ rcColorBuffer = colorBuffer; }
private:
    uint32_t rcColorBuffer;
};

egl_pbuffer_surface_t::egl_pbuffer_surface_t(
        EGLDisplay dpy, EGLConfig config,
        int32_t w, int32_t h, GLenum pixelFormat)
    : egl_surface_t(dpy, config), format(pixelFormat)
{
    setWidth(w);
    setHeight(h);
}

egl_pbuffer_surface_t::~egl_pbuffer_surface_t()
{
    rcColorBuffer = 0;
}

EGLBoolean egl_pbuffer_surface_t::rcCreate()
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

EGLBoolean egl_pbuffer_surface_t::rcDestroy()
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

EGLBoolean egl_pbuffer_surface_t::connect()
{
    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    rcEnc->rcSetWindowColorBuffer(rcEnc, rcSurface, rcColorBuffer);

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
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);

    int attribs_size = 0;
    if (attrib_list) {
        const EGLint * attrib_p = attrib_list;
        while (attrib_p[0] != EGL_NONE) {
            attribs_size += 2;
            attrib_p += 2;
        }
        attribs_size++; //for the terminating EGL_NONE
    }

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    *num_config = rcEnc->rcChooseConfig(rcEnc, (EGLint*)attrib_list, attribs_size, (uint32_t*)configs, config_size);

    return EGL_TRUE;
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
    if (win == 0) {
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
    if (!surface)
        return setError(EGL_BAD_ALLOC, EGL_NO_SURFACE);
    if (!surface->rcCreate()) {
        delete surface;
        return setError(EGL_BAD_ALLOC, EGL_NO_SURFACE);
    }

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
    EGLint texFormat = EGL_NO_TEXTURE;
    EGLint texTarget = EGL_NO_TEXTURE;
    while (attrib_list[0]) {
        switch (attrib_list[0]) {
            case EGL_WIDTH:
                w = attrib_list[1];
                break;
            case EGL_HEIGHT:
                h = attrib_list[1];
                break;
            case EGL_TEXTURE_FORMAT:
                texFormat = attrib_list[1];
                break;
            case EGL_TEXTURE_TARGET:
                texTarget = attrib_list[1];
                break;
            default:
                break;
        };
        attrib_list+=2;
    }
    if (((texFormat == EGL_NO_TEXTURE)&&(texTarget != EGL_NO_TEXTURE)) ||
        ((texFormat != EGL_NO_TEXTURE)&&(texTarget == EGL_NO_TEXTURE))) {
        return setError(EGL_BAD_MATCH, EGL_NO_SURFACE);
    }
    // TODO: check EGL_TEXTURE_FORMAT - need to support eglBindTexImage

    GLenum pixelFormat;
    if (s_display.getConfigPixelFormat(config, &pixelFormat) == EGL_FALSE)
        return setError(EGL_BAD_MATCH, EGL_NO_SURFACE);

    egl_surface_t* surface = new egl_pbuffer_surface_t(dpy, config, w, h, pixelFormat);
    if (!surface)
        return setError(EGL_BAD_ALLOC, EGL_NO_SURFACE);
    if (!surface->rcCreate()) {
        delete surface;
        return setError(EGL_BAD_ALLOC, EGL_NO_SURFACE);
    }

    //setup attributes
    surface->setTextureFormat(texFormat);
    surface->setTextureTarget(texTarget);

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
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);
    VALIDATE_SURFACE_RETURN(eglSurface, EGL_FALSE);

    egl_surface_t* surface( static_cast<egl_surface_t*>(eglSurface) );

    surface->disconnect();
    surface->rcDestroy();
    delete surface;

    return EGL_TRUE;
}

EGLBoolean eglQuerySurface(EGLDisplay dpy, EGLSurface eglSurface, EGLint attribute, EGLint *value)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);
    VALIDATE_SURFACE_RETURN(eglSurface, EGL_FALSE);

    egl_surface_t* surface( static_cast<egl_surface_t*>(eglSurface) );
    EGLBoolean ret = EGL_TRUE;
    switch (attribute) {
        case EGL_CONFIG_ID:
            ret = s_display.getConfigAttrib(surface->config, EGL_CONFIG_ID, value);
            break;
        case EGL_WIDTH:
            *value = surface->getWidth();
            break;
        case EGL_HEIGHT:
            *value = surface->getHeight();
            break;
        case EGL_TEXTURE_FORMAT:
            *value = surface->getTextureFormat();
            break;
        case EGL_TEXTURE_TARGET:
            *value = surface->getTextureTarget();
            break;
            //TODO: complete other attributes
        default:
            ret = setError(EGL_BAD_ATTRIBUTE, EGL_FALSE);
            break;
    }

    return ret;
}

EGLBoolean eglBindAPI(EGLenum api)
{
    if (api != EGL_OPENGL_ES_API)
        return setError(EGL_BAD_PARAMETER, EGL_FALSE);
    return EGL_TRUE;
}

EGLenum eglQueryAPI()
{
    return EGL_OPENGL_ES_API;
}

EGLBoolean eglWaitClient()
{
    return eglWaitGL();
}

EGLBoolean eglReleaseThread()
{
    EGLThreadInfo *tInfo = getEGLThreadInfo();
    if (tInfo && tInfo->currentContext) {
        return eglMakeCurrent(&s_display, EGL_NO_CONTEXT, EGL_NO_SURFACE, EGL_NO_SURFACE);
    }
    return EGL_TRUE;
}

EGLSurface eglCreatePbufferFromClientBuffer(EGLDisplay dpy, EGLenum buftype, EGLClientBuffer buffer, EGLConfig config, const EGLint *attrib_list)
{
    //TODO
    LOGW("%s not implemented", __FUNCTION__);
    return 0;
}

EGLBoolean eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint value)
{
    //TODO
    LOGW("%s not implemented", __FUNCTION__);
    return 0;
}

EGLBoolean eglBindTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    //TODO
    LOGW("%s not implemented", __FUNCTION__);
    return 0;
}

EGLBoolean eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    //TODO
    LOGW("%s not implemented", __FUNCTION__);
    return 0;
}

EGLBoolean eglSwapInterval(EGLDisplay dpy, EGLint interval)
{
    //TODO
    LOGW("%s not implemented", __FUNCTION__);
    return 0;
}

EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig config, EGLContext share_context, const EGLint *attrib_list)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_NO_CONTEXT);
    VALIDATE_CONFIG(config, EGL_NO_CONTEXT);

    EGLint version = 1; //default
    while (attrib_list[0]) {
        if (attrib_list[0] == EGL_CONTEXT_CLIENT_VERSION) version = attrib_list[1];
        attrib_list+=2;
    }

    uint32_t rcShareCtx = 0;
    if (share_context) {
        EGLContext_t * shareCtx = static_cast<EGLContext_t*>(share_context);
        rcShareCtx = shareCtx->rcContext;
        if (shareCtx->dpy != dpy)
            return setError(EGL_BAD_MATCH, EGL_NO_CONTEXT);
    }

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_NO_CONTEXT);
    uint32_t rcContext = rcEnc->rcCreateContext(rcEnc, (uint32_t)config, rcShareCtx, version);
    if (!rcContext) {
        LOGE("rcCreateContext returned 0");
        return setError(EGL_BAD_ALLOC, EGL_NO_CONTEXT);
    }

    EGLContext_t * context = new EGLContext_t(dpy, config);
    if (!context)
        return setError(EGL_BAD_ALLOC, EGL_NO_CONTEXT);

    context->version = version;
    context->rcContext = rcContext;


    return context;
}

EGLBoolean eglDestroyContext(EGLDisplay dpy, EGLContext ctx)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);
    VALIDATE_CONTEXT_RETURN(ctx, EGL_FALSE);

    EGLContext_t * context = static_cast<EGLContext_t*>(ctx);

    if (getEGLThreadInfo()->currentContext == context)
    {
        eglMakeCurrent(dpy, EGL_NO_CONTEXT, EGL_NO_SURFACE, EGL_NO_SURFACE);
    }

    if (context->rcContext) {
        DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
        rcEnc->rcDestroyContext(rcEnc, context->rcContext);
        context->rcContext = 0;
    }

    delete context;
    return EGL_TRUE;
}

EGLBoolean eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);
    VALIDATE_SURFACE_RETURN(draw, EGL_FALSE);
    VALIDATE_SURFACE_RETURN(read, EGL_FALSE);

    if ((read == EGL_NO_SURFACE && draw == EGL_NO_SURFACE) && (ctx != EGL_NO_CONTEXT))
        return setError(EGL_BAD_MATCH, EGL_FALSE);
    if ((read != EGL_NO_SURFACE || draw != EGL_NO_SURFACE) && (ctx == EGL_NO_CONTEXT))
        return setError(EGL_BAD_MATCH, EGL_FALSE);

    EGLContext_t * context = static_cast<EGLContext_t*>(ctx);
    uint32_t ctxHandle = (context) ? context->rcContext : 0;
    egl_surface_t * drawSurf = static_cast<egl_surface_t *>(draw);
    uint32_t drawHandle = (drawSurf) ? drawSurf->getRcSurface() : 0;
    egl_surface_t * readSurf = static_cast<egl_surface_t *>(read);
    uint32_t readHandle = (readSurf) ? readSurf->getRcSurface() : 0;

    //
    // Nothing to do if no binding change has made
    //
    EGLThreadInfo *tInfo = getEGLThreadInfo();
    if (tInfo->currentContext == context &&
        context &&
        context->draw == draw &&
        context->read == read) {
        return EGL_TRUE;
    }

    if (context && (context->flags & EGLContext_t::IS_CURRENT) && (context != tInfo->currentContext)) {
        //context is current to another thread
        return setError(EGL_BAD_ACCESS, EGL_FALSE);
    }

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);
    if (rcEnc->rcMakeCurrent(rcEnc, ctxHandle, drawHandle, readHandle) == EGL_FALSE) {
        LOGE("rcMakeCurrent returned EGL_FALSE");
        return setError(EGL_BAD_CONTEXT, EGL_FALSE);
    }

    //
    // Disconnect from the previous drawable
    //
    if (tInfo->currentContext && tInfo->currentContext->draw) {
        egl_surface_t * prevDrawSurf = static_cast<egl_surface_t *>(tInfo->currentContext->draw);
        prevDrawSurf->disconnect();
    }

    //Now make the local bind
    if (context) {
        context->draw = draw;
        context->read = read;
        context->flags |= EGLContext_t::IS_CURRENT;
    }

    if (tInfo->currentContext)
        tInfo->currentContext->flags &= ~EGLContext_t::IS_CURRENT;

    //Now make current
    tInfo->currentContext = context;


    //connect the color buffer
    if (drawSurf)
        drawSurf->connect();

    return EGL_TRUE;
}

EGLContext eglGetCurrentContext()
{
    return getEGLThreadInfo()->currentContext;
}

EGLSurface eglGetCurrentSurface(EGLint readdraw)
{
    EGLContext_t * context = getEGLThreadInfo()->currentContext;
    if (!context)
        return EGL_NO_SURFACE; //not an error

    switch (readdraw) {
        case EGL_READ:
            return context->read;
        case EGL_DRAW:
            return context->draw;
        default:
            return setError(EGL_BAD_PARAMETER, EGL_NO_SURFACE);
    }
}

EGLDisplay eglGetCurrentDisplay()
{
    EGLContext_t * context = getEGLThreadInfo()->currentContext;
    if (!context)
        return EGL_NO_DISPLAY; //not an error

    return context->dpy;
}

EGLBoolean eglQueryContext(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);
    VALIDATE_CONTEXT_RETURN(ctx, EGL_FALSE);

    EGLContext_t * context = static_cast<EGLContext_t*>(ctx);

    EGLBoolean ret = EGL_TRUE;
    switch (attribute) {
        case EGL_CONFIG_ID:
            ret = s_display.getConfigAttrib(context->config, EGL_CONFIG_ID, value);
            break;
        case EGL_CONTEXT_CLIENT_TYPE:
            *value = EGL_OPENGL_ES_API;
            break;
        case EGL_CONTEXT_CLIENT_VERSION:
            *value = context->version;
            break;
        case EGL_RENDER_BUFFER:
            if (!context->draw)
                *value = EGL_NONE;
            else
                *value = EGL_BACK_BUFFER; //single buffer not supported
            break;
        default:
            return setError(EGL_BAD_ATTRIBUTE, EGL_FALSE);
    }

    return ret;
}

EGLBoolean eglWaitGL()
{
    EGLThreadInfo *tInfo = getEGLThreadInfo();
    if (!tInfo || !tInfo->currentContext) {
        return EGL_FALSE;
    }

    if (tInfo->currentContext->version == 2) {
        s_display.gles2_iface()->finish();
    }
    else {
        s_display.gles_iface()->finish();
    }

    return EGL_TRUE;
}

EGLBoolean eglWaitNative(EGLint engine)
{
    return EGL_TRUE;
}

EGLBoolean eglSwapBuffers(EGLDisplay dpy, EGLSurface eglSurface)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);
    if (eglSurface == EGL_NO_SURFACE)
        return setError(EGL_BAD_SURFACE, EGL_FALSE);

    DEFINE_AND_VALIDATE_HOST_CONNECTION(EGL_FALSE);

    egl_surface_t* d = static_cast<egl_surface_t*>(eglSurface);
    if (!d->isValid())
        return setError(EGL_BAD_SURFACE, EGL_FALSE);
    if (d->dpy != dpy)
        return setError(EGL_BAD_DISPLAY, EGL_FALSE);

    // post the surface
    d->swapBuffers();

    hostCon->flush();
    return EGL_TRUE;
}

EGLBoolean eglCopyBuffers(EGLDisplay dpy, EGLSurface surface, EGLNativePixmapType target)
{
    //TODO :later
    return 0;
}

EGLBoolean eglLockSurfaceKHR(EGLDisplay display, EGLSurface surface, const EGLint *attrib_list)
{
    //TODO later
    return 0;
}

EGLBoolean eglUnlockSurfaceKHR(EGLDisplay display, EGLSurface surface)
{
    //TODO later
    return 0;
}

EGLImageKHR eglCreateImageKHR(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLint *attrib_list)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_NO_IMAGE_KHR);

    if (ctx != EGL_NO_CONTEXT) {
        return setError(EGL_BAD_CONTEXT, EGL_NO_IMAGE_KHR);
    }
    if (target != EGL_NATIVE_BUFFER_ANDROID) {
        return setError(EGL_BAD_PARAMETER, EGL_NO_IMAGE_KHR);
    }

    android_native_buffer_t* native_buffer = (android_native_buffer_t*)buffer;

    if (native_buffer->common.magic != ANDROID_NATIVE_BUFFER_MAGIC)
        return setError(EGL_BAD_PARAMETER, EGL_NO_IMAGE_KHR);

    if (native_buffer->common.version != sizeof(android_native_buffer_t))
        return setError(EGL_BAD_PARAMETER, EGL_NO_IMAGE_KHR);

    switch (native_buffer->format) {
        case HAL_PIXEL_FORMAT_RGBA_8888:
        case HAL_PIXEL_FORMAT_RGBX_8888:
        case HAL_PIXEL_FORMAT_RGB_888:
        case HAL_PIXEL_FORMAT_RGB_565:
        case HAL_PIXEL_FORMAT_BGRA_8888:
        case HAL_PIXEL_FORMAT_RGBA_5551:
        case HAL_PIXEL_FORMAT_RGBA_4444:
            break;
        default:
            return setError(EGL_BAD_PARAMETER, EGL_NO_IMAGE_KHR);
    }

    native_buffer->common.incRef(&native_buffer->common);
    return (EGLImageKHR)native_buffer;
}

EGLBoolean eglDestroyImageKHR(EGLDisplay dpy, EGLImageKHR img)
{
    VALIDATE_DISPLAY_INIT(dpy, EGL_FALSE);

    android_native_buffer_t* native_buffer = (android_native_buffer_t*)img;

    if (native_buffer->common.magic != ANDROID_NATIVE_BUFFER_MAGIC)
        return setError(EGL_BAD_PARAMETER, EGL_FALSE);

    if (native_buffer->common.version != sizeof(android_native_buffer_t))
        return setError(EGL_BAD_PARAMETER, EGL_FALSE);

    native_buffer->common.decRef(&native_buffer->common);

    return EGL_TRUE;
}

EGLSyncKHR eglCreateSyncKHR(EGLDisplay dpy, EGLenum type, const EGLint *attrib_list)
{
    //TODO later
    return 0;
}

EGLBoolean eglDestroySyncKHR(EGLDisplay dpy, EGLSyncKHR sync)
{
    //TODO later
    return 0;
}

EGLint eglClientWaitSyncKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLint flags, EGLTimeKHR timeout)
{
    //TODO
    return 0;
}

EGLBoolean eglSignalSyncKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLenum mode)
{
    //TODO later
    return 0;
}

EGLBoolean eglGetSyncAttribKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLint attribute, EGLint *value)
{
    //TODO later
    return 0;
}

EGLBoolean eglSetSwapRectangleANDROID(EGLDisplay dpy, EGLSurface draw, EGLint left, EGLint top, EGLint width, EGLint height)
{
    //TODO later
    return 0;
}
