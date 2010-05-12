/*
 * Copyright 2009, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "VideoPlugin.h"
#include "android_npapi.h"

#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*        browser;
extern ANPBitmapInterfaceV0    gBitmapI;
extern ANPCanvasInterfaceV0    gCanvasI;
extern ANPLogInterfaceV0       gLogI;
extern ANPPaintInterfaceV0     gPaintI;
extern ANPSurfaceInterfaceV0   gSurfaceI;
extern ANPSystemInterfaceV0    gSystemI;
extern ANPTypefaceInterfaceV0  gTypefaceI;
extern ANPWindowInterfaceV0    gWindowI;

///////////////////////////////////////////////////////////////////////////////

VideoPlugin::VideoPlugin(NPP inst) : SurfaceSubPlugin(inst) {

    // initialize the drawing surface
    m_surface = NULL;

    //register for touch events
    ANPEventFlags flags = kTouch_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(kError_ANPLogType, "Error selecting input events.");
    }
}

VideoPlugin::~VideoPlugin() {
    setContext(NULL);
    destroySurface();
}

jobject VideoPlugin::getSurface() {

    if (m_surface) {
        return m_surface;
    }

    // load the appropriate java class and instantiate it
    JNIEnv* env = NULL;
    if (gVM->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        gLogI.log(kError_ANPLogType, " ---- getSurface: failed to get env");
        return NULL;
    }

    const char* className = "com.android.sampleplugin.VideoSurface";
    jclass videoClass = gSystemI.loadJavaClass(inst(), className);

    if(!videoClass) {
        gLogI.log(kError_ANPLogType, " ---- getSurface: failed to load class");
        return NULL;
    }

    jmethodID constructor = env->GetMethodID(videoClass, "<init>", "(Landroid/content/Context;)V");
    jobject videoSurface = env->NewObject(videoClass, constructor, m_context);

    if(!videoSurface) {
        gLogI.log(kError_ANPLogType, " ---- getSurface: failed to construct object");
        return NULL;
    }

    m_surface = env->NewGlobalRef(videoSurface);
    return m_surface;
}

void VideoPlugin::destroySurface() {
    JNIEnv* env = NULL;
    if (m_surface && gVM->GetEnv((void**) &env, JNI_VERSION_1_4) == JNI_OK) {
        env->DeleteGlobalRef(m_surface);
        m_surface = NULL;
    }
}

int16_t VideoPlugin::handleEvent(const ANPEvent* evt) {
    switch (evt->eventType) {
        case kLifecycle_ANPEventType: {
            switch (evt->data.lifecycle.action) {
                case kEnterFullScreen_ANPLifecycleAction:
                    gLogI.log(kDebug_ANPLogType, " ---- %p entering fullscreen", inst());
                    break;
                case kExitFullScreen_ANPLifecycleAction:
                    gLogI.log(kDebug_ANPLogType, " ---- %p exiting fullscreen", inst());
                    break;
            }
            break; // end kLifecycle_ANPEventType
        }
        case kDraw_ANPEventType:
            gLogI.log(kError_ANPLogType, " ------ %p the plugin did not request draw events", inst());
            break;
        case kTouch_ANPEventType:
            if (kDown_ANPTouchAction == evt->data.touch.action) {
                gLogI.log(kDebug_ANPLogType, " ------ %p requesting fullscreen mode", inst());
                gWindowI.requestFullScreen(inst());
            }
            return 1;
        case kKey_ANPEventType:
            gLogI.log(kError_ANPLogType, " ------ %p the plugin did not request key events", inst());
            break;
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}
