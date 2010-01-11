/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <JNIHelp.h>
#include <utils/Log.h>

#include "PluginObject.h"

#define EXPORT __attribute__((visibility("default")))

extern ANPEventInterfaceV0         gEventI;

static void surfaceCreated(JNIEnv* env, jobject thiz, jint npp, jobject surface) {

    // send custom event
    ANPEvent event;
    event.inSize = sizeof(ANPEvent);
    event.eventType = kCustom_ANPEventType;
    event.data.other[0] = kSurfaceCreated_CustomEvent;

    gEventI.postEvent((NPP)npp, &event);
}

static void surfaceChanged(JNIEnv* env, jobject thiz, jint npp, jint format, jint width, jint height) {
    // send custom event
    ANPEvent event;
    event.inSize = sizeof(ANPEvent);
    event.eventType = kCustom_ANPEventType;
    event.data.other[0] = kSurfaceChanged_CustomEvent;
    event.data.other[1] = width;
    event.data.other[2] = height;

    gEventI.postEvent((NPP)npp, &event);
}

static void surfaceDestroyed(JNIEnv* env, jobject thiz, jint npp) {
    // send custom event
    ANPEvent event;
    event.inSize = sizeof(ANPEvent);
    event.eventType = kCustom_ANPEventType;
    event.data.other[0] = kSurfaceDestroyed_CustomEvent;

    gEventI.postEvent((NPP)npp, &event);
}

/*
 * JNI registration.
 */
static JNINativeMethod gPaintSurfaceMethods[] = {
    { "nativeSurfaceCreated", "(I)V", (void*) surfaceCreated },
    { "nativeSurfaceChanged", "(IIII)V", (void*) surfaceChanged },
    { "nativeSurfaceDestroyed", "(I)V", (void*) surfaceDestroyed },
};

EXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {

    JNIEnv* env = NULL;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    jniRegisterNativeMethods(env, "com/android/sampleplugin/PaintSurface",
                             gPaintSurfaceMethods, NELEM(gPaintSurfaceMethods));

    return JNI_VERSION_1_4;
}
