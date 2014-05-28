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

/*$Id: db_feature_matching.h,v 1.3 2011/06/17 14:03:30 mbansal Exp $*/

#ifndef DB_FEATURE_MATCHING_H
#define DB_FEATURE_MATCHING_H

/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup FeatureMatching Feature Matching
 */
#include "db_utilities.h"
#include "db_utilities_constants.h"

DB_API void db_SignedSquareNormCorr21x21_PreAlign_u(short *patch,const unsigned char * const *f_img,int x_f,int y_f,float *sum,float *recip);
DB_API void db_SignedSquareNormCorr11x11_PreAlign_u(short *patch,const unsigned char * const *f_img,int x_f,int y_f,float *sum,float *recip);
float db_SignedSquareNormCorr21x21Aligned_Post_s(const short *f_patch,const short *g_patch,float fsum_gsum,float f_recip_g_recip);
float db_SignedSquareNormCorr11x11Aligned_Post_s(const short *f_patch,const short *g_patch,float fsum_gsum,float f_recip_g_recip);

class db_PointInfo_f
{
public:
    /*Coordinates of point*/
    int x;
    int y;
    /*Id nr of point*/
    int id;
    /*Best match score*/
    double s;
    /*Best match candidate*/
    db_PointInfo_f *pir;
    /*Precomputed coefficients
    of image patch*/
    float sum;
    float recip;
    /*Pointer to patch layout*/
    const float *patch;
};

class db_Bucket_f
{
public:
    db_PointInfo_f *ptr;
    int nr;
};

class db_PointInfo_u
{
public:
    /*Coordinates of point*/
    int x;
    int y;
    /*Id nr of point*/
    int id;
    /*Best match score*/
    double s;
    /*Best match candidate*/
    db_PointInfo_u *pir;
    /*Precomputed coefficients
    of image patch*/
    float sum;
    float recip;
    /*Pointer to patch layout*/
    const short *patch;
};

class db_Bucket_u
{
public:
    db_PointInfo_u *ptr;
    int nr;
};
/*!
 * \class db_Matcher_f
 * \ingroup FeatureMatching
 * \brief Feature matcher for float images.
 *
 * Normalized correlation feature matcher for <b>float</b> images.
 * Correlation window size is constant and set to 11x11.
 * See \ref FeatureDetection to detect Harris corners.
 * Images are managed with functions in \ref LMImageBasicUtilities.
 */
class DB_API db_Matcher_f
{
public:
    db_Matcher_f();
    ~db_Matcher_f();

    /*!
     * Set parameters and pre-allocate memory. Return an upper bound
     * on the number of matches.
     * \param im_width          width
     * \param im_height         height
     * \param max_disparity     maximum distance (as fraction of image size) between matches
     * \param target_nr_corners maximum number of matches
     * \return maximum number of matches
     */
    unsigned long Init(int im_width,int im_height,
        double max_disparity=DB_DEFAULT_MAX_DISPARITY,
        int target_nr_corners=DB_DEFAULT_TARGET_NR_CORNERS);

    /*!
     * Match two sets of features.
     * If the prewarp H is not NULL it will be applied to the features
     * in the right image before matching.
     * Parameters id_l and id_r must point to arrays of size target_nr_corners
     * (returned by Init()).
     * The results of matching are in id_l and id_r.
     * Interpretaqtion of results: if id_l[i] = m and id_r[i] = n,
     * feature at (x_l[m],y_l[m]) matched to (x_r[n],y_r[n]).
     * \param l_img     left image
     * \param r_img     right image
     * \param x_l       left x coordinates of features
     * \param y_l       left y coordinates of features
     * \param nr_l      number of features in left image
     * \param x_r       right x coordinates of features
     * \param y_r       right y coordinates of features
     * \param nr_r      number of features in right image
     * \param id_l      indices of left features that matched
     * \param id_r      indices of right features that matched
     * \param nr_matches    number of features actually matched
     * \param H         image homography (prewarp) to be applied to right image features
     */
    void Match(const float * const *l_img,const float * const *r_img,
        const double *x_l,const double *y_l,int nr_l,const double *x_r,const double *y_r,int nr_r,
        int *id_l,int *id_r,int *nr_matches,const double H[9]=0);

protected:
    void Clean();

    int m_w,m_h,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,m_target;
    unsigned long m_kA,m_kB;
    db_Bucket_f **m_bp_l;
    db_Bucket_f **m_bp_r;
    float *m_patch_space,*m_aligned_patch_space;
};
/*!
 * \class db_Matcher_u
 * \ingroup FeatureMatching
 * \brief Feature matcher for byte images.
 *
 * Normalized correlation feature matcher for <b>byte</b> images.
 * Correlation window size is constant and set to 11x11.
 * See \ref FeatureDetection to detect Harris corners.
 * Images are managed with functions in \ref LMImageBasicUtilities.
 *
 * If the prewarp matrix H is supplied, the feature coordinates are warped by H before being placed in
 * appropriate buckets. If H is an affine transform and the "affine" parameter is set to 1 or 2,
 * then the correlation patches themselves are warped before being placed in the patch space.
 */
class DB_API db_Matcher_u
{
public:
    db_Matcher_u();

    int GetPatchSize(){return 11;};

    virtual ~db_Matcher_u();

    /*!
     Copy ctor duplicates settings.
     Memory not copied.
     */
    db_Matcher_u(const db_Matcher_u& cm);

    /*!
     Assignment optor duplicates settings
     Memory not copied.
     */
    db_Matcher_u& operator= (const db_Matcher_u& cm);

    /*!
     * Set parameters and pre-allocate memory. Return an upper bound
     * on the number of matches.
     * If max_disparity_v is DB_DEFAULT_NO_DISPARITY, look for matches
     * in a ellipse around a feature of radius max_disparity*im_width by max_disparity*im_height.
     * If max_disparity_v is specified, use a rectangle max_disparity*im_width by max_disparity_v*im_height.
     * \param im_width          width
     * \param im_height         height
     * \param max_disparity     maximum distance (as fraction of image size) between matches
     * \param target_nr_corners maximum number of matches
     * \param max_disparity_v   maximum vertical disparity (distance between matches)
     * \param use_smaller_matching_window   if set to true, uses a correlation window of 5x5 instead of the default 11x11
     * \return maximum number of matches
     */
    virtual unsigned long Init(int im_width,int im_height,
        double max_disparity=DB_DEFAULT_MAX_DISPARITY,
        int target_nr_corners=DB_DEFAULT_TARGET_NR_CORNERS,
        double max_disparity_v=DB_DEFAULT_NO_DISPARITY,
        bool use_smaller_matching_window=false, int use_21=0);

    /*!
     * Match two sets of features.
     * If the prewarp H is not NULL it will be applied to the features
     * in the right image before matching.
     * Parameters id_l and id_r must point to arrays of size target_nr_corners
     * (returned by Init()).
     * The results of matching are in id_l and id_r.
     * Interpretaqtion of results: if id_l[i] = m and id_r[i] = n,
     * feature at (x_l[m],y_l[m]) matched to (x_r[n],y_r[n]).
     * \param l_img     left image
     * \param r_img     right image
     * \param x_l       left x coordinates of features
     * \param y_l       left y coordinates of features
     * \param nr_l      number of features in left image
     * \param x_r       right x coordinates of features
     * \param y_r       right y coordinates of features
     * \param nr_r      number of features in right image
     * \param id_l      indices of left features that matched
     * \param id_r      indices of right features that matched
     * \param nr_matches    number of features actually matched
     * \param H         image homography (prewarp) to be applied to right image features
     * \param affine    prewarp the 11x11 patches by given affine transform. 0 means no warping,
                        1 means nearest neighbor, 2 means bilinear warping.
     */
    virtual void Match(const unsigned char * const *l_img,const unsigned char * const *r_img,
        const double *x_l,const double *y_l,int nr_l,const double *x_r,const double *y_r,int nr_r,
        int *id_l,int *id_r,int *nr_matches,const double H[9]=0,int affine=0);

    /*!
     * Checks if Init() was called.
     * \return 1 if Init() was called, 0 otherwise.
     */
    int IsAllocated();

protected:
    virtual void Clean();


    int m_w,m_h,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,m_target;
    unsigned long m_kA,m_kB;
    db_Bucket_u **m_bp_l;
    db_Bucket_u **m_bp_r;
    short *m_patch_space,*m_aligned_patch_space;

    double m_max_disparity, m_max_disparity_v;
    int m_rect_window;
    bool m_use_smaller_matching_window;
    int m_use_21;
};



#endif /*DB_FEATURE_MATCHING_H*/
