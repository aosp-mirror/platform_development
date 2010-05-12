/*
 * Copyright 2010, The Android Open Source Project
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

#include "NavigationPlugin.h"

#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*         browser;
extern ANPLogInterfaceV0        gLogI;
extern ANPCanvasInterfaceV0     gCanvasI;
extern ANPPaintInterfaceV0      gPaintI;
extern ANPTypefaceInterfaceV0   gTypefaceI;
extern ANPWindowInterfaceV0     gWindowI;


static void inval(NPP instance) {
    browser->invalidaterect(instance, NULL);
}

static uint16_t rnd16(float x, int inset) {
    int ix = (int)roundf(x) + inset;
    if (ix < 0) {
        ix = 0;
    }
    return static_cast<uint16_t>(ix);
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

///////////////////////////////////////////////////////////////////////////////

NavigationPlugin::NavigationPlugin(NPP inst) : SubPlugin(inst) {

    m_hasFocus = false;
    m_activeNav = NULL;

    m_paintDisabled = gPaintI.newPaint();
    gPaintI.setFlags(m_paintDisabled, gPaintI.getFlags(m_paintDisabled) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintDisabled, 0xFFFFFFFF);

    m_paintActive = gPaintI.newPaint();
    gPaintI.setFlags(m_paintActive, gPaintI.getFlags(m_paintActive) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintActive, 0xFFFFFF00);

    //register for key events
    ANPEventFlags flags = kKey_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(kError_ANPLogType, "Error selecting input events.");
    }
}

NavigationPlugin::~NavigationPlugin() {
    gPaintI.deletePaint(m_paintDisabled);
    gPaintI.deletePaint(m_paintActive);
}

bool NavigationPlugin::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kBitmap_ANPDrawingModel);
}

void NavigationPlugin::drawPlugin(const ANPBitmap& bitmap, const ANPRectI& clip) {
    ANPCanvas* canvas = gCanvasI.newCanvas(&bitmap);

    ANPRectF clipR;
    clipR.left = clip.left;
    clipR.top = clip.top;
    clipR.right = clip.right;
    clipR.bottom = clip.bottom;
    gCanvasI.clipRect(canvas, &clipR);

    draw(canvas);
    gCanvasI.deleteCanvas(canvas);
}

void NavigationPlugin::draw(ANPCanvas* canvas) {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;

    const int W = obj->window->width;
    const int H = obj->window->height;
    const int Wm = W/2;
    const int Hm = H/2;

    // color the plugin canvas
    gCanvasI.drawColor(canvas, (m_hasFocus) ? 0xFFCDCDCD : 0xFF545454);

    // draw the nav up box (5 px from the top edge)
    m_navUp.left = Wm - 15;
    m_navUp.top = 5;
    m_navUp.right = m_navUp.left + 30;
    m_navUp.bottom = m_navUp.top + 30;
    gCanvasI.drawRect(canvas, &m_navUp, getPaint(&m_navUp));

    // draw the nav down box (5 px from the bottom edge)
    m_navDown.left = Wm - 15;
    m_navDown.top = H - (30 + 5);
    m_navDown.right = m_navDown.left + 30;
    m_navDown.bottom = m_navDown.top + 30;
    gCanvasI.drawRect(canvas, &m_navDown, getPaint(&m_navDown));

    // draw the nav left box (5 px from the left edge)
    m_navLeft.left = 5;
    m_navLeft.top = Hm - 15;
    m_navLeft.right = m_navLeft.left + 30;
    m_navLeft.bottom = m_navLeft.top + 30;
    gCanvasI.drawRect(canvas, &m_navLeft, getPaint(&m_navLeft));

    // draw the nav right box (5 px from the right edge)
    m_navRight.left = W - (30 + 5);
    m_navRight.top = Hm - 15;
    m_navRight.right = m_navRight.left + 30;
    m_navRight.bottom = m_navRight.top + 30;
    gCanvasI.drawRect(canvas, &m_navRight, getPaint(&m_navRight));

    // draw the nav center box
    m_navCenter.left = Wm - 15;
    m_navCenter.top = Hm - 15;
    m_navCenter.right = m_navCenter.left + 30;
    m_navCenter.bottom = m_navCenter.top + 30;
    gCanvasI.drawRect(canvas, &m_navCenter, getPaint(&m_navCenter));

    gLogI.log(kDebug_ANPLogType, "----%p Drawing Plugin", inst());
}

ANPPaint* NavigationPlugin::getPaint(ANPRectF* input) {
    return (input == m_activeNav) ? m_paintActive : m_paintDisabled;
}

int16_t NavigationPlugin::handleEvent(const ANPEvent* evt) {
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
            break;

        case kLifecycle_ANPEventType:
            if (evt->data.lifecycle.action == kLoseFocus_ANPLifecycleAction) {
                gLogI.log(kDebug_ANPLogType, "----%p Loosing Focus", instance);
                m_hasFocus = false;
                inval(instance);
                return 1;
            }
            else if (evt->data.lifecycle.action == kGainFocus_ANPLifecycleAction) {
                gLogI.log(kDebug_ANPLogType, "----%p Gaining Focus", instance);
                m_hasFocus = true;
                inval(instance);
                return 1;
            }
            break;

        case kMouse_ANPEventType:
            return 1;

        case kKey_ANPEventType:
            if (evt->data.key.action == kDown_ANPKeyAction) {
            	bool result = handleNavigation(evt->data.key.nativeCode);
            	inval(instance);
            	return result;
            }
            return 1;

        default:
            break;
    }
    return 0;   // unknown or unhandled event
}

bool NavigationPlugin::handleNavigation(ANPKeyCode keyCode) {
    NPP instance = this->inst();

    gLogI.log(kDebug_ANPLogType, "----%p Received Key %d", instance, keyCode);

    switch (keyCode) {
		case kDpadUp_ANPKeyCode:
			m_activeNav = &m_navUp;
			break;
		case kDpadDown_ANPKeyCode:
			m_activeNav = &m_navDown;
			break;
		case kDpadLeft_ANPKeyCode:
			m_activeNav = &m_navLeft;
			break;
		case kDpadRight_ANPKeyCode:
			m_activeNav = &m_navRight;
			break;
		case kDpadCenter_ANPKeyCode:
			m_activeNav = &m_navCenter;
			break;
		case kQ_ANPKeyCode:
		case kDel_ANPKeyCode:
			m_activeNav = NULL;
			return false;
		default:
			m_activeNav = NULL;
			break;
    }
    return true;
}
