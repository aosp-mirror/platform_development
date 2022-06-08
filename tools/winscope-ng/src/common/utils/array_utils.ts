/*
 * Copyright (C) 2022 The Android Open Source Project
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
type TypedArray =
  | Int8Array
  | Uint8Array
  | Uint8ClampedArray
  | Int16Array
  | Uint16Array
  | Int32Array
  | Uint32Array
  | Float32Array
  | Float64Array;

class ArrayUtils {
  static equal<T>(a: T[] | TypedArray, b: T[] | TypedArray): boolean {
    if (a.length !== b.length) {
      return false;
    }

    for (let i = 0; i < a.length; i++) {
      if (a[i] != b[i]) {
        return false;
      }
    }

    return true;
  }

  static binarySearchLowerOrEqual<T>(values: T[] | TypedArray, target: T): number|undefined {
    if (values.length == 0) {
      return undefined;
    }

    let low = 0;
    let high = values.length - 1;

    let result: number|undefined = undefined;

    while(low <= high) {
      const mid = (low + high) >> 1;

      if (values[mid] < target) {
        result = mid;
        low = mid + 1;
      }
      else if (values[mid] > target) {
        high = mid - 1;
      }
      else {
        return mid;
      }
    }

    return result;
  }
}

export {ArrayUtils};
