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

#pragma once


#ifdef _WIN32
#ifdef DBREG_EXPORTS
#define DBREG_API __declspec(dllexport)
#else
#define DBREG_API __declspec(dllimport)
#endif
#else
#define DBREG_API
#endif

extern "C" {
#include "vp_motionmodel.h"
}

#define MOTION_ARRAY 5


/*!
 * Performs smoothing on the motion estimate from feature_stab.
 */
class DBREG_API db_StabilizationSmoother
{
public:
    db_StabilizationSmoother();
    ~db_StabilizationSmoother();

    /*!
     * Initialize parameters for stab-smoother.
    */
    void Init();

    //! Smothing type
    typedef enum {
        SimpleSmooth = 0, //!< simple smooth
        AdaptSmooth  = 1, //!< adaptive smooth
        PanSmooth    = 2  //!< pan motion smooth
    } SmoothType;

    /*!
     * Smooth-motion is to do a weight-average between the current affine and
     * motLF. The way to change the affine is only for the display purpose.
     * It removes the high frequency motion and keep the low frequency motion
     * to the display. IIR implmentation.
     * \param inmot input motion parameters
     * \param outmot smoothed output motion parameters
    */
    bool smoothMotion(VP_MOTION *inmot, VP_MOTION *outmot);

    /*!
     * The adaptive smoothing version of the above fixed smoothing function.
     * \param hsize width of the image being aligned
     * \param vsize height of the image being aligned
     * \param inmot input motion parameters
     * \param outmot    smoothed output motion parameters
    */
    bool smoothMotionAdaptive(/*VP_BIMG *bimg,*/int hsize, int vsize, VP_MOTION *inmot, VP_MOTION *outmot);
    bool smoothPanMotion_1(VP_MOTION *inmot, VP_MOTION *outmot);
    bool smoothPanMotion_2(VP_MOTION *inmot, VP_MOTION *outmot);

    /*!
    * Set the smoothing factor for the stab-smoother.
    * \param factor the factor value to set
    */
    inline void setSmoothingFactor(float factor) { f_smoothFactor = factor; }

    /*!
     * Reset smoothing
    */
    inline void resetSmoothing(bool flag) { f_smoothReset = flag; }
    /*!
     * Set the zoom factor value.
     * \param zoom  the value to set to
    */
    inline void setZoomFactor(float zoom) { f_zoom = zoom; }
    /*!
     * Set the minimum damping factor value.
     * \param factor    the value to set to
    */
    inline void setminDampingFactor(float factor) { f_minDampingFactor = factor; }

    /*!
     * Returns the current smoothing factor.
    */
    inline float getSmoothingFactor(void) { return f_smoothFactor; }
    /*!
     * Returns the current zoom factor.
    */
    inline float getZoomFactor(void) { return f_zoom; }
    /*!
     * Returns the current minimum damping factor.
    */
    inline float getminDampingFactor(void) { return f_minDampingFactor; }
    /*!
     * Returns the current state of the smoothing reset flag.
    */
    inline bool  getSmoothReset(void) { return f_smoothReset; }
    /*!
     * Returns the current low frequency motion parameters.
    */
    inline VP_MOTION getMotLF(void) { return f_motLF; }
    /*!
     * Returns the inverse of the current low frequency motion parameters.
    */
    inline VP_MOTION getImotLF(void) { return f_imotLF; }
    /*!
     * Set the dimensions of the alignment image.
     * \param hsize width of the image
     * \param vsize height of the image
    */
    inline void setSize(int hsize, int vsize) { f_hsize = hsize; f_vsize = vsize; }

protected:

    bool smoothMotion(VP_MOTION *inmot, VP_MOTION *outmot, double smooth_factor);
    bool smoothMotion1(VP_MOTION *inmot, VP_MOTION *outmot, VP_MOTION *motLF, VP_MOTION *imotLF, double smooth_factor);
    void iterativeSmooth(VP_MOTION *input, VP_MOTION *output, double border_factor);
    bool is_point_in_rect(double px, double py, double rx, double ry, double w, double h);


private:
    int f_hsize;
    int f_vsize;
    bool f_smoothOn;
    bool f_smoothReset;
    float f_smoothFactor;
    float f_minDampingFactor;
    float f_zoom;
    VP_MOTION f_motLF;
    VP_MOTION f_imotLF;
    VP_MOTION f_hist_mot[MOTION_ARRAY];
    VP_MOTION f_hist_mot_speed[MOTION_ARRAY-1];
    VP_MOTION f_hist_diff_mot[MOTION_ARRAY-1];
    VP_MOTION f_disp_mot;
    VP_MOTION f_src_mot;
    VP_MOTION f_diff_avg;

};

