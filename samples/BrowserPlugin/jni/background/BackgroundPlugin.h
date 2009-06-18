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

#include "PluginObject.h"

#ifndef backgroundPlugin__DEFINED
#define backgroundPlugin__DEFINED

struct ANPCanvas;
struct ANPEvent;
struct ANPPaint;

class BackgroundPlugin : public SubPlugin {
public:
    BackgroundPlugin(NPP inst);
    virtual ~BackgroundPlugin();
    virtual void draw(ANPCanvas*);
    virtual int16 handleEvent(const ANPEvent* evt);

    // Timer Testing Variables
    uint32_t mStartTime;
    uint32_t mPrevTime;
    int      mTimerRepeatCount;
    int      mTimerLatencyCount;
    int      mTimerLatencyCurrentCount;

    // Bitmap Transparency Variables
    bool mFinishedStageOne;   // check default & set transparent
    bool mFinishedStageTwo;   // check transparent & set opaque
    bool mFinishedStageThree; // check opaque

private:

    ANPPaint*   m_paint;

    void test_logging();
    void test_timers();
    void test_bitmaps();
    void test_bitmap_transparency(const ANPEvent* evt);

};

#endif // backgroundPlugin__DEFINED
