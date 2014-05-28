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
// AlignFeatures.cpp
// S.O. # :
// Author(s): zkira, mbansal, bsouthall, narodits
// $Id: AlignFeatures.cpp,v 1.20 2011/06/17 13:35:47 mbansal Exp $

#include <stdio.h>
#include <string.h>

#include "trsMatrix.h"
#include "MatrixUtils.h"
#include "AlignFeatures.h"
#include "Log.h"

#define LOG_TAG "AlignFeatures"

Align::Align()
{
  width = height = 0;
  frame_number = 0;
  num_frames_captured = 0;
  reference_frame_index = 0;
  db_Identity3x3(Hcurr);
  db_Identity3x3(Hprev);
}

Align::~Align()
{
  // Free gray-scale image
  if (imageGray != ImageUtils::IMAGE_TYPE_NOIMAGE)
    ImageUtils::freeImage(imageGray);
}

char* Align::getRegProfileString()
{
  return reg.profile_string;
}

int Align::initialize(int width, int height, bool _quarter_res, float _thresh_still)
{
  int    nr_corners = DEFAULT_NR_CORNERS;
  double max_disparity = DEFAULT_MAX_DISPARITY;
  int    motion_model_type = DEFAULT_MOTION_MODEL;
  int nrsamples = DB_DEFAULT_NR_SAMPLES;
  double scale = DB_POINT_STANDARDDEV;
  int chunk_size = DB_DEFAULT_CHUNK_SIZE;
  int nrhorz = width/48;  // Empirically determined number of horizontal
  int nrvert = height/60; // and vertical buckets for harris corner detection.
  bool linear_polish = false;
  unsigned int reference_update_period = DEFAULT_REFERENCE_UPDATE_PERIOD;

  const bool DEFAULT_USE_SMALLER_MATCHING_WINDOW = false;
  bool   use_smaller_matching_window = DEFAULT_USE_SMALLER_MATCHING_WINDOW;

  quarter_res = _quarter_res;
  thresh_still = _thresh_still;

  frame_number = 0;
  num_frames_captured = 0;
  reference_frame_index = 0;
  db_Identity3x3(Hcurr);
  db_Identity3x3(Hprev);

  if (!reg.Initialized())
  {
    reg.Init(width, height, motion_model_type, 20, linear_polish, quarter_res,
            scale, reference_update_period, false, 0, nrsamples, chunk_size,
            nr_corners, max_disparity, use_smaller_matching_window,
            nrhorz, nrvert);
  }
  this->width = width;
  this->height = height;

  imageGray = ImageUtils::allocateImage(width, height, 1);

  if (reg.Initialized())
    return ALIGN_RET_OK;
  else
    return ALIGN_RET_ERROR;
}

int Align::addFrameRGB(ImageType imageRGB)
{
  ImageUtils::rgb2gray(imageGray, imageRGB, width, height);
  return addFrame(imageGray);
}

int Align::addFrame(ImageType imageGray_)
{
  int ret_code = ALIGN_RET_OK;

 // Obtain a vector of pointers to rows in image and pass in to dbreg
  ImageType *m_rows = ImageUtils::imageTypeToRowPointers(imageGray_, width, height);

  if (frame_number == 0)
  {
      reg.AddFrame(m_rows, Hcurr, true);    // Force this to be a reference frame
      int num_corner_ref = reg.GetNrRefCorners();

      if (num_corner_ref < MIN_NR_REF_CORNERS)
      {
          return ALIGN_RET_LOW_TEXTURE;
      }
  }
  else
  {
      reg.AddFrame(m_rows, Hcurr, false);
  }

  // Average translation per frame =
  //    [Translation from Frame0 to Frame(n-1)] / [(n-1)]
  average_tx_per_frame = (num_frames_captured < 2) ? 0.0 :
        Hprev[2] / (num_frames_captured - 1);

  // Increment the captured frame counter if we already have a reference frame
  num_frames_captured++;

  if (frame_number != 0)
  {
    int num_inliers = reg.GetNrInliers();

    if(num_inliers < MIN_NR_INLIERS)
    {
        ret_code = ALIGN_RET_FEW_INLIERS;

        Hcurr[0] = 1.0;
        Hcurr[1] = 0.0;
        // Set this as the average per frame translation taking into acccount
        // the separation of the current frame from the reference frame...
        Hcurr[2] = -average_tx_per_frame *
                (num_frames_captured - reference_frame_index);
        Hcurr[3] = 0.0;
        Hcurr[4] = 1.0;
        Hcurr[5] = 0.0;
        Hcurr[6] = 0.0;
        Hcurr[7] = 0.0;
        Hcurr[8] = 1.0;
    }

    if(fabs(Hcurr[2])<thresh_still && fabs(Hcurr[5])<thresh_still)  // Still camera
    {
        return ALIGN_RET_ERROR;
    }

    // compute the homography:
    double Hinv33[3][3];
    double Hprev33[3][3];
    double Hcurr33[3][3];

    // Invert and multiple with previous transformation
    Matrix33::convert9to33(Hcurr33, Hcurr);
    Matrix33::convert9to33(Hprev33, Hprev);
    normProjMat33d(Hcurr33);

    inv33d(Hcurr33, Hinv33);

    mult33d(Hcurr33, Hprev33, Hinv33);
    normProjMat33d(Hcurr33);
    Matrix9::convert33to9(Hprev, Hcurr33);
    // Since we have already factored the current transformation
    // into Hprev, we can reset the Hcurr to identity
    db_Identity3x3(Hcurr);

    // Update the reference frame to be the current frame
    reg.UpdateReference(m_rows,quarter_res,false);

    // Update the reference frame index
    reference_frame_index = num_frames_captured;
  }

  frame_number++;

  return ret_code;
}

// Get current transformation
int Align::getLastTRS(double trs[3][3])
{
  if (frame_number < 1)
  {
    trs[0][0] = 1.0;
    trs[0][1] = 0.0;
    trs[0][2] = 0.0;
    trs[1][0] = 0.0;
    trs[1][1] = 1.0;
    trs[1][2] = 0.0;
    trs[2][0] = 0.0;
    trs[2][1] = 0.0;
    trs[2][2] = 1.0;
    return ALIGN_RET_ERROR;
  }

  // Note that the logic here handles the case, where a frame is not used for
  // mosaicing but is captured and used in the preview-rendering.
  // For these frames, we don't set Hcurr to identity in AddFrame() and the
  // logic here appends their transformation to Hprev to render them with the
  // correct transformation. For the frames we do use for mosaicing, we already
  // append their Hcurr to Hprev in AddFrame() and then set Hcurr to identity.

  double Hinv33[3][3];
  double Hprev33[3][3];
  double Hcurr33[3][3];

  Matrix33::convert9to33(Hcurr33, Hcurr);
  normProjMat33d(Hcurr33);
  inv33d(Hcurr33, Hinv33);

  Matrix33::convert9to33(Hprev33, Hprev);

  mult33d(trs, Hprev33, Hinv33);
  normProjMat33d(trs);

  return ALIGN_RET_OK;
}

