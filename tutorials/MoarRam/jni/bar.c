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

char *gPtr2;
static int num2MByteBlocks;

void
Java_com_android_benchmark_moarram_MainActivity_add2MByteBlocksNative(
    JNIEnv*  env,
    jobject  this)
{
    char **ptr = malloc(2*1024*1024);
    *ptr = gPtr2;
    gPtr2 = (char *) ptr;
    num2MByteBlocks++;
    ALOGW("%d 2M-byte blocks allocated so far (just allocated %p)",
          num2MByteBlocks, gPtr2);
}

void
Java_com_android_benchmark_moarram_MainActivity_free2MByteBlocksNative(
    JNIEnv*  env,
    jobject  this)
{
    if (gPtr2 == NULL) {
        ALOGW("All 2M-byte blocks are freed");
        return;
    }

    char **ptr = (char **) gPtr2;
    gPtr2 = *ptr;
    free(ptr);
    num2MByteBlocks--;
    ALOGW("%d 2M-byte blocks allocated so far (just freed %p)",
          num2MByteBlocks, ptr);
}
