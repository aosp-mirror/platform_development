#include "DummyGLfuncs.h"

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

    void GLAPIENTRY dummy_glActiveTexture ( GLenum texture ){}
    void GLAPIENTRY dummy_glBindBuffer (GLenum target, GLuint buffer){}
    void GLAPIENTRY dummy_glBindTexture (GLenum target, GLuint texture){}
    void GLAPIENTRY dummy_glBlendFunc (GLenum sfactor, GLenum dfactor){}
    void GLAPIENTRY dummy_glBlendEquation( GLenum mode ){}
    void GLAPIENTRY dummy_glBlendEquationSeparate(GLenum modeRGB, GLenum modeAlpha){}
    void GLAPIENTRY dummy_glBlendFuncSeparate(GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha){}
    void GLAPIENTRY dummy_glBufferData(GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage){}
    void GLAPIENTRY dummy_glBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data){}
    void GLAPIENTRY dummy_glClear(GLbitfield mask){}
    void GLAPIENTRY dummy_glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha){}
    void GLAPIENTRY dummy_glClearStencil(GLint s){}
    void GLAPIENTRY dummy_glColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha){}
    void GLAPIENTRY dummy_glCompressedTexImage2D( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data ){}
    void GLAPIENTRY dummy_glCompressedTexSubImage2D( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid *data ){}
    void GLAPIENTRY dummy_glCopyTexImage2D(GLenum target, GLint level, GLenum internalFormat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border){}
    void GLAPIENTRY dummy_glCopyTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height){}
    void GLAPIENTRY dummy_glCullFace(GLenum mode){}
    void GLAPIENTRY dummy_glDeleteBuffers(GLsizei n, const GLuint *buffers){}
    void GLAPIENTRY dummy_glDeleteTextures(GLsizei n, const GLuint *textures){}
    void GLAPIENTRY dummy_glDepthFunc(GLenum func){}
    void GLAPIENTRY dummy_glDepthMask(GLboolean flag){}
    void GLAPIENTRY dummy_glDepthRange(GLclampd zNear, GLclampd zFar){}
    void GLAPIENTRY dummy_glDisable(GLenum cap){}
    void GLAPIENTRY dummy_glDrawArrays(GLenum mode, GLint first, GLsizei count){}
    void GLAPIENTRY dummy_glDrawElements(GLenum mode, GLsizei count, GLenum type, const GLvoid *indices){}
    void GLAPIENTRY dummy_glEnable(GLenum cap){}
    void GLAPIENTRY dummy_glFinish(void){}
    void GLAPIENTRY dummy_glFlush(void){}
    void GLAPIENTRY dummy_glFrontFace(GLenum mode){}
    void GLAPIENTRY dummy_glGenBuffers(GLsizei n, GLuint *buffers){}
    void GLAPIENTRY dummy_glGenTextures(GLsizei n, GLuint *textures){}
    void GLAPIENTRY dummy_glGetBooleanv(GLenum pname, GLboolean *params){}
    void GLAPIENTRY dummy_glGetBufferParameteriv(GLenum, GLenum, GLint *){}
    GLenum GLAPIENTRY dummy_glGetError(void){ return 0;}
    void GLAPIENTRY dummy_glGetFloatv(GLenum pname, GLfloat *params){}
    void GLAPIENTRY dummy_glGetIntegerv(GLenum pname, GLint *params){}
    const GLubyte * GLAPIENTRY dummy_glGetString(GLenum name){ return 0;}
    void GLAPIENTRY dummy_glGetTexParameterfv(GLenum target, GLenum pname, GLfloat *params){}
    void GLAPIENTRY dummy_glGetTexParameteriv(GLenum target, GLenum pname, GLint *params){}
    void GLAPIENTRY dummy_glGetTexLevelParameteriv(GLenum target, GLint level, GLenum pname, GLint *params){}
    void GLAPIENTRY dummy_glHint(GLenum target, GLenum mode){}
    GLboolean GLAPIENTRY dummy_glIsBuffer(GLuint){ return false;}
    GLboolean GLAPIENTRY dummy_glIsEnabled(GLenum cap){ return false;}
    GLboolean GLAPIENTRY dummy_glIsTexture(GLuint texture){return false;}
    void GLAPIENTRY dummy_glLineWidth(GLfloat width){}
    void GLAPIENTRY dummy_glPolygonOffset(GLfloat factor, GLfloat units){}
    void GLAPIENTRY dummy_glPixelStorei(GLenum pname, GLint param){}
    void GLAPIENTRY dummy_glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid *pixels){}
    void GLAPIENTRY dummy_glSampleCoverage(GLclampf value, GLboolean invert ){}
    void GLAPIENTRY dummy_glScissor(GLint x, GLint y, GLsizei width, GLsizei height){}
    void GLAPIENTRY dummy_glStencilFunc(GLenum func, GLint ref, GLuint mask){}
    void GLAPIENTRY dummy_glStencilMask(GLuint mask){}
    void GLAPIENTRY dummy_glStencilOp(GLenum fail, GLenum zfail, GLenum zpass){}
    void GLAPIENTRY dummy_glTexImage2D(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid *pixels){}
    void GLAPIENTRY dummy_glTexParameteri(GLenum target, GLenum pname, GLint param){}
    void GLAPIENTRY dummy_glTexParameteriv(GLenum target, GLenum pname, const GLint *params){}
    void GLAPIENTRY dummy_glTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels){}
    void GLAPIENTRY dummy_glViewport(GLint x, GLint y, GLsizei width, GLsizei height){}
    void GLAPIENTRY dummy_glPushAttrib( GLbitfield mask ){}
    void GLAPIENTRY dummy_glPopAttrib( void ){}
    void GLAPIENTRY dummy_glPushClientAttrib( GLbitfield mask ){}
    void GLAPIENTRY dummy_glPopClientAttrib( void ){}

    /* OpenGL functions which are needed ONLY for implementing GLES 1.1*/
    void GLAPIENTRY dummy_glAlphaFunc(GLenum func, GLclampf ref){}
    void GLAPIENTRY dummy_glBegin( GLenum mode ){}
    void GLAPIENTRY dummy_glClearDepth(GLclampd depth){}
    void GLAPIENTRY dummy_glClientActiveTexture( GLenum texture ){}
    void GLAPIENTRY dummy_glClipPlane(GLenum plane, const GLdouble *equation){}
    void GLAPIENTRY dummy_glColor4d(GLdouble red, GLdouble green, GLdouble blue, GLdouble alpha){}
    void GLAPIENTRY dummy_glColor4f(GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha){}
    void GLAPIENTRY dummy_glColor4fv( const GLfloat *v ){}
    void GLAPIENTRY dummy_glColor4ub(GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha){}
    void GLAPIENTRY dummy_glColor4ubv( const GLubyte *v ){}
    void GLAPIENTRY dummy_glColorPointer(GLint size, GLenum type, GLsizei stride, const GLvoid *pointer){}
    void GLAPIENTRY dummy_glDisableClientState(GLenum array){}
    void GLAPIENTRY dummy_glEnableClientState(GLenum array){}
    void GLAPIENTRY dummy_glEnd(void){}
    void GLAPIENTRY dummy_glFogf(GLenum pname, GLfloat param){}
    void GLAPIENTRY dummy_glFogfv(GLenum pname, const GLfloat *params){}
    void GLAPIENTRY dummy_glFrustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar){}
    void GLAPIENTRY dummy_glGetClipPlane(GLenum plane, GLdouble *equation){}
    void GLAPIENTRY dummy_glGetDoublev( GLenum pname, GLdouble *params ){}
    void GLAPIENTRY dummy_glGetLightfv(GLenum light, GLenum pname, GLfloat *params){}
    void GLAPIENTRY dummy_glGetMaterialfv(GLenum face, GLenum pname, GLfloat *params){}
    void GLAPIENTRY dummy_glGetPointerv(GLenum pname, GLvoid* *params){}
    void GLAPIENTRY dummy_glGetTexEnvfv(GLenum target, GLenum pname, GLfloat *params){}
    void GLAPIENTRY dummy_glGetTexEnviv(GLenum target, GLenum pname, GLint *params){}
    void GLAPIENTRY dummy_glLightf(GLenum light, GLenum pname, GLfloat param){}
    void GLAPIENTRY dummy_glLightfv(GLenum light, GLenum pname, const GLfloat *params){}
    void GLAPIENTRY dummy_glLightModelf(GLenum pname, GLfloat param){}
    void GLAPIENTRY dummy_glLightModelfv(GLenum pname, const GLfloat *params){}
    void GLAPIENTRY dummy_glLoadIdentity(void){}
    void GLAPIENTRY dummy_glLoadMatrixf(const GLfloat *m){}
    void GLAPIENTRY dummy_glLogicOp(GLenum opcode){}
    void GLAPIENTRY dummy_glMaterialf(GLenum face, GLenum pname, GLfloat param){}
    void GLAPIENTRY dummy_glMaterialfv(GLenum face, GLenum pname, const GLfloat *params){}
    void GLAPIENTRY dummy_glMultiTexCoord2fv( GLenum target, const GLfloat *v ){}
    void GLAPIENTRY dummy_glMultiTexCoord2sv( GLenum target, const GLshort *v ){}
    void GLAPIENTRY dummy_glMultiTexCoord3fv( GLenum target, const GLfloat *v ){}
    void GLAPIENTRY dummy_glMultiTexCoord3sv( GLenum target, const GLshort *v ){}
    void GLAPIENTRY dummy_glMultiTexCoord4f( GLenum target, GLfloat s, GLfloat t, GLfloat r, GLfloat q ){}
    void GLAPIENTRY dummy_glMultiTexCoord4fv( GLenum target, const GLfloat *v ){}
    void GLAPIENTRY dummy_glMultiTexCoord4sv( GLenum target, const GLshort *v ){}
    void GLAPIENTRY dummy_glMultMatrixf(const GLfloat *m){}
    void GLAPIENTRY dummy_glNormal3f(GLfloat nx, GLfloat ny, GLfloat nz){}
    void GLAPIENTRY dummy_glNormal3fv( const GLfloat *v ){}
    void GLAPIENTRY dummy_glNormal3sv(const GLshort *v ){}
    void GLAPIENTRY dummy_glOrtho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble zNear, GLdouble zFar){}
    void GLAPIENTRY dummy_glPointParameterf(GLenum, GLfloat){}
    void GLAPIENTRY dummy_glPointParameterfv(GLenum, const GLfloat *){}
    void GLAPIENTRY dummy_glPointSize(GLfloat size){}
    void GLAPIENTRY dummy_glRotatef(GLfloat angle, GLfloat x, GLfloat y, GLfloat z){}
    void GLAPIENTRY dummy_glScalef(GLfloat x, GLfloat y, GLfloat z){}
    void GLAPIENTRY dummy_glTexEnvf(GLenum target, GLenum pname, GLfloat param){}
    void GLAPIENTRY dummy_glTexEnvfv(GLenum target, GLenum pname, const GLfloat *params){}
    void GLAPIENTRY dummy_glTexParameterf(GLenum target, GLenum pname, GLfloat param){}
    void GLAPIENTRY dummy_glTexParameterfv(GLenum target, GLenum pname, const GLfloat *params){}
    void GLAPIENTRY dummy_glMatrixMode(GLenum mode){}
    void GLAPIENTRY dummy_glNormalPointer(GLenum type, GLsizei stride, const GLvoid *pointer){}
    void GLAPIENTRY dummy_glPopMatrix(void){}
    void GLAPIENTRY dummy_glPushMatrix(void){}
    void GLAPIENTRY dummy_glShadeModel(GLenum mode){}
    void GLAPIENTRY dummy_glTexCoordPointer(GLint size, GLenum type, GLsizei stride, const GLvoid *pointer){}
    void GLAPIENTRY dummy_glTexEnvi(GLenum target, GLenum pname, GLint param){}
    void GLAPIENTRY dummy_glTexEnviv(GLenum target, GLenum pname, const GLint *params){}
    void GLAPIENTRY dummy_glTranslatef(GLfloat x, GLfloat y, GLfloat z){}
    void GLAPIENTRY dummy_glVertexPointer(GLint size, GLenum type, GLsizei stride, const GLvoid *pointer){}

    /* OpenGL functions which are needed ONLY for implementing GLES 1.1 EXTENSIONS*/
    GLboolean GLAPIENTRY dummy_glIsRenderbufferEXT(GLuint renderbuffer){ return false;}
    void GLAPIENTRY dummy_glBindRenderbufferEXT(GLenum target, GLuint renderbuffer){}
    void GLAPIENTRY dummy_glDeleteRenderbuffersEXT(GLsizei n, const GLuint *renderbuffers){}
    void GLAPIENTRY dummy_glGenRenderbuffersEXT(GLsizei n, GLuint *renderbuffers){}
    void GLAPIENTRY dummy_glRenderbufferStorageEXT(GLenum target, GLenum internalformat, GLsizei width, GLsizei height){}
    void GLAPIENTRY dummy_glGetRenderbufferParameterivEXT(GLenum target, GLenum pname, GLint *params){}
    GLboolean GLAPIENTRY dummy_glIsFramebufferEXT(GLuint framebuffer){ return false;}
    void GLAPIENTRY dummy_glBindFramebufferEXT(GLenum target, GLuint framebuffer){}
    void GLAPIENTRY dummy_glDeleteFramebuffersEXT(GLsizei n, const GLuint *framebuffers){}
    void GLAPIENTRY dummy_glGenFramebuffersEXT(GLsizei n, GLuint *framebuffers){}
    GLenum GLAPIENTRY dummy_glCheckFramebufferStatusEXT(GLenum target){ return 0;}
    void GLAPIENTRY dummy_glFramebufferTexture1DEXT(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level){}
    void GLAPIENTRY dummy_glFramebufferTexture2DEXT(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level){}
    void GLAPIENTRY dummy_glFramebufferTexture3DEXT(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint zoffset){}
    void GLAPIENTRY dummy_glFramebufferRenderbufferEXT(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer){}
    void GLAPIENTRY dummy_glGetFramebufferAttachmentParameterivEXT(GLenum target, GLenum attachment, GLenum pname, GLint *params){}
    void GLAPIENTRY dummy_glGenerateMipmapEXT(GLenum target){}
    void GLAPIENTRY dummy_glCurrentPaletteMatrixARB(GLint index){}
    void GLAPIENTRY dummy_glMatrixIndexuivARB(GLint size, GLuint * indices){}
    void GLAPIENTRY dummy_glMatrixIndexPointerARB(GLint size, GLenum type, GLsizei stride, const GLvoid* pointer){}
    void GLAPIENTRY dummy_glWeightPointerARB(GLint size, GLenum type, GLsizei stride, const GLvoid* pointer){}
    void GLAPIENTRY dummy_glTexGenf(GLenum coord, GLenum pname, GLfloat param ){}
    void GLAPIENTRY dummy_glTexGeni(GLenum coord, GLenum pname, GLint param ){}
    void GLAPIENTRY dummy_glTexGenf(GLenum coord, GLenum pname, const GLfloat *params ){}
    void GLAPIENTRY dummy_glTexGeniv(GLenum coord, GLenum pname, const GLint *params ){}
    void GLAPIENTRY dummy_glGetTexGenfv(GLenum coord, GLenum pname, GLfloat *params ){}
    void GLAPIENTRY dummy_glGetTexGeniv(GLenum coord, GLenum pname, GLint *params ){}

    /* Loading OpenGL functions which are needed ONLY for implementing GLES 2.0*/
    void GL_APIENTRY dummy_glBlendColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha){}
    void GL_APIENTRY dummy_glStencilFuncSeparate(GLenum face, GLenum func, GLint ref, GLuint mask){}
    void GL_APIENTRY dummy_glStencilMaskSeparate(GLenum face, GLuint mask){}
    void GL_APIENTRY dummy_glGenerateMipmap(GLenum target){}
    void GL_APIENTRY dummy_glBindFramebuffer(GLenum target, GLuint framebuffer){}
    void GL_APIENTRY dummy_glBindRenderbuffer(GLenum target, GLuint renderbuffer){}
    void GL_APIENTRY dummy_glDeleteFramebuffers(GLsizei n, const GLuint* framebuffers){}
    void GL_APIENTRY dummy_glDeleteRenderbuffers(GLsizei n, const GLuint* renderbuffers){}
    GLboolean GL_APIENTRY dummy_glIsProgram(GLuint program){ return false;}
    GLboolean GL_APIENTRY dummy_glIsShader(GLuint shader){ return false;}
    void GL_APIENTRY dummy_glVertexAttrib1f(GLuint indx, GLfloat x){}
    void GL_APIENTRY dummy_glVertexAttrib1fv(GLuint indx, const GLfloat* values){}
    void GL_APIENTRY dummy_glVertexAttrib2f(GLuint indx, GLfloat x, GLfloat y){}
    void GL_APIENTRY dummy_glVertexAttrib2fv(GLuint indx, const GLfloat* values){}
    void GL_APIENTRY dummy_glVertexAttrib3f(GLuint indx, GLfloat x, GLfloat y, GLfloat z){}
    void GL_APIENTRY dummy_glVertexAttrib3fv(GLuint indx, const GLfloat* values){}
    void GL_APIENTRY dummy_glVertexAttrib4f(GLuint indx, GLfloat x, GLfloat y, GLfloat z, GLfloat w){}
    void GL_APIENTRY dummy_glVertexAttrib4fv(GLuint indx, const GLfloat* values){}
    void GL_APIENTRY dummy_glVertexAttribPointer(GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid* ptr){}
    void GL_APIENTRY dummy_glDisableVertexAttribArray(GLuint index){}
    void GL_APIENTRY dummy_glEnableVertexAttribArray(GLuint index){}
    void GL_APIENTRY dummy_glGetVertexAttribfv(GLuint index, GLenum pname, GLfloat* params){}
    void GL_APIENTRY dummy_glGetVertexAttribiv(GLuint index, GLenum pname, GLint* params){}
    void GL_APIENTRY dummy_glGetVertexAttribPointerv(GLuint index, GLenum pname, GLvoid** pointer){}
    void GL_APIENTRY dummy_glUniform1f(GLint location, GLfloat x){}
    void GL_APIENTRY dummy_glUniform1fv(GLint location, GLsizei count, const GLfloat* v){}
    void GL_APIENTRY dummy_glUniform1i(GLint location, GLint x){}
    void GL_APIENTRY dummy_glUniform1iv(GLint location, GLsizei count, const GLint* v){}
    void GL_APIENTRY dummy_glUniform2f(GLint location, GLfloat x, GLfloat y){}
    void GL_APIENTRY dummy_glUniform2fv(GLint location, GLsizei count, const GLfloat* v){}
    void GL_APIENTRY dummy_glUniform2i(GLint location, GLint x, GLint y){}
    void GL_APIENTRY dummy_glUniform2iv(GLint location, GLsizei count, const GLint* v){}
    void GL_APIENTRY dummy_glUniform3f(GLint location, GLfloat x, GLfloat y, GLfloat z){}
    void GL_APIENTRY dummy_glUniform3fv(GLint location, GLsizei count, const GLfloat* v){}
    void GL_APIENTRY dummy_glUniform3i(GLint location, GLint x, GLint y, GLint z){}
    void GL_APIENTRY dummy_glUniform3iv(GLint location, GLsizei count, const GLint* v){}
    void GL_APIENTRY dummy_glUniform4f(GLint location, GLfloat x, GLfloat y, GLfloat z, GLfloat w){}
    void GL_APIENTRY dummy_glUniform4fv(GLint location, GLsizei count, const GLfloat* v){}
    void GL_APIENTRY dummy_glUniform4i(GLint location, GLint x, GLint y, GLint z, GLint w){}
    void GL_APIENTRY dummy_glUniform4iv(GLint location, GLsizei count, const GLint* v){}
    void GL_APIENTRY dummy_glUniformMatrix2fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value){}
    void GL_APIENTRY dummy_glUniformMatrix3fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value){}
    void GL_APIENTRY dummy_glUniformMatrix4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value){}
    void GL_APIENTRY dummy_glGetFramebufferAttachmentParameteriv(GLenum target, GLenum attachment, GLenum pname, GLint* params){}
    void GL_APIENTRY dummy_glGetRenderbufferParameteriv(GLenum target, GLenum pname, GLint* params){}
    GLboolean GL_APIENTRY dummy_glIsFramebuffer(GLuint framebuffer){ return false;}
    GLboolean GL_APIENTRY dummy_glIsRenderbuffer(GLuint renderbuffer){ return false;}
    GLenum GL_APIENTRY dummy_glCheckFramebufferStatus(GLenum target){ return 0;}
    void GL_APIENTRY dummy_glAttachShader(GLuint program, GLuint shader){}
    void GL_APIENTRY dummy_glBindAttribLocation(GLuint program, GLuint index, const GLchar* name){}
    void GL_APIENTRY dummy_glCompileShader(GLuint shader){}
    GLuint GL_APIENTRY dummy_glCreateProgram(void){ return 0;}
    GLuint GL_APIENTRY dummy_glCreateShader(GLenum type){ return 0;}
    void GL_APIENTRY dummy_glDeleteProgram(GLuint program){}
    void GL_APIENTRY dummy_glDeleteShader(GLuint shader){}
    void GL_APIENTRY dummy_glDetachShader(GLuint program, GLuint shader){}
    void GL_APIENTRY dummy_glLinkProgram(GLuint program){}
    void GL_APIENTRY dummy_glUseProgram(GLuint program){}
    void GL_APIENTRY dummy_glValidateProgram(GLuint program){}
    void GL_APIENTRY dummy_glGetActiveAttrib(GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name){}
    void GL_APIENTRY dummy_glGetActiveUniform(GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name){}
    void GL_APIENTRY dummy_glGetAttachedShaders(GLuint program, GLsizei maxcount, GLsizei* count, GLuint* shaders){}
    int  GL_APIENTRY dummy_glGetAttribLocation(GLuint program, const GLchar* name){ return 0;}
    void GL_APIENTRY dummy_glGetProgramiv(GLuint program, GLenum pname, GLint* params){}
    void GL_APIENTRY dummy_glGetProgramInfoLog(GLuint program, GLsizei bufsize, GLsizei* length, GLchar* infolog){}
    void GL_APIENTRY dummy_glGetShaderiv(GLuint shader, GLenum pname, GLint* params){}
    void GL_APIENTRY dummy_glGetShaderInfoLog(GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* infolog){}
    void GL_APIENTRY dummy_glGetShaderPrecisionFormat(GLenum shadertype, GLenum precisiontype, GLint* range, GLint* precision){}
    void GL_APIENTRY dummy_glGetShaderSource(GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* source){}
    void GL_APIENTRY dummy_glGetUniformfv(GLuint program, GLint location, GLfloat* params){}
    void GL_APIENTRY dummy_glGetUniformiv(GLuint program, GLint location, GLint* params){}
    int  GL_APIENTRY dummy_glGetUniformLocation(GLuint program, const GLchar* name){ return 0;}
    void GL_APIENTRY dummy_glReleaseShaderCompiler(void){}
    void GL_APIENTRY dummy_glRenderbufferStorage(GLenum target, GLenum internalformat, GLsizei width, GLsizei height){}
    void GL_APIENTRY dummy_glShaderBinary(GLsizei n, const GLuint* shaders, GLenum binaryformat, const GLvoid* binary, GLsizei length){}
    void GL_APIENTRY dummy_glShaderSource(GLuint shader, GLsizei count, const GLchar** string, const GLint* length){}
    void GL_APIENTRY dummy_glFramebufferRenderbuffer(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer){}
    void GL_APIENTRY dummy_glFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level){}
