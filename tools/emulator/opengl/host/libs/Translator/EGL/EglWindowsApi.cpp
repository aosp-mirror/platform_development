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
#include <wingdi.h>
#include <GL/wglext.h>
#include <stdio.h>

#define IS_TRUE(a) \
        if(a != true) return false;


struct DisplayInfo{
    DisplayInfo():dc(NULL),hwnd(NULL),isPixelFormatSet(false){};
    DisplayInfo(HDC hdc,HWND wnd):isPixelFormatSet(false){dc = hdc; hwnd = wnd;};
    HDC  dc;
    HWND hwnd;
    bool isPixelFormatSet;
};

struct TlsData {
    std::map<int,DisplayInfo> m_map;
};

static DWORD s_tlsIndex = 0;

static TlsData *getTLS() {
    TlsData *tls = (TlsData *)TlsGetValue(s_tlsIndex);
    if (!tls) {
        tls = new TlsData();
        TlsSetValue(s_tlsIndex, tls);
    }
    return tls;
}

class WinDisplay{
public:
     typedef enum {
                      DEFAULT_DISPLAY = 0
                  };
     WinDisplay(){};
     DisplayInfo& getInfo(int configurationIndex){ return getTLS()->m_map[configurationIndex];}
     HDC  getDC(int configId){return getTLS()->m_map[configId].dc;}
     void setInfo(int configurationIndex,const DisplayInfo& info);
     bool isPixelFormatSet(int cfgId){ return getTLS()->m_map[cfgId].isPixelFormatSet;}
     void pixelFormatWasSet(int cfgId){getTLS()->m_map[cfgId].isPixelFormatSet = true;}
     bool infoExists(int configurationIndex);
     void releaseAll();
};

void WinDisplay::releaseAll(){
    TlsData * tls = getTLS();
    
    for(std::map<int,DisplayInfo>::iterator it = tls->m_map.begin(); it != tls->m_map.end();it++){
       if((*it).second.hwnd){
           DestroyWindow((*it).second.hwnd);
       }
       DeleteDC((*it).second.dc);
    }
}

bool WinDisplay::infoExists(int configurationIndex){
    return getTLS()->m_map.find(configurationIndex) != getTLS()->m_map.end();
}

void WinDisplay::setInfo(int configurationIndex,const DisplayInfo& info){
    getTLS()->m_map[configurationIndex] = info;
}

struct WglExtProcs{
    PFNWGLGETPIXELFORMATATTRIBIVARBPROC wglGetPixelFormatAttribivARB;
    PFNWGLCHOOSEPIXELFORMATARBPROC wglChoosePixelFormatARB;
    PFNWGLCREATEPBUFFERARBPROC wglCreatePbufferARB;
    PFNWGLRELEASEPBUFFERDCARBPROC wglReleasePbufferDCARB;
    PFNWGLDESTROYPBUFFERARBPROC wglDestroyPbufferARB;
    PFNWGLGETPBUFFERDCARBPROC wglGetPbufferDCARB;
    PFNWGLMAKECONTEXTCURRENTARBPROC wglMakeContextCurrentARB;
    PFNWGLSWAPINTERVALEXTPROC wglSwapIntervalEXT;
};

static WglExtProcs* s_wglExtProcs = NULL;

class SrfcInfo{
public:
    typedef enum {
                 WINDOW  = 0,
                 PBUFFER = 1,
                 PIXMAP  = 2
                 }SurfaceType;
    explicit SrfcInfo(HWND wnd);
    explicit SrfcInfo(HPBUFFERARB pb);
    explicit SrfcInfo(HBITMAP bmap);
    HWND getHwnd(){ return m_hwnd;};
    HDC  getDC(){ return m_hdc;};
    HBITMAP  getBmap(){ return m_bmap;};
    HPBUFFERARB  getPbuffer(){ return m_pb;};
    ~SrfcInfo();
private:
    HWND        m_hwnd;
    HPBUFFERARB m_pb; 
    HBITMAP     m_bmap;
    HDC         m_hdc;
    SurfaceType m_type;
};

SrfcInfo::SrfcInfo(HBITMAP bmap):m_hwnd(NULL),
                                 m_pb(NULL),
                                 m_hdc(NULL),
                                 m_type(PIXMAP){
    m_bmap = bmap;
}

SrfcInfo::SrfcInfo(HWND wnd):m_pb(NULL),
                             m_bmap(NULL),
                             m_type(WINDOW){
    m_hwnd = wnd;
    m_hdc = GetDC(wnd); 
}

SrfcInfo::SrfcInfo(HPBUFFERARB pb):m_hwnd(NULL),
                                   m_bmap(NULL),
                                   m_type(PBUFFER){
    m_pb = pb;
    if(s_wglExtProcs->wglGetPbufferDCARB){
        m_hdc =  s_wglExtProcs->wglGetPbufferDCARB(pb);
    }
}

SrfcInfo::~SrfcInfo(){
    if(m_type == WINDOW){
        ReleaseDC(m_hwnd,m_hdc);
    }
}

namespace EglOS{



PROC wglGetExtentionsProcAddress(HDC hdc,const char *extension_name,const char* proc_name)
{
    // this is pointer to function which returns pointer to string with list of all wgl extensions
    PFNWGLGETEXTENSIONSSTRINGARBPROC _wglGetExtensionsStringARB = NULL;

    // determine pointer to wglGetExtensionsStringEXT function
    _wglGetExtensionsStringARB = (PFNWGLGETEXTENSIONSSTRINGARBPROC) wglGetProcAddress("wglGetExtensionsStringARB");
    if(!_wglGetExtensionsStringARB){
        fprintf(stderr,"could not get wglGetExtensionsStringARB\n");
        return NULL;
    }

    if (!_wglGetExtensionsStringARB || strstr(_wglGetExtensionsStringARB(hdc), extension_name) == NULL)
    {
        fprintf(stderr,"extension %s was not found\n",extension_name);
        // string was not found
        return NULL;
    }

    // extension is supported
    return wglGetProcAddress(proc_name);
}

LRESULT CALLBACK dummyWndProc(HWND hwnd,UINT uMsg,WPARAM wParam,LPARAM lParam)
{
    return DefWindowProc(hwnd, uMsg, wParam, lParam);
}

HWND createDummyWindow(){

    WNDCLASSEX wcx;
    wcx.cbSize = sizeof(wcx);                       // size of structure
    wcx.style =  CS_OWNDC |CS_HREDRAW |CS_VREDRAW;  // redraw if size changes
    wcx.lpfnWndProc = dummyWndProc;                 // points to window procedure
    wcx.cbClsExtra = 0;                             // no extra class memory
    wcx.cbWndExtra = sizeof(void*);                 // save extra window memory, to store VasWindow instance
    wcx.hInstance = NULL;                           // handle to instance
    wcx.hIcon = NULL;                               // predefined app. icon
    wcx.hCursor = NULL;
    wcx.hbrBackground = NULL;                       // no background brush
    wcx.lpszMenuName =  NULL;                       // name of menu resource
    wcx.lpszClassName = "DummyWin";                 // name of window class
    wcx.hIconSm = (HICON) NULL;                     // small class icon

    ATOM winClass = RegisterClassEx(&wcx);
    HWND hwnd = CreateWindowEx(WS_EX_CLIENTEDGE,
                               "DummyWin",
                               "Dummy",
                               WS_POPUP,
                               0,
                               0,
                               1,
                               1,
                               NULL,
                               NULL,
                               0,0);
    return hwnd;
}

EGLNativeInternalDisplayType getDefaultDisplay() {
    if (!s_tlsIndex) s_tlsIndex = TlsAlloc();
    WinDisplay* dpy = new WinDisplay();

    HWND hwnd = createDummyWindow();
    HDC  hdc  =  GetDC(hwnd);
    dpy->setInfo(WinDisplay::DEFAULT_DISPLAY,DisplayInfo(hdc,hwnd));
    return static_cast<EGLNativeInternalDisplayType>(dpy);
}

EGLNativeInternalDisplayType getInternalDisplay(EGLNativeDisplayType display){
    if (!s_tlsIndex) s_tlsIndex = TlsAlloc();
    WinDisplay* dpy = new WinDisplay();
    dpy->setInfo(WinDisplay::DEFAULT_DISPLAY,DisplayInfo(display,NULL));
    return dpy;
}

static HDC getDummyDC(EGLNativeInternalDisplayType display,int cfgId){

    HDC dpy = NULL;
    if(!display->infoExists(cfgId)){
        HWND hwnd = createDummyWindow();
        dpy  = GetDC(hwnd);
        display->setInfo(cfgId,DisplayInfo(dpy,hwnd));
    } else {
        dpy = display->getDC(cfgId);
    }
    return dpy;
}
void initPtrToWglFunctions(){
    HWND hwnd = createDummyWindow();
    HDC dpy =  GetDC(hwnd);
    if(!hwnd || !dpy){
        fprintf(stderr,"error while getting DC\n");
        return;
    }
    EGLNativeContextType ctx = NULL;
    PIXELFORMATDESCRIPTOR pfd = {
                                  sizeof(PIXELFORMATDESCRIPTOR),  //  size of this pfd
                                  1,                     // version number
                                  PFD_DRAW_TO_WINDOW |   // support window
                                  PFD_SUPPORT_OPENGL |   // support OpenGL
                                  PFD_DOUBLEBUFFER,      // double buffered
                                  PFD_TYPE_RGBA,         // RGBA type
                                  24,                    // 24-bit color depth
                                  0, 0, 0, 0, 0, 0,      // color bits ignored
                                  0,                     // no alpha buffer
                                  0,                     // shift bit ignored
                                  0,                     // no accumulation buffer
                                  0, 0, 0, 0,            // accum bits ignored
                                  32,                    // 32-bit z-buffer
                                  0,                     // no stencil buffer
                                  0,                     // no auxiliary buffer
                                  PFD_MAIN_PLANE,        // main layer
                                  0,                     // reserved
                                  0, 0, 0                // layer masks ignored
                                 };

    int  iPixelFormat,err;
    iPixelFormat = ChoosePixelFormat(dpy, &pfd);
    if(iPixelFormat < 0){
        fprintf(stderr,"error while choosing pixel format\n");
        return;
    }
    if(!SetPixelFormat(dpy,iPixelFormat,&pfd)){

        int err = GetLastError();
        fprintf(stderr,"error while setting pixel format 0x%x\n",err);
        return;
    }


    ctx = wglCreateContext(dpy);
    if(!ctx){
        err =  GetLastError();
        fprintf(stderr,"error while creating dummy context %d\n",err);
    }
    if(!wglMakeCurrent(dpy,ctx)){
        err =  GetLastError();
        fprintf(stderr,"error while making dummy context current %d\n",err);
    }

    if(!s_wglExtProcs){
        s_wglExtProcs = new WglExtProcs();
        s_wglExtProcs->wglGetPixelFormatAttribivARB = (PFNWGLGETPIXELFORMATATTRIBIVARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_pixel_format","wglGetPixelFormatAttribivARB");
        s_wglExtProcs->wglChoosePixelFormatARB      = (PFNWGLCHOOSEPIXELFORMATARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_pixel_format","wglChoosePixelFormatARB");
        s_wglExtProcs->wglCreatePbufferARB          = (PFNWGLCREATEPBUFFERARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_pbuffer","wglCreatePbufferARB");
        s_wglExtProcs->wglReleasePbufferDCARB       = (PFNWGLRELEASEPBUFFERDCARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_pbuffer","wglReleasePbufferDCARB");
        s_wglExtProcs->wglDestroyPbufferARB         = (PFNWGLDESTROYPBUFFERARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_pbuffer","wglDestroyPbufferARB");
        s_wglExtProcs->wglGetPbufferDCARB           = (PFNWGLGETPBUFFERDCARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_pbuffer","wglGetPbufferDCARB");
        s_wglExtProcs->wglMakeContextCurrentARB     = (PFNWGLMAKECONTEXTCURRENTARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_make_current_read","wglMakeContextCurrentARB");
        s_wglExtProcs->wglSwapIntervalEXT           = (PFNWGLSWAPINTERVALEXTPROC)wglGetExtentionsProcAddress(dpy,"WGL_EXT_swap_control","wglSwapIntervalEXT");
    }

   wglMakeCurrent(dpy,NULL);
   DestroyWindow(hwnd);
   DeleteDC(dpy);
}

bool releaseDisplay(EGLNativeInternalDisplayType dpy) {
    dpy->releaseAll();
    return true;
}

void deleteDisplay(EGLNativeInternalDisplayType idpy){
    if(idpy){
        delete idpy;
    }
}


static bool initPixelFormat(HDC dc){
    PIXELFORMATDESCRIPTOR  pfd;
    unsigned int numpf;
    int iPixelFormat;

    if(s_wglExtProcs->wglChoosePixelFormatARB) {
        int i0 = 0;
        float f0 = 0.0f;
        return s_wglExtProcs->wglChoosePixelFormatARB(dc,&i0, &f0, 1, &iPixelFormat, &numpf);
    } else {
        return ChoosePixelFormat(dc,&pfd);
    }
}

EglConfig* pixelFormatToConfig(EGLNativeInternalDisplayType display,int renderableType,EGLNativePixelFormatType* frmt,int index){

    EGLint  red,green,blue,alpha,depth,stencil;
    EGLint  supportedSurfaces,visualType,visualId;
    EGLint  transparentType,samples;
    EGLint  tRed,tGreen,tBlue;
    EGLint  pMaxWidth,pMaxHeight,pMaxPixels;
    EGLint  configId,level;
    EGLint  window,bitmap,pbuffer,transparent;
    HDC dpy = getDummyDC(display,WinDisplay::DEFAULT_DISPLAY);

    if(frmt->iPixelType != PFD_TYPE_RGBA) return NULL; // other formats are not supported yet
    if(!((frmt->dwFlags & PFD_SUPPORT_OPENGL) && (frmt->dwFlags & PFD_DOUBLEBUFFER))) return NULL; //pixel format does not supports opengl or double buffer
    if( 0 != (frmt->dwFlags & (PFD_GENERIC_FORMAT | PFD_NEED_PALETTE )) ) return NULL; //discard generic pixel formats as well as pallete pixel formats

    int attribs [] = {
                          WGL_DRAW_TO_WINDOW_ARB,
                          WGL_DRAW_TO_BITMAP_ARB,
                          WGL_DRAW_TO_PBUFFER_ARB,
                          WGL_TRANSPARENT_ARB,
                          WGL_TRANSPARENT_RED_VALUE_ARB,
                          WGL_TRANSPARENT_GREEN_VALUE_ARB,
                          WGL_TRANSPARENT_BLUE_VALUE_ARB
                     };

    supportedSurfaces = 0;
    if(!s_wglExtProcs->wglGetPixelFormatAttribivARB) return NULL;

    IS_TRUE(s_wglExtProcs->wglGetPixelFormatAttribivARB(dpy,index,0,1,&attribs[0],&window));
    IS_TRUE(s_wglExtProcs->wglGetPixelFormatAttribivARB(dpy,index,0,1,&attribs[1],&bitmap));
    IS_TRUE(s_wglExtProcs->wglGetPixelFormatAttribivARB(dpy,index,0,1,&attribs[2],&pbuffer));
    if(window)  supportedSurfaces |= EGL_WINDOW_BIT;
    if(bitmap)  supportedSurfaces |= EGL_PIXMAP_BIT;
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
    samples                   = 0 ;
    level                     = 0 ;

    IS_TRUE(s_wglExtProcs->wglGetPixelFormatAttribivARB(dpy,index,0,1,&attribs[3],&transparent));
    if(transparent) {
        transparentType = EGL_TRANSPARENT_RGB;
        IS_TRUE(s_wglExtProcs->wglGetPixelFormatAttribivARB(dpy,index,0,1,&attribs[4],&tRed));
        IS_TRUE(s_wglExtProcs->wglGetPixelFormatAttribivARB(dpy,index,0,1,&attribs[5],&tGreen));
        IS_TRUE(s_wglExtProcs->wglGetPixelFormatAttribivARB(dpy,index,0,1,&attribs[6],&tBlue));
    } else {
        transparentType = EGL_NONE;
    }

    red     = frmt->cRedBits;
    green   = frmt->cGreenBits;
    blue    = frmt->cBlueBits;
    alpha   = frmt->cAlphaBits;
    depth   = frmt->cDepthBits;
    stencil = frmt->cStencilBits;
    return new EglConfig(red,green,blue,alpha,caveat,(EGLint)index,depth,level,pMaxWidth,pMaxHeight,pMaxPixels,renderable,renderableType,
                         visualId,visualType,samples,stencil,supportedSurfaces,transparentType,tRed,tGreen,tBlue,*frmt);
}


void queryConfigs(EGLNativeInternalDisplayType display,int renderableType,ConfigsList& listOut) {
    PIXELFORMATDESCRIPTOR  pfd;
    int  iPixelFormat = 1;
    HDC dpy = getDummyDC(display,WinDisplay::DEFAULT_DISPLAY);

    //
    // We need to call wglChoosePixelFormat at least once,
    // seems that the driver needs to initialize itself.
    // do it here during initialization.
    //
    initPixelFormat(dpy);

    //quering num of formats
    int nFormats = DescribePixelFormat(dpy, iPixelFormat,sizeof(PIXELFORMATDESCRIPTOR), &pfd);

    //inserting rest of formats
    for(iPixelFormat;iPixelFormat < nFormats; iPixelFormat++) {
         DescribePixelFormat(dpy, iPixelFormat,sizeof(PIXELFORMATDESCRIPTOR), &pfd);
         EglConfig* pConfig = pixelFormatToConfig(display,renderableType,&pfd,iPixelFormat);
         if(pConfig) listOut.push_back(pConfig);
    }
}

bool validNativeWin(EGLNativeInternalDisplayType dpy,EGLNativeWindowType win) {
    return IsWindow(win);
}

bool validNativeWin(EGLNativeInternalDisplayType dpy,EGLNativeSurfaceType win) {
    if (!win) return false;
    return validNativeWin(dpy,win->getHwnd());
}

bool validNativePixmap(EGLNativeInternalDisplayType dpy,EGLNativeSurfaceType pix) {
    BITMAP bm;
    if (!pix) return false;
    return GetObject(pix->getBmap(), sizeof(BITMAP), (LPSTR)&bm);
}

bool checkWindowPixelFormatMatch(EGLNativeInternalDisplayType dpy,EGLNativeWindowType win,EglConfig* cfg,unsigned int* width,unsigned int* height) {
   RECT r;
   if(!GetClientRect(win,&r)) return false;
   *width  = r.right  - r.left;
   *height = r.bottom - r.top;
   HDC dc = GetDC(win);
   EGLNativePixelFormatType nativeConfig = cfg->nativeConfig();
   bool ret = SetPixelFormat(dc,cfg->nativeId(),&nativeConfig);
   DeleteDC(dc);
   return ret;
}

bool checkPixmapPixelFormatMatch(EGLNativeInternalDisplayType dpy,EGLNativePixmapType pix,EglConfig* cfg,unsigned int* width,unsigned int* height){

    BITMAP bm;
    if(!GetObject(pix, sizeof(BITMAP), (LPSTR)&bm)) return false;

    *width  = bm.bmWidth;
    *height = bm.bmHeight;

    return true;
}

EGLNativeSurfaceType createPbufferSurface(EGLNativeInternalDisplayType display,EglConfig* cfg,EglPbufferSurface* pbSurface) {


    HDC dpy = getDummyDC(display,cfg->nativeId());
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
        wglTexFormat = WGL_TEXTURE_RGBA_ARB;
        break;
    }

    int pbAttribs[] = {
                       WGL_TEXTURE_TARGET_ARB   ,wglTexTarget,
                       WGL_TEXTURE_FORMAT_ARB   ,wglTexFormat,
                       0
                      };
    if(!s_wglExtProcs->wglCreatePbufferARB) return NULL;
    EGLNativePbufferType pb = s_wglExtProcs->wglCreatePbufferARB(dpy,cfg->nativeId(),width,height,pbAttribs);
    if(!pb) {
        DWORD err = GetLastError();
        return NULL;
    }
    return new SrfcInfo(pb);
}

bool releasePbuffer(EGLNativeInternalDisplayType display,EGLNativeSurfaceType pb) {
    if (!pb) return false;
    if(!s_wglExtProcs->wglReleasePbufferDCARB || !s_wglExtProcs->wglDestroyPbufferARB) return false;
    if(!s_wglExtProcs->wglReleasePbufferDCARB(pb->getPbuffer(),pb->getDC()) || !s_wglExtProcs->wglDestroyPbufferARB(pb->getPbuffer())){
        DWORD err = GetLastError();
        return false;
    }
    return true;
}

EGLNativeContextType createContext(EGLNativeInternalDisplayType display,EglConfig* cfg,EGLNativeContextType sharedContext) {

    EGLNativeContextType ctx = NULL;
    HDC  dpy  = getDummyDC(display,cfg->nativeId());

    if(!display->isPixelFormatSet(cfg->nativeId())){
        EGLNativePixelFormatType nativeConfig = cfg->nativeConfig();
        if(!SetPixelFormat(dpy,cfg->nativeId(),&nativeConfig)){
            return NULL;
        }
        display->pixelFormatWasSet(cfg->nativeId());
    }

    ctx = wglCreateContext(dpy);

    if(ctx && sharedContext) {
        if(!wglShareLists(sharedContext,ctx)) {
            wglDeleteContext(ctx);
            return NULL;
        }
    }
    return ctx;
}

bool destroyContext(EGLNativeInternalDisplayType dpy,EGLNativeContextType ctx) {
    if(!wglDeleteContext(ctx)) {
        DWORD err = GetLastError();
        return false;
    }
    return true;
}


bool makeCurrent(EGLNativeInternalDisplayType display,EglSurface* read,EglSurface* draw,EGLNativeContextType ctx) {

    HDC hdcRead = read ? read->native()->getDC(): NULL;
    HDC hdcDraw = draw ? draw->native()->getDC(): NULL;
    bool retVal = false;


    if(hdcRead == hdcDraw){
            bool ret =  wglMakeCurrent(hdcDraw,ctx);
            return ret;
    } else if (!s_wglExtProcs->wglMakeContextCurrentARB ) {
        return false;
    }
    retVal = s_wglExtProcs->wglMakeContextCurrentARB(hdcDraw,hdcRead,ctx);

    return retVal;
}

void swapBuffers(EGLNativeInternalDisplayType display,EGLNativeSurfaceType srfc){
    if(srfc && !SwapBuffers(srfc->getDC())) {
        DWORD err = GetLastError();
    }
}


void waitNative(){}

void swapInterval(EGLNativeInternalDisplayType dpy,EGLNativeSurfaceType win,int interval) {

    if (s_wglExtProcs->wglSwapIntervalEXT){
        s_wglExtProcs->wglSwapIntervalEXT(interval);
    }
}

EGLNativeSurfaceType createWindowSurface(EGLNativeWindowType wnd){
    return new SrfcInfo(wnd);
}

EGLNativeSurfaceType createPixmapSurface(EGLNativePixmapType pix){
    return new SrfcInfo(pix);
}

void destroySurface(EGLNativeSurfaceType srfc){
    delete srfc;
}


};
