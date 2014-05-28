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
// Blend.h
// $Id: Blend.h,v 1.23 2011/06/24 04:22:14 mbansal Exp $

#ifndef BLEND_H
#define BLEND_H

#include "MosaicTypes.h"
#include "Pyramid.h"
#include "Delaunay.h"

#define BLEND_RANGE_DEFAULT 6
#define BORDER 8

// Percent of total mosaicing time spent on each of the following operations
const float TIME_PERCENT_ALIGN = 20.0;
const float TIME_PERCENT_BLEND = 75.0;
const float TIME_PERCENT_FINAL = 5.0;

// This threshold determines the minimum separation between the image centers
// of the input image frames for them to be accepted for blending in the
// STRIP_TYPE_WIDE mode.
const float STRIP_SEPARATION_THRESHOLD_PXLS = 10;

// This threshold determines the number of pixels on either side of the strip
// to cross-fade using the images contributing to each seam.
const float STRIP_CROSS_FADE_WIDTH_PXLS = 2;
// This specifies the maximum pyramid level to which cross-fading is applied.
// The original image resolution is Level-0, half of that size is Level-1 and
// so on. BLEND_RANGE_DEFAULT specifies the number of pyramid levels used by
// the blending algorithm.
const int STRIP_CROSS_FADE_MAX_PYR_LEVEL = 2;

/**
 *  Class for pyramid blending a mosaic.
 */
class Blend {

public:

  static const int BLEND_TYPE_NONE    = -1;
  static const int BLEND_TYPE_FULL    = 0;
  static const int BLEND_TYPE_PAN     = 1;
  static const int BLEND_TYPE_CYLPAN  = 2;
  static const int BLEND_TYPE_HORZ   = 3;

  static const int STRIP_TYPE_THIN      = 0;
  static const int STRIP_TYPE_WIDE      = 1;

  static const int BLEND_RET_ERROR        = -1;
  static const int BLEND_RET_OK           = 0;
  static const int BLEND_RET_ERROR_MEMORY = 1;
  static const int BLEND_RET_CANCELLED    = -2;

  Blend();
  ~Blend();

  int initialize(int blendingType, int stripType, int frame_width, int frame_height);

  int runBlend(MosaicFrame **frames, MosaicFrame **rframes, int frames_size, ImageType &imageMosaicYVU,
        int &mosaicWidth, int &mosaicHeight, float &progress, bool &cancelComputation);

protected:

  PyramidShort *m_pFrameYPyr;
  PyramidShort *m_pFrameUPyr;
  PyramidShort *m_pFrameVPyr;

  PyramidShort *m_pMosaicYPyr;
  PyramidShort *m_pMosaicUPyr;
  PyramidShort *m_pMosaicVPyr;

  CDelaunay m_Triangulator;
  CSite *m_AllSites;

  BlendParams m_wb;

  // Height and width of individual frames
  int width, height;

   // Height and width of mosaic
  unsigned short Mwidth, Mheight;

  // Helper functions
  void FrameToMosaic(double trs[3][3], double x, double y, double &wx, double &wy);
  void MosaicToFrame(double trs[3][3], double x, double y, double &wx, double &wy);
  void FrameToMosaicRect(int width, int height, double trs[3][3], BlendRect &brect);
  void ClipBlendRect(CSite *csite, BlendRect &brect);
  void AlignToMiddleFrame(MosaicFrame **frames, int frames_size);

  int  DoMergeAndBlend(MosaicFrame **frames, int nsite,  int width, int height, YUVinfo &imgMos, MosaicRect &rect, MosaicRect &cropping_rect, float &progress, bool &cancelComputation);
  void ComputeMask(CSite *csite, BlendRect &vcrect, BlendRect &brect, MosaicRect &rect, YUVinfo &imgMos, int site_idx);
  void ProcessPyramidForThisFrame(CSite *csite, BlendRect &vcrect, BlendRect &brect, MosaicRect &rect, YUVinfo &imgMos, double trs[3][3], int site_idx);

  int  FillFramePyramid(MosaicFrame *mb);

  // TODO: need to add documentation about the parameters
  void ComputeBlendParameters(MosaicFrame **frames, int frames_size, int is360);
  void SelectRelevantFrames(MosaicFrame **frames, int frames_size,
        MosaicFrame **relevant_frames, int &relevant_frames_size);

  int  PerformFinalBlending(YUVinfo &imgMos, MosaicRect &cropping_rect);
  void CropFinalMosaic(YUVinfo &imgMos, MosaicRect &cropping_rect);

private:
   static const float LIMIT_SIZE_MULTIPLIER = 5.0f * 2.0f;
   static const float LIMIT_HEIGHT_MULTIPLIER = 2.5f;
   int MosaicSizeCheck(float sizeMultiplier, float heightMultiplier);
   void RoundingCroppingSizeToMultipleOf8(MosaicRect& rect);
};

#endif
