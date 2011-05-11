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
#include "RendererSurface.h"
#include <stdio.h>
#include <stdlib.h>

#include "NativeWindowing.h"

#define MAX_ATTRIB 100


EGLConfig RendererSurface::getEglConfig(EGLDisplay eglDisplay, SurfaceConfig config)
{
    EGLConfig eglConfig;
    int nConfigs;

    EGLint attrib[MAX_ATTRIB];
    int pos =0;

    attrib[pos++] = EGL_SURFACE_TYPE; attrib[pos++] = EGL_WINDOW_BIT;
    if (config & CONFIG_DEPTH) {attrib[pos++] = EGL_DEPTH_SIZE; attrib[pos++] = 1;}
    attrib[pos++] = EGL_NONE;

    if (!eglChooseConfig(eglDisplay, attrib, &eglConfig, 1, &nConfigs)) {
        return 0;
    }
    /***/
    int ibuf;
    if (eglGetConfigAttrib(eglDisplay, eglConfig, EGL_BUFFER_SIZE, &ibuf)) {
        fprintf(stderr, "EGL COLOR Buffer size: %d\n", ibuf);
    } else {
        fprintf(stderr, "eglGetConfigAttrib error: %d\n", eglGetError());
    }
    if (eglGetConfigAttrib(eglDisplay, eglConfig, EGL_DEPTH_SIZE, &ibuf)) {
        fprintf(stderr, "EGL DEPTH Buffer size: %d\n", ibuf);
    } else {
        fprintf(stderr, "eglGetConfigAttrib error: %d\n", eglGetError());
    }
    /***/


    if (nConfigs != 1) {
        return 0;
    }
    return eglConfig;
}

RendererSurface * RendererSurface::create(EGLDisplay eglDisplay, SurfaceConfig config, NativeWindowing *nw)
{
    int width = 0, height = 0;
    const char* env;

    env = getenv("ANDROID_WINDOW_WIDTH");
    if (env && *env) {
        width = atoi(env);
    }
    env = getenv("ANDROID_WINDOW_HEIGHT");
    if (env && *env) {
        height = atoi(env);
    }
    if (width <= 160)
        width = DEFAULT_WIDTH;
    if (height <= 160)
        height = DEFAULT_HEIGHT;

    printf("%s: Using width=%d height=%d\n", __FUNCTION__, width, height);

    EGLConfig eglConfig = getEglConfig(eglDisplay, config);
    if (eglConfig == 0) {
        return NULL;
    }

    NativeWindowType window = nw->createNativeWindow(nw->getNativeDisplay(), width, height);
    if (window == 0) {
        return NULL;
    }

    EGLSurface eglSurface = eglCreateWindowSurface(eglDisplay,
                                                   eglConfig,
                                                   window, NULL);

    if (eglGetError() != EGL_SUCCESS) {
        return NULL;
    }

    return new RendererSurface(eglDisplay, window, eglSurface, eglConfig);
}

int RendererSurface::destroy(NativeWindowing *nw)
{
    eglDestroySurface(m_eglDisplay, m_eglSurface);
    nw->destroyNativeWindow(nw->getNativeDisplay(), m_window);
    return 1;
}

