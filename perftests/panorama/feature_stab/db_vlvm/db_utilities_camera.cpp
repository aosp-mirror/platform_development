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

/* $Id: db_utilities_camera.cpp,v 1.3 2011/06/17 14:03:31 mbansal Exp $ */

#include "db_utilities_camera.h"
#include "db_utilities.h"
#include <assert.h>



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/

void db_Approx3DCalMat(double K[9],double Kinv[9],int im_width,int im_height,double f_correction,int field)
{
    double iw,ih,av_size,field_fact;

    if(field) field_fact=2.0;
    else field_fact=1.0;

    iw=(double)im_width;
    ih=(double)(im_height*field_fact);
    av_size=(iw+ih)/2.0;
    K[0]=f_correction*av_size;
    K[1]=0;
    K[2]=iw/2.0;
    K[3]=0;
    K[4]=f_correction*av_size/field_fact;
    K[5]=ih/2.0/field_fact;
    K[6]=0;
    K[7]=0;
    K[8]=1;

    db_InvertCalibrationMatrix(Kinv,K);
}
