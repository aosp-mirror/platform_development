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
#include "RenderContext.h"
#include "FBConfig.h"
#include "FrameBuffer.h"
#include "EGLDispatch.h"
#include "GLDispatch.h"

RenderContext *RenderContext::create(int p_config,
                                     RenderContextPtr p_shareContext,
                                     bool p_isGL2)
{
    const FBConfig *fbconf = FBConfig::get(p_config);
    if (!fbconf) {
        return NULL;
    }

    RenderContext *c = new RenderContext();
    if (!c) {
        return NULL;
    }

    EGLContext share = EGL_NO_CONTEXT;
    if (p_shareContext.Ptr() != NULL) {
        share = p_shareContext->getEGLContext();
    }

    GLint glContextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 1,
        EGL_NONE
    };

    if (p_isGL2) {
        glContextAttribs[1] = 2;
        c->m_isGL2 = true;
    }

    c->m_ctx = s_egl.eglCreateContext(FrameBuffer::getFB()->getDisplay(),
                                      fbconf->getEGLConfig(), share,
                                      glContextAttribs);

    if (c->m_ctx == EGL_NO_CONTEXT) {
        delete c;
        return NULL;
    }

    c->m_config = p_config;
    return c;
}

RenderContext::RenderContext() :
    m_ctx(EGL_NO_CONTEXT),
    m_config(0),
    m_isGL2(false)
{
}

RenderContext::~RenderContext()
{
    if (m_ctx != EGL_NO_CONTEXT) {
        s_egl.eglDestroyContext(FrameBuffer::getFB()->getDisplay(), m_ctx);
    }
}
