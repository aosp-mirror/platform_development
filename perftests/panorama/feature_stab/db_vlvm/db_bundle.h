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

/* $Id: db_bundle.h,v 1.2 2011/06/17 14:03:30 mbansal Exp $ */

#ifndef DB_BUNDLE_H
#define DB_BUNDLE_H


/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup LMBundle (LM) Bundle adjustment utilities (a.k.a. Levenberg-Marquardt algorithm)
 */
/*\{*/

#include "db_utilities.h"

/*!
Solve for update dx such that diagmult(1+lambda,transpose(J)%J)%dx= -Jtf
using only upper half of JtJ, destroying lower half below diagonal in the process
dimension is n and d should point to n allocated doubles of scratch memory
*/
inline void db_Compute_dx(double *dx,double **JtJ,double *min_Jtf,double lambda,double *d,int n)
{
    int i;
    double opl;

    opl=1.0+lambda;
    for(i=0;i<n;i++) d[i]=JtJ[i][i]*opl;

    db_CholeskyDecompSeparateDiagonal(JtJ,d,n);
    db_CholeskyBacksub(dx,JtJ,d,n,min_Jtf);
}

/*!
Solve for update dx such that diagmult(1+lambda,transpose(J)%J)%dx= -Jtf
using only upper half of JtJ, destroying lower half below diagonal in the process
*/
inline void db_Compute_dx_3x3(double dx[3],double JtJ[9],const double min_Jtf[3],double lambda)
{
    double d[3],opl;

    opl=1.0+lambda;
    d[0]=JtJ[0]*opl;
    d[1]=JtJ[4]*opl;
    d[2]=JtJ[8]*opl;
    db_CholeskyDecomp3x3SeparateDiagonal(JtJ,d);
    db_CholeskyBacksub3x3(dx,JtJ,d,min_Jtf);
}

/*\}*/

#endif /* DB_BUNDLE_H */
