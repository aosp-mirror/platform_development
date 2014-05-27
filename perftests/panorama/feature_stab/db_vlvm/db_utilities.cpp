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

/* $Id: db_utilities.cpp,v 1.4 2011/06/17 14:03:31 mbansal Exp $ */

#include "db_utilities.h"
#include <string.h>
#include <stdio.h>

float** db_SetupImageReferences_f(float *im,int w,int h)
{
    int i;
    float **img;
    assert(im);
    img=new float* [h];
    for(i=0;i<h;i++)
    {
        img[i]=im+w*i;
    }
    return(img);
}

unsigned char** db_SetupImageReferences_u(unsigned char *im,int w,int h)
{
    int i;
    unsigned char **img;

    assert(im);

    img=new unsigned char* [h];
    for(i=0;i<h;i++)
    {
        img[i]=im+w*i;
    }
    return(img);
}
float** db_AllocImage_f(int w,int h,int over_allocation)
{
    float **img,*im;

    im=new float [w*h+over_allocation];
    img=db_SetupImageReferences_f(im,w,h);

    return(img);
}

unsigned char** db_AllocImage_u(int w,int h,int over_allocation)
{
    unsigned char **img,*im;

    im=new unsigned char [w*h+over_allocation];
    img=db_SetupImageReferences_u(im,w,h);

    return(img);
}

void db_FreeImage_f(float **img,int h)
{
    delete [] (img[0]);
    delete [] img;
}

void db_FreeImage_u(unsigned char **img,int h)
{
    delete [] (img[0]);
    delete [] img;
}

// ----------------------------------------------------------------------------------------------------------- ;
//
// copy image (source to destination)
// ---> must be a 2D image array with the same image size
// ---> the size of the input and output images must be same
//
// ------------------------------------------------------------------------------------------------------------ ;
void db_CopyImage_u(unsigned char **d,const unsigned char * const *s, int w, int h, int over_allocation)
{
    int i;

    for (i=0;i<h;i++)
    {
        memcpy(d[i],s[i],w*sizeof(unsigned char));
    }

    memcpy(&d[h],&d[h],over_allocation);

}

inline void db_WarpImageLutFast_u(const unsigned char * const * src, unsigned char ** dst, int w, int h,
                                  const float * const * lut_x, const float * const * lut_y)
{
    assert(src && dst);
    int xd=0, yd=0;

    for ( int i = 0; i < w; ++i )
        for ( int j = 0; j < h; ++j )
        {
            //xd = static_cast<unsigned int>(lut_x[j][i]);
            //yd = static_cast<unsigned int>(lut_y[j][i]);
            xd = (unsigned int)(lut_x[j][i]);
            yd = (unsigned int)(lut_y[j][i]);
            if ( xd >= w || yd >= h ||
                 xd < 0 || yd < 0)
                dst[j][i] = 0;
            else
                dst[j][i] = src[yd][xd];
        }
}

inline void db_WarpImageLutBilinear_u(const unsigned char * const * src, unsigned char ** dst, int w, int h,
                                      const float * const * lut_x,const float * const* lut_y)
{
    assert(src && dst);
    double xd=0.0, yd=0.0;

    for ( int i = 0; i < w; ++i )
        for ( int j = 0; j < h; ++j )
        {
            xd = static_cast<double>(lut_x[j][i]);
            yd = static_cast<double>(lut_y[j][i]);
            if ( xd > w   || yd > h ||
                 xd < 0.0 || yd < 0.0)
                dst[j][i] = 0;
            else
                dst[j][i] = db_BilinearInterpolation(yd, xd, src);
        }
}


void db_WarpImageLut_u(const unsigned char * const * src, unsigned char ** dst, int w, int h,
                       const float * const * lut_x,const float * const * lut_y, int type)
{
    switch (type)
    {
    case DB_WARP_FAST:
        db_WarpImageLutFast_u(src,dst,w,h,lut_x,lut_y);
        break;
    case DB_WARP_BILINEAR:
        db_WarpImageLutBilinear_u(src,dst,w,h,lut_x,lut_y);
        break;
    default:
        break;
    }
}


void db_PrintDoubleVector(double *a,long size)
{
    printf("[ ");
    for(long i=0;i<size;i++) printf("%lf ",a[i]);
    printf("]");
}

void db_PrintDoubleMatrix(double *a,long rows,long cols)
{
    printf("[\n");
    for(long i=0;i<rows;i++)
    {
        for(long j=0;j<cols;j++) printf("%lf ",a[i*cols+j]);
        printf("\n");
    }
    printf("]");
}
