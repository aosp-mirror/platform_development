/*
 * Copyright 2019, The Android Open Source Project
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

const FLIP_H_VAL = 1; // (1 << 0)
const FLIP_V_VAL = 2; // (1 << 1)
const ROT_90_VAL = 4; // (1 << 2)
const ROT_INVALID_VAL = 0x80;
const TRANSLATE_VAL = 0x1;
const ROTATE_VAL = 0x2;
const SCALE_VAL = 0x4;

const ROT_INVALID = 'ROT_INVALID';
const ROT_90 = 'ROT_90';
const ROT_0 = 'ROT_0';
const FLIP_V = 'FLIP_V';
const FLIP_H = 'FLIP_H';
const IDENTITY = 'IDENTITY';
const SCALE = 'SCALE';
const ROTATE = 'ROTATE';
const TRANSLATE = 'TRANSLATE';

function has_default_value(transform) {
    return (transform.type || '').indexOf(ROT_INVALID) == -1 &&
        (transform.type || '').indexOf(SCALE) == -1;
}

function preprocess(layer) {
    /**
     * Checks if the loaded file was a stored with ProtoBuf2 or Protobuf3
     *
     * Proto2 files don't have a Type for the transform object but all other
     * fields of the transform are set.
     *
     * Proto3 has a type field for the transform but doesn't store default
     * values (0 for transform type), also, the framework/native implementation
     * doesn't write a transform in case it is an identity matrix.
     *
     * @param {*} layer A layer from the processed file
     */
    function is_proto2(layer) {
        var transform = layer.transform || {};
        return (transform.type == undefined) && (transform.dsdx != undefined);
    }

    /**
     * Ensures all values of the transform object are filled.
     *
     * Creates a new object to ensure the transform values are displayed in the
     * correct order in the property list
     * @param {*} transform A transform object
     */
    function fill_transform_data(transform) {
        if (has_default_value(transform)) {
            return {type: transform.type};
        }

        return {
            dsdx: transform.dsdx || 0.0,
            dtdx: transform.dtdx || 0.0,
            dsdy: transform.dsdy || 0.0,
            dtdy: transform.dtdy || 0.0,
            type: transform.type
        };
    }

    /**
     * Converts a transform type into readable format.
     * Adapted from the dump function from framework/native
     *
     * @param {*} transform Transform object ot be converter
     */
    function get_transform_type(transform) {
        transform = transform || {};
        var type = transform.type || 0;
        var orient = type >> 8;
        var type_flags = [];

        if (orient & ROT_INVALID_VAL) {
            type_flags.push(ROT_INVALID);
        } else {
            if (orient & ROT_90_VAL) {
                type_flags.push(ROT_90);
            } else {
                type_flags.push(ROT_0);
            }
            if (orient & FLIP_V_VAL) {
                type_flags.push(FLIP_V);
            }
            if (orient & FLIP_H_VAL) {
                type_flags.push(FLIP_H);
            }
        }

        if (!(type & (SCALE_VAL | ROTATE_VAL | TRANSLATE_VAL))) {
            type_flags.push(IDENTITY);
        }
        if (type & SCALE_VAL) {
            type_flags.push(SCALE);
        }
        if (type & ROTATE_VAL) {
            type_flags.push(ROTATE);
        }
        if (type & TRANSLATE_VAL) {
            type_flags.push(TRANSLATE);
        }

        if (type_flags.length == 0) {
            throw "Unknown transform type " + type;
        }

        return type_flags.join(', ');
    }


    if (is_proto2(layer)) {
        return;
    }

    layer.original_type = layer.transform.type;
    if (layer.transform == undefined) {
        layer.transform = {}
    }
    layer.transform.type = get_transform_type(layer.transform);
    layer.transform = fill_transform_data(layer.transform);

    if (layer.requestedTransform != undefined) {
        layer.requestedTransform.type = get_transform_type(layer.requestedTransform);
        layer.requestedTransform = fill_transform_data(layer.requestedTransform);
    }
    if (layer.bufferTransform != undefined) {
        layer.bufferTransform.type = get_transform_type(layer.bufferTransform);
        layer.bufferTransform = fill_transform_data(layer.bufferTransform);
    }
    if (layer.effectiveTransform != undefined) {
        layer.effectiveTransform.type = get_transform_type(layer.effectiveTransform);
        layer.effectiveTransform = fill_transform_data(layer.effectiveTransform);
    }
}

function get_transform_value(transform) {
    var type = transform.type || '';
    // Proto2 or ROT_INVALID or SCALE
    if (type == '' || !has_default_value(transform)) {
        return transform;
    }

    // ROT_270 = ROT_90|FLIP_H|FLIP_V;
    if (type.includes(ROT_90) && type.includes(FLIP_V) && type.includes(FLIP_H)) {
        return {
            dsdx: 0.0,
            dtdx: -1.0,
            dsdy: 1.0,
            dtdy: 0.0
        };
    }

    // ROT_180 = FLIP_H|FLIP_V;
    if (type.includes(FLIP_V) && type.includes(FLIP_H)) {
        return {
            dsdx: -1.0,
            dtdx: 0.0,
            dsdy: 0.0,
            dtdy: -1.0
        };
    }

    // ROT_90
    if (type.includes(ROT_90)) {
        return {
            dsdx: 0.0,
            dtdx: 1.0,
            dsdy: -1.0,
            dtdy: 0.0
        };
    }

    // ROT_0
    if (type.includes(ROT_0)) {
        return {
            dsdx: 1.0,
            dtdx: 0.0,
            dsdy: 0.0,
            dtdy: 1.0
        };
    }

    throw "Unknown transform type " + type;
}

export {preprocess, get_transform_value};