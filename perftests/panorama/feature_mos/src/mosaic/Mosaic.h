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
// Mosaic.h
// S.O. # :
// Author(s): zkira
// $Id: Mosaic.h,v 1.16 2011/06/24 04:22:14 mbansal Exp $

#ifndef MOSAIC_H
#define MOSAIC_H

#include "ImageUtils.h"
#include "AlignFeatures.h"
#include "Blend.h"
#include "MosaicTypes.h"

/*! \mainpage Mosaic

    \section intro Introduction
    The class Mosaic provides a simple interface to the panoramic mosaicing algorithm. The class allows passing in individual image frames to be stitched together, computes the alignment transformation between them, and then stitches and blends them together into a single panoramic output which can then be accessed as a single image. \

    \section usage Usage
    The class methods need to be called as outlined in the sample application which is created from the mosaic_main.cpp file in the directory src/mosaic/. A brief snapshot of the flow is given below:

    \code
    Mosaic mosaic;
    // Define blending types to use, and the frame dimensions
    int blendingType = Blend::BLEND_TYPE_CYLPAN;
    int stripType = Blend::STRIP_TYPE_THIN;
    int width = 640;
    int height = 480;

    while (<image frames are available>)
    {
        // Check for initialization and if not, initialize
        if (!mosaic.isInitialized())
        {
          // Initialize mosaic processing
          mosaic.initialize(blendingType, stripType, width, height, -1, false, 5.0f);
        }

        // Add to list of frames
        mosaic.addFrameRGB(imageRGB);

        // Free image
        ImageUtils::freeImage(imageRGB);
    }

    // Create the mosaic
    ret = mosaic.createMosaic();

    // Get back the result
    resultYVU = mosaic.getMosaic(mosaicWidth, mosaicHeight);

    printf("Got mosaic of size %d,%d\n", mosaicWidth, mosaicHeight);

    \endcode
*/

/*!
 *  Main class that creates a mosaic by creating an aligner and blender.
 */
class Mosaic
{

public:

  Mosaic();
  ~Mosaic();

   /*!
    *   Creates the aligner and blender and initializes state.
    *   \param blendingType Type of blending to perform
    *   \param stripType    Type of strip to use. 0: thin, 1: wide. stripType
    *                       is effective only when blendingType is CylPan or
    *                       Horz. Otherwise, it is set to thin irrespective of the input.
    *   \param width        Width of input images (note: all images must be same size)
    *   \param height       Height of input images (note: all images must be same size)
    *   \param nframes      Number of frames to pre-allocate; default value -1 will allocate each frame as it comes
    *   \param quarter_res  Whether to compute alignment at quarter the input resolution (default = false)
    *   \param thresh_still Minimum number of pixels of translation detected between the new frame and the last frame before this frame is added to be mosaiced. For the low-res processing at 320x180 resolution input, we set this to 5 pixels. To reject no frames, set this to 0.0 (default value).
    *   \return             Return code signifying success or failure.
    */
  int initialize(int blendingType, int stripType, int width, int height, int nframes = -1, bool quarter_res = false, float thresh_still = 0.0);

   /*!
    *   Adds a YVU frame to the mosaic.
    *   \param imageYVU     Pointer to a YVU image.
    *   \return             Return code signifying success or failure.
    */
  int addFrame(ImageType imageYVU);

   /*!
    *   Adds a RGB frame to the mosaic.
    *   \param imageRGB     Pointer to a RGB image.
    *   \return             Return code signifying success or failure.
    */
  int addFrameRGB(ImageType imageRGB);

   /*!
    *   After adding all frames, call this function to perform the final blending.
    *   \param progress     Variable to set the current progress in.
    *   \return             Return code signifying success or failure.
    */
  int createMosaic(float &progress, bool &cancelComputation);

    /*!
    *   Obtains the resulting mosaic and its dimensions.
    *   \param width        Width of the resulting mosaic (returned)
    *   \param height       Height of the resulting mosaic (returned)
    *   \return             Pointer to image.
    */
  ImageType getMosaic(int &width, int &height);

    /*!
    *   Provides access to the internal alignment object pointer.
    *   \return             Pointer to the aligner object.
    */
  Align* getAligner() { return aligner; }

    /*!
    *   Obtain initialization state.
    *
    *   return              Returns true if initialized, false otherwise.
    */
  bool isInitialized() { return initialized; }


  /*!
   *  Return codes for mosaic.
   */
  static const int MOSAIC_RET_OK    = 1;
  static const int MOSAIC_RET_ERROR = -1;
  static const int MOSAIC_RET_CANCELLED = -2;
  static const int MOSAIC_RET_LOW_TEXTURE = -3;
  static const int MOSAIC_RET_FEW_INLIERS = 2;

protected:

  /**
   * Size of image frames making up mosaic
   */
  int width, height;

  /**
   * Size of actual mosaic
   */
  int mosaicWidth, mosaicHeight;

  /**
   * Bounding box to crop the mosaic when the gray border is not desired.
   */
  MosaicRect mosaicCroppingRect;

  ImageType imageMosaicYVU;

  /**
   * Collection of frames that will make up mosaic.
   */
  MosaicFrame **frames;

  /**
    * Subset of frames that are considered as relevant.
    */
  MosaicFrame **rframes;

  int frames_size;
  int max_frames;

  /**
    * Implicitly created frames, should be freed by Mosaic.
    */
  ImageType *owned_frames;
  int owned_size;

  /**
   * Initialization state.
   */
  bool initialized;

  /**
   *  Type of blending to perform.
   */
  int blendingType;

  /**
    * Type of strip to use. 0: thin (default), 1: wide
    */
  int stripType;

  /**
   *  Pointer to aligner.
   */
  Align *aligner;

  /**
   *  Pointer to blender.
   */
  Blend *blender;

  /**
   *  Modifies TRS matrices so that rotations are balanced
   *  about center of mosaic
   *
   * Side effect: TRS matrices of all mosaic frames
   *              are modified
   */
  int balanceRotations();

};

#endif
