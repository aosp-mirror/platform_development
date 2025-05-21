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

import {
  binarySearchFirstGreater,
  binarySearchFirstGreaterOrEqual,
  equal,
  searchSubarray,
  toIntLittleEndian,
  toUintLittleEndian,
} from './array_utils';

describe('ArrayUtils', () => {
  it('equal', () => {
    expect(equal([], [1])).toBeFalse();
    expect(equal([1], [])).toBeFalse();

    expect(equal([], [])).toBeTrue();
    expect(equal([undefined], [undefined])).toBeTrue();
    expect(equal([1, 2, 3], [1, 2, 3])).toBeTrue();

    expect(equal([], new Uint8Array(1))).toBeFalse();
    expect(equal([1], new Uint8Array(1))).toBeFalse();

    expect(equal([], new Uint8Array(0))).toBeTrue();
    expect(equal([1, 2, 3], new Uint8Array([1, 2, 3]))).toBeTrue();

    expect(equal(new Uint8Array([]), new Uint8Array([1]))).toBeFalse();
    expect(equal(new Uint8Array([1]), new Uint8Array([]))).toBeFalse();

    expect(equal(new Uint8Array([]), new Uint8Array([]))).toBeTrue();
    expect(
      equal(new Uint8Array([1, 2, 3]), new Uint8Array([1, 2, 3])),
    ).toBeTrue();
  });

  it('equal with predicate', () => {
    const predicate = (a: number, b: number) => a !== b;
    expect(equal([], [1], predicate)).toBeFalse();
    expect(equal([1], [], predicate)).toBeFalse();

    expect(equal([], [], predicate)).toBeTrue();
    expect(equal([1, 2, 3], [1, 2, 3], predicate)).toBeFalse();

    expect(equal([], new Uint8Array(1), predicate)).toBeFalse();
    expect(equal([1], new Uint8Array(1), predicate)).toBeTrue();

    expect(equal([], new Uint8Array(0), predicate)).toBeTrue();
    expect(equal([1, 2, 3], new Uint8Array([1, 2, 3]), predicate)).toBeFalse();

    expect(
      equal(new Uint8Array([]), new Uint8Array([1]), predicate),
    ).toBeFalse();
    expect(
      equal(new Uint8Array([1]), new Uint8Array([]), predicate),
    ).toBeFalse();

    expect(equal(new Uint8Array([]), new Uint8Array([]), predicate)).toBeTrue();
    expect(
      equal(new Uint8Array([1, 2, 3]), new Uint8Array([1, 2, 3]), predicate),
    ).toBeFalse();

    const predicateWithNonNumberType = (
      a: number | undefined,
      b: number | undefined,
    ) => a !== b;
    expect(
      equal([undefined], [undefined], predicateWithNonNumberType),
    ).toBeFalse();
  });

  it('searchSubarray', () => {
    expect(searchSubarray([], [0])).toEqual(undefined);
    expect(searchSubarray([], [])).toEqual(0);
    expect(searchSubarray([0], [])).toEqual(0);

    expect(searchSubarray([0, 1, 2], [-1])).toEqual(undefined);
    expect(searchSubarray([0, 1, 2], [])).toEqual(0);
    expect(searchSubarray([0, 1, 2], [0])).toEqual(0);
    expect(searchSubarray([0, 1, 2], [1])).toEqual(1);
    expect(searchSubarray([0, 1, 2], [2])).toEqual(2);

    expect(searchSubarray([0, 1, 2], [0, 1])).toEqual(0);
    expect(searchSubarray([0, 1, 2], [1, 2])).toEqual(1);
    expect(searchSubarray([0, 1, 2], [2])).toEqual(2);
    expect(searchSubarray([0, 1, 2], [2, 3])).toEqual(undefined);
  });

  it('binarySearchFirstGreaterOrEqual', () => {
    // no match
    expect(binarySearchFirstGreaterOrEqual([], 9)).toBeUndefined();
    expect(binarySearchFirstGreaterOrEqual([8], 9)).toBeUndefined();
    expect(binarySearchFirstGreaterOrEqual([7, 8], 9)).toBeUndefined();
    expect(binarySearchFirstGreaterOrEqual([6, 7, 8], 9)).toBeUndefined();

    // match (greater)
    expect(binarySearchFirstGreaterOrEqual([6], 5)).toEqual(0);
    expect(binarySearchFirstGreaterOrEqual([6, 7], 5)).toEqual(0);
    expect(binarySearchFirstGreaterOrEqual([4, 6], 5)).toEqual(1);
    expect(binarySearchFirstGreaterOrEqual([4, 6, 7, 8], 5)).toEqual(1);
    expect(binarySearchFirstGreaterOrEqual([3, 4, 6, 7, 8], 5)).toEqual(2);

    // match (equal)
    expect(binarySearchFirstGreaterOrEqual([5], 5)).toEqual(0);
    expect(binarySearchFirstGreaterOrEqual([5, 6], 5)).toEqual(0);
    expect(binarySearchFirstGreaterOrEqual([4, 5], 5)).toEqual(1);
    expect(binarySearchFirstGreaterOrEqual([3, 4, 5], 5)).toEqual(2);
    expect(binarySearchFirstGreaterOrEqual([3, 4, 5, 6], 5)).toEqual(2);
    expect(binarySearchFirstGreaterOrEqual([3, 4, 5, 6, 7], 5)).toEqual(2);

    // match (equal with repeated values)
    expect(binarySearchFirstGreaterOrEqual([5, 5], 5)).toEqual(0);
    expect(binarySearchFirstGreaterOrEqual([5, 5, 5], 5)).toEqual(0);
    expect(binarySearchFirstGreaterOrEqual([5, 5, 5, 5], 5)).toEqual(0);
    expect(binarySearchFirstGreaterOrEqual([4, 5, 5, 6], 5)).toEqual(1);
    expect(binarySearchFirstGreaterOrEqual([4, 4, 5, 5, 5, 6], 5)).toEqual(2);
    expect(
      binarySearchFirstGreaterOrEqual([4, 4, 4, 5, 5, 5, 5, 6], 5),
    ).toEqual(3);
  });

  it('binarySearchFirstGreater', () => {
    // no match
    expect(binarySearchFirstGreater([], 9)).toBeUndefined();
    expect(binarySearchFirstGreater([8], 9)).toBeUndefined();
    expect(binarySearchFirstGreater([7, 8], 9)).toBeUndefined();
    expect(binarySearchFirstGreater([6, 7, 8], 9)).toBeUndefined();

    // match
    expect(binarySearchFirstGreater([6], 5)).toEqual(0);
    expect(binarySearchFirstGreater([6, 7], 5)).toEqual(0);
    expect(binarySearchFirstGreater([4, 6], 5)).toEqual(1);
    expect(binarySearchFirstGreater([4, 6, 7, 8], 5)).toEqual(1);
    expect(binarySearchFirstGreater([3, 4, 6, 7, 8], 5)).toEqual(2);

    // match (ignore equal)
    expect(binarySearchFirstGreater([5], 5)).toEqual(undefined);
    expect(binarySearchFirstGreater([5, 6], 5)).toEqual(1);
    expect(binarySearchFirstGreater([4, 5, 6], 5)).toEqual(2);
    expect(binarySearchFirstGreater([3, 4, 5, 6], 5)).toEqual(3);
    expect(binarySearchFirstGreater([3, 4, 5, 6, 7], 5)).toEqual(3);

    // match (with repeated values)
    expect(binarySearchFirstGreater([6, 6], 5)).toEqual(0);
    expect(binarySearchFirstGreater([6, 6, 6], 5)).toEqual(0);
    expect(binarySearchFirstGreater([6, 6, 6, 6], 5)).toEqual(0);
    expect(binarySearchFirstGreater([5, 6, 6, 7], 5)).toEqual(1);
    expect(binarySearchFirstGreater([5, 5, 6, 6, 6, 7], 5)).toEqual(2);
    expect(binarySearchFirstGreater([5, 5, 5, 6, 6, 6, 6, 7], 5)).toEqual(3);
  });

  it('toUintLittleEndian', () => {
    const buffer = new Uint8Array([0, 0, 1, 1]);

    expect(toUintLittleEndian(new Uint8Array([0xff, 0xff]), 0, -1)).toEqual(0n);
    expect(toUintLittleEndian(new Uint8Array([0xff, 0xff]), 0, 0)).toEqual(0n);
    expect(toUintLittleEndian(new Uint8Array([0xff, 0xff]), 1, 1)).toEqual(0n);

    expect(
      toUintLittleEndian(new Uint8Array([0x00, 0x01, 0xff]), 0, 1),
    ).toEqual(0n);
    expect(
      toUintLittleEndian(new Uint8Array([0x00, 0x01, 0xff]), 1, 2),
    ).toEqual(1n);
    expect(
      toUintLittleEndian(new Uint8Array([0x00, 0x01, 0xff]), 2, 3),
    ).toEqual(255n);

    expect(toUintLittleEndian(new Uint8Array([0x00, 0x00]), 0, 2)).toEqual(0n);
    expect(toUintLittleEndian(new Uint8Array([0x01, 0x00]), 0, 2)).toEqual(1n);
    expect(toUintLittleEndian(new Uint8Array([0x00, 0x01]), 0, 2)).toEqual(
      256n,
    );
    expect(toUintLittleEndian(new Uint8Array([0xff, 0xff]), 0, 2)).toEqual(
      0xffffn,
    );

    expect(
      toUintLittleEndian(new Uint8Array([0xff, 0xff, 0xff, 0xff]), 0, 4),
    ).toEqual(0xffffffffn);

    expect(
      toUintLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        0,
        8,
      ),
    ).toEqual(0xffffffffffffffffn);

    expect(
      toUintLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        0,
        9,
      ),
    ).toEqual(0xffffffffffffffffffn);
  });

  it('toIntLittleEndian', () => {
    expect(toIntLittleEndian(new Uint8Array([0xff]), 0, -1)).toEqual(0n);
    expect(toIntLittleEndian(new Uint8Array([0xff]), 0, 0)).toEqual(0n);

    expect(toIntLittleEndian(new Uint8Array([0x00]), 0, 1)).toEqual(0n);
    expect(toIntLittleEndian(new Uint8Array([0x01]), 0, 1)).toEqual(1n);
    expect(toIntLittleEndian(new Uint8Array([0x7f]), 0, 1)).toEqual(127n);
    expect(toIntLittleEndian(new Uint8Array([0x80]), 0, 1)).toEqual(-128n);
    expect(toIntLittleEndian(new Uint8Array([0xff]), 0, 1)).toEqual(-1n);

    expect(toIntLittleEndian(new Uint8Array([0xff, 0x7f]), 0, 2)).toEqual(
      32767n,
    );
    expect(toIntLittleEndian(new Uint8Array([0x00, 0x80]), 0, 2)).toEqual(
      -32768n,
    );
    expect(toIntLittleEndian(new Uint8Array([0x01, 0x80]), 0, 2)).toEqual(
      -32767n,
    );
    expect(toIntLittleEndian(new Uint8Array([0xff, 0xff]), 0, 2)).toEqual(-1n);

    expect(
      toIntLittleEndian(new Uint8Array([0xff, 0xff, 0xff, 0x7f]), 0, 4),
    ).toEqual(0x7fffffffn);
    expect(
      toIntLittleEndian(new Uint8Array([0x00, 0x00, 0x00, 0x80]), 0, 4),
    ).toEqual(-0x80000000n);
    expect(
      toIntLittleEndian(new Uint8Array([0x01, 0x00, 0x00, 0x80]), 0, 4),
    ).toEqual(-0x7fffffffn);
    expect(
      toIntLittleEndian(new Uint8Array([0xff, 0xff, 0xff, 0xff]), 0, 4),
    ).toEqual(-1n);

    expect(
      toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f]),
        0,
        8,
      ),
    ).toEqual(0x7fffffffffffffffn);
    expect(
      toIntLittleEndian(
        new Uint8Array([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80]),
        0,
        8,
      ),
    ).toEqual(-0x8000000000000000n);
    expect(
      toIntLittleEndian(
        new Uint8Array([0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80]),
        0,
        8,
      ),
    ).toEqual(-0x7fffffffffffffffn);
    expect(
      toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        0,
        8,
      ),
    ).toEqual(-1n);

    expect(
      toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f]),
        0,
        9,
      ),
    ).toEqual(0x7fffffffffffffffffn);
    expect(
      toIntLittleEndian(
        new Uint8Array([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80]),
        0,
        9,
      ),
    ).toEqual(-0x800000000000000000n);
    expect(
      toIntLittleEndian(
        new Uint8Array([0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80]),
        0,
        9,
      ),
    ).toEqual(-0x7fffffffffffffffffn);
    expect(
      toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        0,
        9,
      ),
    ).toEqual(-1n);
  });
});
