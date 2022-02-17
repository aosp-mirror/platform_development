/*
 * Copyright 2021, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Transform, Matrix33 } from "../common"

Transform.fromProto = function (transformProto, positionProto): Transform {
    const entry = new Transform(
        transformProto?.type ?? 0,
        getMatrix(transformProto, positionProto))

    return entry
}

function getMatrix(transform, position): Matrix33 {
    const x = position?.x ?? 0
    const y = position?.y ?? 0

    if (transform == null || isSimpleTransform(transform.type)) {
        return getDefaultTransform(transform?.type, x, y)
    }

    return new Matrix33(transform.dsdx, transform.dtdx, x, transform.dsdy, transform.dtdy, y)
}

function getDefaultTransform(type, x, y): Matrix33 {
    // IDENTITY
    if (!type) {
        return new Matrix33(1, 0, x, 0, 1, y)
    }

    // ROT_270 = ROT_90|FLIP_H|FLIP_V
    if (isFlagSet(type, ROT_90_VAL | FLIP_V_VAL | FLIP_H_VAL)) {
        return new Matrix33(0, -1, x, 1, 0, y)
    }

    // ROT_180 = FLIP_H|FLIP_V
    if (isFlagSet(type, FLIP_V_VAL | FLIP_H_VAL)) {
        return new Matrix33(-1, 0, x, 0, -1, y)
    }

    // ROT_90
    if (isFlagSet(type, ROT_90_VAL)) {
        return new Matrix33(0, 1, x, -1, 0, y)
    }

    // IDENTITY
    if (isFlagClear(type, SCALE_VAL | ROTATE_VAL)) {
        return new Matrix33(1, 0, x, 0, 1, y)
    }

    throw new Error(`Unknown transform type ${type}`)
}

export function isFlagSet(type, bits): Boolean {
    var type = type || 0;
    return (type & bits) === bits;
}

export function isFlagClear(type, bits): Boolean {
    return (type & bits) === 0;
}

export function isSimpleTransform(type): Boolean {
    return isFlagClear(type, ROT_INVALID_VAL | SCALE_VAL)
}

/* transform type flags */
const ROTATE_VAL = 0x0002
const SCALE_VAL = 0x0004

/* orientation flags */
const FLIP_H_VAL = 0x0100 // (1 << 0 << 8)
const FLIP_V_VAL = 0x0200 // (1 << 1 << 8)
const ROT_90_VAL = 0x0400 // (1 << 2 << 8)
const ROT_INVALID_VAL = 0x8000 // (0x80 << 8)

export default Transform
