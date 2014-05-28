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

// Pyramid.h

#ifndef PYRAMID_H
#define PYRAMID_H

#include "ImageUtils.h"

typedef unsigned short int real;

//  Structure containing a packed pyramid of type ImageTypeShort.  Used for pyramid
//  blending, among other things.

class PyramidShort
{

public:

  ImageTypeShort *ptr;              // Pointer containing the image
  real width, height;               // Width and height of input images
  real numChannels;                 // Number of channels in input images
  real border;                      // border size
  real pitch;                       // Pitch.  Used for moving through image efficiently.

  static PyramidShort *allocatePyramidPacked(real width, real height, real levels, real border = 0);
  static PyramidShort *allocateImage(real width, real height, real border);
  static void createPyramid(ImageType image, PyramidShort *pyramid, int last = 3 );
  static void freeImage(PyramidShort *image);

  static unsigned int calcStorage(real width, real height, real border2, int levels, int *lines);

  static void BorderSpread(PyramidShort *pyr, int left, int right, int top, int bot);
  static void BorderExpandOdd(PyramidShort *in, PyramidShort *out, PyramidShort *scr, int mode);
  static int BorderExpand(PyramidShort *pyr, int nlev, int mode);
  static int BorderReduce(PyramidShort *pyr, int nlev);
  static void BorderReduceOdd(PyramidShort *in, PyramidShort *out, PyramidShort *scr);
};

#endif
