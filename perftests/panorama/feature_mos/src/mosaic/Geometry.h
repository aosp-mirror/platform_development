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

/////////////////////////////
// Geometry.h
// $Id: Geometry.h,v 1.2 2011/06/17 13:35:48 mbansal Exp $

#pragma once
#include "MosaicTypes.h"

///////////////////////////////////////////////////////////////
///////////////// BEG GLOBAL ROUTINES /////////////////////////
///////////////////////////////////////////////////////////////


inline double hypotSq(double a, double b)
{
    return ((a)*(a)+(b)*(b));
}

inline void ClipRect(double x, double y, BlendRect &brect)
{
    if (y < brect.bot) brect.bot = y;
    if (y > brect.top) brect.top = y;
    if (x < brect.lft) brect.lft = x;
    if (x > brect.rgt) brect.rgt = x;
}

inline void ClipRect(BlendRect rrect, BlendRect &brect)
{
    if (rrect.bot < brect.bot) brect.bot = rrect.bot;
    if (rrect.top > brect.top) brect.top = rrect.top;
    if (rrect.lft < brect.lft) brect.lft = rrect.lft;
    if (rrect.rgt > brect.rgt) brect.rgt = rrect.rgt;
}

// Clip x to be within [-border,width+border-1]
inline void clipToSegment(int &x, int width, int border)
{
    if(x < -border)
        x = -border;
    else if(x >= width+border)
        x = width + border - 1;
}

// Return true if x within [-border,width+border-1]
inline bool inSegment(int x, int width, int border)
{
    return (x >= -border && x < width + border - 1);
}

inline void FindTriangleCentroid(double x0, double y0, double x1, double y1,
                                    double x2, double y2,
                                    double &mass, double &centX, double &centY)
{
    // Calculate the centroid of the triangle
    centX = (x0 + x1 + x2) / 3.0;
    centY = (y0 + y1 + y2) / 3.0;

    // Calculate 2*Area for the triangle
    if (y0 == y2)
    {
        if (x0 == x1)
        {
            mass = fabs((y1 - y0) * (x2 - x0)); // Special case 1a
        }
        else
        {
            mass = fabs((y1 - y0) * (x1 - x0)); // Special case 1b
        }
    }
    else if (x0 == x2)
    {
        if (x0 == x1)
        {
            mass = fabs((x2 - x0) * (y2 - y0)); // Special case 2a
        }
        else
        {
            mass = fabs((x1 - x0) * (y2 - y0)); // Special case 2a
        }
    }
    else if (x1 == x2)
    {
        mass = fabs((x1 - x0) * (y2 - y0)); // Special case 3
    }
    else
    {
        // Calculate line equation from x0,y0 to x2,y2
        double dx = x2 - x0;
        double dy = y2 - y0;
        // Calculate the length of the side
        double len1 = sqrt(dx * dx + dy * dy);
        double m1 = dy / dx;
        double b1 = y0 - m1 * x0;
        // Calculate the line that goes through x1,y1 and is perpendicular to
        // the other line
        double m2 = 1.0 / m1;
        double b2 = y1 - m2 * x1;
        // Calculate the intersection of the two lines
        if (fabs( m1 - m2 ) > 1.e-6)
        {
            double x = (b2 - b1) / (m1 - m2);
            // the mass is the base * height
            dx = x1 - x;
            dy = y1 - m1 * x + b1;
            mass = len1 * sqrt(dx * dx + dy * dy);
        }
        else
        {
            mass = fabs( (y1 - y0) * (x2 - x0) );
        }
    }
}

inline void FindQuadCentroid(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3,
                                     double &centX, double &centY)

{
    // To find the centroid:
    // 1) Divide the quadrilateral into two triangles by scribing a diagonal
    // 2) Calculate the centroid of each triangle (the intersection of the angle bisections).
    // 3) Find the centroid of the quad by weighting each triangle centroids by their area.

    // Calculate the corner points
    double z;

    // The quad is split from x0,y0 to x2,y2
    double mass1, mass2, cent1x, cent2x, cent1y, cent2y;
    FindTriangleCentroid(x0, y0, x1, y1, x2, y2, mass1, cent1x, cent1y);
    FindTriangleCentroid(x0, y0, x3, y3, x2, y2, mass2, cent2x, cent2y);

    // determine position of quad centroid
    z = mass2 / (mass1 + mass2);
    centX = cent1x + (cent2x - cent1x) * z;
    centY = cent1y + (cent2y - cent1y) * z;
}

///////////////////////////////////////////////////////////////
////////////////// END GLOBAL ROUTINES ////////////////////////
///////////////////////////////////////////////////////////////


