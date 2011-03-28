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
#include <dlfcn.h>
#include "egl_dispatch.h"


egl_dispatch *loadEGL(const char *p_eglPath)
{
    void *libEGL = dlopen(p_eglPath, RTLD_NOW);
    if (!libEGL) {
        return NULL;
    }

    egl_dispatch *disp = new egl_dispatch;

    void *ptr;
    ptr = dlsym(libEGL,"eglGetError"); disp->set_eglGetError((eglGetError_t)ptr);
    ptr = dlsym(libEGL,"eglGetDisplay"); disp->set_eglGetDisplay((eglGetDisplay_t)ptr);
    ptr = dlsym(libEGL,"eglInitialize"); disp->set_eglInitialize((eglInitialize_t)ptr);
    ptr = dlsym(libEGL,"eglTerminate"); disp->set_eglTerminate((eglTerminate_t)ptr);
    ptr = dlsym(libEGL,"eglQueryString"); disp->set_eglQueryString((eglQueryString_t)ptr);
    ptr = dlsym(libEGL,"eglGetConfigs"); disp->set_eglGetConfigs((eglGetConfigs_t)ptr);
    ptr = dlsym(libEGL,"eglChooseConfig"); disp->set_eglChooseConfig((eglChooseConfig_t)ptr);
    ptr = dlsym(libEGL,"eglGetConfigAttrib"); disp->set_eglGetConfigAttrib((eglGetConfigAttrib_t)ptr);
    ptr = dlsym(libEGL,"eglCreateWindowSurface"); disp->set_eglCreateWindowSurface((eglCreateWindowSurface_t)ptr);
    ptr = dlsym(libEGL,"eglCreatePbufferSurface"); disp->set_eglCreatePbufferSurface((eglCreatePbufferSurface_t)ptr);
    ptr = dlsym(libEGL,"eglCreatePixmapSurface"); disp->set_eglCreatePixmapSurface((eglCreatePixmapSurface_t)ptr);
    ptr = dlsym(libEGL,"eglDestroySurface"); disp->set_eglDestroySurface((eglDestroySurface_t)ptr);
    ptr = dlsym(libEGL,"eglQuerySurface"); disp->set_eglQuerySurface((eglQuerySurface_t)ptr);
    ptr = dlsym(libEGL,"eglBindAPI"); disp->set_eglBindAPI((eglBindAPI_t)ptr);
    ptr = dlsym(libEGL,"eglQueryAPI"); disp->set_eglQueryAPI((eglQueryAPI_t)ptr);
    ptr = dlsym(libEGL,"eglWaitClient"); disp->set_eglWaitClient((eglWaitClient_t)ptr);
    ptr = dlsym(libEGL,"eglReleaseThread"); disp->set_eglReleaseThread((eglReleaseThread_t)ptr);
    ptr = dlsym(libEGL,"eglCreatePbufferFromClientBuffer"); disp->set_eglCreatePbufferFromClientBuffer((eglCreatePbufferFromClientBuffer_t)ptr);
    ptr = dlsym(libEGL,"eglSurfaceAttrib"); disp->set_eglSurfaceAttrib((eglSurfaceAttrib_t)ptr);
    ptr = dlsym(libEGL,"eglBindTexImage"); disp->set_eglBindTexImage((eglBindTexImage_t)ptr);
    ptr = dlsym(libEGL,"eglReleaseTexImage"); disp->set_eglReleaseTexImage((eglReleaseTexImage_t)ptr);
    ptr = dlsym(libEGL,"eglSwapInterval"); disp->set_eglSwapInterval((eglSwapInterval_t)ptr);
    ptr = dlsym(libEGL,"eglCreateContext"); disp->set_eglCreateContext((eglCreateContext_t)ptr);
    ptr = dlsym(libEGL,"eglDestroyContext"); disp->set_eglDestroyContext((eglDestroyContext_t)ptr);
    ptr = dlsym(libEGL,"eglMakeCurrent"); disp->set_eglMakeCurrent((eglMakeCurrent_t)ptr);
    ptr = dlsym(libEGL,"eglGetCurrentContext"); disp->set_eglGetCurrentContext((eglGetCurrentContext_t)ptr);
    ptr = dlsym(libEGL,"eglGetCurrentSurface"); disp->set_eglGetCurrentSurface((eglGetCurrentSurface_t)ptr);
    ptr = dlsym(libEGL,"eglGetCurrentDisplay"); disp->set_eglGetCurrentDisplay((eglGetCurrentDisplay_t)ptr);
    ptr = dlsym(libEGL,"eglQueryContext"); disp->set_eglQueryContext((eglQueryContext_t)ptr);
    ptr = dlsym(libEGL,"eglWaitGL"); disp->set_eglWaitGL((eglWaitGL_t)ptr);
    ptr = dlsym(libEGL,"eglWaitNative"); disp->set_eglWaitNative((eglWaitNative_t)ptr);
    ptr = dlsym(libEGL,"eglSwapBuffers"); disp->set_eglSwapBuffers((eglSwapBuffers_t)ptr);
    ptr = dlsym(libEGL,"eglCopyBuffers"); disp->set_eglCopyBuffers((eglCopyBuffers_t)ptr);
    ptr = dlsym(libEGL,"eglGetProcAddress"); disp->set_eglGetProcAddress((eglGetProcAddress_t)ptr);
    ptr = dlsym(libEGL,"eglLockSurfaceKHR"); disp->set_eglLockSurfaceKHR((eglLockSurfaceKHR_t)ptr);
    ptr = dlsym(libEGL,"eglUnlockSurfaceKHR"); disp->set_eglUnlockSurfaceKHR((eglUnlockSurfaceKHR_t)ptr);
    ptr = dlsym(libEGL,"eglCreateImageKHR"); disp->set_eglCreateImageKHR((eglCreateImageKHR_t)ptr);
    ptr = dlsym(libEGL,"eglDestroyImageKHR"); disp->set_eglDestroyImageKHR((eglDestroyImageKHR_t)ptr);
    ptr = dlsym(libEGL,"eglCreateSyncKHR"); disp->set_eglCreateSyncKHR((eglCreateSyncKHR_t)ptr);
    ptr = dlsym(libEGL,"eglDestroySyncKHR"); disp->set_eglDestroySyncKHR((eglDestroySyncKHR_t)ptr);
    ptr = dlsym(libEGL,"eglClientWaitSyncKHR"); disp->set_eglClientWaitSyncKHR((eglClientWaitSyncKHR_t)ptr);
    ptr = dlsym(libEGL,"eglSignalSyncKHR"); disp->set_eglSignalSyncKHR((eglSignalSyncKHR_t)ptr);
    ptr = dlsym(libEGL,"eglGetSyncAttribKHR"); disp->set_eglGetSyncAttribKHR((eglGetSyncAttribKHR_t)ptr);
    ptr = dlsym(libEGL,"eglSetSwapRectangleANDROID"); disp->set_eglSetSwapRectangleANDROID((eglSetSwapRectangleANDROID_t)ptr);

    return disp;
}
