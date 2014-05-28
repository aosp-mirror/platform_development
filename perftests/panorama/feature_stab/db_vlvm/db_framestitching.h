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

/* $Id: db_framestitching.h,v 1.2 2011/06/17 14:03:31 mbansal Exp $ */

#ifndef DB_FRAMESTITCHING_H
#define DB_FRAMESTITCHING_H
/*!
 * \defgroup FrameStitching Frame Stitching (2D and 3D homography estimation)
 */
/*\{*/


/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup LMFrameStitching (LM) Frame Stitching (2D and 3D homography estimation)
 */
/*\{*/

/*!
Find scale, rotation and translation of the similarity that
takes the nr_points inhomogenous 3D points X to Xp
(left to right according to Horn), i.e. for the homogenous equivalents
Xp and X we would have
\code
    Xp~
    [sR t]*X
    [0  1]
\endcode
If orientation_preserving is true, R is restricted such that det(R)>0.
allow_scaling, allow_rotation and allow_translation allow s,R and t
to differ from 1,Identity and 0

Full similarity takes the following on 550MHz:
\code
4.5 microseconds with       3 points
4.7 microseconds with       4 points
5.0 microseconds with       5 points
5.2 microseconds with       6 points
5.8 microseconds with      10 points
20  microseconds with     100 points
205 microseconds with    1000 points
2.9 milliseconds with   10000 points
50  milliseconds with  100000 points
0.5 seconds      with 1000000 points
\endcode
Without orientation_preserving:
\code
4 points is minimal for (s,R,t) (R,t)
3 points is minimal for (s,R) (R)
2 points is minimal for (s,t)
1 point is minimal for  (s) (t)
\endcode
With orientation_preserving:
\code
3 points is minimal for (s,R,t) (R,t)
2 points is minimal for (s,R) (s,t) (R)
1 point is minimal for  (s) (t)
\endcode

\param scale                    scale
\param R                        rotation
\param t                        translation
\param Xp                       inhomogenouse 3D points in first coordinate system
\param X                        inhomogenouse 3D points in second coordinate system
\param nr_points                number of points
\param orientation_preserving   if true, R is restricted such that det(R)>0.
\param allow_scaling            estimate scale
\param allow_rotation           estimate rotation
\param allow_translation        estimate translation
*/
DB_API void db_StitchSimilarity3DRaw(double *scale,double R[9],double t[3],
                            double **Xp,double **X,int nr_points,int orientation_preserving=1,
                            int allow_scaling=1,int allow_rotation=1,int allow_translation=1);


/*\}*/

#endif /* DB_FRAMESTITCHING_H */
