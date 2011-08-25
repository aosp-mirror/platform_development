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

#include <GLcommon/GLDispatch.h>
#include <stdio.h>
#include <OpenglOsUtils/osDynLibrary.h>

#ifdef __linux__
#include <GL/glx.h>
#elif defined(WIN32)
#include <windows.h>
#endif

#include "DummyGLfuncs.h"

typedef void (*GL_FUNC_PTR)();

static GL_FUNC_PTR getGLFuncAddress(const char *funcName) {
    GL_FUNC_PTR ret = NULL;
#ifdef __linux__
    static osUtils::dynLibrary* libGL = osUtils::dynLibrary::open("libGL.so");
    ret = (GL_FUNC_PTR)glXGetProcAddress((const GLubyte*)funcName);
#elif defined(WIN32)
    static osUtils::dynLibrary* libGL = osUtils::dynLibrary::open("opengl32");
    ret = (GL_FUNC_PTR)wglGetProcAddress(funcName);
#elif defined(__APPLE__)
    static osUtils::dynLibrary* libGL = osUtils::dynLibrary::open("/System/Library/Frameworks/OpenGL.framework/OpenGL");
#endif
    if(!ret && libGL){
        ret = libGL->findSymbol(funcName);
    }
    return ret;
}

#define LOAD_GL_FUNC(name)  {   void * funcAddrs = NULL;                                    \
                                if(name == NULL){                                           \
                                    funcAddrs = (void *)getGLFuncAddress(#name);            \
                                    if(funcAddrs){                                          \
                                        *(void**)(&name) = funcAddrs;                       \
                                    } else {                                                \
                                        fprintf(stderr,"could not load func %s\n",#name);   \
                                        *(void**)(&name) = (void *)dummy_##name;            \
                                    }                                                       \
                                }                                                           \
                           }

#define LOAD_GLEXT_FUNC(name)  {   void * funcAddrs = NULL;                                \
                                if(name == NULL){                                       \
                                funcAddrs = (void *)getGLFuncAddress(#name);            \
                                if(funcAddrs)                                           \
                                    *(void**)(&name) = funcAddrs;                       \
                                }                                                       \
                           }

/* initializing static GLDispatch members*/

android::Mutex GLDispatch::s_lock;
void (GLAPIENTRY *GLDispatch::glActiveTexture)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glBindBuffer)(GLenum,GLuint) = NULL;
void (GLAPIENTRY *GLDispatch::glBindTexture)(GLenum, GLuint) = NULL;
void (GLAPIENTRY *GLDispatch::glBlendFunc)(GLenum,GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glBlendEquation)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glBlendEquationSeparate)(GLenum,GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glBlendFuncSeparate)(GLenum,GLenum,GLenum,GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glBufferData)(GLenum,GLsizeiptr,const GLvoid *,GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glBufferSubData)(GLenum,GLintptr,GLsizeiptr,const GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glClear)(GLbitfield) = NULL;
void (GLAPIENTRY *GLDispatch::glClearColor)(GLclampf,GLclampf,GLclampf,GLclampf) = NULL;
void (GLAPIENTRY *GLDispatch::glClearStencil)(GLint) = NULL;
void (GLAPIENTRY *GLDispatch::glColorMask)(GLboolean,GLboolean,GLboolean,GLboolean) = NULL;
void (GLAPIENTRY *GLDispatch::glCompressedTexImage2D)(GLenum,GLint,GLenum,GLsizei,GLsizei,GLint,GLsizei, const GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glCompressedTexSubImage2D)(GLenum,GLint,GLint,GLint,GLsizei,GLsizei,GLenum,GLsizei,const GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glCopyTexImage2D)(GLenum,GLint,GLenum,GLint,GLint,GLsizei,GLsizei,GLint) = NULL;
void (GLAPIENTRY *GLDispatch::glCopyTexSubImage2D)(GLenum,GLint,GLint,GLint,GLint,GLint,GLsizei,GLsizei) = NULL;
void (GLAPIENTRY *GLDispatch::glCullFace)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glDeleteBuffers)(GLsizei,const GLuint *) = NULL;
void (GLAPIENTRY *GLDispatch::glDeleteTextures)(GLsizei,const GLuint *) = NULL;
void (GLAPIENTRY *GLDispatch::glDepthFunc)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glDepthMask)(GLboolean) = NULL;
void (GLAPIENTRY *GLDispatch::glDepthRange)(GLclampd,GLclampd) = NULL;
void (GLAPIENTRY *GLDispatch::glDisable)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glDrawArrays)(GLenum,GLint,GLsizei) = NULL;
void (GLAPIENTRY *GLDispatch::glDrawElements)(GLenum,GLsizei,GLenum,const GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glEnable)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glFinish)() = NULL;
void (GLAPIENTRY *GLDispatch::glFlush)() = NULL;
void (GLAPIENTRY *GLDispatch::glFrontFace)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glGenBuffers)(GLsizei,GLuint *) = NULL;
void (GLAPIENTRY *GLDispatch::glGenTextures)(GLsizei,GLuint *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetBooleanv)(GLenum,GLboolean *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetBufferParameteriv)(GLenum, GLenum, GLint *) = NULL;
GLenum (GLAPIENTRY *GLDispatch::glGetError)() = NULL;
void (GLAPIENTRY *GLDispatch::glGetFloatv)(GLenum,GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetIntegerv)(GLenum,GLint *) = NULL;
const GLubyte * (GLAPIENTRY *GLDispatch::glGetString) (GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glGetTexParameterfv)(GLenum,GLenum,GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetTexParameteriv)(GLenum,GLenum,GLint *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetTexLevelParameteriv) (GLenum target, GLint level, GLenum pname, GLint *params) = NULL;
void (GLAPIENTRY *GLDispatch::glHint)(GLenum,GLenum) = NULL;
GLboolean (GLAPIENTRY *GLDispatch::glIsBuffer)(GLuint) = NULL;
GLboolean (GLAPIENTRY *GLDispatch::glIsEnabled)(GLenum) = NULL;
GLboolean (GLAPIENTRY *GLDispatch::glIsTexture)(GLuint) = NULL;
void (GLAPIENTRY *GLDispatch::glLineWidth)(GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glPolygonOffset)(GLfloat, GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glPixelStorei)(GLenum,GLint) = NULL;
void (GLAPIENTRY *GLDispatch::glReadPixels)(GLint,GLint,GLsizei,GLsizei,GLenum,GLenum,GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glSampleCoverage)(GLclampf,GLboolean) = NULL;
void (GLAPIENTRY *GLDispatch::glScissor)(GLint,GLint,GLsizei,GLsizei) = NULL;
void (GLAPIENTRY *GLDispatch::glStencilFunc)(GLenum,GLint,GLuint) = NULL;
void (GLAPIENTRY *GLDispatch::glStencilMask)(GLuint) = NULL;
void (GLAPIENTRY *GLDispatch::glStencilOp)(GLenum, GLenum,GLenum);
void (GLAPIENTRY *GLDispatch::glTexImage2D)(GLenum,GLint,GLint,GLsizei,GLsizei,GLint,GLenum,GLenum,const GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glTexParameterf)(GLenum,GLenum, GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glTexParameterfv)(GLenum,GLenum,const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glTexParameteri)(GLenum,GLenum,GLint) = NULL;
void (GLAPIENTRY *GLDispatch::glTexParameteriv)(GLenum,GLenum,const GLint *) = NULL;
void (GLAPIENTRY *GLDispatch::glTexSubImage2D)(GLenum,GLint,GLint,GLint,GLsizei,GLsizei,GLenum,GLenum,const GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glViewport)(GLint,GLint,GLsizei,GLsizei) = NULL;
void (GLAPIENTRY *GLDispatch::glPushAttrib) ( GLbitfield mask ) = NULL;
void (GLAPIENTRY *GLDispatch::glPopAttrib) ( void ) = NULL;
void (GLAPIENTRY *GLDispatch::glPushClientAttrib) ( GLbitfield mask ) = NULL;
void (GLAPIENTRY *GLDispatch::glPopClientAttrib) ( void ) = NULL;

/*GLES 1.1*/
void (GLAPIENTRY *GLDispatch::glAlphaFunc)(GLenum,GLclampf) = NULL;
void (GLAPIENTRY *GLDispatch::glBegin)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glClearDepth)(GLclampd) = NULL;
void (GLAPIENTRY *GLDispatch::glClientActiveTexture)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glClipPlane)(GLenum,const GLdouble *) = NULL;
void (GLAPIENTRY *GLDispatch::glColor4d)(GLdouble,GLdouble,GLdouble,GLdouble) = NULL;
void (GLAPIENTRY *GLDispatch::glColor4f)(GLfloat,GLfloat,GLfloat,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glColor4fv)(const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glColor4ub)(GLubyte,GLubyte,GLubyte,GLubyte) = NULL;
void (GLAPIENTRY *GLDispatch::glColor4ubv)(const GLubyte *) = NULL;
void (GLAPIENTRY *GLDispatch::glColorPointer)(GLint,GLenum,GLsizei,const GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glDisableClientState)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glEnableClientState)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glEnd)() = NULL;
void (GLAPIENTRY *GLDispatch::glFogf)(GLenum, GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glFogfv)(GLenum,const GLfloat *);
void (GLAPIENTRY *GLDispatch::glFrustum)(GLdouble,GLdouble,GLdouble,GLdouble,GLdouble,GLdouble) = NULL;
void (GLAPIENTRY *GLDispatch::glGetClipPlane)(GLenum,GLdouble *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetDoublev)(GLenum,GLdouble *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetLightfv)(GLenum,GLenum,GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetMaterialfv)(GLenum,GLenum,GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetPointerv)(GLenum,GLvoid**) = NULL;
void (GLAPIENTRY *GLDispatch::glGetTexEnvfv)(GLenum,GLenum,GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glGetTexEnviv)(GLenum,GLenum,GLint *)= NULL;
void (GLAPIENTRY *GLDispatch::glLightf)(GLenum,GLenum,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glLightfv)(GLenum,GLenum,const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glLightModelf)(GLenum,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glLightModelfv)(GLenum,const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glLoadIdentity)() = NULL;
void (GLAPIENTRY *GLDispatch::glLoadMatrixf)(const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glLogicOp)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glMaterialf)(GLenum,GLenum,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glMaterialfv)(GLenum,GLenum,const GLfloat *);
void (GLAPIENTRY *GLDispatch::glMultiTexCoord2fv)(GLenum, const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glMultiTexCoord2sv)(GLenum, const GLshort *) = NULL;
void (GLAPIENTRY *GLDispatch::glMultiTexCoord3fv)(GLenum, const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glMultiTexCoord3sv)(GLenum,const GLshort *) = NULL;
void (GLAPIENTRY *GLDispatch::glMultiTexCoord4f)(GLenum,GLfloat,GLfloat,GLfloat,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glMultiTexCoord4fv)(GLenum,const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glMultiTexCoord4sv)(GLenum,const GLshort *) = NULL;
void (GLAPIENTRY *GLDispatch::glMultMatrixf)(const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glNormal3f)(GLfloat,GLfloat,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glNormal3fv)(const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glNormal3sv)(const GLshort *) = NULL;
void (GLAPIENTRY *GLDispatch::glOrtho)(GLdouble,GLdouble,GLdouble,GLdouble,GLdouble,GLdouble) = NULL;
void (GLAPIENTRY *GLDispatch::glPointParameterf)(GLenum, GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glPointParameterfv)(GLenum, const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glPointSize)(GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glRotatef)(GLfloat,GLfloat,GLfloat,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glScalef)(GLfloat,GLfloat,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glTexEnvf)(GLenum,GLenum,GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glTexEnvfv)(GLenum,GLenum,const GLfloat *) = NULL;
void (GLAPIENTRY *GLDispatch::glMatrixMode)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glNormalPointer)(GLenum,GLsizei,const GLvoid *) = NULL;
void (GLAPIENTRY *GLDispatch::glPopMatrix)() = NULL;
void (GLAPIENTRY *GLDispatch::glPushMatrix)() = NULL;
void (GLAPIENTRY *GLDispatch::glShadeModel)(GLenum) = NULL;
void (GLAPIENTRY *GLDispatch::glTexCoordPointer)(GLint,GLenum, GLsizei, const GLvoid*) = NULL;
void (GLAPIENTRY *GLDispatch::glTexEnvi)(GLenum ,GLenum,GLint) = NULL;
void (GLAPIENTRY *GLDispatch::glTexEnviv)(GLenum, GLenum, const GLint *) = NULL;
void (GLAPIENTRY *GLDispatch::glTranslatef)(GLfloat,GLfloat, GLfloat) = NULL;
void (GLAPIENTRY *GLDispatch::glVertexPointer)(GLint,GLenum,GLsizei, const GLvoid *) = NULL;

/* GLES 1.1 EXTENSIONS*/
GLboolean (GLAPIENTRY *GLDispatch::glIsRenderbufferEXT) (GLuint renderbuffer) = NULL;
void (GLAPIENTRY *GLDispatch::glBindRenderbufferEXT) (GLenum target, GLuint renderbuffer) = NULL;
void (GLAPIENTRY *GLDispatch::glDeleteRenderbuffersEXT) (GLsizei n, const GLuint *renderbuffers) = NULL;
void (GLAPIENTRY *GLDispatch::glGenRenderbuffersEXT) (GLsizei n, GLuint *renderbuffers) = NULL;
void (GLAPIENTRY *GLDispatch::glRenderbufferStorageEXT) (GLenum target, GLenum internalformat, GLsizei width, GLsizei height) = NULL;
void (GLAPIENTRY *GLDispatch::glGetRenderbufferParameterivEXT) (GLenum target, GLenum pname, GLint *params) = NULL;
GLboolean (GLAPIENTRY *GLDispatch::glIsFramebufferEXT) (GLuint framebuffer) = NULL;
void (GLAPIENTRY *GLDispatch::glBindFramebufferEXT) (GLenum target, GLuint framebuffer) = NULL;
void (GLAPIENTRY *GLDispatch::glDeleteFramebuffersEXT) (GLsizei n, const GLuint *framebuffers) = NULL;
void (GLAPIENTRY *GLDispatch::glGenFramebuffersEXT) (GLsizei n, GLuint *framebuffers) = NULL;
GLenum (GLAPIENTRY *GLDispatch::glCheckFramebufferStatusEXT) (GLenum target) = NULL;
void (GLAPIENTRY *GLDispatch::glFramebufferTexture1DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level) = NULL;
void (GLAPIENTRY *GLDispatch::glFramebufferTexture2DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level) = NULL;
void (GLAPIENTRY *GLDispatch::glFramebufferTexture3DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint zoffset) = NULL;
void (GLAPIENTRY *GLDispatch::glFramebufferRenderbufferEXT) (GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer) = NULL;
void (GLAPIENTRY *GLDispatch::glGetFramebufferAttachmentParameterivEXT) (GLenum target, GLenum attachment, GLenum pname, GLint *params) = NULL;
void (GLAPIENTRY *GLDispatch::glGenerateMipmapEXT) (GLenum target) = NULL;
void (GLAPIENTRY *GLDispatch::glCurrentPaletteMatrixARB) (GLint index) = NULL;
void (GLAPIENTRY *GLDispatch::glMatrixIndexuivARB) (GLint size, GLuint * indices) = NULL;
void (GLAPIENTRY *GLDispatch::glMatrixIndexPointerARB) (GLint size, GLenum type, GLsizei stride, const GLvoid* pointer) = NULL;
void (GLAPIENTRY *GLDispatch::glWeightPointerARB) (GLint size, GLenum type, GLsizei stride, const GLvoid* pointer) = NULL;
void (GLAPIENTRY *GLDispatch::glTexGenf) (GLenum coord, GLenum pname, GLfloat param ) = NULL;
void (GLAPIENTRY *GLDispatch::glTexGeni) (GLenum coord, GLenum pname, GLint param ) = NULL;
void (GLAPIENTRY *GLDispatch::glTexGenfv) (GLenum coord, GLenum pname, const GLfloat *params ) = NULL;
void (GLAPIENTRY *GLDispatch::glTexGeniv) (GLenum coord, GLenum pname, const GLint *params ) = NULL;
void (GLAPIENTRY *GLDispatch::glGetTexGenfv) (GLenum coord, GLenum pname, GLfloat *params ) = NULL;
void (GLAPIENTRY *GLDispatch::glGetTexGeniv) (GLenum coord, GLenum pname, GLint *params ) = NULL;

/* GLES 2.0*/
void (GL_APIENTRY *GLDispatch::glBlendColor)(GLclampf,GLclampf,GLclampf,GLclampf) = NULL;
void (GL_APIENTRY *GLDispatch::glStencilFuncSeparate)(GLenum,GLenum,GLint,GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glStencilMaskSeparate)(GLenum,GLuint) = NULL;
GLboolean (GL_APIENTRY *GLDispatch::glIsProgram)(GLuint program) = NULL;
GLboolean (GL_APIENTRY *GLDispatch::glIsShader)(GLuint shader) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttrib1f)(GLuint,GLfloat) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttrib1fv)(GLuint,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttrib2f)(GLuint,GLfloat, GLfloat) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttrib2fv)(GLuint,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttrib3f)(GLuint,GLfloat, GLfloat,GLfloat) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttrib3fv)(GLuint,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttrib4f)(GLuint,GLfloat,GLfloat,GLfloat,GLfloat ) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttrib4fv)(GLuint,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glVertexAttribPointer)(GLuint,GLint,GLenum,GLboolean,GLsizei,const GLvoid*) = NULL;
void (GL_APIENTRY *GLDispatch::glDisableVertexAttribArray)(GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glEnableVertexAttribArray)(GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glGetVertexAttribfv)(GLuint,GLenum,GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetVertexAttribiv)(GLuint,GLenum,GLint*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetVertexAttribPointerv)(GLuint,GLenum,GLvoid**) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform1f)(GLint,GLfloat) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform1fv)(GLint,GLsizei,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform1i)(GLint,GLint) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform1iv)(GLint,GLsizei,const GLint*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform2f)(GLint,GLfloat,GLfloat) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform2fv)(GLint,GLsizei,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform2i)(GLint,GLint,GLint) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform2iv)(GLint ,GLsizei,const GLint*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform3f)(GLint,GLfloat,GLfloat,GLfloat) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform3fv)(GLint,GLsizei,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform3i)(GLint,GLint,GLint,GLint) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform3iv)(GLint,GLsizei,const GLint*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform4f)(GLint,GLfloat,GLfloat,GLfloat,GLfloat) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform4fv)(GLint,GLsizei,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform4i)(GLint,GLint,GLint,GLint,GLint) = NULL;
void (GL_APIENTRY *GLDispatch::glUniform4iv)(GLint,GLsizei,const GLint*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniformMatrix2fv)(GLint,GLsizei,GLboolean,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniformMatrix3fv)(GLint,GLsizei,GLboolean,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glUniformMatrix4fv)(GLint,GLsizei,GLboolean,const GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glAttachShader)(GLuint,GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glBindAttribLocation)(GLuint,GLuint,const GLchar*) = NULL;
void (GL_APIENTRY *GLDispatch::glCompileShader)(GLuint) = NULL;
GLuint (GL_APIENTRY *GLDispatch::glCreateProgram)() = NULL;
GLuint (GL_APIENTRY *GLDispatch::glCreateShader)(GLenum) = NULL;
void (GL_APIENTRY *GLDispatch::glDeleteProgram)(GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glDeleteShader)(GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glDetachShader)(GLuint,GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glLinkProgram)(GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glUseProgram)(GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glValidateProgram)(GLuint) = NULL;
void (GL_APIENTRY *GLDispatch::glGetActiveAttrib)(GLuint,GLuint,GLsizei,GLsizei*,GLint*,GLenum*,GLchar*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetActiveUniform)(GLuint,GLuint,GLsizei,GLsizei*,GLint*,GLenum*,GLchar*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetAttachedShaders)(GLuint,GLsizei,GLsizei*,GLuint*) = NULL;
int  (GL_APIENTRY *GLDispatch::glGetAttribLocation)(GLuint,const GLchar*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetProgramiv)(GLuint,GLenum,GLint*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetProgramInfoLog)(GLuint,GLsizei,GLsizei*,GLchar*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetShaderiv)(GLuint,GLenum,GLint*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetShaderInfoLog)(GLuint,GLsizei,GLsizei*,GLchar*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetShaderPrecisionFormat)(GLenum,GLenum,GLint*,GLint*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetShaderSource)(GLuint,GLsizei,GLsizei*,GLchar*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetUniformfv)(GLuint,GLint,GLfloat*) = NULL;
void (GL_APIENTRY *GLDispatch::glGetUniformiv)(GLuint,GLint,GLint*) = NULL;
int  (GL_APIENTRY *GLDispatch::glGetUniformLocation)(GLuint,const GLchar*) = NULL;
void (GL_APIENTRY *GLDispatch::glReleaseShaderCompiler)() = NULL;
void (GL_APIENTRY *GLDispatch::glShaderBinary)(GLsizei,const GLuint*,GLenum,const GLvoid*,GLsizei) = NULL;
void (GL_APIENTRY *GLDispatch::glShaderSource)(GLuint,GLsizei,const GLchar**,const GLint*) = NULL;

GLDispatch::GLDispatch():m_isLoaded(false){};


void GLDispatch::dispatchFuncs(GLESVersion version){
    android::Mutex::Autolock mutex(s_lock);
    if(m_isLoaded)
        return;

    /* Loading OpenGL functions which are needed for implementing BOTH GLES 1.1 & GLES 2.0*/
    LOAD_GL_FUNC(glActiveTexture);
    LOAD_GL_FUNC(glBindBuffer);
    LOAD_GL_FUNC(glBindTexture);
    LOAD_GL_FUNC(glBlendFunc);
    LOAD_GL_FUNC(glBlendEquation);
    LOAD_GL_FUNC(glBlendEquationSeparate);
    LOAD_GL_FUNC(glBlendFuncSeparate);
    LOAD_GL_FUNC(glBufferData);
    LOAD_GL_FUNC(glBufferSubData);
    LOAD_GL_FUNC(glClear);
    LOAD_GL_FUNC(glClearColor);
    LOAD_GL_FUNC(glClearDepth);
    LOAD_GL_FUNC(glClearStencil);
    LOAD_GL_FUNC(glColorMask);
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
    LOAD_GL_FUNC(glDrawArrays);
    LOAD_GL_FUNC(glDrawElements);
    LOAD_GL_FUNC(glEnable);
    LOAD_GL_FUNC(glFinish);
    LOAD_GL_FUNC(glFlush);
    LOAD_GL_FUNC(glFrontFace);
    LOAD_GL_FUNC(glGenBuffers);
    LOAD_GL_FUNC(glGenTextures);
    LOAD_GL_FUNC(glGetBooleanv);
    LOAD_GL_FUNC(glGetBufferParameteriv);
    LOAD_GL_FUNC(glGetError);
    LOAD_GL_FUNC(glGetFloatv);
    LOAD_GL_FUNC(glGetIntegerv);
    LOAD_GL_FUNC(glGetString);
    LOAD_GL_FUNC(glTexParameterf);
    LOAD_GL_FUNC(glTexParameterfv);
    LOAD_GL_FUNC(glGetTexParameterfv);
    LOAD_GL_FUNC(glGetTexParameteriv);
    LOAD_GL_FUNC(glGetTexLevelParameteriv);
    LOAD_GL_FUNC(glHint);
    LOAD_GL_FUNC(glIsBuffer);
    LOAD_GL_FUNC(glIsEnabled);
    LOAD_GL_FUNC(glIsTexture);
    LOAD_GL_FUNC(glLineWidth);
    LOAD_GL_FUNC(glPolygonOffset);
    LOAD_GL_FUNC(glPixelStorei);
    LOAD_GL_FUNC(glReadPixels);
    LOAD_GL_FUNC(glSampleCoverage);
    LOAD_GL_FUNC(glScissor);
    LOAD_GL_FUNC(glStencilFunc);
    LOAD_GL_FUNC(glStencilMask);
    LOAD_GL_FUNC(glStencilOp);
    LOAD_GL_FUNC(glTexImage2D);
    LOAD_GL_FUNC(glTexParameteri);
    LOAD_GL_FUNC(glTexParameteriv);
    LOAD_GL_FUNC(glTexSubImage2D);
    LOAD_GL_FUNC(glViewport);
    LOAD_GL_FUNC(glPushAttrib);
    LOAD_GL_FUNC(glPushClientAttrib);
    LOAD_GL_FUNC(glPopAttrib);
    LOAD_GL_FUNC(glPopClientAttrib);
    LOAD_GLEXT_FUNC(glIsRenderbufferEXT);
    LOAD_GLEXT_FUNC(glBindRenderbufferEXT);
    LOAD_GLEXT_FUNC(glDeleteRenderbuffersEXT);
    LOAD_GLEXT_FUNC(glGenRenderbuffersEXT);
    LOAD_GLEXT_FUNC(glRenderbufferStorageEXT);
    LOAD_GLEXT_FUNC(glGetRenderbufferParameterivEXT);
    LOAD_GLEXT_FUNC(glIsFramebufferEXT);
    LOAD_GLEXT_FUNC(glBindFramebufferEXT);
    LOAD_GLEXT_FUNC(glDeleteFramebuffersEXT);
    LOAD_GLEXT_FUNC(glGenFramebuffersEXT);
    LOAD_GLEXT_FUNC(glCheckFramebufferStatusEXT);
    LOAD_GLEXT_FUNC(glFramebufferTexture1DEXT);
    LOAD_GLEXT_FUNC(glFramebufferTexture2DEXT);
    LOAD_GLEXT_FUNC(glFramebufferTexture3DEXT);
    LOAD_GLEXT_FUNC(glFramebufferRenderbufferEXT);
    LOAD_GLEXT_FUNC(glGetFramebufferAttachmentParameterivEXT);
    LOAD_GLEXT_FUNC(glGenerateMipmapEXT);

    /* Loading OpenGL functions which are needed ONLY for implementing GLES 1.1*/
    if(version == GLES_1_1){
        LOAD_GL_FUNC(glAlphaFunc);
        LOAD_GL_FUNC(glBegin);
        LOAD_GL_FUNC(glClientActiveTexture);
        LOAD_GL_FUNC(glClipPlane);
        LOAD_GL_FUNC(glColor4d);
        LOAD_GL_FUNC(glColor4f);
        LOAD_GL_FUNC(glColor4fv);
        LOAD_GL_FUNC(glColor4ub);
        LOAD_GL_FUNC(glColor4ubv);
        LOAD_GL_FUNC(glColorPointer);
        LOAD_GL_FUNC(glDisableClientState);
        LOAD_GL_FUNC(glEnableClientState);
        LOAD_GL_FUNC(glEnd);
        LOAD_GL_FUNC(glFogf);
        LOAD_GL_FUNC(glFogfv);
        LOAD_GL_FUNC(glFrustum);
        LOAD_GL_FUNC(glGetClipPlane);
        LOAD_GL_FUNC(glGetDoublev);
        LOAD_GL_FUNC(glGetLightfv);
        LOAD_GL_FUNC(glGetMaterialfv);
        LOAD_GL_FUNC(glGetPointerv);
        LOAD_GL_FUNC(glGetTexEnvfv);
        LOAD_GL_FUNC(glGetTexEnviv);
        LOAD_GL_FUNC(glLightf);
        LOAD_GL_FUNC(glLightfv);
        LOAD_GL_FUNC(glLightModelf);
        LOAD_GL_FUNC(glLightModelfv);
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
        LOAD_GL_FUNC(glRotatef);
        LOAD_GL_FUNC(glScalef);
        LOAD_GL_FUNC(glTexEnvf);
        LOAD_GL_FUNC(glTexEnvfv);
        LOAD_GL_FUNC(glMatrixMode);
        LOAD_GL_FUNC(glNormalPointer);
        LOAD_GL_FUNC(glPopMatrix);
        LOAD_GL_FUNC(glPushMatrix);
        LOAD_GL_FUNC(glShadeModel);
        LOAD_GL_FUNC(glTexCoordPointer);
        LOAD_GL_FUNC(glTexEnvi);
        LOAD_GL_FUNC(glTexEnviv);
        LOAD_GL_FUNC(glTranslatef);
        LOAD_GL_FUNC(glVertexPointer);

        LOAD_GLEXT_FUNC(glCurrentPaletteMatrixARB);
        LOAD_GLEXT_FUNC(glMatrixIndexuivARB);
        LOAD_GLEXT_FUNC(glMatrixIndexPointerARB);
        LOAD_GLEXT_FUNC(glWeightPointerARB);
        LOAD_GLEXT_FUNC(glTexGenf);
        LOAD_GLEXT_FUNC(glTexGeni);
        LOAD_GLEXT_FUNC(glTexGenfv);
        LOAD_GLEXT_FUNC(glTexGeniv);
        LOAD_GLEXT_FUNC(glGetTexGenfv);
        LOAD_GLEXT_FUNC(glGetTexGeniv);

    } else if (version == GLES_2_0){

    /* Loading OpenGL functions which are needed ONLY for implementing GLES 2.0*/

        LOAD_GL_FUNC(glBlendColor);
        LOAD_GL_FUNC(glBlendFuncSeparate);
        LOAD_GL_FUNC(glStencilFuncSeparate);
        LOAD_GL_FUNC(glIsProgram);
        LOAD_GL_FUNC(glIsShader);
        LOAD_GL_FUNC(glVertexAttrib1f);
        LOAD_GL_FUNC(glVertexAttrib1fv);
        LOAD_GL_FUNC(glVertexAttrib2f);
        LOAD_GL_FUNC(glVertexAttrib2fv);
        LOAD_GL_FUNC(glVertexAttrib3f);
        LOAD_GL_FUNC(glVertexAttrib3fv);
        LOAD_GL_FUNC(glVertexAttrib4f);
        LOAD_GL_FUNC(glVertexAttrib4fv);
        LOAD_GL_FUNC(glVertexAttribPointer);
        LOAD_GL_FUNC(glDisableVertexAttribArray);
        LOAD_GL_FUNC(glEnableVertexAttribArray);
        LOAD_GL_FUNC(glGetVertexAttribfv);
        LOAD_GL_FUNC(glGetVertexAttribiv);
        LOAD_GL_FUNC(glGetVertexAttribPointerv);
        LOAD_GL_FUNC(glUniform1f);
        LOAD_GL_FUNC(glUniform1fv);
        LOAD_GL_FUNC(glUniform1i);
        LOAD_GL_FUNC(glUniform1iv);
        LOAD_GL_FUNC(glUniform2f);
        LOAD_GL_FUNC(glUniform2fv);
        LOAD_GL_FUNC(glUniform2i);
        LOAD_GL_FUNC(glUniform2iv);
        LOAD_GL_FUNC(glUniform3f);
        LOAD_GL_FUNC(glUniform3fv);
        LOAD_GL_FUNC(glUniform3i);
        LOAD_GL_FUNC(glUniform3iv);
        LOAD_GL_FUNC(glUniform4f);
        LOAD_GL_FUNC(glUniform4fv);
        LOAD_GL_FUNC(glUniform4i);
        LOAD_GL_FUNC(glUniform4iv);
        LOAD_GL_FUNC(glUniformMatrix2fv);
        LOAD_GL_FUNC(glUniformMatrix3fv);
        LOAD_GL_FUNC(glUniformMatrix4fv);
        LOAD_GL_FUNC(glAttachShader);
        LOAD_GL_FUNC(glBindAttribLocation);
        LOAD_GL_FUNC(glCompileShader);
        LOAD_GL_FUNC(glCreateProgram);
        LOAD_GL_FUNC(glCreateShader);
        LOAD_GL_FUNC(glDeleteProgram);
        LOAD_GL_FUNC(glDeleteShader);
        LOAD_GL_FUNC(glDetachShader);
        LOAD_GL_FUNC(glLinkProgram);
        LOAD_GL_FUNC(glUseProgram);
        LOAD_GL_FUNC(glValidateProgram);
        LOAD_GL_FUNC(glGetActiveAttrib);
        LOAD_GL_FUNC(glGetActiveUniform);
        LOAD_GL_FUNC(glGetAttachedShaders);
        LOAD_GL_FUNC(glGetAttribLocation);
        LOAD_GL_FUNC(glGetProgramiv);
        LOAD_GL_FUNC(glGetProgramInfoLog);
        LOAD_GL_FUNC(glGetShaderiv);
        LOAD_GL_FUNC(glGetShaderInfoLog);
        LOAD_GLEXT_FUNC(glGetShaderPrecisionFormat);
        LOAD_GL_FUNC(glGetShaderSource);
        LOAD_GL_FUNC(glGetUniformfv);
        LOAD_GL_FUNC(glGetUniformiv);
        LOAD_GL_FUNC(glGetUniformLocation);
        LOAD_GLEXT_FUNC(glReleaseShaderCompiler);
        LOAD_GLEXT_FUNC(glShaderBinary);
        LOAD_GL_FUNC(glShaderSource);
        LOAD_GL_FUNC(glStencilMaskSeparate);
    }
    m_isLoaded = true;
}
