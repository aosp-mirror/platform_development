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

#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "main.h"
#include "PluginObject.h"
#include "pluginGraphics.h"
#include "android_npapi.h"

NPNetscapeFuncs* browser;
#define EXPORT __attribute__((visibility("default")))

NPError NPP_New(NPMIMEType pluginType, NPP instance, uint16 mode, int16 argc, 
        char* argn[], char* argv[], NPSavedData* saved);
NPError NPP_Destroy(NPP instance, NPSavedData** save);
NPError NPP_SetWindow(NPP instance, NPWindow* window);
NPError NPP_NewStream(NPP instance, NPMIMEType type, NPStream* stream, 
        NPBool seekable, uint16* stype);
NPError NPP_DestroyStream(NPP instance, NPStream* stream, NPReason reason);
int32   NPP_WriteReady(NPP instance, NPStream* stream);
int32   NPP_Write(NPP instance, NPStream* stream, int32 offset, int32 len, 
        void* buffer);
void    NPP_StreamAsFile(NPP instance, NPStream* stream, const char* fname);
void    NPP_Print(NPP instance, NPPrint* platformPrint);
int16   NPP_HandleEvent(NPP instance, void* event);
void    NPP_URLNotify(NPP instance, const char* URL, NPReason reason, 
        void* notifyData);
NPError NPP_GetValue(NPP instance, NPPVariable variable, void *value);
NPError NPP_SetValue(NPP instance, NPNVariable variable, void *value);

extern "C" {
EXPORT NPError NP_Initialize(NPNetscapeFuncs* browserFuncs, NPPluginFuncs* pluginFuncs, void *java_env, void *application_context);
EXPORT NPError NP_GetValue(NPP instance, NPPVariable variable, void *value);
EXPORT const char* NP_GetMIMEDescription(void);
EXPORT void NP_Shutdown(void); 
};

ANPAudioTrackInterfaceV0    gSoundI;
ANPCanvasInterfaceV0        gCanvasI;
ANPLogInterfaceV0           gLogI;
ANPPaintInterfaceV0         gPaintI;
ANPPathInterfaceV0          gPathI;
ANPTypefaceInterfaceV0      gTypefaceI;

#define ARRAY_COUNT(array)      (sizeof(array) / sizeof(array[0]))

NPError NP_Initialize(NPNetscapeFuncs* browserFuncs, NPPluginFuncs* pluginFuncs, void *java_env, void *application_context)
{
    // Make sure we have a function table equal or larger than we are built against.
    if (browserFuncs->size < sizeof(NPNetscapeFuncs)) {
        return NPERR_GENERIC_ERROR;
    }
    
    // Copy the function table (structure)
    browser = (NPNetscapeFuncs*) malloc(sizeof(NPNetscapeFuncs));
    memcpy(browser, browserFuncs, sizeof(NPNetscapeFuncs));
    
    // Build the plugin function table
    pluginFuncs->version = 11;
    pluginFuncs->size = sizeof(pluginFuncs);
    pluginFuncs->newp = NPP_New;
    pluginFuncs->destroy = NPP_Destroy;
    pluginFuncs->setwindow = NPP_SetWindow;
    pluginFuncs->newstream = NPP_NewStream;
    pluginFuncs->destroystream = NPP_DestroyStream;
    pluginFuncs->asfile = NPP_StreamAsFile;
    pluginFuncs->writeready = NPP_WriteReady;
    pluginFuncs->write = (NPP_WriteProcPtr)NPP_Write;
    pluginFuncs->print = NPP_Print;
    pluginFuncs->event = NPP_HandleEvent;
    pluginFuncs->urlnotify = NPP_URLNotify;
    pluginFuncs->getvalue = NPP_GetValue;
    pluginFuncs->setvalue = NPP_SetValue;

    static const struct {
        NPNVariable     v;
        uint32_t        size;
        ANPInterface*   i;
    } gPairs[] = {
        { kLogInterfaceV0_ANPGetValue,          sizeof(gLogI),      &gLogI },
        { kCanvasInterfaceV0_ANPGetValue,       sizeof(gCanvasI),   &gCanvasI },
        { kPaintInterfaceV0_ANPGetValue,        sizeof(gPaintI),    &gPaintI },
        { kPathInterfaceV0_ANPGetValue,         sizeof(gPathI),     &gPathI },
        { kTypefaceInterfaceV0_ANPGetValue,     sizeof(gPaintI),    &gTypefaceI },
        { kAudioTrackInterfaceV0_ANPGetValue,   sizeof(gSoundI),    &gSoundI },
    };
    for (size_t i = 0; i < ARRAY_COUNT(gPairs); i++) {
        gPairs[i].i->inSize = gPairs[i].size;
        NPError err = browser->getvalue(NULL, gPairs[i].v, gPairs[i].i);
        if (err) {
            return err;
        }
    }
    
    return NPERR_NO_ERROR;
}

void NP_Shutdown(void)
{

}

const char *NP_GetMIMEDescription(void) 
{
    return "application/x-testplugin:tst:Test plugin mimetype is application/x-testplugin";
}

NPError NPP_New(NPMIMEType pluginType, NPP instance, uint16 mode, int16 argc,
                char* argn[], char* argv[], NPSavedData* saved)
{
    PluginObject *obj = NULL;

    // Scripting functions appeared in NPAPI version 14
    if (browser->version >= 14) {
        instance->pdata = browser->createobject (instance, getPluginClass());
        obj = static_cast<PluginObject*>(instance->pdata);
        bzero(obj, sizeof(*obj));
    }
    
    uint32_t bits;
    NPError err = browser->getvalue(instance, kSupportedDrawingModel_ANPGetValue, &bits);
    if (err) {
        gLogI.log(instance, kError_ANPLogType, "supported model err %d", err);
        return err;
    }
    
    ANPDrawingModel model = kBitmap_ANPDrawingModel;

    int count = argc;
    for (int i = 0; i < count; i++) {
        if (!strcmp(argn[i], "DrawingModel")) {
            if (!strcmp(argv[i], "Bitmap")) {
                model = kBitmap_ANPDrawingModel;
            }
            if (!strcmp(argv[i], "Canvas")) {
            //    obj->mTestTimers = true;
            }
            gLogI.log(instance, kDebug_ANPLogType, "------ %p DrawingModel is %d", instance, model);
            break;
        }
    }

    // comment this out to draw via bitmaps (the default)
    err = browser->setvalue(instance, kRequestDrawingModel_ANPSetValue,
                            reinterpret_cast<void*>(model));
    if (err) {
        gLogI.log(instance, kError_ANPLogType, "request model %d err %d", model, err);
    }
    return err;
}

NPError NPP_Destroy(NPP instance, NPSavedData** save)
{
    PluginObject *obj = (PluginObject*) instance->pdata;
    delete obj->anim;
    gSoundI.deleteTrack(obj->track);

    return NPERR_NO_ERROR;
}

static void timer_oneshot(NPP instance, uint32 timerID) {
    gLogI.log(instance, kDebug_ANPLogType, "-------- oneshot timer\n");
}

static int gTimerRepeatCount;
static void timer_repeat(NPP instance, uint32 timerID) {
    
    gLogI.log(instance, kDebug_ANPLogType, "-------- repeat timer %d\n",
              gTimerRepeatCount);
    if (--gTimerRepeatCount == 0) {
        browser->unscheduletimer(instance, timerID);
    }
}

static void timer_neverfires(NPP instance, uint32 timerID) {
    gLogI.log(instance, kError_ANPLogType, "-------- timer_neverfires!!!\n");
}

#define TIMER_INTERVAL     50

static void timer_latency(NPP instance, uint32 timerID) {
    PluginObject *obj = (PluginObject*) instance->pdata;

    obj->mTimerCount += 1;

    uint32_t now = getMSecs();
    uint32_t interval = now - obj->mPrevTime;

    uint32_t dur = now - obj->mStartTime;
    uint32_t expectedDur = obj->mTimerCount * TIMER_INTERVAL;
    int32_t drift = dur - expectedDur;
    int32_t aveDrift = drift / obj->mTimerCount;
    
    obj->mPrevTime = now;
    
    gLogI.log(instance, kDebug_ANPLogType,
              "-------- latency test: [%3d] interval %d expected %d, total %d expected %d, drift %d ave %d\n",
              obj->mTimerCount, interval, TIMER_INTERVAL, dur, expectedDur,
              drift, aveDrift);
}

NPError NPP_SetWindow(NPP instance, NPWindow* window)
{
    PluginObject *obj = (PluginObject*) instance->pdata;
    
    // Do nothing if browser didn't support NPN_CreateObject which would have created the PluginObject.
    if (obj != NULL) {
        obj->window = window;
    }
    
    static bool gTestTimers;
    if (!gTestTimers) {
        gTestTimers = true;
        // test for bogus timerID
        browser->unscheduletimer(instance, 999999);
        // test oneshot
        browser->scheduletimer(instance, 100, false, timer_oneshot);
        // test repeat
        gTimerRepeatCount = 10;
        browser->scheduletimer(instance, 50, true, timer_repeat);
        // test unschedule immediately
        uint32 id = browser->scheduletimer(instance, 100, false, timer_neverfires);
        browser->unscheduletimer(instance, id);
        // test double unschedlue (should be no-op)
        browser->unscheduletimer(instance, id);
    }
    
    if (obj->mTestTimers) {
        browser->scheduletimer(instance, TIMER_INTERVAL, true, timer_latency);
        obj->mStartTime = obj->mPrevTime = getMSecs();
        obj->mTestTimers = false;
    }
    
    browser->invalidaterect(instance, NULL);

    return NPERR_NO_ERROR;
}
 

NPError NPP_NewStream(NPP instance, NPMIMEType type, NPStream* stream, NPBool seekable, uint16* stype)
{
    *stype = NP_ASFILEONLY;
    return NPERR_NO_ERROR;
}

NPError NPP_DestroyStream(NPP instance, NPStream* stream, NPReason reason)
{
    return NPERR_NO_ERROR;
}

int32 NPP_WriteReady(NPP instance, NPStream* stream)
{
    return 0;
}

int32 NPP_Write(NPP instance, NPStream* stream, int32 offset, int32 len, void* buffer)
{
    return 0;
}

void NPP_StreamAsFile(NPP instance, NPStream* stream, const char* fname)
{
}

void NPP_Print(NPP instance, NPPrint* platformPrint)
{

}

struct SoundPlay {
    NPP             instance;
    ANPAudioTrack*  track;
    FILE*           file;
};

static void audioCallback(ANPAudioEvent evt, void* user, ANPAudioBuffer* buffer) {
    switch (evt) {
        case kMoreData_ANPAudioEvent: {
            SoundPlay* play = reinterpret_cast<SoundPlay*>(user);
            size_t amount = fread(buffer->bufferData, 1, buffer->size, play->file);
            buffer->size = amount;
            if (amount == 0) {
                gSoundI.stop(play->track);
                fclose(play->file);
                play->file = NULL;
                // need to notify our main thread to delete the track now
            }
            break;
        }
        default:
            break;
    }
}

static ANPAudioTrack* createTrack(NPP instance, const char path[]) {
    FILE* f = fopen(path, "r");
    gLogI.log(instance, kWarning_ANPLogType, "--- path %s FILE %p", path, f);
    if (NULL == f) {
        return NULL;
    }
    SoundPlay* play = new SoundPlay;
    play->file = f;
    play->track = gSoundI.newTrack(44100, kPCM16Bit_ANPSampleFormat, 2, audioCallback, play);
    if (NULL == play->track) {
        fclose(f);
        delete play;
        return NULL;
    }
    return play->track;
}

int16 NPP_HandleEvent(NPP instance, void* event)
{
    PluginObject *obj = reinterpret_cast<PluginObject*>(instance->pdata);
    const ANPEvent* evt = reinterpret_cast<const ANPEvent*>(event);

    switch (evt->eventType) {
        case kDraw_ANPEventType:
            switch (evt->data.drawContext.model) {
                case kBitmap_ANPDrawingModel:
                    drawPlugin(instance, evt->data.drawContext.data.bitmap,
                               evt->data.drawContext.clip);
                    return 1;
                default:
                    break;   // unknown drawing model
            }

        case kKey_ANPEventType:
            gLogI.log(instance, kDebug_ANPLogType, "---- %p Key action=%d"
                      " code=%d vcode=%d unichar=%d repeat=%d mods=%x", instance,
                      evt->data.key.action,
                      evt->data.key.nativeCode,
                      evt->data.key.virtualCode,
                      evt->data.key.unichar,
                      evt->data.key.repeatCount,
                      evt->data.key.modifiers);
            if (evt->data.key.action == kDown_ANPKeyAction) {
                obj->mUnichar = evt->data.key.unichar;
                browser->invalidaterect(instance, NULL);
            }
            return 1;

        case kTouch_ANPEventType:
            gLogI.log(instance, kDebug_ANPLogType, "---- %p Touch action=%d [%d %d]",
                      instance, evt->data.touch.action, evt->data.touch.x,
                      evt->data.touch.y);
            if (kUp_ANPTouchAction == evt->data.touch.action) {
                if (NULL == obj->track) {
                    obj->track = createTrack(instance, "/sdcard/sample.snd");
                }
                if (obj->track) {
                    gLogI.log(instance, kDebug_ANPLogType, "track %p %d",
                              obj->track, gSoundI.isStopped(obj->track));
                    if (gSoundI.isStopped(obj->track)) {
                        gSoundI.start(obj->track);
                    } else {
                        gSoundI.pause(obj->track);
                    }
                }
            }
            return 1;

        default:
            break;
    }
    return 0;   // unknown or unhandled event
}

void NPP_URLNotify(NPP instance, const char* url, NPReason reason, void* notifyData)
{

}

EXPORT NPError NP_GetValue(NPP instance, NPPVariable variable, void *value) {

    if (variable == NPPVpluginNameString) {
        const char **str = (const char **)value;
        *str = "Test Plugin";
        return NPERR_NO_ERROR;
    }
    
    if (variable == NPPVpluginDescriptionString) {
        const char **str = (const char **)value;
        *str = "Description of Test Plugin";
        return NPERR_NO_ERROR;
    }
    
    return NPERR_GENERIC_ERROR;
}

NPError NPP_GetValue(NPP instance, NPPVariable variable, void *value)
{
    if (variable == NPPVpluginScriptableNPObject) {
        void **v = (void **)value;
        PluginObject *obj = (PluginObject*) instance->pdata;
        
        if (obj)
            browser->retainobject((NPObject*)obj);
        
        *v = obj;
        return NPERR_NO_ERROR;
    }
    
    return NPERR_GENERIC_ERROR;
}

NPError NPP_SetValue(NPP instance, NPNVariable variable, void *value)
{
    return NPERR_GENERIC_ERROR;
}

