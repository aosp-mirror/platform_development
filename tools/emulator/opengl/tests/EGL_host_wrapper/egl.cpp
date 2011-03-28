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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "egl_dispatch.h"
#include "egl_ftable.h"
#include <pthread.h>

#define EGL_LIB "ANDROID_EGL_LIB"

static struct egl_dispatch *s_dispatch = NULL;
static pthread_once_t eglDispatchInitialized = PTHREAD_ONCE_INIT;

void initEglDispatch()
{
    //
    // Load back-end EGL implementation library
    //
    char *eglLib = (char *) "libEGL.so";
    if (getenv(EGL_LIB) != NULL) {
        eglLib = getenv(EGL_LIB);
    }

    s_dispatch = loadEGL(eglLib);
    if (!s_dispatch) {
        fprintf(stderr,"FATAL ERROR: Could not load EGL lib [%s]\n", eglLib);
        exit(-1);
    }
}

static struct egl_dispatch *getDispatch()
{
    pthread_once(&eglDispatchInitialized, initEglDispatch);
    return s_dispatch;
}

__eglMustCastToProperFunctionPointerType eglGetProcAddress(const char *procname)
{
     for (int i=0; i<egl_num_funcs; i++) {
         if (!strcmp(egl_funcs_by_name[i].name, procname)) {
             return (__eglMustCastToProperFunctionPointerType)egl_funcs_by_name[i].proc;
         }
     }

     return getDispatch()->eglGetProcAddress(procname);
}

////////////////  Path through functions //////////

EGLint eglGetError()
{
     return getDispatch()->eglGetError();
}

EGLDisplay eglGetDisplay(EGLNativeDisplayType display_id)
{
    return getDispatch()->eglGetDisplay(display_id);
}

EGLBoolean eglInitialize(EGLDisplay dpy, EGLint *major, EGLint *minor)
{
     return getDispatch()->eglInitialize(dpy, major, minor);
}

EGLBoolean eglTerminate(EGLDisplay dpy)
{
     return getDispatch()->eglTerminate(dpy);
}

const char* eglQueryString(EGLDisplay dpy, EGLint name)
{
     return getDispatch()->eglQueryString(dpy, name);
}

EGLBoolean eglGetConfigs(EGLDisplay dpy, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
     return getDispatch()->eglGetConfigs(dpy, configs, config_size, num_config);
}

EGLBoolean eglChooseConfig(EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
     return getDispatch()->eglChooseConfig(dpy, attrib_list, configs, config_size, num_config);
}

EGLBoolean eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value)
{
     return getDispatch()->eglGetConfigAttrib(dpy, config, attribute, value);
}

EGLSurface eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *attrib_list)
{
     return getDispatch()->eglCreateWindowSurface(dpy, config, win, attrib_list);
}

EGLSurface eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list)
{
     return getDispatch()->eglCreatePbufferSurface(dpy, config, attrib_list);
}

EGLSurface eglCreatePixmapSurface(EGLDisplay dpy, EGLConfig config, EGLNativePixmapType pixmap, const EGLint *attrib_list)
{
     return getDispatch()->eglCreatePixmapSurface(dpy, config, pixmap, attrib_list);
}

EGLBoolean eglDestroySurface(EGLDisplay dpy, EGLSurface surface)
{
     return getDispatch()->eglDestroySurface(dpy, surface);
}

EGLBoolean eglQuerySurface(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint *value)
{
     return getDispatch()->eglQuerySurface(dpy, surface, attribute, value);
}

EGLBoolean eglBindAPI(EGLenum api)
{
     return getDispatch()->eglBindAPI(api);
}

EGLenum eglQueryAPI()
{
     return getDispatch()->eglQueryAPI();
}

EGLBoolean eglWaitClient()
{
     return getDispatch()->eglWaitClient();
}

EGLBoolean eglReleaseThread()
{
     return getDispatch()->eglReleaseThread();
}

EGLSurface eglCreatePbufferFromClientBuffer(EGLDisplay dpy, EGLenum buftype, EGLClientBuffer buffer, EGLConfig config, const EGLint *attrib_list)
{
     return getDispatch()->eglCreatePbufferFromClientBuffer(dpy, buftype, buffer, config, attrib_list);
}

EGLBoolean eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint value)
{
     return getDispatch()->eglSurfaceAttrib(dpy, surface, attribute, value);
}

EGLBoolean eglBindTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
     return getDispatch()->eglBindTexImage(dpy, surface, buffer);
}

EGLBoolean eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
     return getDispatch()->eglReleaseTexImage(dpy, surface, buffer);
}

EGLBoolean eglSwapInterval(EGLDisplay dpy, EGLint interval)
{
     return getDispatch()->eglSwapInterval(dpy, interval);
}

EGLContext eglCreateContext(EGLDisplay dpy, EGLConfig config, EGLContext share_context, const EGLint *attrib_list)
{
     return getDispatch()->eglCreateContext(dpy, config, share_context, attrib_list);
}

EGLBoolean eglDestroyContext(EGLDisplay dpy, EGLContext ctx)
{
     return getDispatch()->eglDestroyContext(dpy, ctx);
}

EGLBoolean eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx)
{
     return getDispatch()->eglMakeCurrent(dpy, draw, read, ctx);
}

EGLContext eglGetCurrentContext()
{
     return getDispatch()->eglGetCurrentContext();
}

EGLSurface eglGetCurrentSurface(EGLint readdraw)
{
     return getDispatch()->eglGetCurrentSurface(readdraw);
}

EGLDisplay eglGetCurrentDisplay()
{
     return getDispatch()->eglGetCurrentDisplay();
}

EGLBoolean eglQueryContext(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value)
{
     return getDispatch()->eglQueryContext(dpy, ctx, attribute, value);
}

EGLBoolean eglWaitGL()
{
     return getDispatch()->eglWaitGL();
}

EGLBoolean eglWaitNative(EGLint engine)
{
     return getDispatch()->eglWaitNative(engine);
}

EGLBoolean eglSwapBuffers(EGLDisplay dpy, EGLSurface surface)
{
     return getDispatch()->eglSwapBuffers(dpy, surface);
}

EGLBoolean eglCopyBuffers(EGLDisplay dpy, EGLSurface surface, EGLNativePixmapType target)
{
     return getDispatch()->eglCopyBuffers(dpy, surface, target);
}

EGLBoolean eglLockSurfaceKHR(EGLDisplay display, EGLSurface surface, const EGLint *attrib_list)
{
     return getDispatch()->eglLockSurfaceKHR(display, surface, attrib_list);
}

EGLBoolean eglUnlockSurfaceKHR(EGLDisplay display, EGLSurface surface)
{
     return getDispatch()->eglUnlockSurfaceKHR(display, surface);
}

EGLImageKHR eglCreateImageKHR(EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, const EGLint *attrib_list)
{
     return getDispatch()->eglCreateImageKHR(dpy, ctx, target, buffer, attrib_list);
}

EGLBoolean eglDestroyImageKHR(EGLDisplay dpy, EGLImageKHR image)
{
     return getDispatch()->eglDestroyImageKHR(dpy, image);
}

EGLSyncKHR eglCreateSyncKHR(EGLDisplay dpy, EGLenum type, const EGLint *attrib_list)
{
     return getDispatch()->eglCreateSyncKHR(dpy, type, attrib_list);
}

EGLBoolean eglDestroySyncKHR(EGLDisplay dpy, EGLSyncKHR sync)
{
     return getDispatch()->eglDestroySyncKHR(dpy, sync);
}

EGLint eglClientWaitSyncKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLint flags, EGLTimeKHR timeout)
{
     return getDispatch()->eglClientWaitSyncKHR(dpy, sync, flags, timeout);
}

EGLBoolean eglSignalSyncKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLenum mode)
{
     return getDispatch()->eglSignalSyncKHR(dpy, sync, mode);
}

EGLBoolean eglGetSyncAttribKHR(EGLDisplay dpy, EGLSyncKHR sync, EGLint attribute, EGLint *value)
{
     return getDispatch()->eglGetSyncAttribKHR(dpy, sync, attribute, value);
}

EGLBoolean eglSetSwapRectangleANDROID(EGLDisplay dpy, EGLSurface draw, EGLint left, EGLint top, EGLint width, EGLint height)
{
     return getDispatch()->eglSetSwapRectangleANDROID(dpy, draw, left, top, width, height);
}
