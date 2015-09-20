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

/*
#sourcefile  vp_motionmodel.h
#category    warp
#description general motion model for tranlation/affine/projective
#title       motion-model
#parentlink  hindex.html
*
* Copyright 1998 Sarnoff Corporation
* All Rights Reserved
*
* Modification History
*      Date: 02/13/98
*      Author: supuns
*      Shop Order: 15491 001
*              @(#) $Id: vp_motionmodel.h,v 1.4 2011/06/17 14:04:33 mbansal Exp $
*/

#ifndef VP_MOTIONMODEL_H
#define VP_MOTIONMODEL_H
#include <stdio.h>

#define         FALSE           0
#define         TRUE            1

#if 0 /* Moved mottomat.c and mattomot_d.c from vpmotion.h to vpcompat.h
     in order to remove otherwise unnecessary dependency of vpmotion,
     vpwarp, and newvpio on vpmath */
#ifndef VPMATH_H
#include "vpmath.h"
#endif
#endif

#if 0
#ifndef VP_WARP_H
#include "vp_warp.h"
#endif
#endif
/*

#htmlstart
# ===================================================================
#h 1 Introduction

  This defines a motion model that can describe translation,
  affine, and projective projective 3d and 3d view transforms.

  The main structure VP_MOTION contains a 16 parameter array (That
  can be considered as elements of a 4x4 matrix) and a type field
  which can be one of VP_MOTION_NONE,VP_MOTION_TRANSLATION,
  VP_MOTION_AFFINE, VP_MOTION_PROJECTIVE,VP_MOTION_PROJ_3D or
  VP_MOTION_VIEW_3D. (These are defined using enums with gaps of 10
  so that subsets of these motions that are still consistant can be
  added in between. Motion models that are inconsistant with this set
  should be added at the end so the routines can hadle them
  independently.

  The transformation VP_MOTION_NONE,VP_MOTION_TRANSLATION,
  VP_MOTION_AFFINE, VP_MOTION_PROJECTIVE, VP_MOTION_PROJ_3D and
  VP_MOTION_SEMI_PROJ_3D would map a point P={x,y,z,w} to a new point
  P'={x',y',z',w'} using a motion model M such that P'= M.par * P.
  Where M.par is thought of as  elements of a 4x4 matrix ordered row
  by row. The interpretation of all models except VP_MOTION_SEMI_PROJ_3D
  is taken to be mapping of a 3d point P"={x",y",z"} which is obtained
  from the normalization {x'/w',y'/w',z'/w'}. In the VP_MOTION_SEMI_PROJ_3D
  the mapping to a point P"={x",y",z"} is obtained from the normalization
  {x'/w',y'/w',z'}. All these motion models have the property that they
  can be inverted using 4x4 matrices. Except for the VP_MOTION_SEMI_PROJ_3D all
  other types can also be cascaded using 4x4 matrices.

  Specific macros and functions have been provided to handle 2d instances
  of these functions. As the parameter interpretations can change when adding
  new motion models it is HIGHLY RECOMMENDED that you use the macros MXX,MXY..
  ect. to interpret each motion component.
#pre
*/

/*
#endpre
# ===================================================================
#h 1 Typedef and Struct Declarations
#pre
*/

#define VP_MAX_MOTION_PAR 16

typedef double VP_PAR;
typedef VP_PAR VP_TRS[VP_MAX_MOTION_PAR];

/* Do not add any motion models before VP_MOTION_PROJECTIVE */
/* The order is assumed in vp functions */
enum VP_MOTION_MODEL {
  VP_MOTION_NONE=0,
  VP_MOTION_TRANSLATION=10,
  VP_MOTION_SCALE=11,
  VP_MOTION_ROTATE=12,
  VP_MOTION_X_SHEAR=13,
  VP_MOTION_Y_SHEAR=14,
  VP_MOTION_SIMILARITY=15,
  VP_MOTION_AFFINE=20,
  VP_MOTION_PROJECTIVE=30,
  VP_MOTION_PROJ_3D=40,
  VP_MOTION_SEMI_PROJ_3D=80,
  VP_SIMILARITY=100,
  VP_VFE_AFFINE=120
};

#define VP_REFID -1   /* Default ID used for reference frame */

typedef struct {
  VP_TRS par;            /* Contains the motion paramerers.
                For the standard motion types this is
                represented as 16 number that refer
                to a 4x4 matrix */
  enum VP_MOTION_MODEL type;
  int refid;            /* Reference frame ( takes a point in refid frame
               and moves it by the par to get a point in insid
               frame ) */
  int insid;            /* Inspection frame */
} VP_MOTION;

//typedef VP_LIST VP_MOTION_LIST;
/*
#endpre
# ===================================================================
#h 1 Constant Declarations
*/

/* Macros related to the 4x4 matrix parameters */
#define MXX(m) (m).par[0]
#define MXY(m) (m).par[1]
#define MXZ(m) (m).par[2]
#define MXW(m) (m).par[3]
#define MYX(m) (m).par[4]
#define MYY(m) (m).par[5]
#define MYZ(m) (m).par[6]
#define MYW(m) (m).par[7]
#define MZX(m) (m).par[8]
#define MZY(m) (m).par[9]
#define MZZ(m) (m).par[10]
#define MZW(m) (m).par[11]
#define MWX(m) (m).par[12]
#define MWY(m) (m).par[13]
#define MWZ(m) (m).par[14]
#define MWW(m) (m).par[15]

/* The do {...} while (0) technique creates a statement that can be used legally
   in an if-else statement.  See "Swallowing the semicolon",
   http://gcc.gnu.org/onlinedocs/gcc-2.95.3/cpp_1.html#SEC23 */
/* Initialize the Motion to be Identity */
#define VP_MOTION_ID(m) do {\
  MXX(m)=MYY(m)=MZZ(m)=MWW(m)=(VP_PAR)1.0; \
  MXY(m)=MXZ(m)=MXW(m)=(VP_PAR)0.0; \
  MYX(m)=MYZ(m)=MYW(m)=(VP_PAR)0.0; \
  MZX(m)=MZY(m)=MZW(m)=(VP_PAR)0.0; \
  MWX(m)=MWY(m)=MWZ(m)=(VP_PAR)0.0; \
(m).type = VP_MOTION_TRANSLATION; } while (0)

/* Initialize without altering the translation components */
#define VP_KEEP_TRANSLATION_3D(m) do {\
  MXX(m)=MYY(m)=MZZ(m)=MWW(m)=(VP_PAR)1.0; \
  MXY(m)=MXZ(m)=(VP_PAR)0.0; \
  MYX(m)=MYZ(m)=(VP_PAR)0.0; \
  MZX(m)=MZY(m)=(VP_PAR)0.0; \
  MWX(m)=MWY(m)=MWZ(m)=(VP_PAR)0.0; \
  (m).type = VP_MOTION_PROJ_3D; } while (0)

/* Initialize without altering the 2d translation components */
#define VP_KEEP_TRANSLATION_2D(m) do {\
  VP_KEEP_TRANSLATION_3D(m); MZW(m)=(VP_PAR)0.0; (m).type= VP_MOTION_TRANSLATION;} while (0)

/* Initialize without altering the affine & translation components */
#define VP_KEEP_AFFINE_3D(m) do {\
  MWX(m)=MWY(m)=MWZ(m)=(VP_PAR)0.0; MWW(m)=(VP_PAR)1.0; \
  (m).type = VP_MOTION_PROJ_3D; } while (0)

/* Initialize without altering the 2d affine & translation components */
#define VP_KEEP_AFFINE_2D(m) do {\
  VP_KEEP_AFFINE_3D(m); \
  MXZ(m)=MYZ(m)=(VP_PAR)0.0; MZZ(m)=(VP_PAR)1.0; \
  MZX(m)=MZY(m)=MZW(m)=(VP_PAR)0.0; \
  (m).type = VP_MOTION_AFFINE; } while (0)

/* Initialize without altering the 2d projective parameters */
#define VP_KEEP_PROJECTIVE_2D(m) do {\
  MXZ(m)=MYZ(m)=(VP_PAR)0.0; MZZ(m)=(VP_PAR)1.0; \
  MZX(m)=MZY(m)=MZW(m)=MWZ(m)=(VP_PAR)0.0; \
  (m).type = VP_MOTION_PROJECTIVE; } while (0)

/* Warp a 2d point (assuming the z component is zero) */
#define VP_WARP_POINT_2D(inx,iny,m,outx,outy) do {\
  VP_PAR vpTmpWarpPnt___= MWX(m)*(inx)+MWY(m)*(iny)+MWW(m); \
  outx = (MXX(m)*((VP_PAR)inx)+MXY(m)*((VP_PAR)iny)+MXW(m))/vpTmpWarpPnt___; \
  outy = (MYX(m)*((VP_PAR)inx)+MYY(m)*((VP_PAR)iny)+MYW(m))/vpTmpWarpPnt___; } while (0)

/* Warp a 3d point */
#define VP_WARP_POINT_3D(inx,iny,inz,m,outx,outy,outz) do {\
  VP_PAR vpTmpWarpPnt___= MWX(m)*(inx)+MWY(m)*(iny)+MWZ(m)*((VP_PAR)inz)+MWW(m); \
  outx = (MXX(m)*((VP_PAR)inx)+MXY(m)*((VP_PAR)iny)+MXZ(m)*((VP_PAR)inz)+MXW(m))/vpTmpWarpPnt___; \
  outy = (MYX(m)*((VP_PAR)inx)+MYY(m)*((VP_PAR)iny)+MYZ(m)*((VP_PAR)inz)+MYW(m))/vpTmpWarpPnt___; \
  outz = MZX(m)*((VP_PAR)inx)+MZY(m)*((VP_PAR)iny)+MZZ(m)*((VP_PAR)inz)+MZW(m); \
  if ((m).type==VP_MOTION_PROJ_3D) outz/=vpTmpWarpPnt___; } while (0)

/* Projections of each component */
#define VP_PROJW_3D(m,x,y,z,f)   ( MWX(m)*(x)+MWY(m)*(y)+MWZ(m)*(z)+MWW(m) )
#define VP_PROJX_3D(m,x,y,z,f,w) ((MXX(m)*(x)+MXY(m)*(y)+MXZ(m)*(z)+MXW(m))/(w))
#define VP_PROJY_3D(m,x,y,z,f,w) ((MYX(m)*(x)+MYY(m)*(y)+MYZ(m)*(z)+MYW(m))/(w))
#define VP_PROJZ_3D(m,x,y,z,f,w) ((MZX(m)*(x)+MZY(m)*(y)+MZZ(m)*(z)+MZW(m))/(w))

/* Scale Down a matrix by Sfactor */
#define VP_SCALEDOWN(m,Sfactor) do { \
  MXW(m) /= (VP_PAR)Sfactor; MWX(m) *= (VP_PAR)Sfactor; \
  MYW(m) /= (VP_PAR)Sfactor; MWY(m) *= (VP_PAR)Sfactor; \
  MZW(m) /= (VP_PAR)Sfactor; MWZ(m) *= (VP_PAR)Sfactor; } while (0)

/* Scale Up a matrix by Sfactor */
#define VP_SCALEUP(m,Sfactor) do { \
  MXW(m) *= (VP_PAR)Sfactor; MWX(m) /= (VP_PAR)Sfactor; \
  MYW(m) *= (VP_PAR)Sfactor; MWY(m) /= (VP_PAR)Sfactor; \
  MZW(m) *= (VP_PAR)Sfactor; MWZ(m) /= (VP_PAR)Sfactor; } while (0)

/* Normalize the transformation matrix so that MWW is 1 */
#define VP_NORMALIZE(m) if (MWW(m)!=(VP_PAR)0.0) do { \
  MXX(m)/=MWW(m); MXY(m)/=MWW(m); MXZ(m)/=MWW(m); MXW(m)/= MWW(m); \
  MYX(m)/=MWW(m); MYY(m)/=MWW(m); MYZ(m)/=MWW(m); MYW(m)/= MWW(m); \
  MZX(m)/=MWW(m); MZY(m)/=MWW(m); MZZ(m)/=MWW(m); MZW(m)/= MWW(m); \
  MWX(m)/=MWW(m); MWY(m)/=MWW(m); MWZ(m)/=MWW(m); MWW(m) = (VP_PAR)1.0; } while (0)

#define VP_PRINT_TRANS(msg,b) do { \
  fprintf(stderr, \
      "%s\n%f %f %f %f\n%f %f %f %f\n%f %f %f %f\n%f %f %f %f\n", \
      msg, \
      MXX(b),MXY(b),MXZ(b),MXW(b),  \
      MYX(b),MYY(b),MYZ(b),MYW(b),  \
      MZX(b),MZY(b),MZZ(b),MZW(b),  \
      MWX(b),MWY(b),MWZ(b),MWW(b)); \
} while (0)

/* w' projection given a point x,y,0,f */
#define VP_PROJZ(m,x,y,f) ( \
    MWX(m)*((VP_PAR)x)+MWY(m)*((VP_PAR)y)+MWW(m)*((VP_PAR)f))

/* X Projection given a point x,y,0,f and w' */
#define VP_PROJX(m,x,y,w,f) (\
   (MXX(m)*((VP_PAR)x)+MXY(m)*((VP_PAR)y)+MXW(m)*((VP_PAR)f))/((VP_PAR)w))

/* Y Projection given a point x,y,0,f and the w' */
#define VP_PROJY(m,x,y,w,f) (\
  (MYX(m)*((VP_PAR)x)+MYY(m)*((VP_PAR)y)+MYW(m)*((VP_PAR)f))/((VP_PAR)w))

/* Set the reference id for a motion */
#define VP_SET_REFID(m,id) do { (m).refid=id; } while (0)

/* Set the inspection id for a motion */
#define VP_SET_INSID(m,id) do { (m).insid=id; } while (0)

void vp_copy_motion  (const VP_MOTION *src, VP_MOTION *dst);
int vp_invert_motion(const VP_MOTION* in,VP_MOTION* out);
int vp_cascade_motion(const VP_MOTION* InAB, const VP_MOTION* InBC,VP_MOTION* OutAC);
int vp_zoom_motion2d(VP_MOTION* in, VP_MOTION* out,
              int n, int w, int h, double zoom);
double vp_motion_cornerdiff(const VP_MOTION *mot_a, const VP_MOTION *mot_b,
                     int xo, int yo, int w, int h);

#endif /* VP_MOTIONMODEL_H */
/* =================================================================== */
/* end vp_motionmodel.h */
