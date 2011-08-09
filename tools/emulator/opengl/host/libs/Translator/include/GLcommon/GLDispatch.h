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
#include <GLES2/gl2.h>
#include <utils/threads.h>
#include "gldefs.h"
#include "GLutils.h"

#define GLAPIENTRY GL_APIENTRY
typedef void(*FUNCPTR)();

class GLDispatch
{
public:

    GLDispatch();
    void dispatchFuncs(GLESVersion version);

    /* OpenGL functions which are needed for implementing BOTH GLES 1.1 & GLES 2.0*/
    static void (GLAPIENTRY *glActiveTexture) ( GLenum texture );
    static void (GLAPIENTRY *glBindBuffer) (GLenum target, GLuint buffer);
    static void (GLAPIENTRY *glBindTexture) (GLenum target, GLuint texture);
    static void (GLAPIENTRY *glBlendFunc) (GLenum sfactor, GLenum dfactor);
    static void (GLAPIENTRY *glBlendEquation)( GLenum mode );
    static void (GLAPIENTRY *glBlendEquationSeparate)(GLenum modeRGB, GLenum modeAlpha);
    static void (GLAPIENTRY *glBlendFuncSeparate)(GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha);
    static void (GLAPIENTRY *glBufferData) (GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage);
    static void (GLAPIENTRY *glBufferSubData) (GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data);
    static void (GLAPIENTRY *glClear) (GLbitfield mask);
    static void (GLAPIENTRY *glClearColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
    static void (GLAPIENTRY *glClearStencil) (GLint s);
    static void (GLAPIENTRY *glColorMask) (GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
    static void (GLAPIENTRY *glCompressedTexImage2D) ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data );
    static void (GLAPIENTRY *glCompressedTexSubImage2D) ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid *data );
    static void (GLAPIENTRY *glCopyTexImage2D) (GLenum target, GLint level, GLenum internalFormat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border);
    static void (GLAPIENTRY *glCopyTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height);
    static void (GLAPIENTRY *glCullFace) (GLenum mode);
    static void (GLAPIENTRY *glDeleteBuffers) (GLsizei n, const GLuint *buffers);
    static void (GLAPIENTRY *glDeleteTextures) (GLsizei n, const GLuint *textures);
    static void (GLAPIENTRY *glDepthFunc) (GLenum func);
    static void (GLAPIENTRY *glDepthMask) (GLboolean flag);
    static void (GLAPIENTRY *glDepthRange) (GLclampd zNear, GLclampd zFar);
    static void (GLAPIENTRY *glDisable) (GLenum cap);
    static void (GLAPIENTRY *glDrawArrays) (GLenum mode, GLint first, GLsizei count);
    static void (GLAPIENTRY *glDrawElements) (GLenum mode, GLsizei count, GLenum type, const GLvoid *indices);
    static void (GLAPIENTRY *glEnable) (GLenum cap);
    static void (GLAPIENTRY *glFinish) (void);
    static void (GLAPIENTRY *glFlush) (void);
    static void (GLAPIENTRY *glFrontFace) (GLenum mode);
    static void (GLAPIENTRY *glGenBuffers) (GLsizei n, GLuint *buffers);
    static void (GLAPIENTRY *glGenTextures) (GLsizei n, GLuint *textures);
    static void (GLAPIENTRY *glGetBooleanv) (GLenum pname, GLboolean *params);
    static void (GLAPIENTRY *glGetBufferParameteriv) (GLenum, GLenum, GLint *);
    static GLenum (GLAPIENTRY *glGetError) (void);
    static void (GLAPIENTRY *glGetFloatv) (GLenum pname, GLfloat *params);
    static void (GLAPIENTRY *glGetIntegerv) (GLenum pname, GLint *params);
    static const GLubyte * (GLAPIENTRY *glGetString) (GLenum name);
    static void (GLAPIENTRY *glGetTexParameterfv) (GLenum target, GLenum pname, GLfloat *params);
    static void (GLAPIENTRY *glGetTexParameteriv) (GLenum target, GLenum pname, GLint *params);
    static void (GLAPIENTRY *glGetTexLevelParameteriv) (GLenum target, GLint level, GLenum pname, GLint *params);
    static void (GLAPIENTRY *glHint) (GLenum target, GLenum mode);
    static GLboolean (GLAPIENTRY *glIsBuffer) (GLuint);
    static GLboolean (GLAPIENTRY *glIsEnabled) (GLenum cap);
    static GLboolean (GLAPIENTRY *glIsTexture) (GLuint texture);
    static void (GLAPIENTRY *glLineWidth) (GLfloat width);
    static void (GLAPIENTRY *glPolygonOffset) (GLfloat factor, GLfloat units);
    static void (GLAPIENTRY *glPixelStorei) (GLenum pname, GLint param);
    static void (GLAPIENTRY *glReadPixels) (GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid *pixels);
    static void (GLAPIENTRY *glSampleCoverage) ( GLclampf value, GLboolean invert );
    static void (GLAPIENTRY *glScissor) (GLint x, GLint y, GLsizei width, GLsizei height);
    static void (GLAPIENTRY *glStencilFunc) (GLenum func, GLint ref, GLuint mask);
    static void (GLAPIENTRY *glStencilMask) (GLuint mask);
    static void (GLAPIENTRY *glStencilOp) (GLenum fail, GLenum zfail, GLenum zpass);
    static void (GLAPIENTRY *glTexImage2D) (GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid *pixels);
    static void (GLAPIENTRY *glTexParameteri) (GLenum target, GLenum pname, GLint param);
    static void (GLAPIENTRY *glTexParameteriv) (GLenum target, GLenum pname, const GLint *params);
    static void (GLAPIENTRY *glTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels);
    static void (GLAPIENTRY *glViewport) (GLint x, GLint y, GLsizei width, GLsizei height);
    static void (GLAPIENTRY *glPushAttrib) ( GLbitfield mask );
    static void (GLAPIENTRY *glPopAttrib) ( void );
    static void (GLAPIENTRY *glPushClientAttrib) ( GLbitfield mask );
    static void (GLAPIENTRY *glPopClientAttrib) ( void );
    static GLboolean (GLAPIENTRY *glIsRenderbufferEXT) (GLuint renderbuffer);
    static void (GLAPIENTRY *glBindRenderbufferEXT) (GLenum target, GLuint renderbuffer);
    static void (GLAPIENTRY *glDeleteRenderbuffersEXT) (GLsizei n, const GLuint *renderbuffers);
    static void (GLAPIENTRY *glGenRenderbuffersEXT) (GLsizei n, GLuint *renderbuffers);
    static void (GLAPIENTRY *glRenderbufferStorageEXT) (GLenum target, GLenum internalformat, GLsizei width, GLsizei height);
    static void (GLAPIENTRY *glGetRenderbufferParameterivEXT) (GLenum target, GLenum pname, GLint *params);
    static GLboolean (GLAPIENTRY *glIsFramebufferEXT) (GLuint framebuffer);
    static void (GLAPIENTRY *glBindFramebufferEXT) (GLenum target, GLuint framebuffer);
    static void (GLAPIENTRY *glDeleteFramebuffersEXT) (GLsizei n, const GLuint *framebuffers);
    static void (GLAPIENTRY *glGenFramebuffersEXT) (GLsizei n, GLuint *framebuffers);
    static GLenum (GLAPIENTRY *glCheckFramebufferStatusEXT) (GLenum target);
    static void (GLAPIENTRY *glFramebufferTexture1DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
    static void (GLAPIENTRY *glFramebufferTexture2DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
    static void (GLAPIENTRY *glFramebufferTexture3DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint zoffset);
    static void (GLAPIENTRY *glFramebufferRenderbufferEXT) (GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer);
    static void (GLAPIENTRY *glGetFramebufferAttachmentParameterivEXT) (GLenum target, GLenum attachment, GLenum pname, GLint *params);
    static void (GLAPIENTRY *glGenerateMipmapEXT) (GLenum target);

    /* OpenGL functions which are needed ONLY for implementing GLES 1.1*/
    static void (GLAPIENTRY *glAlphaFunc) (GLenum func, GLclampf ref);
    static void (GLAPIENTRY *glBegin)( GLenum mode );
    static void (GLAPIENTRY *glClearDepth) (GLclampd depth);
    static void (GLAPIENTRY *glClientActiveTexture) ( GLenum texture );
    static void (GLAPIENTRY *glClipPlane) (GLenum plane, const GLdouble *equation);
    static void (GLAPIENTRY *glColor4d) (GLdouble red, GLdouble green, GLdouble blue, GLdouble alpha);
    static void (GLAPIENTRY *glColor4f) (GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha);
    static void (GLAPIENTRY *glColor4fv) ( const GLfloat *v );
    static void (GLAPIENTRY *glColor4ub) (GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha);
    static void (GLAPIENTRY *glColor4ubv) ( const GLubyte *v );
    static void (GLAPIENTRY *glColorPointer) (GLint size, GLenum type, GLsizei stride, const GLvoid *pointer);
    static void (GLAPIENTRY *glDisableClientState) (GLenum array);
    static void (GLAPIENTRY *glEnableClientState) (GLenum array);
    static void (GLAPIENTRY *glEnd) (void);
    static void (GLAPIENTRY *glFogf) (GLenum pname, GLfloat param);
    static void (GLAPIENTRY *glFogfv) (GLenum pname, const GLfloat *params);
    static void (GLAPIENTRY *glFrustum) (GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar);
    static void (GLAPIENTRY *glGetClipPlane) (GLenum plane, GLdouble *equation);
    static void (GLAPIENTRY *glGetDoublev) ( GLenum pname, GLdouble *params );
    static void (GLAPIENTRY *glGetLightfv) (GLenum light, GLenum pname, GLfloat *params);
    static void (GLAPIENTRY *glGetMaterialfv) (GLenum face, GLenum pname, GLfloat *params);
    static void (GLAPIENTRY *glGetPointerv) (GLenum pname, GLvoid* *params);
    static void (GLAPIENTRY *glGetTexEnvfv) (GLenum target, GLenum pname, GLfloat *params);
    static void (GLAPIENTRY *glGetTexEnviv) (GLenum target, GLenum pname, GLint *params);
    static void (GLAPIENTRY *glLightf) (GLenum light, GLenum pname, GLfloat param);
    static void (GLAPIENTRY *glLightfv) (GLenum light, GLenum pname, const GLfloat *params);
    static void (GLAPIENTRY *glLightModelf) (GLenum pname, GLfloat param);
    static void (GLAPIENTRY *glLightModelfv) (GLenum pname, const GLfloat *params);
    static void (GLAPIENTRY *glLoadIdentity) (void);
    static void (GLAPIENTRY *glLoadMatrixf) (const GLfloat *m);
    static void (GLAPIENTRY *glLogicOp) (GLenum opcode);
    static void (GLAPIENTRY *glMaterialf) (GLenum face, GLenum pname, GLfloat param);
    static void (GLAPIENTRY *glMaterialfv) (GLenum face, GLenum pname, const GLfloat *params);
    static void (GLAPIENTRY *glMultiTexCoord2fv) ( GLenum target, const GLfloat *v );
    static void (GLAPIENTRY *glMultiTexCoord2sv) ( GLenum target, const GLshort *v );
    static void (GLAPIENTRY *glMultiTexCoord3fv) ( GLenum target, const GLfloat *v );
    static void (GLAPIENTRY *glMultiTexCoord3sv) ( GLenum target, const GLshort *v );
    static void (GLAPIENTRY *glMultiTexCoord4f) ( GLenum target, GLfloat s, GLfloat t, GLfloat r, GLfloat q );
    static void (GLAPIENTRY *glMultiTexCoord4fv) ( GLenum target, const GLfloat *v );
    static void (GLAPIENTRY *glMultiTexCoord4sv) ( GLenum target, const GLshort *v );
    static void (GLAPIENTRY *glMultMatrixf) (const GLfloat *m);
    static void (GLAPIENTRY *glNormal3f) (GLfloat nx, GLfloat ny, GLfloat nz);
    static void (GLAPIENTRY *glNormal3fv) ( const GLfloat *v );
    static void (GLAPIENTRY *glNormal3sv) ( const GLshort *v );
    static void (GLAPIENTRY *glOrtho) (GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar);
    static void (GLAPIENTRY *glPointParameterf) (GLenum, GLfloat);
    static void (GLAPIENTRY *glPointParameterfv) (GLenum, const GLfloat *);
    static void (GLAPIENTRY *glPointSize) (GLfloat size);
    static void (GLAPIENTRY *glRotatef) (GLfloat angle, GLfloat x, GLfloat y, GLfloat z);
    static void (GLAPIENTRY *glScalef) (GLfloat x, GLfloat y, GLfloat z);
    static void (GLAPIENTRY *glTexEnvf) (GLenum target, GLenum pname, GLfloat param);
    static void (GLAPIENTRY *glTexEnvfv) (GLenum target, GLenum pname, const GLfloat *params);
    static void (GLAPIENTRY *glTexParameterf) (GLenum target, GLenum pname, GLfloat param);
    static void (GLAPIENTRY *glTexParameterfv) (GLenum target, GLenum pname, const GLfloat *params);
    static void (GLAPIENTRY *glMatrixMode) (GLenum mode);
    static void (GLAPIENTRY *glNormalPointer) (GLenum type, GLsizei stride, const GLvoid *pointer);
    static void (GLAPIENTRY *glPopMatrix) (void);
    static void (GLAPIENTRY *glPushMatrix) (void);
    static void (GLAPIENTRY *glShadeModel) (GLenum mode);
    static void (GLAPIENTRY *glTexCoordPointer) (GLint size, GLenum type, GLsizei stride, const GLvoid *pointer);
    static void (GLAPIENTRY *glTexEnvi) (GLenum target, GLenum pname, GLint param);
    static void (GLAPIENTRY *glTexEnviv) (GLenum target, GLenum pname, const GLint *params);
    static void (GLAPIENTRY *glTranslatef) (GLfloat x, GLfloat y, GLfloat z);
    static void (GLAPIENTRY *glVertexPointer) (GLint size, GLenum type, GLsizei stride, const GLvoid *pointer);

    /* OpenGL functions which are needed ONLY for implementing GLES 1.1 EXTENSIONS*/
    static void (GLAPIENTRY *glCurrentPaletteMatrixARB) (GLint index);
    static void (GLAPIENTRY *glMatrixIndexuivARB) (GLint size, GLuint * indices);
    static void (GLAPIENTRY *glMatrixIndexPointerARB) (GLint size, GLenum type, GLsizei stride, const GLvoid* pointer);
    static void (GLAPIENTRY *glWeightPointerARB) (GLint size, GLenum type, GLsizei stride, const GLvoid* pointer);
    static void (GLAPIENTRY *glTexGenf) (GLenum coord, GLenum pname, GLfloat param );
    static void (GLAPIENTRY *glTexGeni) (GLenum coord, GLenum pname, GLint param );
    static void (GLAPIENTRY *glTexGenfv) (GLenum coord, GLenum pname, const GLfloat *params );
    static void (GLAPIENTRY *glTexGeniv) (GLenum coord, GLenum pname, const GLint *params );
    static void (GLAPIENTRY *glGetTexGenfv) (GLenum coord, GLenum pname, GLfloat *params );
    static void (GLAPIENTRY *glGetTexGeniv) (GLenum coord, GLenum pname, GLint *params );

    /* Loading OpenGL functions which are needed ONLY for implementing GLES 2.0*/
    static void (GL_APIENTRY *glBlendColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
    static void (GL_APIENTRY *glStencilFuncSeparate)(GLenum face, GLenum func, GLint ref, GLuint mask);
    static void (GL_APIENTRY *glStencilMaskSeparate)(GLenum face, GLuint mask);
    static GLboolean (GL_APIENTRY *glIsProgram)(GLuint program);
    static GLboolean (GL_APIENTRY *glIsShader)(GLuint shader);
    static void (GL_APIENTRY *glVertexAttrib1f)(GLuint indx, GLfloat x);
    static void (GL_APIENTRY *glVertexAttrib1fv)(GLuint indx, const GLfloat* values);
    static void (GL_APIENTRY *glVertexAttrib2f)(GLuint indx, GLfloat x, GLfloat y);
    static void (GL_APIENTRY *glVertexAttrib2fv)(GLuint indx, const GLfloat* values);
    static void (GL_APIENTRY *glVertexAttrib3f)(GLuint indx, GLfloat x, GLfloat y, GLfloat z);
    static void (GL_APIENTRY *glVertexAttrib3fv)(GLuint indx, const GLfloat* values);
    static void (GL_APIENTRY *glVertexAttrib4f)(GLuint indx, GLfloat x, GLfloat y, GLfloat z, GLfloat w);
    static void (GL_APIENTRY *glVertexAttrib4fv)(GLuint indx, const GLfloat* values);
    static void (GL_APIENTRY *glVertexAttribPointer)(GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid* ptr);
    static void (GL_APIENTRY *glDisableVertexAttribArray)(GLuint index);
    static void (GL_APIENTRY *glEnableVertexAttribArray)(GLuint index);
    static void (GL_APIENTRY *glGetVertexAttribfv)(GLuint index, GLenum pname, GLfloat* params);
    static void (GL_APIENTRY *glGetVertexAttribiv)(GLuint index, GLenum pname, GLint* params);
    static void (GL_APIENTRY *glGetVertexAttribPointerv)(GLuint index, GLenum pname, GLvoid** pointer);
    static void (GL_APIENTRY *glUniform1f)(GLint location, GLfloat x);
    static void (GL_APIENTRY *glUniform1fv)(GLint location, GLsizei count, const GLfloat* v);
    static void (GL_APIENTRY *glUniform1i)(GLint location, GLint x);
    static void (GL_APIENTRY *glUniform1iv)(GLint location, GLsizei count, const GLint* v);
    static void (GL_APIENTRY *glUniform2f)(GLint location, GLfloat x, GLfloat y);
    static void (GL_APIENTRY *glUniform2fv)(GLint location, GLsizei count, const GLfloat* v);
    static void (GL_APIENTRY *glUniform2i)(GLint location, GLint x, GLint y);
    static void (GL_APIENTRY *glUniform2iv)(GLint location, GLsizei count, const GLint* v);
    static void (GL_APIENTRY *glUniform3f)(GLint location, GLfloat x, GLfloat y, GLfloat z);
    static void (GL_APIENTRY *glUniform3fv)(GLint location, GLsizei count, const GLfloat* v);
    static void (GL_APIENTRY *glUniform3i)(GLint location, GLint x, GLint y, GLint z);
    static void (GL_APIENTRY *glUniform3iv)(GLint location, GLsizei count, const GLint* v);
    static void (GL_APIENTRY *glUniform4f)(GLint location, GLfloat x, GLfloat y, GLfloat z, GLfloat w);
    static void (GL_APIENTRY *glUniform4fv)(GLint location, GLsizei count, const GLfloat* v);
    static void (GL_APIENTRY *glUniform4i)(GLint location, GLint x, GLint y, GLint z, GLint w);
    static void (GL_APIENTRY *glUniform4iv)(GLint location, GLsizei count, const GLint* v);
    static void (GL_APIENTRY *glUniformMatrix2fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value);
    static void (GL_APIENTRY *glUniformMatrix3fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value);
    static void (GL_APIENTRY *glUniformMatrix4fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value);
    static void (GL_APIENTRY *glAttachShader)(GLuint program, GLuint shader);
    static void (GL_APIENTRY *glBindAttribLocation)(GLuint program, GLuint index, const GLchar* name);
    static void (GL_APIENTRY *glCompileShader)(GLuint shader);
    static GLuint (GL_APIENTRY *glCreateProgram)(void);
    static GLuint (GL_APIENTRY *glCreateShader)(GLenum type);
    static void (GL_APIENTRY *glDeleteProgram)(GLuint program);
    static void (GL_APIENTRY *glDeleteShader)(GLuint shader);
    static void (GL_APIENTRY *glDetachShader)(GLuint program, GLuint shader);
    static void (GL_APIENTRY *glLinkProgram)(GLuint program);
    static void (GL_APIENTRY *glUseProgram)(GLuint program);
    static void (GL_APIENTRY *glValidateProgram)(GLuint program);
    static void (GL_APIENTRY *glGetActiveAttrib)(GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name);
    static void (GL_APIENTRY *glGetActiveUniform)(GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name);
    static void (GL_APIENTRY *glGetAttachedShaders)(GLuint program, GLsizei maxcount, GLsizei* count, GLuint* shaders);
    static int  (GL_APIENTRY *glGetAttribLocation)(GLuint program, const GLchar* name);
    static void (GL_APIENTRY *glGetProgramiv)(GLuint program, GLenum pname, GLint* params);
    static void (GL_APIENTRY *glGetProgramInfoLog)(GLuint program, GLsizei bufsize, GLsizei* length, GLchar* infolog);
    static void (GL_APIENTRY *glGetShaderiv)(GLuint shader, GLenum pname, GLint* params);
    static void (GL_APIENTRY *glGetShaderInfoLog)(GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* infolog);
    static void (GL_APIENTRY *glGetShaderPrecisionFormat)(GLenum shadertype, GLenum precisiontype, GLint* range, GLint* precision);
    static void (GL_APIENTRY *glGetShaderSource)(GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* source);
    static void (GL_APIENTRY *glGetUniformfv)(GLuint program, GLint location, GLfloat* params);
    static void (GL_APIENTRY *glGetUniformiv)(GLuint program, GLint location, GLint* params);
    static int  (GL_APIENTRY *glGetUniformLocation)(GLuint program, const GLchar* name);
    static void (GL_APIENTRY *glReleaseShaderCompiler)(void);
    static void (GL_APIENTRY *glShaderBinary)(GLsizei n, const GLuint* shaders, GLenum binaryformat, const GLvoid* binary, GLsizei length);
    static void (GL_APIENTRY *glShaderSource)(GLuint shader, GLsizei count, const GLchar** string, const GLint* length);

private:
    bool                    m_isLoaded;
    static android::Mutex   s_lock;
};

#endif
