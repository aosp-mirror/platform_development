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
#include "AnimationThread.h"

#include <utils/SystemClock.h>
#include "ANPNativeWindow_npapi.h"

extern ANPLogInterfaceV0           gLogI;
extern ANPNativeWindowInterfaceV0  gNativeWindowI;

AnimationThread::AnimationThread(NPP npp) : RenderingThread(npp) {
    m_counter = 0;
    m_lastPrintTime = android::uptimeMillis();
    m_executionTime = 0;
    m_idleTime = 0;

    m_x = m_y = 0;
    m_dx = 0;
    m_dy = 0;

    memset(&m_oval, 0, sizeof(m_oval));

    m_paint = new SkPaint;
    m_paint->setAntiAlias(true);

    m_bitmap = constructBitmap(0, 0);
    m_canvas = new SkCanvas(*m_bitmap);

    m_startExecutionTime = 0;
    m_startTime = android::uptimeMillis();
    m_stallTime = android::uptimeMillis();

}

AnimationThread::~AnimationThread() {
    delete m_paint;
    delete m_canvas;
    delete m_bitmap;
}

SkBitmap* AnimationThread::constructBitmap(int width, int height) {
    SkBitmap* bitmap = new SkBitmap;
    bitmap->setConfig(SkBitmap::kARGB_8888_Config, width, height);
    bitmap->allocPixels();
    bitmap->eraseColor(0x00000000);
    return bitmap;
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

bool AnimationThread::threadLoop() {
    if (android::uptimeMillis() - m_stallTime < MS_PER_FRAME)
        return true;
    m_stallTime = android::uptimeMillis();

    m_idleTime += android::uptimeMillis() - m_startIdleTime;
    m_startExecutionTime = android::uptimeMillis();

    bool reCreateFlag = false;
    int width, height;
    getDimensions(width, height);

    if (m_bitmap->width() != width || m_bitmap->height() != height) {
        delete m_canvas;
        delete m_bitmap;
        m_bitmap = constructBitmap(width, height);
        m_canvas = new SkCanvas(*m_bitmap);

        // change the ball's speed to match the size
        m_dx = width * .005f;
        m_dy = height * .007f;
        reCreateFlag = true;
    }

    // setup variables
    const float OW = width * .125f;
    const float OH = height * .125f;

    // clear the old oval
    m_bitmap->eraseColor(0x880000FF);

    // update the coordinates of the oval
    bounce(&m_x, &m_dx, width - OW);
    bounce(&m_y, &m_dy, height - OH);

    // draw the new oval
    m_oval.fLeft = m_x;
    m_oval.fTop = m_y;
    m_oval.fRight = m_x + OW;
    m_oval.fBottom = m_y + OH;
    m_paint->setColor(0xAAFF0000);
    m_canvas->drawOval(m_oval, *m_paint);

    if (!reCreateFlag) {
        updateNativeWindow(m_ANW, *m_bitmap);
    } else {
        setupNativeWindow(m_ANW, *m_bitmap);
    }

    m_executionTime += android::uptimeMillis() - m_startExecutionTime;
    m_counter++;

    if (android::uptimeMillis() - m_lastPrintTime > 5000) {
        float fps = m_counter / ((android::uptimeMillis() - m_startTime) / 1000);
        float spf = ((android::uptimeMillis() - m_startTime)) / m_counter;
        float lpf = (m_idleTime) / m_counter;
        float exe = (m_executionTime) / m_counter;
        gLogI.log(kError_ANPLogType, "TEXT: counter(%d) fps(%f) spf(%f) lock(%f) execution(%f)\n", (int)m_counter, fps, spf, lpf, exe);
        m_lastPrintTime = android::uptimeMillis();

        m_counter = 0;
        m_executionTime = 0;
        m_idleTime = 0;
        m_startExecutionTime = 0;
        m_startTime = android::uptimeMillis();
    }

    m_startIdleTime = android::uptimeMillis(); // count delay between frames
    return true;
}
