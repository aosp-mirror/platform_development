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
extern ANPTypefaceInterfaceV0  gTypefaceI;
extern ANPWindowInterfaceV0  gWindowI;

///////////////////////////////////////////////////////////////////////////////

VideoPlugin::VideoPlugin(NPP inst) : SurfaceSubPlugin(inst) {

    // initialize the drawing surface
    m_surface = NULL;

    //register for touch events
    ANPEventFlags flags = kTouch_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(inst, kError_ANPLogType, "Error selecting input events.");
    }
}

VideoPlugin::~VideoPlugin() {
    surfaceDestroyed();
}

bool VideoPlugin::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kSurface_ANPDrawingModel);
}

bool VideoPlugin::isFixedSurface() {
    return true;
}

void VideoPlugin::surfaceCreated(jobject surface) {
    m_surface = surface;
    drawPlugin();
}

void VideoPlugin::surfaceChanged(int format, int width, int height) {
    gLogI.log(inst(), kDebug_ANPLogType, "----%p SurfaceChanged Event: %d",
              inst(), format);
    drawPlugin();
}

void VideoPlugin::surfaceDestroyed() {
    JNIEnv* env = NULL;
    if (m_surface && gVM->GetEnv((void**) &env, JNI_VERSION_1_4) == JNI_OK) {
        env->DeleteGlobalRef(m_surface);
        m_surface = NULL;
    }
}

void VideoPlugin::drawPlugin() {

    ANPBitmap bitmap;
    JNIEnv* env = NULL;
    if (!m_surface || gVM->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK ||
        !gSurfaceI.lock(env, m_surface, &bitmap, NULL)) {
            gLogI.log(inst(), kError_ANPLogType, "----%p Unable to Lock Surface", inst());
            return;
    }

    ANPCanvas* canvas = gCanvasI.newCanvas(&bitmap);

    // get the plugin's dimensions according to the DOM
    PluginObject *obj = (PluginObject*) inst()->pdata;
    const int pW = obj->window->width;
    const int pH = obj->window->height;

    // compare DOM dimensions to the plugin's surface dimensions
    if (pW != bitmap.width || pH != bitmap.height)
        gLogI.log(inst(), kError_ANPLogType,
                  "----%p Invalid Surface Dimensions (%d,%d):(%d,%d)",
                  inst(), pW, pH, bitmap.width, bitmap.height);

    // set constants
    const int fontSize = 16;
    const int leftMargin = 10;

    gCanvasI.drawColor(canvas, 0xFFCDCDCD);

    ANPPaint* paint = gPaintI.newPaint();
    gPaintI.setFlags(paint, gPaintI.getFlags(paint) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(paint, 0xFFFF0000);
    gPaintI.setTextSize(paint, fontSize);

    ANPTypeface* tf = gTypefaceI.createFromName("serif", kItalic_ANPTypefaceStyle);
    gPaintI.setTypeface(paint, tf);
    gTypefaceI.unref(tf);

    ANPFontMetrics fm;
    gPaintI.getFontMetrics(paint, &fm);

    gPaintI.setColor(paint, 0xFF0000FF);
    const char c[] = "Touch anywhere on the plugin to begin video playback!";
    gCanvasI.drawText(canvas, c, sizeof(c)-1, leftMargin, -fm.fTop, paint);

    // clean up variables and unlock the surface
    gPaintI.deletePaint(paint);
    gCanvasI.deleteCanvas(canvas);
    gSurfaceI.unlock(env, m_surface);
}

int16 VideoPlugin::handleEvent(const ANPEvent* evt) {
    switch (evt->eventType) {
        case kDraw_ANPEventType:
            gLogI.log(inst(), kError_ANPLogType, " ------ %p the plugin did not request draw events", inst());
            break;
        case kTouch_ANPEventType:
            if (kDown_ANPTouchAction == evt->data.touch.action) {
                gLogI.log(inst(), kDebug_ANPLogType, " ------ %p requesting fullscreen mode", inst());
                gWindowI.requestFullScreen(inst());
            }
            return 1;
        case kKey_ANPEventType:
            gLogI.log(inst(), kError_ANPLogType, " ------ %p the plugin did not request key events", inst());
            break;
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}
