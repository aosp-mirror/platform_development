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

///////////////////////////////////////////////////////////
// Interp.h
// $Id: Interp.h,v 1.2 2011/06/17 13:35:48 mbansal Exp $

#ifndef INTERP_H
#define INTERP_H

#include "Pyramid.h"

#define CTAPS 40
static double ciTable[81] = {
        1, 0.998461, 0.993938, 0.98657, 0.9765,
        0.963867, 0.948813, 0.931477, 0.912, 0.890523,
        0.867188, 0.842133, 0.8155, 0.78743, 0.758062,
        0.727539, 0.696, 0.663586, 0.630437, 0.596695,
        0.5625, 0.527992, 0.493312, 0.458602, 0.424,
        0.389648, 0.355687, 0.322258, 0.2895, 0.257555,
        0.226562, 0.196664, 0.168, 0.140711, 0.114937,
        0.0908203, 0.0685, 0.0481172, 0.0298125, 0.0137266,
        0, -0.0118828, -0.0225625, -0.0320859, -0.0405,
        -0.0478516, -0.0541875, -0.0595547, -0.064, -0.0675703,
        -0.0703125, -0.0722734, -0.0735, -0.0740391, -0.0739375,
        -0.0732422, -0.072, -0.0702578, -0.0680625, -0.0654609,
        -0.0625, -0.0592266, -0.0556875, -0.0519297, -0.048,
        -0.0439453, -0.0398125, -0.0356484, -0.0315, -0.0274141,
        -0.0234375, -0.0196172, -0.016, -0.0126328, -0.0095625,
        -0.00683594, -0.0045, -0.00260156, -0.0011875, -0.000304687, 0.0
};

inline double ciCalc(PyramidShort *img, int xi, int yi, double xfrac, double yfrac)
{
  double tmpf[4];

  // Interpolate using 16 points
  ImageTypeShortBase *in = img->ptr[yi-1] + xi - 1;
  int off = (int)(xfrac * CTAPS);

  tmpf[0] = in[0] * ciTable[off + 40];
  tmpf[0] += in[1] * ciTable[off];
  tmpf[0] += in[2] * ciTable[40 - off];
  tmpf[0] += in[3] * ciTable[80 - off];
  in += img->pitch;
  tmpf[1] = in[0] * ciTable[off + 40];
  tmpf[1] += in[1] * ciTable[off];
  tmpf[1] += in[2] * ciTable[40 - off];
  tmpf[1] += in[3] * ciTable[80 - off];
  in += img->pitch;
  tmpf[2] = in[0] * ciTable[off + 40];
  tmpf[2] += in[1] * ciTable[off];
  tmpf[2] += in[2] * ciTable[40 - off];
  tmpf[2] += in[3] * ciTable[80 - off];
  in += img->pitch;
  tmpf[3] = in[0] * ciTable[off + 40];
  tmpf[3] += in[1] * ciTable[off];
  tmpf[3] += in[2] * ciTable[40 - off];
  tmpf[3] += in[3] * ciTable[80 - off];

  // this is the final interpolation
  off = (int)(yfrac * CTAPS);
  return (ciTable[off + 40] * tmpf[0] + ciTable[off] * tmpf[1] +
          ciTable[40 - off] * tmpf[2] + ciTable[80 - off] * tmpf[3]);
}

#endif
