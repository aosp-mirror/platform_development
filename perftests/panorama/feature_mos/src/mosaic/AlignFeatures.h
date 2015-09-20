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
// Align.h
// S.O. # :
// Author(s): zkira
// $Id: AlignFeatures.h,v 1.13 2011/06/17 13:35:47 mbansal Exp $

#ifndef ALIGN_H
#define ALIGN_H

#include "dbreg/dbreg.h"
#include <db_utilities_camera.h>

#include "ImageUtils.h"
#include "MatrixUtils.h"

class Align {

public:
  // Types of alignment possible
  static const int ALIGN_TYPE_PAN    = 1;

  // Return codes
  static const int ALIGN_RET_LOW_TEXTURE  = -2;
  static const int ALIGN_RET_ERROR        = -1;
  static const int ALIGN_RET_OK           = 0;
  static const int ALIGN_RET_FEW_INLIERS  = 1;

  ///// Settings for feature-based alignment
  // Number of features to use from corner detection
  static const int DEFAULT_NR_CORNERS=750;
  static const double DEFAULT_MAX_DISPARITY=0.1;//0.4;
  // Type of homography to model
  static const int DEFAULT_MOTION_MODEL=DB_HOMOGRAPHY_TYPE_R_T;
// static const int DEFAULT_MOTION_MODEL=DB_HOMOGRAPHY_TYPE_PROJECTIVE;
//  static const int DEFAULT_MOTION_MODEL=DB_HOMOGRAPHY_TYPE_AFFINE;
  static const unsigned int DEFAULT_REFERENCE_UPDATE_PERIOD=1500; //  Manual reference frame update so set this to a large number

  static const int MIN_NR_REF_CORNERS = 25;
  static const int MIN_NR_INLIERS = 10;

  Align();
  ~Align();

  // Initialization of structures, etc.
  int initialize(int width, int height, bool quarter_res, float thresh_still);

  // Add a frame.  Note: The alignment computation is performed
  // in this function
  int addFrameRGB(ImageType image);
  int addFrame(ImageType image);

  // Obtain the TRS matrix from the last two frames
  int getLastTRS(double trs[3][3]);
  char* getRegProfileString();

protected:

  db_FrameToReferenceRegistration reg;

  int frame_number;

  double Hcurr[9];   // Homography from the alignment reference to the frame-t
  double Hprev[9];   // Homography from frame-0 to the frame-(t-1)

  int reference_frame_index; // Index of the reference frame from all captured frames
  int num_frames_captured; // Total number of frames captured (different from frame_number)
  double average_tx_per_frame; // Average pixel translation per captured frame

  int width,height;

  bool quarter_res;     // Whether to process at quarter resolution
  float thresh_still;   // Translation threshold in pixels to detect still camera
  ImageType imageGray;
};


#endif
