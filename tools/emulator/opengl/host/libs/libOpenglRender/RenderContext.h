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
#ifndef _LIBRENDER_RENDERCONTEXT_H
#define _LIBRENDER_RENDERCONTEXT_H

#include "SmartPtr.h"
#include <EGL/egl.h>
#include "GLDecoderContextData.h"

class RenderContext;
typedef SmartPtr<RenderContext> RenderContextPtr;

class RenderContext
{
public:
    static RenderContext *create(int p_config, RenderContextPtr p_shareContext,
                                 bool p_isGL2 = false);
    ~RenderContext();
    int getConfig() const { return m_config; }

    EGLContext getEGLContext() const { return m_ctx; }
    bool isGL2() const { return m_isGL2; }

    GLDecoderContextData & decoderContextData() { return m_contextData; }

private:
    RenderContext();

private:
    EGLContext m_ctx;
    int        m_config;
    bool       m_isGL2;
    GLDecoderContextData    m_contextData;
};

#endif
