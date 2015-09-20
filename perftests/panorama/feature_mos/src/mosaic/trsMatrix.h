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

///////////////////////////////////////////////////
// trsMatrix.h
// $Id: trsMatrix.h,v 1.8 2011/06/17 13:35:48 mbansal Exp $

#ifndef TRSMATRIX_H_
#define TRSMATRIX_H_


// Calculate the determinant of a matrix
double det33d(const double m[3][3]);

// Invert a matrix
void inv33d(const double m[3][3], double out[3][3]);

// Multiply a = b * c
void mult33d(double a[3][3], double b[3][3], double c[3][3]);

// Normalize matrix so matrix[2][2] is '1'
int normProjMat33d(double m[3][3]);

inline double ProjZ(double trs[3][3], double x, double y, double f)
{
    return ((trs)[2][0]*(x) + (trs)[2][1]*(y) + (trs)[2][2]*(f));
}

inline double ProjX(double trs[3][3], double x, double y, double z, double f)
{
    return (((trs)[0][0]*(x) + (trs)[0][1]*(y) + (trs)[0][2]*(f)) / (z));
}

inline double ProjY(double trs[3][3], double x, double y, double z, double f)
{
    return (((trs)[1][0]*(x) + (trs)[1][1]*(y) + (trs)[1][2]*(f)) / (z));
}


#endif
