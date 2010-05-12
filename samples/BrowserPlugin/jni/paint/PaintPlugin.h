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

#include "PluginObject.h"
#include <stdio.h>

#ifndef paintPlugin__DEFINED
#define paintPlugin__DEFINED

class PaintPlugin : public SurfaceSubPlugin {
public:
    PaintPlugin(NPP inst);
    virtual ~PaintPlugin();
    virtual int16_t handleEvent(const ANPEvent* evt);
    virtual jobject getSurface();

private:
    void        drawCleanPlugin(ANPCanvas* canvas = NULL);
    ANPCanvas*  getCanvas(ANPRectI* dirtyRect = NULL);
    ANPCanvas*  getCanvas(ANPRectF* dirtyRect);
    const char* getColorText();
    void        destroySurface();
    void        paintMouse(int x, int y);
    void        paintTouch();
    void        releaseCanvas(ANPCanvas*);
    void        toggleInputMethod();
    void        togglePaintColor();
    ANPRectF*   validTouch(int x, int y);

    bool        m_isTouchActive;
    bool        m_isTouchCurrentInput;

    jobject     m_surface;
    ANPPath*    m_touchPath;

    ANPRectF    m_drawingSurface;
    ANPRectF    m_inputToggle;
    ANPRectF    m_colorToggle;
    ANPRectF    m_fullScreenToggle;
    ANPRectF    m_clearSurface;

    ANPPaint*   m_paintSurface;
    ANPPaint*   m_paintButton;

    ANPColor    m_activePaintColor;
    static const ANPColor s_redColor   = 0xFFFF0000;
    static const ANPColor s_greenColor = 0xFF00FF00;
    static const ANPColor s_blueColor  = 0xFF0000FF;
};

#endif // paintPlugin__DEFINED
