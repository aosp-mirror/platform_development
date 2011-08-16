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
#include "FBConfig.h"
#include "FrameBuffer.h"
#include "EGLDispatch.h"
#include <stdio.h>

FBConfig **FBConfig::s_fbConfigs = NULL;
int FBConfig::s_numConfigs = 0;

const GLuint FBConfig::s_configAttribs[] = {
    EGL_DEPTH_SIZE,     // must be first - see getDepthSize()
    EGL_STENCIL_SIZE,   // must be second - see getStencilSize()
    EGL_RENDERABLE_TYPE,// must be third - see getRenderableType()
    EGL_SURFACE_TYPE,   // must be fourth - see getSurfaceType()
    EGL_CONFIG_ID,      // must be fifth  - see chooseConfig()
    EGL_BUFFER_SIZE,
    EGL_ALPHA_SIZE,
    EGL_BLUE_SIZE,
    EGL_GREEN_SIZE,
    EGL_RED_SIZE,
    EGL_CONFIG_CAVEAT,
    EGL_LEVEL,
    EGL_MAX_PBUFFER_HEIGHT,
    EGL_MAX_PBUFFER_PIXELS,
    EGL_MAX_PBUFFER_WIDTH,
    EGL_NATIVE_RENDERABLE,
    EGL_NATIVE_VISUAL_ID,
    EGL_NATIVE_VISUAL_TYPE,
    EGL_SAMPLES,
    EGL_SAMPLE_BUFFERS,
    EGL_TRANSPARENT_TYPE,
    EGL_TRANSPARENT_BLUE_VALUE,
    EGL_TRANSPARENT_GREEN_VALUE,
    EGL_TRANSPARENT_RED_VALUE,
    EGL_BIND_TO_TEXTURE_RGB,
    EGL_BIND_TO_TEXTURE_RGBA,
    EGL_MIN_SWAP_INTERVAL,
    EGL_MAX_SWAP_INTERVAL,
    EGL_LUMINANCE_SIZE,
    EGL_ALPHA_MASK_SIZE,
    EGL_COLOR_BUFFER_TYPE,
    //EGL_MATCH_NATIVE_PIXMAP,
    EGL_CONFORMANT
};

const int FBConfig::s_numConfigAttribs = sizeof(FBConfig::s_configAttribs) / sizeof(GLuint);

InitConfigStatus FBConfig::initConfigList(FrameBuffer *fb)
{
    InitConfigStatus ret = INIT_CONFIG_FAILED;

    if (!fb) {
        return ret;
    }

    const FrameBufferCaps &caps = fb->getCaps();
    EGLDisplay dpy = fb->getDisplay();

    if (dpy == EGL_NO_DISPLAY) {
        fprintf(stderr,"Could not get EGL Display\n");
        return ret;
    }

    //
    // Query the set of configs in the EGL backend
    //
    EGLint nConfigs;
    if (!s_egl.eglGetConfigs(dpy, NULL, 0, &nConfigs)) {
        fprintf(stderr, "Could not get number of available configs\n");
        return ret;
    }
    EGLConfig *configs = new EGLConfig[nConfigs];
    s_egl.eglGetConfigs(dpy, configs, nConfigs, &nConfigs);

    //
    // copy the config attributes, filter out
    // configs we do not want to support.
    //
    int j = 0;
    s_fbConfigs = new FBConfig*[nConfigs];
    for (int i=0; i<nConfigs; i++) {

        //
        // filter out configs which does not support pbuffers.
        // we only support pbuffer configs since we use a pbuffer
        // handle to bind a guest created window object.
        //
        EGLint surfaceType;
        s_egl.eglGetConfigAttrib(dpy, configs[i],
                                 EGL_SURFACE_TYPE, &surfaceType);
        if (!(surfaceType & EGL_PBUFFER_BIT)) continue;

        //
        // Filter out not RGB configs
        //
        EGLint redSize, greenSize, blueSize;
        s_egl.eglGetConfigAttrib(dpy, configs[i], EGL_RED_SIZE, &redSize);
        s_egl.eglGetConfigAttrib(dpy, configs[i], EGL_BLUE_SIZE, &blueSize);
        s_egl.eglGetConfigAttrib(dpy, configs[i], EGL_GREEN_SIZE, &greenSize);
        if (redSize==0 || greenSize==0 || blueSize==0) continue;

        s_fbConfigs[j++] = new FBConfig(dpy, configs[i]);
    }
    s_numConfigs = j;

    delete[] configs;

    return s_numConfigs > 0 ? INIT_CONFIG_PASSED : INIT_CONFIG_FAILED;
}

const FBConfig *FBConfig::get(int p_config)
{
    if (p_config >= 0 && p_config < s_numConfigs) {
        return s_fbConfigs[p_config];
    }
    return NULL;
}

int FBConfig::getNumConfigs()
{
    return s_numConfigs;
}

void FBConfig::packConfigsInfo(GLuint *buffer)
{
    memcpy(buffer, s_configAttribs, s_numConfigAttribs * sizeof(GLuint));
    for (int i=0; i<s_numConfigs; i++) {
        memcpy(buffer+(i+1)*s_numConfigAttribs,
               s_fbConfigs[i]->m_attribValues,
               s_numConfigAttribs * sizeof(GLuint));
    }
}

int FBConfig::chooseConfig(FrameBuffer *fb, EGLint * attribs, uint32_t * configs, uint32_t configs_size)
{
    EGLDisplay dpy = fb->getDisplay();
    int ret = 0;

    if (dpy == EGL_NO_DISPLAY) {
        fprintf(stderr,"Could not get EGL Display\n");
        return ret;
    }
    //
    // Query the num of configs in the EGL backend
    //
    EGLint nConfigs;
    if (!s_egl.eglGetConfigs(dpy, NULL, 0, &nConfigs)) {
        fprintf(stderr, "Could not get number of available configs\n");
        return ret;
    }
    //
    // Query the max matching configs in the backend
    //
    EGLConfig *matchedConfigs = new EGLConfig[nConfigs];

    //
    //Until we have EGLImage implementation, we force pbuf configs
    //
    bool needToAddPbufAttr = true;
    int attribCnt = 0;
    EGLint * attrib_p = attribs;
    if (attribs) {
        while (attrib_p[0] != EGL_NONE) {
            if (attrib_p[0] == EGL_SURFACE_TYPE) {
                attrib_p[1] = EGL_PBUFFER_BIT; //replace whatever was there before
                needToAddPbufAttr = false;
            }
            attrib_p += 2;
            attribCnt += 2;
        }
    }
    EGLint * newAttribs = new EGLint[attribCnt + 1 + ((needToAddPbufAttr) ? 2 : 0)];
    attrib_p = newAttribs;
    if (needToAddPbufAttr) {
        *(attrib_p++) = EGL_SURFACE_TYPE;
        *(attrib_p++) = EGL_PBUFFER_BIT;
    }
    memcpy(attrib_p, attribs, attribCnt*sizeof(EGLint));
    attrib_p += attribCnt;
    *attrib_p = EGL_NONE;

#if 0
    if (newAttribs) {
        EGLint * attrib_p = newAttribs;
        while (attrib_p[0] != EGL_NONE) {
            DBG("attr: 0x%x %d, ", attrib_p[0], attrib_p[1]);
            attrib_p += 2;
        }
    }
#endif

    s_egl.eglChooseConfig(dpy, newAttribs, matchedConfigs, nConfigs, &nConfigs);

    delete[] newAttribs;

    //
    // From all matchedConfigs we need only config_size FBConfigs, so we intersect both lists compating the CONFIG_ID attribute
    //
    uint32_t nVerifiedCfgs = 0;
    for (int matchedIdx=0; matchedIdx<nConfigs; matchedIdx++) {
        if ((configs != NULL) && (configs_size > 0) && (nVerifiedCfgs >= configs_size)) break; //We have enouhgt configs
        int sCfgId;
        s_egl.eglGetConfigAttrib(dpy, matchedConfigs[matchedIdx], EGL_CONFIG_ID, &sCfgId);
        for (int fbIdx=0; fbIdx<s_numConfigs; fbIdx++) {
            int dCfgId = s_fbConfigs[fbIdx]->m_attribValues[4]; //CONFIG_ID
            if (sCfgId == dCfgId) {
                //This config matches the requested attributes and filtered into fbConfigs, so we're happy with it
                if (configs && nVerifiedCfgs < configs_size) {
                    configs[nVerifiedCfgs] = fbIdx;
                }
                nVerifiedCfgs++;
                break;
            }
        }
    }

    delete[] matchedConfigs;

    return nVerifiedCfgs;
}

FBConfig::FBConfig(EGLDisplay p_eglDpy, EGLConfig p_eglCfg)
{
    m_eglConfig = p_eglCfg;
    m_attribValues = new GLint[s_numConfigAttribs];
    for (int i=0; i<s_numConfigAttribs; i++) {
        m_attribValues[i] = 0;
        s_egl.eglGetConfigAttrib(p_eglDpy, p_eglCfg, s_configAttribs[i], &m_attribValues[i]);

        //
        // All exported configs supports android native window rendering
        //
        if (s_configAttribs[i] == EGL_SURFACE_TYPE) {
            m_attribValues[i] |= EGL_WINDOW_BIT;
        }
    }
}

FBConfig::~FBConfig()
{
    if (m_attribValues) {
        delete[] m_attribValues;
    }
}
