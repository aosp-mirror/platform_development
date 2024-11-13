/*
 * Copyright (C) 2024 The Android Open Source Project
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

import {assertDefined} from 'common/assert_utils';
import {
  IDENTITY_MATRIX,
  TransformMatrix,
} from 'common/geometry/transform_matrix';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export enum TransformTypeFlags {
  EMPTY = 0x0,
  TRANSLATE_VAL = 0x0001,
  ROTATE_VAL = 0x0002,
  SCALE_VAL = 0x0004,
  FLIP_H_VAL = 0x0100,
  FLIP_V_VAL = 0x0200,
  ROT_90_VAL = 0x0400,
  ROT_INVALID_VAL = 0x8000,
}

export class Transform {
  static EMPTY = new Transform(TransformTypeFlags.EMPTY, IDENTITY_MATRIX);

  constructor(
    public type: TransformTypeFlags,
    public matrix: TransformMatrix,
  ) {}

  static from(
    transformNode: PropertyTreeNode,
    position?: PropertyTreeNode,
  ): Transform {
    if (transformNode.getAllChildren().length === 0) return Transform.EMPTY;

    const transformType = transformNode.getChildByName('type')?.getValue() ?? 0;
    const matrixNode = transformNode.getChildByName('matrix');

    if (matrixNode) {
      return new Transform(
        transformType,
        TransformMatrix.from({
          dsdx: assertDefined(matrixNode.getChildByName('dsdx')).getValue(),
          dtdx: assertDefined(matrixNode.getChildByName('dtdx')).getValue(),
          tx: assertDefined(matrixNode.getChildByName('tx')).getValue(),
          dtdy: assertDefined(matrixNode.getChildByName('dtdy')).getValue(),
          dsdy: assertDefined(matrixNode.getChildByName('dsdy')).getValue(),
          ty: assertDefined(matrixNode.getChildByName('ty')).getValue(),
        }),
      );
    }

    const x = position?.getChildByName('x')?.getValue() ?? 0;
    const y = position?.getChildByName('y')?.getValue() ?? 0;

    if (TransformType.isSimpleTransform(transformType)) {
      return TransformType.getDefaultTransform(transformType, x, y);
    }

    return new Transform(
      transformType,
      TransformMatrix.from({
        dsdx: transformNode.getChildByName('dsdx')?.getValue() ?? 0,
        dtdx: transformNode.getChildByName('dtdx')?.getValue() ?? 0,
        tx: x,
        dtdy: transformNode.getChildByName('dtdy')?.getValue() ?? 0,
        dsdy: transformNode.getChildByName('dsdy')?.getValue() ?? 0,
        ty: y,
      }),
    );
  }
}

export class TransformType {
  static isSimpleRotation(type: TransformTypeFlags | undefined): boolean {
    return !(type
      ? TransformType.isFlagSet(type, TransformTypeFlags.ROT_INVALID_VAL)
      : false);
  }

  static getTypeFlags(type: TransformTypeFlags): string {
    const typeFlags: string[] = [];

    if (
      TransformType.isFlagClear(
        type,
        TransformTypeFlags.SCALE_VAL |
          TransformTypeFlags.ROTATE_VAL |
          TransformTypeFlags.TRANSLATE_VAL,
      )
    ) {
      typeFlags.push('IDENTITY');
    }

    if (TransformType.isFlagSet(type, TransformTypeFlags.SCALE_VAL)) {
      typeFlags.push('SCALE');
    }

    if (TransformType.isFlagSet(type, TransformTypeFlags.TRANSLATE_VAL)) {
      typeFlags.push('TRANSLATE');
    }

    if (TransformType.isFlagSet(type, TransformTypeFlags.ROT_INVALID_VAL)) {
      typeFlags.push('ROT_INVALID');
    } else if (
      TransformType.isFlagSet(
        type,
        TransformTypeFlags.ROT_90_VAL |
          TransformTypeFlags.FLIP_V_VAL |
          TransformTypeFlags.FLIP_H_VAL,
      )
    ) {
      typeFlags.push('ROT_270');
    } else if (
      TransformType.isFlagSet(
        type,
        TransformTypeFlags.FLIP_V_VAL | TransformTypeFlags.FLIP_H_VAL,
      )
    ) {
      typeFlags.push('ROT_180');
    } else {
      if (TransformType.isFlagSet(type, TransformTypeFlags.ROT_90_VAL)) {
        typeFlags.push('ROT_90');
      }
      if (TransformType.isFlagSet(type, TransformTypeFlags.FLIP_V_VAL)) {
        typeFlags.push('FLIP_V');
      }
      if (TransformType.isFlagSet(type, TransformTypeFlags.FLIP_H_VAL)) {
        typeFlags.push('FLIP_H');
      }
    }

    if (typeFlags.length === 0) {
      throw TransformType.makeUnknownTransformTypeError(type);
    }
    return typeFlags.join('|');
  }

  static getDefaultTransform(
    type: TransformTypeFlags,
    x: number,
    y: number,
  ): Transform {
    // IDENTITY
    if (!type) {
      return new Transform(
        type,
        TransformMatrix.from({
          dsdx: 1,
          dtdx: 0,
          tx: x,
          dtdy: 0,
          dsdy: 1,
          ty: y,
        }),
      );
    }

    // ROT_270 = ROT_90|FLIP_H|FLIP_V
    if (
      TransformType.isFlagSet(
        type,
        TransformTypeFlags.ROT_90_VAL |
          TransformTypeFlags.FLIP_V_VAL |
          TransformTypeFlags.FLIP_H_VAL,
      )
    ) {
      return new Transform(
        type,
        TransformMatrix.from({
          dsdx: 0,
          dtdx: -1,
          tx: x,
          dtdy: 1,
          dsdy: 0,
          ty: y,
        }),
      );
    }

    // ROT_180 = FLIP_H|FLIP_V
    if (
      TransformType.isFlagSet(
        type,
        TransformTypeFlags.FLIP_V_VAL | TransformTypeFlags.FLIP_H_VAL,
      )
    ) {
      return new Transform(
        type,
        TransformMatrix.from({
          dsdx: -1,
          dtdx: 0,
          tx: x,
          dtdy: 0,
          dsdy: -1,
          ty: y,
        }),
      );
    }

    // ROT_90
    if (TransformType.isFlagSet(type, TransformTypeFlags.ROT_90_VAL)) {
      return new Transform(
        type,
        TransformMatrix.from({
          dsdx: 0,
          dtdx: 1,
          tx: x,
          dtdy: -1,
          dsdy: 0,
          ty: y,
        }),
      );
    }

    // IDENTITY
    if (
      TransformType.isFlagClear(
        type,
        TransformTypeFlags.SCALE_VAL | TransformTypeFlags.ROTATE_VAL,
      )
    ) {
      return new Transform(
        type,
        TransformMatrix.from({
          dsdx: 1,
          dtdx: 0,
          tx: x,
          dtdy: 0,
          dsdy: 1,
          ty: y,
        }),
      );
    }

    throw TransformType.makeUnknownTransformTypeError(type);
  }

  static makeUnknownTransformTypeError(type: TransformTypeFlags): Error {
    return new Error(`Unknown transform type ${type} found in SF trace entry`);
  }

  static isSimpleTransform(type: TransformTypeFlags): boolean {
    return TransformType.isFlagClear(
      type,
      TransformTypeFlags.ROT_INVALID_VAL | TransformTypeFlags.SCALE_VAL,
    );
  }

  private static isFlagSet(type: TransformTypeFlags, bits: number): boolean {
    type = type || 0;
    return (type & bits) === bits;
  }

  private static isFlagClear(type: TransformTypeFlags, bits: number): boolean {
    return (type & bits) === 0;
  }
}
