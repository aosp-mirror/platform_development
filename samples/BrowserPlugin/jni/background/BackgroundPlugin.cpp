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

#include "BackgroundPlugin.h"
#include "android_npapi.h"

#include <stdio.h>
#include <sys/time.h>
#include <time.h>
#include <math.h>
#include <string.h>

extern NPNetscapeFuncs*        browser;
extern ANPBitmapInterfaceV0    gBitmapI;
extern ANPLogInterfaceV0       gLogI;
extern ANPCanvasInterfaceV0    gCanvasI;
extern ANPPaintInterfaceV0     gPaintI;
extern ANPPathInterfaceV0      gPathI;
extern ANPTypefaceInterfaceV0  gTypefaceI;

extern uint32_t getMSecs();

#define ARRAY_COUNT(array)      (sizeof(array) / sizeof(array[0]))

//#define LOG_ERROR(inst, string, params...) gLogI.log(inst, kError_ANPLogType, (log_prefix + string), inst, params)

///////////////////////////////////////////////////////////////////////////////

BackgroundPlugin::BackgroundPlugin(NPP inst) : SubPlugin(inst) {

    m_paint = gPaintI.newPaint();
    gPaintI.setFlags(m_paint, gPaintI.getFlags(m_paint) | kAntiAlias_ANPPaintFlag);
    gPaintI.setColor(m_paint, 0xFFFF0000);
    gPaintI.setTextSize(m_paint, 16);

    ANPTypeface* tf = gTypefaceI.createFromName("serif", kItalic_ANPTypefaceStyle);
    gPaintI.setTypeface(m_paint, tf);
    gTypefaceI.unref(tf);

    //initialize variables
    mFinishedStageOne   = false;
    mFinishedStageTwo   = false;
    mFinishedStageThree = false;

    // test basic plugin functionality
    test_logging(); // android logging
    test_timers();  // plugin timers
    test_bitmaps(); // android bitmaps
    test_domAccess();
    test_javascript();
}

BackgroundPlugin::~BackgroundPlugin() {
}

bool BackgroundPlugin::supportsDrawingModel(ANPDrawingModel model) {
    return (model == kBitmap_ANPDrawingModel);
}

void BackgroundPlugin::drawPlugin(const ANPBitmap& bitmap, const ANPRectI& clip) {
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

void BackgroundPlugin::draw(ANPCanvas* canvas) {

    gCanvasI.drawColor(canvas, 0xFFFFFFFF);

    ANPFontMetrics fm;
    gPaintI.getFontMetrics(m_paint, &fm);

    gPaintI.setColor(m_paint, 0xFF0000FF);
    const char c[] = "This is a background plugin.";
    gCanvasI.drawText(canvas, c, sizeof(c)-1, 10, -fm.fTop, m_paint);
}

int16 BackgroundPlugin::handleEvent(const ANPEvent* evt) {
    NPP instance = this->inst();

    switch (evt->eventType) {
        case kDraw_ANPEventType:
            switch (evt->data.draw.model) {
                case kBitmap_ANPDrawingModel:
                    test_bitmap_transparency(evt);
                    drawPlugin(evt->data.draw.data.bitmap, evt->data.draw.clip);
                    return 1;
                default:
                    break;   // unknown drawing model
            }
        case kTouch_ANPEventType:
            gLogI.log(instance, kError_ANPLogType, " ------ %p the plugin did not request touch events", instance);
        case kKey_ANPEventType:
            gLogI.log(instance, kError_ANPLogType, " ------ %p the plugin did not request key events", instance);
        default:
            break;
    }
    return 0;   // unknown or unhandled event
}

///////////////////////////////////////////////////////////////////////////////
// LOGGING TESTS
///////////////////////////////////////////////////////////////////////////////


void BackgroundPlugin::test_logging() {
    NPP instance = this->inst();

    //LOG_ERROR(instance, " ------ %p Testing Log Error", instance);
    gLogI.log(instance, kError_ANPLogType, " ------ %p Testing Log Error", instance);
    gLogI.log(instance, kWarning_ANPLogType, " ------ %p Testing Log Warning", instance);
    gLogI.log(instance, kDebug_ANPLogType, " ------ %p Testing Log Debug", instance);
}

///////////////////////////////////////////////////////////////////////////////
// TIMER TESTS
///////////////////////////////////////////////////////////////////////////////

#define TIMER_INTERVAL     50
static void timer_oneshot(NPP instance, uint32 timerID);
static void timer_repeat(NPP instance, uint32 timerID);
static void timer_neverfires(NPP instance, uint32 timerID);
static void timer_latency(NPP instance, uint32 timerID);

void BackgroundPlugin::test_timers() {
    NPP instance = this->inst();

    //Setup the testing counters
    mTimerRepeatCount = 5;
    mTimerLatencyCount = 5;

    // test for bogus timerID
    browser->unscheduletimer(instance, 999999);
    // test one-shot
    browser->scheduletimer(instance, 100, false, timer_oneshot);
    // test repeat
    browser->scheduletimer(instance, 50, true, timer_repeat);
    // test timer latency
    browser->scheduletimer(instance, TIMER_INTERVAL, true, timer_latency);
    mStartTime = mPrevTime = getMSecs();
    // test unschedule immediately
    uint32 id = browser->scheduletimer(instance, 100, false, timer_neverfires);
    browser->unscheduletimer(instance, id);
    // test double unschedule (should be no-op)
    browser->unscheduletimer(instance, id);

}

static void timer_oneshot(NPP instance, uint32 timerID) {
    gLogI.log(instance, kDebug_ANPLogType, "-------- oneshot timer\n");
}

static void timer_repeat(NPP instance, uint32 timerID) {
    BackgroundPlugin *obj = ((BackgroundPlugin*) ((PluginObject*) instance->pdata)->activePlugin);

    gLogI.log(instance, kDebug_ANPLogType, "-------- repeat timer %d\n",
              obj->mTimerRepeatCount);
    if (--obj->mTimerRepeatCount == 0) {
        browser->unscheduletimer(instance, timerID);
    }
}

static void timer_neverfires(NPP instance, uint32 timerID) {
    gLogI.log(instance, kError_ANPLogType, "-------- timer_neverfires!!!\n");
}

static void timer_latency(NPP instance, uint32 timerID) {
    BackgroundPlugin *obj = ((BackgroundPlugin*) ((PluginObject*) instance->pdata)->activePlugin);

    obj->mTimerLatencyCurrentCount += 1;

    uint32_t now = getMSecs();
    uint32_t interval = now - obj->mPrevTime;
    uint32_t dur = now - obj->mStartTime;
    uint32_t expectedDur = obj->mTimerLatencyCurrentCount * TIMER_INTERVAL;
    int32_t drift = dur - expectedDur;
    int32_t avgDrift = drift / obj->mTimerLatencyCurrentCount;

    obj->mPrevTime = now;

    gLogI.log(instance, kDebug_ANPLogType,
              "-------- latency test: [%3d] interval %d expected %d, total %d expected %d, drift %d avg %d\n",
              obj->mTimerLatencyCurrentCount, interval, TIMER_INTERVAL, dur,
              expectedDur, drift, avgDrift);

    if (--obj->mTimerLatencyCount == 0) {
        browser->unscheduletimer(instance, timerID);
    }
}

///////////////////////////////////////////////////////////////////////////////
// BITMAP TESTS
///////////////////////////////////////////////////////////////////////////////

static void test_formats(NPP instance);

void BackgroundPlugin::test_bitmaps() {
    test_formats(this->inst());
}

static void test_formats(NPP instance) {

    // TODO pull names from enum in npapi instead of hardcoding them
    static const struct {
        ANPBitmapFormat fFormat;
        const char*     fName;
    } gRecs[] = {
        { kUnknown_ANPBitmapFormat,   "unknown" },
        { kRGBA_8888_ANPBitmapFormat, "8888" },
        { kRGB_565_ANPBitmapFormat,   "565" },
    };

    ANPPixelPacking packing;
    for (size_t i = 0; i < ARRAY_COUNT(gRecs); i++) {
        if (gBitmapI.getPixelPacking(gRecs[i].fFormat, &packing)) {
            gLogI.log(instance, kDebug_ANPLogType,
                      "pixel format [%d] %s has packing ARGB [%d %d] [%d %d] [%d %d] [%d %d]\n",
                      gRecs[i].fFormat, gRecs[i].fName,
                      packing.AShift, packing.ABits,
                      packing.RShift, packing.RBits,
                      packing.GShift, packing.GBits,
                      packing.BShift, packing.BBits);
        } else {
            gLogI.log(instance, kDebug_ANPLogType,
                      "pixel format [%d] %s has no packing\n",
                      gRecs[i].fFormat, gRecs[i].fName);
        }
    }
}

void BackgroundPlugin::test_bitmap_transparency(const ANPEvent* evt) {
    NPP instance = this->inst();

    // check default & set transparent
    if (!mFinishedStageOne) {

        gLogI.log(instance, kDebug_ANPLogType, "BEGIN: testing bitmap transparency");

        //check to make sure it is not transparent
        if (evt->data.draw.data.bitmap.format == kRGBA_8888_ANPBitmapFormat) {
            gLogI.log(instance, kError_ANPLogType, "bitmap default format is transparent");
        }

        //make it transparent (any non-null value will set it to true)
        bool value = true;
        NPError err = browser->setvalue(instance, NPPVpluginTransparentBool, &value);
        if (err != NPERR_NO_ERROR) {
            gLogI.log(instance, kError_ANPLogType, "Error setting transparency.");
        }

        mFinishedStageOne = true;
        browser->invalidaterect(instance, NULL);
    }
    // check transparent & set opaque
    else if (!mFinishedStageTwo) {

        //check to make sure it is transparent
        if (evt->data.draw.data.bitmap.format != kRGBA_8888_ANPBitmapFormat) {
            gLogI.log(instance, kError_ANPLogType, "bitmap did not change to transparent format");
        }

        //make it opaque
        NPError err = browser->setvalue(instance, NPPVpluginTransparentBool, NULL);
        if (err != NPERR_NO_ERROR) {
            gLogI.log(instance, kError_ANPLogType, "Error setting transparency.");
        }

        mFinishedStageTwo = true;
    }
    // check opaque
    else if (!mFinishedStageThree) {

        //check to make sure it is not transparent
        if (evt->data.draw.data.bitmap.format == kRGBA_8888_ANPBitmapFormat) {
            gLogI.log(instance, kError_ANPLogType, "bitmap default format is transparent");
        }

        gLogI.log(instance, kDebug_ANPLogType, "END: testing bitmap transparency");

        mFinishedStageThree = true;
    }
}

///////////////////////////////////////////////////////////////////////////////
// DOM TESTS
///////////////////////////////////////////////////////////////////////////////

void BackgroundPlugin::test_domAccess() {
    NPP instance = this->inst();

    gLogI.log(instance, kDebug_ANPLogType, " ------ %p Testing DOM Access", instance);

    // Get the plugin's DOM object
    NPObject* windowObject = NULL;
    browser->getvalue(instance, NPNVWindowNPObject, &windowObject);

    if (!windowObject)
        gLogI.log(instance, kError_ANPLogType, " ------ %p Unable to retrieve DOM Window", instance);

    // Retrieve a property from the plugin's DOM object
    NPIdentifier topIdentifier = browser->getstringidentifier("top");
    NPVariant topObjectVariant;
    browser->getproperty(instance, windowObject, topIdentifier, &topObjectVariant);

    if (topObjectVariant.type != NPVariantType_Object)
        gLogI.log(instance, kError_ANPLogType, " ------ %p Invalid Variant type for DOM Property: %d,%d", instance, topObjectVariant.type, NPVariantType_Object);
}


///////////////////////////////////////////////////////////////////////////////
// JAVASCRIPT TESTS
///////////////////////////////////////////////////////////////////////////////


void BackgroundPlugin::test_javascript() {
    NPP instance = this->inst();

    gLogI.log(instance, kDebug_ANPLogType, " ------ %p Testing JavaScript Access", instance);

    // Get the plugin's DOM object
    NPObject* windowObject = NULL;
    browser->getvalue(instance, NPNVWindowNPObject, &windowObject);

    if (!windowObject)
        gLogI.log(instance, kError_ANPLogType, " ------ %p Unable to retrieve DOM Window", instance);

    // create a string (JS code) that is stored in memory allocated by the browser
    const char* jsString = "1200 + 34";
    void* stringMem = browser->memalloc(strlen(jsString));
    memcpy(stringMem, jsString, strlen(jsString));

    // execute the javascript in the plugin's DOM object
    NPString script = { (char*)stringMem, strlen(jsString) };
    NPVariant scriptVariant;
    if (!browser->evaluate(instance, windowObject, &script, &scriptVariant))
        gLogI.log(instance, kError_ANPLogType, " ------ %p Unable to eval the JS.", instance);

    if (scriptVariant.type == NPVariantType_Int32) {
        if (scriptVariant.value.intValue != 1234)
            gLogI.log(instance, kError_ANPLogType, " ------ %p Invalid Value for JS Return: %d,1234", instance, scriptVariant.value.intValue);
    } else {
        gLogI.log(instance, kError_ANPLogType, " ------ %p Invalid Variant type for JS Return: %d,%d", instance, scriptVariant.type, NPVariantType_Int32);
    }

    // free the memory allocated within the browser
    browser->memfree(stringMem);
}
