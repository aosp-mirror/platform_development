
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


//TODO: implementation for mac for all funcs
namespace EglOS {

EGLNativeDisplayType getDefaultDisplay() {return NULL;}

bool releaseDisplay(EGLNativeDisplayType dpy) {
    return false;
}

EglConfig* pixelFormatToConfig(EGLNativeDisplayType dpy,EGLNativePixelFormatType* frmt){
    return NULL;
}

void queryConfigs(EGLNativeDisplayType dpy,ConfigsList& listOut) {
}

bool validNativeWin(EGLNativeWindowType win) {
   return true;
}

bool validNativePixmap(EGLNativePixmapType pix) {
   return true;
}

bool checkWindowPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativeWindowType win,EglConfig* cfg,unsigned int* width,unsigned int* height) {
    return false;
}

bool checkPixmapPixelFormatMatch(EGLNativeDisplayType dpy,EGLNativePixmapType pix,EglConfig* cfg,unsigned int* width,unsigned int* height) {
    return false;
}

EGLNativePbufferType createPbuffer(EGLNativeDisplayType dpy,EglConfig* cfg,EglPbufferSurface* srfc){
    return NULL;
}

bool releasePbuffer(EGLNativeDisplayType dis,EGLNativePbufferType pb) {
    return true;
}

EGLNativeContextType createContext(EGLNativeDisplayType dpy,EglConfig* cfg,EGLNativeContextType sharedContext) {
 return NULL;
}

bool destroyContext(EGLNativeDisplayType dpy,EGLNativeContextType ctx) {
    return false;
}



bool makeCurrent(EGLNativeDisplayType dpy,EglSurface* read,EglSurface* draw,EGLNativeContextType ctx){
    return false;
}

void swapBuffers(EGLNativeDisplayType dpy,EGLNativeWindowType win) {
}

void waitNative() {
}

void swapInterval(EGLNativeDisplayType dpy,EGLNativeWindowType win,int interval){
}

};
