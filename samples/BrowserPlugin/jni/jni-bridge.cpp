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

static SurfaceSubPlugin* getPluginObject(int npp) {
    NPP instance = (NPP)npp;
    PluginObject* obj = static_cast<PluginObject*>(instance->pdata);
    if (obj && obj->activePlugin
            && obj->activePlugin->supportsDrawingModel(kSurface_ANPDrawingModel)) {
        return static_cast<SurfaceSubPlugin*>(obj->activePlugin);
    }
    return NULL;
}

static void surfaceCreated(JNIEnv* env, jobject thiz, jint npp, jobject surface) {
    SurfaceSubPlugin* obj = getPluginObject(npp);
    jobject globalSurface = env->NewGlobalRef(surface);
    obj->surfaceCreated(globalSurface);
}

static void surfaceChanged(JNIEnv* env, jobject thiz, jint npp, jint format, jint width, jint height) {
    SurfaceSubPlugin* obj = getPluginObject(npp);
    obj->surfaceChanged(format, width, height);
}

static void surfaceDestroyed(JNIEnv* env, jobject thiz, jint npp) {
    SurfaceSubPlugin* obj = getPluginObject(npp);
    if (obj) {
        obj->surfaceDestroyed();
    }
}

static jint getSurfaceWidth(JNIEnv* env, jobject thiz, jint npp) {
    SurfaceSubPlugin* obj = getPluginObject(npp);
    return obj->getPluginWidth();
}

static jint getSurfaceHeight(JNIEnv* env, jobject thiz, jint npp) {
    SurfaceSubPlugin* obj = getPluginObject(npp);
    return obj->getPluginHeight();
}

static jboolean isFixedSurface(JNIEnv* env, jobject thiz, jint npp) {
    SurfaceSubPlugin* obj = getPluginObject(npp);
    return obj->isFixedSurface();
}

/*
 * JNI registration.
 */
static JNINativeMethod gJavaSamplePluginStubMethods[] = {
    { "nativeSurfaceCreated", "(ILandroid/view/View;)V", (void*) surfaceCreated },
    { "nativeSurfaceChanged", "(IIII)V", (void*) surfaceChanged },
    { "nativeSurfaceDestroyed", "(I)V", (void*) surfaceDestroyed },
    { "nativeGetSurfaceWidth", "(I)I", (void*) getSurfaceWidth },
    { "nativeGetSurfaceHeight", "(I)I", (void*) getSurfaceHeight },
    { "nativeIsFixedSurface", "(I)Z", (void*) isFixedSurface },
};

EXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {

    JNIEnv* env = NULL;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    jniRegisterNativeMethods(env, "com/android/sampleplugin/SamplePluginStub",
                             gJavaSamplePluginStubMethods, NELEM(gJavaSamplePluginStubMethods));

    return JNI_VERSION_1_4;
}
