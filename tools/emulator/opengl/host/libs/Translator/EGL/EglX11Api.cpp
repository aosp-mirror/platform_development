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
#include <X11/Xlib.h>
#include <GL/glx.h>
#include <utils/threads.h>


class ErrorHandler{
public:
ErrorHandler(EGLNativeDisplayType dpy);
~ErrorHandler();
int getLastError(){ return s_lastErrorCode;};

private:
static int s_lastErrorCode;
int (*m_oldErrorHandler) (Display *, XErrorEvent *);
static android::Mutex s_lock;
static int errorHandlerProc(EGLNativeDisplayType dpy,XErrorEvent* event);

};

class SrfcInfo{
public:
    typedef enum{
                 WINDOW  = 0,
                 PBUFFER = 1, 
                 PIXMAP
                }SurfaceType;
    SrfcInfo(GLXDrawable drawable,SurfaceType type):m_type(type),
                                                    m_srfc(drawable){};
    GLXDrawable srfc(){return m_srfc;};
private: 
    SurfaceType m_type;
    GLXDrawable  m_srfc; 
};

int ErrorHandler::s_lastErrorCode = 0;
android::Mutex ErrorHandler::s_lock;

ErrorHandler::ErrorHandler(EGLNativeDisplayType dpy){
   android::Mutex::Autolock mutex(s_lock);
   XSync(dpy,False);
   s_lastErrorCode = 0;
   m_oldErrorHandler = XSetErrorHandler(errorHandlerProc);
}

ErrorHandler::~ErrorHandler(){
   android::Mutex::Autolock mutex(s_lock);
   XSetErrorHandler(m_oldErrorHandler);
   s_lastErrorCode = 0;
}

int ErrorHandler::errorHandlerProc(EGLNativeDisplayType dpy,XErrorEvent* event){
    android::Mutex::Autolock mutex(s_lock);
    s_lastErrorCode = event->error_code;
    return 0;
}

#define IS_SUCCESS(a) \
        if(a != Success) return false;

namespace EglOS {

EGLNativeDisplayType getDefaultDisplay() {return XOpenDisplay(0);}

bool releaseDisplay(EGLNativeDisplayType dpy) {
    return XCloseDisplay(dpy);
}

EglConfig* pixelFormatToConfig(EGLNativeDisplayType dpy,int renderableType,EGLNativePixelFormatType* frmt){

    int  bSize,red,green,blue,alpha,depth,stencil;
    int  supportedSurfaces,visualType,visualId;
    int  caveat,transparentType,samples;
    int  tRed=0,tGreen=0,tBlue=0;
    int  pMaxWidth,pMaxHeight,pMaxPixels;
    int  tmp;
    int  configId,level,renderable;
    int  doubleBuffer;

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


    //
    // filter out single buffer configurations
    //
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_DOUBLEBUFFER,&doubleBuffer));
    if (!doubleBuffer) return NULL;

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
    if(tmp & GLX_WINDOW_BIT && visualId != 0) {
        supportedSurfaces |= EGL_WINDOW_BIT;
    } else {
        visualId = 0;
        visualType = EGL_NONE;
    }
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
    //Filter out configs that does not support RGBA
    IS_SUCCESS(glXGetFBConfigAttrib(dpy,*frmt,GLX_RENDER_TYPE,&tmp));
    if (!(tmp & GLX_RGBA_BIT)) {
        return NULL;
    }

    return new EglConfig(red,green,blue,alpha,caveat,configId,depth,level,pMaxWidth,pMaxHeight,
                              pMaxPixels,renderable,renderableType,visualId,visualType,samples,stencil,
                              supportedSurfaces,transparentType,tRed,tGreen,tBlue,*frmt);
}

void queryConfigs(EGLNativeDisplayType dpy,int renderableType,ConfigsList& listOut) {
    int n;
    EGLNativePixelFormatType*  frmtList =  glXGetFBConfigs(dpy,0,&n);
    for(int i =0 ;i < n ; i++) {
        EglConfig* conf = pixelFormatToConfig(dpy,renderableType,&frmtList[i]);
        if(conf) listOut.push_back(conf);
    }
    XFree(frmtList);
}

bool validNativeWin(EGLNativeDisplayType dpy,EGLNativeWindowType win) {
   Window root;
   int tmp;
   unsigned int utmp;
   ErrorHandler handler(dpy);
   if(!XGetGeometry(dpy,win,&root,&tmp,&tmp,&utmp,&utmp,&utmp,&utmp)) return false;
   return handler.getLastError() == 0;
}

bool validNativeWin(EGLNativeDisplayType dpy,EGLNativeSurfaceType win) {
    if (!win) return false;
    return validNativeWin(dpy,win->srfc());
}

bool validNativePixmap(EGLNativeDisplayType dpy,EGLNativeSurfaceType pix) {
   Window root;
   int tmp;
   unsigned int utmp;
   ErrorHandler handler(dpy);
   if(!XGetGeometry(dpy,pix ? pix->srfc() : NULL,&root,&tmp,&tmp,&utmp,&utmp,&utmp,&utmp)) return false;
   return handler.getLastError() == 0;
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

EGLNativeSurfaceType createPbufferSurface(EGLNativeDisplayType dpy,EglConfig* cfg,EglPbufferSurface* srfc){
    EGLint width,height,largest;
    srfc->getDim(&width,&height,&largest);

    int attribs[] = {
                     GLX_PBUFFER_WIDTH           ,width,
                     GLX_PBUFFER_HEIGHT          ,height,
                     GLX_LARGEST_PBUFFER         ,largest,
                     None
                    };
    GLXPbuffer pb = glXCreatePbuffer(dpy,cfg->nativeConfig(),attribs);
    return pb ? new SrfcInfo(pb,SrfcInfo::PBUFFER) : NULL;
}

bool releasePbuffer(EGLNativeDisplayType dis,EGLNativeSurfaceType pb) {
    if (!pb) return false;
    glXDestroyPbuffer(dis,pb->srfc());

    return true;
}

EGLNativeContextType createContext(EGLNativeDisplayType dpy,EglConfig* cfg,EGLNativeContextType sharedContext) {
 ErrorHandler handler(dpy);
 EGLNativeContextType retVal = glXCreateNewContext(dpy,cfg->nativeConfig(),GLX_RGBA_TYPE,sharedContext,true);
 return handler.getLastError() == 0 ? retVal : NULL;
}

bool destroyContext(EGLNativeDisplayType dpy,EGLNativeContextType ctx) {
    glXDestroyContext(dpy,ctx);
    return true;
}

bool makeCurrent(EGLNativeDisplayType dpy,EglSurface* read,EglSurface* draw,EGLNativeContextType ctx){

    ErrorHandler handler(dpy);
    bool retval = false;
    if (!ctx && !read && !draw) {
        // unbind
        retval = glXMakeContextCurrent(dpy, NULL, NULL, NULL);
    }
    else if (ctx && read && draw) {
        retval = glXMakeContextCurrent(dpy,draw->native()->srfc(),read->native()->srfc(),ctx);
    }
    return (handler.getLastError() == 0) && retval;
}

void swapBuffers(EGLNativeDisplayType dpy,EGLNativeSurfaceType srfc){
    if (srfc) {
        glXSwapBuffers(dpy,srfc->srfc());
    }
}

void waitNative() {
    glXWaitX();
}

void swapInterval(EGLNativeDisplayType dpy,EGLNativeSurfaceType win,int interval){
    const char* extensions = glXQueryExtensionsString(dpy,DefaultScreen(dpy));
    typedef void (*GLXSWAPINTERVALEXT)(Display*,GLXDrawable,int);
    GLXSWAPINTERVALEXT glXSwapIntervalEXT = NULL;

    if(strstr(extensions,"EXT_swap_control")) {
        glXSwapIntervalEXT = (GLXSWAPINTERVALEXT)glXGetProcAddress((const GLubyte*)"glXSwapIntervalEXT");
    }
    if(glXSwapIntervalEXT && win) {
        glXSwapIntervalEXT(dpy,win->srfc(),interval);
    }
}

EGLNativeSurfaceType createWindowSurface(EGLNativeWindowType wnd){
    return new SrfcInfo(wnd,SrfcInfo::WINDOW);
}

EGLNativeSurfaceType createPixmapSurface(EGLNativePixmapType pix){
    return new SrfcInfo(pix,SrfcInfo::PIXMAP);
}

void destroySurface(EGLNativeSurfaceType srfc){
    delete srfc;
};

EGLNativeInternalDisplayType getInternalDisplay(EGLNativeDisplayType dpy){
    return dpy;
}

void deleteDisplay(EGLNativeInternalDisplayType idpy){
}

};
