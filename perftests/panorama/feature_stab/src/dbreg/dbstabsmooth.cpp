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

#include <stdlib.h>
#include "dbstabsmooth.h"

///// TODO TODO ////////// Replace this with the actual definition from Jayan's reply /////////////
#define vp_copy_motion_no_id vp_copy_motion
///////////////////////////////////////////////////////////////////////////////////////////////////

static bool vpmotion_add(VP_MOTION *in1, VP_MOTION *in2, VP_MOTION *out);
static bool vpmotion_multiply(VP_MOTION *in1, double factor, VP_MOTION *out);

db_StabilizationSmoother::db_StabilizationSmoother()
{
    Init();
}

void db_StabilizationSmoother::Init()
{
    f_smoothOn = true;
    f_smoothReset = false;
    f_smoothFactor = 1.0f;
    f_minDampingFactor = 0.2f;
    f_zoom = 1.0f;
    VP_MOTION_ID(f_motLF);
    VP_MOTION_ID(f_imotLF);
    f_hsize = 0;
    f_vsize = 0;

    VP_MOTION_ID(f_disp_mot);
    VP_MOTION_ID(f_src_mot);
    VP_MOTION_ID(f_diff_avg);

    for( int i = 0; i < MOTION_ARRAY-1; i++) {
        VP_MOTION_ID(f_hist_mot_speed[i]);
        VP_MOTION_ID(f_hist_mot[i]);
        VP_MOTION_ID(f_hist_diff_mot[i]);
    }
    VP_MOTION_ID(f_hist_mot[MOTION_ARRAY-1]);

}

db_StabilizationSmoother::~db_StabilizationSmoother()
{}


bool db_StabilizationSmoother::smoothMotion(VP_MOTION *inmot, VP_MOTION *outmot)
{
    VP_MOTION_ID(f_motLF);
    VP_MOTION_ID(f_imotLF);
    f_motLF.insid = inmot->refid;
    f_motLF.refid = inmot->insid;

    if(f_smoothOn) {
        if(!f_smoothReset) {
            MXX(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MXX(f_motLF) + (1.0-f_smoothFactor)* (double) MXX(*inmot));
            MXY(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MXY(f_motLF) + (1.0-f_smoothFactor)* (double) MXY(*inmot));
            MXZ(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MXZ(f_motLF) + (1.0-f_smoothFactor)* (double) MXZ(*inmot));
            MXW(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MXW(f_motLF) + (1.0-f_smoothFactor)* (double) MXW(*inmot));

            MYX(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MYX(f_motLF) + (1.0-f_smoothFactor)* (double) MYX(*inmot));
            MYY(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MYY(f_motLF) + (1.0-f_smoothFactor)* (double) MYY(*inmot));
            MYZ(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MYZ(f_motLF) + (1.0-f_smoothFactor)* (double) MYZ(*inmot));
            MYW(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MYW(f_motLF) + (1.0-f_smoothFactor)* (double) MYW(*inmot));

            MZX(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MZX(f_motLF) + (1.0-f_smoothFactor)* (double) MZX(*inmot));
            MZY(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MZY(f_motLF) + (1.0-f_smoothFactor)* (double) MZY(*inmot));
            MZZ(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MZZ(f_motLF) + (1.0-f_smoothFactor)* (double) MZZ(*inmot));
            MZW(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MZW(f_motLF) + (1.0-f_smoothFactor)* (double) MZW(*inmot));

            MWX(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MWX(f_motLF) + (1.0-f_smoothFactor)* (double) MWX(*inmot));
            MWY(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MWY(f_motLF) + (1.0-f_smoothFactor)* (double) MWY(*inmot));
            MWZ(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MWZ(f_motLF) + (1.0-f_smoothFactor)* (double) MWZ(*inmot));
            MWW(f_motLF) = (VP_PAR) (f_smoothFactor*(double) MWW(f_motLF) + (1.0-f_smoothFactor)* (double) MWW(*inmot));
        }
        else
            vp_copy_motion_no_id(inmot, &f_motLF); // f_smoothFactor = 0.0

        // Only allow LF motion to be compensated. Remove HF motion from
        // the output transformation
        if(!vp_invert_motion(&f_motLF, &f_imotLF))
            return false;

        if(!vp_cascade_motion(&f_imotLF, inmot, outmot))
            return false;
    }
    else {
        vp_copy_motion_no_id(inmot, outmot);
    }

    return true;
}

bool db_StabilizationSmoother::smoothMotionAdaptive(/*VP_BIMG *bimg,*/int hsize, int vsize, VP_MOTION *inmot, VP_MOTION *outmot)
{
    VP_MOTION tmpMotion, testMotion;
    VP_PAR p1x, p2x, p3x, p4x;
    VP_PAR p1y, p2y, p3y, p4y;
    double smoothFactor;
    double minSmoothFactor = f_minDampingFactor;

//  int hsize = bimg->w;
//  int vsize = bimg->h;
    double border_factor = 0.01;//0.2;
    double border_x = border_factor * hsize;
    double border_y = border_factor * vsize;

    VP_MOTION_ID(f_motLF);
    VP_MOTION_ID(f_imotLF);
    VP_MOTION_ID(testMotion);
    VP_MOTION_ID(tmpMotion);

    if (f_smoothOn) {
        VP_MOTION identityMotion;
        VP_MOTION_ID(identityMotion); // initialize the motion
        vp_copy_motion(inmot/*in*/, &testMotion/*out*/);
        VP_PAR delta = vp_motion_cornerdiff(&testMotion, &identityMotion, 0, 0,(int)hsize, (int)vsize);

        smoothFactor = 0.99 - 0.0015 * delta;

        if(smoothFactor < minSmoothFactor)
            smoothFactor = minSmoothFactor;

        // Find the amount of motion that must be compensated so that no "border" pixels are seen in the stable video
        for (smoothFactor = smoothFactor; smoothFactor >= minSmoothFactor; smoothFactor -= 0.01) {
            // Compute the smoothed motion
            if(!smoothMotion(inmot, &tmpMotion, smoothFactor))
                break;

            // TmpMotion, or Qsi where s is the smoothed display reference and i is the
            // current image, tells us how points in the S co-ordinate system map to
            // points in the I CS.  We would like to check whether the four corners of the
            // warped and smoothed display reference lies entirely within the I co-ordinate
            // system.  If yes, then the amount of smoothing is sufficient so that NO
            // border pixels are seen at the output.  We test for f_smoothFactor terms
            // between 0.9 and 1.0, in steps of 0.01, and between 0.5 ands 0.9 in steps of 0.1

            (void) vp_zoom_motion2d(&tmpMotion, &testMotion, 1, hsize, vsize, (double)f_zoom); // needs to return bool

            VP_WARP_POINT_2D(0, 0, testMotion, p1x, p1y);
            VP_WARP_POINT_2D(hsize - 1, 0, testMotion, p2x, p2y);
            VP_WARP_POINT_2D(hsize - 1, vsize - 1, testMotion, p3x, p3y);
            VP_WARP_POINT_2D(0, vsize - 1, testMotion, p4x, p4y);

            if (!is_point_in_rect((double)p1x,(double)p1y,-border_x,-border_y,(double)(hsize+2.0*border_x),(double)(vsize+2.0*border_y))) {
                continue;
            }
            if (!is_point_in_rect((double)p2x, (double)p2y,-border_x,-border_y,(double)(hsize+2.0*border_x),(double)(vsize+2.0*border_y))) {
                continue;
            }
            if (!is_point_in_rect((double)p3x,(double)p3y,-border_x,-border_y,(double)(hsize+2.0*border_x),(double)(vsize+2.0*border_y))) {
                continue;
            }
            if (!is_point_in_rect((double)p4x, (double)p4y,-border_x,-border_y,(double)(hsize+2.0*border_x),(double)(vsize+2.0*border_y))) {
                continue;
            }

            // If we get here, then all the points are in the rectangle.
            // Therefore, break out of this loop
            break;
        }

        // if we get here and f_smoothFactor <= fMinDampingFactor, reset the stab reference
        if (smoothFactor < f_minDampingFactor)
            smoothFactor = f_minDampingFactor;

        // use the smoothed motion for stabilization
        vp_copy_motion_no_id(&tmpMotion/*in*/, outmot/*out*/);
    }
    else
    {
        vp_copy_motion_no_id(inmot, outmot);
    }

    return true;
}

bool db_StabilizationSmoother::smoothMotion(VP_MOTION *inmot, VP_MOTION *outmot, double smooth_factor)
{
    f_motLF.insid = inmot->refid;
    f_motLF.refid = inmot->insid;

    if(f_smoothOn) {
        if(!f_smoothReset) {
            MXX(f_motLF) = (VP_PAR) (smooth_factor*(double) MXX(f_motLF) + (1.0-smooth_factor)* (double) MXX(*inmot));
            MXY(f_motLF) = (VP_PAR) (smooth_factor*(double) MXY(f_motLF) + (1.0-smooth_factor)* (double) MXY(*inmot));
            MXZ(f_motLF) = (VP_PAR) (smooth_factor*(double) MXZ(f_motLF) + (1.0-smooth_factor)* (double) MXZ(*inmot));
            MXW(f_motLF) = (VP_PAR) (smooth_factor*(double) MXW(f_motLF) + (1.0-smooth_factor)* (double) MXW(*inmot));

            MYX(f_motLF) = (VP_PAR) (smooth_factor*(double) MYX(f_motLF) + (1.0-smooth_factor)* (double) MYX(*inmot));
            MYY(f_motLF) = (VP_PAR) (smooth_factor*(double) MYY(f_motLF) + (1.0-smooth_factor)* (double) MYY(*inmot));
            MYZ(f_motLF) = (VP_PAR) (smooth_factor*(double) MYZ(f_motLF) + (1.0-smooth_factor)* (double) MYZ(*inmot));
            MYW(f_motLF) = (VP_PAR) (smooth_factor*(double) MYW(f_motLF) + (1.0-smooth_factor)* (double) MYW(*inmot));

            MZX(f_motLF) = (VP_PAR) (smooth_factor*(double) MZX(f_motLF) + (1.0-smooth_factor)* (double) MZX(*inmot));
            MZY(f_motLF) = (VP_PAR) (smooth_factor*(double) MZY(f_motLF) + (1.0-smooth_factor)* (double) MZY(*inmot));
            MZZ(f_motLF) = (VP_PAR) (smooth_factor*(double) MZZ(f_motLF) + (1.0-smooth_factor)* (double) MZZ(*inmot));
            MZW(f_motLF) = (VP_PAR) (smooth_factor*(double) MZW(f_motLF) + (1.0-smooth_factor)* (double) MZW(*inmot));

            MWX(f_motLF) = (VP_PAR) (smooth_factor*(double) MWX(f_motLF) + (1.0-smooth_factor)* (double) MWX(*inmot));
            MWY(f_motLF) = (VP_PAR) (smooth_factor*(double) MWY(f_motLF) + (1.0-smooth_factor)* (double) MWY(*inmot));
            MWZ(f_motLF) = (VP_PAR) (smooth_factor*(double) MWZ(f_motLF) + (1.0-smooth_factor)* (double) MWZ(*inmot));
            MWW(f_motLF) = (VP_PAR) (smooth_factor*(double) MWW(f_motLF) + (1.0-smooth_factor)* (double) MWW(*inmot));
        }
        else
            vp_copy_motion_no_id(inmot, &f_motLF); // smooth_factor = 0.0

        // Only allow LF motion to be compensated. Remove HF motion from
        // the output transformation
        if(!vp_invert_motion(&f_motLF, &f_imotLF))
            return false;

        if(!vp_cascade_motion(&f_imotLF, inmot, outmot))
            return false;
    }
    else {
        vp_copy_motion_no_id(inmot, outmot);
    }

    return true;
}

//! Overloaded smoother function that takes in user-specidied smoothing factor
bool
db_StabilizationSmoother::smoothMotion1(VP_MOTION *inmot, VP_MOTION *outmot, VP_MOTION *motLF, VP_MOTION *imotLF, double factor)
{

    if(!f_smoothOn) {
        vp_copy_motion(inmot, outmot);
        return true;
    }
    else {
        if(!f_smoothReset) {
            MXX(*motLF) = (VP_PAR) (factor*(double) MXX(*motLF) + (1.0-factor)* (double) MXX(*inmot));
            MXY(*motLF) = (VP_PAR) (factor*(double) MXY(*motLF) + (1.0-factor)* (double) MXY(*inmot));
            MXZ(*motLF) = (VP_PAR) (factor*(double) MXZ(*motLF) + (1.0-factor)* (double) MXZ(*inmot));
            MXW(*motLF) = (VP_PAR) (factor*(double) MXW(*motLF) + (1.0-factor)* (double) MXW(*inmot));

            MYX(*motLF) = (VP_PAR) (factor*(double) MYX(*motLF) + (1.0-factor)* (double) MYX(*inmot));
            MYY(*motLF) = (VP_PAR) (factor*(double) MYY(*motLF) + (1.0-factor)* (double) MYY(*inmot));
            MYZ(*motLF) = (VP_PAR) (factor*(double) MYZ(*motLF) + (1.0-factor)* (double) MYZ(*inmot));
            MYW(*motLF) = (VP_PAR) (factor*(double) MYW(*motLF) + (1.0-factor)* (double) MYW(*inmot));

            MZX(*motLF) = (VP_PAR) (factor*(double) MZX(*motLF) + (1.0-factor)* (double) MZX(*inmot));
            MZY(*motLF) = (VP_PAR) (factor*(double) MZY(*motLF) + (1.0-factor)* (double) MZY(*inmot));
            MZZ(*motLF) = (VP_PAR) (factor*(double) MZZ(*motLF) + (1.0-factor)* (double) MZZ(*inmot));
            MZW(*motLF) = (VP_PAR) (factor*(double) MZW(*motLF) + (1.0-factor)* (double) MZW(*inmot));

            MWX(*motLF) = (VP_PAR) (factor*(double) MWX(*motLF) + (1.0-factor)* (double) MWX(*inmot));
            MWY(*motLF) = (VP_PAR) (factor*(double) MWY(*motLF) + (1.0-factor)* (double) MWY(*inmot));
            MWZ(*motLF) = (VP_PAR) (factor*(double) MWZ(*motLF) + (1.0-factor)* (double) MWZ(*inmot));
            MWW(*motLF) = (VP_PAR) (factor*(double) MWW(*motLF) + (1.0-factor)* (double) MWW(*inmot));
        }
        else {
            vp_copy_motion(inmot, motLF);
        }
        // Only allow LF motion to be compensated. Remove HF motion from the output transformation
        if(!vp_invert_motion(motLF, imotLF)) {
#if DEBUG_PRINT
            printfOS("Invert failed \n");
#endif
            return false;
        }
        if(!vp_cascade_motion(imotLF, inmot, outmot)) {
#if DEBUG_PRINT
            printfOS("cascade failed \n");
#endif
            return false;
        }
    }
    return true;
}




bool db_StabilizationSmoother::is_point_in_rect(double px, double py, double rx, double ry, double w, double h)
{
    if (px < rx)
        return(false);
    if (px >= rx + w)
        return(false);
    if (py < ry)
        return(false);
    if (py >= ry + h)
        return(false);

    return(true);
}



static bool vpmotion_add(VP_MOTION *in1, VP_MOTION *in2, VP_MOTION *out)
{
    int i;
    if(in1 == NULL || in2 == NULL || out == NULL)
        return false;

    for(i = 0; i < VP_MAX_MOTION_PAR; i++)
        out->par[i] = in1->par[i] + in2->par[i];

    return true;
}

static bool vpmotion_multiply(VP_MOTION *in1, double factor, VP_MOTION *out)
{
    int i;
    if(in1 == NULL || out == NULL)
        return false;

    for(i = 0; i < VP_MAX_MOTION_PAR; i++)
        out->par[i] = in1->par[i] * factor;

    return true;
}

