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
#ifndef _LIBRENDER_FBCONFIG_H
#define _LIBRENDER_FBCONFIG_H

#include <EGL/egl.h>
#include <GLES/gl.h>

class FrameBuffer;

enum InitConfigStatus {
    INIT_CONFIG_FAILED = 0,
    INIT_CONFIG_PASSED = 1
};

class FBConfig
{
public:
    static InitConfigStatus initConfigList(FrameBuffer *fb);
    static const FBConfig *get(int p_config);
    static int getNumConfigs();
    static int getNumAttribs() { return s_numConfigAttribs; }
    static void packConfigsInfo(GLuint *buffer);
    static int chooseConfig(FrameBuffer *fb, EGLint * attribs, uint32_t * configs, uint32_t configs_size);
    ~FBConfig();

    EGLConfig getEGLConfig() const { return m_eglConfig; }
    GLuint  getDepthSize() const { return (m_attribValues ? m_attribValues[0] : 0); }
    GLuint  getStencilSize() const { return (m_attribValues ? m_attribValues[1] : 0); }
    GLuint  getRenderableType() const { return (m_attribValues ? m_attribValues[2] : 0); }
    GLuint getSurfaceType() const { return (m_attribValues ? m_attribValues[3] : 0); }

private:
    FBConfig(EGLDisplay p_eglDpy, EGLConfig p_eglCfg);

private:
    static FBConfig **s_fbConfigs;
    static int s_numConfigs;
    static const int s_numConfigAttribs;
    static const GLuint s_configAttribs[];

private:
    EGLConfig m_eglConfig;
    GLint *m_attribValues;
};

#endif
