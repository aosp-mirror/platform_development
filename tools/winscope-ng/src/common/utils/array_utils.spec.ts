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
import {ArrayUtils} from './array_utils';

describe("ArrayUtils", () => {
  it("equal", () => {
    expect(ArrayUtils.equal([], [1])).toBeFalse();
    expect(ArrayUtils.equal([1], [])).toBeFalse();

    expect(ArrayUtils.equal([], [])).toBeTrue();
    expect(ArrayUtils.equal([undefined], [undefined])).toBeTrue();
    expect(ArrayUtils.equal([1, 2, 3], [1, 2, 3])).toBeTrue();

    expect(ArrayUtils.equal([], new Uint8Array(1))).toBeFalse();
    expect(ArrayUtils.equal([1], new Uint8Array(1))).toBeFalse();

    expect(ArrayUtils.equal([], new Uint8Array())).toBeTrue();
    expect(ArrayUtils.equal([], new Uint8Array())).toBeTrue();
    expect(ArrayUtils.equal([1, 2, 3], new Uint8Array([1, 2, 3]))).toBeTrue();

    expect(ArrayUtils.equal(new Uint8Array([]), new Uint8Array([1]))).toBeFalse();
    expect(ArrayUtils.equal(new Uint8Array([1]), new Uint8Array([]))).toBeFalse();

    expect(ArrayUtils.equal(new Uint8Array([]), new Uint8Array([]))).toBeTrue();
    expect(ArrayUtils.equal(new Uint8Array([1, 2, 3]), new Uint8Array([1, 2, 3]))).toBeTrue();
  });

  it("binarySearchLowerOrEqual", () => {
    // no match
    expect(ArrayUtils.binarySearchLowerOrEqual([], 5)).toBeUndefined();
    expect(ArrayUtils.binarySearchLowerOrEqual([6], 5)).toBeUndefined();
    expect(ArrayUtils.binarySearchLowerOrEqual([6, 7], 5)).toBeUndefined();
    expect(ArrayUtils.binarySearchLowerOrEqual([6, 7, 8], 5)).toBeUndefined();

    // match (lower)
    expect(ArrayUtils.binarySearchLowerOrEqual([4], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchLowerOrEqual([3, 4], 5)).toEqual(1);
    expect(ArrayUtils.binarySearchLowerOrEqual([2, 3, 4], 5)).toEqual(2);
    expect(ArrayUtils.binarySearchLowerOrEqual([2, 3, 4, 6], 5)).toEqual(2);
    expect(ArrayUtils.binarySearchLowerOrEqual([2, 3, 4, 6, 7], 5)).toEqual(2);

    // match (equal)
    expect(ArrayUtils.binarySearchLowerOrEqual([5], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchLowerOrEqual([4, 5], 5)).toEqual(1);
    expect(ArrayUtils.binarySearchLowerOrEqual([3, 4, 5], 5)).toEqual(2);
    expect(ArrayUtils.binarySearchLowerOrEqual([3, 4, 5, 6], 5)).toEqual(2);
    expect(ArrayUtils.binarySearchLowerOrEqual([3, 4, 5, 6, 7], 5)).toEqual(2);
  });
});
