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
#include "AnimationPlugin.h"
#include "AudioPlugin.h"
#include "BackgroundPlugin.h"
#include "FormPlugin.h"
#include "NavigationPlugin.h"
#include "PaintPlugin.h"
#include "VideoPlugin.h"

NPNetscapeFuncs* browser;
JavaVM* gVM;

#define EXPORT __attribute__((visibility("default")))

NPError NPP_New(NPMIMEType pluginType, NPP instance, uint16_t mode, int16_t argc,
        char* argn[], char* argv[], NPSavedData* saved);
NPError NPP_Destroy(NPP instance, NPSavedData** save);
NPError NPP_SetWindow(NPP instance, NPWindow* window);
NPError NPP_NewStream(NPP instance, NPMIMEType type, NPStream* stream,
        NPBool seekable, uint16_t* stype);
NPError NPP_DestroyStream(NPP instance, NPStream* stream, NPReason reason);
int32_t   NPP_WriteReady(NPP instance, NPStream* stream);
int32_t   NPP_Write(NPP instance, NPStream* stream, int32_t offset, int32_t len,
        void* buffer);
void    NPP_StreamAsFile(NPP instance, NPStream* stream, const char* fname);
void    NPP_Print(NPP instance, NPPrint* platformPrint);
int16_t   NPP_HandleEvent(NPP instance, void* event);
void    NPP_URLNotify(NPP instance, const char* URL, NPReason reason,
        void* notifyData);
NPError NPP_GetValue(NPP instance, NPPVariable variable, void *value);
NPError NPP_SetValue(NPP instance, NPNVariable variable, void *value);

extern "C" {
EXPORT NPError NP_Initialize(NPNetscapeFuncs* browserFuncs, NPPluginFuncs* pluginFuncs, void *java_env);
EXPORT NPError NP_GetValue(NPP instance, NPPVariable variable, void *value);
EXPORT const char* NP_GetMIMEDescription(void);
EXPORT void NP_Shutdown(void);
};

ANPAudioTrackInterfaceV0    gSoundI;
ANPBitmapInterfaceV0        gBitmapI;
ANPCanvasInterfaceV0        gCanvasI;
ANPEventInterfaceV0         gEventI;
ANPLogInterfaceV0           gLogI;
ANPPaintInterfaceV0         gPaintI;
ANPPathInterfaceV0          gPathI;
ANPSurfaceInterfaceV0       gSurfaceI;
ANPSystemInterfaceV0        gSystemI;
ANPTypefaceInterfaceV0      gTypefaceI;
ANPWindowInterfaceV1        gWindowI;
ANPNativeWindowInterfaceV0  gNativeWindowI;

#define ARRAY_COUNT(array)      (sizeof(array) / sizeof(array[0]))
#define DEBUG_PLUGIN_EVENTS     0

NPError NP_Initialize(NPNetscapeFuncs* browserFuncs, NPPluginFuncs* pluginFuncs, void *java_env)
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
        { kAudioTrackInterfaceV0_ANPGetValue,   sizeof(gSoundI),        &gSoundI },
        { kBitmapInterfaceV0_ANPGetValue,       sizeof(gBitmapI),       &gBitmapI },
        { kCanvasInterfaceV0_ANPGetValue,       sizeof(gCanvasI),       &gCanvasI },
        { kEventInterfaceV0_ANPGetValue,        sizeof(gEventI),        &gEventI },
        { kLogInterfaceV0_ANPGetValue,          sizeof(gLogI),          &gLogI },
        { kPaintInterfaceV0_ANPGetValue,        sizeof(gPaintI),        &gPaintI },
        { kPathInterfaceV0_ANPGetValue,         sizeof(gPathI),         &gPathI },
        { kSurfaceInterfaceV0_ANPGetValue,      sizeof(gSurfaceI),      &gSurfaceI },
        { kSystemInterfaceV0_ANPGetValue,       sizeof(gSystemI),       &gSystemI },
        { kTypefaceInterfaceV0_ANPGetValue,     sizeof(gTypefaceI),     &gTypefaceI },
        { kWindowInterfaceV1_ANPGetValue,       sizeof(gWindowI),       &gWindowI },
        { kNativeWindowInterfaceV0_ANPGetValue, sizeof(gNativeWindowI), &gNativeWindowI },
    };
    for (size_t i = 0; i < ARRAY_COUNT(gPairs); i++) {
        gPairs[i].i->inSize = gPairs[i].size;
        NPError err = browser->getvalue(NULL, gPairs[i].v, gPairs[i].i);
        if (err) {
            return err;
        }
    }

    // store the JavaVM for the plugin
    JNIEnv* env = (JNIEnv*)java_env;
    env->GetJavaVM(&gVM);

    return NPERR_NO_ERROR;
}

void NP_Shutdown(void)
{

}

const char *NP_GetMIMEDescription(void)
{
    return "application/x-testbrowserplugin:tst:Test plugin mimetype is application/x-testbrowserplugin";
}

NPError NPP_New(NPMIMEType pluginType, NPP instance, uint16_t mode, int16_t argc,
                char* argn[], char* argv[], NPSavedData* saved)
{

    /* BEGIN: STANDARD PLUGIN FRAMEWORK */
    PluginObject *obj = NULL;

    // Scripting functions appeared in NPAPI version 14
    if (browser->version >= 14) {
        instance->pdata = browser->createobject (instance, getPluginClass());
        obj = static_cast<PluginObject*>(instance->pdata);
        obj->pluginType = 0;
    }
    /* END: STANDARD PLUGIN FRAMEWORK */

    // select the drawing model based on user input
    ANPDrawingModel model = kBitmap_ANPDrawingModel;

    for (int i = 0; i < argc; i++) {
        if (!strcmp(argn[i], "DrawingModel")) {
            if (!strcmp(argv[i], "Bitmap")) {
                model = kBitmap_ANPDrawingModel;
            }
            else if (!strcmp(argv[i], "Surface")) {
               model = kSurface_ANPDrawingModel;
            }
            else if (!strcmp(argv[i], "OpenGL")) {
               model = kOpenGL_ANPDrawingModel;
            }
            gLogI.log(kDebug_ANPLogType, "------ %p DrawingModel is %d", instance, model);
            break;
        }
    }

    // notify the plugin API of the drawing model we wish to use. This must be
    // done prior to creating certain subPlugin objects (e.g. surfaceViews)
    NPError err = browser->setvalue(instance, kRequestDrawingModel_ANPSetValue,
                            reinterpret_cast<void*>(model));
    if (err) {
        gLogI.log(kError_ANPLogType, "request model %d err %d", model, err);
        return err;
    }

    const char* path = gSystemI.getApplicationDataDirectory();
    if (path) {
        gLogI.log(kDebug_ANPLogType, "Application data dir is %s", path);
    } else {
        gLogI.log(kError_ANPLogType, "Can't find Application data dir");
    }

    // select the pluginType
    for (int i = 0; i < argc; i++) {
        if (!strcmp(argn[i], "PluginType")) {
            if (!strcmp(argv[i], "Animation")) {
                obj->pluginType = kAnimation_PluginType;
                obj->activePlugin = new BallAnimation(instance);
            }
            else if (!strcmp(argv[i], "Audio")) {
                obj->pluginType = kAudio_PluginType;
                obj->activePlugin = new AudioPlugin(instance);
            }
            else if (!strcmp(argv[i], "Background")) {
                obj->pluginType = kBackground_PluginType;
                obj->activePlugin = new BackgroundPlugin(instance);
            }
            else if (!strcmp(argv[i], "Form")) {
                obj->pluginType = kForm_PluginType;
                obj->activePlugin = new FormPlugin(instance);
            }
            else if (!strcmp(argv[i], "Navigation")) {
                obj->pluginType = kNavigation_PluginType;
                obj->activePlugin = new NavigationPlugin(instance);
            }
            else if (!strcmp(argv[i], "Paint")) {
                obj->pluginType = kPaint_PluginType;
                obj->activePlugin = new PaintPlugin(instance);
            }
            else if (!strcmp(argv[i], "Video")) {
                obj->pluginType = kVideo_PluginType;
                obj->activePlugin = new VideoPlugin(instance);
            }
            else {
                gLogI.log(kError_ANPLogType, "PluginType %s unknown!", argv[i]);
            }
            break;
        }
    }

    // if no pluginType is specified then default to Animation
    if (!obj->pluginType) {
        gLogI.log(kError_ANPLogType, "------ %p No PluginType attribute was found", instance);
        obj->pluginType = kAnimation_PluginType;
        obj->activePlugin = new BallAnimation(instance);
    }

    gLogI.log(kDebug_ANPLogType, "------ %p PluginType is %d", instance, obj->pluginType);

    // check to ensure the pluginType supports the model
    if (!obj->activePlugin->supportsDrawingModel(model)) {
        gLogI.log(kError_ANPLogType, "------ %p Unsupported DrawingModel (%d)", instance, model);
        return NPERR_GENERIC_ERROR;
    }

    // if the plugin uses the surface drawing model then set the java context
    if (model == kSurface_ANPDrawingModel || model == kOpenGL_ANPDrawingModel) {
        SurfaceSubPlugin* surfacePlugin = static_cast<SurfaceSubPlugin*>(obj->activePlugin);

        jobject context;
        NPError err = browser->getvalue(instance, kJavaContext_ANPGetValue,
                                        static_cast<void*>(&context));
        if (err) {
            gLogI.log(kError_ANPLogType, "request context err: %d", err);
            return err;
        }

        surfacePlugin->setContext(context);
    }


    return NPERR_NO_ERROR;
}

NPError NPP_Destroy(NPP instance, NPSavedData** save)
{
    PluginObject *obj = (PluginObject*) instance->pdata;
    if (obj) {
        delete obj->activePlugin;
        browser->releaseobject(&obj->header);
    }

    return NPERR_NO_ERROR;
}

NPError NPP_SetWindow(NPP instance, NPWindow* window)
{
    PluginObject *obj = (PluginObject*) instance->pdata;

    // Do nothing if browser didn't support NPN_CreateObject which would have created the PluginObject.
    if (obj != NULL) {
        obj->window = window;
    }

    browser->invalidaterect(instance, NULL);

    return NPERR_NO_ERROR;
}

NPError NPP_NewStream(NPP instance, NPMIMEType type, NPStream* stream, NPBool seekable, uint16_t* stype)
{
    *stype = NP_ASFILEONLY;
    return NPERR_NO_ERROR;
}

NPError NPP_DestroyStream(NPP instance, NPStream* stream, NPReason reason)
{
    return NPERR_NO_ERROR;
}

int32_t NPP_WriteReady(NPP instance, NPStream* stream)
{
    return 0;
}

int32_t NPP_Write(NPP instance, NPStream* stream, int32_t offset, int32_t len, void* buffer)
{
    return 0;
}

void NPP_StreamAsFile(NPP instance, NPStream* stream, const char* fname)
{
}

void NPP_Print(NPP instance, NPPrint* platformPrint)
{
}

int16_t NPP_HandleEvent(NPP instance, void* event)
{
    PluginObject *obj = reinterpret_cast<PluginObject*>(instance->pdata);
    const ANPEvent* evt = reinterpret_cast<const ANPEvent*>(event);

#if DEBUG_PLUGIN_EVENTS
    switch (evt->eventType) {
        case kDraw_ANPEventType:

            if (evt->data.draw.model == kBitmap_ANPDrawingModel) {

                static ANPBitmapFormat currentFormat = -1;
                if (evt->data.draw.data.bitmap.format != currentFormat) {
                    currentFormat = evt->data.draw.data.bitmap.format;
                    gLogI.log(kDebug_ANPLogType, "---- %p Draw (bitmap)"
                              " clip=%d,%d,%d,%d format=%d", instance,
                              evt->data.draw.clip.left,
                              evt->data.draw.clip.top,
                              evt->data.draw.clip.right,
                              evt->data.draw.clip.bottom,
                              evt->data.draw.data.bitmap.format);
                }
            }
            break;

        case kKey_ANPEventType:
            gLogI.log(kDebug_ANPLogType, "---- %p Key action=%d"
                      " code=%d vcode=%d unichar=%d repeat=%d mods=%x", instance,
                      evt->data.key.action,
                      evt->data.key.nativeCode,
                      evt->data.key.virtualCode,
                      evt->data.key.unichar,
                      evt->data.key.repeatCount,
                      evt->data.key.modifiers);
            break;

        case kLifecycle_ANPEventType:
            gLogI.log(kDebug_ANPLogType, "---- %p Lifecycle action=%d",
                                instance, evt->data.lifecycle.action);
            break;

       case kTouch_ANPEventType:
            gLogI.log(kDebug_ANPLogType, "---- %p Touch action=%d [%d %d]",
                      instance, evt->data.touch.action, evt->data.touch.x,
                      evt->data.touch.y);
            break;

       case kMouse_ANPEventType:
            gLogI.log(kDebug_ANPLogType, "---- %p Mouse action=%d [%d %d]",
                      instance, evt->data.mouse.action, evt->data.mouse.x,
                      evt->data.mouse.y);
            break;

       case kVisibleRect_ANPEventType:
            gLogI.log(kDebug_ANPLogType, "---- %p VisibleRect [%d %d %d %d]",
                      instance, evt->data.visibleRect.rect.left, evt->data.visibleRect.rect.top,
                      evt->data.visibleRect.rect.right, evt->data.visibleRect.rect.bottom);
            break;

        default:
            gLogI.log(kError_ANPLogType, "---- %p Unknown Event [%d]",
                      instance, evt->eventType);
            break;
    }
#endif

    if(!obj->activePlugin) {
        gLogI.log(kError_ANPLogType, "the active plugin is null.");
        return 0; // unknown or unhandled event
    }
    else {
        return obj->activePlugin->handleEvent(evt);
    }
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

NPError NPP_GetValue(NPP instance, NPPVariable variable, void* value)
{
    if (variable == NPPVpluginScriptableNPObject) {
        void **v = (void **)value;
        PluginObject *obj = (PluginObject*) instance->pdata;

        if (obj)
            browser->retainobject(&obj->header);

        *v = &(obj->header);
        return NPERR_NO_ERROR;
    }

    if (variable == kJavaSurface_ANPGetValue) {
        //get the surface sub-plugin
        PluginObject* obj = static_cast<PluginObject*>(instance->pdata);
        if (obj && obj->activePlugin) {

            if(obj->activePlugin->supportsDrawingModel(kSurface_ANPDrawingModel)
                    || obj->activePlugin->supportsDrawingModel(kOpenGL_ANPDrawingModel)) {
                SurfaceSubPlugin* plugin = static_cast<SurfaceSubPlugin*>(obj->activePlugin);
                jobject* surface = static_cast<jobject*>(value);
                *surface = plugin->getSurface();
                return NPERR_NO_ERROR;
            } else {
                gLogI.log(kError_ANPLogType,
                          "-- %p Tried to retrieve surface for non-surface plugin",
                          instance);
            }
        }
    }

    return NPERR_GENERIC_ERROR;
}

NPError NPP_SetValue(NPP instance, NPNVariable variable, void *value)
{
    return NPERR_GENERIC_ERROR;
}

