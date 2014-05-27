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

///////////////////////////////////////////////////
// MosaicTypes.h
// S.O. # :
// Author(s): zkira
// $Id: MosaicTypes.h,v 1.15 2011/06/17 13:35:48 mbansal Exp $


#ifndef MOSAIC_TYPES_H
#define MOSAIC_TYPES_H

#include "ImageUtils.h"

/**
 *  Definition of rectangle in a mosaic.
 */
class MosaicRect
{
    public:
        MosaicRect()
        {
            left = right = top = bottom = 0.0;
        }

        inline int Width()
        {
            return right - left;
        }

        inline int Height()
        {
            return bottom - top;
        }

        /**
         *  Bounds of the rectangle
         */
        int left, right, top, bottom;
};

class BlendRect
{
    public:
    double lft, rgt, top, bot;
};

/**
 *  A frame making up the mosaic.
 *  Note: Currently assumes a YVU image
 *  containing separate Y,V, and U planes
 *  in contiguous memory (in that order).
 */
class MosaicFrame {
public:
  ImageType image;
  double trs[3][3];
  int width, height;
  BlendRect brect;  // This frame warped to the Mosaic coordinate system
  BlendRect vcrect; // brect clipped using the voronoi neighbors
  bool internal_allocation;

  MosaicFrame() { };
  MosaicFrame(int _width, int _height, bool allocate=true)
  {
    width = _width;
    height = _height;
    internal_allocation = allocate;
    if(internal_allocation)
        image = ImageUtils::allocateImage(width, height, ImageUtils::IMAGE_TYPE_NUM_CHANNELS);
  }


  ~MosaicFrame()
  {
    if(internal_allocation)
        if (image)
        free(image);
  }

  /**
  *  Get the V plane of the image.
  */
  inline ImageType getV()
  {
    return (image + (width*height));
  }

  /**
  *  Get the U plane of the image.
  */
  inline ImageType getU()
  {
    return (image + (width*height*2));
  }

  /**
  *  Get a pixel from the V plane of the image.
  */
  inline int getV(int y, int x)
  {
    ImageType U = image + (width*height);
    return U[y*width+x];
  }

  /**
  *  Get a pixel from the U plane of the image.
  */
  inline int getU(int y, int x)
  {
    ImageType U = image + (width*height*2);
    return U[y*width+x];
  }

};

/**
 *  Structure for describing a warp.
 */
typedef struct {
  int horizontal;
  double theta;
  double x;
  double y;
  double width;
  double radius;
  double direction;
  double correction;
  int blendRange;
  int blendRangeUV;
  int nlevs;
  int nlevsC;
  int blendingType;
  int stripType;
  // Add an overlap to prevent a gap between pictures due to roundoffs
  double roundoffOverlap;// 1.5

} BlendParams;

#endif
