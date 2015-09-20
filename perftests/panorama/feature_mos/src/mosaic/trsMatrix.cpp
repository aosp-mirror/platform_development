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

// trsMatrix.cpp
// $Id: trsMatrix.cpp,v 1.9 2011/06/17 13:35:48 mbansal Exp $

#include "stdio.h"
#include <math.h>
#include "trsMatrix.h"

void mult33d(double a[3][3], double b[3][3], double c[3][3])
{
    a[0][0] = b[0][0]*c[0][0] + b[0][1]*c[1][0] + b[0][2]*c[2][0];
    a[0][1] = b[0][0]*c[0][1] + b[0][1]*c[1][1] + b[0][2]*c[2][1];
    a[0][2] = b[0][0]*c[0][2] + b[0][1]*c[1][2] + b[0][2]*c[2][2];
    a[1][0] = b[1][0]*c[0][0] + b[1][1]*c[1][0] + b[1][2]*c[2][0];
    a[1][1] = b[1][0]*c[0][1] + b[1][1]*c[1][1] + b[1][2]*c[2][1];
    a[1][2] = b[1][0]*c[0][2] + b[1][1]*c[1][2] + b[1][2]*c[2][2];
    a[2][0] = b[2][0]*c[0][0] + b[2][1]*c[1][0] + b[2][2]*c[2][0];
    a[2][1] = b[2][0]*c[0][1] + b[2][1]*c[1][1] + b[2][2]*c[2][1];
    a[2][2] = b[2][0]*c[0][2] + b[2][1]*c[1][2] + b[2][2]*c[2][2];
}


// normProjMat33d
// m = input matrix
// return: result if successful
int normProjMat33d(double m[3][3])
{
    double m22;

    if(m[2][2] == 0.0)
        {
        return 0;
}

    m[0][0] /= m[2][2];
    m[0][1] /= m[2][2];
    m[0][2] /= m[2][2];
    m[1][0] /= m[2][2];
    m[1][1] /= m[2][2];
    m[1][2] /= m[2][2];
    m[2][0] /= m[2][2];
    m[2][1] /= m[2][2];
    m[2][2] = 1.0;

    return 1;
}

// det33d
// m = input matrix
// returns: determinant
double det33d(const double m[3][3])
{
    double result;

    result  = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]);
    result += m[0][1] * (m[1][2] * m[2][0] - m[1][0] * m[2][2]);
    result += m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);

    return result;
}

// inv33d
//
void inv33d(const double m[3][3], double out[3][3])
{
    double det = det33d(m);

    out[0][0] = (m[1][1]*m[2][2] - m[1][2]*m[2][1]) / det;
    out[1][0] = (m[1][2]*m[2][0] - m[1][0]*m[2][2]) / det;
    out[2][0] = (m[1][0]*m[2][1] - m[1][1]*m[2][0]) / det;

    out[0][1] = (m[0][2]*m[2][1] - m[0][1]*m[2][2]) / det;
    out[1][1] = (m[0][0]*m[2][2] - m[0][2]*m[2][0]) / det;
    out[2][1] = (m[0][1]*m[2][0] - m[0][0]*m[2][1]) / det;

    out[0][2] = (m[0][1]*m[1][2] - m[0][2]*m[1][1]) / det;
    out[1][2] = (m[0][2]*m[1][0] - m[0][0]*m[1][2]) / det;
    out[2][2] = (m[0][0]*m[1][1] - m[0][1]*m[1][0]) / det;
}
