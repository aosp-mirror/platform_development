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
#ifndef _RENDERER_CONTEXT_H_
#define _RENDERER_CONTEXT_H_

#include "RendererObject.h"
#include "GLDecoderContextData.h"

#include <EGL/egl.h>
#define GL_API
#define GL_APIENTRY
#include <GLES/gl.h>
#include <string.h>

#ifdef PVR_WAR
#include <set>
struct PendingCropRect
{
    GLuint texture;
    int rect[4];
};

typedef std::set<PendingCropRect *> PendingCropRectSet;
#endif

class RendererContext : public RendererObject {
public:
    static RendererContext *create(EGLDisplay dpy, EGLConfig config, RendererContext *shareCtx, int version);
    EGLContext eglContext() { return m_ctx; }
    int destroy();
    GLDecoderContextData & decoderContextData() { return m_contextData; }
#ifdef PVR_WAR
    void setActiveTexture(GLenum texture);
    GLenum getActiveTexture() { return GL_TEXTURE0 + m_activeTexture; }
    void setTex2DBind(GLuint texture);
    void setTex2DEnable(bool enable) {
        m_tex2DEnable[m_activeTexture] = enable;
    }
    bool isTex2DEnable(int texunit) { return m_tex2DEnable[texunit]; }
    GLuint getTex2DBind();
    void addPendingCropRect(const int *rect);
    PendingCropRectSet &getPendingCropRects() { return m_pendingCropRects; }

    void setClientActiveTexture(GLenum texture) { m_clientActiveTexture = texture - GL_TEXTURE0; }
    GLenum getClientActiveTexture() { return m_clientActiveTexture + GL_TEXTURE0; }
    void enableClientState(GLenum cap, bool enable) {
        switch(cap) {
            case GL_VERTEX_ARRAY:
                m_clientStateEnable[0] = enable;
                break;
            case GL_NORMAL_ARRAY:
                m_clientStateEnable[1] = enable;
                break;
            case GL_COLOR_ARRAY:
                m_clientStateEnable[2] = enable;
                break;
            case GL_POINT_SIZE_ARRAY_OES:
                m_clientStateEnable[3] = enable;
                break;
            case GL_TEXTURE_COORD_ARRAY:
                m_clientStateEnable[4 + m_clientActiveTexture] = enable;
                break;
        }
    }

    bool getClientState(GLenum cap, int texUnit) {
        switch(cap) {
            case GL_VERTEX_ARRAY:
                return m_clientStateEnable[0];
            case GL_NORMAL_ARRAY:
                return m_clientStateEnable[1];
            case GL_COLOR_ARRAY:
                return m_clientStateEnable[2];
            case GL_POINT_SIZE_ARRAY_OES:
                return m_clientStateEnable[3];
                break;
            case GL_TEXTURE_COORD_ARRAY:
                return m_clientStateEnable[4 + texUnit];
                break;
        }
        return false;
    }
#endif

private:
    EGLDisplay m_dpy;
    EGLContext m_ctx;
    GLDecoderContextData m_contextData;
    int m_version;

    RendererContext(EGLDisplay dpy, EGLContext ctx, int version) :
        m_dpy(dpy),
        m_ctx(ctx),
        m_version(version)
    {
#ifdef PVR_WAR
        m_activeTexture = 0;
        m_clientActiveTexture = 0;
        memset(m_tex2DBind, 0, 8*sizeof(GLuint));
        memset(m_tex2DEnable, 0, 8*sizeof(bool));
        memset(m_clientStateEnable, 0, 16*sizeof(bool));
#endif
    }

#ifdef PVR_WAR
    int m_tex2DBind[8];
    bool m_tex2DEnable[8];
    int m_activeTexture;
    int m_clientActiveTexture;
    bool m_clientStateEnable[16];
    PendingCropRectSet m_pendingCropRects;
#endif
};
#endif
