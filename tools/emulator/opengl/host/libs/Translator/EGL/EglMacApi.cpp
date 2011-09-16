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
#include "MacNative.h"
#define MAX_PBUFFER_MIPMAP_LEVEL 1

namespace EglOS {

static std::list<EGLNativePixelFormatType> s_nativeConfigs;

EGLNativeDisplayType getDefaultDisplay() {return 0;}

bool releaseDisplay(EGLNativeDisplayType dpy) {
    return true;
}

static EglConfig* pixelFormatToConfig(int index,int renderableType,EGLNativePixelFormatType* frmt){
    if(!frmt) return NULL;

    EGLint  red,green,blue,alpha,depth,stencil;
    EGLint  supportedSurfaces,visualType,visualId;
    EGLint  transparentType,samples;
    EGLint  tRed,tGreen,tBlue;
    EGLint  pMaxWidth,pMaxHeight,pMaxPixels;
    EGLint  configId,level;
    EGLint  window,pbuffer;
    EGLint  doubleBuffer,colorSize;

    getPixelFormatAttrib(*frmt,MAC_HAS_DOUBLE_BUFFER,&doubleBuffer);
    if(!doubleBuffer) return NULL; //pixel double buffer

    supportedSurfaces = 0;

    getPixelFormatAttrib(*frmt,MAC_DRAW_TO_WINDOW,&window);
    getPixelFormatAttrib(*frmt,MAC_DRAW_TO_PBUFFER,&pbuffer);

    if(window)  supportedSurfaces |= EGL_WINDOW_BIT;
    if(pbuffer) supportedSurfaces |= EGL_PBUFFER_BIT;

    if(!supportedSurfaces) return NULL;

    //default values
    visualId                  = 0;
    visualType                = EGL_NONE;
    EGLenum caveat            = EGL_NONE;
    EGLBoolean renderable     = EGL_FALSE;
    pMaxWidth                 = PBUFFER_MAX_WIDTH;
    pMaxHeight                = PBUFFER_MAX_HEIGHT;
    pMaxPixels                = PBUFFER_MAX_PIXELS;
    samples                   = 0;
    level                     = 0;
    tRed = tGreen = tBlue     = 0;

    transparentType = EGL_NONE;

    getPixelFormatAttrib(*frmt,MAC_SAMPLES_PER_PIXEL,&samples);
    getPixelFormatAttrib(*frmt,MAC_COLOR_SIZE,&colorSize);
    getPixelFormatAttrib(*frmt,MAC_ALPHA_SIZE,&alpha);
    getPixelFormatAttrib(*frmt,MAC_DEPTH_SIZE,&depth);
    getPixelFormatAttrib(*frmt,MAC_STENCIL_SIZE,&stencil);

    red = green = blue = (colorSize / 4); //TODO: ask guy if it is OK

    return new EglConfig(red,green,blue,alpha,caveat,(EGLint)index,depth,level,pMaxWidth,pMaxHeight,pMaxPixels,renderable,renderableType,
                         visualId,visualType,samples,stencil,supportedSurfaces,transparentType,tRed,tGreen,tBlue,*frmt);
}


static void initNativeConfigs(){
    int nConfigs = getNumPixelFormats();
    if(s_nativeConfigs.empty()){
        for(int i=0; i < nConfigs ;i++){
             EGLNativePixelFormatType frmt = getPixelFormat(i);
             if(frmt){
                 s_nativeConfigs.push_back(frmt);
             }
        }
    }
}

void queryConfigs(EGLNativeDisplayType dpy,int renderableType,ConfigsList& listOut) {
    int i = 0 ;
    initNativeConfigs();
    for(std::list<EGLNativePixelFormatType>::iterator it = s_nativeConfigs.begin(); it != s_nativeConfigs.end();it++){
         EGLNativePixelFormatType frmt = *it;
         EglConfig* conf = pixelFormatToConfig(i++,renderableType,&frmt);
         if(conf){
             listOut.push_front(conf);
         };
    }
}

bool validNativeWin(EGLNativeDisplayType dpy, EGLNativeWindowType win) {
    unsigned int width,height;
    return nsGetWinDims(win,&width,&height);
}

bool validNativeWin(EGLNativeDisplayType dpy, EGLNativeSurfaceType win) {
    return validNativeWin(dpy,(EGLNativeWindowType)win);
}

//no support for pixmap in mac
bool validNativePixmap(EGLNativeDisplayType dpy, EGLNativeSurfaceType pix) {

   return true;
}

bool checkWindowPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativeWindowType win,EglConfig* cfg,unsigned int* width,unsigned int* height) {
    int r,g,b;
    bool ret = nsGetWinDims(win,width,height);

    cfg->getConfAttrib(EGL_RED_SIZE,&r);
    cfg->getConfAttrib(EGL_GREEN_SIZE,&g);
    cfg->getConfAttrib(EGL_BLUE_SIZE,&b);
    bool match = nsCheckColor(win,r + g + b);

    return ret && match;
}

//no support for pixmap in mac
bool checkPixmapPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativePixmapType pix,EglConfig* cfg,unsigned int* width,unsigned int* height) {
    return false;
}

EGLNativeSurfaceType createPbufferSurface(EGLNativeDisplayType dpy,EglConfig* cfg,EglPbufferSurface* srfc){
    EGLint width,height,hasMipmap,tmp;
    EGLint target,format;
    srfc->getDim(&width,&height,&tmp);
    srfc->getTexInfo(&format,&target);
    srfc->getAttrib(EGL_MIPMAP_TEXTURE,&hasMipmap);
    EGLint maxMipmap = hasMipmap ? MAX_PBUFFER_MIPMAP_LEVEL:0;
    return (EGLNativeSurfaceType)nsCreatePBuffer(target,format,maxMipmap,width,height);
}

bool releasePbuffer(EGLNativeDisplayType dis,EGLNativeSurfaceType pb) {
    nsDestroyPBuffer(pb);
    return true;
}

EGLNativeContextType createContext(EGLNativeDisplayType dpy,EglConfig* cfg,EGLNativeContextType sharedContext) {
 return nsCreateContext(cfg->nativeConfig(),sharedContext);
}

bool destroyContext(EGLNativeDisplayType dpy,EGLNativeContextType ctx) {
    nsDestroyContext(ctx);
    return true;
}

bool makeCurrent(EGLNativeDisplayType dpy,EglSurface* read,EglSurface* draw,EGLNativeContextType ctx){

    // check for unbind
    if (ctx == NULL && read == NULL && draw == NULL) {
        nsWindowMakeCurrent(NULL, NULL);
        return true;
    }
    else if (ctx == NULL || read == NULL || draw == NULL) {
        // error !
        return false;
    }

    //dont supporting diffrent read & draw surfaces on Mac
    if(read->native() != draw->native()) return false;
    switch(draw->type()){
    case EglSurface::WINDOW:
        nsWindowMakeCurrent(ctx,draw->native());
        break;
    case EglSurface::PBUFFER:
    {
        EGLint hasMipmap;
        draw->getAttrib(EGL_MIPMAP_TEXTURE,&hasMipmap);
        int mipmapLevel = hasMipmap ? MAX_PBUFFER_MIPMAP_LEVEL:0;
        nsPBufferMakeCurrent(ctx,draw->native(),mipmapLevel);
        break;
    }
    case EglSurface::PIXMAP: // not supported on Mac
    default:
        return false;
    }
    return true;
}

void swapBuffers(EGLNativeDisplayType dpy,EGLNativeSurfaceType srfc){
    nsSwapBuffers();
}

void waitNative(){}

void swapInterval(EGLNativeDisplayType dpy,EGLNativeSurfaceType win,int interval){
    nsSwapInterval(&interval);
}

EGLNativeSurfaceType createWindowSurface(EGLNativeWindowType wnd){
    return (EGLNativeSurfaceType)wnd;
}

EGLNativeSurfaceType createPixmapSurface(EGLNativePixmapType pix){
    return (EGLNativeSurfaceType)pix;
}

void destroySurface(EGLNativeSurfaceType srfc){
}

EGLNativeInternalDisplayType getInternalDisplay(EGLNativeDisplayType dpy){
    return (EGLNativeInternalDisplayType)dpy;
}

void deleteDisplay(EGLNativeInternalDisplayType idpy){
}

};
