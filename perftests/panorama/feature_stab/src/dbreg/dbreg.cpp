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

// $Id: dbreg.cpp,v 1.31 2011/06/17 14:04:32 mbansal Exp $
#include "dbreg.h"
#include <string.h>
#include <stdio.h>


#if PROFILE
#endif

//#include <iostream>

db_FrameToReferenceRegistration::db_FrameToReferenceRegistration() :
  m_initialized(false),m_nr_matches(0),m_over_allocation(256),m_nr_bins(20),m_max_cost_pix(30), m_quarter_resolution(false)
{
  m_reference_image = NULL;
  m_aligned_ins_image = NULL;

  m_quarter_res_image = NULL;
  m_horz_smooth_subsample_image = NULL;

  m_x_corners_ref = NULL;
  m_y_corners_ref = NULL;

  m_x_corners_ins = NULL;
  m_y_corners_ins = NULL;

  m_match_index_ref = NULL;
  m_match_index_ins = NULL;

  m_inlier_indices = NULL;

  m_num_inlier_indices = 0;

  m_temp_double = NULL;
  m_temp_int = NULL;

  m_corners_ref = NULL;
  m_corners_ins = NULL;

  m_sq_cost = NULL;
  m_cost_histogram = NULL;

  profile_string = NULL;

  db_Identity3x3(m_K);
  db_Identity3x3(m_H_ref_to_ins);
  db_Identity3x3(m_H_dref_to_ref);

  m_sq_cost_computed = false;
  m_reference_set = false;

  m_reference_update_period = 0;
  m_nr_frames_processed = 0;

  return;
}

db_FrameToReferenceRegistration::~db_FrameToReferenceRegistration()
{
  Clean();
}

void db_FrameToReferenceRegistration::Clean()
{
  if ( m_reference_image )
    db_FreeImage_u(m_reference_image,m_im_height);

  if ( m_aligned_ins_image )
    db_FreeImage_u(m_aligned_ins_image,m_im_height);

  if ( m_quarter_res_image )
  {
    db_FreeImage_u(m_quarter_res_image, m_im_height);
  }

  if ( m_horz_smooth_subsample_image )
  {
    db_FreeImage_u(m_horz_smooth_subsample_image, m_im_height*2);
  }

  delete [] m_x_corners_ref;
  delete [] m_y_corners_ref;

  delete [] m_x_corners_ins;
  delete [] m_y_corners_ins;

  delete [] m_match_index_ref;
  delete [] m_match_index_ins;

  delete [] m_temp_double;
  delete [] m_temp_int;

  delete [] m_corners_ref;
  delete [] m_corners_ins;

  delete [] m_sq_cost;
  delete [] m_cost_histogram;

  delete [] m_inlier_indices;

  if(profile_string)
    delete [] profile_string;

  m_reference_image = NULL;
  m_aligned_ins_image = NULL;

  m_quarter_res_image = NULL;
  m_horz_smooth_subsample_image = NULL;

  m_x_corners_ref = NULL;
  m_y_corners_ref = NULL;

  m_x_corners_ins = NULL;
  m_y_corners_ins = NULL;

  m_match_index_ref = NULL;
  m_match_index_ins = NULL;

  m_inlier_indices = NULL;

  m_temp_double = NULL;
  m_temp_int = NULL;

  m_corners_ref = NULL;
  m_corners_ins = NULL;

  m_sq_cost = NULL;
  m_cost_histogram = NULL;
}

void db_FrameToReferenceRegistration::Init(int width, int height,
                       int    homography_type,
                       int    max_iterations,
                       bool   linear_polish,
                       bool   quarter_resolution,
                       double scale,
                       unsigned int reference_update_period,
                       bool   do_motion_smoothing,
                       double motion_smoothing_gain,
                       int    nr_samples,
                       int    chunk_size,
                       int    cd_target_nr_corners,
                       double cm_max_disparity,
                           bool   cm_use_smaller_matching_window,
                       int    cd_nr_horz_blocks,
                       int    cd_nr_vert_blocks
                       )
{
  Clean();

  m_reference_update_period = reference_update_period;
  m_nr_frames_processed = 0;

  m_do_motion_smoothing = do_motion_smoothing;
  m_motion_smoothing_gain = motion_smoothing_gain;

  m_stab_smoother.setSmoothingFactor(m_motion_smoothing_gain);

  m_quarter_resolution = quarter_resolution;

  profile_string = new char[10240];

  if (m_quarter_resolution == true)
  {
    width = width/2;
    height = height/2;

    m_horz_smooth_subsample_image = db_AllocImage_u(width,height*2,m_over_allocation);
    m_quarter_res_image = db_AllocImage_u(width,height,m_over_allocation);
  }

  m_im_width = width;
  m_im_height = height;

  double temp[9];
  db_Approx3DCalMat(m_K,temp,m_im_width,m_im_height);

  m_homography_type = homography_type;
  m_max_iterations = max_iterations;
  m_scale = 2/(m_K[0]+m_K[4]);
  m_nr_samples = nr_samples;
  m_chunk_size = chunk_size;

  double outlier_t1 = 5.0;

  m_outlier_t2 = outlier_t1*outlier_t1;//*m_scale*m_scale;

  m_current_is_reference = false;

  m_linear_polish = linear_polish;

  m_reference_image = db_AllocImage_u(m_im_width,m_im_height,m_over_allocation);
  m_aligned_ins_image = db_AllocImage_u(m_im_width,m_im_height,m_over_allocation);

  // initialize feature detection and matching:
  //m_max_nr_corners = m_cd.Init(m_im_width,m_im_height,cd_target_nr_corners,cd_nr_horz_blocks,cd_nr_vert_blocks,0.0,0.0);
  m_max_nr_corners = m_cd.Init(m_im_width,m_im_height,cd_target_nr_corners,cd_nr_horz_blocks,cd_nr_vert_blocks,DB_DEFAULT_ABS_CORNER_THRESHOLD/500.0,0.0);

    int use_21 = 0;
  m_max_nr_matches = m_cm.Init(m_im_width,m_im_height,cm_max_disparity,m_max_nr_corners,DB_DEFAULT_NO_DISPARITY,cm_use_smaller_matching_window,use_21);

  // allocate space for corner feature locations for reference and inspection images:
  m_x_corners_ref = new double [m_max_nr_corners];
  m_y_corners_ref = new double [m_max_nr_corners];

  m_x_corners_ins = new double [m_max_nr_corners];
  m_y_corners_ins = new double [m_max_nr_corners];

  // allocate space for match indices:
  m_match_index_ref = new int [m_max_nr_matches];
  m_match_index_ins = new int [m_max_nr_matches];

  m_temp_double = new double [12*DB_DEFAULT_NR_SAMPLES+10*m_max_nr_matches];
  m_temp_int = new int [db_maxi(DB_DEFAULT_NR_SAMPLES,m_max_nr_matches)];

  // allocate space for homogenous image points:
  m_corners_ref = new double [3*m_max_nr_corners];
  m_corners_ins = new double [3*m_max_nr_corners];

  // allocate cost array and histogram:
  m_sq_cost = new double [m_max_nr_matches];
  m_cost_histogram = new int [m_nr_bins];

  // reserve array:
  //m_inlier_indices.reserve(m_max_nr_matches);
  m_inlier_indices = new int[m_max_nr_matches];

  m_initialized = true;

  m_max_inlier_count = 0;
}


#define MB 0
// Save the reference image, detect features and update the dref-to-ref transformation
int db_FrameToReferenceRegistration::UpdateReference(const unsigned char * const * im, bool subsample, bool detect_corners)
{
  double temp[9];
  db_Multiply3x3_3x3(temp,m_H_dref_to_ref,m_H_ref_to_ins);
  db_Copy9(m_H_dref_to_ref,temp);

  const unsigned char * const * imptr = im;

  if (m_quarter_resolution && subsample)
  {
    GenerateQuarterResImage(im);
    imptr = m_quarter_res_image;
  }

  // save the reference image, detect features and quit
  db_CopyImage_u(m_reference_image,imptr,m_im_width,m_im_height,m_over_allocation);

  if(detect_corners)
  {
    #if MB
    m_cd.DetectCorners(imptr, m_x_corners_ref,m_y_corners_ref,&m_nr_corners_ref);
    int nr = 0;
    for(int k=0; k<m_nr_corners_ref; k++)
    {
        if(m_x_corners_ref[k]>m_im_width/3)
        {
            m_x_corners_ref[nr] = m_x_corners_ref[k];
            m_y_corners_ref[nr] = m_y_corners_ref[k];
            nr++;
        }

    }
    m_nr_corners_ref = nr;
    #else
    m_cd.DetectCorners(imptr, m_x_corners_ref,m_y_corners_ref,&m_nr_corners_ref);
    #endif
  }
  else
  {
    m_nr_corners_ref = m_nr_corners_ins;

    for(int k=0; k<m_nr_corners_ins; k++)
    {
        m_x_corners_ref[k] = m_x_corners_ins[k];
        m_y_corners_ref[k] = m_y_corners_ins[k];
    }

  }

  db_Identity3x3(m_H_ref_to_ins);

  m_max_inlier_count = 0;   // Reset to 0 as no inliers seen until now
  m_sq_cost_computed = false;
  m_reference_set = true;
  m_current_is_reference = true;
  return 1;
}

void db_FrameToReferenceRegistration::Get_H_dref_to_ref(double H[9])
{
  db_Copy9(H,m_H_dref_to_ref);
}

void db_FrameToReferenceRegistration::Get_H_dref_to_ins(double H[9])
{
  db_Multiply3x3_3x3(H,m_H_dref_to_ref,m_H_ref_to_ins);
}

void db_FrameToReferenceRegistration::Set_H_dref_to_ins(double H[9])
{
    double H_ins_to_ref[9];

    db_Identity3x3(H_ins_to_ref);   // Ensure it has proper values
    db_InvertAffineTransform(H_ins_to_ref,m_H_ref_to_ins);  // Invert to get ins to ref
    db_Multiply3x3_3x3(m_H_dref_to_ref,H,H_ins_to_ref); // Update dref to ref using the input H from dref to ins
}


void db_FrameToReferenceRegistration::ResetDisplayReference()
{
  db_Identity3x3(m_H_dref_to_ref);
}

bool db_FrameToReferenceRegistration::NeedReferenceUpdate()
{
  // If less than 50% of the starting number of inliers left, then its time to update the reference.
  if(m_max_inlier_count>0 && float(m_num_inlier_indices)/float(m_max_inlier_count)<0.5)
    return true;
  else
    return false;
}

int db_FrameToReferenceRegistration::AddFrame(const unsigned char * const * im, double H[9],bool force_reference,bool prewarp)
{
  m_current_is_reference = false;
  if(!m_reference_set || force_reference)
    {
      db_Identity3x3(m_H_ref_to_ins);
      db_Copy9(H,m_H_ref_to_ins);

      UpdateReference(im,true,true);
      return 0;
    }

  const unsigned char * const * imptr = im;

  if (m_quarter_resolution)
  {
    if (m_quarter_res_image)
    {
      GenerateQuarterResImage(im);
    }

    imptr = (const unsigned char * const* )m_quarter_res_image;
  }

  double H_last[9];
  db_Copy9(H_last,m_H_ref_to_ins);
  db_Identity3x3(m_H_ref_to_ins);

  m_sq_cost_computed = false;

  // detect corners on inspection image and match to reference image features:s

  // @jke - Adding code to time the functions.  TODO: Remove after test
#if PROFILE
  double iTimer1, iTimer2;
  char str[255];
  strcpy(profile_string,"\n");
  sprintf(str,"[%dx%d] %p\n",m_im_width,m_im_height,im);
  strcat(profile_string, str);
#endif

  // @jke - Adding code to time the functions.  TODO: Remove after test
#if PROFILE
  iTimer1 = now_ms();
#endif
  m_cd.DetectCorners(imptr, m_x_corners_ins,m_y_corners_ins,&m_nr_corners_ins);
  // @jke - Adding code to time the functions.  TODO: Remove after test
# if PROFILE
  iTimer2 = now_ms();
  double elapsedTimeCorner = iTimer2 - iTimer1;
  sprintf(str,"Corner Detection [%d corners] = %g ms\n",m_nr_corners_ins, elapsedTimeCorner);
  strcat(profile_string, str);
#endif

  // @jke - Adding code to time the functions.  TODO: Remove after test
#if PROFILE
  iTimer1 = now_ms();
#endif
    if(prewarp)
  m_cm.Match(m_reference_image,imptr,m_x_corners_ref,m_y_corners_ref,m_nr_corners_ref,
         m_x_corners_ins,m_y_corners_ins,m_nr_corners_ins,
         m_match_index_ref,m_match_index_ins,&m_nr_matches,H,0);
    else
  m_cm.Match(m_reference_image,imptr,m_x_corners_ref,m_y_corners_ref,m_nr_corners_ref,
         m_x_corners_ins,m_y_corners_ins,m_nr_corners_ins,
         m_match_index_ref,m_match_index_ins,&m_nr_matches);
  // @jke - Adding code to time the functions.  TODO: Remove after test
# if PROFILE
  iTimer2 = now_ms();
  double elapsedTimeMatch = iTimer2 - iTimer1;
  sprintf(str,"Matching [%d] = %g ms\n",m_nr_matches,elapsedTimeMatch);
  strcat(profile_string, str);
#endif


  // copy out matching features:
  for ( int i = 0; i < m_nr_matches; ++i )
    {
      int offset = 3*i;
      m_corners_ref[offset  ] = m_x_corners_ref[m_match_index_ref[i]];
      m_corners_ref[offset+1] = m_y_corners_ref[m_match_index_ref[i]];
      m_corners_ref[offset+2] = 1.0;

      m_corners_ins[offset  ] = m_x_corners_ins[m_match_index_ins[i]];
      m_corners_ins[offset+1] = m_y_corners_ins[m_match_index_ins[i]];
      m_corners_ins[offset+2] = 1.0;
    }

  // @jke - Adding code to time the functions.  TODO: Remove after test
#if PROFILE
  iTimer1 = now_ms();
#endif
  // perform the alignment:
  db_RobImageHomography(m_H_ref_to_ins, m_corners_ref, m_corners_ins, m_nr_matches, m_K, m_K, m_temp_double, m_temp_int,
            m_homography_type,NULL,m_max_iterations,m_max_nr_matches,m_scale,
            m_nr_samples, m_chunk_size);
  // @jke - Adding code to time the functions.  TODO: Remove after test
# if PROFILE
  iTimer2 = now_ms();
  double elapsedTimeHomography = iTimer2 - iTimer1;
  sprintf(str,"Homography = %g ms\n",elapsedTimeHomography);
  strcat(profile_string, str);
#endif


  SetOutlierThreshold();

  // Compute the inliers for the db compute m_H_ref_to_ins
  ComputeInliers(m_H_ref_to_ins);

  // Update the max inlier count
  m_max_inlier_count = (m_max_inlier_count > m_num_inlier_indices)?m_max_inlier_count:m_num_inlier_indices;

  // Fit a least-squares model to just the inliers and put it in m_H_ref_to_ins
  if(m_linear_polish)
    Polish(m_inlier_indices, m_num_inlier_indices);

  if (m_quarter_resolution)
  {
    m_H_ref_to_ins[2] *= 2.0;
    m_H_ref_to_ins[5] *= 2.0;
  }

#if PROFILE
  sprintf(str,"#Inliers = %d \n",m_num_inlier_indices);
  strcat(profile_string, str);
#endif
/*
  ///// CHECK IF CURRENT TRANSFORMATION GOOD OR BAD ////
  ///// IF BAD, then update reference to the last correctly aligned inspection frame;
  if(m_num_inlier_indices<5)//0.9*m_nr_matches || m_nr_matches < 20)
  {
    db_Copy9(m_H_ref_to_ins,H_last);
    UpdateReference(imptr,false);
//  UpdateReference(m_aligned_ins_image,false);
  }
  else
  {
  ///// IF GOOD, then update the last correctly aligned inspection frame to be this;
  //db_CopyImage_u(m_aligned_ins_image,imptr,m_im_width,m_im_height,m_over_allocation);
*/
  if(m_do_motion_smoothing)
    SmoothMotion();

   // Disable debug printing
   // db_PrintDoubleMatrix(m_H_ref_to_ins,3,3);

  db_Copy9(H, m_H_ref_to_ins);

  m_nr_frames_processed++;
{
  if ( (m_nr_frames_processed % m_reference_update_period) == 0 )
  {
    //UpdateReference(imptr,false, false);

    #if MB
    UpdateReference(imptr,false, true);
    #else
    UpdateReference(imptr,false, false);
    #endif
  }


  }



  return 1;
}

//void db_FrameToReferenceRegistration::ComputeInliers(double H[9],std::vector<int> &inlier_indices)
void db_FrameToReferenceRegistration::ComputeInliers(double H[9])
{
  double totnummatches = m_nr_matches;
  int inliercount=0;

  m_num_inlier_indices = 0;
//  inlier_indices.clear();

  for(int c=0; c < totnummatches; c++ )
    {
      if (m_sq_cost[c] <= m_outlier_t2)
    {
      m_inlier_indices[inliercount] = c;
      inliercount++;
    }
    }

  m_num_inlier_indices = inliercount;
  double frac=inliercount/totnummatches;
}

//void db_FrameToReferenceRegistration::Polish(std::vector<int> &inlier_indices)
void db_FrameToReferenceRegistration::Polish(int *inlier_indices, int &num_inlier_indices)
{
  db_Zero(m_polish_C,36);
  db_Zero(m_polish_D,6);
  for (int i=0;i<num_inlier_indices;i++)
    {
      int j = 3*inlier_indices[i];
      m_polish_C[0]+=m_corners_ref[j]*m_corners_ref[j];
      m_polish_C[1]+=m_corners_ref[j]*m_corners_ref[j+1];
      m_polish_C[2]+=m_corners_ref[j];
      m_polish_C[7]+=m_corners_ref[j+1]*m_corners_ref[j+1];
      m_polish_C[8]+=m_corners_ref[j+1];
      m_polish_C[14]+=1;
      m_polish_D[0]+=m_corners_ref[j]*m_corners_ins[j];
      m_polish_D[1]+=m_corners_ref[j+1]*m_corners_ins[j];
      m_polish_D[2]+=m_corners_ins[j];
      m_polish_D[3]+=m_corners_ref[j]*m_corners_ins[j+1];
      m_polish_D[4]+=m_corners_ref[j+1]*m_corners_ins[j+1];
      m_polish_D[5]+=m_corners_ins[j+1];
    }

  double a=db_maxd(m_polish_C[0],m_polish_C[7]);
  m_polish_C[0]/=a; m_polish_C[1]/=a;   m_polish_C[2]/=a;
  m_polish_C[7]/=a; m_polish_C[8]/=a; m_polish_C[14]/=a;

  m_polish_D[0]/=a; m_polish_D[1]/=a;   m_polish_D[2]/=a;
  m_polish_D[3]/=a; m_polish_D[4]/=a;   m_polish_D[5]/=a;


  m_polish_C[6]=m_polish_C[1];
  m_polish_C[12]=m_polish_C[2];
  m_polish_C[13]=m_polish_C[8];

  m_polish_C[21]=m_polish_C[0]; m_polish_C[22]=m_polish_C[1]; m_polish_C[23]=m_polish_C[2];
  m_polish_C[28]=m_polish_C[7]; m_polish_C[29]=m_polish_C[8];
  m_polish_C[35]=m_polish_C[14];


  double d[6];
  db_CholeskyDecomp6x6(m_polish_C,d);
  db_CholeskyBacksub6x6(m_H_ref_to_ins,m_polish_C,d,m_polish_D);
}

void db_FrameToReferenceRegistration::EstimateSecondaryModel(double H[9])
{
  /*      if ( m_current_is_reference )
      {
      db_Identity3x3(H);
      return;
      }
  */

  // select the outliers of the current model:
  SelectOutliers();

  // perform the alignment:
  db_RobImageHomography(m_H_ref_to_ins, m_corners_ref, m_corners_ins, m_nr_matches, m_K, m_K, m_temp_double, m_temp_int,
            m_homography_type,NULL,m_max_iterations,m_max_nr_matches,m_scale,
            m_nr_samples, m_chunk_size);

  db_Copy9(H,m_H_ref_to_ins);
}

void db_FrameToReferenceRegistration::ComputeCostArray()
{
  if ( m_sq_cost_computed ) return;

  for( int c=0, k=0 ;c < m_nr_matches; c++, k=k+3)
    {
      m_sq_cost[c] = SquaredInhomogenousHomographyError(m_corners_ins+k,m_H_ref_to_ins,m_corners_ref+k);
    }

  m_sq_cost_computed = true;
}

void db_FrameToReferenceRegistration::SelectOutliers()
{
  int nr_outliers=0;

  ComputeCostArray();

  for(int c=0, k=0 ;c<m_nr_matches;c++,k=k+3)
    {
      if (m_sq_cost[c] > m_outlier_t2)
    {
      int offset = 3*nr_outliers++;
      db_Copy3(m_corners_ref+offset,m_corners_ref+k);
      db_Copy3(m_corners_ins+offset,m_corners_ins+k);
    }
    }

  m_nr_matches = nr_outliers;
}

void db_FrameToReferenceRegistration::ComputeCostHistogram()
{
  ComputeCostArray();

  for ( int b = 0; b < m_nr_bins; ++b )
    m_cost_histogram[b] = 0;

  for(int c = 0; c < m_nr_matches; c++)
    {
      double error = db_SafeSqrt(m_sq_cost[c]);
      int bin = (int)(error/m_max_cost_pix*m_nr_bins);
      if ( bin < m_nr_bins )
    m_cost_histogram[bin]++;
      else
    m_cost_histogram[m_nr_bins-1]++;
    }

/*
  for ( int i = 0; i < m_nr_bins; ++i )
    std::cout << m_cost_histogram[i] << " ";
  std::cout << std::endl;
*/
}

void db_FrameToReferenceRegistration::SetOutlierThreshold()
{
  ComputeCostHistogram();

  int i = 0, last=0;
  for (; i < m_nr_bins-1; ++i )
    {
      if ( last > m_cost_histogram[i] )
    break;
      last = m_cost_histogram[i];
    }

  //std::cout << "I " <<  i << std::endl;

  int max = m_cost_histogram[i];

  for (; i < m_nr_bins-1; ++i )
    {
      if ( m_cost_histogram[i] < (int)(0.1*max) )
    //if ( last < m_cost_histogram[i] )
    break;
      last = m_cost_histogram[i];
    }
  //std::cout << "J " <<  i << std::endl;

  m_outlier_t2 = db_sqr(i*m_max_cost_pix/m_nr_bins);

  //std::cout << "m_outlier_t2 " <<  m_outlier_t2 << std::endl;
}

void db_FrameToReferenceRegistration::SmoothMotion(void)
{
  VP_MOTION inmot,outmot;

  double H[9];

  Get_H_dref_to_ins(H);

      MXX(inmot) = H[0];
    MXY(inmot) = H[1];
    MXZ(inmot) = H[2];
    MXW(inmot) = 0.0;

    MYX(inmot) = H[3];
    MYY(inmot) = H[4];
    MYZ(inmot) = H[5];
    MYW(inmot) = 0.0;

    MZX(inmot) = H[6];
    MZY(inmot) = H[7];
    MZZ(inmot) = H[8];
    MZW(inmot) = 0.0;

    MWX(inmot) = 0.0;
    MWY(inmot) = 0.0;
    MWZ(inmot) = 0.0;
    MWW(inmot) = 1.0;

    inmot.type = VP_MOTION_AFFINE;

    int w = m_im_width;
    int h = m_im_height;

    if(m_quarter_resolution)
    {
    w = w*2;
    h = h*2;
    }

#if 0
    m_stab_smoother.smoothMotionAdaptive(w,h,&inmot,&outmot);
#else
    m_stab_smoother.smoothMotion(&inmot,&outmot);
#endif

    H[0] = MXX(outmot);
    H[1] = MXY(outmot);
    H[2] = MXZ(outmot);

    H[3] = MYX(outmot);
    H[4] = MYY(outmot);
    H[5] = MYZ(outmot);

    H[6] = MZX(outmot);
    H[7] = MZY(outmot);
    H[8] = MZZ(outmot);

    Set_H_dref_to_ins(H);
}

void db_FrameToReferenceRegistration::GenerateQuarterResImage(const unsigned char* const* im)
{
  int input_h = m_im_height*2;
  int input_w = m_im_width*2;

  for (int j = 0; j < input_h; j++)
  {
    const unsigned char* in_row_ptr = im[j];
    unsigned char* out_row_ptr = m_horz_smooth_subsample_image[j]+1;

    for (int i = 2; i < input_w-2; i += 2)
    {
      int smooth_val = (
            6*in_row_ptr[i] +
            ((in_row_ptr[i-1]+in_row_ptr[i+1])<<2) +
            in_row_ptr[i-2]+in_row_ptr[i+2]
            ) >> 4;
      *out_row_ptr++ = (unsigned char) smooth_val;

      if ( (smooth_val < 0) || (smooth_val > 255))
      {
        return;
      }

    }
  }

  for (int j = 2; j < input_h-2; j+=2)
  {

    unsigned char* in_row_ptr = m_horz_smooth_subsample_image[j];
    unsigned char* out_row_ptr = m_quarter_res_image[j/2];

    for (int i = 1; i < m_im_width-1; i++)
    {
      int smooth_val = (
            6*in_row_ptr[i] +
            ((in_row_ptr[i-m_im_width]+in_row_ptr[i+m_im_width]) << 2)+
            in_row_ptr[i-2*m_im_width]+in_row_ptr[i+2*m_im_width]
            ) >> 4;
      *out_row_ptr++ = (unsigned char)smooth_val;

      if ( (smooth_val < 0) || (smooth_val > 255))
      {
        return;
      }

    }
  }
}
