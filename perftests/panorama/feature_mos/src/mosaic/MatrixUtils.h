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
// Matrixutils.h
// $Id: MatrixUtils.h,v 1.5 2011/05/16 15:33:06 mbansal Exp $


#ifndef MATRIX_UTILS_H
#define MATRIX_UTILS_H

/* Simple class for 3x3 matrix, mainly used to convert from 9x1
 * to 3x3
 */
class Matrix33 {
public:

  /**
   *  Empty constructor
   */
  Matrix33() {
    initialize();
  }

  /**
   *  Constructor with identity initialization
   *  Arguments:
   *     identity: Specifies wether to initialize matrix to
   *     identity or zeros
   */
  Matrix33(bool identity) {
    initialize(identity);
  }

  /**
   *  Initialize to identity matrix
   */
  void initialize(bool identity = false) {
    mat[0][1] = mat[0][2] = mat[1][0] = mat[1][2] = mat[2][0] = mat[2][1] = 0.0;
    if (identity) {
      mat[0][0] = mat[1][1] = mat[2][2] = 1.0;
    } else {
      mat[0][0] = mat[1][1] = mat[2][2] = 0.0;
    }
  }

  /**
   *  Conver ta 9x1 matrix to a 3x3 matrix
   */
  static void convert9to33(double out[3][3], double in[9]) {
    out[0][0] = in[0];
    out[0][1] = in[1];
    out[0][2] = in[2];

    out[1][0] = in[3];
    out[1][1] = in[4];
    out[1][2] = in[5];

    out[2][0] = in[6];
    out[2][1] = in[7];
    out[2][2] = in[8];

  }

  /* Matrix data */
  double mat[3][3];

};

/* Simple class for 9x1 matrix, mainly used to convert from 3x3
 * to 9x1
 */
class Matrix9 {
public:

  /**
   *  Empty constructor
   */
  Matrix9() {
    initialize();
  }

  /**
   *  Constructor with identity initialization
   *  Arguments:
   *     identity: Specifies wether to initialize matrix to
   *     identity or zeros
   */
  Matrix9(bool identity) {
    initialize(identity);
  }

  /**
   *  Initialize to identity matrix
   */
  void initialize(bool identity = false) {
    mat[1] = mat[2] = mat[3] = mat[5] = mat[6] = mat[7] = 0.0;
    if (identity) {
      mat[0] = mat[4] = mat[8] = 1.0;
    } else {
      mat[0] = mat[4] = mat[8] = 0.0;
    }
  }

  /**
   *  Conver ta 3x3 matrix to a 9x1 matrix
   */
  static void convert33to9(double out[9], double in[3][3]) {
    out[0] = in[0][0];
    out[1] = in[0][1];
    out[2] = in[0][2];

    out[3] = in[1][0];
    out[4] = in[1][1];
    out[5] = in[1][2];

    out[6] = in[2][0];
    out[7] = in[2][1];
    out[8] = in[2][2];

  }

  /* Matrix data */
  double mat[9];

};

#endif
