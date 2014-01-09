/*
 * Copyright 2013 The Android Open Source Project
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
 */

#pragma once

#include <jni.h>
#include <errno.h>
#include <vector>
#include <map>
#include <fstream>
#include <iostream>
#include <string>

#include <EGL/egl.h>
#include <GLES2/gl2.h>

#include <android/sensor.h>
#include <android/log.h>
#include <android_native_app_glue.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, JNIHelper::getAppName(), __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, JNIHelper::getAppName(), __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, JNIHelper::getAppName(), __VA_ARGS__))

jclass retrieveClass(JNIEnv *jni, ANativeActivity* activity, const char* className);

/******************************************************************
 * Helpers to invoke Java methods
 * To use this class, add NDKHelper.java as a corresponding helpers in Java side
 */
class JNIHelper
{
private:
    static ANativeActivity* _activity;
    static jobject _objJNIHelper;
    static jclass _clsJNIHelper;

    static jstring getExternalFilesDir( JNIEnv *env );

    static std::string _appName;
public:
    JNIHelper()
    {
    };
    ~JNIHelper() {
        JNIEnv *env;
        _activity->vm->AttachCurrentThread(&env, NULL);

        env->DeleteGlobalRef(_objJNIHelper);
        env->DeleteGlobalRef(_clsJNIHelper);

        _activity->vm->DetachCurrentThread();

    };
    static void init( ANativeActivity* activity );
    static bool readFile( const char* fileName, std::vector<uint8_t>& buffer );
    static uint32_t loadTexture(const char* fileName );
    static std::string convertString( const char* str, const char* encode );

    static const char* getAppName() {
        return _appName.c_str();
    };




};
