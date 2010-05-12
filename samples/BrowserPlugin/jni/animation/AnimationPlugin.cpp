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
extern ANPWindowInterfaceV0    gWindowI;

static uint16_t rnd16(float x, int inset) {
    int ix = (int)roundf(x) + inset;
    if (ix < 0) {
        ix = 0;
    }
    return static_cast<uint16_t>(ix);
}

static void inval(NPP instance, const ANPRectF& r, bool doAA) {
    const int inset = doAA ? -1 : 0;

    NPRect inval;
    inval.left = rnd16(r.left, inset);
    inval.top = rnd16(r.top, inset);
    inval.right = rnd16(r.right, -inset);
    inval.bottom = rnd16(r.bottom, -inset);
    browser->invalidaterect(instance, &inval);
}

static void bounce(float* x, float* dx, const float max) {
    *x += *dx;
    if (*x < 0) {
        *x = 0;
        if (*dx < 0) {
            *dx = -*dx;
        }
    } else if (*x > max) {
        *x = max;
        if (*dx > 0) {
            *dx = -*dx;
        }
    }
}
///////////////////////////////////////////////////////////////////////////////

BallAnimation::BallAnimation(NPP inst) : SubPlugin(inst) {
    m_x = m_y = 0;
    m_dx = 7 * SCALE;
    m_dy = 5 * SCALE;

    memset(&m_oval, 0, sizeof(m_oval));

    m_paint = gPaintI.newPaint();
    gPaintI.setFlags(m_paint, gPaintI.getFlags(m_paint) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paint, 0xFFFF0000);

    //register for touch events
    ANPEventFlags flags = kTouch_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(kError_ANPLogType, "Error selecting input events.");
    }
}

BallAnimation::~BallAnimation() {
    gPaintI.deletePaint(m_paint);
}

bool BallAnimation::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kBitmap_ANPDrawingModel);
}

void BallAnimation::drawPlugin(const ANPBitmap& bitmap, const ANPRectI& clip) {

    // create a canvas
    ANPCanvas* canvas = gCanvasI.newCanvas(&bitmap);

    // clip the canvas
    ANPRectF clipR;
    clipR.left = clip.left;
    clipR.top = clip.top;
    clipR.right = clip.right;
    clipR.bottom = clip.bottom;
    gCanvasI.clipRect(canvas, &clipR);

    // setup variables
    PluginObject *obj = (PluginObject*) inst()->pdata;
    const float OW = 20;
    const float OH = 20;
    const int W = obj->window->width;
    const int H = obj->window->height;

    // paint the canvas (using the path API)
    gCanvasI.drawColor(canvas, 0xFFFFFFFF);
    {
        ANPPath* path = gPathI.newPath();

        float cx = W * 0.5f;
        float cy = H * 0.5f;
        gPathI.moveTo(path, 0, 0);
        gPathI.quadTo(path, cx, cy, W, 0);
        gPathI.quadTo(path, cx, cy, W, H);
        gPathI.quadTo(path, cx, cy, 0, H);
        gPathI.quadTo(path, cx, cy, 0, 0);

        gPaintI.setColor(m_paint, 0xFF0000FF);
        gCanvasI.drawPath(canvas, path, m_paint);

        ANPRectF bounds;
        memset(&bounds, 0, sizeof(bounds));
        gPathI.getBounds(path, &bounds);
        gPathI.deletePath(path);
    }

    // draw the oval
    inval(inst(), m_oval, true);  // inval the old
    m_oval.left = m_x;
    m_oval.top = m_y;
    m_oval.right = m_x + OW;
    m_oval.bottom = m_y + OH;
    inval(inst(), m_oval, true);  // inval the new
    gPaintI.setColor(m_paint, 0xFFFF0000);
    gCanvasI.drawOval(canvas, &m_oval, m_paint);

    // update the coordinates of the oval
    bounce(&m_x, &m_dx, obj->window->width - OW);
    bounce(&m_y, &m_dy, obj->window->height - OH);

    // delete the canvas
    gCanvasI.deleteCanvas(canvas);
}

void BallAnimation::showEntirePluginOnScreen() {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;
    NPWindow *window = obj->window;

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
                case kBitmap_ANPDrawingModel:
                    drawPlugin(evt->data.draw.data.bitmap, evt->data.draw.clip);
                    return 1;
                default:
                    break;   // unknown drawing model
            }
        case kTouch_ANPEventType:
             if (kDown_ANPTouchAction == evt->data.touch.action) {
                 showEntirePluginOnScreen();
             }
             return 1;
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}
