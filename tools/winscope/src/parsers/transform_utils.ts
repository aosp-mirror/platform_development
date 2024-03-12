/*
 * Copyright (C) 2023 The Android Open Source Project
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

import {IDENTITY_MATRIX, TransformMatrix} from 'common/geometry_utils';

export class Transform {
  constructor(public type: TransformType, public matrix: TransformMatrix) {}
}

export enum TransformType {
  EMPTY = 0x0,
  TRANSLATE_VAL = 0x0001,
  ROTATE_VAL = 0x0002,
  SCALE_VAL = 0x0004,
  FLIP_H_VAL = 0x0100,
  FLIP_V_VAL = 0x0200,
  ROT_90_VAL = 0x0400,
  ROT_INVALID_VAL = 0x8000,
}

export const EMPTY_TRANSFORM = new Transform(TransformType.EMPTY, IDENTITY_MATRIX);

export class TransformUtils {
  static isValidTransform(transform: any): boolean {
    if (!transform) return false;
    return transform.dsdx * transform.dtdy !== transform.dtdx * transform.dsdy;
  }

  static getTransform(transform: any, position: any): Transform {
    const transformType = transform?.type ?? 0;
    const x = position?.x ?? 0;
    const y = position?.y ?? 0;

    if (!transform || TransformUtils.isSimpleTransform(transformType)) {
      return TransformUtils.getDefaultTransform(transformType, x, y);
    }

    return new Transform(transformType, {
      dsdx: transform?.matrix.dsdx ?? 0,
      dtdx: transform?.matrix.dtdx ?? 0,
      tx: x,
      dsdy: transform?.matrix.dsdy ?? 0,
      dtdy: transform?.matrix.dtdy ?? 0,
      ty: y,
    });
  }

  static isSimpleRotation(transform: any): boolean {
    return !(transform?.type
      ? TransformUtils.isFlagSet(transform.type, TransformType.ROT_INVALID_VAL)
      : false);
  }

  private static getDefaultTransform(type: TransformType, x: number, y: number): Transform {
    // IDENTITY
    if (!type) {
      return new Transform(type, {dsdx: 1, dtdx: 0, tx: x, dsdy: 0, dtdy: 1, ty: y});
    }

    // ROT_270 = ROT_90|FLIP_H|FLIP_V
    if (
      TransformUtils.isFlagSet(
        type,
        TransformType.ROT_90_VAL | TransformType.FLIP_V_VAL | TransformType.FLIP_H_VAL
      )
    ) {
      return new Transform(type, {dsdx: 0, dtdx: -1, tx: x, dsdy: 1, dtdy: 0, ty: y});
    }

    // ROT_180 = FLIP_H|FLIP_V
    if (TransformUtils.isFlagSet(type, TransformType.FLIP_V_VAL | TransformType.FLIP_H_VAL)) {
      return new Transform(type, {dsdx: -1, dtdx: 0, tx: x, dsdy: 0, dtdy: -1, ty: y});
    }

    // ROT_90
    if (TransformUtils.isFlagSet(type, TransformType.ROT_90_VAL)) {
      return new Transform(type, {dsdx: 0, dtdx: 1, tx: x, dsdy: -1, dtdy: 0, ty: y});
    }

    // IDENTITY
    if (TransformUtils.isFlagClear(type, TransformType.SCALE_VAL | TransformType.ROTATE_VAL)) {
      return new Transform(type, {dsdx: 1, dtdx: 0, tx: x, dsdy: 0, dtdy: 1, ty: y});
    }

    throw new Error(`Unknown transform type ${type}`);
  }

  private static isFlagSet(type: number, bits: number): boolean {
    type = type || 0;
    return (type & bits) === bits;
  }

  private static isFlagClear(type: number, bits: number): boolean {
    return (type & bits) === 0;
  }

  private static isSimpleTransform(type: number): boolean {
    return TransformUtils.isFlagClear(
      type,
      TransformType.ROT_INVALID_VAL | TransformType.SCALE_VAL
    );
  }
}
