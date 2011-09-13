/*
 * Copyright (C) 2011 The Android Open Source Project
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

/*
 * Contains implemenation of framebuffer conversion routines.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_Converter"
#include <cutils/log.h>
#include "Converters.h"

namespace android {

void YV12ToRGB565(const void* yv12, void* rgb, int width, int height)
{
    const int pix_total = width * height;
    uint16_t* rgb_buf = reinterpret_cast<uint16_t*>(rgb);
    const uint8_t* Y = reinterpret_cast<const uint8_t*>(yv12);
    const uint8_t* U_pos = Y + pix_total;
    const uint8_t* V_pos = U_pos + pix_total / 4;
    const uint8_t* U = U_pos;
    const uint8_t* V = V_pos;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x += 2) {
            const uint8_t nU = *U; U++;
            const uint8_t nV = *V; V++;
            *rgb_buf = YUVToRGB565(*Y, nU, nV);
            Y++; rgb_buf++;
            *rgb_buf = YUVToRGB565(*Y, nU, nV);
            Y++; rgb_buf++;
        }
        if (y & 0x1) {
            U_pos = U;
            V_pos = V;
        } else {
            U = U_pos;
            V = V_pos;
        }
    }
}

void YV12ToRGB32(const void* yv12, void* rgb, int width, int height)
{
    const int pix_total = width * height;
    uint32_t* rgb_buf = reinterpret_cast<uint32_t*>(rgb);
    const uint8_t* Y = reinterpret_cast<const uint8_t*>(yv12);
    const uint8_t* U_pos = Y + pix_total;
    const uint8_t* V_pos = U_pos + pix_total / 4;
    const uint8_t* U = U_pos;
    const uint8_t* V = V_pos;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x += 2) {
            const uint8_t nU = *U; U++;
            const uint8_t nV = *V; V++;
            *rgb_buf = YUVToRGB32(*Y, nU, nV);
            Y++; rgb_buf++;
            *rgb_buf = YUVToRGB32(*Y, nU, nV);
            Y++; rgb_buf++;
        }
        if (y & 0x1) {
            U_pos = U;
            V_pos = V;
        } else {
            U = U_pos;
            V = V_pos;
        }
    }
}

}; /* namespace android */
