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
#include <string.h>
#include <GL/glx.h>


#define IS_SUCCESS(a) \
        if(a != Success) return false;

namespace EglOS {

EGLNativeDisplayType getDefaultDisplay() {return XOpenDisplay(0);}

bool releaseDisplay(EGLNativeDisplayType dpy) {
    return XCloseDisplay(dpy);
}

EglConfig* pixelFormatToConfig(EGLNativeDisplayType dpy,EGLNativePixelFormatType* frmt){

    int  bSize,red,green,blue,alpha,depth,stencil;
    int  supportedSurfaces,visualType,visualId;
    int  caveat,transparentType,samples;
    int  tRed,tGreen,tBlue;
    int  pMaxWidth,pMaxHeight,pMaxPixels;
    int  tmp;
    int  configId,level,renderable;

    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_TRANSPARENT_TYPE,&tmp));
    if(tmp == GLX_TRANSPARENT_INDEX) {
        return NULL; // not supporting transparent index
    } else if( tmp == GLX_NONE) {
        transparentType = EGL_NONE;
    } else {
        transparentType = EGL_TRANSPARENT_RGB;

        IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_TRANSPARENT_RED_VALUE,&tRed));
        IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_TRANSPARENT_GREEN_VALUE,&tGreen));
        IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_TRANSPARENT_BLUE_VALUE,&tBlue));
    }


    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_BUFFER_SIZE,&bSize));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_RED_SIZE,&red));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_GREEN_SIZE,&green));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_BLUE_SIZE,&blue));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_ALPHA_SIZE,&alpha));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_DEPTH_SIZE,&depth));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_STENCIL_SIZE,&stencil));


    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_X_RENDERABLE,&renderable));

    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_X_VISUAL_TYPE,&visualType));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_VISUAL_ID,&visualId));

    //supported surfaces types
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_DRAWABLE_TYPE,&tmp));
    supportedSurfaces = 0;
    if(tmp & GLX_WINDOW_BIT) {
        supportedSurfaces |= EGL_WINDOW_BIT;
    } else {
        visualId = 0;
        visualType = EGL_NONE;
    }
    if(tmp & GLX_PIXMAP_BIT)  supportedSurfaces |= EGL_PIXMAP_BIT;
    if(tmp & GLX_PBUFFER_BIT) supportedSurfaces |= EGL_PBUFFER_BIT;

    caveat = 0;
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_CONFIG_CAVEAT,&tmp));
    if     (tmp == GLX_NONE) caveat = EGL_NONE;
    else if(tmp == GLX_SLOW_CONFIG) caveat = EGL_SLOW_CONFIG;
    else if(tmp == GLX_NON_CONFORMANT_CONFIG) caveat = EGL_NON_CONFORMANT_CONFIG;
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_MAX_PBUFFER_WIDTH,&pMaxWidth));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_MAX_PBUFFER_HEIGHT,&pMaxHeight));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_MAX_PBUFFER_HEIGHT,&pMaxPixels));

    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_LEVEL,&level));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_FBCONFIG_ID,&configId));
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_SAMPLES,&samples));


    return new EglConfig(red,green,blue,alpha,caveat,configId,depth,level,pMaxWidth,pMaxHeight,
                              pMaxPixels,renderable,visualId,visualType,samples,stencil,
                              supportedSurfaces,transparentType,tRed,tGreen,tBlue,*frmt);
}

void queryConfigs(EGLNativeDisplayType dpy,ConfigsList& listOut) {
    int n;
    EGLNativePixelFormatType*  frmtList =  glXGetFBConfigs(dpy,0,&n);
    for(int i =0 ;i < n ; i++) {
        EglConfig* conf = pixelFormatToConfig(dpy,&frmtList[i]);
        if(conf) listOut.push_back(conf);
    }
    listOut.sort();
    XFree(frmtList);
}

bool validNativeWin(EGLNativeWindowType win) {
  //TODO: use XGetgeometry to check validity
   return true;
}

bool validNativePixmap(EGLNativePixmapType pix) {
  //TODO: use XGetgeometry to check validity
   return true;
}

bool checkWindowPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativeWindowType win,EglConfig* cfg,unsigned int* width,unsigned int* height) {
//TODO: to check what does ATI & NVIDIA enforce on win pixelformat
   unsigned int depth,configDepth,border;
   int r,g,b,x,y;
   IS_SUCCESS(glXGetFBConfigAttrib(dpy,cfg->nativeConfig(),GLX_RED_SIZE,&r));
   IS_SUCCESS(glXGetFBConfigAttrib(dpy,cfg->nativeConfig(),GLX_GREEN_SIZE,&g));
   IS_SUCCESS(glXGetFBConfigAttrib(dpy,cfg->nativeConfig(),GLX_BLUE_SIZE,&b));
   configDepth = r + g + b;
   Window root;
   if(!XGetGeometry(dpy,win,&root,&x,&y,width,height,&border,&depth)) return false;
   return depth >= configDepth;
}

bool checkPixmapPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativePixmapType pix,EglConfig* cfg,unsigned int* width,unsigned int* height) {
   unsigned int depth,configDepth,border;
   int r,g,b,x,y;
   IS_SUCCESS(glXGetFBConfigAttrib(dpy,cfg->nativeConfig(),GLX_RED_SIZE,&r));
   IS_SUCCESS(glXGetFBConfigAttrib(dpy,cfg->nativeConfig(),GLX_GREEN_SIZE,&g));
   IS_SUCCESS(glXGetFBConfigAttrib(dpy,cfg->nativeConfig(),GLX_BLUE_SIZE,&b));
   configDepth = r + g + b;
   Window root;
   if(!XGetGeometry(dpy,pix,&root,&x,&y,width,height,&border,&depth)) return false;
   return depth >= configDepth;
}

EGLNativePbufferType createPbuffer(EGLNativeDisplayType dpy,EglConfig* cfg,EglPbufferSurface* srfc){
    EGLint width,height,largest;
    srfc->getDim(&width,&height,&largest);

    int attribs[] = {
                     GLX_PBUFFER_WIDTH           ,width,
                     GLX_PBUFFER_HEIGHT          ,height,
                     GLX_LARGEST_PBUFFER         ,largest,
                     None
                    };
    return glXCreatePbuffer(dpy,cfg->nativeConfig(),attribs);
}

bool releasePbuffer(EGLNativeDisplayType dis,EGLNativePbufferType pb) {
    glXDestroyPbuffer(dis,pb);

    return true;
}

EGLNativeContextType createContext(EGLNativeDisplayType dpy,EglConfig* cfg,EGLNativeContextType sharedContext) {
 return glXCreateNewContext(dpy,cfg->nativeConfig(),GLX_RGBA_TYPE,sharedContext,true);
}

bool destroyContext(EGLNativeDisplayType dpy,EGLNativeContextType ctx) {
    glXDestroyContext(dpy,ctx);
    return true;
}

GLXDrawable convertSurface(EglSurface* srfc) {
    if(!srfc) return None;
    switch(srfc->type()){
    case EglSurface::PIXMAP:
        return (GLXPixmap)srfc->native();
    case EglSurface::PBUFFER:
        return (GLXPbuffer)srfc->native();
    case EglSurface::WINDOW:
    default:
        return (GLXWindow)srfc->native();
    }
}


bool makeCurrent(EGLNativeDisplayType dpy,EglSurface* read,EglSurface* draw,EGLNativeContextType ctx){


return glXMakeContextCurrent(dpy,convertSurface(draw),convertSurface(read),ctx);
}

void swapBuffers(EGLNativeDisplayType dpy,EGLNativeWindowType win) {
    glXSwapBuffers(dpy,win);
}

void waitNative() {
    glXWaitX();
}

void swapInterval(EGLNativeDisplayType dpy,EGLNativeWindowType win,int interval){
    const char* extensions = glXQueryExtensionsString(dpy,DefaultScreen(dpy));
    typedef void (*GLXSWAPINTERVALEXT)(Display*,GLXDrawable,int);
    GLXSWAPINTERVALEXT glXSwapIntervalEXT = NULL;

    if(strstr(extensions,"EXT_swap_control")) {
        glXSwapIntervalEXT = (GLXSWAPINTERVALEXT)glXGetProcAddress((const GLubyte*)"glXSwapIntervalEXT");
    }
    if(glXSwapIntervalEXT) {
        glXSwapIntervalEXT(dpy,win,interval);
    }
}

};
