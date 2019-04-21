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

/* transform type flags */
const TRANSLATE_VAL   = 0x0001;
const ROTATE_VAL      = 0x0002;
const SCALE_VAL       = 0x0004;

/* orientation flags */
const FLIP_H_VAL      = 0x0100; // (1 << 0 << 8)
const FLIP_V_VAL      = 0x0200; // (1 << 1 << 8)
const ROT_90_VAL      = 0x0400; // (1 << 2 << 8)
const ROT_INVALID_VAL = 0x8000; // (0x80 << 8)

function is_proto_2(transform) {
  /*
  * Checks if the loaded file was a stored with ProtoBuf2 or Protobuf3
  *
  * Proto2 files don't have a Type for the transform object but all other
  * fields of the transform are set.
  *
  * Proto3 has a type field for the transform but doesn't store default
  * values (0 for transform type), also, the framework/native implementation
  * doesn't write a transform in case it is an identity matrix.
  */
  var propertyNames = Object.getOwnPropertyNames(transform);
  return (!propertyNames.includes("type") && propertyNames.includes("dsdx"));
}

function is_simple_transform(transform) {
  transform = transform || {};
  if (is_proto_2(transform)) {
    return false;
  }
  return is_type_flag_clear(transform, ROT_INVALID_VAL|SCALE_VAL);
}

/**
 * Converts a transform type into readable format.
 * Adapted from the dump function from framework/native
 *
 * @param {*} transform Transform object ot be converter
 */
function format_transform_type(transform) {
  if (is_proto_2(transform)) {
    return "";
  }

  if (is_type_flag_clear(transform, SCALE_VAL | ROTATE_VAL | TRANSLATE_VAL)) {
    return "IDENTITY";
  }

  var type_flags = [];
  if (is_type_flag_set(transform, SCALE_VAL)) {
    type_flags.push("SCALE");
  }
  if (is_type_flag_set(transform, TRANSLATE_VAL)) {
    type_flags.push("TRANSLATE");
  }

  if (is_type_flag_set(transform, ROT_INVALID_VAL)) {
    type_flags.push("ROT_INVALID");
  } else if (is_type_flag_set(transform, ROT_90_VAL|FLIP_V_VAL|FLIP_H_VAL)) {
    type_flags.push("ROT_270");
  } else if (is_type_flag_set(transform, FLIP_V_VAL|FLIP_H_VAL)) {
    type_flags.push("ROT_180");
  } else {
    if (is_type_flag_set(transform, ROT_90_VAL)) {
      type_flags.push("ROT_90");
    }
    if (is_type_flag_set(transform, FLIP_V_VAL)) {
      type_flags.push("FLIP_V");
    }
    if (is_type_flag_set(transform, FLIP_H_VAL)) {
      type_flags.push("FLIP_H");
    }
  }

  if (type_flags.length == 0) {
    throw "Unknown transform type " + transform ;
  }

  return type_flags.join(', ');
}


/**
 * Ensures all values of the transform object are set.
 */
function fill_transform_data(transform) {
  function fill_simple_transform(transform) {
    // ROT_270 = ROT_90|FLIP_H|FLIP_V;
    if (is_type_flag_set(transform, ROT_90_VAL|FLIP_V_VAL|FLIP_H_VAL)) {
      transform.dsdx =  0.0;
      transform.dtdx = -1.0;
      transform.dsdy = 1.0;
      transform.dtdy = 0.0;
      return;
    }

    // ROT_180 = FLIP_H|FLIP_V;
    if (is_type_flag_set(transform, FLIP_V_VAL|FLIP_H_VAL)) {
      transform.dsdx = -1.0;
      transform.dtdx = 0.0;
      transform.dsdy = 0.0;
      transform.dtdy = -1.0;
      return;
    }

    // ROT_90
    if (is_type_flag_set(transform, ROT_90_VAL)) {
      transform.dsdx = 0.0;
      transform.dtdx = 1.0;
      transform.dsdy = -1.0;
      transform.dtdy = 0.0;
      return;
    }

    // IDENTITY
    if (is_type_flag_clear(transform, SCALE_VAL | ROTATE_VAL)) {
      transform.dsdx = 1.0;
      transform.dtdx = 0.0;
      transform.dsdy = 0.0;
      transform.dtdy = 1.0;
      transform.type = 0;
      return;
    }

    throw "Unknown transform type " + transform;
  }

  if (!transform) {
    return;
  }

  if (is_proto_2(transform)) {
    return;
  }

  if (is_simple_transform(transform)){
    fill_simple_transform(transform);
  }

  transform.dsdx = transform.dsdx || 0.0;
  transform.dtdx = transform.dtdx || 0.0;
  transform.dsdy = transform.dsdy || 0.0;
  transform.dtdy = transform.dtdy || 0.0;
}

function is_type_flag_set(transform, bits) {
    transform = transform || {};
    var type = transform.type || 0;
    return (type & bits) === bits;
}

function is_type_flag_clear(transform, bits) {
  transform = transform || {};
  var type = transform.type || 0;
  return (type & bits) === 0;
}

export {format_transform_type, fill_transform_data, is_simple_transform};