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

/* $Id: db_utilities_rotation.h,v 1.2 2010/09/03 12:00:11 bsouthall Exp $ */

#ifndef DB_UTILITIES_ROTATION
#define DB_UTILITIES_ROTATION

#include "db_utilities.h"



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup LMRotation (LM) Rotation Utilities (quaternions, orthonormal)
 */
/*\{*/
/*!
 Takes a unit quaternion and gives its corresponding rotation matrix.
 \param R rotation matrix (out)
 \param q quaternion
 */
inline void db_QuaternionToRotation(double R[9],const double q[4])
{
    double q0q0,q0qx,q0qy,q0qz,qxqx,qxqy,qxqz,qyqy,qyqz,qzqz;

    q0q0=q[0]*q[0];
    q0qx=q[0]*q[1];
    q0qy=q[0]*q[2];
    q0qz=q[0]*q[3];
    qxqx=q[1]*q[1];
    qxqy=q[1]*q[2];
    qxqz=q[1]*q[3];
    qyqy=q[2]*q[2];
    qyqz=q[2]*q[3];
    qzqz=q[3]*q[3];

    R[0]=q0q0+qxqx-qyqy-qzqz; R[1]=2.0*(qxqy-q0qz);     R[2]=2.0*(qxqz+q0qy);
    R[3]=2.0*(qxqy+q0qz);     R[4]=q0q0-qxqx+qyqy-qzqz; R[5]=2.0*(qyqz-q0qx);
    R[6]=2.0*(qxqz-q0qy);     R[7]=2.0*(qyqz+q0qx);     R[8]=q0q0-qxqx-qyqy+qzqz;
}

/*\}*/
#endif /* DB_UTILITIES_ROTATION */
