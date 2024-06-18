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
import {IDENTITY_MATRIX, TransformMatrix} from 'common/geometry_types';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

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

export class Transform {
  static EMPTY = new Transform(TransformType.EMPTY, IDENTITY_MATRIX);

  constructor(public type: TransformType, public matrix: TransformMatrix) {}

  static from(
    transformNode: PropertyTreeNode,
    position?: PropertyTreeNode,
  ): Transform {
    if (transformNode.getAllChildren().length === 0) return Transform.EMPTY;

    const transformType = transformNode.getChildByName('type')?.getValue() ?? 0;
    const matrixNode = transformNode.getChildByName('matrix');

    if (matrixNode) {
      return new Transform(transformType, {
        dsdx: assertDefined(matrixNode.getChildByName('dsdx')).getValue(),
        dtdx: assertDefined(matrixNode.getChildByName('dtdx')).getValue(),
        tx: assertDefined(matrixNode.getChildByName('tx')).getValue(),
        dsdy: assertDefined(matrixNode.getChildByName('dsdy')).getValue(),
        dtdy: assertDefined(matrixNode.getChildByName('dtdy')).getValue(),
        ty: assertDefined(matrixNode.getChildByName('ty')).getValue(),
      });
    }

    const x = position?.getChildByName('x')?.getValue() ?? 0;
    const y = position?.getChildByName('y')?.getValue() ?? 0;

    if (TransformUtils.isSimpleTransform(transformType)) {
      return TransformUtils.getDefaultTransform(transformType, x, y);
    }

    return new Transform(transformType, {
      dsdx: transformNode.getChildByName('dsdx')?.getValue() ?? 0,
      dtdx: transformNode.getChildByName('dtdx')?.getValue() ?? 0,
      tx: x,
      dsdy: transformNode.getChildByName('dsdy')?.getValue() ?? 0,
      dtdy: transformNode.getChildByName('dtdy')?.getValue() ?? 0,
      ty: y,
    });
  }
}

export class TransformUtils {
  static isValidTransform(transform: Transform): boolean {
    return (
      transform.matrix.dsdx * transform.matrix.dtdy !==
      transform.matrix.dtdx * transform.matrix.dsdy
    );
  }

  static isSimpleRotation(type: TransformType | undefined): boolean {
    return !(type
      ? TransformUtils.isFlagSet(type, TransformType.ROT_INVALID_VAL)
      : false);
  }

  static getTypeFlags(type: TransformType): string {
    const typeFlags: string[] = [];

    if (
      TransformUtils.isFlagClear(
        type,
        TransformType.SCALE_VAL |
          TransformType.ROTATE_VAL |
          TransformType.TRANSLATE_VAL,
      )
    ) {
      typeFlags.push('IDENTITY');
    }

    if (TransformUtils.isFlagSet(type, TransformType.SCALE_VAL)) {
      typeFlags.push('SCALE');
    }

    if (TransformUtils.isFlagSet(type, TransformType.TRANSLATE_VAL)) {
      typeFlags.push('TRANSLATE');
    }

    if (TransformUtils.isFlagSet(type, TransformType.ROT_INVALID_VAL)) {
      typeFlags.push('ROT_INVALID');
    } else if (
      TransformUtils.isFlagSet(
        type,
        TransformType.ROT_90_VAL |
          TransformType.FLIP_V_VAL |
          TransformType.FLIP_H_VAL,
      )
    ) {
      typeFlags.push('ROT_270');
    } else if (
      TransformUtils.isFlagSet(
        type,
        TransformType.FLIP_V_VAL | TransformType.FLIP_H_VAL,
      )
    ) {
      typeFlags.push('ROT_180');
    } else {
      if (TransformUtils.isFlagSet(type, TransformType.ROT_90_VAL)) {
        typeFlags.push('ROT_90');
      }
      if (TransformUtils.isFlagSet(type, TransformType.FLIP_V_VAL)) {
        typeFlags.push('FLIP_V');
      }
      if (TransformUtils.isFlagSet(type, TransformType.FLIP_H_VAL)) {
        typeFlags.push('FLIP_H');
      }
    }

    if (typeFlags.length === 0) {
      throw Error(`Unknown transform type ${type}`);
    }
    return typeFlags.join('|');
  }

  static getDefaultTransform(
    type: TransformType,
    x: number,
    y: number,
  ): Transform {
    // IDENTITY
    if (!type) {
      return new Transform(type, {
        dsdx: 1,
        dtdx: 0,
        tx: x,
        dsdy: 0,
        dtdy: 1,
        ty: y,
      });
    }

    // ROT_270 = ROT_90|FLIP_H|FLIP_V
    if (
      TransformUtils.isFlagSet(
        type,
        TransformType.ROT_90_VAL |
          TransformType.FLIP_V_VAL |
          TransformType.FLIP_H_VAL,
      )
    ) {
      return new Transform(type, {
        dsdx: 0,
        dtdx: -1,
        tx: x,
        dsdy: 1,
        dtdy: 0,
        ty: y,
      });
    }

    // ROT_180 = FLIP_H|FLIP_V
    if (
      TransformUtils.isFlagSet(
        type,
        TransformType.FLIP_V_VAL | TransformType.FLIP_H_VAL,
      )
    ) {
      return new Transform(type, {
        dsdx: -1,
        dtdx: 0,
        tx: x,
        dsdy: 0,
        dtdy: -1,
        ty: y,
      });
    }

    // ROT_90
    if (TransformUtils.isFlagSet(type, TransformType.ROT_90_VAL)) {
      return new Transform(type, {
        dsdx: 0,
        dtdx: 1,
        tx: x,
        dsdy: -1,
        dtdy: 0,
        ty: y,
      });
    }

    // IDENTITY
    if (
      TransformUtils.isFlagClear(
        type,
        TransformType.SCALE_VAL | TransformType.ROTATE_VAL,
      )
    ) {
      return new Transform(type, {
        dsdx: 1,
        dtdx: 0,
        tx: x,
        dsdy: 0,
        dtdy: 1,
        ty: y,
      });
    }

    throw new Error(`Unknown transform type ${type}`);
  }

  static isSimpleTransform(type: TransformType): boolean {
    return TransformUtils.isFlagClear(
      type,
      TransformType.ROT_INVALID_VAL | TransformType.SCALE_VAL,
    );
  }

  private static isFlagSet(type: TransformType, bits: number): boolean {
    type = type || 0;
    return (type & bits) === bits;
  }

  private static isFlagClear(type: TransformType, bits: number): boolean {
    return (type & bits) === 0;
  }
}
