/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include <jni.h>
#include <cutils/log.h>

#if defined(LOG_TAG)
#undef LOG_TAG
#define LOG_TAG "MOARRAM"
#endif

char *gPtr17;
char *gPtr71;
static int num17ByteBlocks;
static int num71ByteBlocks;

void
Java_com_android_benchmark_moarram_MainActivity_addVariableSizedBlocksNative(
    JNIEnv*  env,
    jobject  this,
    jint id)
{
    int size;
    char **gPtr;
    char **ptr;
    if (id == 0) {
        size = 17;
        gPtr = &gPtr17;
    } else {
        size = 71;
        gPtr = &gPtr71;
    }
    ptr = malloc(size);
    *ptr = *gPtr;
    *gPtr = (char *) ptr;
    ALOGW("%d %d-byte blocks allocated so far (just allocated %p)",
          id == 0 ? ++num17ByteBlocks : ++num71ByteBlocks,
          size, ptr);
}

void
Java_com_android_benchmark_moarram_MainActivity_freeVariableSizedBlocksNative(
    JNIEnv*  env,
    jobject  this,
    jint id)
{
    int size;
    char **ptr;
    char **gPtr;
    if (id == 0) {
        size = 17;
        gPtr = &gPtr17;
    } else {
        size = 71;
        gPtr = &gPtr71;
    }
    if (*gPtr == NULL) {
        ALOGW("All %d-byte blocks are freed", size);
        return;
    }
    ptr = (char **) *gPtr;
    *gPtr = *ptr;
    free(ptr);
    ALOGW("%d %d-byte blocks allocated so far (just freed %p)",
          id == 0 ? --num17ByteBlocks : --num71ByteBlocks,
          size, ptr);
}
