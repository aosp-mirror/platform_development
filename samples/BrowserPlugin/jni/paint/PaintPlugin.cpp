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
extern ANPPathInterfaceV0       gPathI;
extern ANPSurfaceInterfaceV0    gSurfaceI;
extern ANPTypefaceInterfaceV0   gTypefaceI;

///////////////////////////////////////////////////////////////////////////////

PaintPlugin::PaintPlugin(NPP inst) : SubPlugin(inst) {

    m_isTouchActive = false;
    m_isTouchCurrentInput = true;
    m_activePaintColor = s_redColor;

    memset(&m_drawingSurface, 0, sizeof(m_drawingSurface));
    memset(&m_inputToggle,  0, sizeof(m_inputToggle));
    memset(&m_colorToggle, 0, sizeof(m_colorToggle));
    memset(&m_clearSurface,  0, sizeof(m_clearSurface));

    // initialize the drawing surface
    m_surfaceReady = false;
    m_surface = gSurfaceI.newSurface(inst, kRGBA_ANPSurfaceType, true);
    if(!m_surface)
        gLogI.log(inst, kError_ANPLogType, "----%p Unable to create RGBA surface", inst);

    // initialize the path
    m_touchPath = gPathI.newPath();
    if(!m_touchPath)
        gLogI.log(inst, kError_ANPLogType, "----%p Unable to create the touch path", inst);

    // initialize the paint colors
    m_paintSurface = gPaintI.newPaint();
    gPaintI.setFlags(m_paintSurface, gPaintI.getFlags(m_paintSurface) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintSurface, 0xFFC0C0C0);
    gPaintI.setTextSize(m_paintSurface, 18);

    m_paintButton = gPaintI.newPaint();
    gPaintI.setFlags(m_paintButton, gPaintI.getFlags(m_paintButton) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paintButton, 0xFFA8A8A8);

    // initialize the typeface (set the colors)
    ANPTypeface* tf = gTypefaceI.createFromName("serif", kItalic_ANPTypefaceStyle);
    gPaintI.setTypeface(m_paintSurface, tf);
    gTypefaceI.unref(tf);

    //register for touch events
    ANPEventFlags flags = kTouch_ANPEventFlag;
    NPError err = browser->setvalue(inst, kAcceptEvents_ANPSetValue, &flags);
    if (err != NPERR_NO_ERROR) {
        gLogI.log(inst, kError_ANPLogType, "Error selecting input events.");
    }
}

PaintPlugin::~PaintPlugin() {
    gSurfaceI.deleteSurface(m_surface);
    gPathI.deletePath(m_touchPath);
    gPaintI.deletePaint(m_paintSurface);
    gPaintI.deletePaint(m_paintButton);
}

bool PaintPlugin::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kSurface_ANPDrawingModel);
}

ANPCanvas* PaintPlugin::getCanvas(ANPRectI* dirtyRect) {

    ANPBitmap bitmap;
    if (!m_surfaceReady || !gSurfaceI.lock(m_surface, &bitmap, dirtyRect))
        return NULL;

    ANPCanvas* canvas = gCanvasI.newCanvas(&bitmap);

    // clip the canvas to the dirty rect b/c the surface is only required to
    // copy a minimum of the dirty rect and may copy more. The clipped canvas
    // however will never write to pixels outside of the clipped area.
    if (dirtyRect) {
        ANPRectF clipR;
        clipR.left = dirtyRect->left;
        clipR.top = dirtyRect->top;
        clipR.right = dirtyRect->right;
        clipR.bottom = dirtyRect->bottom;
        gCanvasI.clipRect(canvas, &clipR);
    }

    return canvas;
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
    const char* inputText = m_isTouchCurrentInput ? "Touch" : "Mouse";
    gCanvasI.drawText(canvas, inputText, strlen(inputText), m_inputToggle.left + 5,
                      m_inputToggle.top - fontMetrics.fTop, m_paintSurface);

    // draw the color selector button
    m_colorToggle.left = (W/2) - (buttonWidth/2);
    m_colorToggle.top = H - buttonHeight - 5;
    m_colorToggle.right = m_colorToggle.left + buttonWidth;
    m_colorToggle.bottom = m_colorToggle.top + buttonHeight;
    gCanvasI.drawRect(canvas, &m_colorToggle, m_paintButton);
    const char* colorText = getColorText();
    gCanvasI.drawText(canvas, colorText, strlen(colorText), m_colorToggle.left + 5,
                      m_colorToggle.top - fontMetrics.fTop, m_paintSurface);

    // draw the clear canvas button
    m_clearSurface.left = W - buttonWidth - 5;
    m_clearSurface.top = H - buttonHeight - 5;
    m_clearSurface.right = m_clearSurface.left + buttonWidth;
    m_clearSurface.bottom = m_clearSurface.top + buttonHeight;
    gCanvasI.drawRect(canvas, &m_clearSurface, m_paintButton);
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

    if (m_activePaintColor == s_blueColor)
        return "Blue";
    else if (m_activePaintColor == s_greenColor)
        return "Green";
    else
        return "Red";
}

int16 PaintPlugin::handleEvent(const ANPEvent* evt) {
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
                case kChanged_ANPSurfaceAction:
                    // get the plugin's dimensions according to the DOM
                    PluginObject *obj = (PluginObject*) inst()->pdata;
                    const int pW = obj->window->width;
                    const int pH = obj->window->height;
                    // get the plugin's surface dimensions
                    const int sW = evt->data.surface.data.changed.width;
                    const int sH = evt->data.surface.data.changed.height;
                    if (pW != sW || pH != sH)
                        gLogI.log(inst(), kError_ANPLogType,
                                  "----%p Invalid Surface Dimensions (%d,%d):(%d,%d)",
                                  inst(), pW, pH, sW, sH);
                    return 1;
            }
            break;

        case kTouch_ANPEventType: {
            float x = (float) evt->data.touch.x;
            float y = (float) evt->data.touch.y;
            if (kDown_ANPTouchAction == evt->data.touch.action && m_isTouchCurrentInput) {

                ANPRectF* rect = validTouch(evt->data.touch.x, evt->data.touch.y);
                if(rect == &m_drawingSurface) {
                    m_isTouchActive = true;
                    gPathI.moveTo(m_touchPath, x, y);
                    paintTouch();
                    return 1;
                }

            } else if (kMove_ANPTouchAction == evt->data.touch.action && m_isTouchActive) {
                gPathI.lineTo(m_touchPath, x, y);
                paintTouch();
                return 1;
            } else if (kUp_ANPTouchAction == evt->data.touch.action && m_isTouchActive) {
                gPathI.lineTo(m_touchPath, x, y);
                paintTouch();
                m_isTouchActive = false;
                gPathI.reset(m_touchPath);
                return 1;
            } else if (kCancel_ANPTouchAction == evt->data.touch.action) {
                m_isTouchActive = false;
                gPathI.reset(m_touchPath);
                return 1;
            }

            break;
        }
        case kMouse_ANPEventType: {

            if (m_isTouchActive)
                gLogI.log(inst(), kError_ANPLogType, "----%p Received unintended mouse event", inst());

            if (kDown_ANPMouseAction == evt->data.mouse.action) {
                ANPRectF* rect = validTouch(evt->data.mouse.x, evt->data.mouse.y);
                if (rect == &m_drawingSurface)
                    paintMouse(evt->data.mouse.x, evt->data.mouse.y);
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
    ANPCanvas* lockedCanvas = getCanvas(&m_inputToggle);
    drawCleanPlugin(lockedCanvas);
}

void PaintPlugin::togglePaintColor() {
    if (m_activePaintColor == s_blueColor)
        m_activePaintColor = s_redColor;
    else if (m_activePaintColor == s_greenColor)
        m_activePaintColor = s_blueColor;
    else
        m_activePaintColor = s_greenColor;

    // lock only the color toggle and redraw the canvas
    ANPCanvas* lockedCanvas = getCanvas(&m_colorToggle);
    drawCleanPlugin(lockedCanvas);
}

void PaintPlugin::paintMouse(int x, int y) {
    //TODO do not paint outside the drawing surface

    //create the paint color
    ANPPaint* fillPaint = gPaintI.newPaint();
    gPaintI.setFlags(fillPaint, gPaintI.getFlags(fillPaint) | kAntiAlias_ANPPaintFlag);
    gPaintI.setStyle(fillPaint, kFill_ANPPaintStyle);
    gPaintI.setColor(fillPaint, m_activePaintColor);

    // handle the simple "mouse" paint (draw a point)
    ANPRectF point;
    point.left =   (float) x-3;
    point.top =    (float) y-3;
    point.right =  (float) x+3;
    point.bottom = (float) y+3;

    // get a canvas that is only locked around the point and draw it
    ANPCanvas* canvas = getCanvas(&point);
    gCanvasI.drawOval(canvas, &point, fillPaint);

    // clean up
    releaseCanvas(canvas);
    gPaintI.deletePaint(fillPaint);
}

void PaintPlugin::paintTouch() {
    //TODO do not paint outside the drawing surface

    //create the paint color
    ANPPaint* strokePaint = gPaintI.newPaint();
    gPaintI.setFlags(strokePaint, gPaintI.getFlags(strokePaint) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(strokePaint, m_activePaintColor);
    gPaintI.setStyle(strokePaint, kStroke_ANPPaintStyle);
    gPaintI.setStrokeWidth(strokePaint, 6.0);
    gPaintI.setStrokeCap(strokePaint, kRound_ANPPaintCap);
    gPaintI.setStrokeJoin(strokePaint, kRound_ANPPaintJoin);

    // handle the complex "touch" paint (draw a line)
    ANPRectF bounds;
    gPathI.getBounds(m_touchPath, &bounds);

    // get a canvas that is only locked around the point and draw the path
    ANPCanvas* canvas = getCanvas(&bounds);
    gCanvasI.drawPath(canvas, m_touchPath, strokePaint);

    // clean up
    releaseCanvas(canvas);
    gPaintI.deletePaint(strokePaint);
}
