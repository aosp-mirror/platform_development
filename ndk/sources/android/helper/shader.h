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

#ifndef SHADER_H_
#define SHADER_H_

#include <jni.h>
#include <errno.h>

#include <vector>
#include <map>
#include <string>

#include <EGL/egl.h>
#include <GLES/gl.h>

#include <android/sensor.h>
#include <android/log.h>
#include <android_native_app_glue.h>
#include <android/native_window_jni.h>
#include <cpu-features.h>

#include "JNIHelper.h"

/******************************************************************
 * Shader compiler helper
 *
 * compileShader() with std::map helps patching on a shader on the fly.
 * For a example,
 * map : %KEY% -> %VALUE% replaces all %KEY% entries in the given shader code to %VALUE"
 *
 */
class shader {
public:
    static bool    compileShader(GLuint *shader, const GLenum type,
            std::vector<uint8_t>& data);
    static bool    compileShader(GLuint *shader, const GLenum type,
            const GLchar *source, const int32_t iSize);
    static bool compileShader(GLuint *shader, const GLenum type,
            const char *strFileName);
    static bool compileShader(GLuint *shader, const GLenum type,
            const char *strFileName, const std::map<std::string, std::string>& mapParameters);
    static bool linkProgram(const GLuint prog);
    static bool validateProgram(const GLuint prog);
};




#endif /* SHADER_H_ */
