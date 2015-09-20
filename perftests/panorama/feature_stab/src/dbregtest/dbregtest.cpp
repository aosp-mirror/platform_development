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

// $Id: dbregtest.cpp,v 1.24 2011/06/17 14:04:33 mbansal Exp $
#include "stdafx.h"
#include "PgmImage.h"
#include "../dbreg/dbreg.h"
#include "../dbreg/dbstabsmooth.h"
#include <db_utilities_camera.h>

#include <iostream>
#include <iomanip>

#if PROFILE
    #include <sys/time.h>
#endif


using namespace std;

const int DEFAULT_NR_CORNERS=500;
const double DEFAULT_MAX_DISPARITY=0.2;
const int DEFAULT_MOTION_MODEL=DB_HOMOGRAPHY_TYPE_AFFINE;
//const int DEFAULT_MOTION_MODEL=DB_HOMOGRAPHY_TYPE_R_T;
//const int DEFAULT_MOTION_MODEL=DB_HOMOGRAPHY_TYPE_TRANSLATION;
const bool DEFAULT_QUARTER_RESOLUTION=false;
const unsigned int DEFAULT_REFERENCE_UPDATE_PERIOD=3;
const bool DEFAULT_DO_MOTION_SMOOTHING = false;
const double DEFAULT_MOTION_SMOOTHING_GAIN = 0.75;
const bool DEFAULT_LINEAR_POLISH = false;
const int DEFAULT_MAX_ITERATIONS = 10;

void usage(string name) {

  const char *helpmsg[] = {
    "Function: point-based frame to reference registration.",
    "  -m [rt,a,p]  : motion model, rt = rotation+translation, a = affine (default = affine).",
    "  -c <int>   : number of corners (default 1000).",
    "  -d <double>: search disparity as portion of image size (default 0.1).",
    "  -q         : quarter the image resolution (i.e. half of each dimension) (default on)",
    "  -r <int>   : the period (in nr of frames) for reference frame updates (default = 5)",
    "  -s <0/1>   : motion smoothing (1 activates motion smoothing, 0 turns it off - default value = 1)",
    "  -g <double>: motion smoothing gain, only used if smoothing is on (default value =0.75)",
    NULL
  };

  cerr << "Usage: " << name << " [options] image_list.txt" << endl;

  const char **p = helpmsg;

  while (*p)
  {
    cerr << *p++ << endl;
  }
}

void parse_cmd_line(stringstream& cmdline,
            const int argc,
            const string& progname,
            string& image_list_file_name,
            int& nr_corners,
            double& max_disparity,
            int& motion_model_type,
            bool& quarter_resolution,
            unsigned int& reference_update_period,
            bool& do_motion_smoothing,
            double& motion_smoothing_gain
            );

int main(int argc, char* argv[])
{
  int    nr_corners = DEFAULT_NR_CORNERS;
  double max_disparity = DEFAULT_MAX_DISPARITY;
  int    motion_model_type = DEFAULT_MOTION_MODEL;
  bool   quarter_resolution = DEFAULT_QUARTER_RESOLUTION;

  unsigned int reference_update_period = DEFAULT_REFERENCE_UPDATE_PERIOD;

  bool   do_motion_smoothing = DEFAULT_DO_MOTION_SMOOTHING;
  double motion_smoothing_gain = DEFAULT_MOTION_SMOOTHING_GAIN;
  const bool DEFAULT_USE_SMALLER_MATCHING_WINDOW = true;

  int default_nr_samples = DB_DEFAULT_NR_SAMPLES/5;

  bool   use_smaller_matching_window = DEFAULT_USE_SMALLER_MATCHING_WINDOW;


  bool   linear_polish = DEFAULT_LINEAR_POLISH;

  if (argc < 2) {
    usage(argv[0]);
    exit(1);
  }

  stringstream cmdline;
  string progname(argv[0]);
  string image_list_file_name;

#if PROFILE
  timeval ts1, ts2, ts3, ts4;
#endif

  // put the options and image list file name into the cmdline stringstream
  for (int c = 1; c < argc; c++)
  {
    cmdline << argv[c] << " ";
  }

  parse_cmd_line(cmdline, argc, progname, image_list_file_name, nr_corners, max_disparity, motion_model_type,quarter_resolution,reference_update_period,do_motion_smoothing,motion_smoothing_gain);

  ifstream in(image_list_file_name.c_str(),ios::in);

  if ( !in.is_open() )
  {
    cerr << "Could not open file " << image_list_file_name << ".  Exiting" << endl;

    return false;
  }

  // feature-based image registration class:
  db_FrameToReferenceRegistration reg;
//  db_StabilizationSmoother stab_smoother;

  // input file name:
  string file_name;

  // look-up tables for image warping:
  float ** lut_x = NULL, **lut_y = NULL;

  // if the images are color, the input is saved in color_ref:
  PgmImage color_ref(0,0);

  // image width, height:
  int w,h;

  int frame_number = 0;

  while ( !in.eof() )
  {
    getline(in,file_name);

    PgmImage ref(file_name);

    if ( ref.GetDataPointer() == NULL )
    {
      cerr << "Could not open image" << file_name << ". Exiting." << endl;
      return -1;
    }

    cout << ref << endl;

    // color format:
    int format = ref.GetFormat();

    // is the input image color?:
    bool color = format == PgmImage::PGM_BINARY_PIXMAP;

    w = ref.GetWidth();
    h = ref.GetHeight();

    if ( !reg.Initialized() )
    {
      reg.Init(w,h,motion_model_type,DEFAULT_MAX_ITERATIONS,linear_polish,quarter_resolution,DB_POINT_STANDARDDEV,reference_update_period,do_motion_smoothing,motion_smoothing_gain,default_nr_samples,DB_DEFAULT_CHUNK_SIZE,nr_corners,max_disparity,use_smaller_matching_window);
      lut_x = db_AllocImage_f(w,h);
      lut_y = db_AllocImage_f(w,h);

    }

    if ( color )
    {
      // save the color image:
      color_ref = ref;
    }

    // make a grayscale image:
    ref.ConvertToGray();

    // compute the homography:
    double H[9],Hinv[9];
    db_Identity3x3(Hinv);
    db_Identity3x3(H);

    bool force_reference = false;

#if PROFILE
    gettimeofday(&ts1, NULL);
#endif

    reg.AddFrame(ref.GetRowPointers(),H,false,false);
    cout << reg.profile_string << std::endl;

#if PROFILE
    gettimeofday(&ts2, NULL);

    double elapsedTime = (ts2.tv_sec - ts1.tv_sec)*1000.0; // sec to ms
    elapsedTime += (ts2.tv_usec - ts1.tv_usec)/1000.0; // us to ms
    cout <<"\nelapsedTime for Reg<< "<<elapsedTime<<" ms >>>>>>>>>>>>>\n";
#endif

    if (frame_number == 0)
    {
      reg.UpdateReference(ref.GetRowPointers());
    }


    //std::vector<int> &inlier_indices = reg.GetInliers();
    int *inlier_indices = reg.GetInliers();
    int num_inlier_indices = reg.GetNrInliers();
    printf("[%d] #Inliers = %d\n",frame_number,num_inlier_indices);

    reg.Get_H_dref_to_ins(H);

    db_GenerateHomographyLut(lut_x,lut_y,w,h,H);

    // create a new image and warp:
    PgmImage warped(w,h,format);

#if PROFILE
    gettimeofday(&ts3, NULL);
#endif

    if ( color )
      db_WarpImageLutBilinear_rgb(color_ref.GetRowPointers(),warped.GetRowPointers(),w,h,lut_x,lut_y);
    else
      db_WarpImageLut_u(ref.GetRowPointers(),warped.GetRowPointers(),w,h,lut_x,lut_y,DB_WARP_FAST);

#if PROFILE
    gettimeofday(&ts4, NULL);
    elapsedTime = (ts4.tv_sec - ts3.tv_sec)*1000.0; // sec to ms
    elapsedTime += (ts4.tv_usec - ts3.tv_usec)/1000.0;     // us to ms
    cout <<"\nelapsedTime for Warp <<"<<elapsedTime<<" ms >>>>>>>>>>>>>\n";
#endif

    // write aligned image: name is aligned_<corresponding input file name>
    stringstream s;
    s << "aligned_" << file_name;
    warped.WritePGM(s.str());

    /*
    // Get the reference and inspection corners to write to file
    double *ref_corners = reg.GetRefCorners();
    double *ins_corners = reg.GetInsCorners();

    // get the image file name (without extension), so we
    // can generate the corresponding filenames for matches
    // and inliers
    string file_name_root(file_name.substr(0,file_name.rfind(".")));

    // write matches to file
    s.str(string(""));
    s << "Matches_" << file_name_root << ".txt";

    ofstream  match_file(s.str().c_str());

    for (int i = 0; i < reg.GetNrMatches(); i++)
    {
      match_file << ref_corners[3*i] << " " << ref_corners[3*i+1] << " " << ins_corners[3*i] << " " << ins_corners[3*i+1] << endl;
    }

    match_file.close();

    // write the inlier matches to file
    s.str(string(""));
    s << "InlierMatches_" << file_name_root << ".txt";

    ofstream inlier_match_file(s.str().c_str());

    for(int i=0; i<num_inlier_indices; i++)
    {
      int k = inlier_indices[i];
      inlier_match_file << ref_corners[3*k] << " "
            << ref_corners[3*k+1] << " "
            << ins_corners[3*k] << " "
            << ins_corners[3*k+1] << endl;
    }
    inlier_match_file.close();
    */

    frame_number++;
  }

  if ( reg.Initialized() )
  {
    db_FreeImage_f(lut_x,h);
    db_FreeImage_f(lut_y,h);
  }

  return 0;
}

void parse_cmd_line(stringstream& cmdline,
            const int argc,
            const string& progname,
            string& image_list_file_name,
            int& nr_corners,
            double& max_disparity,
            int& motion_model_type,
            bool& quarter_resolution,
            unsigned int& reference_update_period,
            bool& do_motion_smoothing,
            double& motion_smoothing_gain)
{
  // for counting down the parsed arguments.
  int c = argc;

  // a holder
  string token;

  while (cmdline >> token)
  {
    --c;

    int pos = token.find("-");

    if (pos == 0)
    {
      switch (token[1])
      {
      case 'm':
    --c; cmdline >> token;
    if (token.compare("rt") == 0)
    {
      motion_model_type = DB_HOMOGRAPHY_TYPE_R_T;
    }
    else if (token.compare("a") == 0)
    {
      motion_model_type = DB_HOMOGRAPHY_TYPE_AFFINE;
    }
    else if (token.compare("p") == 0)
    {
      motion_model_type = DB_HOMOGRAPHY_TYPE_PROJECTIVE;
    }
    else
    {
      usage(progname);
      exit(1);
    }
    break;
      case 'c':
    --c; cmdline >> nr_corners;
    break;
      case 'd':
    --c; cmdline >> max_disparity;
    break;
      case 'q':
    quarter_resolution = true;
    break;
      case 'r':
    --c; cmdline >> reference_update_period;
    break;
      case 's':
    --c; cmdline >> do_motion_smoothing;
    break;
      case 'g':
    --c; cmdline >> motion_smoothing_gain;
    break;
      default:
    cerr << progname << "illegal option " << token << endl;
      case 'h':
    usage(progname);
    exit(1);
    break;
      }
    }
    else
    {
      if (c != 1)
      {
    usage(progname);
    exit(1);
      }
      else
      {
    --c;
    image_list_file_name = token;
      }
    }
  }

  if (c != 0)
  {
    usage(progname);
    exit(1);
  }
}

