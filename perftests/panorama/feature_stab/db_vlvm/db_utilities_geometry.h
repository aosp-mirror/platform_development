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

/* $Id: db_utilities_geometry.h,v 1.3 2011/06/17 14:03:31 mbansal Exp $ */

#ifndef DB_UTILITIES_GEOMETRY_H
#define DB_UTILITIES_GEOMETRY_H

#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*! Get the inhomogenous 2D-point centroid of nr_point inhomogenous
points in X*/
inline void db_PointCentroid2D(double c[2],const double *X,int nr_points)
{
    int i;
    double cx,cy,m;

    cx=0;cy=0;
    for(i=0;i<nr_points;i++)
    {
        cx+= *X++;
        cy+= *X++;
    }
    if(nr_points)
    {
        m=1.0/((double)nr_points);
        c[0]=cx*m;
        c[1]=cy*m;
    }
    else c[0]=c[1]=0;
}

inline void db_PointCentroid2D(double c[2],const double * const *X,int nr_points)
{
    int i;
    double cx,cy,m;
    const double *temp;

    cx=0;cy=0;
    for(i=0;i<nr_points;i++)
    {
        temp= *X++;
        cx+=temp[0];
        cy+=temp[1];
    }
    if(nr_points)
    {
        m=1.0/((double)nr_points);
        c[0]=cx*m;
        c[1]=cy*m;
    }
    else c[0]=c[1]=0;
}

/*! Get the inhomogenous 3D-point centroid of nr_point inhomogenous
points in X*/
inline void db_PointCentroid3D(double c[3],const double *X,int nr_points)
{
    int i;
    double cx,cy,cz,m;

    cx=0;cy=0;cz=0;
    for(i=0;i<nr_points;i++)
    {
        cx+= *X++;
        cy+= *X++;
        cz+= *X++;
    }
    if(nr_points)
    {
        m=1.0/((double)nr_points);
        c[0]=cx*m;
        c[1]=cy*m;
        c[2]=cz*m;
    }
    else c[0]=c[1]=c[2]=0;
}

inline void db_PointCentroid3D(double c[3],const double * const *X,int nr_points)
{
    int i;
    double cx,cy,cz,m;
    const double *temp;

    cx=0;cy=0;cz=0;
    for(i=0;i<nr_points;i++)
    {
        temp= *X++;
        cx+=temp[0];
        cy+=temp[1];
        cz+=temp[2];
    }
    if(nr_points)
    {
        m=1.0/((double)nr_points);
        c[0]=cx*m;
        c[1]=cy*m;
        c[2]=cz*m;
    }
    else c[0]=c[1]=c[2]=0;
}

#endif /* DB_UTILITIES_GEOMETRY_H */
