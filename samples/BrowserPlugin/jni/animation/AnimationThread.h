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
#include "RenderingThread.h"
#include "SkCanvas.h"
#include "SkBitmap.h"
#include "SkRect.h"
#include "SkPaint.h"

#ifndef AnimationThread__DEFINED
#define AnimationThread__DEFINED

class AnimationThread : public RenderingThread {
public:
    AnimationThread(NPP npp);
    virtual ~AnimationThread();

private:
    virtual bool threadLoop();
    SkBitmap* constructBitmap(int width, int height);

    float m_counter;

    int64_t m_lastPrintTime;
    int64_t m_executionTime;
    int64_t m_idleTime;
    int64_t m_startTime;
    int64_t m_startExecutionTime;
    int64_t m_startIdleTime;
    int64_t m_stallTime;

    float m_x;
    float m_y;
    float m_dx;
    float m_dy;

    SkRect m_oval;
    SkPaint* m_paint;
    SkBitmap* m_bitmap;
    SkCanvas* m_canvas;
};



#endif // AnimationThread__DEFINED
