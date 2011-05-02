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
#include "EglOsApi.h"
#include <windows.h>
#include <GL/wglext.h>

#define IS_TRUE(a) \
        if(a != true) return false;

namespace EglOS{
bool WGLExtensionSupported(const char *extension_name)
{
    // this is pointer to function which returns pointer to string with list of all wgl extensions
    PFNWGLGETEXTENSIONSSTRINGEXTPROC _wglGetExtensionsStringEXT = NULL;

    // determine pointer to wglGetExtensionsStringEXT function
    _wglGetExtensionsStringEXT = (PFNWGLGETEXTENSIONSSTRINGEXTPROC) wglGetProcAddress("wglGetExtensionsStringEXT");

    if (strstr(_wglGetExtensionsString(), extension_name) == NULL)
    {
        // string was not found
        return false;
    }

    // extension is supported
    return true;
}

EGLNativeDisplayType getDefaultDisplay() {
    return GetDC();
}

bool releaseDisplay(EGLNativeDisplay dpy) {
    return DeleteDC(dpy);
}



EglConfig* pixelFormatToConfig(EGLNativeDisplayType dpy,EGLNativePixelFormatType* frmt,int index){

    int  supportedSurfaces,visualType,visualId;
    int  caveat,transparentType,samples;
    int  tRed,tGreen,tBlue;
    int  pMaxWidth,pMaxHeight,pMaxPixels;
    int  configId,level,renderable;
    bool window,bitmap,pbuffer,transparent;

    if(frmt->iPixelType != PFD_TYPE_RGBA) return NULL; // other formats are not supported yet

    supportedSurfaces = 0;
    IS_TRUE(wglGetPixelFormatAttribivARB(dpy,index,0,1,WGL_DRAW_TO_WINDOW_ARB,&window));
    IS_TRUE(wglGetPixelFormatAttribivARB(dpy,index,0,1,WGL_DRAW_TO_BITMAP_ARB,&bitmap));
    IS_TRUE(wglGetPixelFormatAttribivARB(dpy,index,0,1,WGL_DRAW_TO_PBUFFER_ARB,&pbuffer));
    if(window)  supportedSurfaces |= EGL_WINDOW_BIT;
    if(bitmap)  supportedSurfaces |= EGL_PIXMAP_BIT;
    if(pbuffer) supportedSurfaces |= EGL_PBUFFER_BIT;

    //default values
    visualId   = 0;
    visualType = EGL_NONE;
    caveat     = EGL_NONE;
    pMaxWidth  = PBUFFER_MAX_WIDTH;
    pMaxHeight = PBUFFER_MAX_HEIGHT;
    pMaxPixels = PBUFFER_MAX_PIXELS;
    samples    = 0 ;
    level      = 0 ;
    renderable = EGL_FALSE;

    IS_TRUE(wglGetPixelFormatAttribivARB(dpy,index,0,1,WGL_TRANSPARENT_ARB,&transparent));
    if(transparent) {
        transparentType = EGL_TRANSPARENT_RGB;
        IS_TRUE(wglGetPixelFormatAttribivARB(dpy,index,0,1,WGL_TRANSPARENT_RED_VALUE_ARB,&tRed));
        IS_TRUE(wglGetPixelFormatAttribivARB(dpy,index,0,1,WGL_TRANSPARENT_GREEN_VALUE_ARB,&tGreen));
        IS_TRUE(wglGetPixelFormatAttribivARB(dpy,index,0,1,WGL_TRANSPARENT_BLUE_VALUE_ARB,&tBlue));
    } else {
        transparentType = EGL_NONE;
    }

    return new EglConfig(frmt->cRedBits,frmt->cGreenBits,frmt->cBlueBits,frmt->cAlphaBits,caveat,
                              index,frmt->cDepthBits,level,pMaxWidth,pMaxHeight,pMaxPixels,renderable,
                              visualId,visualType,samples,frmt->cStencilBits,supportedSurfaces,transparentType,tRed,tGreen,tBlue,frmt);
}

void queryConfigs(EGLNativeDisplayType dpy,ConfigList& listOut) {
    PIXELFORMATDESCRIPTOR  pfd;
    int  iPixelFormat = 1;

    //quering num of formats
    nFormats = DescribePixelFormat(dpy, iPixelFormat,sizeof(PIXELFORMATDESCRIPTOR), &pfd);
    EglConfig* p = pixelFormatToConfig(dpy,&pfd,iPixelFormat);
    //inserting first format
    if(p) listOut.push_front(p);

    //inserting rest of formats
    for(iPixelFormat++;iPixelFormat < nFormats; iPixelFormat++) {
         DescribePixelFormat(dpy, iPixelFormat,sizeof(PIXELFORMATDESCRIPTOR), &pfd);
         EglConfig* pConfig = pixelFormatToConfig(dpy,&pfd,iPixelFormat);
         if(pConfig) listOut.push_front(pConfig);
    }

}


bool validNativeWin(EGLNativeWindowType win) {
    return IsWindow(win);
}

bool validNativePixmap(EGLNativePixmapType pix) {
    BITMAP bm;
    return GetObject(pix, sizeof(BITMAP), (LPSTR)&bm);
}

static bool setPixelFormat(EGLNativeDisplayType dpy,EglConfig* cfg) {
   int iPixelFormat = ChoosePixelFormat(dpy,cfg->nativeConfig());
   if(!iPixelFormat) return false;
   if(!SetPixelFormat(dpy,iPixelFormat,cfg->nativeConfig())) return false;
   return true;
}

bool checkWindowPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativeWindowType win,EglConfig* cfg,unsigned int* width,unsigned int* height) {
   RECT r;
   if(!GetClientRect(win,&r)) return false;
   *width  = r.right  - r.left;
   *height = r.bottom - r.top;

   return setPixelFormat(dpy,cfg);
}

bool checkPixmapPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativePixmapType pix,EglConfig* cfg) {

    BITMAP bm;
    if(!GetObject(pix, sizeof(BITMAP), (LPSTR)&bm)) return false;

    *width  = bm.bmWidth;
    *height = bm.bmHeight;

   return setPixelFormat(dpy,cfg);
}

EGLNativePbufferType createPbuffer(EGLNativeDisplayType dpy,EglConfig* cfg,EglPbSurface* pbSurface) {

  //converting configuration into WGL pixel Format
   EGLint red,green,blue,alpha,depth,stencil;
   bool   gotAttribs = getConfAttrib(EGL_RED_SIZE,&red)     &&
                       getConfAttrib(EGL_GREEN_SIZE,&green) &&
                       getConfAttrib(EGL_BLUE_SIZE,&blue)   &&
                       getConfAttrib(EGL_ALPHA_SIZE,&alpha) &&
                       getConfAttrib(EGL_DEPTH_SIZE,&depth) &&
                       getConfAttrib(EGL_STENCIL_SIZE,&stencil) ;

 if(!gotAttribs) return false;
 int wglPixelFormatAttribs[] = {
                                WGL_SUPPORT_OPENGL_ARB       ,TRUE,
                                WGL_DRAW_TO_BUFFER_ARB       ,TRUE,
                                WGL_BIND_TO_TEXTURE_RGBA_ARB ,TRUE,
                                WGL_COLOR_BITS_ARB           ,red+green+blue,
                                WGL_RED_BITS_ARB             ,red,
                                WGL_GREEN_BITS_ARB           ,green,
                                WGL_BLUE_BITS_ARB            ,blue,
                                WGL_ALPHA_BITS_ARB           ,alpha,
                                WGL_STENCIL_BITS_ARB         ,stencil,
                                WGL_DEPTH_BITS_ARB           ,depth,
                                WGL_DOUBLE_BUFFER_ARB        ,TRUE,
                                0
                               };

    int pixfmt;
    unsigned int numpf;
    if(!wglChoosePixelFormatARB(dpy,wglPixelFormatAttribs, NULL, 1, &pixfmt, &numpf)) {
        DWORD err = GetLastError();
        return NULL;
    }

    EGLint width,height,largest,texTarget,texFormat;
    pbSurface->getDim(&width,&height,&largest);
    pbSurface->getTexInfo(&texTarget,&texFormat);

    int wglTexFormat = WGL_NO_TEXTURE_ARB;
    int wglTexTarget = (texTarget == EGL_TEXTURE_2D)? WGL_TEXTURE_2D_ARB:
                                                      WGL_NO_TEXTURE_ARB;

    switch(texFormat) {
    case EGL_TEXTURE_RGB:
        wglTexFormat = WGL_TEXTURE_RGB_ARB;
        break;
    case EGL_TEXTURE_RGBA:
        wglTexFormat = WGL_TEXTURE_RGB_ARBA;
        break;
    }

    int pbAttribs[] = {
                       WGL_TEXTURE_TARGET_ARB   ,wglTexTarget,
                       WGL_TEXTURE_FORMAT_ARB   ,wglTexFormat,
                       WGL_TEXTURE_LARGEST_ARB  ,largest,
                       0
                      };
    EGLNativePbufferType pb = wglCreatePbufferARB(dpy,pixfmt,width,height,pbAttribs);
    if(!pb) {
        DWORD err = GetLastError();
        return NULL;
    }
    return pb;
}

bool releasePbuffer(EGLNativeDisplayType dis,EGLNativePbufferType pb) {
    if(wglReleasePbufferDCARB(pb,dis) || wglDestroyPbufferArb(pb)){
        DWORD err = GetLastError();
        return false;
    }
    return true;
}

EGLNativeContextType createContext(EGLNativeDisplayType dpy,EglConfig* cfg,EGLNativeContextType sharedContext) {

    EGLNativeContextType ctx = NULL;

    if(!setPixelFormat(dpy,cfg)) return NULL;
    ctx = wglCreateContext(dpy);
    if(ctx && sharedContext) {
        if(!wglShareLists(sharedContext,ctx)) {
            wglDeleteContext(ctx);
            return NULL;
        }
    }
    return ctx;
}

bool destroyContext(EGLNativeDisplayType dpy,EGLNativeContextType ctx) {
    if(!wglDeleteContext(ctx)) {
        DWORD err = GetLastError();
        return false;
    }
    return true;
}

bool makeCurrent(EGLNativeDisplayType dpy,EglSurface* read,EglSurface* draw,EGLNativeContextType ctx) {
    return ctx ? :wglMakeCurrent(dpy,NULL): wglMakeContextCurrentARB(dpy,draw->native(),read->native());


}

void swapBuffers(EGLNativeDisplayType dpy,EGLNativeWindowType win) {
    if(!SwapBuffers(dpy)) {
        DWORD err = GetLastError();
    }

}


void waitNative(){}

void swapInterval(EGLNativeDisplayType dpy,EGLNativeWindowType win,int interval) {

    PFNWGLSWAPINTERVALEXTPROC       wglSwapIntervalEXT = NULL;

    if (WGLExtensionSupported("WGL_EXT_swap_control"))
    {
        // Extension is supported, init pointers.
        wglSwapIntervalEXT = (PFNWGLSWAPINTERVALEXTPROC) LogGetProcAddress("wglSwapIntervalEXT");

    }
    wglSwapIntervalEXT(interval);
}

};
