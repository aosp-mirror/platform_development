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

#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*        browser;
extern ANPLogInterfaceV0       gLogI;
extern ANPCanvasInterfaceV0    gCanvasI;
extern ANPPaintInterfaceV0     gPaintI;
extern ANPPathInterfaceV0      gPathI;
extern ANPTypefaceInterfaceV0  gTypefaceI;
extern ANPWindowInterfaceV0    gWindowI;

static void inval(NPP instance) {
    browser->invalidaterect(instance, NULL);
}

static uint16 rnd16(float x, int inset) {
    int ix = (int)roundf(x) + inset;
    if (ix < 0) {
        ix = 0;
    }
    return static_cast<uint16>(ix);
}

static void inval(NPP instance, const ANPRectF& r, bool doAA) {
    const int inset = doAA ? -1 : 0;

    PluginObject *obj = reinterpret_cast<PluginObject*>(instance->pdata);
    NPRect inval;
    inval.left = rnd16(r.left, inset);
    inval.top = rnd16(r.top, inset);
    inval.right = rnd16(r.right, -inset);
    inval.bottom = rnd16(r.bottom, -inset);
    browser->invalidaterect(instance, &inval);
}

static void drawPlugin(SubPlugin* plugin, const ANPBitmap& bitmap, const ANPRectI& clip) {
    ANPCanvas* canvas = gCanvasI.newCanvas(&bitmap);

    ANPRectF clipR;
    clipR.left = clip.left;
    clipR.top = clip.top;
    clipR.right = clip.right;
    clipR.bottom = clip.bottom;
    gCanvasI.clipRect(canvas, &clipR);

    plugin->draw(canvas);
    gCanvasI.deleteCanvas(canvas);
}

uint32_t getMSecs() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (uint32_t) (tv.tv_sec * 1000 + tv.tv_usec / 1000 ); // microseconds to milliseconds
}

///////////////////////////////////////////////////////////////////////////////

BallAnimation::BallAnimation(NPP inst) : SubPlugin(inst) {
    m_x = m_y = 0;
    m_dx = 7 * SCALE;
    m_dy = 5 * SCALE;
    m_scrollX = m_scrollY = m_screenW = m_screenH = 0;
    m_zoom = 1;

    memset(&m_oval, 0, sizeof(m_oval));

    m_paint = gPaintI.newPaint();
    gPaintI.setFlags(m_paint, gPaintI.getFlags(m_paint) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paint, 0xFFFF0000);
    gPaintI.setTextSize(m_paint, 24);

    ANPTypeface* tf = gTypefaceI.createFromName("serif", kItalic_ANPTypefaceStyle);
    gPaintI.setTypeface(m_paint, tf);
    gTypefaceI.unref(tf);

    //register for key and touch events
    ANPEventFlags flags = kKey_ANPEventFlag | kTouch_ANPEventFlag | kVisibleRect_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(inst, kError_ANPLogType, "Error selecting input events.");
    }
}

BallAnimation::~BallAnimation() {
    gPaintI.deletePaint(m_paint);
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

bool BallAnimation::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kBitmap_ANPDrawingModel);
}

void BallAnimation::draw(ANPCanvas* canvas) {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;
    const float OW = 20;
    const float OH = 20;
    const int W = obj->window->width;
    const int H = obj->window->height;

    inval(instance, m_oval, true);  // inval the old
    m_oval.left = m_x;
    m_oval.top = m_y;
    m_oval.right = m_x + OW;
    m_oval.bottom = m_y + OH;
    inval(instance, m_oval, true);  // inval the new

    gCanvasI.drawColor(canvas, 0xFFFFFFFF);

    // test out the Path API
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
#if 0
        gLogI.log(instance, kDebug_ANPLogType, "drawpath: center %g %g bounds [%g %g %g %g]\n",
                  cx, cy,
                  bounds.left, bounds.top, bounds.right, bounds.bottom);
#endif
        gPathI.deletePath(path);
    }

    gPaintI.setColor(m_paint, 0xFFFF0000);
    gCanvasI.drawOval(canvas, &m_oval, m_paint);

    bounce(&m_x, &m_dx, obj->window->width - OW);
    bounce(&m_y, &m_dy, obj->window->height - OH);

    if (mUnichar) {
        ANPFontMetrics fm;
        gPaintI.getFontMetrics(m_paint, &fm);

        gPaintI.setColor(m_paint, 0xFF0000FF);
        char c = static_cast<char>(mUnichar);
        gCanvasI.drawText(canvas, &c, 1, 10, -fm.fTop, m_paint);
    }
}

void BallAnimation::centerPluginOnScreen() {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;
    NPWindow *window = obj->window;

    //find global (x,y) coordinates for center of the plugin
    int pluginCenterX = window->x + (window->width / 2);
    int pluginCenterY = window->y + (window->height / 2);

    gLogI.log(instance, kDebug_ANPLogType, "---- %p Plugin Center: %d,%d : %d,%d",
              instance, pluginCenterX, pluginCenterY, window->x, window->y);

    //find global (x,y) coordinates for center of the visible screen
    int screenCenterX = m_scrollX + (m_screenW / 2);
    int screenCenterY = m_scrollY + (m_screenH / 2);

    gLogI.log(instance, kDebug_ANPLogType, "---- %p Screen Center: %d,%d : %d,%d",
              instance, screenCenterX, screenCenterY, m_scrollX, m_scrollY);

    //compute the delta of the two coordinates
    int deltaX = pluginCenterX - screenCenterX;
    int deltaY = pluginCenterY - screenCenterY;

    gLogI.log(instance, kDebug_ANPLogType, "---- %p Centering: %d,%d : %d,%d",
              instance, deltaX, deltaY, m_scrollX + deltaX, m_scrollY + deltaY);

    //move the visible screen
    //webviewCore...
    // (m_scrollX + deltaX, m_scrollY + deltaY)
    gWindowI.scrollTo(instance, m_scrollX + deltaX, m_scrollY + deltaY);

}

int16 BallAnimation::handleEvent(const ANPEvent* evt) {
    NPP instance = this->inst();

    switch (evt->eventType) {
        case kDraw_ANPEventType:
            switch (evt->data.draw.model) {
                case kBitmap_ANPDrawingModel:
                    drawPlugin(this, evt->data.draw.data.bitmap, evt->data.draw.clip);
                    return 1;
                default:
                    break;   // unknown drawing model
            }

        case kKey_ANPEventType:
            if (evt->data.key.action == kDown_ANPKeyAction) {
                mUnichar = evt->data.key.unichar;
                gLogI.log(instance, kDebug_ANPLogType, "ball downkey event");
                browser->invalidaterect(instance, NULL);
            }
            return 1;
        case kTouch_ANPEventType:
             if (kDown_ANPTouchAction == evt->data.touch.action) {
                 centerPluginOnScreen();
             }
             return 1;

        case kVisibleRect_ANPEventType:
             m_scrollX = evt->data.visibleRect.rect.left;
             m_scrollY = evt->data.visibleRect.rect.top;
             m_screenW = evt->data.visibleRect.rect.right - m_scrollX;
             m_screenH = evt->data.visibleRect.rect.bottom - m_scrollY;
             m_zoom = evt->data.visibleRect.zoomScale;
             gLogI.log(instance, kDebug_ANPLogType, "zoom event %g", m_zoom);
             return 1;
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}
