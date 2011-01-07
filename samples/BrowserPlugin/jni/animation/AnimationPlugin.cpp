/*
 * Copyright 2008, The Android Open Source Project
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

#include "AnimationPlugin.h"

#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*        browser;
extern ANPLogInterfaceV0       gLogI;
extern ANPCanvasInterfaceV0    gCanvasI;
extern ANPPaintInterfaceV0     gPaintI;
extern ANPPathInterfaceV0      gPathI;
extern ANPSystemInterfaceV0    gSystemI;
extern ANPWindowInterfaceV1    gWindowI;

static uint16_t rnd16(float x, int inset) {
    int ix = (int)roundf(x) + inset;
    if (ix < 0) {
        ix = 0;
    }
    return static_cast<uint16_t>(ix);
}

///////////////////////////////////////////////////////////////////////////////

BallAnimation::BallAnimation(NPP inst) : SurfaceSubPlugin(inst) {
    //register for touch events
    ANPEventFlags flags = kTouch_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(kError_ANPLogType, "Error selecting input events.");
    }

    gLogI.log(kError_ANPLogType, "Starting Rendering Thread");

    //start a thread and do your drawing there
    m_renderingThread = new AnimationThread(inst);
    m_renderingThread->incStrong(inst);
    m_renderingThread->run("AnimationThread");
}

BallAnimation::~BallAnimation() {
    m_renderingThread->requestExitAndWait();
    destroySurface();
}

bool BallAnimation::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kOpenGL_ANPDrawingModel);
}

jobject BallAnimation::getSurface() {

    if (m_surface) {
        return m_surface;
    }

    // load the appropriate java class and instantiate it
    JNIEnv* env = NULL;
    if (gVM->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        gLogI.log(kError_ANPLogType, " ---- getSurface: failed to get env");
        return NULL;
    }

    const char* className = "com.android.sampleplugin.AnimationSurface";
    jclass fullScreenClass = gSystemI.loadJavaClass(inst(), className);

    if(!fullScreenClass) {
        gLogI.log(kError_ANPLogType, " ---- getSurface: failed to load class");
        return NULL;
    }

    jmethodID constructor = env->GetMethodID(fullScreenClass, "<init>", "(Landroid/content/Context;)V");
    jobject fullScreenSurface = env->NewObject(fullScreenClass, constructor, m_context);

    if(!fullScreenSurface) {
        gLogI.log(kError_ANPLogType, " ---- getSurface: failed to construct object");
        return NULL;
    }

    gLogI.log(kError_ANPLogType, " ---- object %p", fullScreenSurface);

    m_surface = env->NewGlobalRef(fullScreenSurface);
    return m_surface;
}

void BallAnimation::destroySurface() {
    JNIEnv* env = NULL;
    if (m_surface && gVM->GetEnv((void**) &env, JNI_VERSION_1_4) == JNI_OK) {
        env->DeleteGlobalRef(m_surface);
        m_surface = NULL;
    }
}

void BallAnimation::showEntirePluginOnScreen() {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;
    NPWindow *window = obj->window;

    // log the current visible rect
    ANPRectI visibleRect = gWindowI.visibleRect(instance);
    gLogI.log(kDebug_ANPLogType, "Current VisibleRect: (%d,%d,%d,%d)",
            visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.bottom);

    ANPRectI visibleRects[1];

    visibleRects[0].left = 0;
    visibleRects[0].top = 0;
    visibleRects[0].right = window->width;
    visibleRects[0].bottom = window->height;

    gWindowI.setVisibleRects(instance, visibleRects, 1);
    gWindowI.clearVisibleRects(instance);
}

int16_t BallAnimation::handleEvent(const ANPEvent* evt) {
    NPP instance = this->inst();

    switch (evt->eventType) {
        case kDraw_ANPEventType:
            switch (evt->data.draw.model) {
                case kOpenGL_ANPDrawingModel: {
                    //send the width and height to the rendering thread
                    int width = evt->data.draw.data.surface.width;
                    int height = evt->data.draw.data.surface.height;
                    gLogI.log(kError_ANPLogType, "New Dimensions (%d,%d)", width, height);
                    m_renderingThread->setDimensions(width, height);
                    return 1;
                }
                default:
                    return 0;   // unknown drawing model
            }
        case kTouch_ANPEventType:
             if (kDown_ANPTouchAction == evt->data.touch.action) {
                 showEntirePluginOnScreen();
             }
            else if (kDoubleTap_ANPTouchAction == evt->data.touch.action) {
                browser->geturl(inst(), "javascript:alert('Detected double tap event.')", 0);
                gWindowI.requestFullScreen(inst());
            }
            return 1;
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}
