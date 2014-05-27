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

// @jke - the next few lines are for extracting timing data.  TODO: Remove after test
#define PROFILE 0

#include "dbstabsmooth.h"

#include <db_feature_detection.h>
#include <db_feature_matching.h>
#include <db_rob_image_homography.h>

#if PROFILE
    #include <sys/time.h>
#endif

/*! \mainpage db_FrameToReferenceRegistration

 \section intro Introduction

 db_FrameToReferenceRegistration provides a simple interface to a set of sophisticated algorithms for stabilizing
 video sequences.  As its name suggests, the class is used to compute parameters that will allow us to warp incoming video
 frames and register them with respect to a so-called <i>reference</i> frame.  The reference frame is simply the first
 frame of a sequence; the registration process is that of estimating the parameters of a warp that can be applied to
 subsequent frames to make those frames align with the reference.  A video made up of these warped frames will be more
 stable than the input video.

 For more technical information on the internal structure of the algorithms used within the db_FrameToRegistration class,
 please follow this <a href="../Sarnoff image registration.docx">link</a>.

 \section usage Usage
 In addition to the class constructor, there are two main functions of db_FrameToReferenceRegistration that are of
 interest to the programmer.  db_FrameToReferenceRegistration::Init(...) is used to initialize the parameters of the
 registration algorithm. db_FrameToReferenceRegistration::AddFrame(...) is the method by which each new video frame
 is introduced to the registration algorithm, and produces the estimated registration warp parameters.

 The following example illustrates how the major methods of the class db_FrameToReferenceRegistration can be used together
 to calculate the registration parameters for an image sequence.  In the example, the calls to the methods of
 db_FrameToReferenceRegistration match those found in the API, but supporting code should be considered pseudo-code.
 For a more complete example, please consult the source code for dbregtest.


    \code
    // feature-based image registration class:
    db_FrameToReferenceRegistration reg;

    // Image data
    const unsigned char * const * image_storage;

    // The 3x3 frame to reference registration parameters
    double frame_to_ref_homography[9];

    // a counter to count the number of frames processed.
    unsigned long frame_counter;
    // ...

    // main loop - keep going while there are images to process.
    while (ImagesAreAvailable)
    {
        // Call functions to place latest data into image_storage
        // ...

        // if the registration object is not yet initialized, then do so
        // The arguments to this function are explained in the accompanying
        // html API documentation
        if (!reg.Initialized())
        {
            reg.Init(w,h,motion_model_type,25,linear_polish,quarter_resolution,
                   DB_POINT_STANDARDDEV,reference_update_period,
                   do_motion_smoothing,motion_smoothing_gain,
                   DB_DEFAULT_NR_SAMPLES,DB_DEFAULT_CHUNK_SIZE,
                   nr_corners,max_disparity);
        }

        // Present the new image data to the registration algorithm,
        // with the result being stored in the frame_to_ref_homography
        // variable.
        reg.AddFrame(image_storage,frame_to_ref_homography);

        // frame_to_ref_homography now contains the stabilizing transform
        // use this to warp the latest image for display, etc.

        // if this is the first frame, we need to tell the registration
        // class to store the image as its reference.  Otherwise, AddFrame
        // takes care of that.
        if (frame_counter == 0)
        {
            reg.UpdateReference(image_storage);
        }

        // increment the frame counter
        frame_counter++;
    }

    \endcode

 */

/*!
 * Performs feature-based frame to reference image registration.
 */
class DBREG_API db_FrameToReferenceRegistration
{
public:
    db_FrameToReferenceRegistration(void);
    ~db_FrameToReferenceRegistration();

    /*!
     * Set parameters and allocate memory. Note: The default values of these parameters have been set to the values used for the android implementation (i.e. the demo APK).
     * \param width         image width
     * \param height        image height
     * \param homography_type see definitions in \ref LMRobImageHomography
     * \param max_iterations    max number of polishing steps
     * \param linear_polish     whether to perform a linear polishing step after RANSAC
     * \param quarter_resolution    whether to process input images at quarter resolution (for computational efficiency)
     * \param scale         Cauchy scale coefficient (see db_ExpCauchyReprojectionError() )
     * \param reference_update_period   how often to update the alignment reference (in units of number of frames)
     * \param do_motion_smoothing   whether to perform display reference smoothing
     * \param motion_smoothing_gain weight factor to reflect how fast the display reference must follow the current frame if motion smoothing is enabled
     * \param nr_samples        number of times to compute a hypothesis
     * \param chunk_size        size of cost chunks
     * \param cd_target_nr_corners  target number of corners for corner detector
     * \param cm_max_disparity      maximum disparity search range for corner matcher (in units of ratio of image width)
     * \param cm_use_smaller_matching_window    if set to true, uses a correlation window of 5x5 instead of the default 11x11
     * \param cd_nr_horz_blocks     the number of horizontal blocks for the corner detector to partition the image
     * \param cd_nr_vert_blocks     the number of vertical blocks for the corner detector to partition the image
    */
    void Init(int width, int height,
          int       homography_type = DB_HOMOGRAPHY_TYPE_DEFAULT,
          int       max_iterations = DB_DEFAULT_MAX_ITERATIONS,
          bool      linear_polish = false,
          bool   quarter_resolution = true,
          double  scale = DB_POINT_STANDARDDEV,
          unsigned int reference_update_period = 3,
          bool   do_motion_smoothing = false,
          double motion_smoothing_gain = 0.75,
          int   nr_samples = DB_DEFAULT_NR_SAMPLES,
          int   chunk_size = DB_DEFAULT_CHUNK_SIZE,
          int    cd_target_nr_corners = 500,
          double cm_max_disparity = 0.2,
          bool   cm_use_smaller_matching_window = false,
          int    cd_nr_horz_blocks = 5,
          int    cd_nr_vert_blocks = 5);

    /*!
     * Reset the transformation type that is being use to perform alignment. Use this to change the alignment type at run time.
     * \param homography_type   the type of transformation to use for performing alignment (see definitions in \ref LMRobImageHomography)
    */
    void ResetHomographyType(int homography_type) { m_homography_type = homography_type; }

    /*!
     * Enable/Disable motion smoothing. Use this to turn motion smoothing on/off at run time.
     * \param enable    flag indicating whether to turn the motion smoothing on or off.
    */
    void ResetSmoothing(bool enable) { m_do_motion_smoothing = enable; }

    /*!
     * Align an inspection image to an existing reference image, update the reference image if due and perform motion smoothing if enabled.
     * \param im                new inspection image
     * \param H             computed transformation from reference to inspection coordinate frame. Identity is returned if no reference frame was set.
     * \param force_reference   make this the new reference image
     */
    int AddFrame(const unsigned char * const * im, double H[9], bool force_reference=false, bool prewarp=false);

    /*!
     * Returns true if Init() was run.
     */
    bool Initialized() const { return m_initialized; }

    /*!
     * Returns true if the current frame is being used as the alignment reference.
    */
    bool IsCurrentReference() const { return m_current_is_reference; }

    /*!
     * Returns true if we need to call UpdateReference now.
     */
    bool NeedReferenceUpdate();

    /*!
     * Returns the pointer reference to the alignment reference image data
    */
    unsigned char ** GetReferenceImage() { return m_reference_image; }

    /*!
     * Returns the pointer reference to the double array containing the homogeneous coordinates for the matched reference image corners.
    */
    double * GetRefCorners() { return m_corners_ref; }
    /*!
     * Returns the pointer reference to the double array containing the homogeneous coordinates for the matched inspection image corners.
    */
    double * GetInsCorners() { return m_corners_ins; }
    /*!
     * Returns the number of correspondences between the reference and inspection images.
    */
    int GetNrMatches() { return m_nr_matches; }

    /*!
     * Returns the number of corners detected in the current reference image.
    */
    int GetNrRefCorners() { return m_nr_corners_ref; }

    /*!
     * Returns the pointer to an array of indices that were found to be RANSAC inliers from the matched corner lists.
    */
    int* GetInliers() { return m_inlier_indices; }

    /*!
     * Returns the number of inliers from the RANSAC matching step.
    */
    int  GetNrInliers() { return m_num_inlier_indices; }

    //std::vector<int>& GetInliers();
    //void Polish(std::vector<int> &inlier_indices);

    /*!
     * Perform a linear polishing step by re-estimating the alignment transformation using the RANSAC inliers.
     * \param inlier_indices    pointer to an array of indices that were found to be RANSAC inliers from the matched corner lists.
     * \param num_inlier_indices    number of inliers i.e. the length of the array passed as the first argument.
    */
    void Polish(int *inlier_indices, int &num_inlier_indices);

    /*!
     * Reset the motion smoothing parameters to their initial values.
    */
    void ResetMotionSmoothingParameters() { m_stab_smoother.Init(); }

    /*!
     * Update the alignment reference image to the specified image.
     * \param im    pointer to the image data to be used as the new alignment reference.
     * \param subsample boolean flag to control whether the function should internally subsample the provided image to the size provided in the Init() function.
    */
    int UpdateReference(const unsigned char * const * im, bool subsample = true, bool detect_corners = true);

    /*!
     * Returns the transformation from the display reference to the alignment reference frame
    */
    void Get_H_dref_to_ref(double H[9]);
    /*!
     * Returns the transformation from the display reference to the inspection reference frame
    */
    void Get_H_dref_to_ins(double H[9]);
    /*!
     * Set the transformation from the display reference to the inspection reference frame
     * \param H the transformation to set
    */
    void Set_H_dref_to_ins(double H[9]);

    /*!
     * Reset the display reference to the current frame.
    */
    void ResetDisplayReference();

    /*!
     * Estimate a secondary motion model starting from the specified transformation.
     * \param H the primary motion model to start from
    */
    void EstimateSecondaryModel(double H[9]);

    /*!
     *
    */
    void SelectOutliers();

    char *profile_string;

protected:
    void Clean();
    void GenerateQuarterResImage(const unsigned char* const * im);

    int     m_im_width;
    int     m_im_height;

    // RANSAC and refinement parameters:
    int m_homography_type;
    int     m_max_iterations;
    double  m_scale;
    int     m_nr_samples;
    int     m_chunk_size;
    double  m_outlier_t2;

    // Whether to fit a linear model to just the inliers at the end
    bool   m_linear_polish;
    double m_polish_C[36];
    double m_polish_D[6];

    // local state
    bool m_current_is_reference;
    bool m_initialized;

    // inspection to reference homography:
    double m_H_ref_to_ins[9];
    double m_H_dref_to_ref[9];

    // feature extraction and matching:
    db_CornerDetector_u m_cd;
    db_Matcher_u        m_cm;

    // length of corner arrays:
    unsigned long m_max_nr_corners;

    // corner locations of reference image features:
    double * m_x_corners_ref;
    double * m_y_corners_ref;
    int  m_nr_corners_ref;

    // corner locations of inspection image features:
    double * m_x_corners_ins;
    double * m_y_corners_ins;
    int      m_nr_corners_ins;

    // length of match index arrays:
    unsigned long m_max_nr_matches;

    // match indices:
    int * m_match_index_ref;
    int * m_match_index_ins;
    int   m_nr_matches;

    // pointer to internal copy of the reference image:
    unsigned char ** m_reference_image;

    // pointer to internal copy of last aligned inspection image:
    unsigned char ** m_aligned_ins_image;

    // pointer to quarter resolution image, if used.
    unsigned char** m_quarter_res_image;

    // temporary storage for the quarter resolution image processing
    unsigned char** m_horz_smooth_subsample_image;

    // temporary space for homography computation:
    double * m_temp_double;
    int * m_temp_int;

    // homogenous image point arrays:
    double * m_corners_ref;
    double * m_corners_ins;

    // Indices of the points within the match lists
    int * m_inlier_indices;
    int m_num_inlier_indices;

    //void ComputeInliers(double H[9], std::vector<int> &inlier_indices);
    void ComputeInliers(double H[9]);

    // cost arrays:
    void ComputeCostArray();
    bool m_sq_cost_computed;
    double * m_sq_cost;

    // cost histogram:
    void ComputeCostHistogram();
    int *m_cost_histogram;

    void SetOutlierThreshold();

    // utility function for smoothing the motion parameters.
    void SmoothMotion(void);

private:
    double m_K[9];
    const int m_over_allocation;

    bool m_reference_set;

    // Maximum number of inliers seen until now w.r.t the current reference frame
    int m_max_inlier_count;

    // Number of cost histogram bins:
    int m_nr_bins;
    // All costs above this threshold get put into the last bin:
    int m_max_cost_pix;

    // whether to quarter the image resolution for processing, or not
    bool m_quarter_resolution;

    // the period (in number of frames) for reference update.
    unsigned int m_reference_update_period;

    // the number of frames processed so far.
    unsigned int m_nr_frames_processed;

    // smoother for motion transformations
    db_StabilizationSmoother m_stab_smoother;

    // boolean to control whether motion smoothing occurs (or not)
    bool m_do_motion_smoothing;

    // double to set the gain for motion smoothing
    double m_motion_smoothing_gain;
};
/*!
 Create look-up tables to undistort images. Only Bougeut (Matlab toolkit)
 is currently supported. Can be used with db_WarpImageLut_u().
 \code
    xd = H*xs;
    xd = xd/xd(3);
 \endcode
 \param lut_x   pre-allocated float image
 \param lut_y   pre-allocated float image
 \param w       width
 \param h       height
 \param H       image homography from source to destination
 */
inline void db_GenerateHomographyLut(float ** lut_x,float ** lut_y,int w,int h,const double H[9])
{
    assert(lut_x && lut_y);
    double x[3] = {0.0,0.0,1.0};
    double xb[3];

/*
    double xl[3];

    // Determine the output coordinate system ROI
    double Hinv[9];
    db_InvertAffineTransform(Hinv,H);
    db_Multiply3x3_3x1(xl, Hinv, x);
    xl[0] = db_SafeDivision(xl[0],xl[2]);
    xl[1] = db_SafeDivision(xl[1],xl[2]);
*/

    for ( int i = 0; i < w; ++i )
        for ( int j = 0; j < h; ++j )
        {
            x[0] = double(i);
            x[1] = double(j);
            db_Multiply3x3_3x1(xb, H, x);
            xb[0] = db_SafeDivision(xb[0],xb[2]);
            xb[1] = db_SafeDivision(xb[1],xb[2]);

            lut_x[j][i] = float(xb[0]);
            lut_y[j][i] = float(xb[1]);
        }
}

/*!
 * Perform a look-up table warp for packed RGB ([rgbrgbrgb...]) images.
 * The LUTs must be float images of the same size as source image.
 * The source value x_s is determined from destination (x_d,y_d) through lut_x
 * and y_s is determined from lut_y:
   \code
   x_s = lut_x[y_d][x_d];
   y_s = lut_y[y_d][x_d];
   \endcode

 * \param src   source image (w*3 by h)
 * \param dst   destination image (w*3 by h)
 * \param w     width
 * \param h     height
 * \param lut_x LUT for x
 * \param lut_y LUT for y
 */
inline void db_WarpImageLutFast_rgb(const unsigned char * const * src, unsigned char ** dst, int w, int h,
                                  const float * const * lut_x, const float * const * lut_y)
{
    assert(src && dst);
    int xd=0, yd=0;

    for ( int i = 0; i < w; ++i )
        for ( int j = 0; j < h; ++j )
        {
            xd = static_cast<unsigned int>(lut_x[j][i]);
            yd = static_cast<unsigned int>(lut_y[j][i]);
            if ( xd >= w || yd >= h ||
                 xd < 0 || yd < 0)
            {
                dst[j][3*i  ] = 0;
                dst[j][3*i+1] = 0;
                dst[j][3*i+2] = 0;
            }
            else
            {
                dst[j][3*i  ] = src[yd][3*xd  ];
                dst[j][3*i+1] = src[yd][3*xd+1];
                dst[j][3*i+2] = src[yd][3*xd+2];
            }
        }
}

inline unsigned char db_BilinearInterpolationRGB(double y, double x, const unsigned char * const * v, int offset)
{
         int floor_x=(int) x;
         int floor_y=(int) y;

         int ceil_x=floor_x+1;
         int ceil_y=floor_y+1;

         unsigned char f00 = v[floor_y][3*floor_x+offset];
         unsigned char f01 = v[floor_y][3*ceil_x+offset];
         unsigned char f10 = v[ceil_y][3*floor_x+offset];
         unsigned char f11 = v[ceil_y][3*ceil_x+offset];

         double xl = x-floor_x;
         double yl = y-floor_y;

         return (unsigned char)(f00*(1-yl)*(1-xl) + f10*yl*(1-xl) + f01*(1-yl)*xl + f11*yl*xl);
}

inline void db_WarpImageLutBilinear_rgb(const unsigned char * const * src, unsigned char ** dst, int w, int h,
                                  const float * const * lut_x, const float * const * lut_y)
{
    assert(src && dst);
    double xd=0.0, yd=0.0;

    for ( int i = 0; i < w; ++i )
        for ( int j = 0; j < h; ++j )
        {
            xd = static_cast<double>(lut_x[j][i]);
            yd = static_cast<double>(lut_y[j][i]);
            if ( xd > w-2 || yd > h-2 ||
                 xd < 0.0 || yd < 0.0)
            {
                dst[j][3*i  ] = 0;
                dst[j][3*i+1] = 0;
                dst[j][3*i+2] = 0;
            }
            else
            {
                dst[j][3*i  ] = db_BilinearInterpolationRGB(yd,xd,src,0);
                dst[j][3*i+1] = db_BilinearInterpolationRGB(yd,xd,src,1);
                dst[j][3*i+2] = db_BilinearInterpolationRGB(yd,xd,src,2);
            }
        }
}

inline double SquaredInhomogenousHomographyError(double y[3],double H[9],double x[3]){
    double x0,x1,x2,mult;
    double sd;

    x0=H[0]*x[0]+H[1]*x[1]+H[2];
    x1=H[3]*x[0]+H[4]*x[1]+H[5];
    x2=H[6]*x[0]+H[7]*x[1]+H[8];
    mult=1.0/((x2!=0.0)?x2:1.0);
    sd=(y[0]-x0*mult)*(y[0]-x0*mult)+(y[1]-x1*mult)*(y[1]-x1*mult);

    return(sd);
}


// functions related to profiling
#if PROFILE

/* return current time in milliseconds */
static double
now_ms(void)
{
    //struct timespec res;
    struct timeval res;
    //clock_gettime(CLOCK_REALTIME, &res);
    gettimeofday(&res, NULL);
    return 1000.0*res.tv_sec + (double)res.tv_usec/1e3;
}

#endif
