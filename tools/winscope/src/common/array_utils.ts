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

/**
 * Utility functions for working with arrays.
 */
export class ArrayUtils {
  /**
   * Checks if two arrays are equal.
   *
   * @param a The first array.
   * @param b The second array.
   * @param predicate A function that takes two elements and returns true if they are equal. Defaults to strict equality.
   *  True if the arrays are equal, false otherwise.
   */
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

  /**
   * Searches for a subarray within an array.
   *
   * @param array The array to search in.
   * @param subarray The subarray to search for.
   * @return The index of the first occurrence of the subarray, or undefined if it is not found.
   */
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

  /**
   * Performs a binary search to find the first element in the array that is greater than or equal to the target value.
   *
   * @param values The array to search in.
   * @param target The value to search for.
   * @return The index of the first element that is greater than or equal to the target value, or undefined if no such element exists.
   */
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

  /**
   * Performs a binary search to find the first element in the array that is greater than the target value.
   *
   * @param values The array to search in.
   * @param target The value to search for.
   * @return The index of the first element that is greater than the target value, or undefined if no such element exists.
   */
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

  /**
   * Converts an array of bytes to a bigint in little-endian order.
   *
   * @param buffer The array of bytes to convert.
   * @param start The starting index of the bytes to convert.
   * @param end The ending index of the bytes to convert.
   * @return The bigint representation of the bytes in little-endian order.
   */
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

  /**
   * Converts an array of bytes to a bigint in little-endian order, treating the bytes as a signed integer.
   *
   * @param buffer The array of bytes to convert.
   * @param start The starting index of the bytes to convert.
   * @param end The ending index of the bytes to convert.
   * @return The bigint representation of the bytes in little-endian order, treating the bytes as a signed integer.
   */
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
