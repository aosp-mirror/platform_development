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

#include "PaintPlugin.h"

#include <fcntl.h>
#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*         browser;
extern ANPLogInterfaceV0        gLogI;
extern ANPCanvasInterfaceV0     gCanvasI;
extern ANPPaintInterfaceV0      gPaintI;
extern ANPSurfaceInterfaceV0    gSurfaceI;
extern ANPTypefaceInterfaceV0   gTypefaceI;

///////////////////////////////////////////////////////////////////////////////

PaintPlugin::PaintPlugin(NPP inst) : SubPlugin(inst) {

    m_isTouchCurrentInput = true;
    m_isTouchActive = false;
    m_prevX = m_prevY = 0;

    memset(&m_drawingSurface, 0, sizeof(m_drawingSurface));
    memset(&m_inputToggle,  0, sizeof(m_inputToggle));
    memset(&m_colorToggle, 0, sizeof(m_colorToggle));
    memset(&m_clearSurface,  0, sizeof(m_clearSurface));

    // initialize the drawing surface
    m_surfaceReady = false;
    m_surface = gSurfaceI.newSurface(inst, kRGBA_ANPSurfaceType);
    if(!m_surface)
        gLogI.log(inst, kError_ANPLogType, "----%p Unable to create RGBA surface", inst);

    m_paintSurface = gPaintI.newPaint();
    gPaintI.setFlags(m_paintSurface, gPaintI.getFlags(m_paintSurface) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintSurface, 0xFFC0C0C0);
    gPaintI.setTextSize(m_paintSurface, 18);

    m_paintButton = gPaintI.newPaint();
    gPaintI.setFlags(m_paintButton, gPaintI.getFlags(m_paintButton) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintButton, 0xFFA8A8A8);

    m_paintBlue = gPaintI.newPaint();
    gPaintI.setFlags(m_paintBlue, gPaintI.getFlags(m_paintBlue) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintBlue, 0xFF0000FF);
    gPaintI.setTextSize(m_paintBlue, 18);

    m_paintGreen = gPaintI.newPaint();
    gPaintI.setFlags(m_paintGreen, gPaintI.getFlags(m_paintGreen) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintGreen, 0xFF00FF00);
    gPaintI.setTextSize(m_paintGreen, 18);

    m_paintRed = gPaintI.newPaint();
    gPaintI.setFlags(m_paintRed, gPaintI.getFlags(m_paintRed) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintRed, 0xFFFF0000);
    gPaintI.setTextSize(m_paintRed, 18);

    ANPTypeface* tf = gTypefaceI.createFromName("serif", kItalic_ANPTypefaceStyle);
    gPaintI.setTypeface(m_paintSurface, tf);
    gPaintI.setTypeface(m_paintBlue, tf);
    gPaintI.setTypeface(m_paintGreen, tf);
    gPaintI.setTypeface(m_paintRed, tf);
    gTypefaceI.unref(tf);

    // set the default paint color
    m_activePaint = m_paintRed;

    //register for touch events
    ANPEventFlags flags = kTouch_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(inst, kError_ANPLogType, "Error selecting input events.");
    }
}

PaintPlugin::~PaintPlugin() {
    gPaintI.deletePaint(m_paintSurface);
    gPaintI.deletePaint(m_paintButton);
    gPaintI.deletePaint(m_paintBlue);
    gPaintI.deletePaint(m_paintGreen);
    gPaintI.deletePaint(m_paintRed);
}

bool PaintPlugin::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kSurface_ANPDrawingModel);
}

ANPCanvas* PaintPlugin::getCanvas(ANPRectI* dirtyRect) {

    ANPBitmap bitmap;
    if (!m_surfaceReady || !gSurfaceI.lock(m_surface, &bitmap, dirtyRect))
        return NULL;
    return gCanvasI.newCanvas(&bitmap);
}

ANPCanvas* PaintPlugin::getCanvas(ANPRectF* dirtyRect) {

    ANPRectI newRect;
    newRect.left = (int) dirtyRect->left;
    newRect.top = (int) dirtyRect->top;
    newRect.right = (int) dirtyRect->right;
    newRect.bottom = (int) dirtyRect->bottom;

    return getCanvas(&newRect);
}

void PaintPlugin::releaseCanvas(ANPCanvas* canvas) {
    gSurfaceI.unlock(m_surface);
    gCanvasI.deleteCanvas(canvas);
}

void PaintPlugin::drawCleanPlugin(ANPCanvas* canvas) {
    NPP instance = this->inst();
    PluginObject *obj = (PluginObject*) instance->pdata;

    // if no canvas get a locked canvas
    if (!canvas)
        canvas = getCanvas();

    if (!canvas)
        return;

    const float buttonWidth = 60;
    const float buttonHeight = 30;
    const int W = obj->window->width;
    const int H = obj->window->height;

    // color the plugin canvas
    gCanvasI.drawColor(canvas, 0xFFCDCDCD);

    // get font metrics
    ANPFontMetrics fontMetrics;
    gPaintI.getFontMetrics(m_paintSurface, &fontMetrics);

    // draw the input toggle button
    m_inputToggle.left = 5;
    m_inputToggle.top = H - buttonHeight - 5;
    m_inputToggle.right = m_inputToggle.left + buttonWidth;
    m_inputToggle.bottom = m_inputToggle.top + buttonHeight;
    gCanvasI.drawRect(canvas, &m_inputToggle, m_paintButton);
    // draw the play box (under track box)
    const char* inputText = m_isTouchCurrentInput ? "Touch" : "Mouse";
    gCanvasI.drawText(canvas, inputText, strlen(inputText), m_inputToggle.left + 5,
                      m_inputToggle.top - fontMetrics.fTop, m_paintSurface);

    // draw the color selector button
    m_colorToggle.left = (W/2) - (buttonWidth/2);
    m_colorToggle.top = H - buttonHeight - 5;
    m_colorToggle.right = m_colorToggle.left + buttonWidth;
    m_colorToggle.bottom = m_colorToggle.top + buttonHeight;
    gCanvasI.drawRect(canvas, &m_colorToggle, m_paintButton);
    // draw the play box (under track box)
    const char* colorText = getColorText();
    gCanvasI.drawText(canvas, colorText, strlen(colorText), m_colorToggle.left + 5,
                      m_colorToggle.top - fontMetrics.fTop, m_paintSurface);

    // draw the clear canvas button
    m_clearSurface.left = W - buttonWidth - 5;
    m_clearSurface.top = H - buttonHeight - 5;
    m_clearSurface.right = m_clearSurface.left + buttonWidth;
    m_clearSurface.bottom = m_clearSurface.top + buttonHeight;
    gCanvasI.drawRect(canvas, &m_clearSurface, m_paintButton);
    // draw the play box (under track box)
    const char* clearText = "Clear";
    gCanvasI.drawText(canvas, clearText, strlen(clearText), m_clearSurface.left + 5,
                      m_clearSurface.top - fontMetrics.fTop, m_paintSurface);

    // draw the drawing surface box (5 px from the edge)
    m_drawingSurface.left = 5;
    m_drawingSurface.top = 5;
    m_drawingSurface.right = W - 5;
    m_drawingSurface.bottom = m_colorToggle.top - 5;
    gCanvasI.drawRect(canvas, &m_drawingSurface, m_paintSurface);

    // release the canvas
    releaseCanvas(canvas);
}

const char* PaintPlugin::getColorText() {

    if (m_activePaint == m_paintBlue)
        return "Blue";
    else if (m_activePaint == m_paintGreen)
        return "Green";
    else
        return "Red";
}

int16 PaintPlugin::handleEvent(const ANPEvent* evt) {
    NPP instance = this->inst();

    switch (evt->eventType) {
        case kSurface_ANPEventType:
            switch (evt->data.surface.action) {
                case kCreated_ANPSurfaceAction:
                    m_surfaceReady = true;
                    drawCleanPlugin();
                    return 1;
                case kDestroyed_ANPSurfaceAction:
                    m_surfaceReady = false;
                    return 1;
            }
            break;

        case kTouch_ANPEventType: {
            int x = evt->data.touch.x;
            int y = evt->data.touch.y;
            if (kDown_ANPTouchAction == evt->data.touch.action) {

                ANPRectF* rect = validTouch(x,y);
                if(rect == &m_drawingSurface) {
                    m_isTouchActive = true;
                    m_prevX = x;
                    m_prevY = y;
                    paint(x, y, true);
                    return 1;
                }

            } else if (kMove_ANPTouchAction == evt->data.touch.action && m_isTouchActive) {
                paint(x, y, true);
                return 1;
            } else if (kUp_ANPTouchAction == evt->data.touch.action && m_isTouchActive) {
                paint(x, y, true);
                m_isTouchActive = false;
                return 1;
            } else if (kCancel_ANPTouchAction == evt->data.touch.action) {
                m_isTouchActive = false;
                return 1;
            }
            break;
        }
        case kMouse_ANPEventType: {
            if (kDown_ANPMouseAction == evt->data.mouse.action) {
                ANPRectF* rect = validTouch(evt->data.mouse.x, evt->data.mouse.y);
                if (rect == &m_drawingSurface)
                    paint(evt->data.mouse.x, evt->data.mouse.y, false);
                else if (rect == &m_inputToggle)
                    toggleInputMethod();
                else if (rect == &m_colorToggle)
                    togglePaintColor();
                else if (rect == &m_clearSurface)
                    drawCleanPlugin();
            }
            return 1;
        }
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}

ANPRectF* PaintPlugin::validTouch(int x, int y) {

    //convert to float
    float fx = (int) x;
    float fy = (int) y;

    if (fx > m_drawingSurface.left && fx < m_drawingSurface.right && fy > m_drawingSurface.top && fy < m_drawingSurface.bottom)
        return &m_drawingSurface;
    else if (fx > m_inputToggle.left && fx < m_inputToggle.right && fy > m_inputToggle.top && fy < m_inputToggle.bottom)
        return &m_inputToggle;
    else if (fx > m_colorToggle.left && fx < m_colorToggle.right && fy > m_colorToggle.top && fy < m_colorToggle.bottom)
        return &m_colorToggle;
    else if (fx > m_clearSurface.left && fx < m_clearSurface.right && fy > m_clearSurface.top && fy < m_clearSurface.bottom)
        return &m_clearSurface;
    else
        return NULL;
}

void PaintPlugin::toggleInputMethod() {
    m_isTouchCurrentInput = !m_isTouchCurrentInput;

    // lock only the input toggle and redraw the canvas
    ANPCanvas* lockedCanvas = getCanvas(&m_colorToggle);
    drawCleanPlugin(lockedCanvas);
}

void PaintPlugin::togglePaintColor() {
    if (m_activePaint == m_paintBlue)
        m_activePaint = m_paintRed;
    else if (m_activePaint == m_paintGreen)
        m_activePaint = m_paintBlue;
    else
        m_activePaint = m_paintGreen;

    // lock only the color toggle and redraw the canvas
    ANPCanvas* lockedCanvas = getCanvas(&m_colorToggle);
    drawCleanPlugin(lockedCanvas);
}

void PaintPlugin::paint(int x, int y, bool isTouch) {
    NPP instance = this->inst();

    // check to make sure the input types match
    if (m_isTouchCurrentInput != isTouch)
        return;

    //TODO do not paint outside the drawing surface (mouse & touch)

    // handle the simple "mouse" paint (draw a point)
    if (!isTouch) {

        ANPRectF point;
        point.left =   (float) x-3;
        point.top =    (float) y-3;
        point.right =  (float) x+3;
        point.bottom = (float) y+3;

        // get a canvas that is only locked around the point
        ANPCanvas* canvas = getCanvas(&point);
        gCanvasI.drawOval(canvas, &point, m_activePaint);
        releaseCanvas(canvas);
        return;
    }

    // TODO handle the complex "touch" paint (draw a line)
}
