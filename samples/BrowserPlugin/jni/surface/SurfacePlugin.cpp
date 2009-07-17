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

#include "SurfacePlugin.h"

#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*        browser;
extern ANPLogInterfaceV0       gLogI;
extern ANPPaintInterfaceV0     gPaintI;
extern ANPSurfaceInterfaceV0   gSurfaceI;
extern ANPTypefaceInterfaceV0  gTypefaceI;
extern ANPWindowInterfaceV0    gWindowI;

///////////////////////////////////////////////////////////////////////////////

SurfacePlugin::SurfacePlugin(NPP inst, ANPSurfaceType surfaceType) : SubPlugin(inst) {

    m_surface = gSurfaceI.newSurface(inst, surfaceType);

    if(!m_surface)
        gLogI.log(inst, kError_ANPLogType, "----%p Unable to create surface (%d)", inst, surfaceType);
}

SurfacePlugin::~SurfacePlugin() {
    if (m_surface)
        gSurfaceI.deleteSurface(m_surface);
}

bool SurfacePlugin::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kSurface_ANPDrawingModel);
}

void SurfacePlugin::draw() {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;

    ANPBitmap bitmap;

    bool value = gSurfaceI.lock(m_surface, &bitmap, NULL);
    gLogI.log(instance, kDebug_ANPLogType, "----%p locking: %b", instance, value);
    gSurfaceI.unlock(m_surface);
}

int16 SurfacePlugin::handleEvent(const ANPEvent* evt) {
    NPP instance = this->inst();

    switch (evt->eventType) {
        case kDraw_ANPEventType:
            switch (evt->data.draw.model) {
                case kSurface_ANPDrawingModel:
                    if (m_surface)
                        draw();
                    return 1;
                default:
                    break;   // unknown drawing model
            }
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}
