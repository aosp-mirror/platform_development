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

#ifdef _WIN32
#undef GL_API
#define GL_API __declspec(dllexport)
#endif
#define GL_GLEXT_PROTOTYPES
#include "GLEScmContext.h"
#include "GLEScmValidate.h"
#include "GLEScmUtils.h"
#include "TextureUtils.h"

#include <stdio.h>
#include <GLcommon/gldefs.h>
#include <GLcommon/GLDispatch.h>
#include <GLcommon/GLfixed_ops.h>
#include <GLcommon/TranslatorIfaces.h>
#include <GLcommon/ThreadInfo.h>
#include <GLES/gl.h>
#include <GLES/glext.h>
#include <cmath>
#include <map>

extern "C" {

//decleration
static void initContext(GLEScontext* ctx);
static void deleteGLESContext(GLEScontext* ctx);
static void setShareGroup(GLEScontext* ctx,ShareGroupPtr grp);
static GLEScontext* createGLESContext();
static __translatorMustCastToProperFunctionPointerType getProcAddress(const char* procName);

}

/************************************** GLES EXTENSIONS *********************************************************/
//extentions descriptor
typedef std::map<std::string, __translatorMustCastToProperFunctionPointerType> ProcTableMap;
ProcTableMap *s_glesExtensions = NULL;
/****************************************************************************************************************/

static EGLiface*  s_eglIface = NULL;
static GLESiface  s_glesIface = {
    createGLESContext:createGLESContext,
    initContext      :initContext,
    deleteGLESContext:deleteGLESContext,
    flush            :(FUNCPTR)glFlush,
    finish           :(FUNCPTR)glFinish,
    setShareGroup    :setShareGroup,
    getProcAddress   :getProcAddress
};

#include <GLcommon/GLESmacros.h>

extern "C" {

static void initContext(GLEScontext* ctx) {
    ctx->init();
}

static GLEScontext* createGLESContext() {
    return new GLEScmContext();
}

static void deleteGLESContext(GLEScontext* ctx) {
    if(ctx) delete ctx;
}

static void setShareGroup(GLEScontext* ctx,ShareGroupPtr grp) {
    if(ctx) {
        ctx->setShareGroup(grp);
    }
}
static __translatorMustCastToProperFunctionPointerType getProcAddress(const char* procName) {
    GET_CTX_RET(NULL)
    ctx->getGlobalLock();
    static bool proc_table_initialized = false;
    if (!proc_table_initialized) {
        proc_table_initialized = true;
        if (!s_glesExtensions)
            s_glesExtensions = new ProcTableMap();
        else
            s_glesExtensions->clear();
        (*s_glesExtensions)["glEGLImageTargetTexture2DOES"] = (__translatorMustCastToProperFunctionPointerType)glEGLImageTargetTexture2DOES;
        (*s_glesExtensions)["glEGLImageTargetRenderbufferStorageOES"]=(__translatorMustCastToProperFunctionPointerType)glEGLImageTargetRenderbufferStorageOES;
        (*s_glesExtensions)["glBlendEquationSeparateOES"] = (__translatorMustCastToProperFunctionPointerType)glBlendEquationSeparateOES;
        (*s_glesExtensions)["glBlendFuncSeparateOES"] = (__translatorMustCastToProperFunctionPointerType)glBlendFuncSeparateOES;
        (*s_glesExtensions)["glBlendEquationOES"] = (__translatorMustCastToProperFunctionPointerType)glBlendEquationOES;

        if (ctx->getCaps()->GL_ARB_MATRIX_PALETTE && ctx->getCaps()->GL_ARB_VERTEX_BLEND) {
            (*s_glesExtensions)["glCurrentPaletteMatrixOES"] = (__translatorMustCastToProperFunctionPointerType)glCurrentPaletteMatrixOES;
            (*s_glesExtensions)["glLoadPaletteFromModelViewMatrixOES"]=(__translatorMustCastToProperFunctionPointerType)glLoadPaletteFromModelViewMatrixOES;
            (*s_glesExtensions)["glMatrixIndexPointerOES"] = (__translatorMustCastToProperFunctionPointerType)glMatrixIndexPointerOES;
            (*s_glesExtensions)["glWeightPointerOES"] = (__translatorMustCastToProperFunctionPointerType)glWeightPointerOES;
        }
        (*s_glesExtensions)["glDepthRangefOES"] = (__translatorMustCastToProperFunctionPointerType)glDepthRangef;
        (*s_glesExtensions)["glFrustumfOES"] = (__translatorMustCastToProperFunctionPointerType)glFrustumf;
        (*s_glesExtensions)["glOrthofOES"] = (__translatorMustCastToProperFunctionPointerType)glOrthof;
        (*s_glesExtensions)["glClipPlanefOES"] = (__translatorMustCastToProperFunctionPointerType)glClipPlanef;
        (*s_glesExtensions)["glGetClipPlanefOES"] = (__translatorMustCastToProperFunctionPointerType)glGetClipPlanef;
        (*s_glesExtensions)["glClearDepthfOES"] = (__translatorMustCastToProperFunctionPointerType)glClearDepthf;
        (*s_glesExtensions)["glPointSizePointerOES"] = (__translatorMustCastToProperFunctionPointerType)glPointSizePointerOES;
        (*s_glesExtensions)["glTexGenfOES"] = (__translatorMustCastToProperFunctionPointerType)glTexGenfOES;
        (*s_glesExtensions)["glTexGenfvOES"] = (__translatorMustCastToProperFunctionPointerType)glTexGenfvOES;
        (*s_glesExtensions)["glTexGeniOES"] = (__translatorMustCastToProperFunctionPointerType)glTexGeniOES;
        (*s_glesExtensions)["glTexGenivOES"] = (__translatorMustCastToProperFunctionPointerType)glTexGenivOES;
        (*s_glesExtensions)["glTexGenxOES"] = (__translatorMustCastToProperFunctionPointerType)glTexGenxOES;
        (*s_glesExtensions)["glTexGenxvOES"] = (__translatorMustCastToProperFunctionPointerType)glTexGenxvOES;
        (*s_glesExtensions)["glGetTexGenfvOES"] = (__translatorMustCastToProperFunctionPointerType)glGetTexGenfvOES;
        (*s_glesExtensions)["glGetTexGenivOES"] = (__translatorMustCastToProperFunctionPointerType)glGetTexGenivOES;
        (*s_glesExtensions)["glGetTexGenxvOES"] = (__translatorMustCastToProperFunctionPointerType)glGetTexGenxvOES;
        if (ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT) {
            (*s_glesExtensions)["glIsRenderbufferOES"] = (__translatorMustCastToProperFunctionPointerType)glIsRenderbufferOES;
            (*s_glesExtensions)["glBindRenderbufferOES"] = (__translatorMustCastToProperFunctionPointerType)glBindRenderbufferOES;
            (*s_glesExtensions)["glDeleteRenderbuffersOES"] = (__translatorMustCastToProperFunctionPointerType)glDeleteRenderbuffersOES;
            (*s_glesExtensions)["glGenRenderbuffersOES"] = (__translatorMustCastToProperFunctionPointerType)glGenRenderbuffersOES;
            (*s_glesExtensions)["glRenderbufferStorageOES"] = (__translatorMustCastToProperFunctionPointerType)glRenderbufferStorageOES;
            (*s_glesExtensions)["glGetRenderbufferParameterivOES"] = (__translatorMustCastToProperFunctionPointerType)glGetRenderbufferParameterivOES;
            (*s_glesExtensions)["glIsFramebufferOES"] = (__translatorMustCastToProperFunctionPointerType)glIsFramebufferOES;
            (*s_glesExtensions)["glBindFramebufferOES"] = (__translatorMustCastToProperFunctionPointerType)glBindFramebufferOES;
            (*s_glesExtensions)["glDeleteFramebuffersOES"] = (__translatorMustCastToProperFunctionPointerType)glDeleteFramebuffersOES;
            (*s_glesExtensions)["glGenFramebuffersOES"] = (__translatorMustCastToProperFunctionPointerType)glGenFramebuffersOES;
            (*s_glesExtensions)["glCheckFramebufferStatusOES"] = (__translatorMustCastToProperFunctionPointerType)glCheckFramebufferStatusOES;
            (*s_glesExtensions)["glFramebufferTexture2DOES"] = (__translatorMustCastToProperFunctionPointerType)glFramebufferTexture2DOES;
            (*s_glesExtensions)["glFramebufferRenderbufferOES"] = (__translatorMustCastToProperFunctionPointerType)glFramebufferRenderbufferOES;
            (*s_glesExtensions)["glGetFramebufferAttachmentParameterivOES"] = (__translatorMustCastToProperFunctionPointerType)glGetFramebufferAttachmentParameterivOES;
            (*s_glesExtensions)["glGenerateMipmapOES"] = (__translatorMustCastToProperFunctionPointerType)glGenerateMipmapOES;
        }
        (*s_glesExtensions)["glDrawTexsOES"] = (__translatorMustCastToProperFunctionPointerType)glDrawTexsOES;
        (*s_glesExtensions)["glDrawTexiOES"] = (__translatorMustCastToProperFunctionPointerType)glDrawTexiOES;
        (*s_glesExtensions)["glDrawTexfOES"] = (__translatorMustCastToProperFunctionPointerType)glDrawTexfOES;
        (*s_glesExtensions)["glDrawTexxOES"] = (__translatorMustCastToProperFunctionPointerType)glDrawTexxOES;
        (*s_glesExtensions)["glDrawTexsvOES"] = (__translatorMustCastToProperFunctionPointerType)glDrawTexsvOES;
        (*s_glesExtensions)["glDrawTexivOES"] = (__translatorMustCastToProperFunctionPointerType)glDrawTexivOES;
        (*s_glesExtensions)["glDrawTexfvOES"] = (__translatorMustCastToProperFunctionPointerType)glDrawTexfvOES;
        (*s_glesExtensions)["glDrawTexxvOES"] = (__translatorMustCastToProperFunctionPointerType)glDrawTexxvOES;
    }
    __translatorMustCastToProperFunctionPointerType ret=NULL;
    ProcTableMap::iterator val = s_glesExtensions->find(procName);
    if (val!=s_glesExtensions->end())
        ret = val->second;
    ctx->releaseGlobalLock();

    return ret;
}

GL_API GLESiface* __translator_getIfaces(EGLiface* eglIface){
    s_eglIface = eglIface;
    return & s_glesIface;
}

}

static TextureData* getTextureData(){
    GET_CTX_RET(NULL);
    unsigned int tex = ctx->getBindedTexture();
    TextureData *texData = NULL;
    ObjectDataPtr objData = thrd->shareGroup->getObjectData(TEXTURE,tex);
    if(!objData.Ptr()){
        texData = new TextureData();
        thrd->shareGroup->setObjectData(TEXTURE, tex, ObjectDataPtr(texData));
    } else {
        texData = (TextureData*)objData.Ptr();
    }
    return texData;
}

GL_API GLboolean GL_APIENTRY glIsBuffer(GLuint buffer) {
    GET_CTX_RET(GL_FALSE)

    if(buffer && thrd->shareGroup.Ptr()) {
       ObjectDataPtr objData = thrd->shareGroup->getObjectData(VERTEXBUFFER,buffer);
       return objData.Ptr() ? ((GLESbuffer*)objData.Ptr())->wasBinded():GL_FALSE;
    }
    return GL_FALSE;
}

GL_API GLboolean GL_APIENTRY  glIsEnabled( GLenum cap) {
    GET_CTX_CM_RET(GL_FALSE)
    RET_AND_SET_ERROR_IF(!GLEScmValidate::capability(cap,ctx->getMaxLights(),ctx->getMaxClipPlanes()),GL_INVALID_ENUM,GL_FALSE);

    if (cap == GL_POINT_SIZE_ARRAY_OES) 
        return ctx->isArrEnabled(cap);
    else if (cap==GL_TEXTURE_GEN_STR_OES)
        return (ctx->dispatcher().glIsEnabled(GL_TEXTURE_GEN_S) &&
                ctx->dispatcher().glIsEnabled(GL_TEXTURE_GEN_T) &&
                ctx->dispatcher().glIsEnabled(GL_TEXTURE_GEN_R));
    else
        return ctx->dispatcher().glIsEnabled(cap);
}

GL_API GLboolean GL_APIENTRY  glIsTexture( GLuint texture) {
    GET_CTX_RET(GL_FALSE)
    if(texture && thrd->shareGroup.Ptr()){
        return thrd->shareGroup->isObject(TEXTURE,texture) ? GL_TRUE :GL_FALSE;
    }
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

    switch(name) {
        case GL_VENDOR:
            return VENDOR;
        case GL_RENDERER:
            return RENDERER;
        case GL_VERSION:
            return VERSION;
        case GL_EXTENSIONS:
            return (const GLubyte*)ctx->getExtensionString();
        default:
            RET_AND_SET_ERROR_IF(true,GL_INVALID_ENUM,NULL);
    }
}

GL_API void GL_APIENTRY  glActiveTexture( GLenum texture) {
    GET_CTX_CM()
    SET_ERROR_IF(!GLEScmValidate::textureEnum(texture,ctx->getMaxTexUnits()),GL_INVALID_ENUM);
    ctx->setActiveTexture(texture);
    ctx->dispatcher().glActiveTexture(texture);
}

GL_API void GL_APIENTRY  glAlphaFunc( GLenum func, GLclampf ref) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::alphaFunc(func),GL_INVALID_ENUM);
    ctx->dispatcher().glAlphaFunc(func,ref);
}


GL_API void GL_APIENTRY  glAlphaFuncx( GLenum func, GLclampx ref) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::alphaFunc(func),GL_INVALID_ENUM);
    ctx->dispatcher().glAlphaFunc(func,X2F(ref));
}


GL_API void GL_APIENTRY  glBindBuffer( GLenum target, GLuint buffer) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::bufferTarget(target),GL_INVALID_ENUM);

    //if buffer wasn't generated before,generate one
    if(thrd->shareGroup.Ptr() && !thrd->shareGroup->isObject(VERTEXBUFFER,buffer)){
        thrd->shareGroup->genName(VERTEXBUFFER,buffer);
        thrd->shareGroup->setObjectData(VERTEXBUFFER,buffer,ObjectDataPtr(new GLESbuffer()));
    }
    ctx->bindBuffer(target,buffer);
    GLESbuffer* vbo = (GLESbuffer*)thrd->shareGroup->getObjectData(VERTEXBUFFER,buffer).Ptr();
    vbo->wasBinded();
}


GL_API void GL_APIENTRY  glBindTexture( GLenum target, GLuint texture) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::textureTarget(target),GL_INVALID_ENUM)

    GLuint globalTextureName = texture;
    if(texture && thrd->shareGroup.Ptr()){
        globalTextureName = thrd->shareGroup->getGlobalName(TEXTURE,texture);
        //if texture wasn't generated before,generate one
        if(!globalTextureName){
            thrd->shareGroup->genName(TEXTURE,texture);
            globalTextureName = thrd->shareGroup->getGlobalName(TEXTURE,texture);
        }
    }
    ctx->setBindedTexture(texture);
    ctx->dispatcher().glBindTexture(target,globalTextureName);
}

GL_API void GL_APIENTRY  glBlendFunc( GLenum sfactor, GLenum dfactor) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::blendSrc(sfactor) || !GLEScmValidate::blendDst(dfactor),GL_INVALID_ENUM)
    ctx->dispatcher().glBlendFunc(sfactor,dfactor);
}

GL_API void GL_APIENTRY  glBufferData( GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::bufferTarget(target),GL_INVALID_ENUM);
    SET_ERROR_IF(!ctx->isBindedBuffer(target),GL_INVALID_OPERATION);
    ctx->setBufferData(target,size,data,usage);
}

GL_API void GL_APIENTRY  glBufferSubData( GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data) {
    GET_CTX()
    SET_ERROR_IF(!ctx->isBindedBuffer(target),GL_INVALID_OPERATION);
    SET_ERROR_IF(!GLEScmValidate::bufferTarget(target),GL_INVALID_ENUM);
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
    ctx->dispatcher().glClearDepth(depth);
}

GL_API void GL_APIENTRY  glClearDepthx( GLclampx depth) {
    GET_CTX()
    ctx->dispatcher().glClearDepth(X2F(depth));
}

GL_API void GL_APIENTRY  glClearStencil( GLint s) {
    GET_CTX()
    ctx->dispatcher().glClearStencil(s);
}

GL_API void GL_APIENTRY  glClientActiveTexture( GLenum texture) {
    GET_CTX_CM()
    SET_ERROR_IF(!GLEScmValidate::textureEnum(texture,ctx->getMaxTexUnits()),GL_INVALID_ENUM);
    ctx->setClientActiveTexture(texture);
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
    SET_ERROR_IF(!GLEScmValidate::colorPointerParams(size,stride),GL_INVALID_VALUE);

    const GLvoid* data = ctx->setPointer(GL_COLOR_ARRAY,size,type,stride,pointer);
    if(type != GL_FIXED) ctx->dispatcher().glColorPointer(size,type,stride,data);
}

GL_API void GL_APIENTRY  glCompressedTexImage2D( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data) {
    GET_CTX_CM()
    SET_ERROR_IF(!(GLEScmValidate::texCompImgFrmt(internalformat) && GLEScmValidate::textureTargetEx(target)),GL_INVALID_ENUM);
    SET_ERROR_IF(level > log2(ctx->getMaxTexSize())|| border !=0 || level > 0 || !GLEScmValidate::texImgDim(width,height,ctx->getMaxTexSize()+2),GL_INVALID_VALUE)

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
}

GL_API void GL_APIENTRY  glCompressedTexSubImage2D( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid *data) {
    GET_CTX_CM()
    SET_ERROR_IF(!(GLEScmValidate::texCompImgFrmt(format) && GLEScmValidate::textureTargetEx(target)),GL_INVALID_ENUM);
    SET_ERROR_IF(level < 0 || level > log2(ctx->getMaxTexSize()),GL_INVALID_VALUE)

    GLenum uncompressedFrmt;
    unsigned char* uncompressed = uncompressTexture(format,uncompressedFrmt,width,height,imageSize,data,level);
    ctx->dispatcher().glTexSubImage2D(target,level,xoffset,yoffset,width,height,uncompressedFrmt,GL_UNSIGNED_BYTE,uncompressed);
    delete uncompressed;
}

GL_API void GL_APIENTRY  glCopyTexImage2D( GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border) {
    GET_CTX()
    SET_ERROR_IF(!(GLEScmValidate::pixelFrmt(ctx,internalformat) && GLEScmValidate::textureTargetEx(target)),GL_INVALID_ENUM);
    SET_ERROR_IF(border != 0,GL_INVALID_VALUE);
    ctx->dispatcher().glCopyTexImage2D(target,level,internalformat,x,y,width,height,border);
}

GL_API void GL_APIENTRY  glCopyTexSubImage2D( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::textureTargetEx(target),GL_INVALID_ENUM);
    ctx->dispatcher().glCopyTexSubImage2D(target,level,xoffset,yoffset,x,y,width,height);
}

GL_API void GL_APIENTRY  glCullFace( GLenum mode) {
    GET_CTX()
    ctx->dispatcher().glCullFace(mode);
}

GL_API void GL_APIENTRY  glDeleteBuffers( GLsizei n, const GLuint *buffers) {
    GET_CTX()
    SET_ERROR_IF(n<0,GL_INVALID_VALUE);
    if(thrd->shareGroup.Ptr()) {
        for(int i=0; i < n; i++){
           thrd->shareGroup->deleteName(VERTEXBUFFER,buffers[i]);
           ctx->unbindBuffer(buffers[i]);
        }
    }
}

GL_API void GL_APIENTRY  glDeleteTextures( GLsizei n, const GLuint *textures) {
    GET_CTX()
    SET_ERROR_IF(n<0,GL_INVALID_VALUE);
    if(thrd->shareGroup.Ptr()) {
        for(int i=0; i < n; i++){
           thrd->shareGroup->deleteName(TEXTURE,textures[i]);
           const GLuint globalTextureName = thrd->shareGroup->getGlobalName(TEXTURE,textures[i]);
           ctx->dispatcher().glDeleteTextures(1,&globalTextureName);
        }
    }
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
    if (cap==GL_TEXTURE_GEN_STR_OES) {
        ctx->dispatcher().glDisable(GL_TEXTURE_GEN_S);
        ctx->dispatcher().glDisable(GL_TEXTURE_GEN_T);
        ctx->dispatcher().glDisable(GL_TEXTURE_GEN_R);
    }
    ctx->dispatcher().glDisable(cap);
    if (cap==GL_TEXTURE_2D)
        ctx->setTextureEnabled(TEXTURE_2D,false);
    else if (cap==GL_TEXTURE_CUBE_MAP_OES)
        ctx->setTextureEnabled(TEXTURE_CUBE_MAP,false);
}

GL_API void GL_APIENTRY  glDisableClientState( GLenum array) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::supportedArrays(array),GL_INVALID_ENUM)

    ctx->enableArr(array,false);
    if(array != GL_POINT_SIZE_ARRAY_OES) ctx->dispatcher().glDisableClientState(array);
}


GL_API void GL_APIENTRY  glDrawArrays( GLenum mode, GLint first, GLsizei count) {
    GET_CTX_CM()
    SET_ERROR_IF(count < 0,GL_INVALID_VALUE)
    SET_ERROR_IF(!GLEScmValidate::drawMode(mode),GL_INVALID_ENUM)

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
    GET_CTX_CM()
    SET_ERROR_IF(count < 0,GL_INVALID_VALUE)
    SET_ERROR_IF((!GLEScmValidate::drawMode(mode) || !GLEScmValidate::drawType(type)),GL_INVALID_ENUM)
    if(!ctx->isArrEnabled(GL_VERTEX_ARRAY)) return;

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
    if (cap==GL_TEXTURE_GEN_STR_OES) {
        ctx->dispatcher().glEnable(GL_TEXTURE_GEN_S);
        ctx->dispatcher().glEnable(GL_TEXTURE_GEN_T);
        ctx->dispatcher().glEnable(GL_TEXTURE_GEN_R);
    }
    else
        ctx->dispatcher().glEnable(cap);
    if (cap==GL_TEXTURE_2D)
        ctx->setTextureEnabled(TEXTURE_2D,true);
    else if (cap==GL_TEXTURE_CUBE_MAP_OES)
        ctx->setTextureEnabled(TEXTURE_CUBE_MAP,true);
}

GL_API void GL_APIENTRY  glEnableClientState( GLenum array) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::supportedArrays(array),GL_INVALID_ENUM)

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
    if(thrd->shareGroup.Ptr()) {
        for(int i=0; i<n ;i++) {
            buffers[i] = thrd->shareGroup->genName(VERTEXBUFFER);
            //generating vbo object related to this buffer name
            thrd->shareGroup->setObjectData(VERTEXBUFFER,buffers[i],ObjectDataPtr(new GLESbuffer()));
        }
    }
}

GL_API void GL_APIENTRY  glGenTextures( GLsizei n, GLuint *textures) {
    GET_CTX();
    if(thrd->shareGroup.Ptr()) {
        for(int i=0; i<n ;i++) {
            textures[i] = thrd->shareGroup->genName(TEXTURE);
        }
    }
}

GL_API void GL_APIENTRY  glGetBooleanv( GLenum pname, GLboolean *params) {
    GET_CTX()

    GLint i;

    switch(pname)
    {
    case GL_IMPLEMENTATION_COLOR_READ_TYPE_OES:
    case GL_IMPLEMENTATION_COLOR_READ_FORMAT_OES:
    case GL_NUM_COMPRESSED_TEXTURE_FORMATS:
        glGetIntegerv(pname, &i);
        *params = (i != 0) ? GL_TRUE : GL_FALSE;
        break;

    case GL_COMPRESSED_TEXTURE_FORMATS:
        glGetIntegerv(GL_NUM_COMPRESSED_TEXTURE_FORMATS, &i);
        *params = (i != 0) ? GL_TRUE : GL_FALSE;
        break;

    case GL_TEXTURE_GEN_STR_OES:
        {
            GLboolean state_s = GL_FALSE;
            GLboolean state_t = GL_FALSE;
            GLboolean state_r = GL_FALSE;
            ctx->dispatcher().glGetBooleanv(GL_TEXTURE_GEN_S,&state_s);
            ctx->dispatcher().glGetBooleanv(GL_TEXTURE_GEN_T,&state_t);
            ctx->dispatcher().glGetBooleanv(GL_TEXTURE_GEN_R,&state_r);
            *params = state_s && state_t && state_r ? GL_TRUE: GL_FALSE;
        }
        break;

    default:
        ctx->dispatcher().glGetBooleanv(pname,params);
    }
}

GL_API void GL_APIENTRY  glGetBufferParameteriv( GLenum target, GLenum pname, GLint *params) {
    GET_CTX()
    SET_ERROR_IF(!(GLEScmValidate::bufferTarget(target) && GLEScmValidate::bufferParam(pname)),GL_INVALID_ENUM);
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
    GLint i;

    switch(pname)
    {
    case GL_IMPLEMENTATION_COLOR_READ_TYPE_OES:
    case GL_IMPLEMENTATION_COLOR_READ_FORMAT_OES:
    case GL_NUM_COMPRESSED_TEXTURE_FORMATS:
        glGetIntegerv(pname, &i);
        *params = I2X(i);
        nParams = 0;
        break;

    case GL_COMPRESSED_TEXTURE_FORMATS:
        glGetIntegerv(GL_NUM_COMPRESSED_TEXTURE_FORMATS, &i);
        if(i > 0)
        {
            GLint* iParams = new GLint[i];
            glGetIntegerv(GL_COMPRESSED_TEXTURE_FORMATS, iParams);
            while(i >= 0)
            {
                params[i] = I2X(iParams[i]);
                i--;
            }
            delete [] iParams;
        }
        break;

    case GL_TEXTURE_GEN_STR_OES:
        ctx->dispatcher().glGetFloatv(GL_TEXTURE_GEN_S,&fParams[0]);
        break;

    default:
        ctx->dispatcher().glGetFloatv(pname,fParams);
    }

    if (nParams)
    {
        for(size_t i =0 ; i < nParams;i++) {
            params[i] = F2X(fParams[i]);
        }
    }
}

GL_API void GL_APIENTRY  glGetFloatv( GLenum pname, GLfloat *params) {
    GET_CTX()

    GLint i;

    switch(pname)
    {
    case GL_IMPLEMENTATION_COLOR_READ_TYPE_OES:
    case GL_IMPLEMENTATION_COLOR_READ_FORMAT_OES:
    case GL_NUM_COMPRESSED_TEXTURE_FORMATS:
        glGetIntegerv(pname, &i);
        *params = (GLfloat)i;
        break;

    case GL_COMPRESSED_TEXTURE_FORMATS:
        glGetIntegerv(GL_NUM_COMPRESSED_TEXTURE_FORMATS, &i);
        if(i > 0)
        {
            GLint* iParams = new GLint[i];
            glGetIntegerv(GL_COMPRESSED_TEXTURE_FORMATS, iParams);
            while(i >= 0)
            {
                params[i] = (GLfloat)iParams[i];
                i--;
            }
            delete [] iParams;
        }
        break;

    case GL_TEXTURE_GEN_STR_OES:
        ctx->dispatcher().glGetFloatv(GL_TEXTURE_GEN_S,&params[0]);
        break;

    default:
        ctx->dispatcher().glGetFloatv(pname,params);
    }
}

GL_API void GL_APIENTRY  glGetIntegerv( GLenum pname, GLint *params) {
    GET_CTX()
    switch(pname)
    {
    case GL_IMPLEMENTATION_COLOR_READ_TYPE_OES:
        *params = GL_UNSIGNED_BYTE;
        break;

    case GL_IMPLEMENTATION_COLOR_READ_FORMAT_OES:
        *params = GL_RGBA;
        break;

    case GL_TEXTURE_GEN_STR_OES:
        ctx->dispatcher().glGetIntegerv(GL_TEXTURE_GEN_S,&params[0]);
        break;

    case GL_COMPRESSED_TEXTURE_FORMATS:
        getCompressedFormats(params);
        break;

    case GL_NUM_COMPRESSED_TEXTURE_FORMATS:
        *params = getCompressedFormats(NULL);
        break;

    default:
        ctx->dispatcher().glGetIntegerv(pname,params);
    }
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
        case GL_SPOT_EXPONENT:
        case GL_SPOT_CUTOFF:
        case GL_CONSTANT_ATTENUATION:
        case GL_LINEAR_ATTENUATION:
        case GL_QUADRATIC_ATTENUATION:
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
        break;
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
   if (pname==GL_TEXTURE_CROP_RECT_OES) {
      TextureData *texData = getTextureData();
      SET_ERROR_IF(texData==NULL,GL_INVALID_OPERATION);
      for (int i=0;i<4;++i)
        params[i] = texData->crop_rect[i];
    }
    else {
      ctx->dispatcher().glGetTexParameterfv(target,pname,params);
    }
}

GL_API void GL_APIENTRY  glGetTexParameteriv( GLenum target, GLenum pname, GLint *params) {
    GET_CTX()
    if (pname==GL_TEXTURE_CROP_RECT_OES) {
      TextureData *texData = getTextureData();
      SET_ERROR_IF(texData==NULL,GL_INVALID_OPERATION);
      for (int i=0;i<4;++i)
        params[i] = texData->crop_rect[i];
    }
    else {
      ctx->dispatcher().glGetTexParameteriv(target,pname,params);
    }
}

GL_API void GL_APIENTRY  glGetTexParameterxv( GLenum target, GLenum pname, GLfixed *params) {
    GET_CTX()
    if (pname==GL_TEXTURE_CROP_RECT_OES) {
      TextureData *texData = getTextureData();
      SET_ERROR_IF(texData==NULL,GL_INVALID_OPERATION);
      for (int i=0;i<4;++i)
        params[i] = F2X(texData->crop_rect[i]);
    }
    else {
      GLfloat tmpParam;
      ctx->dispatcher().glGetTexParameterfv(target,pname,&tmpParam);
      params[0] = static_cast<GLfixed>(tmpParam);
    }
}

GL_API void GL_APIENTRY  glHint( GLenum target, GLenum mode) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::hintTargetMode(target,mode),GL_INVALID_ENUM);
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
        case GL_EMISSION:
        case GL_POSITION:
            tmpParams[3] = X2F(params[3]);
        case GL_SPOT_DIRECTION:
            tmpParams[2] = X2F(params[2]);
            tmpParams[1] = X2F(params[1]);
        case GL_SPOT_EXPONENT:
        case GL_SPOT_CUTOFF:
        case GL_CONSTANT_ATTENUATION:
        case GL_LINEAR_ATTENUATION:
        case GL_QUADRATIC_ATTENUATION:
            tmpParams[0] = X2F(params[0]);
            break;
        default: {
                ctx->setGLerror(GL_INVALID_ENUM);
                return;
            }
    }
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
    GET_CTX_CM()
    SET_ERROR_IF(!GLEScmValidate::textureEnum(target,ctx->getMaxTexUnits()),GL_INVALID_ENUM);
    ctx->dispatcher().glMultiTexCoord4f(target,s,t,r,q);
}

GL_API void GL_APIENTRY  glMultiTexCoord4x( GLenum target, GLfixed s, GLfixed t, GLfixed r, GLfixed q) {
    GET_CTX_CM()
    SET_ERROR_IF(!GLEScmValidate::textureEnum(target,ctx->getMaxTexUnits()),GL_INVALID_ENUM);
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

    GLfloat tmpParam = X2F(*params) ;
    ctx->dispatcher().glPointParameterfv(pname,&tmpParam);
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
    SET_ERROR_IF(!(GLEScmValidate::pixelFrmt(ctx,format) && GLEScmValidate::pixelType(ctx,type)),GL_INVALID_ENUM);
    SET_ERROR_IF(!(GLEScmValidate::pixelOp(format,type)),GL_INVALID_OPERATION);

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
    SET_ERROR_IF(!(GLEScmValidate::stencilOp(fail) && GLEScmValidate::stencilOp(zfail) && GLEScmValidate::stencilOp(zpass)),GL_INVALID_ENUM);
    ctx->dispatcher().glStencilOp(fail,zfail,zpass);
}

GL_API void GL_APIENTRY  glTexCoordPointer( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texCoordPointerParams(size,stride),GL_INVALID_VALUE);

    const GLvoid* data = ctx->setPointer(GL_TEXTURE_COORD_ARRAY,size,type,stride,pointer);
    if(type != GL_FIXED) ctx->dispatcher().glTexCoordPointer(size,type,stride,data);
}

GL_API void GL_APIENTRY  glTexEnvf( GLenum target, GLenum pname, GLfloat param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texEnv(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexEnvf(target,pname,param);
}

GL_API void GL_APIENTRY  glTexEnvfv( GLenum target, GLenum pname, const GLfloat *params) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texEnv(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexEnvfv(target,pname,params);
}

GL_API void GL_APIENTRY  glTexEnvi( GLenum target, GLenum pname, GLint param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texEnv(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexEnvi(target,pname,param);
}

GL_API void GL_APIENTRY  glTexEnviv( GLenum target, GLenum pname, const GLint *params) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texEnv(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexEnviv(target,pname,params);
}

GL_API void GL_APIENTRY  glTexEnvx( GLenum target, GLenum pname, GLfixed param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texEnv(target,pname),GL_INVALID_ENUM);
    GLfloat tmpParam = static_cast<GLfloat>(param);
    ctx->dispatcher().glTexEnvf(target,pname,tmpParam);
}

GL_API void GL_APIENTRY  glTexEnvxv( GLenum target, GLenum pname, const GLfixed *params) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texEnv(target,pname),GL_INVALID_ENUM);

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

    SET_ERROR_IF(!(GLEScmValidate::textureTargetEx(target) &&
                     GLEScmValidate::pixelFrmt(ctx,internalformat) &&
                     GLEScmValidate::pixelFrmt(ctx,format) &&
                     GLEScmValidate::pixelType(ctx,type)),GL_INVALID_ENUM);

    SET_ERROR_IF(!(GLEScmValidate::pixelOp(format,type) && internalformat == ((GLint)format)),GL_INVALID_OPERATION);

    if (thrd->shareGroup.Ptr()){
        TextureData *texData = getTextureData();
        SET_ERROR_IF(texData==NULL,GL_INVALID_OPERATION);
        if(texData) {
            texData->width = width;
            texData->height = height;
            texData->border = border;
            texData->internalFormat = internalformat;
        }
    }
    ctx->dispatcher().glTexImage2D(target,level,internalformat,width,height,border,format,type,pixels);
}

GL_API void GL_APIENTRY  glTexParameterf( GLenum target, GLenum pname, GLfloat param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texParams(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexParameterf(target,pname,param);
}

GL_API void GL_APIENTRY  glTexParameterfv( GLenum target, GLenum pname, const GLfloat *params) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texParams(target,pname),GL_INVALID_ENUM);
    if (pname==GL_TEXTURE_CROP_RECT_OES) {
        TextureData *texData = getTextureData();
        SET_ERROR_IF(texData==NULL,GL_INVALID_OPERATION);
        for (int i=0;i<4;++i)
            texData->crop_rect[i] = params[i];
    }
    else {
        ctx->dispatcher().glTexParameterfv(target,pname,params);
    }
}

GL_API void GL_APIENTRY  glTexParameteri( GLenum target, GLenum pname, GLint param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texParams(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexParameteri(target,pname,param);
}

GL_API void GL_APIENTRY  glTexParameteriv( GLenum target, GLenum pname, const GLint *params) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texParams(target,pname),GL_INVALID_ENUM);
    if (pname==GL_TEXTURE_CROP_RECT_OES) {
        TextureData *texData = getTextureData();
        SET_ERROR_IF(texData==NULL,GL_INVALID_OPERATION);
        for (int i=0;i<4;++i)
            texData->crop_rect[i] = params[i];
    }
    else {
        ctx->dispatcher().glTexParameteriv(target,pname,params);
    }
}

GL_API void GL_APIENTRY  glTexParameterx( GLenum target, GLenum pname, GLfixed param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texParams(target,pname),GL_INVALID_ENUM);
    ctx->dispatcher().glTexParameterf(target,pname,static_cast<GLfloat>(param));
}

GL_API void GL_APIENTRY  glTexParameterxv( GLenum target, GLenum pname, const GLfixed *params) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texParams(target,pname),GL_INVALID_ENUM);
    if (pname==GL_TEXTURE_CROP_RECT_OES) {
        TextureData *texData = getTextureData();
        SET_ERROR_IF(texData==NULL,GL_INVALID_OPERATION);
        for (int i=0;i<4;++i)
            texData->crop_rect[i] = X2F(params[i]);
    }
    else {
        GLfloat param = static_cast<GLfloat>(params[0]);
        ctx->dispatcher().glTexParameterfv(target,pname,&param);
    }
}

GL_API void GL_APIENTRY  glTexSubImage2D( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels) {
    GET_CTX()
    SET_ERROR_IF(!(GLEScmValidate::textureTargetEx(target) &&
                   GLEScmValidate::pixelFrmt(ctx,format)&&
                   GLEScmValidate::pixelType(ctx,type)),GL_INVALID_ENUM);
    SET_ERROR_IF(!GLEScmValidate::pixelOp(format,type),GL_INVALID_OPERATION);

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
    SET_ERROR_IF(!GLEScmValidate::vertexPointerParams(size,stride),GL_INVALID_VALUE);

    const GLvoid* data = ctx->setPointer(GL_VERTEX_ARRAY,size,type,stride,pointer);
    if(type != GL_FIXED) ctx->dispatcher().glVertexPointer(size,type,stride,data);
}

GL_API void GL_APIENTRY  glViewport( GLint x, GLint y, GLsizei width, GLsizei height) {
    GET_CTX()
    ctx->dispatcher().glViewport(x,y,width,height);
}

GL_API void GL_APIENTRY glEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image)
{
    GET_CTX();
    SET_ERROR_IF(!GLEScmValidate::textureTargetLimited(target),GL_INVALID_ENUM);
    EglImage *img = s_eglIface->eglAttachEGLImage((unsigned int)image);
    if (img) {
        // Create the texture object in the underlying EGL implementation,
        // flag to the OpenGL layer to skip the image creation and map the
        // current binded texture object to the existing global object.
        if (thrd->shareGroup.Ptr()) {
            unsigned int tex = ctx->getBindedTexture();
            unsigned int oldGlobal = thrd->shareGroup->getGlobalName(TEXTURE, tex);
            // Delete old texture object
            if (oldGlobal) {
                ctx->dispatcher().glDeleteTextures(1, &oldGlobal);
            }
            // replace mapping and bind the new global object
            thrd->shareGroup->replaceGlobalName(TEXTURE, tex,img->globalTexName);
            ctx->dispatcher().glBindTexture(GL_TEXTURE_2D, img->globalTexName);
            TextureData *texData = getTextureData();
            SET_ERROR_IF(texData==NULL,GL_INVALID_OPERATION);
            texData->sourceEGLImage = (unsigned int)image;
            texData->eglImageDetach = s_eglIface->eglDetachEGLImage;
        }
    }
}

GL_API void GL_APIENTRY glEGLImageTargetRenderbufferStorageOES(GLenum target, GLeglImageOES image)
{
    GET_CTX()
    //not supported by EGL
    SET_ERROR_IF(false,GL_INVALID_OPERATION); 
}

/* GL_OES_blend_subtract*/
GL_API void GL_APIENTRY glBlendEquationOES(GLenum mode) {
    GET_CTX()
    SET_ERROR_IF(!(GLEScmValidate::blendEquationMode(mode)), GL_INVALID_ENUM);
    ctx->dispatcher().glBlendEquation(mode);   
}

/* GL_OES_blend_equation_separate */
GL_API void GL_APIENTRY glBlendEquationSeparateOES (GLenum modeRGB, GLenum modeAlpha) {
    GET_CTX()
    SET_ERROR_IF(!(GLEScmValidate::blendEquationMode(modeRGB) && GLEScmValidate::blendEquationMode(modeAlpha)), GL_INVALID_ENUM);
    ctx->dispatcher().glBlendEquationSeparate(modeRGB,modeAlpha);   
}

/* GL_OES_blend_func_separate */
GL_API void GL_APIENTRY glBlendFuncSeparateOES(GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::blendSrc(srcRGB) || !GLEScmValidate::blendDst(dstRGB) ||
                 !GLEScmValidate::blendSrc(srcAlpha) || ! GLEScmValidate::blendDst(dstAlpha) ,GL_INVALID_ENUM);
    ctx->dispatcher().glBlendFuncSeparate(srcRGB,dstRGB,srcAlpha,dstAlpha);
}

/* GL_OES_framebuffer_object */
GL_API GLboolean GL_APIENTRY glIsRenderbufferOES(GLuint renderbuffer) {
    GET_CTX_RET(GL_FALSE)
    RET_AND_SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION,GL_FALSE);
    if(renderbuffer && thrd->shareGroup.Ptr()){
        return thrd->shareGroup->isObject(RENDERBUFFER,renderbuffer) ? GL_TRUE :GL_FALSE;
    }
    return ctx->dispatcher().glIsRenderbufferEXT(renderbuffer);
}

GL_API void GLAPIENTRY glBindRenderbufferOES(GLenum target, GLuint renderbuffer) {
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION);
    SET_ERROR_IF(!GLEScmValidate::renderbufferTarget(target),GL_INVALID_ENUM);

    //if buffer wasn't generated before,generate one
    if(thrd->shareGroup.Ptr() && !thrd->shareGroup->isObject(RENDERBUFFER,renderbuffer)){
        thrd->shareGroup->genName(RENDERBUFFER,renderbuffer);
    }

    int globalBufferName = thrd->shareGroup->getGlobalName(RENDERBUFFER,renderbuffer);
    ctx->dispatcher().glBindRenderbufferEXT(target,globalBufferName); 
}

GL_API void GLAPIENTRY glDeleteRenderbuffersOES(GLsizei n, const GLuint *renderbuffers) {
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION);
    for (int i=0;i<n;++i) {
        GLuint globalBufferName = thrd->shareGroup->getGlobalName(RENDERBUFFER,renderbuffers[i]);
        ctx->dispatcher().glDeleteRenderbuffersEXT(1,&globalBufferName);
    }
}

GL_API void GLAPIENTRY glGenRenderbuffersOES(GLsizei n, GLuint *renderbuffers) {
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION);
    SET_ERROR_IF(n<0,GL_INVALID_VALUE);
    if(thrd->shareGroup.Ptr()) {
        for(int i=0; i<n ;i++) {
            renderbuffers[i] = thrd->shareGroup->genName(RENDERBUFFER);
        }
    }
}

GL_API void GLAPIENTRY glRenderbufferStorageOES(GLenum target, GLenum internalformat, GLsizei width, GLsizei height){
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION);
    SET_ERROR_IF(!GLEScmValidate::renderbufferTarget(target) || !GLEScmValidate::renderbufferInternalFrmt(ctx,internalformat) ,GL_INVALID_ENUM);
    if (internalformat==GL_RGB565_OES) //RGB565 not supported by GL
        internalformat = GL_RGB8_OES;
    ctx->dispatcher().glRenderbufferStorageEXT(target,internalformat,width,height);
}

GL_API void GLAPIENTRY glGetRenderbufferParameterivOES(GLenum target, GLenum pname, GLint* params) {
    GET_CTX() 
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION);
    SET_ERROR_IF(!GLEScmValidate::renderbufferTarget(target) || !GLEScmValidate::renderbufferParams(pname) ,GL_INVALID_ENUM);
    ctx->dispatcher().glGetRenderbufferParameterivEXT(target,pname,params);
}

GL_API GLboolean GLAPIENTRY glIsFramebufferOES(GLuint framebuffer) {
    GET_CTX_RET(GL_FALSE)
    RET_AND_SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION,GL_FALSE);
    if (framebuffer && thrd->shareGroup.Ptr()) {
        return thrd->shareGroup->isObject(FRAMEBUFFER,framebuffer) ? GL_TRUE : GL_FALSE;
    }
    return ctx->dispatcher().glIsFramebufferEXT(framebuffer);
}

GL_API void GLAPIENTRY glBindFramebufferOES(GLenum target, GLuint framebuffer) {
    GET_CTX() 
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION); 
    SET_ERROR_IF(!GLEScmValidate::framebufferTarget(target) ,GL_INVALID_ENUM);
    if (thrd->shareGroup.Ptr() && !thrd->shareGroup->isObject(FRAMEBUFFER,framebuffer)) {
        thrd->shareGroup->genName(FRAMEBUFFER,framebuffer);
    }
    int globalBufferName = thrd->shareGroup->getGlobalName(FRAMEBUFFER,framebuffer);
    ctx->dispatcher().glBindFramebufferEXT(target,globalBufferName);
}

GL_API void GLAPIENTRY glDeleteFramebuffersOES(GLsizei n, const GLuint *framebuffers) {
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION); 
    for (int i=0;i<n;++i) {
        GLuint globalBufferName = thrd->shareGroup->getGlobalName(FRAMEBUFFER,framebuffers[i]);
        ctx->dispatcher().glDeleteFramebuffersEXT(1,&globalBufferName);
    }
}

GL_API void GLAPIENTRY glGenFramebuffersOES(GLsizei n, GLuint *framebuffers) { 
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION); 
    SET_ERROR_IF(n<0,GL_INVALID_VALUE);
    if (thrd->shareGroup.Ptr()) {
        for (int i=0;i<n;i++) {
            framebuffers[i] = thrd->shareGroup->genName(FRAMEBUFFER);
        }
    }
}

GL_API GLenum GLAPIENTRY glCheckFramebufferStatusOES(GLenum target) {
    GET_CTX_RET(0) 
    RET_AND_SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION,0); 
    RET_AND_SET_ERROR_IF(!GLEScmValidate::framebufferTarget(target) ,GL_INVALID_ENUM,0);
    return ctx->dispatcher().glCheckFramebufferStatusEXT(target);
}

GL_API void GLAPIENTRY glFramebufferTexture2DOES(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level) {
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION); 
    SET_ERROR_IF(!GLEScmValidate::framebufferTarget(target) || !GLEScmValidate::framebufferAttachment(attachment) ||
                 !GLEScmValidate::textureTargetEx(textarget),GL_INVALID_ENUM);
    if (thrd->shareGroup.Ptr() && !thrd->shareGroup->isObject(TEXTURE,texture)) {
        thrd->shareGroup->genName(TEXTURE,texture);
    }
    GLuint globalTexName = thrd->shareGroup->getGlobalName(TEXTURE,texture);
    ctx->dispatcher().glFramebufferTexture2DEXT(target,attachment,textarget,globalTexName,level);
}

GL_API void GLAPIENTRY glFramebufferRenderbufferOES(GLenum target, GLenum attachment,GLenum renderbuffertarget, GLuint renderbuffer) {
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION);
    SET_ERROR_IF(!GLEScmValidate::framebufferTarget(target) || !GLEScmValidate::framebufferAttachment(attachment) ||
                 !GLEScmValidate::renderbufferTarget(renderbuffertarget), GL_INVALID_ENUM);
    if (thrd->shareGroup.Ptr() && !thrd->shareGroup->isObject(RENDERBUFFER,renderbuffer)) {
        thrd->shareGroup->genName(RENDERBUFFER,renderbuffer);
    }
    GLuint globalBufferName = thrd->shareGroup->getGlobalName(RENDERBUFFER,renderbuffer);
    ctx->dispatcher().glFramebufferRenderbufferEXT(target,attachment,renderbuffertarget,globalBufferName);
}

GL_API void GLAPIENTRY glGetFramebufferAttachmentParameterivOES(GLenum target, GLenum attachment, GLenum pname, GLint *params) {
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION);
    SET_ERROR_IF(!GLEScmValidate::framebufferTarget(target) || !GLEScmValidate::framebufferAttachment(attachment) ||
                 !GLEScmValidate::framebufferAttachmentParams(pname), GL_INVALID_ENUM);
    ctx->dispatcher().glGetFramebufferAttachmentParameterivEXT(target,attachment,pname,params);
}

GL_API void GL_APIENTRY glGenerateMipmapOES(GLenum target) {
    GET_CTX()
    SET_ERROR_IF(!ctx->getCaps()->GL_EXT_FRAMEBUFFER_OBJECT,GL_INVALID_OPERATION); 
    SET_ERROR_IF(!GLEScmValidate::textureTargetLimited(target),GL_INVALID_ENUM);
    ctx->dispatcher().glGenerateMipmapEXT(target);
}

GL_API void GL_APIENTRY glCurrentPaletteMatrixOES(GLuint index) {
    GET_CTX()
    SET_ERROR_IF(!(ctx->getCaps()->GL_ARB_MATRIX_PALETTE && ctx->getCaps()->GL_ARB_VERTEX_BLEND),GL_INVALID_OPERATION); 
    ctx->dispatcher().glCurrentPaletteMatrixARB(index);
}

GL_API void GL_APIENTRY glLoadPaletteFromModelViewMatrixOES() {
    GET_CTX()
    SET_ERROR_IF(!(ctx->getCaps()->GL_ARB_MATRIX_PALETTE && ctx->getCaps()->GL_ARB_VERTEX_BLEND),GL_INVALID_OPERATION);
    GLint matrix[16];
    ctx->dispatcher().glGetIntegerv(GL_MODELVIEW_MATRIX,matrix);
    ctx->dispatcher().glMatrixIndexuivARB(1,(GLuint*)matrix);

}

GL_API void GL_APIENTRY glMatrixIndexPointerOES(GLint size, GLenum type, GLsizei stride, const GLvoid *pointer) {
    GET_CTX()
    SET_ERROR_IF(!(ctx->getCaps()->GL_ARB_MATRIX_PALETTE && ctx->getCaps()->GL_ARB_VERTEX_BLEND),GL_INVALID_OPERATION); 
    ctx->dispatcher().glMatrixIndexPointerARB(size,type,stride,pointer);
}

GL_API void GL_APIENTRY glWeightPointerOES(GLint size, GLenum type, GLsizei stride, const GLvoid *pointer) {
    GET_CTX()
    SET_ERROR_IF(!(ctx->getCaps()->GL_ARB_MATRIX_PALETTE && ctx->getCaps()->GL_ARB_VERTEX_BLEND),GL_INVALID_OPERATION); 
    ctx->dispatcher().glWeightPointerARB(size,type,stride,pointer);

}

GL_API void GL_APIENTRY glTexGenfOES (GLenum coord, GLenum pname, GLfloat param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texGen(coord,pname),GL_INVALID_ENUM);
    if (coord == GL_TEXTURE_GEN_STR_OES) {
        ctx->dispatcher().glTexGenf(GL_TEXTURE_GEN_S,pname,param);
        ctx->dispatcher().glTexGenf(GL_TEXTURE_GEN_T,pname,param);
        ctx->dispatcher().glTexGenf(GL_TEXTURE_GEN_R,pname,param); 
    }
    else
        ctx->dispatcher().glTexGenf(coord,pname,param);
}

GL_API void GL_APIENTRY glTexGenfvOES (GLenum coord, GLenum pname, const GLfloat *params) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texGen(coord,pname),GL_INVALID_ENUM);
    if (coord == GL_TEXTURE_GEN_STR_OES) {
        ctx->dispatcher().glTexGenfv(GL_TEXTURE_GEN_S,pname,params);
        ctx->dispatcher().glTexGenfv(GL_TEXTURE_GEN_T,pname,params);
        ctx->dispatcher().glTexGenfv(GL_TEXTURE_GEN_R,pname,params);
    }
    else
        ctx->dispatcher().glTexGenfv(coord,pname,params);
}
GL_API void GL_APIENTRY glTexGeniOES (GLenum coord, GLenum pname, GLint param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texGen(coord,pname),GL_INVALID_ENUM);
    if (coord == GL_TEXTURE_GEN_STR_OES) {
        ctx->dispatcher().glTexGeni(GL_TEXTURE_GEN_S,pname,param);
        ctx->dispatcher().glTexGeni(GL_TEXTURE_GEN_T,pname,param);
        ctx->dispatcher().glTexGeni(GL_TEXTURE_GEN_R,pname,param); 
    }
    else
        ctx->dispatcher().glTexGeni(coord,pname,param);
}
GL_API void GL_APIENTRY glTexGenivOES (GLenum coord, GLenum pname, const GLint *params) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texGen(coord,pname),GL_INVALID_ENUM);
    if (coord == GL_TEXTURE_GEN_STR_OES) {
        ctx->dispatcher().glTexGeniv(GL_TEXTURE_GEN_S,pname,params);
        ctx->dispatcher().glTexGeniv(GL_TEXTURE_GEN_T,pname,params);
        ctx->dispatcher().glTexGeniv(GL_TEXTURE_GEN_R,pname,params); 
    }
    else
        ctx->dispatcher().glTexGeniv(coord,pname,params);
}
GL_API void GL_APIENTRY glTexGenxOES (GLenum coord, GLenum pname, GLfixed param) {
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texGen(coord,pname),GL_INVALID_ENUM);
    if (coord == GL_TEXTURE_GEN_STR_OES) {
        ctx->dispatcher().glTexGenf(GL_TEXTURE_GEN_S,pname,X2F(param));
        ctx->dispatcher().glTexGenf(GL_TEXTURE_GEN_T,pname,X2F(param));
        ctx->dispatcher().glTexGenf(GL_TEXTURE_GEN_R,pname,X2F(param)); 
    }
    else
        ctx->dispatcher().glTexGenf(coord,pname,X2F(param));
}
GL_API void GL_APIENTRY glTexGenxvOES (GLenum coord, GLenum pname, const GLfixed *params) {
    GLfloat tmpParams[1];
    GET_CTX()
    SET_ERROR_IF(!GLEScmValidate::texGen(coord,pname),GL_INVALID_ENUM);
    tmpParams[0] = X2F(params[0]);
    if (coord == GL_TEXTURE_GEN_STR_OES) {
        ctx->dispatcher().glTexGenfv(GL_TEXTURE_GEN_S,pname,tmpParams);
        ctx->dispatcher().glTexGenfv(GL_TEXTURE_GEN_T,pname,tmpParams);
        ctx->dispatcher().glTexGenfv(GL_TEXTURE_GEN_R,pname,tmpParams); 
    }
    else
        ctx->dispatcher().glTexGenfv(coord,pname,tmpParams);
}

GL_API void GL_APIENTRY glGetTexGenfvOES (GLenum coord, GLenum pname, GLfloat *params) {
    GET_CTX()
    if (coord == GL_TEXTURE_GEN_STR_OES)
    {
        GLfloat state_s = GL_FALSE;
        GLfloat state_t = GL_FALSE;
        GLfloat state_r = GL_FALSE;
        ctx->dispatcher().glGetTexGenfv(GL_TEXTURE_GEN_S,pname,&state_s);
        ctx->dispatcher().glGetTexGenfv(GL_TEXTURE_GEN_T,pname,&state_t);
        ctx->dispatcher().glGetTexGenfv(GL_TEXTURE_GEN_R,pname,&state_r);
        *params = state_s && state_t && state_r ? GL_TRUE: GL_FALSE;
    }
    else
        ctx->dispatcher().glGetTexGenfv(coord,pname,params);

}
GL_API void GL_APIENTRY glGetTexGenivOES (GLenum coord, GLenum pname, GLint *params) {
    GET_CTX()
    if (coord == GL_TEXTURE_GEN_STR_OES)
    {
        GLint state_s = GL_FALSE;
        GLint state_t = GL_FALSE;
        GLint state_r = GL_FALSE;
        ctx->dispatcher().glGetTexGeniv(GL_TEXTURE_GEN_S,pname,&state_s);
        ctx->dispatcher().glGetTexGeniv(GL_TEXTURE_GEN_T,pname,&state_t);
        ctx->dispatcher().glGetTexGeniv(GL_TEXTURE_GEN_R,pname,&state_r);
        *params = state_s && state_t && state_r ? GL_TRUE: GL_FALSE;
    }
    else
        ctx->dispatcher().glGetTexGeniv(coord,pname,params);
}

GL_API void GL_APIENTRY glGetTexGenxvOES (GLenum coord, GLenum pname, GLfixed *params) {
    GET_CTX()
    GLfloat tmpParams[1];

    if (coord == GL_TEXTURE_GEN_STR_OES)
    {
        GLfloat state_s = GL_FALSE;
        GLfloat state_t = GL_FALSE;
        GLfloat state_r = GL_FALSE;
        ctx->dispatcher().glGetTexGenfv(GL_TEXTURE_GEN_S,pname,&state_s);
        ctx->dispatcher().glGetTexGenfv(GL_TEXTURE_GEN_T,pname,&state_t);
        ctx->dispatcher().glGetTexGenfv(GL_TEXTURE_GEN_R,pname,&state_r);
        tmpParams[0] = state_s && state_t && state_r ? GL_TRUE: GL_FALSE;
    }
    else
        ctx->dispatcher().glGetTexGenfv(coord,pname,tmpParams);

    params[0] = F2X(tmpParams[1]);
}

template <class T, GLenum TypeName>
void glDrawTexOES (T x, T y, T z, T width, T height) {
    GET_CTX()
    int numClipPlanes;

    GLint viewport[4];
    z = (z>1 ? 1 : (z<0 ?  0 : z));

    T     vertices[4*3] = {x , y, z,
                             x , y+height, z,
                             x+width, y+height, z,
                             x+width, y, z};
    GLfloat texels[ctx->getMaxTexUnits()][4*2];

    ctx->dispatcher().glPushClientAttrib(GL_CLIENT_VERTEX_ARRAY_BIT);
    ctx->dispatcher().glPushAttrib(GL_TRANSFORM_BIT);

    //setup projection matrix to draw in viewport aligned coordinates
    ctx->dispatcher().glMatrixMode(GL_PROJECTION);
    ctx->dispatcher().glPushMatrix();
    ctx->dispatcher().glLoadIdentity();
    ctx->dispatcher().glGetIntegerv(GL_VIEWPORT,viewport);
    ctx->dispatcher().glOrtho(viewport[0],viewport[0] + viewport[2],viewport[1],viewport[1]+viewport[3],0,-1);
    //setup texture matrix
    ctx->dispatcher().glMatrixMode(GL_TEXTURE);
    ctx->dispatcher().glPushMatrix();
    ctx->dispatcher().glLoadIdentity();
    //setup modelview matrix
    ctx->dispatcher().glMatrixMode(GL_MODELVIEW);
    ctx->dispatcher().glPushMatrix();
    ctx->dispatcher().glLoadIdentity();
    //backup vbo's
    int array_buffer,element_array_buffer;
    glGetIntegerv(GL_ARRAY_BUFFER_BINDING,&array_buffer);
    glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING,&element_array_buffer);
    ctx->dispatcher().glBindBuffer(GL_ARRAY_BUFFER,0);
    ctx->dispatcher().glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,0);

    //disable clip planes
    ctx->dispatcher().glGetIntegerv(GL_MAX_CLIP_PLANES,&numClipPlanes);
    for (int i=0;i<numClipPlanes;++i)
        ctx->dispatcher().glDisable(GL_CLIP_PLANE0+i);

    for (int i=0;i<ctx->getMaxTexUnits();++i) {
        if (ctx->isTextureUnitEnabled(GL_TEXTURE0+i)) {
            TextureData * texData = NULL;
            int tex = ctx->getBindedTexture(GL_TEXTURE0+i);
            ctx->dispatcher().glClientActiveTexture(GL_TEXTURE0+i);
            ObjectDataPtr objData = thrd->shareGroup->getObjectData(TEXTURE,tex);
            if (objData.Ptr()) {
                texData = (TextureData*)objData.Ptr();
                //calculate texels
                texels[i][0] = (float)(texData->crop_rect[0])/(float)(texData->width);
                texels[i][1] = (float)(texData->crop_rect[1])/(float)(texData->height);

                texels[i][2] = (float)(texData->crop_rect[0])/(float)(texData->width);
                texels[i][3] = (float)(texData->crop_rect[3]+texData->crop_rect[1])/(float)(texData->height);

                texels[i][4] = (float)(texData->crop_rect[2]+texData->crop_rect[0])/(float)(texData->width);
                texels[i][5] = (float)(texData->crop_rect[3]+texData->crop_rect[1])/(float)(texData->height);
                
                texels[i][6] = (float)(texData->crop_rect[2]+texData->crop_rect[0])/(float)(texData->width);
                texels[i][7] = (float)(texData->crop_rect[1])/(float)(texData->height);

                ctx->dispatcher().glTexCoordPointer(2,GL_FLOAT,0,texels[i]);
             }
        }
    }

    //draw rectangle
    ctx->dispatcher().glEnableClientState(GL_VERTEX_ARRAY);
    ctx->dispatcher().glVertexPointer(3,TypeName,0,vertices);
    ctx->dispatcher().glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    ctx->dispatcher().glDrawArrays(GL_TRIANGLE_FAN,0,4);

    //restore vbo's
    ctx->dispatcher().glBindBuffer(GL_ARRAY_BUFFER,array_buffer);
    ctx->dispatcher().glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,element_array_buffer);

    //restore matrix state

    ctx->dispatcher().glMatrixMode(GL_MODELVIEW);
    ctx->dispatcher().glPopMatrix();
    ctx->dispatcher().glMatrixMode(GL_TEXTURE);
    ctx->dispatcher().glPopMatrix();
    ctx->dispatcher().glMatrixMode(GL_PROJECTION);
    ctx->dispatcher().glPopMatrix();

    ctx->dispatcher().glPopAttrib();
    ctx->dispatcher().glPopClientAttrib();
}

GL_API void GL_APIENTRY glDrawTexsOES (GLshort x, GLshort y, GLshort z, GLshort width, GLshort height) {
    glDrawTexOES<GLshort,GL_SHORT>(x,y,z,width,height);
}

GL_API void GL_APIENTRY glDrawTexiOES (GLint x, GLint y, GLint z, GLint width, GLint height) {
    glDrawTexOES<GLint,GL_INT>(x,y,z,width,height);
}

GL_API void GL_APIENTRY glDrawTexfOES (GLfloat x, GLfloat y, GLfloat z, GLfloat width, GLfloat height) {
    glDrawTexOES<GLfloat,GL_FLOAT>(x,y,z,width,height);
}

GL_API void GL_APIENTRY glDrawTexxOES (GLfixed x, GLfixed y, GLfixed z, GLfixed width, GLfixed height) {
    glDrawTexOES<GLfloat,GL_FLOAT>(X2F(x),X2F(y),X2F(z),X2F(width),X2F(height));
}

GL_API void GL_APIENTRY glDrawTexsvOES (const GLshort * coords) { 
    glDrawTexOES<GLshort,GL_SHORT>(coords[0],coords[1],coords[2],coords[3],coords[4]);
}

GL_API void GL_APIENTRY glDrawTexivOES (const GLint * coords) { 
    glDrawTexOES<GLint,GL_INT>(coords[0],coords[1],coords[2],coords[3],coords[4]);
}

GL_API void GL_APIENTRY glDrawTexfvOES (const GLfloat * coords) { 
    glDrawTexOES<GLfloat,GL_FLOAT>(coords[0],coords[1],coords[2],coords[3],coords[4]);
}

GL_API void GL_APIENTRY glDrawTexxvOES (const GLfixed * coords) { 
    glDrawTexOES<GLfloat,GL_FLOAT>(X2F(coords[0]),X2F(coords[1]),X2F(coords[2]),X2F(coords[3]),X2F(coords[4]));
}
