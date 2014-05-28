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
#sourcefile  vpmotion/vp_motionmodel.c
#category    motion-model
*
* Copyright 1998 Sarnoff Corporation
* All Rights Reserved
*
* Modification History
*      Date: 02/14/98
*      Author: supuns
*      Shop Order: 17xxx
*              @(#) $Id: vp_motionmodel.c,v 1.4 2011/06/17 14:04:33 mbansal Exp $
*/

/*
* ===================================================================
* Include Files
*/

#include <string.h> /* memmove */
#include <math.h>
#include "vp_motionmodel.h"

/* Static Functions */
static
double Det3(double m[3][3])
{
  double result;

  result =
    m[0][0]*m[1][1]*m[2][2] + m[0][1]*m[1][2]*m[2][0] +
    m[0][2]*m[1][0]*m[2][1] - m[0][2]*m[1][1]*m[2][0] -
    m[0][0]*m[1][2]*m[2][1] - m[0][1]*m[1][0]*m[2][2];

  return(result);
}

typedef double MATRIX[4][4];

static
double Det4(MATRIX m)
{
    /* ==> This is a poor implementation of determinant.
       Writing the formula out in closed form is unnecessarily complicated
       and mistakes are easy to make. */
  double result;

  result=
    m[0][3] *m[1][2] *m[2][1] *m[3][0] - m[0][2] *m[1][3] *m[2][1] *m[3][0] - m[0][3] *m[1][1] *m[2][2] *m[3][0] +
    m[0][1] *m[1][3] *m[2][2] *m[3][0] + m[0][2] *m[1][1] *m[2][3] *m[3][0] - m[0][1] *m[1][2] *m[2][3] *m[3][0] - m[0][3] *m[1][2] *m[2][0] *m[3][1] +
    m[0][2] *m[1][3] *m[2][0] *m[3][1] + m[0][3] *m[1][0] *m[2][2] *m[3][1] - m[0][0] *m[1][3] *m[2][2] *m[3][1] - m[0][2] *m[1][0] *m[2][3] *m[3][1] +
    m[0][0] *m[1][2] *m[2][3] *m[3][1] + m[0][3] *m[1][1] *m[2][0] *m[3][2] - m[0][1] *m[1][3] *m[2][0] *m[3][2] - m[0][3] *m[1][0] *m[2][1] *m[3][2] +
    m[0][0] *m[1][3] *m[2][1] *m[3][2] + m[0][1] *m[1][0] *m[2][3] *m[3][2] - m[0][0] *m[1][1] *m[2][3] *m[3][2] - m[0][2] *m[1][1] *m[2][0] *m[3][3] +
    m[0][1] *m[1][2] *m[2][0] *m[3][3] + m[0][2] *m[1][0] *m[2][1] *m[3][3] - m[0][0] *m[1][2] *m[2][1] *m[3][3] - m[0][1] *m[1][0] *m[2][2] *m[3][3] +
    m[0][0] *m[1][1] *m[2][2] *m[3][3];
  /*
    m[0][0]*m[1][1]*m[2][2]*m[3][3]-m[0][1]*m[1][0]*m[2][2]*m[3][3]+
    m[0][1]*m[1][2]*m[2][0]*m[3][3]-m[0][2]*m[1][1]*m[2][0]*m[3][3]+
    m[0][2]*m[1][0]*m[2][1]*m[3][3]-m[0][0]*m[1][2]*m[2][1]*m[3][3]+
    m[0][0]*m[1][2]*m[2][3]*m[3][1]-m[0][2]*m[1][0]*m[2][3]*m[3][1]+
    m[0][2]*m[1][3]*m[2][0]*m[3][1]-m[0][3]*m[1][2]*m[2][0]*m[3][1]+
    m[0][3]*m[1][0]*m[2][2]*m[3][1]-m[0][0]*m[1][3]*m[2][2]*m[3][1]+
    m[0][0]*m[1][3]*m[2][1]*m[3][2]-m[0][3]*m[1][0]*m[2][3]*m[3][2]+
    m[0][1]*m[1][0]*m[2][3]*m[3][2]-m[0][0]*m[1][1]*m[2][0]*m[3][2]+
    m[0][3]*m[1][1]*m[2][0]*m[3][2]-m[0][1]*m[1][3]*m[2][1]*m[3][2]+
    m[0][1]*m[1][3]*m[2][2]*m[3][0]-m[0][3]*m[1][1]*m[2][2]*m[3][0]+
    m[0][2]*m[1][1]*m[2][3]*m[3][0]-m[0][1]*m[1][2]*m[2][3]*m[3][0]+
    m[0][3]*m[1][2]*m[2][1]*m[3][0]-m[0][2]*m[1][3]*m[2][1]*m[3][0];
    */
  return(result);
}

static
int inv4Mat(const VP_MOTION* in, VP_MOTION* out)
{
    /* ==> This is a poor implementation of inversion.  The determinant
       method is O(N^4), i.e. unnecessarily slow, and not numerically accurate.
       The real complexity of inversion is O(N^3), and is best done using
       LU decomposition. */

  MATRIX inmat,outmat;
  int i, j, k, l, m, n,ntemp;
  double mat[3][3], indet, temp;

  /* check for non-empty structures structure */
  if (((VP_MOTION *) NULL == in) || ((VP_MOTION *) NULL == out)) {
    return 1;
  }

  for(k=0,i=0;i<4;i++)
    for(j=0;j<4;j++,k++)
      inmat[i][j]=(double)in->par[k];

  indet = Det4(inmat);
  if (indet==0) return(-1);

  for (i=0;i<4;i++) {
    for (j=0;j<4;j++) {
      m = 0;
      for (k=0;k<4;k++) {
    if (i != k) {
      n = 0;
      for (l=0;l<4;l++)
        if (j != l) {
          mat[m][n] = inmat[k][l];
          n++;
        }
      m++;
    }
      }

      temp = -1.;
      ntemp = (i +j ) %2;
      if( ntemp == 0)  temp = 1.;

      outmat[j][i] = temp * Det3(mat)/indet;
    }
  }

  for(k=0,i=0;i<4;i++)
    for(j=0;j<4;j++,k++)
      out->par[k]=(VP_PAR)outmat[i][j]; /*lint !e771*/

  return(0);
}

/*
* ===================================================================
* Public Functions
#htmlstart
*/

/*
 * ===================================================================
#fn vp_invert_motion
#ft invert a motion
#fd DEFINITION
       Bool
       vp_invert_motion(const VP_MOTION* in,VP_MOTION* out)
#fd PURPOSE
       This inverts the motion given in 'in'.
       All motion models upto VP_MOTION_SEMI_PROJ_3D are supported.
       It is assumed that the all 16 parameters are properly
       initialized although you may not be using them. You could
       use the VP_KEEP_ macro's defined in vp_motionmodel.h to set
       the un-initialized parameters. This uses a 4x4 matrix invertion
       function internally.
       It is SAFE to pass the same pointer as both the 'in' and 'out'
       parameters.
#fd INPUTS
       in  - input motion
#fd OUTPUTS
       out - output inverted motion. If singular matrix uninitialized.
             if MWW(in) is non-zero it is also normalized.
#fd RETURNS
       FALSE - matrix is singular or motion model not supported
       TRUE  - otherwise
#fd SIDE EFFECTS
       None
#endfn
*/

int vp_invert_motion(const VP_MOTION* in,VP_MOTION* out)
{
  int refid;

  /* check for non-empty structures structure */
  if (((VP_MOTION *) NULL == in) || ((VP_MOTION *) NULL == out)) {
    return FALSE;
  }

  if (in->type>VP_MOTION_SEMI_PROJ_3D) {
    return FALSE;
  }

  if (inv4Mat(in,out)<0)
    return FALSE;

  /*VP_NORMALIZE(*out);*/
  out->type = in->type;
  refid=in->refid;
  out->refid=in->insid;
  out->insid=refid;
  return TRUE;
}

/*
* ===================================================================
#fn vp_cascade_motion
#ft Cascade two motion transforms
#fd DEFINITION
      Bool
      vp_cascade_motion(const VP_MOTION* InAB,const VP_MOTION* InBC,VP_MOTION* OutAC)
#fd PURPOSE
      Given Motion Transforms A->B and B->C, this function will
      generate a New Motion that describes the transformation
      from A->C.
      More specifically, OutAC = InBC * InAC.
      This function works ok if InAB,InBC and OutAC are the same pointer.
#fd INPUTS
      InAB - First Motion Transform
      InBC - Second Motion Tranform
#fd OUTPUTS
      OutAC - Cascaded Motion
#fd RETURNS
      FALSE - motion model not supported
      TRUE  - otherwise
#fd SIDE EFFECTS
      None
#endfn
*/

int vp_cascade_motion(const VP_MOTION* InA, const VP_MOTION* InB,VP_MOTION* Out)
{
    /* ==> This is a poor implementation of matrix multiplication.
       Writing the formula out in closed form is unnecessarily complicated
       and mistakes are easy to make. */
  VP_PAR mxx,mxy,mxz,mxw;
  VP_PAR myx,myy,myz,myw;
  VP_PAR mzx,mzy,mzz,mzw;
  VP_PAR mwx,mwy,mwz,mww;

  /* check for non-empty structures structure */
  if (((VP_MOTION *) NULL == InA) || ((VP_MOTION *) NULL == InB) ||
      ((VP_MOTION *) NULL == Out)) {
    return FALSE;
  }

  if (InA->type>VP_MOTION_PROJ_3D) {
    return FALSE;
  }

  if (InB->type>VP_MOTION_PROJ_3D) {
    return FALSE;
  }

  mxx = MXX(*InB)*MXX(*InA)+MXY(*InB)*MYX(*InA)+MXZ(*InB)*MZX(*InA)+MXW(*InB)*MWX(*InA);
  mxy = MXX(*InB)*MXY(*InA)+MXY(*InB)*MYY(*InA)+MXZ(*InB)*MZY(*InA)+MXW(*InB)*MWY(*InA);
  mxz = MXX(*InB)*MXZ(*InA)+MXY(*InB)*MYZ(*InA)+MXZ(*InB)*MZZ(*InA)+MXW(*InB)*MWZ(*InA);
  mxw = MXX(*InB)*MXW(*InA)+MXY(*InB)*MYW(*InA)+MXZ(*InB)*MZW(*InA)+MXW(*InB)*MWW(*InA);
  myx = MYX(*InB)*MXX(*InA)+MYY(*InB)*MYX(*InA)+MYZ(*InB)*MZX(*InA)+MYW(*InB)*MWX(*InA);
  myy = MYX(*InB)*MXY(*InA)+MYY(*InB)*MYY(*InA)+MYZ(*InB)*MZY(*InA)+MYW(*InB)*MWY(*InA);
  myz = MYX(*InB)*MXZ(*InA)+MYY(*InB)*MYZ(*InA)+MYZ(*InB)*MZZ(*InA)+MYW(*InB)*MWZ(*InA);
  myw = MYX(*InB)*MXW(*InA)+MYY(*InB)*MYW(*InA)+MYZ(*InB)*MZW(*InA)+MYW(*InB)*MWW(*InA);
  mzx = MZX(*InB)*MXX(*InA)+MZY(*InB)*MYX(*InA)+MZZ(*InB)*MZX(*InA)+MZW(*InB)*MWX(*InA);
  mzy = MZX(*InB)*MXY(*InA)+MZY(*InB)*MYY(*InA)+MZZ(*InB)*MZY(*InA)+MZW(*InB)*MWY(*InA);
  mzz = MZX(*InB)*MXZ(*InA)+MZY(*InB)*MYZ(*InA)+MZZ(*InB)*MZZ(*InA)+MZW(*InB)*MWZ(*InA);
  mzw = MZX(*InB)*MXW(*InA)+MZY(*InB)*MYW(*InA)+MZZ(*InB)*MZW(*InA)+MZW(*InB)*MWW(*InA);
  mwx = MWX(*InB)*MXX(*InA)+MWY(*InB)*MYX(*InA)+MWZ(*InB)*MZX(*InA)+MWW(*InB)*MWX(*InA);
  mwy = MWX(*InB)*MXY(*InA)+MWY(*InB)*MYY(*InA)+MWZ(*InB)*MZY(*InA)+MWW(*InB)*MWY(*InA);
  mwz = MWX(*InB)*MXZ(*InA)+MWY(*InB)*MYZ(*InA)+MWZ(*InB)*MZZ(*InA)+MWW(*InB)*MWZ(*InA);
  mww = MWX(*InB)*MXW(*InA)+MWY(*InB)*MYW(*InA)+MWZ(*InB)*MZW(*InA)+MWW(*InB)*MWW(*InA);

  MXX(*Out)=mxx; MXY(*Out)=mxy; MXZ(*Out)=mxz; MXW(*Out)=mxw;
  MYX(*Out)=myx; MYY(*Out)=myy; MYZ(*Out)=myz; MYW(*Out)=myw;
  MZX(*Out)=mzx; MZY(*Out)=mzy; MZZ(*Out)=mzz; MZW(*Out)=mzw;
  MWX(*Out)=mwx; MWY(*Out)=mwy; MWZ(*Out)=mwz; MWW(*Out)=mww;
  /* VP_NORMALIZE(*Out); */
  Out->type= (InA->type > InB->type) ? InA->type : InB->type;
  Out->refid=InA->refid;
  Out->insid=InB->insid;

  return TRUE;
}

/*
* ===================================================================
#fn vp_copy_motion
#ft Copies the source motion to the destination motion.
#fd DEFINITION
    void
    vp_copy_motion  (const VP_MOTION *src, VP_MOTION *dst)
#fd PURPOSE
    Copies the source motion to the destination motion.
        It is OK if src == dst.
    NOTE THAT THE SOURCE IS THE FIRST ARGUMENT.
    This is different from some of the other VP
    copy functions.
#fd INPUTS
    src is the source motion
    dst is the destination motion
#fd RETURNS
    void
#endfn
*/
void vp_copy_motion  (const VP_MOTION *src, VP_MOTION *dst)
{
  /* Use memmove rather than memcpy because it handles overlapping memory
     OK. */
  memmove(dst, src, sizeof(VP_MOTION));
  return;
} /* vp_copy_motion() */

#define VP_SQR(x)   ( (x)*(x) )
double vp_motion_cornerdiff(const VP_MOTION *mot_a, const VP_MOTION *mot_b,
                     int xo, int yo, int w, int h)
{
  double ax1, ay1, ax2, ay2, ax3, ay3, ax4, ay4;
  double bx1, by1, bx2, by2, bx3, by3, bx4, by4;
  double err;

  /*lint -e639 -e632 -e633 */
  VP_WARP_POINT_2D(xo, yo,         *mot_a, ax1, ay1);
  VP_WARP_POINT_2D(xo+w-1, yo,     *mot_a, ax2, ay2);
  VP_WARP_POINT_2D(xo+w-1, yo+h-1, *mot_a, ax3, ay3);
  VP_WARP_POINT_2D(xo, yo+h-1,     *mot_a, ax4, ay4);
  VP_WARP_POINT_2D(xo, yo,         *mot_b, bx1, by1);
  VP_WARP_POINT_2D(xo+w-1, yo,     *mot_b, bx2, by2);
  VP_WARP_POINT_2D(xo+w-1, yo+h-1, *mot_b, bx3, by3);
  VP_WARP_POINT_2D(xo, yo+h-1,     *mot_b, bx4, by4);
  /*lint +e639 +e632 +e633 */

  err = 0;
  err += (VP_SQR(ax1 - bx1) + VP_SQR(ay1 - by1));
  err += (VP_SQR(ax2 - bx2) + VP_SQR(ay2 - by2));
  err += (VP_SQR(ax3 - bx3) + VP_SQR(ay3 - by3));
  err += (VP_SQR(ax4 - bx4) + VP_SQR(ay4 - by4));

  return(sqrt(err));
}

int vp_zoom_motion2d(VP_MOTION* in, VP_MOTION* out,
                 int n, int w, int h, double zoom)
{
  int ii;
  VP_PAR inv_zoom;
  VP_PAR cx, cy;
  VP_MOTION R2r,R2f;
  VP_MOTION *res;

  /* check for non-empty structures structure */
  if (((VP_MOTION *) NULL == in)||(zoom <= 0.0)||(w <= 0)||(h <= 0)) {
    return FALSE;
  }

  /* ==> Not sure why the special case of out=NULL is necessary.  Why couldn't
     the caller just pass the same pointer for both in and out? */
  res = ((VP_MOTION *) NULL == out)?in:out;

  cx = (VP_PAR) (w/2.0);
  cy = (VP_PAR) (h/2.0);

  VP_MOTION_ID(R2r);
  inv_zoom = (VP_PAR)(1.0/zoom);
  MXX(R2r) = inv_zoom;
  MYY(R2r) = inv_zoom;
  MXW(R2r)=cx*(((VP_PAR)1.0) - inv_zoom);
  MYW(R2r)=cy*(((VP_PAR)1.0) - inv_zoom);

  VP_KEEP_AFFINE_2D(R2r);

  for(ii=0;ii<n;ii++) {
    (void) vp_cascade_motion(&R2r,in+ii,&R2f);
    res[ii]=R2f;
  }

  return TRUE;
} /* vp_zoom_motion2d() */

/* =================================================================== */
/* end vp_motionmodel.c */
