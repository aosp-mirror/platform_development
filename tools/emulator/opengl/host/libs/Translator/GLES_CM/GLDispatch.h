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
#ifndef GLDISPATCHH
#define GLDISPATCHH

#include <GLES/gl.h>
#include <utils/threads.h>

#define GLAPIENTRY GL_APIENTRY

typedef double      GLclampd;   /* double precision float in [0,1] */
typedef double      GLdouble;   /* double precision float */

class GLDispatch
{
public:

    GLDispatch();
    void dispatchFuncs();

    void (GLAPIENTRY *glActiveTexture) ( GLenum texture );
    void (GLAPIENTRY *glAlphaFunc) (GLenum func, GLclampf ref);
    void (GLAPIENTRY *glBegin)( GLenum mode );
    void (GLAPIENTRY *glBindBuffer) (GLenum target, GLuint buffer);
    void (GLAPIENTRY *glBindTexture) (GLenum target, GLuint texture);
    void (GLAPIENTRY *glBlendFunc) (GLenum sfactor, GLenum dfactor);
    void (GLAPIENTRY *glBufferData) (GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage);
    void (GLAPIENTRY *glBufferSubData) (GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data);
    void (GLAPIENTRY *glClear) (GLbitfield mask);
    void (GLAPIENTRY *glClearColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
    void (GLAPIENTRY *glClearDepth) (GLclampd depth);
    void (GLAPIENTRY *glClearStencil) (GLint s);
    void (GLAPIENTRY *glClientActiveTexture) ( GLenum texture );
    void (GLAPIENTRY *glClipPlane) (GLenum plane, const GLdouble *equation);
    void (GLAPIENTRY *glColor4d) (GLdouble red, GLdouble green, GLdouble blue, GLdouble alpha);
    void (GLAPIENTRY *glColor4f) (GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);
    void (GLAPIENTRY *glColor4fv) ( const GLfloat *v );
    void (GLAPIENTRY *glColor4ub) (GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha);
    void (GLAPIENTRY *glColor4ubv) ( const GLubyte *v );
    void (GLAPIENTRY *glColorMask) (GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
    void (GLAPIENTRY *glColorPointer) (GLint size, GLenum type, GLsizei stride, const GLvoid *pointer);
    void (GLAPIENTRY *glCompressedTexImage2D) ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data );
    void (GLAPIENTRY *glCompressedTexSubImage2D) ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid *data );
    void (GLAPIENTRY *glCopyTexImage2D) (GLenum target, GLint level, GLenum internalFormat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border);
    void (GLAPIENTRY *glCopyTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height);
    void (GLAPIENTRY *glCullFace) (GLenum mode);
    void (GLAPIENTRY *glDeleteBuffers) (GLsizei n, const GLuint *buffers);
    void (GLAPIENTRY *glDeleteTextures) (GLsizei n, const GLuint *textures);
    void (GLAPIENTRY *glDepthFunc) (GLenum func);
    void (GLAPIENTRY *glDepthMask) (GLboolean flag);
    void (GLAPIENTRY *glDepthRange) (GLclampd zNear, GLclampd zFar);
    void (GLAPIENTRY *glDisable) (GLenum cap);
    void (GLAPIENTRY *glDisableClientState) (GLenum array);
    void (GLAPIENTRY *glDrawArrays) (GLenum mode, GLint first, GLsizei count);
    void (GLAPIENTRY *glDrawElements) (GLenum mode, GLsizei count, GLenum type, const GLvoid *indices);
    void (GLAPIENTRY *glEnable) (GLenum cap);
    void (GLAPIENTRY *glEnableClientState) (GLenum array);
    void (GLAPIENTRY *glEnd) ( void );
    void (GLAPIENTRY *glFinish) (void);
    void (GLAPIENTRY *glFlush) (void);
    void (GLAPIENTRY *glFogf) (GLenum pname, GLfloat param);
    void (GLAPIENTRY *glFogfv) (GLenum pname, const GLfloat *params);
    void (GLAPIENTRY *glFrontFace) (GLenum mode);
    void (GLAPIENTRY *glFrustum) (GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar);
    void (GLAPIENTRY *glGenBuffers) (GLsizei n, GLuint *buffers);
    void (GLAPIENTRY *glGenTextures) (GLsizei n, GLuint *textures);
    void (GLAPIENTRY *glGetBooleanv) (GLenum pname, GLboolean *params);
    void (GLAPIENTRY *glGetBufferParameteriv) (GLenum, GLenum, GLint *);
    void (GLAPIENTRY *glGetClipPlane) (GLenum plane, GLdouble *equation);
    void (GLAPIENTRY *glGetDoublev) ( GLenum pname, GLdouble *params );
    GLenum (GLAPIENTRY *glGetError) (void);
    void (GLAPIENTRY *glGetFloatv) (GLenum pname, GLfloat *params);
    void (GLAPIENTRY *glGetIntegerv) (GLenum pname, GLint *params);
    void (GLAPIENTRY *glGetLightfv) (GLenum light, GLenum pname, GLfloat *params);
    void (GLAPIENTRY *glGetMaterialfv) (GLenum face, GLenum pname, GLfloat *params);
    void (GLAPIENTRY *glGetPointerv) (GLenum pname, GLvoid* *params);
    const GLubyte * (GLAPIENTRY *glGetString) (GLenum name);
    void (GLAPIENTRY *glGetTexEnvfv) (GLenum target, GLenum pname, GLfloat *params);
    void (GLAPIENTRY *glGetTexEnviv) (GLenum target, GLenum pname, GLint *params);
    void (GLAPIENTRY *glGetTexParameterfv) (GLenum target, GLenum pname, GLfloat *params);
    void (GLAPIENTRY *glGetTexParameteriv) (GLenum target, GLenum pname, GLint *params);
    void (GLAPIENTRY *glHint) (GLenum target, GLenum mode);
    GLboolean (GLAPIENTRY *glIsBuffer) (GLuint);
    GLboolean (GLAPIENTRY *glIsEnabled) (GLenum cap);
    GLboolean (GLAPIENTRY *glIsTexture) (GLuint texture);
    void (GLAPIENTRY *glLightf) (GLenum light, GLenum pname, GLfloat param);
    void (GLAPIENTRY *glLightfv) (GLenum light, GLenum pname, const GLfloat *params);
    void (GLAPIENTRY *glLightModelf) (GLenum pname, GLfloat param);
    void (GLAPIENTRY *glLightModelfv) (GLenum pname, const GLfloat *params);
    void (GLAPIENTRY *glLineWidth) (GLfloat width);
    void (GLAPIENTRY *glLoadIdentity) (void);
    void (GLAPIENTRY *glLoadMatrixf) (const GLfloat *m);
    void (GLAPIENTRY *glLogicOp) (GLenum opcode);
    void (GLAPIENTRY *glMaterialf) (GLenum face, GLenum pname, GLfloat param);
    void (GLAPIENTRY *glMaterialfv) (GLenum face, GLenum pname, const GLfloat *params);
    void (GLAPIENTRY *glMultiTexCoord2fv) ( GLenum target, const GLfloat *v );
    void (GLAPIENTRY *glMultiTexCoord2sv) ( GLenum target, const GLshort *v );
    void (GLAPIENTRY *glMultiTexCoord3fv) ( GLenum target, const GLfloat *v );
    void (GLAPIENTRY *glMultiTexCoord3sv) ( GLenum target, const GLshort *v );
    void (GLAPIENTRY *glMultiTexCoord4f) ( GLenum target, GLfloat s, GLfloat t, GLfloat r, GLfloat q );
    void (GLAPIENTRY *glMultiTexCoord4fv) ( GLenum target, const GLfloat *v );
    void (GLAPIENTRY *glMultiTexCoord4sv) ( GLenum target, const GLshort *v );
    void (GLAPIENTRY *glMultMatrixf) (const GLfloat *m);
    void (GLAPIENTRY *glNormal3f) (GLfloat nx, GLfloat ny, GLfloat nz);
    void (GLAPIENTRY *glNormal3fv) ( const GLfloat *v );
    void (GLAPIENTRY *glNormal3sv) ( const GLshort *v );
    void (GLAPIENTRY *glOrtho) (GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar);
    void (GLAPIENTRY *glPointParameterf) (GLenum, GLfloat);
    void (GLAPIENTRY *glPointParameterfv) (GLenum, const GLfloat *);
    void (GLAPIENTRY *glPointSize) (GLfloat size);
    void (GLAPIENTRY *glPolygonOffset) (GLfloat factor, GLfloat units);
    void (GLAPIENTRY *glRotatef) (GLfloat angle, GLfloat x, GLfloat y, GLfloat z);
    void (GLAPIENTRY *glScalef) (GLfloat x, GLfloat y, GLfloat z);
    void (GLAPIENTRY *glTexEnvf) (GLenum target, GLenum pname, GLfloat param);
    void (GLAPIENTRY *glTexEnvfv) (GLenum target, GLenum pname, const GLfloat *params);
    void (GLAPIENTRY *glTexParameterf) (GLenum target, GLenum pname, GLfloat param);
    void (GLAPIENTRY *glTexParameterfv) (GLenum target, GLenum pname, const GLfloat *params);
    void (GLAPIENTRY *glMatrixMode) (GLenum mode);
    void (GLAPIENTRY *glNormalPointer) (GLenum type, GLsizei stride, const GLvoid *pointer);
    void (GLAPIENTRY *glPixelStorei) (GLenum pname, GLint param);
    void (GLAPIENTRY *glPopMatrix) (void);
    void (GLAPIENTRY *glPushMatrix) (void);
    void (GLAPIENTRY *glReadPixels) (GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid *pixels);
    void (GLAPIENTRY *glSampleCoverage) ( GLclampf value, GLboolean invert );
    void (GLAPIENTRY *glScissor) (GLint x, GLint y, GLsizei width, GLsizei height);
    void (GLAPIENTRY *glShadeModel) (GLenum mode);
    void (GLAPIENTRY *glStencilFunc) (GLenum func, GLint ref, GLuint mask);
    void (GLAPIENTRY *glStencilMask) (GLuint mask);
    void (GLAPIENTRY *glStencilOp) (GLenum fail, GLenum zfail, GLenum zpass);
    void (GLAPIENTRY *glTexCoordPointer) (GLint size, GLenum type, GLsizei stride, const GLvoid *pointer);
    void (GLAPIENTRY *glTexEnvi) (GLenum target, GLenum pname, GLint param);
    void (GLAPIENTRY *glTexEnviv) (GLenum target, GLenum pname, const GLint *params);
    void (GLAPIENTRY *glTexImage2D) (GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid *pixels);
    void (GLAPIENTRY *glTexParameteri) (GLenum target, GLenum pname, GLint param);
    void (GLAPIENTRY *glTexParameteriv) (GLenum target, GLenum pname, const GLint *params);
    void (GLAPIENTRY *glTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels);
    void (GLAPIENTRY *glTranslatef) (GLfloat x, GLfloat y, GLfloat z);
    void (GLAPIENTRY *glVertexPointer) (GLint size, GLenum type, GLsizei stride, const GLvoid *pointer);
    void (GLAPIENTRY *glViewport) (GLint x, GLint y, GLsizei width, GLsizei height);
private:
    bool             m_isLoaded;
    android::Mutex   m_lock;
};

#endif
