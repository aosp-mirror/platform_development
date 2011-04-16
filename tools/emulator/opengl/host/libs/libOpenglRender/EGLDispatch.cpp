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
#include "EGLDispatch.h"
#include <stdio.h>
#include <stdlib.h>
#include "osDynLibrary.h"

EGLDispatch s_egl;

bool init_egl_dispatch()
{
    const char *libName = getenv("ANDROID_EGL_LIB");
    if (!libName) libName = "libEGL.so";

    osUtils::dynLibrary *lib = osUtils::dynLibrary::open(libName);
    if (!lib) {
        return NULL;
    }

    s_egl.eglGetError = (eglGetError_t) lib->findSymbol("eglGetError");
    s_egl.eglGetDisplay = (eglGetDisplay_t) lib->findSymbol("eglGetDisplay");
    s_egl.eglInitialize = (eglInitialize_t) lib->findSymbol("eglInitialize");
    s_egl.eglTerminate = (eglTerminate_t) lib->findSymbol("eglTerminate");
    s_egl.eglQueryString = (eglQueryString_t) lib->findSymbol("eglQueryString");
    s_egl.eglGetConfigs = (eglGetConfigs_t) lib->findSymbol("eglGetConfigs");
    s_egl.eglChooseConfig = (eglChooseConfig_t) lib->findSymbol("eglChooseConfig");
    s_egl.eglGetConfigAttrib = (eglGetConfigAttrib_t) lib->findSymbol("eglGetConfigAttrib");
    s_egl.eglCreateWindowSurface = (eglCreateWindowSurface_t) lib->findSymbol("eglCreateWindowSurface");
    s_egl.eglCreatePbufferSurface = (eglCreatePbufferSurface_t) lib->findSymbol("eglCreatePbufferSurface");
    s_egl.eglCreatePixmapSurface = (eglCreatePixmapSurface_t) lib->findSymbol("eglCreatePixmapSurface");
    s_egl.eglDestroySurface = (eglDestroySurface_t) lib->findSymbol("eglDestroySurface");
    s_egl.eglQuerySurface = (eglQuerySurface_t) lib->findSymbol("eglQuerySurface");
    s_egl.eglBindAPI = (eglBindAPI_t) lib->findSymbol("eglBindAPI");
    s_egl.eglQueryAPI = (eglQueryAPI_t) lib->findSymbol("eglQueryAPI");
    s_egl.eglWaitClient = (eglWaitClient_t) lib->findSymbol("eglWaitClient");
    s_egl.eglReleaseThread = (eglReleaseThread_t) lib->findSymbol("eglReleaseThread");
    s_egl.eglCreatePbufferFromClientBuffer = (eglCreatePbufferFromClientBuffer_t) lib->findSymbol("eglCreatePbufferFromClientBuffer");
    s_egl.eglSurfaceAttrib = (eglSurfaceAttrib_t) lib->findSymbol("eglSurfaceAttrib");
    s_egl.eglBindTexImage = (eglBindTexImage_t) lib->findSymbol("eglBindTexImage");
    s_egl.eglReleaseTexImage = (eglReleaseTexImage_t) lib->findSymbol("eglReleaseTexImage");
    s_egl.eglSwapInterval = (eglSwapInterval_t) lib->findSymbol("eglSwapInterval");
    s_egl.eglCreateContext = (eglCreateContext_t) lib->findSymbol("eglCreateContext");
    s_egl.eglDestroyContext = (eglDestroyContext_t) lib->findSymbol("eglDestroyContext");
    s_egl.eglMakeCurrent = (eglMakeCurrent_t) lib->findSymbol("eglMakeCurrent");
    s_egl.eglGetCurrentContext = (eglGetCurrentContext_t) lib->findSymbol("eglGetCurrentContext");
    s_egl.eglGetCurrentSurface = (eglGetCurrentSurface_t) lib->findSymbol("eglGetCurrentSurface");
    s_egl.eglGetCurrentDisplay = (eglGetCurrentDisplay_t) lib->findSymbol("eglGetCurrentDisplay");
    s_egl.eglQueryContext = (eglQueryContext_t) lib->findSymbol("eglQueryContext");
    s_egl.eglWaitGL = (eglWaitGL_t) lib->findSymbol("eglWaitGL");
    s_egl.eglWaitNative = (eglWaitNative_t) lib->findSymbol("eglWaitNative");
    s_egl.eglSwapBuffers = (eglSwapBuffers_t) lib->findSymbol("eglSwapBuffers");
    s_egl.eglCopyBuffers = (eglCopyBuffers_t) lib->findSymbol("eglCopyBuffers");
    s_egl.eglGetProcAddress = (eglGetProcAddress_t) lib->findSymbol("eglGetProcAddress");
    s_egl.eglLockSurfaceKHR = (eglLockSurfaceKHR_t) lib->findSymbol("eglLockSurfaceKHR");
    s_egl.eglUnlockSurfaceKHR = (eglUnlockSurfaceKHR_t) lib->findSymbol("eglUnlockSurfaceKHR");
    s_egl.eglCreateImageKHR = (eglCreateImageKHR_t) lib->findSymbol("eglCreateImageKHR");
    s_egl.eglDestroyImageKHR = (eglDestroyImageKHR_t) lib->findSymbol("eglDestroyImageKHR");
    s_egl.eglCreateSyncKHR = (eglCreateSyncKHR_t) lib->findSymbol("eglCreateSyncKHR");
    s_egl.eglDestroySyncKHR = (eglDestroySyncKHR_t) lib->findSymbol("eglDestroySyncKHR");
    s_egl.eglClientWaitSyncKHR = (eglClientWaitSyncKHR_t) lib->findSymbol("eglClientWaitSyncKHR");
    s_egl.eglSignalSyncKHR = (eglSignalSyncKHR_t) lib->findSymbol("eglSignalSyncKHR");
    s_egl.eglGetSyncAttribKHR = (eglGetSyncAttribKHR_t) lib->findSymbol("eglGetSyncAttribKHR");
    s_egl.eglSetSwapRectangleANDROID = (eglSetSwapRectangleANDROID_t) lib->findSymbol("eglSetSwapRectangleANDROID");

    return true;
}
