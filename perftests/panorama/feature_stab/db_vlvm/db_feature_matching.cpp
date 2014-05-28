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

/*$Id: db_feature_matching.cpp,v 1.4 2011/06/17 14:03:30 mbansal Exp $*/

/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

#include "db_utilities.h"
#include "db_feature_matching.h"
#ifdef _VERBOSE_
#include <iostream>
#endif


int AffineWarpPoint_NN_LUT_x[11][11];
int AffineWarpPoint_NN_LUT_y[11][11];

float AffineWarpPoint_BL_LUT_x[11][11];
float AffineWarpPoint_BL_LUT_y[11][11];


inline float db_SignedSquareNormCorr7x7_u(unsigned char **f_img,unsigned char **g_img,int x_f,int y_f,int x_g,int y_g)
{
    unsigned char *pf,*pg;
    float f,g,fgsum,f2sum,g2sum,fsum,gsum,fg_corr,den;
    int xm_f,xm_g;

    xm_f=x_f-3;
    xm_g=x_g-3;
    fgsum=0.0; f2sum=0.0; g2sum=0.0; fsum=0.0; gsum=0.0;

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    fg_corr=49.0f*fgsum-fsum*gsum;
    den=(49.0f*f2sum-fsum*fsum)*(49.0f*g2sum-gsum*gsum);
    if(den!=0.0)
    {
        if(fg_corr>=0.0) return(fg_corr*fg_corr/den);
        return(-fg_corr*fg_corr/den);
    }
    return(0.0);
}

inline float db_SignedSquareNormCorr9x9_u(unsigned char **f_img,unsigned char **g_img,int x_f,int y_f,int x_g,int y_g)
{
    unsigned char *pf,*pg;
    float f,g,fgsum,f2sum,g2sum,fsum,gsum,fg_corr,den;
    int xm_f,xm_g;

    xm_f=x_f-4;
    xm_g=x_g-4;
    fgsum=0.0; f2sum=0.0; g2sum=0.0; fsum=0.0; gsum=0.0;

    pf=f_img[y_f-4]+xm_f; pg=g_img[y_g-4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+4]+xm_f; pg=g_img[y_g+4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    fg_corr=81.0f*fgsum-fsum*gsum;
    den=(81.0f*f2sum-fsum*fsum)*(81.0f*g2sum-gsum*gsum);
    if(den!=0.0)
    {
        if(fg_corr>=0.0) return(fg_corr*fg_corr/den);
        return(-fg_corr*fg_corr/den);
    }
    return(0.0);
}

inline float db_SignedSquareNormCorr11x11_u(unsigned char **f_img,unsigned char **g_img,int x_f,int y_f,int x_g,int y_g)
{
    unsigned char *pf,*pg;
    float f,g,fgsum,f2sum,g2sum,fsum,gsum,fg_corr,den;
    int xm_f,xm_g;

    xm_f=x_f-5;
    xm_g=x_g-5;
    fgsum=0.0; f2sum=0.0; g2sum=0.0; fsum=0.0; gsum=0.0;

    pf=f_img[y_f-5]+xm_f; pg=g_img[y_g-5]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-4]+xm_f; pg=g_img[y_g-4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+4]+xm_f; pg=g_img[y_g+4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+5]+xm_f; pg=g_img[y_g+5]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    fg_corr=121.0f*fgsum-fsum*gsum;
    den=(121.0f*f2sum-fsum*fsum)*(121.0f*g2sum-gsum*gsum);
    if(den!=0.0)
    {
        if(fg_corr>=0.0) return(fg_corr*fg_corr/den);
        return(-fg_corr*fg_corr/den);
    }
    return(0.0);
}

inline void db_SignedSquareNormCorr11x11_Pre_u(unsigned char **f_img,int x_f,int y_f,float *sum,float *recip)
{
    unsigned char *pf;
    float den;
    int f,f2sum,fsum;
    int xm_f;

    xm_f=x_f-5;

    pf=f_img[y_f-5]+xm_f;
    f= *pf++; f2sum=f*f;  fsum=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f-4]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f-3]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f-2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f-1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+3]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+4]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+5]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    *sum= (float) fsum;
    den=(121.0f*f2sum-fsum*fsum);
    *recip=(float)(((den!=0.0)?1.0/den:0.0));
}

inline void db_SignedSquareNormCorr5x5_PreAlign_u(short *patch,const unsigned char * const *f_img,int x_f,int y_f,float *sum,float *recip)
{
    float den;
    int f2sum,fsum;
    int xm_f=x_f-2;

#ifndef DB_USE_SSE2
    const unsigned char *pf;
    short f;

    pf=f_img[y_f-2]+xm_f;
    f= *pf++; f2sum=f*f; fsum=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;
    //int xwi;
    //int ywi;
    //f2sum=0;
    //fsum=0;
    //for (int r=-5;r<=5;r++){
    //  ywi=y_f+r;
    //  for (int c=-5;c<=5;c++){
    //      xwi=x_f+c;
    //      f=f_img[ywi][xwi];
    //      f2sum+=f*f;
    //      fsum+=f;
    //      (*patch++)=f;
    //  }
    //}
    (*patch++)=0; (*patch++)=0; (*patch++)=0; (*patch++)=0; (*patch++)=0;
    (*patch++)=0; (*patch++)=0;
#endif /* DB_USE_SSE2 */

    *sum= (float) fsum;
    den=(25.0f*f2sum-fsum*fsum);
    *recip= (float)((den!=0.0)?1.0/den:0.0);
}

inline void db_SignedSquareNormCorr21x21_PreAlign_u(short *patch,const unsigned char * const *f_img,int x_f,int y_f,float *sum,float *recip)
{
    float den;
    int f2sum,fsum;
    int xm_f=x_f-10;
    short f;

    int xwi;
    int ywi;
    f2sum=0;
    fsum=0;
    for (int r=-10;r<=10;r++){
        ywi=y_f+r;
        for (int c=-10;c<=10;c++){
            xwi=x_f+c;
            f=f_img[ywi][xwi];
            f2sum+=f*f;
            fsum+=f;
            (*patch++)=f;
        }
    }

    for(int i=442; i<512; i++)
        (*patch++)=0;

    *sum= (float) fsum;
    den=(441.0f*f2sum-fsum*fsum);
    *recip= (float)((den!=0.0)?1.0/den:0.0);


}

/* Lay out the image in the patch, computing norm and
*/
inline void db_SignedSquareNormCorr11x11_PreAlign_u(short *patch,const unsigned char * const *f_img,int x_f,int y_f,float *sum,float *recip)
{
    float den;
    int f2sum,fsum;
    int xm_f=x_f-5;

#ifndef DB_USE_SSE2
    const unsigned char *pf;
    short f;

    pf=f_img[y_f-5]+xm_f;
    f= *pf++; f2sum=f*f;  fsum=f;  (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-4]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-3]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+3]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+4]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+5]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    //int xwi;
    //int ywi;
    //f2sum=0;
    //fsum=0;
    //for (int r=-5;r<=5;r++){
    //  ywi=y_f+r;
    //  for (int c=-5;c<=5;c++){
    //      xwi=x_f+c;
    //      f=f_img[ywi][xwi];
    //      f2sum+=f*f;
    //      fsum+=f;
    //      (*patch++)=f;
    //  }
    //}

    (*patch++)=0; (*patch++)=0; (*patch++)=0; (*patch++)=0; (*patch++)=0;
    (*patch++)=0; (*patch++)=0;
#else
    const unsigned char *pf0 =f_img[y_f-5]+xm_f;
    const unsigned char *pf1 =f_img[y_f-4]+xm_f;
    const unsigned char *pf2 =f_img[y_f-3]+xm_f;
    const unsigned char *pf3 =f_img[y_f-2]+xm_f;
    const unsigned char *pf4 =f_img[y_f-1]+xm_f;
    const unsigned char *pf5 =f_img[y_f  ]+xm_f;
    const unsigned char *pf6 =f_img[y_f+1]+xm_f;
    const unsigned char *pf7 =f_img[y_f+2]+xm_f;
    const unsigned char *pf8 =f_img[y_f+3]+xm_f;
    const unsigned char *pf9 =f_img[y_f+4]+xm_f;
    const unsigned char *pf10=f_img[y_f+5]+xm_f;

    /* pixel mask */
    const unsigned char pm[16] = {
        0xFF,0xFF,
        0xFF,0xFF,
        0xFF,0xFF,
        0,0,0,0,0,
        0,0,0,0,0};
    const unsigned char * pm_p = pm;

    _asm
    {
        mov         ecx,patch   /* load patch pointer */
        mov         ebx, pm_p   /* load pixel mask pointer */
        movdqu      xmm1,[ebx]  /* load pixel mask */

        pxor        xmm5,xmm5   /* set xmm5 to 0 accumulator for sum squares */
        pxor        xmm4,xmm4   /* set xmm4 to 0 accumulator for sum */
        pxor        xmm0,xmm0   /* set xmm0 to 0 */

        /* row 0 */
        mov         eax,pf0     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqa      [ecx+0*22],xmm7 /* move short values to patch */
        movdqa      [ecx+0*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 1 */
        mov         eax,pf1     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+1*22],xmm7 /* move short values to patch */
        movdqu      [ecx+1*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 2 */
        mov         eax,pf2     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+2*22],xmm7 /* move short values to patch */
        movdqu      [ecx+2*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 3 */
        mov         eax,pf3     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+3*22],xmm7 /* move short values to patch */
        movdqu      [ecx+3*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 4 */
        mov         eax,pf4     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+4*22],xmm7 /* move short values to patch */
        movdqu      [ecx+4*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 5 */
        mov         eax,pf5     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+5*22],xmm7 /* move short values to patch */
        movdqu      [ecx+5*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 6 */
        mov         eax,pf6     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+6*22],xmm7 /* move short values to patch */
        movdqu      [ecx+6*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 7 */
        mov         eax,pf7     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+7*22],xmm7 /* move short values to patch */
        movdqu      [ecx+7*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 8 */
        mov         eax,pf8     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqa      [ecx+8*22],xmm7 /* move short values to patch */
        movdqa      [ecx+8*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 9 */
        mov         eax,pf9     /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+9*22],xmm7 /* move short values to patch */
        movdqu      [ecx+9*22+16],xmm6  /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit uints into 16 bit uints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* row 10 */
        mov         eax,pf10    /* load image pointer */
        movdqu      xmm7,[eax]  /* load 16 pixels */
        movdqa      xmm6,xmm7

        punpcklbw   xmm7,xmm0   /* unpack low pixels (first 8)*/
        punpckhbw   xmm6,xmm0   /* unpack high pixels (last 8)*/

        pand        xmm6,xmm1   /* mask out pixels 12-16 */

        movdqu      [ecx+10*22],xmm7    /* move short values to patch */
        movdqu      [ecx+10*22+16],xmm6 /* move short values to patch */

        paddusw     xmm4,xmm7   /* accumulate sums */
        pmaddwd     xmm7,xmm7   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm7   /* accumulate sum squares */

        paddw       xmm4,xmm6   /* accumulate sums */
        pmaddwd     xmm6,xmm6   /* multiply 16 bit ints and add into 32 bit ints */
        paddd       xmm5,xmm6   /* accumulate sum squares */

        /* add up the sum squares */
        movhlps     xmm0,xmm5   /* high half to low half */
        paddd       xmm5,xmm0   /* add high to low */
        pshuflw     xmm0,xmm5, 0xE /* reshuffle */
        paddd       xmm5,xmm0   /* add remaining */
        movd        f2sum,xmm5

        /* add up the sum */
        movhlps     xmm0,xmm4
        paddw       xmm4,xmm0   /* halves added */
        pshuflw     xmm0,xmm4,0xE
        paddw       xmm4,xmm0   /* quarters added */
        pshuflw     xmm0,xmm4,0x1
        paddw       xmm4,xmm0   /* eighth added */
        movd        fsum, xmm4

        emms
    }

    fsum = fsum & 0xFFFF;

    patch[126] = 0;
    patch[127] = 0;
#endif /* DB_USE_SSE2 */

    *sum= (float) fsum;
    den=(121.0f*f2sum-fsum*fsum);
    *recip= (float)((den!=0.0)?1.0/den:0.0);
}

void AffineWarpPointOffset(float &r_w,float &c_w,double Hinv[9],int r,int c)
{
    r_w=(float)(Hinv[3]*c+Hinv[4]*r);
    c_w=(float)(Hinv[0]*c+Hinv[1]*r);
}



/*!
Prewarp the patches with given affine transform. For a given homogeneous point "x", "H*x" is
the warped point and for any displacement "d" in the warped image resulting in point "y", the
corresponding point in the original image is given by "Hinv*y", which can be simplified for affine H.
If "affine" is 1, then nearest neighbor method is used, else if it is 2, then
bilinear method is used.
 */
inline void db_SignedSquareNormCorr11x11_PreAlign_AffinePatchWarp_u(short *patch,const unsigned char * const *f_img,
                                                                    int xi,int yi,float *sum,float *recip,
                                                                    const double Hinv[9],int affine)
{
    float den;
    short f;
    int f2sum,fsum;

    f2sum=0;
    fsum=0;

    if (affine==1)
    {
        for (int r=0;r<11;r++){
            for (int c=0;c<11;c++){
                f=f_img[yi+AffineWarpPoint_NN_LUT_y[r][c]][xi+AffineWarpPoint_NN_LUT_x[r][c]];
                f2sum+=f*f;
                fsum+=f;
                (*patch++)=f;
            }
        }
    }
    else if (affine==2)
    {
        for (int r=0;r<11;r++){
            for (int c=0;c<11;c++){
                f=db_BilinearInterpolation(yi+AffineWarpPoint_BL_LUT_y[r][c]
                ,xi+AffineWarpPoint_BL_LUT_x[r][c],f_img);
                f2sum+=f*f;
                fsum+=f;
                (*patch++)=f;
            }
        }
    }



    (*patch++)=0; (*patch++)=0; (*patch++)=0; (*patch++)=0; (*patch++)=0;
    (*patch++)=0; (*patch++)=0;

    *sum= (float) fsum;
    den=(121.0f*f2sum-fsum*fsum);
    *recip= (float)((den!=0.0)?1.0/den:0.0);
}


inline float db_SignedSquareNormCorr11x11_Post_u(unsigned char **f_img,unsigned char **g_img,int x_f,int y_f,int x_g,int y_g,
                                                float fsum_gsum,float f_recip_g_recip)
{
    unsigned char *pf,*pg;
    int fgsum;
    float fg_corr;
    int xm_f,xm_g;

    xm_f=x_f-5;
    xm_g=x_g-5;

    pf=f_img[y_f-5]+xm_f; pg=g_img[y_g-5]+xm_g;
    fgsum=(*pf++)*(*pg++);  fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f-4]+xm_f; pg=g_img[y_g-4]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+4]+xm_f; pg=g_img[y_g+4]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+5]+xm_f; pg=g_img[y_g+5]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    fg_corr=121.0f*fgsum-fsum_gsum;
    if(fg_corr>=0.0) return(fg_corr*fg_corr*f_recip_g_recip);
    return(-fg_corr*fg_corr*f_recip_g_recip);
}

float db_SignedSquareNormCorr21x21Aligned_Post_s(const short *f_patch,const short *g_patch,float fsum_gsum,float f_recip_g_recip)
{
    float fgsum,fg_corr;

    fgsum= (float) db_ScalarProduct512_s(f_patch,g_patch);

    fg_corr=441.0f*fgsum-fsum_gsum;
    if(fg_corr>=0.0) return(fg_corr*fg_corr*f_recip_g_recip);
    return(-fg_corr*fg_corr*f_recip_g_recip);
}


float db_SignedSquareNormCorr11x11Aligned_Post_s(const short *f_patch,const short *g_patch,float fsum_gsum,float f_recip_g_recip)
{
    float fgsum,fg_corr;

    fgsum= (float) db_ScalarProduct128_s(f_patch,g_patch);

    fg_corr=121.0f*fgsum-fsum_gsum;
    if(fg_corr>=0.0) return(fg_corr*fg_corr*f_recip_g_recip);
    return(-fg_corr*fg_corr*f_recip_g_recip);
}

float db_SignedSquareNormCorr5x5Aligned_Post_s(const short *f_patch,const short *g_patch,float fsum_gsum,float f_recip_g_recip)
{
    float fgsum,fg_corr;

    fgsum= (float) db_ScalarProduct32_s(f_patch,g_patch);

    fg_corr=25.0f*fgsum-fsum_gsum;
    if(fg_corr>=0.0) return(fg_corr*fg_corr*f_recip_g_recip);
    return(-fg_corr*fg_corr*f_recip_g_recip);
}


inline float db_SignedSquareNormCorr15x15_u(unsigned char **f_img,unsigned char **g_img,int x_f,int y_f,int x_g,int y_g)
{
    unsigned char *pf,*pg;
    float f,g,fgsum,f2sum,g2sum,fsum,gsum,fg_corr,den;
    int xm_f,xm_g;

    xm_f=x_f-7;
    xm_g=x_g-7;
    fgsum=0.0; f2sum=0.0; g2sum=0.0; fsum=0.0; gsum=0.0;

    pf=f_img[y_f-7]+xm_f; pg=g_img[y_g-7]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-6]+xm_f; pg=g_img[y_g-6]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-5]+xm_f; pg=g_img[y_g-5]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-4]+xm_f; pg=g_img[y_g-4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+4]+xm_f; pg=g_img[y_g+4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+5]+xm_f; pg=g_img[y_g+5]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+6]+xm_f; pg=g_img[y_g+6]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+7]+xm_f; pg=g_img[y_g+7]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    fg_corr=225.0f*fgsum-fsum*gsum;
    den=(225.0f*f2sum-fsum*fsum)*(225.0f*g2sum-gsum*gsum);
    if(den!=0.0)
    {
        if(fg_corr>=0.0) return(fg_corr*fg_corr/den);
        return(-fg_corr*fg_corr/den);
    }
    return(0.0);
}

inline float db_SignedSquareNormCorr7x7_f(float **f_img,float **g_img,int x_f,int y_f,int x_g,int y_g)
{
    float f,g,*pf,*pg,fgsum,f2sum,g2sum,fsum,gsum,fg_corr,den;
    int xm_f,xm_g;

    xm_f=x_f-3;
    xm_g=x_g-3;
    fgsum=0.0; f2sum=0.0; g2sum=0.0; fsum=0.0; gsum=0.0;

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    fg_corr=49.0f*fgsum-fsum*gsum;
    den=(49.0f*f2sum-fsum*fsum)*(49.0f*g2sum-gsum*gsum);
    if(den!=0.0)
    {
        if(fg_corr>=0.0) return(fg_corr*fg_corr/den);
        return(-fg_corr*fg_corr/den);
    }
    return(0.0);
}

inline float db_SignedSquareNormCorr9x9_f(float **f_img,float **g_img,int x_f,int y_f,int x_g,int y_g)
{
    float f,g,*pf,*pg,fgsum,f2sum,g2sum,fsum,gsum,fg_corr,den;
    int xm_f,xm_g;

    xm_f=x_f-4;
    xm_g=x_g-4;
    fgsum=0.0; f2sum=0.0; g2sum=0.0; fsum=0.0; gsum=0.0;

    pf=f_img[y_f-4]+xm_f; pg=g_img[y_g-4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+4]+xm_f; pg=g_img[y_g+4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    fg_corr=81.0f*fgsum-fsum*gsum;
    den=(81.0f*f2sum-fsum*fsum)*(81.0f*g2sum-gsum*gsum);
    if(den!=0.0)
    {
        if(fg_corr>=0.0) return(fg_corr*fg_corr/den);
        return(-fg_corr*fg_corr/den);
    }
    return(0.0);
}

inline float db_SignedSquareNormCorr11x11_f(float **f_img,float **g_img,int x_f,int y_f,int x_g,int y_g)
{
    float *pf,*pg;
    float f,g,fgsum,f2sum,g2sum,fsum,gsum,fg_corr,den;
    int xm_f,xm_g;

    xm_f=x_f-5;
    xm_g=x_g-5;
    fgsum=0.0; f2sum=0.0; g2sum=0.0; fsum=0.0; gsum=0.0;

    pf=f_img[y_f-5]+xm_f; pg=g_img[y_g-5]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-4]+xm_f; pg=g_img[y_g-4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+4]+xm_f; pg=g_img[y_g+4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+5]+xm_f; pg=g_img[y_g+5]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    fg_corr=121.0f*fgsum-fsum*gsum;
    den=(121.0f*f2sum-fsum*fsum)*(121.0f*g2sum-gsum*gsum);
    if(den!=0.0)
    {
        if(fg_corr>=0.0) return(fg_corr*fg_corr/den);
        return(-fg_corr*fg_corr/den);
    }
    return(0.0);
}

inline void db_SignedSquareNormCorr11x11_Pre_f(float **f_img,int x_f,int y_f,float *sum,float *recip)
{
    float *pf,den;
    float f,f2sum,fsum;
    int xm_f;

    xm_f=x_f-5;

    pf=f_img[y_f-5]+xm_f;
    f= *pf++; f2sum=f*f;  fsum=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f-4]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f-3]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f-2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f-1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+3]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+4]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    pf=f_img[y_f+5]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf++; f2sum+=f*f; fsum+=f;
    f= *pf;   f2sum+=f*f; fsum+=f;

    *sum=fsum;
    den=(121.0f*f2sum-fsum*fsum);
    *recip= (float) ((den!=0.0)?1.0/den:0.0);
}

inline void db_SignedSquareNormCorr11x11_PreAlign_f(float *patch,const float * const *f_img,int x_f,int y_f,float *sum,float *recip)
{
    const float *pf;
    float den,f,f2sum,fsum;
    int xm_f;

    xm_f=x_f-5;

    pf=f_img[y_f-5]+xm_f;
    f= *pf++; f2sum=f*f;  fsum=f;  (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-4]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-3]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f-1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+1]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+2]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+3]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+4]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    pf=f_img[y_f+5]+xm_f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf++; f2sum+=f*f; fsum+=f; (*patch++)=f;
    f= *pf;   f2sum+=f*f; fsum+=f; (*patch++)=f;

    (*patch++)=0.0; (*patch++)=0.0; (*patch++)=0.0; (*patch++)=0.0; (*patch++)=0.0;
    (*patch++)=0.0; (*patch++)=0.0;

    *sum=fsum;
    den=(121.0f*f2sum-fsum*fsum);
    *recip= (float) ((den!=0.0)?1.0/den:0.0);
}

inline float db_SignedSquareNormCorr11x11_Post_f(float **f_img,float **g_img,int x_f,int y_f,int x_g,int y_g,
                                                float fsum_gsum,float f_recip_g_recip)
{
    float *pf,*pg;
    float fgsum,fg_corr;
    int xm_f,xm_g;

    xm_f=x_f-5;
    xm_g=x_g-5;

    pf=f_img[y_f-5]+xm_f; pg=g_img[y_g-5]+xm_g;
    fgsum=(*pf++)*(*pg++);  fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f-4]+xm_f; pg=g_img[y_g-4]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+4]+xm_f; pg=g_img[y_g+4]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    pf=f_img[y_f+5]+xm_f; pg=g_img[y_g+5]+xm_g;
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++); fgsum+=(*pf++)*(*pg++);
    fgsum+=(*pf++)*(*pg++); fgsum+=(*pf)*(*pg);

    fg_corr=121.0f*fgsum-fsum_gsum;
    if(fg_corr>=0.0) return(fg_corr*fg_corr*f_recip_g_recip);
    return(-fg_corr*fg_corr*f_recip_g_recip);
}

inline float db_SignedSquareNormCorr11x11Aligned_Post_f(const float *f_patch,const float *g_patch,float fsum_gsum,float f_recip_g_recip)
{
    float fgsum,fg_corr;

    fgsum=db_ScalarProduct128Aligned16_f(f_patch,g_patch);

    fg_corr=121.0f*fgsum-fsum_gsum;
    if(fg_corr>=0.0) return(fg_corr*fg_corr*f_recip_g_recip);
    return(-fg_corr*fg_corr*f_recip_g_recip);
}

inline float db_SignedSquareNormCorr15x15_f(float **f_img,float **g_img,int x_f,int y_f,int x_g,int y_g)
{
    float *pf,*pg;
    float f,g,fgsum,f2sum,g2sum,fsum,gsum,fg_corr,den;
    int xm_f,xm_g;

    xm_f=x_f-7;
    xm_g=x_g-7;
    fgsum=0.0; f2sum=0.0; g2sum=0.0; fsum=0.0; gsum=0.0;

    pf=f_img[y_f-7]+xm_f; pg=g_img[y_g-7]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-6]+xm_f; pg=g_img[y_g-6]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-5]+xm_f; pg=g_img[y_g-5]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-4]+xm_f; pg=g_img[y_g-4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-3]+xm_f; pg=g_img[y_g-3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-2]+xm_f; pg=g_img[y_g-2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f-1]+xm_f; pg=g_img[y_g-1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f]+xm_f; pg=g_img[y_g]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+1]+xm_f; pg=g_img[y_g+1]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+2]+xm_f; pg=g_img[y_g+2]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+3]+xm_f; pg=g_img[y_g+3]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+4]+xm_f; pg=g_img[y_g+4]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+5]+xm_f; pg=g_img[y_g+5]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+6]+xm_f; pg=g_img[y_g+6]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    pf=f_img[y_f+7]+xm_f; pg=g_img[y_g+7]+xm_g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf++; g= *pg++; fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;
    f= *pf;   g= *pg;   fgsum+=f*g; f2sum+=f*f; g2sum+=g*g; fsum+=f; gsum+=g;

    fg_corr=225.0f*fgsum-fsum*gsum;
    den=(225.0f*f2sum-fsum*fsum)*(225.0f*g2sum-gsum*gsum);
    if(den!=0.0)
    {
        if(fg_corr>=0.0) return(fg_corr*fg_corr/den);
        return(-fg_corr*fg_corr/den);
    }
    return(0.0);
}

db_Bucket_f** db_AllocBuckets_f(int nr_h,int nr_v,int bd)
{
    int i,j;
    db_Bucket_f **bp,*b;

    b=new db_Bucket_f [(nr_h+2)*(nr_v+2)];
    bp=new db_Bucket_f* [(nr_v+2)];
    bp=bp+1;
    for(i= -1;i<=nr_v;i++)
    {
        bp[i]=b+1+(nr_h+2)*(i+1);
        for(j= -1;j<=nr_h;j++)
        {
            bp[i][j].ptr=new db_PointInfo_f [bd];
        }
    }

    return(bp);
}

db_Bucket_u** db_AllocBuckets_u(int nr_h,int nr_v,int bd)
{
    int i,j;
    db_Bucket_u **bp,*b;

    b=new db_Bucket_u [(nr_h+2)*(nr_v+2)];
    bp=new db_Bucket_u* [(nr_v+2)];
    bp=bp+1;
    for(i= -1;i<=nr_v;i++)
    {
        bp[i]=b+1+(nr_h+2)*(i+1);
        for(j= -1;j<=nr_h;j++)
        {
            bp[i][j].ptr=new db_PointInfo_u [bd];
        }
    }

    return(bp);
}

void db_FreeBuckets_f(db_Bucket_f **bp,int nr_h,int nr_v)
{
    int i,j;

    for(i= -1;i<=nr_v;i++) for(j= -1;j<=nr_h;j++)
    {
        delete [] bp[i][j].ptr;
    }
    delete [] (bp[-1]-1);
    delete [] (bp-1);
}

void db_FreeBuckets_u(db_Bucket_u **bp,int nr_h,int nr_v)
{
    int i,j;

    for(i= -1;i<=nr_v;i++) for(j= -1;j<=nr_h;j++)
    {
        delete [] bp[i][j].ptr;
    }
    delete [] (bp[-1]-1);
    delete [] (bp-1);
}

void db_EmptyBuckets_f(db_Bucket_f **bp,int nr_h,int nr_v)
{
    int i,j;
    for(i= -1;i<=nr_v;i++) for(j= -1;j<=nr_h;j++) bp[i][j].nr=0;
}

void db_EmptyBuckets_u(db_Bucket_u **bp,int nr_h,int nr_v)
{
    int i,j;
    for(i= -1;i<=nr_v;i++) for(j= -1;j<=nr_h;j++) bp[i][j].nr=0;
}

float* db_FillBuckets_f(float *patch_space,const float * const *f_img,db_Bucket_f **bp,int bw,int bh,int nr_h,int nr_v,int bd,const double *x,const double *y,int nr_corners)
{
    int i,xi,yi,xpos,ypos,nr;
    db_Bucket_f *br;
    db_PointInfo_f *pir;

    db_EmptyBuckets_f(bp,nr_h,nr_v);
    for(i=0;i<nr_corners;i++)
    {
        xi=(int) x[i];
        yi=(int) y[i];
        xpos=xi/bw;
        ypos=yi/bh;
        if(xpos>=0 && xpos<nr_h && ypos>=0 && ypos<nr_v)
        {
            br=&bp[ypos][xpos];
            nr=br->nr;
            if(nr<bd)
            {
                pir=&(br->ptr[nr]);
                pir->x=xi;
                pir->y=yi;
                pir->id=i;
                pir->pir=0;
                pir->patch=patch_space;
                br->nr=nr+1;

                db_SignedSquareNormCorr11x11_PreAlign_f(patch_space,f_img,xi,yi,&(pir->sum),&(pir->recip));
                patch_space+=128;
            }
        }
    }
    return(patch_space);
}

short* db_FillBuckets_u(short *patch_space,const unsigned char * const *f_img,db_Bucket_u **bp,int bw,int bh,int nr_h,int nr_v,int bd,const double *x,const double *y,int nr_corners,int use_smaller_matching_window, int use_21)
{
    int i,xi,yi,xpos,ypos,nr;
    db_Bucket_u *br;
    db_PointInfo_u *pir;

    db_EmptyBuckets_u(bp,nr_h,nr_v);
    for(i=0;i<nr_corners;i++)
    {
        xi=(int)db_roundi(x[i]);
        yi=(int)db_roundi(y[i]);
        xpos=xi/bw;
        ypos=yi/bh;
        if(xpos>=0 && xpos<nr_h && ypos>=0 && ypos<nr_v)
        {
            br=&bp[ypos][xpos];
            nr=br->nr;
            if(nr<bd)
            {
                pir=&(br->ptr[nr]);
                pir->x=xi;
                pir->y=yi;
                pir->id=i;
                pir->pir=0;
                pir->patch=patch_space;
                br->nr=nr+1;

                if(use_21)
                {
                    db_SignedSquareNormCorr21x21_PreAlign_u(patch_space,f_img,xi,yi,&(pir->sum),&(pir->recip));
                    patch_space+=512;
                }
                else
                {
                if(!use_smaller_matching_window)
                {
                    db_SignedSquareNormCorr11x11_PreAlign_u(patch_space,f_img,xi,yi,&(pir->sum),&(pir->recip));
                    patch_space+=128;
                }
                else
                {
                    db_SignedSquareNormCorr5x5_PreAlign_u(patch_space,f_img,xi,yi,&(pir->sum),&(pir->recip));
                    patch_space+=32;
                }
                }
            }
        }
    }
    return(patch_space);
}



float* db_FillBucketsPrewarped_f(float *patch_space,const float *const *f_img,db_Bucket_f **bp,int bw,int bh,int nr_h,int nr_v,int bd,const double *x,const double *y,int nr_corners,const double H[9])
{
    int i,xi,yi,xpos,ypos,nr,wxi,wyi;
    db_Bucket_f *br;
    db_PointInfo_f *pir;
    double xd[2],wx[2];

    db_EmptyBuckets_f(bp,nr_h,nr_v);
    for(i=0;i<nr_corners;i++)
    {
        xd[0]=x[i];
        xd[1]=y[i];
        xi=(int) xd[0];
        yi=(int) xd[1];
        db_ImageHomographyInhomogenous(wx,H,xd);
        wxi=(int) wx[0];
        wyi=(int) wx[1];

        xpos=((wxi+bw)/bw)-1;
        ypos=((wyi+bh)/bh)-1;
        if(xpos>= -1 && xpos<=nr_h && ypos>= -1 && ypos<=nr_v)
        {
            br=&bp[ypos][xpos];
            nr=br->nr;
            if(nr<bd)
            {
                pir=&(br->ptr[nr]);
                pir->x=wxi;
                pir->y=wyi;
                pir->id=i;
                pir->pir=0;
                pir->patch=patch_space;
                br->nr=nr+1;

                db_SignedSquareNormCorr11x11_PreAlign_f(patch_space,f_img,xi,yi,&(pir->sum),&(pir->recip));
                patch_space+=128;
            }
        }
    }
    return(patch_space);
}

short* db_FillBucketsPrewarped_u(short *patch_space,const unsigned char * const *f_img,db_Bucket_u **bp,
                                 int bw,int bh,int nr_h,int nr_v,int bd,const double *x,const double *y,
                                 int nr_corners,const double H[9])
{
    int i,xi,yi,xpos,ypos,nr,wxi,wyi;
    db_Bucket_u *br;
    db_PointInfo_u *pir;
    double xd[2],wx[2];

    db_EmptyBuckets_u(bp,nr_h,nr_v);
    for(i=0;i<nr_corners;i++)
    {
        xd[0]=x[i];
        xd[1]=y[i];
        xi=(int) db_roundi(xd[0]);
        yi=(int) db_roundi(xd[1]);
        db_ImageHomographyInhomogenous(wx,H,xd);
        wxi=(int) wx[0];
        wyi=(int) wx[1];

        xpos=((wxi+bw)/bw)-1;
        ypos=((wyi+bh)/bh)-1;
        if(xpos>= -1 && xpos<=nr_h && ypos>= -1 && ypos<=nr_v)
        {
            br=&bp[ypos][xpos];
            nr=br->nr;
            if(nr<bd)
            {
                pir=&(br->ptr[nr]);
                pir->x=wxi;
                pir->y=wyi;
                pir->id=i;
                pir->pir=0;
                pir->patch=patch_space;
                br->nr=nr+1;

                db_SignedSquareNormCorr11x11_PreAlign_u(patch_space,f_img,xi,yi,&(pir->sum),&(pir->recip));
                patch_space+=128;
            }
        }
    }
    return(patch_space);
}



short* db_FillBucketsPrewarpedAffine_u(short *patch_space,const unsigned char * const *f_img,db_Bucket_u **bp,
                                 int bw,int bh,int nr_h,int nr_v,int bd,const double *x,const double *y,
                                 int nr_corners,const double H[9],const double Hinv[9],const int warpboundsp[4],
                                 int affine)
{
    int i,xi,yi,xpos,ypos,nr,wxi,wyi;
    db_Bucket_u *br;
    db_PointInfo_u *pir;
    double xd[2],wx[2];

    db_EmptyBuckets_u(bp,nr_h,nr_v);
    for(i=0;i<nr_corners;i++)
    {
        xd[0]=x[i];
        xd[1]=y[i];
        xi=(int) db_roundi(xd[0]);
        yi=(int) db_roundi(xd[1]);
        db_ImageHomographyInhomogenous(wx,H,xd);
        wxi=(int) wx[0];
        wyi=(int) wx[1];

        xpos=((wxi+bw)/bw)-1;
        ypos=((wyi+bh)/bh)-1;


        if (xpos>= -1 && xpos<=nr_h && ypos>= -1 && ypos<=nr_v)
        {
            if( xi>warpboundsp[0] && xi<warpboundsp[1] && yi>warpboundsp[2] && yi<warpboundsp[3])
            {

                br=&bp[ypos][xpos];
                nr=br->nr;
                if(nr<bd)
                {
                    pir=&(br->ptr[nr]);
                    pir->x=wxi;
                    pir->y=wyi;
                    pir->id=i;
                    pir->pir=0;
                    pir->patch=patch_space;
                    br->nr=nr+1;

                    db_SignedSquareNormCorr11x11_PreAlign_AffinePatchWarp_u(patch_space,f_img,xi,yi,&(pir->sum),&(pir->recip),Hinv,affine);
                    patch_space+=128;
                }
            }
        }
    }
    return(patch_space);
}



inline void db_MatchPointPair_f(db_PointInfo_f *pir_l,db_PointInfo_f *pir_r,
                            unsigned long kA,unsigned long kB)
{
    int x_l,y_l,x_r,y_r,xm,ym;
    double score;

    x_l=pir_l->x;
    y_l=pir_l->y;
    x_r=pir_r->x;
    y_r=pir_r->y;
    xm=x_l-x_r;
    ym=y_l-y_r;
    /*Check if disparity is within the maximum disparity
    with the formula xm^2*256+ym^2*kA<kB
    where kA=256*w^2/h^2
    and   kB=256*max_disp^2*w^2*/
    if(((xm*xm)<<8)+ym*ym*kA<kB)
    {
        /*Correlate*/
        score=db_SignedSquareNormCorr11x11Aligned_Post_f(pir_l->patch,pir_r->patch,
            (pir_l->sum)*(pir_r->sum),
            (pir_l->recip)*(pir_r->recip));

        if((!(pir_l->pir)) || (score>pir_l->s))
        {
            /*Update left corner*/
            pir_l->s=score;
            pir_l->pir=pir_r;
        }
        if((!(pir_r->pir)) || (score>pir_r->s))
        {
            /*Update right corner*/
            pir_r->s=score;
            pir_r->pir=pir_l;
        }
    }
}

inline void db_MatchPointPair_u(db_PointInfo_u *pir_l,db_PointInfo_u *pir_r,
                            unsigned long kA,unsigned long kB, unsigned int rect_window,bool use_smaller_matching_window, int use_21)
{
    int xm,ym;
    double score;
    bool compute_score;


    if( rect_window )
        compute_score = ((unsigned)db_absi(pir_l->x - pir_r->x)<kA && (unsigned)db_absi(pir_l->y - pir_r->y)<kB);
    else
    {   /*Check if disparity is within the maximum disparity
        with the formula xm^2*256+ym^2*kA<kB
        where kA=256*w^2/h^2
        and   kB=256*max_disp^2*w^2*/
        xm= pir_l->x - pir_r->x;
        ym= pir_l->y - pir_r->y;
        compute_score = ((xm*xm)<<8)+ym*ym*kA < kB;
    }

    if ( compute_score )
    {
        if(use_21)
        {
            score=db_SignedSquareNormCorr21x21Aligned_Post_s(pir_l->patch,pir_r->patch,
                (pir_l->sum)*(pir_r->sum),
                (pir_l->recip)*(pir_r->recip));
        }
        else
        {
        /*Correlate*/
        if(!use_smaller_matching_window)
        {
            score=db_SignedSquareNormCorr11x11Aligned_Post_s(pir_l->patch,pir_r->patch,
                (pir_l->sum)*(pir_r->sum),
                (pir_l->recip)*(pir_r->recip));
        }
        else
        {
            score=db_SignedSquareNormCorr5x5Aligned_Post_s(pir_l->patch,pir_r->patch,
                (pir_l->sum)*(pir_r->sum),
                (pir_l->recip)*(pir_r->recip));
        }
        }

        if((!(pir_l->pir)) || (score>pir_l->s))
        {
            /*Update left corner*/
            pir_l->s=score;
            pir_l->pir=pir_r;
        }
        if((!(pir_r->pir)) || (score>pir_r->s))
        {
            /*Update right corner*/
            pir_r->s=score;
            pir_r->pir=pir_l;
        }
    }
}

inline void db_MatchPointAgainstBucket_f(db_PointInfo_f *pir_l,db_Bucket_f *b_r,
                                       unsigned long kA,unsigned long kB)
{
    int p_r,nr;
    db_PointInfo_f *pir_r;

    nr=b_r->nr;
    pir_r=b_r->ptr;
    for(p_r=0;p_r<nr;p_r++) db_MatchPointPair_f(pir_l,pir_r+p_r,kA,kB);
}

inline void db_MatchPointAgainstBucket_u(db_PointInfo_u *pir_l,db_Bucket_u *b_r,
                                       unsigned long kA,unsigned long kB,int rect_window, bool use_smaller_matching_window, int use_21)
{
    int p_r,nr;
    db_PointInfo_u *pir_r;

    nr=b_r->nr;
    pir_r=b_r->ptr;

    for(p_r=0;p_r<nr;p_r++) db_MatchPointPair_u(pir_l,pir_r+p_r,kA,kB, rect_window, use_smaller_matching_window, use_21);

}

void db_MatchBuckets_f(db_Bucket_f **bp_l,db_Bucket_f **bp_r,int nr_h,int nr_v,
                     unsigned long kA,unsigned long kB)
{
    int i,j,k,a,b,br_nr;
    db_Bucket_f *br;
    db_PointInfo_f *pir_l;

    /*For all buckets*/
    for(i=0;i<nr_v;i++) for(j=0;j<nr_h;j++)
    {
        br=&bp_l[i][j];
        br_nr=br->nr;
        /*For all points in bucket*/
        for(k=0;k<br_nr;k++)
        {
            pir_l=br->ptr+k;
            for(a=i-1;a<=i+1;a++)
            {
                for(b=j-1;b<=j+1;b++)
                {
                    db_MatchPointAgainstBucket_f(pir_l,&bp_r[a][b],kA,kB);
                }
            }
        }
    }
}

void db_MatchBuckets_u(db_Bucket_u **bp_l,db_Bucket_u **bp_r,int nr_h,int nr_v,
                     unsigned long kA,unsigned long kB,int rect_window,bool use_smaller_matching_window, int use_21)
{
    int i,j,k,a,b,br_nr;
    db_Bucket_u *br;
    db_PointInfo_u *pir_l;

    /*For all buckets*/
    for(i=0;i<nr_v;i++) for(j=0;j<nr_h;j++)
    {
        br=&bp_l[i][j];
        br_nr=br->nr;
        /*For all points in bucket*/
        for(k=0;k<br_nr;k++)
        {
            pir_l=br->ptr+k;
            for(a=i-1;a<=i+1;a++)
            {
                for(b=j-1;b<=j+1;b++)
                {
                    db_MatchPointAgainstBucket_u(pir_l,&bp_r[a][b],kA,kB,rect_window,use_smaller_matching_window, use_21);
                }
            }
        }
    }
}

void db_CollectMatches_f(db_Bucket_f **bp_l,int nr_h,int nr_v,unsigned long target,int *id_l,int *id_r,int *nr_matches)
{
    int i,j,k,br_nr;
    unsigned long count;
    db_Bucket_f *br;
    db_PointInfo_f *pir,*pir2;

    count=0;
    /*For all buckets*/
    for(i=0;i<nr_v;i++) for(j=0;j<nr_h;j++)
    {
        br=&bp_l[i][j];
        br_nr=br->nr;
        /*For all points in bucket*/
        for(k=0;k<br_nr;k++)
        {
            pir=br->ptr+k;
            pir2=pir->pir;
            if(pir2)
            {
                /*This point has a best match*/
                if((pir2->pir)==pir)
                {
                    /*We have a mutually consistent match*/
                    if(count<target)
                    {
                        id_l[count]=pir->id;
                        id_r[count]=pir2->id;
                        count++;
                    }
                }
            }
        }
    }
    *nr_matches=count;
}

void db_CollectMatches_u(db_Bucket_u **bp_l,int nr_h,int nr_v,unsigned long target,int *id_l,int *id_r,int *nr_matches)
{
    int i,j,k,br_nr;
    unsigned long count;
    db_Bucket_u *br;
    db_PointInfo_u *pir,*pir2;

    count=0;
    /*For all buckets*/
    for(i=0;i<nr_v;i++) for(j=0;j<nr_h;j++)
    {
        br=&bp_l[i][j];
        br_nr=br->nr;
        /*For all points in bucket*/
        for(k=0;k<br_nr;k++)
        {
            pir=br->ptr+k;
            pir2=pir->pir;
            if(pir2)
            {
                /*This point has a best match*/
                if((pir2->pir)==pir)
                {
                    /*We have a mutually consistent match*/
                    if(count<target)
                    {
                        id_l[count]=pir->id;
                        id_r[count]=pir2->id;
                        count++;
                    }
                }
            }
        }
    }
    *nr_matches=count;
}

db_Matcher_f::db_Matcher_f()
{
    m_w=0; m_h=0;
}

db_Matcher_f::~db_Matcher_f()
{
    Clean();
}

void db_Matcher_f::Clean()
{
    if(m_w)
    {
        /*Free buckets*/
        db_FreeBuckets_f(m_bp_l,m_nr_h,m_nr_v);
        db_FreeBuckets_f(m_bp_r,m_nr_h,m_nr_v);
        /*Free space for patch layouts*/
        delete [] m_patch_space;
    }
    m_w=0; m_h=0;
}

unsigned long db_Matcher_f::Init(int im_width,int im_height,double max_disparity,int target_nr_corners)
{
    Clean();
    m_w=im_width;
    m_h=im_height;
    m_bw=db_maxi(1,(int) (max_disparity*((double)im_width)));
    m_bh=db_maxi(1,(int) (max_disparity*((double)im_height)));
    m_nr_h=1+(im_width-1)/m_bw;
    m_nr_v=1+(im_height-1)/m_bh;
    m_bd=db_maxi(1,(int)(((double)target_nr_corners)*
        max_disparity*max_disparity));
    m_target=target_nr_corners;
    m_kA=(long)(256.0*((double)(m_w*m_w))/((double)(m_h*m_h)));
    m_kB=(long)(256.0*max_disparity*max_disparity*((double)(m_w*m_w)));

    /*Alloc bucket structure*/
    m_bp_l=db_AllocBuckets_f(m_nr_h,m_nr_v,m_bd);
    m_bp_r=db_AllocBuckets_f(m_nr_h,m_nr_v,m_bd);

    /*Alloc 16byte-aligned space for patch layouts*/
    m_patch_space=new float [2*(m_nr_h+2)*(m_nr_v+2)*m_bd*128+16];
    m_aligned_patch_space=db_AlignPointer_f(m_patch_space,16);

    return(m_target);
}

void db_Matcher_f::Match(const float * const *l_img,const float * const *r_img,
        const double *x_l,const double *y_l,int nr_l,const double *x_r,const double *y_r,int nr_r,
        int *id_l,int *id_r,int *nr_matches,const double H[9])
{
    float *ps;

    /*Insert the corners into bucket structure*/
    ps=db_FillBuckets_f(m_aligned_patch_space,l_img,m_bp_l,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,x_l,y_l,nr_l);
    if(H==0) db_FillBuckets_f(ps,r_img,m_bp_r,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,x_r,y_r,nr_r);
    else db_FillBucketsPrewarped_f(ps,r_img,m_bp_r,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,x_r,y_r,nr_r,H);

    /*Compute all the necessary match scores*/
    db_MatchBuckets_f(m_bp_l,m_bp_r,m_nr_h,m_nr_v,m_kA,m_kB);

    /*Collect the correspondences*/
    db_CollectMatches_f(m_bp_l,m_nr_h,m_nr_v,m_target,id_l,id_r,nr_matches);
}

db_Matcher_u::db_Matcher_u()
{
    m_w=0; m_h=0;
    m_rect_window = 0;
    m_bw=m_bh=m_nr_h=m_nr_v=m_bd=m_target=0;
    m_bp_l=m_bp_r=0;
    m_patch_space=m_aligned_patch_space=0;
}

db_Matcher_u::db_Matcher_u(const db_Matcher_u& cm)
{
    Init(cm.m_w, cm.m_h, cm.m_max_disparity, cm.m_target, cm.m_max_disparity_v);
}

db_Matcher_u& db_Matcher_u::operator= (const db_Matcher_u& cm)
{
    if ( this == &cm ) return *this;
    Init(cm.m_w, cm.m_h, cm.m_max_disparity, cm.m_target, cm.m_max_disparity_v);
    return *this;
}


db_Matcher_u::~db_Matcher_u()
{
    Clean();
}

void db_Matcher_u::Clean()
{
    if(m_w)
    {
        /*Free buckets*/
        db_FreeBuckets_u(m_bp_l,m_nr_h,m_nr_v);
        db_FreeBuckets_u(m_bp_r,m_nr_h,m_nr_v);
        /*Free space for patch layouts*/
        delete [] m_patch_space;
    }
    m_w=0; m_h=0;
}


unsigned long db_Matcher_u::Init(int im_width,int im_height,double max_disparity,int target_nr_corners,
                                 double max_disparity_v, bool use_smaller_matching_window, int use_21)
{
    Clean();
    m_w=im_width;
    m_h=im_height;
    m_max_disparity=max_disparity;
    m_max_disparity_v=max_disparity_v;

    if ( max_disparity_v != DB_DEFAULT_NO_DISPARITY )
    {
        m_rect_window = 1;

        m_bw=db_maxi(1,(int)(max_disparity*((double)im_width)));
        m_bh=db_maxi(1,(int)(max_disparity_v*((double)im_height)));

        m_bd=db_maxi(1,(int)(((double)target_nr_corners)*max_disparity*max_disparity_v));

        m_kA=(int)(max_disparity*m_w);
        m_kB=(int)(max_disparity_v*m_h);

    } else
    {
        m_bw=(int)db_maxi(1,(int)(max_disparity*((double)im_width)));
        m_bh=(int)db_maxi(1,(int)(max_disparity*((double)im_height)));

        m_bd=db_maxi(1,(int)(((double)target_nr_corners)*max_disparity*max_disparity));

        m_kA=(long)(256.0*((double)(m_w*m_w))/((double)(m_h*m_h)));
        m_kB=(long)(256.0*max_disparity*max_disparity*((double)(m_w*m_w)));
    }

    m_nr_h=1+(im_width-1)/m_bw;
    m_nr_v=1+(im_height-1)/m_bh;

    m_target=target_nr_corners;

    /*Alloc bucket structure*/
    m_bp_l=db_AllocBuckets_u(m_nr_h,m_nr_v,m_bd);
    m_bp_r=db_AllocBuckets_u(m_nr_h,m_nr_v,m_bd);

    m_use_smaller_matching_window = use_smaller_matching_window;
    m_use_21 = use_21;

    if(m_use_21)
    {
        /*Alloc 64byte-aligned space for patch layouts*/
        m_patch_space=new short [2*(m_nr_h+2)*(m_nr_v+2)*m_bd*512+64];
        m_aligned_patch_space=db_AlignPointer_s(m_patch_space,64);
    }
    else
    {
    if(!m_use_smaller_matching_window)
    {
        /*Alloc 16byte-aligned space for patch layouts*/
        m_patch_space=new short [2*(m_nr_h+2)*(m_nr_v+2)*m_bd*128+16];
        m_aligned_patch_space=db_AlignPointer_s(m_patch_space,16);
    }
    else
    {
        /*Alloc 4byte-aligned space for patch layouts*/
        m_patch_space=new short [2*(m_nr_h+2)*(m_nr_v+2)*m_bd*32+4];
        m_aligned_patch_space=db_AlignPointer_s(m_patch_space,4);
    }
    }

    return(m_target);
}

void db_Matcher_u::Match(const unsigned char * const *l_img,const unsigned char * const *r_img,
        const double *x_l,const double *y_l,int nr_l,const double *x_r,const double *y_r,int nr_r,
        int *id_l,int *id_r,int *nr_matches,const double H[9],int affine)
{
    short *ps;

    /*Insert the corners into bucket structure*/
    ps=db_FillBuckets_u(m_aligned_patch_space,l_img,m_bp_l,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,x_l,y_l,nr_l,m_use_smaller_matching_window,m_use_21);
    if(H==0)
        db_FillBuckets_u(ps,r_img,m_bp_r,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,x_r,y_r,nr_r,m_use_smaller_matching_window,m_use_21);
    else
    {
        if (affine)
        {
            double Hinv[9];
            db_InvertAffineTransform(Hinv,H);
            float r_w, c_w;
            float stretch_x[2];
            float stretch_y[2];
            AffineWarpPointOffset(r_w,c_w,Hinv, 5,5);
            stretch_x[0]=db_absf(c_w);stretch_y[0]=db_absf(r_w);
            AffineWarpPointOffset(r_w,c_w,Hinv, 5,-5);
            stretch_x[1]=db_absf(c_w);stretch_y[1]=db_absf(r_w);
            int max_stretxh_x=(int) (db_maxd(stretch_x[0],stretch_x[1]));
            int max_stretxh_y=(int) (db_maxd(stretch_y[0],stretch_y[1]));
            int warpbounds[4]={max_stretxh_x,m_w-1-max_stretxh_x,max_stretxh_y,m_h-1-max_stretxh_y};

            for (int r=-5;r<=5;r++){
                for (int c=-5;c<=5;c++){
                    AffineWarpPointOffset(r_w,c_w,Hinv,r,c);
                    AffineWarpPoint_BL_LUT_y[r+5][c+5]=r_w;
                    AffineWarpPoint_BL_LUT_x[r+5][c+5]=c_w;

                    AffineWarpPoint_NN_LUT_y[r+5][c+5]=db_roundi(r_w);
                    AffineWarpPoint_NN_LUT_x[r+5][c+5]=db_roundi(c_w);

                }
            }

            db_FillBucketsPrewarpedAffine_u(ps,r_img,m_bp_r,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,
                x_r,y_r,nr_r,H,Hinv,warpbounds,affine);
        }
        else
            db_FillBucketsPrewarped_u(ps,r_img,m_bp_r,m_bw,m_bh,m_nr_h,m_nr_v,m_bd,x_r,y_r,nr_r,H);
    }


    /*Compute all the necessary match scores*/
    db_MatchBuckets_u(m_bp_l,m_bp_r,m_nr_h,m_nr_v,m_kA,m_kB, m_rect_window,m_use_smaller_matching_window,m_use_21);

    /*Collect the correspondences*/
    db_CollectMatches_u(m_bp_l,m_nr_h,m_nr_v,m_target,id_l,id_r,nr_matches);
}

int db_Matcher_u::IsAllocated()
{
    return (int)(m_w != 0);
}
