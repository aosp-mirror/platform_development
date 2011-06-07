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

#define IS_TRUE(a) \
        if(a != true) return false;




struct DisplayInfo{
    DisplayInfo():dc(NULL),hwnd(NULL),isPixelFormatSet(false){};
    DisplayInfo(HDC hdc,HWND wnd):isPixelFormatSet(false){dc = hdc; hwnd = wnd;};
    HDC  dc;
    HWND hwnd;
    bool isPixelFormatSet;
};

class WinDisplay{
public:
     typedef enum {
                      DEFAULT_DISPLAY = 0
                  };
     WinDisplay():m_current(DEFAULT_DISPLAY){};
     DisplayInfo& getInfo(int configurationIndex){ return m_map[configurationIndex];}
     HDC  getCurrentDC(){return m_map[m_current].dc;}
     void setDefault(const DisplayInfo& info){m_map[DEFAULT_DISPLAY] = info; m_current = DEFAULT_DISPLAY;}
     void setCurrent(int configurationIndex,const DisplayInfo& info);
     bool isCurrentPixelFormatSet(){ return m_map[m_current].isPixelFormatSet;}
     void currentPixelFormatWasSet(){m_map[m_current].isPixelFormatSet = true;}
     void releaseAll();
private:
    std::map<int,DisplayInfo> m_map;
    int                       m_current;
};

void WinDisplay::releaseAll(){
    for(std::map<int,DisplayInfo>::iterator it = m_map.begin(); it != m_map.end();it++){
       DestroyWindow((*it).second.hwnd);
       DeleteDC((*it).second.dc);
    }
}

void WinDisplay::setCurrent(int configurationIndex,const DisplayInfo& info){
    if(m_map.find(configurationIndex) != m_map.end()) return;
    m_map[configurationIndex] = info;
    m_current = configurationIndex;
}
namespace EglOS{

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

EGLNativeDisplayType getDefaultDisplay() {
    WinDisplay* dpy = new WinDisplay();

    HWND hwnd = createDummyWindow();
    HDC  hdc  =  GetDC(hwnd);
    dpy->setDefault(DisplayInfo(hdc,hwnd));
    return static_cast<EGLNativeDisplayType>(dpy);
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
        printf("error while creating dummy context %d\n",err);
    }
    if(!wglMakeCurrent(dpy,ctx)){
        err =  GetLastError();
        printf("error while making dummy context current %d\n",err);
    }

    if(!s_wglExtProcs){
        s_wglExtProcs = new WglExtProcs();
        s_wglExtProcs->wglGetPixelFormatAttribivARB = (PFNWGLGETPIXELFORMATATTRIBIVARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_pixel_format","wglGetPixelFormatAttribivARB");
        s_wglExtProcs->wglChoosePixelFormatARB      = (PFNWGLCHOOSEPIXELFORMATARBPROC)wglGetExtentionsProcAddress(dpy,"WGL_ARB_pixel_format","wglPixelFormatARB");
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

bool releaseDisplay(EGLNativeDisplayType dpy) {
    dpy->releaseAll();
    return true;
}



EglConfig* pixelFormatToConfig(EGLNativeDisplayType display,int renderableType,EGLNativePixelFormatType* frmt,int index){

    EGLint  red,green,blue,alpha,depth,stencil;
    EGLint  supportedSurfaces,visualType,visualId;
    EGLint  transparentType,samples;
    EGLint  tRed,tGreen,tBlue;
    EGLint  pMaxWidth,pMaxHeight,pMaxPixels;
    EGLint  configId,level;
    EGLint  window,bitmap,pbuffer,transparent;
    HDC dpy = display->getCurrentDC();

    if(frmt->iPixelType != PFD_TYPE_RGBA) return NULL; // other formats are not supported yet
    if(!((frmt->dwFlags & PFD_SUPPORT_OPENGL) && (frmt->dwFlags & PFD_DOUBLEBUFFER))) return NULL; //pixel format does not supports opengl or double buffer

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


void queryConfigs(EGLNativeDisplayType display,int renderableType,ConfigsList& listOut) {
    PIXELFORMATDESCRIPTOR  pfd;
    int  iPixelFormat = 1;
    HDC dpy = display->getCurrentDC();


    //quering num of formats
    int nFormats = DescribePixelFormat(dpy, iPixelFormat,sizeof(PIXELFORMATDESCRIPTOR), &pfd);

    //inserting rest of formats
    for(iPixelFormat;iPixelFormat < nFormats; iPixelFormat++) {
         DescribePixelFormat(dpy, iPixelFormat,sizeof(PIXELFORMATDESCRIPTOR), &pfd);
         EglConfig* pConfig = pixelFormatToConfig(display,renderableType,&pfd,iPixelFormat);
         if(pConfig) listOut.push_back(pConfig);
    }

}


bool validNativeWin(EGLNativeDisplayType dpy,EGLNativeWindowType win) {
    return IsWindow(win);
}

bool validNativePixmap(EGLNativeDisplayType dpy,EGLNativePixmapType pix) {
    BITMAP bm;
    return GetObject(pix, sizeof(BITMAP), (LPSTR)&bm);
}

static bool setPixelFormat(HDC dc,EglConfig* cfg) {
   EGLNativePixelFormatType frmt = cfg->nativeConfig();
   int iPixelFormat = ChoosePixelFormat(dc,&frmt);
   if(!iPixelFormat) return false;
   return SetPixelFormat(dc,iPixelFormat,&frmt);
}


bool checkWindowPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativeWindowType win,EglConfig* cfg,unsigned int* width,unsigned int* height) {
   RECT r;
   if(!GetClientRect(win,&r)) return false;
   *width  = r.right  - r.left;
   *height = r.bottom - r.top;
   HDC dc = GetDC(win);
   bool ret = setPixelFormat(dc,cfg);
   DeleteDC(dc);
   return ret;
}

bool checkPixmapPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativePixmapType pix,EglConfig* cfg,unsigned int* width,unsigned int* height){

    BITMAP bm;
    if(!GetObject(pix, sizeof(BITMAP), (LPSTR)&bm)) return false;

    *width  = bm.bmWidth;
    *height = bm.bmHeight;

    return true;
}

EGLNativePbufferType createPbuffer(EGLNativeDisplayType display,EglConfig* cfg,EglPbufferSurface* pbSurface) {

  //converting configuration into WGL pixel Format
   EGLint red,green,blue,alpha,depth,stencil;
   bool   gotAttribs = cfg->getConfAttrib(EGL_RED_SIZE,&red)     &&
                       cfg->getConfAttrib(EGL_GREEN_SIZE,&green) &&
                       cfg->getConfAttrib(EGL_BLUE_SIZE,&blue)   &&
                       cfg->getConfAttrib(EGL_ALPHA_SIZE,&alpha) &&
                       cfg->getConfAttrib(EGL_DEPTH_SIZE,&depth) &&
                       cfg->getConfAttrib(EGL_STENCIL_SIZE,&stencil) ;

 if(!gotAttribs) return false;
 int wglPixelFormatAttribs[] = {
                                WGL_SUPPORT_OPENGL_ARB       ,TRUE,
                                WGL_DRAW_TO_PBUFFER_ARB      ,TRUE,
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

    HDC dpy = display->getCurrentDC();
    int pixfmt;
    unsigned int numpf;
    if(!s_wglExtProcs->wglChoosePixelFormatARB || !s_wglExtProcs->wglChoosePixelFormatARB(dpy,wglPixelFormatAttribs, NULL, 1, &pixfmt, &numpf)) {
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
        wglTexFormat = WGL_TEXTURE_RGBA_ARB;
        break;
    }

    int pbAttribs[] = {
                       WGL_TEXTURE_TARGET_ARB   ,wglTexTarget,
                       WGL_TEXTURE_FORMAT_ARB   ,wglTexFormat,
                       0
                      };
    if(!s_wglExtProcs->wglCreatePbufferARB) return NULL;
    EGLNativePbufferType pb = s_wglExtProcs->wglCreatePbufferARB(dpy,pixfmt,width,height,pbAttribs);
    if(!pb) {
        DWORD err = GetLastError();
        return NULL;
    }
    return pb;
}

bool releasePbuffer(EGLNativeDisplayType display,EGLNativePbufferType pb) {
    HDC  dis =  display->getCurrentDC();
    if(!s_wglExtProcs->wglReleasePbufferDCARB || !s_wglExtProcs->wglDestroyPbufferARB) return false;
    if(!s_wglExtProcs->wglReleasePbufferDCARB(pb,dis) || !s_wglExtProcs->wglDestroyPbufferARB(pb)){
        DWORD err = GetLastError();
        return false;
    }
    return true;
}

EGLNativeContextType createContext(EGLNativeDisplayType display,EglConfig* cfg,EGLNativeContextType sharedContext) {

    EGLNativeContextType ctx = NULL;
    HWND hwnd = createDummyWindow();
    HDC  dpy  =  GetDC(hwnd);
    display->setCurrent(cfg->id(),DisplayInfo(dpy,hwnd));

    if(!display->isCurrentPixelFormatSet()){
        if(!setPixelFormat(dpy,cfg)) return NULL;
        display->currentPixelFormatWasSet();
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

bool destroyContext(EGLNativeDisplayType dpy,EGLNativeContextType ctx) {
    if(!wglDeleteContext(ctx)) {
        DWORD err = GetLastError();
        return false;
    }
    return true;
}


HDC getSurfaceDC(EGLNativeDisplayType dpy,EglSurface* srfc){
    switch(srfc->type()){
    case EglSurface::WINDOW:
        return GetDC(static_cast<EGLNativeWindowType>(srfc->native()));
    case EglSurface::PBUFFER:
        if(!s_wglExtProcs->wglGetPbufferDCARB) return NULL;
        return s_wglExtProcs->wglGetPbufferDCARB(static_cast<EGLNativePbufferType>(srfc->native()));
    case EglSurface::PIXMAP: //not supported;
    default:
        return NULL;
    }
}

bool releaseSurfaceDC(EGLNativeDisplayType dpy,HDC dc,EglSurface*srfc){
    switch(srfc->type()){
    case EglSurface::WINDOW:
         ReleaseDC(static_cast<EGLNativeWindowType>(srfc->native()),dc);
         return true;
    case EglSurface::PBUFFER:
        if(!s_wglExtProcs->wglReleasePbufferDCARB) return false;
        s_wglExtProcs->wglReleasePbufferDCARB(static_cast<EGLNativePbufferType>(srfc->native()),dc);
        return true;
    case EglSurface::PIXMAP: //not supported;
    default:
        return false;
    }
}

bool makeCurrent(EGLNativeDisplayType display,EglSurface* read,EglSurface* draw,EGLNativeContextType ctx) {

    HDC hdcRead = getSurfaceDC(display,read);
    HDC hdcDraw = getSurfaceDC(display,draw);
    bool retVal = false;

    if(!s_wglExtProcs->wglMakeContextCurrentARB ){
        if(hdcRead == hdcDraw){
            return wglMakeCurrent(hdcDraw,ctx);
        }
        if(!hdcRead || !hdcDraw) return false;
    }
    retVal = s_wglExtProcs->wglMakeContextCurrentARB(hdcDraw,hdcRead,ctx);

    releaseSurfaceDC(display,hdcRead,read);
    releaseSurfaceDC(display,hdcDraw,draw);
    return retVal;
}

void swapBuffers(EGLNativeDisplayType display,EGLNativeWindowType win) {

    HDC dc  = GetDC(win);
    if(!SwapBuffers(dc)) {
        DWORD err = GetLastError();
    }
    ReleaseDC(win,dc);

}


void waitNative(){}

void swapInterval(EGLNativeDisplayType dpy,EGLNativeWindowType win,int interval) {

    if (s_wglExtProcs->wglSwapIntervalEXT){
        s_wglExtProcs->wglSwapIntervalEXT(interval);
    }
}

};
