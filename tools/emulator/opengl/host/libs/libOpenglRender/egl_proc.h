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
#ifndef _EGL_PROC_H
#define _EGL_PROC_H

#include <EGL/egl.h>
#define EGL_EGLEXT_PROTOTYPES
#include <EGL/eglext.h>

typedef EGLint (EGLAPIENTRY *eglGetError_t) ();
typedef EGLDisplay (EGLAPIENTRY *eglGetDisplay_t) (EGLNativeDisplayType);
typedef EGLBoolean (EGLAPIENTRY *eglInitialize_t) (EGLDisplay, EGLint*, EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglTerminate_t) (EGLDisplay);
typedef char* (EGLAPIENTRY *eglQueryString_t) (EGLDisplay, EGLint);
typedef EGLBoolean (EGLAPIENTRY *eglGetConfigs_t) (EGLDisplay, EGLConfig*, EGLint, EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglChooseConfig_t) (EGLDisplay, const EGLint*, EGLConfig*, EGLint, EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglGetConfigAttrib_t) (EGLDisplay, EGLConfig, EGLint, EGLint*);
typedef EGLSurface (EGLAPIENTRY *eglCreateWindowSurface_t) (EGLDisplay, EGLConfig, EGLNativeWindowType, const EGLint*);
typedef EGLSurface (EGLAPIENTRY *eglCreatePbufferSurface_t) (EGLDisplay, EGLConfig, const EGLint*);
typedef EGLSurface (EGLAPIENTRY *eglCreatePixmapSurface_t) (EGLDisplay, EGLConfig, EGLNativePixmapType, const EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglDestroySurface_t) (EGLDisplay, EGLSurface);
typedef EGLBoolean (EGLAPIENTRY *eglQuerySurface_t) (EGLDisplay, EGLSurface, EGLint, EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglBindAPI_t) (EGLenum);
typedef EGLenum (* eglQueryAPI_t) ();
typedef EGLBoolean (EGLAPIENTRY *eglWaitClient_t) ();
typedef EGLBoolean (EGLAPIENTRY *eglReleaseThread_t) ();
typedef EGLSurface (EGLAPIENTRY *eglCreatePbufferFromClientBuffer_t) (EGLDisplay, EGLenum, EGLClientBuffer, EGLConfig, const EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglSurfaceAttrib_t) (EGLDisplay, EGLSurface, EGLint, EGLint);
typedef EGLBoolean (EGLAPIENTRY *eglBindTexImage_t) (EGLDisplay, EGLSurface, EGLint);
typedef EGLBoolean (EGLAPIENTRY *eglReleaseTexImage_t) (EGLDisplay, EGLSurface, EGLint);
typedef EGLBoolean (EGLAPIENTRY *eglSwapInterval_t) (EGLDisplay, EGLint);
typedef EGLContext (EGLAPIENTRY *eglCreateContext_t) (EGLDisplay, EGLConfig, EGLContext, const EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglDestroyContext_t) (EGLDisplay, EGLContext);
typedef EGLBoolean (EGLAPIENTRY *eglMakeCurrent_t) (EGLDisplay, EGLSurface, EGLSurface, EGLContext);
typedef EGLContext (EGLAPIENTRY *eglGetCurrentContext_t) ();
typedef EGLSurface (EGLAPIENTRY *eglGetCurrentSurface_t) (EGLint);
typedef EGLDisplay (EGLAPIENTRY *eglGetCurrentDisplay_t) ();
typedef EGLBoolean (EGLAPIENTRY *eglQueryContext_t) (EGLDisplay, EGLContext, EGLint, EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglWaitGL_t) ();
typedef EGLBoolean (EGLAPIENTRY *eglWaitNative_t) (EGLint);
typedef EGLBoolean (EGLAPIENTRY *eglSwapBuffers_t) (EGLDisplay, EGLSurface);
typedef EGLBoolean (EGLAPIENTRY *eglCopyBuffers_t) (EGLDisplay, EGLSurface, EGLNativePixmapType);
typedef __eglMustCastToProperFunctionPointerType (EGLAPIENTRY *eglGetProcAddress_t) (const char*);
typedef EGLBoolean (EGLAPIENTRY *eglLockSurfaceKHR_t) (EGLDisplay, EGLSurface, const EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglUnlockSurfaceKHR_t) (EGLDisplay, EGLSurface);
typedef EGLImageKHR (EGLAPIENTRY *eglCreateImageKHR_t) (EGLDisplay, EGLContext, EGLenum, EGLClientBuffer, const EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglDestroyImageKHR_t) (EGLDisplay, EGLImageKHR image);
typedef EGLSyncKHR (EGLAPIENTRY *eglCreateSyncKHR_t) (EGLDisplay, EGLenum, const EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglDestroySyncKHR_t) (EGLDisplay, EGLSyncKHR sync);
typedef EGLint (EGLAPIENTRY *eglClientWaitSyncKHR_t) (EGLDisplay, EGLSyncKHR, EGLint, EGLTimeKHR timeout);
typedef EGLBoolean (EGLAPIENTRY *eglSignalSyncKHR_t) (EGLDisplay, EGLSyncKHR, EGLenum);
typedef EGLBoolean (EGLAPIENTRY *eglGetSyncAttribKHR_t) (EGLDisplay, EGLSyncKHR, EGLint, EGLint*);
typedef EGLBoolean (EGLAPIENTRY *eglSetSwapRectangleANDROID_t) (EGLDisplay, EGLSurface, EGLint, EGLint, EGLint, EGLint);

#endif // of  _EGL_PROC_H
