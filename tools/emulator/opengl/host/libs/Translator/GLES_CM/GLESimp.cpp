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
#include <stdio.h>
#include "GLDispatch.h"
#include "GLEScontext.h"
#include "GLESvalidate.h"
#include "GLESutils.h"
#include "GLfixed_ops.h"
#include "TextureUtils.h"

#include <GLcommon/TranslatorIfaces.h>
#include <GLcommon/ThreadInfo.h>
#include <GLES/gl.h>
#include <cmath>


extern "C" {

//decleration
static void initContext(GLEScontext* ctx);
static GLEScontext* createGLESContext();
static void deleteGLESContext(GLEScontext* ctx);

}

static EGLiface*  s_eglIface = NULL;
static GLESiface  s_glesIface = {
    createGLESContext:createGLESContext,
    initContext      :initContext,
    deleteGLESContext:deleteGLESContext,
    flush            :glFlush,
    finish           :glFinish
};

extern "C" {

static void initContext(GLEScontext* ctx) {
    ctx->init();
}
static GLEScontext* createGLESContext() {
    GLEScontext* ctx = new GLEScontext();
    return ctx;
}

static void deleteGLESContext(GLEScontext* ctx) {
    if(ctx) delete ctx;
}

GLESiface* __translator_getIfaces(EGLiface* eglIface){
    s_eglIface = eglIface;
    return & s_glesIface;
}

}

#define GET_THREAD()                                                         \
            ThreadInfo* thrd = NULL;                                         \
            if(s_eglIface) {                                                 \
                thrd = s_eglIface->getThreadInfo();                          \
            } else {                                                         \
                fprintf(stderr,"Context wasn't initialized yet \n");         \
            }
 

#define GET_CTX()                                                            \
            GET_THREAD();                                                    \
            if(!thrd) return;                                                \
            GLEScontext *ctx = static_cast<GLEScontext*>(thrd->glesContext);

#define GET_CTX_RET(failure_ret)                                             \
            GET_THREAD();                                                    \
            if(!thrd) return failure_ret;                                    \
            GLEScontext *ctx = static_cast<GLEScontext*>(thrd->glesContext);


#define SET_ERROR_IF(condition,err) if((condition)) {                        \
                        ctx->setGLerror(err);                                \
                        return;                                              \
                    }


#define RET_AND_SET_ERROR_IF(condition,err,ret) if((condition)) {            \
                        ctx->setGLerror(err);                                \
                        return ret;                                          \
                    }


GL_API GLboolean GL_APIENTRY glIsBuffer(GLuint buffer) {
    GET_CTX_RET(GL_FALSE)
    return ctx->isBuffer(buffer);
}

GL_API GLboolean GL_APIENTRY  glIsEnabled( GLenum cap) {
    GET_CTX_RET(GL_FALSE)
    RET_AND_SET_ERROR_IF(!GLESvalidate::capability(cap,ctx->getMaxLights(),ctx->getMaxClipPlanes()),GL_INVALID_ENUM,GL_FALSE);

    if(cap == GL_POINT_SIZE_ARRAY_OES) return ctx->isArrEnabled(cap);
    return ctx->dispatcher().glIsEnabled(cap);
}

GL_API GLboolean GL_APIENTRY  glIsTexture( GLuint texture) {
    GET_CTX_RET(GL_FALSE)
    return ctx->dispatcher().glIsTexture(texture);
}

GL_API GLenum GL_APIENTRY  glGetError(void) {
    GET_CTX_RET(GL_NO_ERROR)
    GLenum err = ctx->getGLerror();
    if(err != GL_NO_ERROR) {
        ctx->setGLerror(GL_NO_ERROR);
        return err;
    }

    return ctx->dispatcher().glGetError();
}

GL_API const GLubyte * GL_APIENTRY  glGetString( GLenum name) {

    GET_CTX_RET(NULL)
    static GLubyte VENDOR[]     = "Google";
    static GLubyte RENDERER[]   = "OpenGL ES-CM 1.1";
    static GLubyte VERSION[]    = "OpenGL ES-CM 1.1";
    static GLubyte EXTENSIONS[] = "GL_OES_compressed_paletted_texture "
                                  "GL_OES_point_size_array";
    switch(name) {
        case GL_VENDOR:
            return VENDOR;
        case GL_RENDERER:
            return RENDERER;
        case GL_VERSION:
            return VERSION;
        case GL_EXTENSIONS:
            return EXTENSIONS;
        default:
            RET_AND_SET_ERROR_IF(true,GL_INVALID_ENUM,NULL);
    }
}

GL_API void GL_APIENTRY  glActiveTexture( GLenum texture) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::textureEnum(texture,ctx->getMaxTexUnits()),GL_INVALID_ENUM);
    ctx->dispatcher().glActiveTexture(texture);
}

GL_API void GL_APIENTRY  glAlphaFunc( GLenum func, GLclampf ref) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::alphaFunc(func),GL_INVALID_ENUM);
    ctx->dispatcher().glAlphaFunc(func,ref);
}


GL_API void GL_APIENTRY  glAlphaFuncx( GLenum func, GLclampx ref) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::alphaFunc(func),GL_INVALID_ENUM);
    ctx->dispatcher().glAlphaFunc(func,X2F(ref));
}


GL_API void GL_APIENTRY  glBindBuffer( GLenum target, GLuint buffer) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::bufferTarget(target),GL_INVALID_ENUM);
    ctx->bindBuffer(target,buffer);
}

GL_API void GL_APIENTRY  glBindTexture( GLenum target, GLuint texture) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::textureTarget(target),GL_INVALID_ENUM)
    ctx->dispatcher().glBindTexture(target,texture);
}

GL_API void GL_APIENTRY  glBlendFunc( GLenum sfactor, GLenum dfactor) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::blendSrc(sfactor) || !GLESvalidate::blendDst(dfactor),GL_INVALID_ENUM)
    ctx->dispatcher().glBlendFunc(sfactor,dfactor);
}

GL_API void GL_APIENTRY  glBufferData( GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::bufferTarget(target),GL_INVALID_ENUM);
    SET_ERROR_IF(!ctx->isBindedBuffer(target),GL_INVALID_OPERATION);
    ctx->setBufferData(target,size,data,usage);
}

GL_API void GL_APIENTRY  glBufferSubData( GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data) {
    GET_CTX()
    SET_ERROR_IF(!ctx->isBindedBuffer(target),GL_INVALID_OPERATION);
    SET_ERROR_IF(!GLESvalidate::bufferTarget(target),GL_INVALID_ENUM);
    SET_ERROR_IF(!ctx->setBufferSubData(target,offset,size,data),GL_INVALID_VALUE);
}

GL_API void GL_APIENTRY  glClear( GLbitfield mask) {
    GET_CTX()
    ctx->dispatcher().glClear(mask);
}

GL_API void GL_APIENTRY  glClearColor( GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha) {
    GET_CTX()
    ctx->dispatcher().glClearColor(red,green,blue,alpha);
}

GL_API void GL_APIENTRY  glClearColorx( GLclampx red, GLclampx green, GLclampx blue, GLclampx alpha) {
    GET_CTX()
    ctx->dispatcher().glClearColor(X2F(red),X2F(green),X2F(blue),X2F(alpha));
}


GL_API void GL_APIENTRY  glClearDepthf( GLclampf depth) {
    GET_CTX()
    ctx->dispatcher().glClearDepthf(depth);
}

GL_API void GL_APIENTRY  glClearDepthx( GLclampx depth) {
    GET_CTX()
    ctx->dispatcher().glClearDepthf(X2F(depth));
}

GL_API void GL_APIENTRY  glClearStencil( GLint s) {
    GET_CTX()
    ctx->dispatcher().glClearStencil(s);
}

GL_API void GL_APIENTRY  glClientActiveTexture( GLenum texture) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::textureEnum(texture,ctx->getMaxTexUnits()),GL_INVALID_ENUM);
    ctx->setActiveTexture(texture);
    ctx->dispatcher().glClientActiveTexture(texture);

}

GL_API void GL_APIENTRY  glClipPlanef( GLenum plane, const GLfloat *equation) {
    GET_CTX()
    GLdouble tmpEquation[4];

    for(int i = 0; i < 4; i++) {
         tmpEquation[i] = static_cast<GLdouble>(equation[i]);
    }
    ctx->dispatcher().glClipPlane(plane,tmpEquation);
}

GL_API void GL_APIENTRY  glClipPlanex( GLenum plane, const GLfixed *equation) {
    GET_CTX()
    GLdouble tmpEquation[4];
    for(int i = 0; i < 4; i++) {
        tmpEquation[i] = X2D(equation[i]);
    }
    ctx->dispatcher().glClipPlane(plane,tmpEquation);
}

GL_API void GL_APIENTRY  glColor4f( GLfloat red, GLfloat green, GLfloat blue, GLfloat alpha) {
    GET_CTX()
    ctx->dispatcher().glColor4f(red,green,blue,alpha);
}

GL_API void GL_APIENTRY  glColor4ub( GLubyte red, GLubyte green, GLubyte blue, GLubyte alpha) {
    GET_CTX()
    ctx->dispatcher().glColor4ub(red,green,blue,alpha);
}

GL_API void GL_APIENTRY  glColor4x( GLfixed red, GLfixed green, GLfixed blue, GLfixed alpha) {
    GET_CTX()
    ctx->dispatcher().glColor4f(X2F(red),X2F(green),X2F(blue),X2F(alpha));
}

GL_API void GL_APIENTRY  glColorMask( GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha) {
    GET_CTX()
    ctx->dispatcher().glColorMask(red,green,blue,alpha);
}

GL_API void GL_APIENTRY  glColorPointer( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::colorPointerParams(size,stride),GL_INVALID_VALUE);

    const GLvoid* data = ctx->setPointer(GL_COLOR_ARRAY,size,type,stride,pointer);
    if(type != GL_FIXED) ctx->dispatcher().glColorPointer(size,type,stride,data);
}

GL_API void GL_APIENTRY  glCompressedTexImage2D( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data) {
    GET_CTX()
    SET_ERROR_IF(!(GLESvalidate::texCompImgFrmt(internalformat) && GLESvalidate::textureTarget(target)),GL_INVALID_ENUM);
    SET_ERROR_IF(level > log2(ctx->getMaxTexSize())|| border !=0 || level > 0 || !GLESvalidate::texImgDim(width,height,ctx->getMaxTexSize()+2),GL_INVALID_VALUE)

    int nMipmaps = -level + 1;
    GLsizei tmpWidth  = width;
    GLsizei tmpHeight = height;

    for(int i = 0; i < nMipmaps ; i++)
    {
       GLenum uncompressedFrmt;
       unsigned char* uncompressed = uncompressTexture(internalformat,uncompressedFrmt,width,height,imageSize,data,i);
       ctx->dispatcher().glTexImage2D(target,i,uncompressedFrmt,width,height,border,uncompressedFrmt,GL_UNSIGNED_BYTE,uncompressed);
       tmpWidth/=2;
       tmpHeight/=2;
       delete uncompressed;
    }
    ctx->dispatcher().glCompressedTexImage2D(target,level,internalformat,width,height,border,imageSize,data);
}

GL_API void GL_APIENTRY  glCompressedTexSubImage2D( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid *data) {
    GET_CTX()
    SET_ERROR_IF(!(GLESvalidate::texCompImgFrmt(format) && GLESvalidate::textureTarget(target)),GL_INVALID_ENUM);
    SET_ERROR_IF(level < 0 || level > log2(ctx->getMaxTexSize()),GL_INVALID_VALUE)

    GLenum uncompressedFrmt;
    unsigned char* uncompressed = uncompressTexture(format,uncompressedFrmt,width,height,imageSize,data,level);
    ctx->dispatcher().glTexSubImage2D(target,level,xoffset,yoffset,width,height,uncompressedFrmt,GL_UNSIGNED_BYTE,uncompressed);
    delete uncompressed;
}

GL_API void GL_APIENTRY  glCopyTexImage2D( GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border) {
    GET_CTX()
    SET_ERROR_IF(!(GLESvalidate::pixelFrmt(internalformat) && GLESvalidate::textureTarget(target)),GL_INVALID_ENUM);
    SET_ERROR_IF(border != 0,GL_INVALID_VALUE);
    ctx->dispatcher().glCopyTexImage2D(target,level,internalformat,x,y,width,height,border);
}

GL_API void GL_APIENTRY  glCopyTexSubImage2D( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::textureTarget(target),GL_INVALID_ENUM);
    ctx->dispatcher().glCopyTexSubImage2D(target,level,xoffset,yoffset,x,y,width,height);
}

GL_API void GL_APIENTRY  glCullFace( GLenum mode) {
    GET_CTX()
    ctx->dispatcher().glCullFace(mode);
}

GL_API void GL_APIENTRY  glDeleteBuffers( GLsizei n, const GLuint *buffers) {
    GET_CTX()
    SET_ERROR_IF(n<0,GL_INVALID_VALUE);
    ctx->deleteBuffers(n,buffers);
}

GL_API void GL_APIENTRY  glDeleteTextures( GLsizei n, const GLuint *textures) {
    GET_CTX()
    ctx->dispatcher().glDeleteTextures(n,textures);
}

GL_API void GL_APIENTRY  glDepthFunc( GLenum func) {
    GET_CTX()
    ctx->dispatcher().glDepthFunc(func);
}

GL_API void GL_APIENTRY  glDepthMask( GLboolean flag) {
    GET_CTX()
    ctx->dispatcher().glDepthMask(flag);
}

GL_API void GL_APIENTRY  glDepthRangef( GLclampf zNear, GLclampf zFar) {
    GET_CTX()
    ctx->dispatcher().glDepthRange(zNear,zFar);
}

GL_API void GL_APIENTRY  glDepthRangex( GLclampx zNear, GLclampx zFar) {
    GET_CTX()
    ctx->dispatcher().glDepthRange(X2F(zNear),X2F(zFar));
}

GL_API void GL_APIENTRY  glDisable( GLenum cap) {
    GET_CTX()
    ctx->dispatcher().glDisable(cap);
}

GL_API void GL_APIENTRY  glDisableClientState( GLenum array) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::supportedArrays(array),GL_INVALID_ENUM)

    ctx->enableArr(array,false);
    if(array != GL_POINT_SIZE_ARRAY_OES) ctx->dispatcher().glDisableClientState(array);
}


GL_API void GL_APIENTRY  glDrawArrays( GLenum mode, GLint first, GLsizei count) {
    GET_CTX()
    SET_ERROR_IF(count < 0,GL_INVALID_VALUE)
    SET_ERROR_IF(!GLESvalidate::drawMode(mode),GL_INVALID_ENUM)

    if(!ctx->isArrEnabled(GL_VERTEX_ARRAY)) return;

    GLESFloatArrays tmpArrs;
    ctx->convertArrs(tmpArrs,first,count,0,NULL,true);
    if(mode != GL_POINTS || !ctx->isArrEnabled(GL_POINT_SIZE_ARRAY_OES)){
        ctx->dispatcher().glDrawArrays(mode,first,count);
    }
    else{
        ctx->drawPointsArrs(tmpArrs,first,count);
    }
}

GL_API void GL_APIENTRY  glDrawElements( GLenum mode, GLsizei count, GLenum type, const GLvoid *elementsIndices) {
    GET_CTX()
    SET_ERROR_IF(count < 0,GL_INVALID_VALUE)
    SET_ERROR_IF((!GLESvalidate::drawMode(mode) || !GLESvalidate::drawType(type)),GL_INVALID_ENUM)
    const GLvoid* indices = elementsIndices;
    GLESFloatArrays tmpArrs;
    if(ctx->isBindedBuffer(GL_ELEMENT_ARRAY_BUFFER)) { // if vbo is binded take the indices from the vbo
        const unsigned char* buf = static_cast<unsigned char *>(ctx->getBindedBuffer(GL_ELEMENT_ARRAY_BUFFER));
        indices = buf+reinterpret_cast<unsigned int>(elementsIndices);
    }

    ctx->convertArrs(tmpArrs,0,count,type,indices,false);
    if(mode != GL_POINTS || !ctx->isArrEnabled(GL_POINT_SIZE_ARRAY_OES)){
        ctx->dispatcher().glDrawElements(mode,count,type,indices);
    }
    else{
        ctx->drawPointsElems(tmpArrs,count,type,indices);
    }
}

GL_API void GL_APIENTRY  glEnable( GLenum cap) {
    GET_CTX()
    ctx->dispatcher().glEnable(cap);
}

GL_API void GL_APIENTRY  glEnableClientState( GLenum array) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::supportedArrays(array),GL_INVALID_ENUM)

    ctx->enableArr(array,true);
    if(array != GL_POINT_SIZE_ARRAY_OES) ctx->dispatcher().glEnableClientState(array);
}

GL_API void GL_APIENTRY  glFinish( void) {
    GET_CTX()
    ctx->dispatcher().glFinish();
}

GL_API void GL_APIENTRY  glFlush( void) {
    GET_CTX()
    ctx->dispatcher().glFlush();
}

GL_API void GL_APIENTRY  glFogf( GLenum pname, GLfloat param) {
    GET_CTX()
    ctx->dispatcher().glFogf(pname,param);
}

GL_API void GL_APIENTRY  glFogfv( GLenum pname, const GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glFogfv(pname,params);
}

GL_API void GL_APIENTRY  glFogx( GLenum pname, GLfixed param) {
    GET_CTX()
    ctx->dispatcher().glFogf(pname,(pname == GL_FOG_MODE)? static_cast<GLfloat>(param):X2F(param));
}

GL_API void GL_APIENTRY  glFogxv( GLenum pname, const GLfixed *params) {
    GET_CTX()
    if(pname == GL_FOG_MODE) {
        GLfloat tmpParam = static_cast<GLfloat>(params[0]);
        ctx->dispatcher().glFogfv(pname,&tmpParam);
    } else {
        GLfloat tmpParams[4];
        for(int i=0; i< 4; i++) {
            tmpParams[i] = X2F(params[i]);
        }
        ctx->dispatcher().glFogfv(pname,tmpParams);
    }

}

GL_API void GL_APIENTRY  glFrontFace( GLenum mode) {
    GET_CTX()
    ctx->dispatcher().glFrontFace(mode);
}

GL_API void GL_APIENTRY  glFrustumf( GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar) {
    GET_CTX()
    ctx->dispatcher().glFrustum(left,right,bottom,top,zNear,zFar);
}

GL_API void GL_APIENTRY  glFrustumx( GLfixed left, GLfixed right, GLfixed bottom, GLfixed top, GLfixed zNear, GLfixed zFar) {
    GET_CTX()
    ctx->dispatcher().glFrustum(X2F(left),X2F(right),X2F(bottom),X2F(top),X2F(zNear),X2F(zFar));
}

GL_API void GL_APIENTRY  glGenBuffers( GLsizei n, GLuint *buffers) {
    GET_CTX()
    SET_ERROR_IF(n<0,GL_INVALID_VALUE);
    ctx->genBuffers(n,buffers);
}

GL_API void GL_APIENTRY  glGenTextures( GLsizei n, GLuint *textures) {
    GET_CTX()
    ctx->dispatcher().glGenTextures(n,textures);
}

GL_API void GL_APIENTRY  glGetBooleanv( GLenum pname, GLboolean *params) {
    GET_CTX()
    ctx->dispatcher().glGetBooleanv(pname,params);
}

GL_API void GL_APIENTRY  glGetBufferParameteriv( GLenum target, GLenum pname, GLint *params) {
    GET_CTX()
    SET_ERROR_IF(!(GLESvalidate::bufferTarget(target) && GLESvalidate::bufferParam(pname)),GL_INVALID_ENUM);
    SET_ERROR_IF(!ctx->isBindedBuffer(target),GL_INVALID_OPERATION);
    bool ret = true;
    switch(pname) {
    case GL_BUFFER_SIZE:
        ctx->getBufferSize(target,params);
        break;
    case GL_BUFFER_USAGE:
        ctx->getBufferUsage(target,params);
        break;
    }

}

GL_API void GL_APIENTRY  glGetClipPlanef( GLenum pname, GLfloat eqn[4]) {
    GET_CTX()
    GLdouble tmpEqn[4];

    ctx->dispatcher().glGetClipPlane(pname,tmpEqn);
    for(int i =0 ;i < 4; i++){
        eqn[i] = static_cast<GLfloat>(tmpEqn[i]);
    }
}

GL_API void GL_APIENTRY  glGetClipPlanex( GLenum pname, GLfixed eqn[4]) {
    GET_CTX()
    GLdouble tmpEqn[4];

    ctx->dispatcher().glGetClipPlane(pname,tmpEqn);
    for(int i =0 ;i < 4; i++){
        eqn[i] = F2X(tmpEqn[i]);
    }
}

GL_API void GL_APIENTRY  glGetFixedv( GLenum pname, GLfixed *params) {
    GET_CTX()
    size_t nParams = glParamSize(pname);
    GLfloat fParams[16];
    ctx->dispatcher().glGetFloatv(pname,fParams);
    for(size_t i =0 ; i < nParams;i++) {
        params[i] = F2X(fParams[i]);
    }
}

GL_API void GL_APIENTRY  glGetFloatv( GLenum pname, GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glGetFloatv(pname,params);
}

GL_API void GL_APIENTRY  glGetIntegerv( GLenum pname, GLint *params) {
    GET_CTX()
    ctx->dispatcher().glGetIntegerv(pname,params);
}

GL_API void GL_APIENTRY  glGetLightfv( GLenum light, GLenum pname, GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glGetLightfv(light,pname,params);
}

GL_API void GL_APIENTRY  glGetLightxv( GLenum light, GLenum pname, GLfixed *params) {
    GET_CTX()
    GLfloat tmpParams[4];

    ctx->dispatcher().glGetLightfv(light,pname,tmpParams);
    switch (pname){
        case GL_AMBIENT:
        case GL_DIFFUSE:
        case GL_SPECULAR:
        case GL_POSITION:
            params[3] = F2X(tmpParams[3]);
        case GL_SPOT_DIRECTION:
            params[2] = F2X(tmpParams[2]);
            params[1] = F2X(tmpParams[1]);
            break;
        default:{
            ctx->setGLerror(GL_INVALID_ENUM);
            return;
        }

    }
    params[0] = F2X(tmpParams[0]);
}

GL_API void GL_APIENTRY  glGetMaterialfv( GLenum face, GLenum pname, GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glGetMaterialfv(face,pname,params);
}

GL_API void GL_APIENTRY  glGetMaterialxv( GLenum face, GLenum pname, GLfixed *params) {
    GET_CTX()
    GLfloat tmpParams[4];
    ctx->dispatcher().glGetMaterialfv(face,pname,tmpParams);
    switch(pname){
    case GL_AMBIENT:
    case GL_DIFFUSE:
    case GL_SPECULAR:
    case GL_EMISSION:
    case GL_AMBIENT_AND_DIFFUSE:
        params[3] = tmpParams[3];
        params[2] = tmpParams[2];
        params[1] = tmpParams[1];
    case GL_SHININESS:
        params[0] = tmpParams[0];
    default:{
            ctx->setGLerror(GL_INVALID_ENUM);
            return;
        }
    }
}

GL_API void GL_APIENTRY  glGetPointerv( GLenum pname, void **params) {
    GET_CTX()
    const GLESpointer* p = ctx->getPointer(pname);
    if(p) {
        *params = const_cast<void *>( p->getArrayData());
    } else {
        ctx->setGLerror(GL_INVALID_ENUM);
    }

}

GL_API void GL_APIENTRY  glGetTexEnvfv( GLenum env, GLenum pname, GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glGetTexEnvfv(env,pname,params);
}

GL_API void GL_APIENTRY  glGetTexEnviv( GLenum env, GLenum pname, GLint *params) {
    GET_CTX()
    ctx->dispatcher().glGetTexEnviv(env,pname,params);
}

GL_API void GL_APIENTRY  glGetTexEnvxv( GLenum env, GLenum pname, GLfixed *params) {
    GET_CTX()
    GLfloat tmpParams[4];

    ctx->dispatcher().glGetTexEnvfv(env,pname,tmpParams);
    if(pname == GL_TEXTURE_ENV_MODE) {
        params[0] = static_cast<GLfixed>(tmpParams[0]);
    } else {
        for(int i=0 ; i < 4 ; i++)
            params[i] = F2X(tmpParams[i]);
    }
}

GL_API void GL_APIENTRY  glGetTexParameterfv( GLenum target, GLenum pname, GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glGetTexParameterfv(target,pname,params);
}

GL_API void GL_APIENTRY  glGetTexParameteriv( GLenum target, GLenum pname, GLint *params) {
    GET_CTX()
    ctx->dispatcher().glGetTexParameteriv(target,pname,params);
}

GL_API void GL_APIENTRY  glGetTexParameterxv( GLenum target, GLenum pname, GLfixed *params) {
    GET_CTX()
    GLfloat tmpParam;
    ctx->dispatcher().glGetTexParameterfv(target,pname,&tmpParam);
    params[0] = static_cast<GLfixed>(tmpParam);
}

GL_API void GL_APIENTRY  glHint( GLenum target, GLenum mode) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::hintTargetMode(target,mode),GL_INVALID_ENUM);
    ctx->dispatcher().glHint(target,mode);
}

GL_API void GL_APIENTRY  glLightModelf( GLenum pname, GLfloat param) {
    GET_CTX()
    ctx->dispatcher().glLightModelf(pname,param);
}

GL_API void GL_APIENTRY  glLightModelfv( GLenum pname, const GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glLightModelfv(pname,params);
}

GL_API void GL_APIENTRY  glLightModelx( GLenum pname, GLfixed param) {
    GET_CTX()
    GLfloat tmpParam = static_cast<GLfloat>(param);
    ctx->dispatcher().glLightModelf(pname,tmpParam);
}

GL_API void GL_APIENTRY  glLightModelxv( GLenum pname, const GLfixed *params) {
    GET_CTX()
    GLfloat tmpParams[4];
    if(pname == GL_LIGHT_MODEL_TWO_SIDE) {
        tmpParams[0] = X2F(params[0]);
    } else if (pname == GL_LIGHT_MODEL_AMBIENT) {
        for(int i=0;i<4;i++) {
            tmpParams[i] = X2F(params[i]);
        }
    }

    ctx->dispatcher().glLightModelfv(pname,tmpParams);
}

GL_API void GL_APIENTRY  glLightf( GLenum light, GLenum pname, GLfloat param) {
    GET_CTX()
    ctx->dispatcher().glLightf(light,pname,param);
}

GL_API void GL_APIENTRY  glLightfv( GLenum light, GLenum pname, const GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glLightfv(light,pname,params);
}

GL_API void GL_APIENTRY  glLightx( GLenum light, GLenum pname, GLfixed param) {
    GET_CTX()
    ctx->dispatcher().glLightf(light,pname,X2F(param));
}

GL_API void GL_APIENTRY  glLightxv( GLenum light, GLenum pname, const GLfixed *params) {
    GET_CTX()
    GLfloat tmpParams[4];

    switch (pname) {
        case GL_AMBIENT:
        case GL_DIFFUSE:
        case GL_SPECULAR:
        case GL_POSITION:
            tmpParams[3] = X2F(params[3]);
        case GL_SPOT_DIRECTION:
            tmpParams[2] = X2F(params[2]);
            tmpParams[1] = X2F(params[1]);
            break;
        default: {
                ctx->setGLerror(GL_INVALID_ENUM);
                return;
            }
    }
    tmpParams[0] = X2F(params[0]);
    ctx->dispatcher().glLightfv(light,pname,tmpParams);
}

GL_API void GL_APIENTRY  glLineWidth( GLfloat width) {
    GET_CTX()
    ctx->dispatcher().glLineWidth(width);
}

GL_API void GL_APIENTRY  glLineWidthx( GLfixed width) {
    GET_CTX()
    ctx->dispatcher().glLineWidth(X2F(width));
}

GL_API void GL_APIENTRY  glLoadIdentity( void) {
    GET_CTX()
    ctx->dispatcher().glLoadIdentity();
}

GL_API void GL_APIENTRY  glLoadMatrixf( const GLfloat *m) {
    GET_CTX()
    ctx->dispatcher().glLoadMatrixf(m);
}

GL_API void GL_APIENTRY  glLoadMatrixx( const GLfixed *m) {
    GET_CTX()
    GLfloat mat[16];
    for(int i=0; i< 16 ; i++) {
        mat[i] = X2F(m[i]);
    }
    ctx->dispatcher().glLoadMatrixf(mat);
}

GL_API void GL_APIENTRY  glLogicOp( GLenum opcode) {
    GET_CTX()
    ctx->dispatcher().glLogicOp(opcode);
}

GL_API void GL_APIENTRY  glMaterialf( GLenum face, GLenum pname, GLfloat param) {
    GET_CTX()
    ctx->dispatcher().glMaterialf(face,pname,param);
}

GL_API void GL_APIENTRY  glMaterialfv( GLenum face, GLenum pname, const GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glMaterialfv(face,pname,params);
}

GL_API void GL_APIENTRY  glMaterialx( GLenum face, GLenum pname, GLfixed param) {
    GET_CTX()
    ctx->dispatcher().glMaterialf(face,pname,X2F(param));
}

GL_API void GL_APIENTRY  glMaterialxv( GLenum face, GLenum pname, const GLfixed *params) {
    GET_CTX()
    GLfloat tmpParams[4];

    for(int i=0; i< 4; i++) {
        tmpParams[i] = X2F(params[i]);
    }
    ctx->dispatcher().glMaterialfv(face,pname,tmpParams);
}

GL_API void GL_APIENTRY  glMatrixMode( GLenum mode) {
    GET_CTX()
    ctx->dispatcher().glMatrixMode(mode);
}

GL_API void GL_APIENTRY  glMultMatrixf( const GLfloat *m) {
    GET_CTX()
    ctx->dispatcher().glMultMatrixf(m);
}

GL_API void GL_APIENTRY  glMultMatrixx( const GLfixed *m) {
    GET_CTX()
    GLfloat mat[16];
    for(int i=0; i< 16 ; i++) {
        mat[i] = X2F(m[i]);
    }
    ctx->dispatcher().glMultMatrixf(mat);
}

GL_API void GL_APIENTRY  glMultiTexCoord4f( GLenum target, GLfloat s, GLfloat t, GLfloat r, GLfloat q) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::textureEnum(target,ctx->getMaxTexUnits()),GL_INVALID_ENUM);
    ctx->dispatcher().glMultiTexCoord4f(target,s,t,r,q);
}

GL_API void GL_APIENTRY  glMultiTexCoord4x( GLenum target, GLfixed s, GLfixed t, GLfixed r, GLfixed q) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::textureEnum(target,ctx->getMaxTexUnits()),GL_INVALID_ENUM);
    ctx->dispatcher().glMultiTexCoord4f(target,X2F(s),X2F(t),X2F(r),X2F(q));
}

GL_API void GL_APIENTRY  glNormal3f( GLfloat nx, GLfloat ny, GLfloat nz) {
    GET_CTX()
    ctx->dispatcher().glNormal3f(nx,ny,nz);
}

GL_API void GL_APIENTRY  glNormal3x( GLfixed nx, GLfixed ny, GLfixed nz) {
    GET_CTX()
    ctx->dispatcher().glNormal3f(X2F(nx),X2F(ny),X2F(nz));
}

GL_API void GL_APIENTRY  glNormalPointer( GLenum type, GLsizei stride, const GLvoid *pointer) {
    GET_CTX()
    SET_ERROR_IF(stride < 0,GL_INVALID_VALUE);
    const GLvoid* data = ctx->setPointer(GL_NORMAL_ARRAY,3,type,stride,pointer);//3 normal verctor
    if(type != GL_FIXED) ctx->dispatcher().glNormalPointer(type,stride,data);
}

GL_API void GL_APIENTRY  glOrthof( GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat zNear, GLfloat zFar) {
    GET_CTX()
    ctx->dispatcher().glOrtho(left,right,bottom,top,zNear,zFar);
}

GL_API void GL_APIENTRY  glOrthox( GLfixed left, GLfixed right, GLfixed bottom, GLfixed top, GLfixed zNear, GLfixed zFar) {
    GET_CTX()
    ctx->dispatcher().glOrtho(X2F(left),X2F(right),X2F(bottom),X2F(top),X2F(zNear),X2F(zFar));
}

GL_API void GL_APIENTRY  glPixelStorei( GLenum pname, GLint param) {
    GET_CTX()
    ctx->dispatcher().glPixelStorei(pname,param);
}

GL_API void GL_APIENTRY  glPointParameterf( GLenum pname, GLfloat param) {
    GET_CTX()
    ctx->dispatcher().glPointParameterf(pname,param);
}

GL_API void GL_APIENTRY  glPointParameterfv( GLenum pname, const GLfloat *params) {
    GET_CTX()
    ctx->dispatcher().glPointParameterfv(pname,params);
}

GL_API void GL_APIENTRY  glPointParameterx( GLenum pname, GLfixed param)
{
    GET_CTX()
    ctx->dispatcher().glPointParameterf(pname,X2F(param));
}

GL_API void GL_APIENTRY  glPointParameterxv( GLenum pname, const GLfixed *params) {
    GET_CTX()
    GLfloat tmpParams[3];
    int i = 0;

    do {
        tmpParams[i] = X2F(params[i]);
        i++;
    }while(pname != GL_POINT_DISTANCE_ATTENUATION);
    ctx->dispatcher().glPointParameterfv(pname,tmpParams);
}

GL_API void GL_APIENTRY  glPointSize( GLfloat size) {
    GET_CTX()
    ctx->dispatcher().glPointSize(size);
}

GL_API void GL_APIENTRY  glPointSizePointerOES( GLenum type, GLsizei stride, const GLvoid *pointer) {
    GET_CTX()
    SET_ERROR_IF(stride < 0,GL_INVALID_VALUE);
    ctx->setPointer(GL_POINT_SIZE_ARRAY_OES,1,type,stride,pointer);
}

GL_API void GL_APIENTRY  glPointSizex( GLfixed size) {
    GET_CTX()
    ctx->dispatcher().glPointSize(X2F(size));
}

GL_API void GL_APIENTRY  glPolygonOffset( GLfloat factor, GLfloat units) {
    GET_CTX()
    ctx->dispatcher().glPolygonOffset(factor,units);
}

GL_API void GL_APIENTRY  glPolygonOffsetx( GLfixed factor, GLfixed units) {
    GET_CTX()
    ctx->dispatcher().glPolygonOffset(X2F(factor),X2F(units));
}

GL_API void GL_APIENTRY  glPopMatrix(void) {
    GET_CTX()
    ctx->dispatcher().glPopMatrix();
}

GL_API void GL_APIENTRY  glPushMatrix(void) {
    GET_CTX()
    ctx->dispatcher().glPushMatrix();
}

GL_API void GL_APIENTRY  glReadPixels( GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid *pixels) {
    GET_CTX()
    SET_ERROR_IF(!(GLESvalidate::pixelFrmt(format) && GLESvalidate::pixelType(type)),GL_INVALID_ENUM);
    SET_ERROR_IF(!(GLESvalidate::pixelOp(format,type)),GL_INVALID_OPERATION);

    ctx->dispatcher().glReadPixels(x,y,width,height,format,type,pixels);
}

GL_API void GL_APIENTRY  glRotatef( GLfloat angle, GLfloat x, GLfloat y, GLfloat z) {
    GET_CTX()
    ctx->dispatcher().glRotatef(angle,x,y,z);
}

GL_API void GL_APIENTRY  glRotatex( GLfixed angle, GLfixed x, GLfixed y, GLfixed z) {
    GET_CTX()
    ctx->dispatcher().glRotatef(angle,X2F(x),X2F(y),X2F(z));
}

GL_API void GL_APIENTRY  glSampleCoverage( GLclampf value, GLboolean invert) {
    GET_CTX()
    ctx->dispatcher().glSampleCoverage(value,invert);
}

GL_API void GL_APIENTRY  glSampleCoveragex( GLclampx value, GLboolean invert) {
    GET_CTX()
    ctx->dispatcher().glSampleCoverage(X2F(value),invert);
}

GL_API void GL_APIENTRY  glScalef( GLfloat x, GLfloat y, GLfloat z) {
    GET_CTX()
    ctx->dispatcher().glScalef(x,y,z);
}

GL_API void GL_APIENTRY  glScalex( GLfixed x, GLfixed y, GLfixed z) {
    GET_CTX()
    ctx->dispatcher().glScalef(X2F(x),X2F(y),X2F(z));
}

GL_API void GL_APIENTRY  glScissor( GLint x, GLint y, GLsizei width, GLsizei height) {
    GET_CTX()
    ctx->dispatcher().glScissor(x,y,width,height);
}

GL_API void GL_APIENTRY  glShadeModel( GLenum mode) {
    GET_CTX()
    ctx->dispatcher().glShadeModel(mode);
}

GL_API void GL_APIENTRY  glStencilFunc( GLenum func, GLint ref, GLuint mask) {
    GET_CTX()
    ctx->dispatcher().glStencilFunc(func,ref,mask);
}

GL_API void GL_APIENTRY  glStencilMask( GLuint mask) {
    GET_CTX()
    ctx->dispatcher().glStencilMask(mask);
}

GL_API void GL_APIENTRY  glStencilOp( GLenum fail, GLenum zfail, GLenum zpass) {
    GET_CTX()
    ctx->dispatcher().glStencilOp(fail,zfail,zpass);
}

GL_API void GL_APIENTRY  glTexCoordPointer( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texCoordPointerParams(size,stride),GL_INVALID_VALUE);

    const GLvoid* data = ctx->setPointer(GL_TEXTURE_COORD_ARRAY,size,type,stride,pointer);
    if(type != GL_FIXED) ctx->dispatcher().glTexCoordPointer(size,type,stride,data);
}

GL_API void GL_APIENTRY  glTexEnvf( GLenum target, GLenum pname, GLfloat param) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texEnv(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexEnvf(target,pname,param);
}

GL_API void GL_APIENTRY  glTexEnvfv( GLenum target, GLenum pname, const GLfloat *params) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texEnv(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexEnvfv(target,pname,params);
}

GL_API void GL_APIENTRY  glTexEnvi( GLenum target, GLenum pname, GLint param) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texEnv(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexEnvi(target,pname,param);
}

GL_API void GL_APIENTRY  glTexEnviv( GLenum target, GLenum pname, const GLint *params) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texEnv(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexEnviv(target,pname,params);
}

GL_API void GL_APIENTRY  glTexEnvx( GLenum target, GLenum pname, GLfixed param) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texEnv(target,pname),GL_INVALID_ENUM);
    GLfloat tmpParam = static_cast<GLfloat>(param);
    ctx->dispatcher().glTexEnvf(target,pname,tmpParam);
}

GL_API void GL_APIENTRY  glTexEnvxv( GLenum target, GLenum pname, const GLfixed *params) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texEnv(target,pname),GL_INVALID_ENUM);

    GLfloat tmpParams[4];
    if(pname == GL_TEXTURE_ENV_COLOR) {
        for(int i =0;i<4;i++) {
            tmpParams[i] = X2F(params[i]);
        }
    } else {
        tmpParams[0] = static_cast<GLfloat>(params[0]);
    }
    ctx->dispatcher().glTexEnvfv(target,pname,tmpParams);
}

GL_API void GL_APIENTRY  glTexImage2D( GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid *pixels) {
    GET_CTX()

    SET_ERROR_IF(!(GLESvalidate::textureTarget(target) &&
                   GLESvalidate::pixelFrmt(internalformat) &&
                   GLESvalidate::pixelFrmt(format)&&
                   GLESvalidate::pixelType(type)),GL_INVALID_ENUM);

    //SET_ERROR_IF(level < 0 || border !=0 || level > log2(ctx->getMaxTexSize()) || !GLESvalidate::texImgDim(width,height,ctx->getMaxTexSize()),GL_INVALID_VALUE);
    SET_ERROR_IF(!(GLESvalidate::pixelOp(format,type) && internalformat == ((GLint)format)),GL_INVALID_OPERATION);

    ctx->dispatcher().glTexImage2D(target,level,internalformat,width,height,border,format,type,pixels);
}

GL_API void GL_APIENTRY  glTexParameterf( GLenum target, GLenum pname, GLfloat param) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texParams(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexParameterf(target,pname,param);
}

GL_API void GL_APIENTRY  glTexParameterfv( GLenum target, GLenum pname, const GLfloat *params) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texParams(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexParameterfv(target,pname,params);
}

GL_API void GL_APIENTRY  glTexParameteri( GLenum target, GLenum pname, GLint param) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texParams(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexParameteri(target,pname,param);
}

GL_API void GL_APIENTRY  glTexParameteriv( GLenum target, GLenum pname, const GLint *params) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texParams(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexParameteriv(target,pname,params);
}

GL_API void GL_APIENTRY  glTexParameterx( GLenum target, GLenum pname, GLfixed param) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texParams(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexParameterf(target,pname,static_cast<GLfloat>(param));
}

GL_API void GL_APIENTRY  glTexParameterxv( GLenum target, GLenum pname, const GLfixed *params) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::texParams(target,pname),GL_INVALID_ENUM);
    GLfloat param = static_cast<GLfloat>(params[0]);
    ctx->dispatcher().glTexParameterfv(target,pname,&param);
}

GL_API void GL_APIENTRY  glTexSubImage2D( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels) {
    GET_CTX()
    SET_ERROR_IF(!(GLESvalidate::textureTarget(target) &&
                   GLESvalidate::pixelFrmt(format)&&
                   GLESvalidate::pixelType(type)),GL_INVALID_ENUM);
    SET_ERROR_IF(!GLESvalidate::pixelOp(format,type),GL_INVALID_OPERATION);

    ctx->dispatcher().glTexSubImage2D(target,level,xoffset,yoffset,width,height,format,type,pixels);
}

GL_API void GL_APIENTRY  glTranslatef( GLfloat x, GLfloat y, GLfloat z) {
    GET_CTX()
    ctx->dispatcher().glTranslatef(x,y,z);
}

GL_API void GL_APIENTRY  glTranslatex( GLfixed x, GLfixed y, GLfixed z) {
    GET_CTX()
    ctx->dispatcher().glTranslatef(x,y,z);
}

GL_API void GL_APIENTRY  glVertexPointer( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer) {
    GET_CTX()
    SET_ERROR_IF(!GLESvalidate::vertexPointerParams(size,stride),GL_INVALID_VALUE);

    const GLvoid* data = ctx->setPointer(GL_VERTEX_ARRAY,size,type,stride,pointer);
    if(type != GL_FIXED) ctx->dispatcher().glVertexPointer(size,type,stride,data);
}

GL_API void GL_APIENTRY  glViewport( GLint x, GLint y, GLsizei width, GLsizei height) {
    GET_CTX()
    ctx->dispatcher().glViewport(x,y,width,height);
}
