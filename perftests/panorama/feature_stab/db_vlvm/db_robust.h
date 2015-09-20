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

/* $Id: db_robust.h,v 1.4 2011/06/17 14:03:31 mbansal Exp $ */

#ifndef DB_ROBUST
#define DB_ROBUST



/*****************************************************************
*    Lean and mean begins here                                   *
*****************************************************************/
/*!
 * \defgroup LMRobust (LM) Robust Estimation
 */

/*!
    \struct     db_Statistics
    \ingroup    LMRobust
    \brief      (LnM) Sampling problem statistics
    \date       Mon Sep 10 10:28:08 EDT 2007
    \par        Copyright: 2007 Sarnoff Corporation.  All Rights Reserved
 */
 struct db_stat_struct
 {
     int nr_points;
     int nr_inliers;
     double inlier_fraction;
     double cost;
     double one_over_scale2;
     double lambda1;
     double lambda2;
     double lambda3;
     int nr_parameters;
     int model_dimension;
     double gric;
     double inlier_evidence;
     double posestd[6];
     double rotationvecCov[9];
     double translationvecCov[9];
     int posecov_inliercount;
     int posecovready;
     double median_reprojection_error;
 };
 typedef db_stat_struct db_Statistics;

#endif /* DB_ROBUST */
