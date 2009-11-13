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

#define EXPORT __attribute__((visibility("default")))

static jstring stringFromJNI( JNIEnv* env, jobject thiz )
{
    return env->NewStringUTF("Hello from JNI !");
}

/*
 * JNI registration.
 */
static JNINativeMethod gJavaSamplePluginStubMethods[] = {
    { "nativeStringFromJNI", "()Ljava/lang/String;", (void*) stringFromJNI },
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
