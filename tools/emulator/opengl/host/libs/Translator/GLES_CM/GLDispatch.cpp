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
#include "GLDispatch.h"
#include <stdio.h>
#include <OpenglOsUtils/osDynLibrary.h>

#ifdef __linux__
#include <GL/glx.h>
#elif defined(WIN32)
#include <windows.h>
#endif

typedef void (*GL_FUNC_PTR)();

static GL_FUNC_PTR getGLFuncAddress(const char *funcName) {
    GL_FUNC_PTR ret = NULL;
#ifdef __linux__
    static osUtils::dynLibrary* libGL = osUtils::dynLibrary::open("libGL.so");
    ret = (GL_FUNC_PTR)glXGetProcAddress((const GLubyte*)funcName);
#elif defined(WIN32)
    static osUtils::dynLibrary* libGL = osUtils::dynLibrary::open("opengl32");
    ret = (GL_FUNC_PTR)wglGetProcAddress(funcName);
#endif
    if(!ret && libGL){
        ret = libGL->findSymbol(funcName);
    }
    return ret;
}

#define LOAD_GL_FUNC(name)  {   void * funcAddrs = NULL;              \
                funcAddrs = (void *)getGLFuncAddress(#name);          \
                if(funcAddrs)                                         \
                    *(void**)(&name) = funcAddrs;                     \
                else                          \
                    fprintf(stderr,"could not load func %s\n",#name); }

GLDispatch::GLDispatch():m_isLoaded(false){};


void GLDispatch::dispatchFuncs() {
    android::Mutex::Autolock mutex(m_lock);
    if(m_isLoaded)
        return;
    LOAD_GL_FUNC(glActiveTexture);
    LOAD_GL_FUNC(glAlphaFunc);
    LOAD_GL_FUNC(glBegin);
    LOAD_GL_FUNC(glBindBuffer);
    LOAD_GL_FUNC(glBindTexture);
    LOAD_GL_FUNC(glBlendFunc);
    LOAD_GL_FUNC(glBufferData);
    LOAD_GL_FUNC(glBufferSubData);
    LOAD_GL_FUNC(glClear);
    LOAD_GL_FUNC(glClearColor);
    LOAD_GL_FUNC(glClearDepth);
    LOAD_GL_FUNC(glClearStencil);
    LOAD_GL_FUNC(glClientActiveTexture);
    LOAD_GL_FUNC(glClipPlane);
    LOAD_GL_FUNC(glColor4d);
    LOAD_GL_FUNC(glColor4f);
    LOAD_GL_FUNC(glColor4fv);
    LOAD_GL_FUNC(glColor4ub);
    LOAD_GL_FUNC(glColor4ubv);
    LOAD_GL_FUNC(glColorMask);
    LOAD_GL_FUNC(glColorPointer);
    LOAD_GL_FUNC(glCompressedTexImage2D);
    LOAD_GL_FUNC(glCompressedTexSubImage2D);
    LOAD_GL_FUNC(glCopyTexImage2D);
    LOAD_GL_FUNC(glCopyTexSubImage2D);
    LOAD_GL_FUNC(glCullFace);
    LOAD_GL_FUNC(glDeleteBuffers);
    LOAD_GL_FUNC(glDeleteTextures);
    LOAD_GL_FUNC(glDepthFunc);
    LOAD_GL_FUNC(glDepthMask);
    LOAD_GL_FUNC(glDepthRange);
    LOAD_GL_FUNC(glDisable);
    LOAD_GL_FUNC(glDisableClientState);
    LOAD_GL_FUNC(glDrawArrays);
    LOAD_GL_FUNC(glDrawElements);
    LOAD_GL_FUNC(glEnable);
    LOAD_GL_FUNC(glEnableClientState);
    LOAD_GL_FUNC(glEnd);
    LOAD_GL_FUNC(glFinish);
    LOAD_GL_FUNC(glFlush);
    LOAD_GL_FUNC(glFogf);
    LOAD_GL_FUNC(glFogfv);
    LOAD_GL_FUNC(glFrontFace);
    LOAD_GL_FUNC(glFrustum);
    LOAD_GL_FUNC(glGenBuffers);
    LOAD_GL_FUNC(glGenTextures);
    LOAD_GL_FUNC(glGetBooleanv);
    LOAD_GL_FUNC(glGetBufferParameteriv);
    LOAD_GL_FUNC(glGetClipPlane);
    LOAD_GL_FUNC(glGetDoublev);
    LOAD_GL_FUNC(glGetError);
    LOAD_GL_FUNC(glGetFloatv);
    LOAD_GL_FUNC(glGetIntegerv);
    LOAD_GL_FUNC(glGetLightfv);
    LOAD_GL_FUNC(glGetMaterialfv);
    LOAD_GL_FUNC(glGetPointerv);
    LOAD_GL_FUNC(glGetString);
    LOAD_GL_FUNC(glGetTexEnvfv);
    LOAD_GL_FUNC(glGetTexEnviv);
    LOAD_GL_FUNC(glGetTexParameterfv);
    LOAD_GL_FUNC(glGetTexParameteriv);
    LOAD_GL_FUNC(glHint);
    LOAD_GL_FUNC(glIsBuffer);
    LOAD_GL_FUNC(glIsEnabled);
    LOAD_GL_FUNC(glIsTexture);
    LOAD_GL_FUNC(glLightf);
    LOAD_GL_FUNC(glLightfv);
    LOAD_GL_FUNC(glLightModelf);
    LOAD_GL_FUNC(glLightModelfv);
    LOAD_GL_FUNC(glLineWidth);
    LOAD_GL_FUNC(glLoadIdentity);
    LOAD_GL_FUNC(glLoadMatrixf);
    LOAD_GL_FUNC(glLogicOp);
    LOAD_GL_FUNC(glMaterialf);
    LOAD_GL_FUNC(glMaterialfv);
    LOAD_GL_FUNC(glMultiTexCoord2fv);
    LOAD_GL_FUNC(glMultiTexCoord2sv);
    LOAD_GL_FUNC(glMultiTexCoord3fv);
    LOAD_GL_FUNC(glMultiTexCoord3sv);
    LOAD_GL_FUNC(glMultiTexCoord4fv);
    LOAD_GL_FUNC(glMultiTexCoord4sv);
    LOAD_GL_FUNC(glMultiTexCoord4f);
    LOAD_GL_FUNC(glMultMatrixf);
    LOAD_GL_FUNC(glNormal3f);
    LOAD_GL_FUNC(glNormal3fv);
    LOAD_GL_FUNC(glNormal3sv);
    LOAD_GL_FUNC(glOrtho);
    LOAD_GL_FUNC(glPointParameterf);
    LOAD_GL_FUNC(glPointParameterfv);
    LOAD_GL_FUNC(glPointSize);
    LOAD_GL_FUNC(glPolygonOffset);
    LOAD_GL_FUNC(glRotatef);
    LOAD_GL_FUNC(glScalef);
    LOAD_GL_FUNC(glTexEnvf);
    LOAD_GL_FUNC(glTexEnvfv);
    LOAD_GL_FUNC(glTexParameterf);
    LOAD_GL_FUNC(glTexParameterfv);
    LOAD_GL_FUNC(glMatrixMode);
    LOAD_GL_FUNC(glNormalPointer);
    LOAD_GL_FUNC(glPixelStorei);
    LOAD_GL_FUNC(glPopMatrix);
    LOAD_GL_FUNC(glPushMatrix);
    LOAD_GL_FUNC(glReadPixels);
    LOAD_GL_FUNC(glSampleCoverage);
    LOAD_GL_FUNC(glScissor);
    LOAD_GL_FUNC(glShadeModel);
    LOAD_GL_FUNC(glStencilFunc);
    LOAD_GL_FUNC(glStencilMask);
    LOAD_GL_FUNC(glStencilOp);
    LOAD_GL_FUNC(glTexCoordPointer);
    LOAD_GL_FUNC(glTexEnvi);
    LOAD_GL_FUNC(glTexEnviv);
    LOAD_GL_FUNC(glTexImage2D);
    LOAD_GL_FUNC(glTexParameteri);
    LOAD_GL_FUNC(glTexParameteriv);
    LOAD_GL_FUNC(glTexSubImage2D);
    LOAD_GL_FUNC(glTranslatef);
    LOAD_GL_FUNC(glVertexPointer);
    LOAD_GL_FUNC(glViewport);

    m_isLoaded = true;
}
