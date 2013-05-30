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

char *gPtr;
static int num32ByteBlocks;

void
Java_com_android_benchmark_moarram_MainActivity_add32ByteBlocksNative(
    JNIEnv*  env,
    jobject  this)
{
    char **ptr = malloc(32);
    *ptr = gPtr;
    gPtr = (char *) ptr;
    num32ByteBlocks++;
    ALOGW("%d 32-byte blocks allocated so far (just allocated %p)",
          num32ByteBlocks, gPtr);
}

void
Java_com_android_benchmark_moarram_MainActivity_free32ByteBlocksNative(
    JNIEnv*  env,
    jobject  this)
{
    if (gPtr == NULL) {
        ALOGW("All 32-byte blocks are freed");
        return;
    }

    char **ptr = (char **) gPtr;
    gPtr = *ptr;
    free(ptr);
    num32ByteBlocks--;
    ALOGW("%d 32-byte blocks allocated so far (just freed %p)",
          num32ByteBlocks, ptr);
}
