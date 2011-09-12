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

#ifndef HW_EMULATOR_CAMERA_CONVERTERS_H
#define HW_EMULATOR_CAMERA_CONVERTERS_H

/*
 * Contains declaration of framebuffer conversion routines.
 */

namespace android {

inline uint8_t clamp(int  x)
{
    if (x > 255) return 255;
    if (x < 0)   return 0;
    return static_cast<uint8_t>(x);
}

/*
 * RGB565 color masks
 */

static const int kRed   = 0xf800;
static const int kGreen = 0x07e0;
static const int kBlue  = 0x001f;

/*
 * RGB -> YCbCr conversion constants and macros
 */

static const double kR0     = 0.299;
static const double kR1     = 0.587;
static const double kR2     = 0.114;
static const double kR3     = 0.169;
static const double kR4     = 0.331;
static const double kR5     = 0.499;
static const double kR6     = 0.499;
static const double kR7     = 0.418;
static const double kR8     = 0.0813;

#define RGB2Y(R,G,B)    static_cast<uint8_t>(kR0*R + kR1*G + kR2*B)
#define RGB2Cb(R,G,B)   static_cast<uint8_t>(-kR3*R - kR4*G + kR5*B + 128)
#define RGB2Cr(R,G,B)   static_cast<uint8_t>(kR6*R - kR7*G - kR8*B + 128)

/* Converts RGB565 color to YCbCr */
inline void RGB565ToYCbCr(uint16_t rgb, uint8_t* y, uint8_t* Cb, uint8_t* Cr)
{
    const uint32_t r = (rgb & kRed) >> 11;
    const uint32_t g = (rgb & kGreen) >> 5;
    const uint32_t b = rgb & kBlue;

    *y = RGB2Y(r,g,b);
    *Cb = RGB2Cb(r,g,b);
    *Cr = RGB2Cr(r,g,b);
}

/* Gets a 'Y' value for RGB565 color. */
inline uint8_t RGB565ToY(uint16_t rgb)
{
    const uint32_t r = (rgb & kRed) >> 11;
    const uint32_t g = (rgb & kGreen) >> 5;
    const uint32_t b = rgb & kBlue;

    return RGB2Y(r,g,b);
}

/*
 * YCbCr -> RGB conversion constants and macros
 */
static const double kY0     = 1.402;
static const double kY1     = 0.344;
static const double kY2     = 0.714;
static const double kY3     = 1.772;

#define YCbCr2R(Y, Cb, Cr)  clamp(Y + kY0*(Cr-128))
#define YCbCr2G(Y, Cb, Cr)  clamp(Y - kY1*(Cb-128) - kY2*(Cr-128))
#define YCbCr2B(Y, Cb, Cr)  clamp(Y + kY3*(Cb-128))

/* Converts YCbCr color to RGB565. */
inline uint16_t YCbCrToRGB565(uint8_t y, uint8_t Cb, uint8_t Cr)
{
    const uint16_t r = YCbCr2R(y, Cb, Cr) & 0x1f;
    const uint16_t g = YCbCr2G(y, Cb, Cr) & 0x3f;
    const uint16_t b = YCbCr2B(y, Cb, Cr) & 0x1f;

    return b | (g << 5) | (r << 11);
}

/* YCbCr pixel descriptor. */
struct YCbCrPixel {
    uint8_t     Y;
    uint8_t     Cb;
    uint8_t     Cr;

    inline YCbCrPixel()
        : Y(0), Cb(0), Cr(0)
    {
    }

    inline explicit YCbCrPixel(uint16_t rgb565)
    {
        RGB565ToYCbCr(rgb565, &Y, &Cb, &Cr);
    }

    inline void get(uint8_t* pY, uint8_t* pCb, uint8_t* pCr) const
    {
        *pY = Y; *pCb = Cb; *pCr = Cr;
    }
};

/* Converts an YV12 framebuffer to RGB565 framebuffer.
 * Param:
 *  yv12 - YV12 framebuffer.
 *  rgb - RGB565 framebuffer.
 *  width, height - Dimensions for both framebuffers.
 */
void YV12ToRGB565(const void* yv12, void* rgb, int width, int height);

}; /* namespace android */

#endif  /* HW_EMULATOR_CAMERA_CONVERTERS_H */

