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
#include "RendererContext.h"
#include <stdio.h>
#include <stdlib.h>

RendererContext * RendererContext::create(EGLDisplay dpy, EGLConfig config, RendererContext *shareCtx, int version)
{
    EGLContext ctx;
    EGLContext shared = shareCtx == NULL ? EGL_NO_CONTEXT : shareCtx->eglContext();

    EGLint context_attributes[] = { EGL_CONTEXT_CLIENT_VERSION, 1, EGL_NONE };
    context_attributes[1] = version;

    ctx = eglCreateContext(dpy, config, shared, context_attributes);
    if (eglGetError() != EGL_SUCCESS) return NULL;

    return new RendererContext(dpy, ctx, version);
}

int RendererContext::destroy()
{
    if (count() <= 0) {
        eglDestroyContext(m_dpy, m_ctx);
        return 1;
    }
    return 0;
}

#ifdef PVR_WAR
void RendererContext::setActiveTexture(GLenum texture)
{
    m_activeTexture = texture - GL_TEXTURE0;
}

void RendererContext::setTex2DBind(GLuint texture)
{
    m_tex2DBind[m_activeTexture] = texture;
}

GLuint RendererContext::getTex2DBind()
{
    return m_tex2DBind[m_activeTexture];
}

void RendererContext::addPendingCropRect(const int *rect)
{
    PendingCropRect *r = new PendingCropRect;
    r->texture = m_tex2DBind[m_activeTexture];
    memcpy(r->rect, rect, 4*sizeof(int));
    m_pendingCropRects.insert(r);
}
#endif
