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

export class ArrayUtils {
  static equal<T>(
    a: T[] | TypedArray,
    b: T[] | TypedArray,
    predicate: (a: T | number, b: T | number) => boolean = (a, b) => a === b,
  ): boolean {
    if (a.length !== b.length) {
      return false;
    }

    for (let i = 0; i < a.length; i++) {
      if (!predicate(a[i], b[i])) {
        return false;
      }
    }

    return true;
  }

  static searchSubarray<T>(
    array: T[] | TypedArray,
    subarray: T[] | TypedArray,
  ): number | undefined {
    for (let i = 0; i + subarray.length <= array.length; ++i) {
      let match = true;

      for (let j = 0; j < subarray.length; ++j) {
        if (array[i + j] !== subarray[j]) {
          match = false;
          break;
        }
      }

      if (match) {
        return i;
      }
    }

    return undefined;
  }

  static binarySearchFirstGreaterOrEqual<T>(
    values: T[] | TypedArray,
    target: T,
  ): number | undefined {
    if (values.length === 0) {
      return undefined;
    }

    let low = 0;
    let high = values.length - 1;

    let result: number | undefined = undefined;

    while (low <= high) {
      const mid = (low + high) >> 1;

      if (values[mid] < target) {
        low = mid + 1;
      } else if (values[mid] > target) {
        if (result === undefined || result > mid) {
          result = mid;
        }
        high = mid - 1;
      } else {
        result = mid;
        high = mid - 1;
      }
    }

    return result;
  }

  static binarySearchFirstGreater<T>(
    values: T[] | TypedArray,
    target: T,
  ): number | undefined {
    if (values.length === 0) {
      return undefined;
    }

    let low = 0;
    let high = values.length - 1;

    let result: number | undefined = undefined;

    while (low <= high) {
      const mid = (low + high) >> 1;

      if (values[mid] < target) {
        low = mid + 1;
      } else if (values[mid] > target) {
        if (result === undefined || result > mid) {
          result = mid;
        }
        high = mid - 1;
      } else {
        low = mid + 1;
      }
    }

    return result;
  }

  static toUintLittleEndian(
    buffer: Uint8Array,
    start: number,
    end: number,
  ): bigint {
    let result = 0n;
    for (let i = end - 1; i >= start; --i) {
      result *= 256n;
      result += BigInt(buffer[i]);
    }
    return result;
  }

  static toIntLittleEndian(
    buffer: Uint8Array,
    start: number,
    end: number,
  ): bigint {
    const numOfBits = BigInt(Math.max(0, 8 * (end - start)));
    if (numOfBits <= 0n) {
      return 0n;
    }

    let result = ArrayUtils.toUintLittleEndian(buffer, start, end);
    const maxSignedValue = 2n ** (numOfBits - 1n) - 1n;
    if (result > maxSignedValue) {
      const valuesRange = 2n ** numOfBits;
      result -= valuesRange;
    }

    return result;
  }
}
