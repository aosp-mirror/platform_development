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

describe('ArrayUtils', () => {
  it('equal', () => {
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

    expect(
      ArrayUtils.equal(new Uint8Array([]), new Uint8Array([1])),
    ).toBeFalse();
    expect(
      ArrayUtils.equal(new Uint8Array([1]), new Uint8Array([])),
    ).toBeFalse();

    expect(ArrayUtils.equal(new Uint8Array([]), new Uint8Array([]))).toBeTrue();
    expect(
      ArrayUtils.equal(new Uint8Array([1, 2, 3]), new Uint8Array([1, 2, 3])),
    ).toBeTrue();
  });

  it('searchSubarray', () => {
    expect(ArrayUtils.searchSubarray([], [0])).toEqual(undefined);
    expect(ArrayUtils.searchSubarray([], [])).toEqual(0);
    expect(ArrayUtils.searchSubarray([0], [])).toEqual(0);

    expect(ArrayUtils.searchSubarray([0, 1, 2], [-1])).toEqual(undefined);
    expect(ArrayUtils.searchSubarray([0, 1, 2], [])).toEqual(0);
    expect(ArrayUtils.searchSubarray([0, 1, 2], [0])).toEqual(0);
    expect(ArrayUtils.searchSubarray([0, 1, 2], [1])).toEqual(1);
    expect(ArrayUtils.searchSubarray([0, 1, 2], [2])).toEqual(2);

    expect(ArrayUtils.searchSubarray([0, 1, 2], [0, 1])).toEqual(0);
    expect(ArrayUtils.searchSubarray([0, 1, 2], [1, 2])).toEqual(1);
    expect(ArrayUtils.searchSubarray([0, 1, 2], [2])).toEqual(2);
    expect(ArrayUtils.searchSubarray([0, 1, 2], [2, 3])).toEqual(undefined);
  });

  it('binarySearchFirstGreaterOrEqual', () => {
    // no match
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([], 9)).toBeUndefined();
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([8], 9)).toBeUndefined();
    expect(
      ArrayUtils.binarySearchFirstGreaterOrEqual([7, 8], 9),
    ).toBeUndefined();
    expect(
      ArrayUtils.binarySearchFirstGreaterOrEqual([6, 7, 8], 9),
    ).toBeUndefined();

    // match (greater)
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([6], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([6, 7], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([4, 6], 5)).toEqual(1);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([4, 6, 7, 8], 5)).toEqual(
      1,
    );
    expect(
      ArrayUtils.binarySearchFirstGreaterOrEqual([3, 4, 6, 7, 8], 5),
    ).toEqual(2);

    // match (equal)
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([5], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([5, 6], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([4, 5], 5)).toEqual(1);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([3, 4, 5], 5)).toEqual(2);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([3, 4, 5, 6], 5)).toEqual(
      2,
    );
    expect(
      ArrayUtils.binarySearchFirstGreaterOrEqual([3, 4, 5, 6, 7], 5),
    ).toEqual(2);

    // match (equal with repeated values)
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([5, 5], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([5, 5, 5], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([5, 5, 5, 5], 5)).toEqual(
      0,
    );
    expect(ArrayUtils.binarySearchFirstGreaterOrEqual([4, 5, 5, 6], 5)).toEqual(
      1,
    );
    expect(
      ArrayUtils.binarySearchFirstGreaterOrEqual([4, 4, 5, 5, 5, 6], 5),
    ).toEqual(2);
    expect(
      ArrayUtils.binarySearchFirstGreaterOrEqual([4, 4, 4, 5, 5, 5, 5, 6], 5),
    ).toEqual(3);
  });

  it('binarySearchFirstGreater', () => {
    // no match
    expect(ArrayUtils.binarySearchFirstGreater([], 9)).toBeUndefined();
    expect(ArrayUtils.binarySearchFirstGreater([8], 9)).toBeUndefined();
    expect(ArrayUtils.binarySearchFirstGreater([7, 8], 9)).toBeUndefined();
    expect(ArrayUtils.binarySearchFirstGreater([6, 7, 8], 9)).toBeUndefined();

    // match
    expect(ArrayUtils.binarySearchFirstGreater([6], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreater([6, 7], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreater([4, 6], 5)).toEqual(1);
    expect(ArrayUtils.binarySearchFirstGreater([4, 6, 7, 8], 5)).toEqual(1);
    expect(ArrayUtils.binarySearchFirstGreater([3, 4, 6, 7, 8], 5)).toEqual(2);

    // match (ignore equal)
    expect(ArrayUtils.binarySearchFirstGreater([5], 5)).toEqual(undefined);
    expect(ArrayUtils.binarySearchFirstGreater([5, 6], 5)).toEqual(1);
    expect(ArrayUtils.binarySearchFirstGreater([4, 5, 6], 5)).toEqual(2);
    expect(ArrayUtils.binarySearchFirstGreater([3, 4, 5, 6], 5)).toEqual(3);
    expect(ArrayUtils.binarySearchFirstGreater([3, 4, 5, 6, 7], 5)).toEqual(3);

    // match (with repeated values)
    expect(ArrayUtils.binarySearchFirstGreater([6, 6], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreater([6, 6, 6], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreater([6, 6, 6, 6], 5)).toEqual(0);
    expect(ArrayUtils.binarySearchFirstGreater([5, 6, 6, 7], 5)).toEqual(1);
    expect(ArrayUtils.binarySearchFirstGreater([5, 5, 6, 6, 6, 7], 5)).toEqual(
      2,
    );
    expect(
      ArrayUtils.binarySearchFirstGreater([5, 5, 5, 6, 6, 6, 6, 7], 5),
    ).toEqual(3);
  });

  it('toUintLittleEndian', () => {
    const buffer = new Uint8Array([0, 0, 1, 1]);

    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0xff, 0xff]), 0, -1),
    ).toEqual(0n);
    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0xff, 0xff]), 0, 0),
    ).toEqual(0n);
    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0xff, 0xff]), 1, 1),
    ).toEqual(0n);

    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0x00, 0x01, 0xff]), 0, 1),
    ).toEqual(0n);
    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0x00, 0x01, 0xff]), 1, 2),
    ).toEqual(1n);
    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0x00, 0x01, 0xff]), 2, 3),
    ).toEqual(255n);

    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0x00, 0x00]), 0, 2),
    ).toEqual(0n);
    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0x01, 0x00]), 0, 2),
    ).toEqual(1n);
    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0x00, 0x01]), 0, 2),
    ).toEqual(256n);
    expect(
      ArrayUtils.toUintLittleEndian(new Uint8Array([0xff, 0xff]), 0, 2),
    ).toEqual(0xffffn);

    expect(
      ArrayUtils.toUintLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff]),
        0,
        4,
      ),
    ).toEqual(0xffffffffn);

    expect(
      ArrayUtils.toUintLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        0,
        8,
      ),
    ).toEqual(0xffffffffffffffffn);

    expect(
      ArrayUtils.toUintLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        0,
        9,
      ),
    ).toEqual(0xffffffffffffffffffn);
  });

  it('toIntLittleEndian', () => {
    expect(ArrayUtils.toIntLittleEndian(new Uint8Array([0xff]), 0, -1)).toEqual(
      0n,
    );
    expect(ArrayUtils.toIntLittleEndian(new Uint8Array([0xff]), 0, 0)).toEqual(
      0n,
    );

    expect(ArrayUtils.toIntLittleEndian(new Uint8Array([0x00]), 0, 1)).toEqual(
      0n,
    );
    expect(ArrayUtils.toIntLittleEndian(new Uint8Array([0x01]), 0, 1)).toEqual(
      1n,
    );
    expect(ArrayUtils.toIntLittleEndian(new Uint8Array([0x7f]), 0, 1)).toEqual(
      127n,
    );
    expect(ArrayUtils.toIntLittleEndian(new Uint8Array([0x80]), 0, 1)).toEqual(
      -128n,
    );
    expect(ArrayUtils.toIntLittleEndian(new Uint8Array([0xff]), 0, 1)).toEqual(
      -1n,
    );

    expect(
      ArrayUtils.toIntLittleEndian(new Uint8Array([0xff, 0x7f]), 0, 2),
    ).toEqual(32767n);
    expect(
      ArrayUtils.toIntLittleEndian(new Uint8Array([0x00, 0x80]), 0, 2),
    ).toEqual(-32768n);
    expect(
      ArrayUtils.toIntLittleEndian(new Uint8Array([0x01, 0x80]), 0, 2),
    ).toEqual(-32767n);
    expect(
      ArrayUtils.toIntLittleEndian(new Uint8Array([0xff, 0xff]), 0, 2),
    ).toEqual(-1n);

    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0x7f]),
        0,
        4,
      ),
    ).toEqual(0x7fffffffn);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0x00, 0x00, 0x00, 0x80]),
        0,
        4,
      ),
    ).toEqual(-0x80000000n);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0x01, 0x00, 0x00, 0x80]),
        0,
        4,
      ),
    ).toEqual(-0x7fffffffn);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff]),
        0,
        4,
      ),
    ).toEqual(-1n);

    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f]),
        0,
        8,
      ),
    ).toEqual(0x7fffffffffffffffn);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80]),
        0,
        8,
      ),
    ).toEqual(-0x8000000000000000n);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80]),
        0,
        8,
      ),
    ).toEqual(-0x7fffffffffffffffn);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        0,
        8,
      ),
    ).toEqual(-1n);

    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f]),
        0,
        9,
      ),
    ).toEqual(0x7fffffffffffffffffn);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80]),
        0,
        9,
      ),
    ).toEqual(-0x800000000000000000n);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80]),
        0,
        9,
      ),
    ).toEqual(-0x7fffffffffffffffffn);
    expect(
      ArrayUtils.toIntLittleEndian(
        new Uint8Array([0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff]),
        0,
        9,
      ),
    ).toEqual(-1n);
  });
});
